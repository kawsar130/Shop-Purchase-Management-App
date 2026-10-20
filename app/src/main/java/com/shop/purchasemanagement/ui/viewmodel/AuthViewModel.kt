package com.shop.purchasemanagement.ui.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.SheetsScopes
import com.shop.purchasemanagement.data.model.ShopInfo
import com.shop.purchasemanagement.data.remote.GoogleDriveSheetsService
import com.shop.purchasemanagement.data.repository.ProductRepository
import com.shop.purchasemanagement.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    private val driveSheetsService: GoogleDriveSheetsService,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                Scope(DriveScopes.DRIVE_FILE),
                Scope(SheetsScopes.SPREADSHEETS)
            )
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    init {
        checkCurrentLogin()
    }

    fun checkCurrentLogin() {
        val lastAccount = GoogleSignIn.getLastSignedInAccount(context)
        if (lastAccount != null && GoogleSignIn.hasPermissions(
                lastAccount,
                Scope(DriveScopes.DRIVE_FILE),
                Scope(SheetsScopes.SPREADSHEETS)
            )
        ) {
            handleSignInSuccess(lastAccount)
        }
    }

    fun getSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    fun handleSignInSuccess(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            driveSheetsService.setupCredentials(account)

            val email = account.email ?: ""
            val name = account.displayName ?: email.substringBefore("@")
            userSession.saveUser(email, name)

            val savedSpreadsheetId = userSession.getSpreadsheetId()
            if (savedSpreadsheetId.isNotEmpty()) {
                val shopName = userSession.shopNameFlow.first()
                _authState.value = AuthState.Authenticated(email, shopName, savedSpreadsheetId)
                productRepository.syncFromRemote()
                return@launch
            }

            // Check if user already has shop spreadsheets in Drive or shared with them
            val foundShopsResult = driveSheetsService.findExistingShopSpreadsheets()
            val existingShops = foundShopsResult.getOrNull() ?: emptyList()

            if (existingShops.isNotEmpty()) {
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

            val result = driveSheetsService.createShopSpreadsheet(trimmedName, trimmedCurrency)
            if (result.isSuccess) {
                val shopInfo = result.getOrThrow()
                userSession.saveShopInfo(shopInfo)
                productRepository.syncFromRemote()
                _authState.value = AuthState.Authenticated(
                    email = userSession.getEmail(),
                    shopName = shopInfo.shopName,
                    spreadsheetId = shopInfo.spreadsheetId
                )
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to create Google Sheet database"
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
                spreadsheetId = shopInfo.spreadsheetId
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

    fun logout() {
        viewModelScope.launch {
            googleSignInClient.signOut()
            userSession.clearSession()
            _authState.value = AuthState.Idle
        }
    }
}

class AuthViewModelFactory(
    private val context: Context,
    private val userSession: UserSessionRepository,
    private val driveSheetsService: GoogleDriveSheetsService,
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context, userSession, driveSheetsService, productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
