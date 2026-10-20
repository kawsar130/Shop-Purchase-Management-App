package com.shop.purchasemanagement.data.model

/**
 * State representing synchronization between local cache and Google Sheets
 */
sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val message: String = "Synced with Google Sheet", val timestamp: Long = System.currentTimeMillis()) : SyncStatus()
    data class Error(val errorMessage: String) : SyncStatus()
    data class Offline(val pendingChangesCount: Int = 0) : SyncStatus()
}
