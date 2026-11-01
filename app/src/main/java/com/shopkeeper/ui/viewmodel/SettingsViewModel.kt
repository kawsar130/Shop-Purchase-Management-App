package com.shopkeeper.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.shopkeeper.data.model.StaffMember
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
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

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        loadStaff()
    }

    fun updateShopName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { _message.emit("Shop name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            userSession.updateShopMetadata(trimmed, currencySymbol.value)
            val currentShopId = userSession.getShopId()
            if (currentShopId.isNotEmpty() && isOwner.value) {
                productRepository.firestoreService.updateShopName(currentShopId, trimmed)
            }
            _message.emit("Shop name updated to '$trimmed'")
        }
    }

    fun updateCurrencySymbol(newCurrency: String) {
        viewModelScope.launch {
            userSession.updateCurrencySymbol(newCurrency)
            val currentShopId = userSession.getShopId()
            if (currentShopId.isNotEmpty() && isOwner.value) {
                productRepository.firestoreService.updateShopCurrency(currentShopId, newCurrency)
            }
            _message.emit("Currency updated to $newCurrency")
        }
    }

    fun triggerManualSync() {
        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch { _message.emit("Cannot sync offline. Check your internet connection.") }
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
                    _message.emit("Sync complete! Data is up to date.")
                } else {
                    val err = syncResult.exceptionOrNull()?.message ?: "Unknown sync error"
                    _message.emit("Sync failed: $err")
                }
            } catch (e: Exception) {
                _message.emit("Sync failed: ${e.message}")
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
                _message.emit("Inventory exported successfully.")
                onFileReady?.invoke(file)
            } else {
                val err = result.exceptionOrNull()?.message ?: "Export failed"
                _message.emit("Export error: $err")
            }
        }
    }

    fun exportInventory(context: Context) {
        viewModelScope.launch {
            val result = productRepository.exportInventoryToCsv(context)
            if (result.isSuccess) {
                _message.emit("Inventory exported successfully.")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Export failed"
                _message.emit("Export error: $err")
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
            viewModelScope.launch { _message.emit("Only the shop owner can manage staff members.") }
            return
        }

        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch { _message.emit("Internet connection is required to add staff members.") }
            return
        }

        val trimmed = email.trim().lowercase()
        if (trimmed.isEmpty() || !trimmed.contains("@")) {
            viewModelScope.launch { _message.emit("Please enter a valid Gmail address.") }
            return
        }

        viewModelScope.launch {
            val result = productRepository.addStaffMember(trimmed)
            if (result.isSuccess) {
                _message.emit("Added $trimmed to shop staff!")
                loadStaff()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to add staff"
                _message.emit("Error: $err")
            }
        }
    }

    fun removeStaff(email: String) {
        if (!isOwner.value) {
            viewModelScope.launch { _message.emit("Only the shop owner can manage staff members.") }
            return
        }

        if (!productRepository.networkMonitor.isCurrentlyOnline()) {
            viewModelScope.launch { _message.emit("Internet connection is required to remove staff members.") }
            return
        }

        viewModelScope.launch {
            val result = productRepository.removeStaffMember(email.trim().lowercase())
            if (result.isSuccess) {
                _message.emit("Staff member removed.")
                loadStaff()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to remove staff"
                _message.emit("Error: $err")
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
                _message.emit("Internet connection is required to delete your account and cloud data.")
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
                            _message.emit("Error deleting cloud data: $error")
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
                _message.emit("Account deletion failed: ${e.message}")
            }
        }
    }
}
