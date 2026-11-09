package com.shopkeeper.ui.viewmodel

import android.content.Context
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.shopkeeper.data.model.ShopInfo
import com.shopkeeper.data.remote.FirestoreService
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class AuthState {
    object CheckingLogin : AuthState()
    object Idle : AuthState()
    object Loading : AuthState()
    data class NeedsShopSetup(val email: String, val displayName: String) : AuthState()
    data class SelectExistingShop(val email: String, val availableShops: List<ShopInfo>) : AuthState()
    data class Authenticated(val email: String, val shopName: String, val shopId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userSession: UserSessionRepository,
    private val firestoreService: FirestoreService,
    private val productRepository: ProductRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.CheckingLogin)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkCurrentLogin()
    }

    fun checkCurrentLogin() {
        viewModelScope.launch {
            val currentUser = auth.currentUser
            val savedShopId = userSession.getShopId()

            if (currentUser != null && savedShopId.isNotEmpty()) {
                val email = currentUser.email ?: ""
                val shopName = userSession.shopNameFlow.first()
                _authState.value = AuthState.Authenticated(email, shopName, savedShopId)
                productRepository.startRealtimeSync(savedShopId)
                return@launch
            }

            if (savedShopId.isNotEmpty()) {
                val email = userSession.userEmailFlow.first()
                val shopName = userSession.shopNameFlow.first()
                _authState.value = AuthState.Authenticated(email, shopName, savedShopId)
                productRepository.startRealtimeSync(savedShopId)
                return@launch
            }

            if (currentUser != null) {
                val email = currentUser.email ?: ""
                val name = currentUser.displayName ?: email.substringBefore("@")
                userSession.saveUser(email, name, currentUser.uid)
                resolveUserShops(email, currentUser.uid, name)
                return@launch
            }

            _authState.value = AuthState.Idle
        }
    }

    /**
     * Modern Google Authentication via Jetpack Credential Manager.
     */
    fun signInWithCredentialManager(context: Context) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
                val serverClientId = if (resId != 0) context.getString(resId) else null

                if (serverClientId.isNullOrBlank()) {
                    _authState.value = AuthState.Error("Google Web Client ID is not configured in google-services.json.")
                    return@launch
                }

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )

                handleCredential(result.credential)
            } catch (e: GetCredentialCancellationException) {
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Sign-in failed: ${e.localizedMessage ?: e.message}")
            }
        }
    }

    private suspend fun handleCredential(credential: Credential) {
        when (credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data)
                    authenticateWithFirebaseToken(
                        idToken = googleIdToken.idToken,
                        displayName = googleIdToken.displayName,
                        email = googleIdToken.id
                    )
                } else {
                    _authState.value = AuthState.Error("Unexpected credential type: ${credential.type}")
                }
            }
            else -> {
                _authState.value = AuthState.Error("Unsupported credential provider.")
            }
        }
    }

    private suspend fun authenticateWithFirebaseToken(idToken: String, displayName: String?, email: String?) {
        try {
            val cred = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(cred).await()
            val user = authResult.user
            if (user != null) {
                val effectiveEmail = user.email ?: email ?: ""
                val effectiveName = user.displayName ?: displayName ?: effectiveEmail.substringBefore("@")
                userSession.saveUser(effectiveEmail, effectiveName, user.uid)
                resolveUserShops(effectiveEmail, user.uid, effectiveName)
            } else {
                _authState.value = AuthState.Error("Failed to obtain authenticated Firebase user.")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Authentication error: ${e.localizedMessage ?: e.message}")
        }
    }

    private suspend fun resolveUserShops(email: String, uid: String, name: String) {
        val foundShopsResult = firestoreService.findUserShops(email, uid)
        val existingShops = foundShopsResult.getOrNull() ?: emptyList()

        if (existingShops.size == 1) {
            selectExistingShop(existingShops.first())
        } else if (existingShops.size > 1) {
            _authState.value = AuthState.SelectExistingShop(email, existingShops)
        } else {
            _authState.value = AuthState.NeedsShopSetup(email, name)
        }
    }

    fun createNewShop(shopName: String, currency: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val trimmedName = shopName.trim().ifEmpty { "My Shop" }
            val trimmedCurrency = currency.trim().ifEmpty { "$" }
            val ownerEmail = userSession.getEmail()
            val uid = auth.currentUser?.uid ?: ""

            val createResult = firestoreService.createShop(
                shopName = trimmedName,
                currency = trimmedCurrency,
                ownerEmail = ownerEmail,
                ownerUid = uid
            )

            if (createResult.isSuccess) {
                val createdShop = createResult.getOrThrow()
                userSession.saveShopInfo(createdShop)
                productRepository.seedDemoProductsIfEmpty()
                productRepository.startRealtimeSync(createdShop.shopId)
                _authState.value = AuthState.Authenticated(ownerEmail, createdShop.shopName, createdShop.shopId)
            } else {
                val err = createResult.exceptionOrNull()?.message ?: "Failed to create shop."
                _authState.value = AuthState.Error("Error creating shop: $err")
            }
        }
    }

    fun selectExistingShop(shop: ShopInfo) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userSession.saveShopInfo(shop)
            productRepository.syncFromRemote()
            productRepository.startRealtimeSync(shop.shopId)
            _authState.value = AuthState.Authenticated(
                email = userSession.getEmail(),
                shopName = shop.shopName,
                shopId = shop.shopId
            )
        }
    }

    fun resetToShopSetup() {
        viewModelScope.launch {
            val email = userSession.getEmail()
            val name = userSession.userNameFlow.first()
            _authState.value = AuthState.NeedsShopSetup(email, name)
        }
    }

    fun continueInDemoMode() {
        viewModelScope.launch {
            val demoShopId = "DEMO_SHOP_LOCAL"
            val demoShopName = "Demo Local Shop"
            userSession.saveUser(
                email = "demo@shopkeeper.local",
                name = "Demo Shopkeeper",
                uid = "DEMO_USER"
            )
            userSession.saveShopInfo(
                ShopInfo(
                    shopId = demoShopId,
                    shopName = demoShopName,
                    currencySymbol = "$",
                    ownerEmail = "demo@shopkeeper.local",
                    isOwner = true
                )
            )
            productRepository.seedDemoProductsIfEmpty()
            _authState.value = AuthState.Authenticated("demo@shopkeeper.local", demoShopName, demoShopId)
        }
    }

    fun showError(msg: String) {
        _authState.value = AuthState.Error(msg)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                // Ignore
            }
            productRepository.stopRealtimeSync()
            productRepository.clearLocalData()
            userSession.clearSession()
            _authState.value = AuthState.Idle
        }
    }

    fun signOut() {
        logout()
    }
}
