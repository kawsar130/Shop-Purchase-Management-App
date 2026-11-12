package com.shopkeeper.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.shopkeeper.R
import com.shopkeeper.data.model.StaffMember
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import com.shopkeeper.ui.util.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userSession: UserSessionRepository,
    private val productRepository: ProductRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    val shopName: StateFlow<String> = userSession.shopNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "My Shop")

    val currencySymbol: StateFlow<String> = userSession.currencySymbolFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "$")

    val userEmail: StateFlow<String> = userSession.userEmailFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isOwner: StateFlow<Boolean> = userSession.isOwnerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val shopId: StateFlow<String> = userSession.shopIdFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val lastSyncTime: StateFlow<Long> = userSession.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val syncStatus: StateFlow<SyncStatus> = productRepository.syncStatus

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _showSyncSuccess = MutableStateFlow(false)
    val showSyncSuccess: StateFlow<Boolean> = _showSyncSuccess.asStateFlow()

    private val _isDeletingAccount = MutableStateFlow(false)
    val isDeletingAccount: StateFlow<Boolean> = _isDeletingAccount.asStateFlow()

    private val _staffMembers = MutableStateFlow<List<StaffMember>>(emptyList())
    val staffMembers: StateFlow<List<StaffMember>> = _staffMembers.asStateFlow()

    private val _message = MutableSharedFlow<UiText>()
    val message: SharedFlow<UiText> = _message.asSharedFlow()

    init {
        loadStaff()
    }

    fun updateShopName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.setup_shop_name_required_error)) }
            return
        }
        viewModelScope.launch {
            userSession.updateShopMetadata(trimmed, currencySymbol.value)
            val currentShopId = userSession.getShopId()
            if (currentShopId.isNotEmpty() && isOwner.value) {
                productRepository.firestoreService.updateShopName(currentShopId, trimmed)
            }
            _message.emit(UiText.StringResource(R.string.settings_shop_name_updated, trimmed))
        }
    }

    fun updateCurrencySymbol(newCurrency: String) {
        viewModelScope.launch {
            userSession.updateCurrencySymbol(newCurrency)
            val currentShopId = userSession.getShopId()
            if (currentShopId.isNotEmpty() && isOwner.value) {
                productRepository.firestoreService.updateShopCurrency(currentShopId, newCurrency)
            }
            _message.emit(UiText.StringResource(R.string.settings_currency_updated, newCurrency))
        }
    }

    fun triggerManualSync() {
        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.settings_sync_offline_error)) }
            return
        }

        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val currentShopId = userSession.getShopId()
                if (currentShopId.isNotEmpty()) {
                    productRepository.pushPendingChanges(currentShopId)
                }
                val syncResult = productRepository.syncFromRemote()
                if (syncResult.isSuccess) {
                    _showSyncSuccess.value = true
                    userSession.updateLastSyncTime(System.currentTimeMillis())
                    _message.emit(UiText.StringResource(R.string.settings_sync_success))
                } else {
                    val err = syncResult.exceptionOrNull()?.message ?: "Unknown sync error"
                    _message.emit(UiText.StringResource(R.string.settings_sync_failed, err))
                }
            } catch (e: Exception) {
                _message.emit(UiText.StringResource(R.string.settings_sync_failed, e.message ?: ""))
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun syncNow() {
        triggerManualSync()
    }

    fun exportInventory(cacheDir: File, onFileReady: ((File) -> Unit)? = null) {
        viewModelScope.launch {
            val result = productRepository.exportInventoryToCsv(cacheDir)
            if (result.isSuccess) {
                val file = result.getOrThrow()
                _message.emit(UiText.StringResource(R.string.settings_export_success))
                onFileReady?.invoke(file)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Export failed"
                _message.emit(UiText.StringResource(R.string.settings_export_error, err))
            }
        }
    }

    fun exportInventory(context: Context) {
        viewModelScope.launch {
            val result = productRepository.exportInventoryToCsv(context)
            if (result.isSuccess) {
                _message.emit(UiText.StringResource(R.string.settings_export_success))
            } else {
                val err = result.exceptionOrNull()?.message ?: "Export failed"
                _message.emit(UiText.StringResource(R.string.settings_export_error, err))
            }
        }
    }

    fun loadStaff() {
        if (!productRepository.networkMonitor.isCurrentlyOnline()) return
        viewModelScope.launch {
            val result = productRepository.getStaffList()
            if (result.isSuccess) {
                _staffMembers.value = result.getOrNull() ?: emptyList()
            }
        }
    }

    fun addStaff(email: String) {
        if (!isOwner.value) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.staff_owner_only_error)) }
            return
        }

        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.staff_add_offline_error)) }
            return
        }

        val trimmed = email.trim().lowercase()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.staff_error_invalid_email)) }
            return
        }

        viewModelScope.launch {
            val result = productRepository.addStaffMember(trimmed)
            if (result.isSuccess) {
                _message.emit(UiText.StringResource(R.string.staff_added_success, trimmed))
                loadStaff()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to add staff"
                _message.emit(UiText.DynamicString("Error: $err"))
            }
        }
    }

    fun removeStaff(email: String) {
        if (!isOwner.value) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.staff_owner_only_error)) }
            return
        }

        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch { _message.emit(UiText.StringResource(R.string.staff_remove_offline_error)) }
            return
        }

        viewModelScope.launch {
            val result = productRepository.removeStaffMember(email.trim().lowercase())
            if (result.isSuccess) {
                _message.emit(UiText.StringResource(R.string.staff_removed_success))
                loadStaff()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to remove staff"
                _message.emit(UiText.DynamicString("Error: $err"))
            }
        }
    }

    /**
     * Permanently deletes user account, shop records, and associated cloud data.
     * Required by Google Play's Account Deletion mandate.
     */
    fun deleteAccountAndData(onSuccess: () -> Unit) {
        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch {
                _message.emit(UiText.StringResource(R.string.error_delete_account_offline))
            }
            return
        }

        viewModelScope.launch {
            _isDeletingAccount.value = true
            try {
                val currentShopId = userSession.getShopId()
                val email = userSession.getEmail()
                val isShopOwner = isOwner.value

                // 1. Delete remote shop data if owner, or remove staff mapping if collaborator
                if (currentShopId.isNotEmpty() && currentShopId != "DEMO_SHOP_LOCAL") {
                    if (isShopOwner) {
                        val deleteResult = productRepository.firestoreService.deleteShop(currentShopId)
                        if (deleteResult.isFailure) {
                            val error = deleteResult.exceptionOrNull()?.message ?: "Failed to delete cloud data"
                            _message.emit(UiText.DynamicString("Error deleting cloud data: $error"))
                            _isDeletingAccount.value = false
                            return@launch
                        }
                    } else if (email.isNotEmpty()) {
                        productRepository.firestoreService.removeStaff(currentShopId, email)
                    }
                }

                // 2. Delete Firebase Auth user account if present
                try {
                    auth.currentUser?.delete()?.await()
                } catch (e: Exception) {
                    // Fall back to sign out if token expired
                    try { auth.signOut() } catch (_: Exception) {}
                }

                // 3. Stop background sync and purge local storage
                productRepository.stopRealtimeSync()
                productRepository.clearLocalData()
                userSession.clearSession()

                _isDeletingAccount.value = false
                onSuccess()
            } catch (e: Exception) {
                _isDeletingAccount.value = false
                _message.emit(UiText.StringResource(R.string.settings_account_deletion_failed, e.message ?: ""))
            }
        }
    }
}
