package com.shopkeeper.data.sync

/**
 * Interface decoupling repository sync scheduling from Android framework Context and WorkManager.
 */
interface SyncScheduler {
    fun scheduleSync()
}
