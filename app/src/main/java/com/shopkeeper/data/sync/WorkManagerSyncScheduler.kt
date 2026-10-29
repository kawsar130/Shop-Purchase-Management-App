package com.shopkeeper.data.sync

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : SyncScheduler {
    override fun scheduleSync() {
        SyncWorker.enqueue(context)
    }
}
