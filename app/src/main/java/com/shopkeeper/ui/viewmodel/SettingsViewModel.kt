package com.shopkeeper.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shopkeeper.data.model.StaffMember
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userSession: UserSessionRepository,
    private val productRepository: ProductRepository
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

    private var syncSuccessTimerJob: kotlinx.coroutines.Job? = null

    private val _staffMembers = MutableStateFlow<List<StaffMember>>(emptyList())
    val staffMembers: StateFlow<List<StaffMember>> = _staffMembers.asStateFlow()

    private val _message = MutableSharedFlow<String>()
    val message: SharedFlow<String> = _message.asSharedFlow()

    init {
        loadStaff()
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            val trimmed = symbol.trim()
            val result = productRepository.updateShopCurrency(trimmed)
            if (result.isSuccess) {
                _message.emit("Currency updated to '$trimmed'")
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to sync"
                _message.emit("Currency updated locally ($err)")
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = productRepository.syncFromRemote()
            _isSyncing.value = false
            if (result.isSuccess) {
                _message.emit("All products synced successfully.")
                _showSyncSuccess.value = true
                syncSuccessTimerJob?.cancel()
                syncSuccessTimerJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(10_000)
                    _showSyncSuccess.value = false
                }
            } else {
                val err = result.exceptionOrNull()?.message ?: "Unknown error"
                _message.emit("Sync failed: $err")
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
}

class SettingsViewModelFactory(
    private val userSession: UserSessionRepository,
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(userSession, productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
