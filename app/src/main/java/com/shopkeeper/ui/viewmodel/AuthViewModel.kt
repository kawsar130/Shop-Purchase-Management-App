package com.shopkeeper.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.shopkeeper.data.model.ShopInfo
import com.shopkeeper.data.remote.FirestoreService
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class NeedsShopSetup(val email: String, val displayName: String) : AuthState()
    data class SelectExistingShop(val email: String, val availableShops: List<ShopInfo>) : AuthState()
    data class Authenticated(val email: String, val shopName: String, val spreadsheetId: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(
    private val context: Context,
    private val userSession: UserSessionRepository,
    private val firestoreService: FirestoreService,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val googleSignInClient: GoogleSignInClient by lazy {
        val webClientId = try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else null
        } catch (e: Exception) {
            null
        }

        val builder = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()

        if (!webClientId.isNullOrBlank()) {
            builder.requestIdToken(webClientId)
        }

        GoogleSignIn.getClient(context, builder.build())
    }

    init {
        checkCurrentLogin()
    }

    fun checkCurrentLogin() {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null) {
            handleSignInSuccess(lastAccount)
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInSuccess(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading

            // Sign in to Firebase Auth using Google credentials if ID token is present
            var firebaseUid = auth.currentUser?.uid ?: ""
            if (!account.idToken.isNullOrBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    val authResult = auth.signInWithCredential(credential).await()
                    firebaseUid = authResult.user?.uid ?: firebaseUid
                } catch (e: Exception) {
                    // Fall back to account data if offline or network hiccup
                }
            }

            val email = account.email ?: auth.currentUser?.email ?: ""
            val name = account.displayName ?: auth.currentUser?.displayName ?: email.substringBefore("@")
            userSession.saveUser(email, name, firebaseUid)

            // 1. Check if user already has an active shop stored in session
            val savedShopId = userSession.getShopId()
            if (savedShopId.isNotEmpty()) {
                val shopName = userSession.shopNameFlow.first()
                _authState.value = AuthState.Authenticated(email, shopName, savedShopId)
                productRepository.syncFromRemote()
                return@launch
            }

            // 2. Query Cloud Firestore for shops owned by user or shared with them as staff
            val foundShopsResult = firestoreService.findUserShops(email, firebaseUid)
            val existingShops = foundShopsResult.getOrNull() ?: emptyList()

            if (existingShops.size == 1) {
                // Exactly 1 shop found (e.g. invited helper or existing owner) -> auto-select
                selectExistingShop(existingShops.first())
            } else if (existingShops.size > 1) {
                _authState.value = AuthState.SelectExistingShop(email, existingShops)
            } else {
                _authState.value = AuthState.NeedsShopSetup(email, name)
            }
        }
    }

    fun createNewShop(shopName: String, currencySymbol: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val trimmedName = shopName.trim().ifEmpty { "My Shop" }
            val trimmedCurrency = currencySymbol.trim().ifEmpty { "$" }
            val email = userSession.getEmail()
            val uid = userSession.getUid().ifEmpty { auth.currentUser?.uid ?: "" }

            val result = firestoreService.createShop(trimmedName, trimmedCurrency, uid, email)
            if (result.isSuccess) {
                val shopInfo = result.getOrThrow()
                userSession.saveShopInfo(shopInfo)
                productRepository.startRealtimeSync(shopInfo.shopId)
                _authState.value = AuthState.Authenticated(
                    email = email,
                    shopName = shopInfo.shopName,
                    spreadsheetId = shopInfo.shopId
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to create shop database"
                _authState.value = AuthState.Error(err)
            }
        }
    }

    fun selectExistingShop(shopInfo: ShopInfo) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userSession.saveShopInfo(shopInfo)
            productRepository.syncFromRemote()
            _authState.value = AuthState.Authenticated(
                email = userSession.getEmail(),
                shopName = shopInfo.shopName,
                spreadsheetId = shopInfo.shopId
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

    fun showError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun continueInDemoMode() {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            userSession.saveUser("demo@shop.local", "Shop Manager")
            userSession.saveShopInfo(
                ShopInfo(
                    shopId = "DEMO_SHOP",
                    shopName = "Demo Shop",
                    currencySymbol = "$",
                    ownerEmail = "demo@shop.local",
                    isOwner = true
                )
            )
            productRepository.seedDemoProductsIfEmpty()
            _authState.value = AuthState.Authenticated(
                email = "demo@shop.local",
                shopName = "Demo Shop",
                spreadsheetId = "DEMO_SHOP"
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                auth.signOut()
            } catch (e: Exception) {
                // Ignore
            }
            try {
                googleSignInClient.signOut()
            } catch (e: Exception) {
                // Ignore
            }
            productRepository.stopRealtimeSync()
            userSession.clearSession()
            _authState.value = AuthState.Idle
        }
    }
}

class AuthViewModelFactory(
    private val context: Context,
    private val userSession: UserSessionRepository,
    private val firestoreService: FirestoreService,
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context, userSession, firestoreService, productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
