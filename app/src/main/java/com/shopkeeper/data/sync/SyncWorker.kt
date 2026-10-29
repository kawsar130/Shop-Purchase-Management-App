package com.shopkeeper.data.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.shopkeeper.data.repository.ProductRepository
import com.shopkeeper.data.repository.UserSessionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ProductRepository,
    private val userSession: UserSessionRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "com.shopkeeper.data.sync.SyncWorker"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "SyncWorker enqueued with CONNECTED network constraints")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val shopId = userSession.getShopId()
            if (shopId.isEmpty()) {
                Log.d(TAG, "SyncWorker: shopId is empty, skipping work")
                return Result.success()
            }

            val pendingCount = repository.getPendingCount()
            if (pendingCount == 0) {
                Log.d(TAG, "SyncWorker: 0 pending items, skipping work")
                return Result.success()
            }

            Log.d(TAG, "SyncWorker: pushing $pendingCount pending change(s) for shop $shopId")
            val pushSuccess = repository.pushPendingChanges(shopId)
            if (pushSuccess) {
                Log.d(TAG, "SyncWorker completed successfully")
                Result.success()
            } else {
                Log.w(TAG, "SyncWorker failed to push pending changes, scheduling retry")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker exception: ${e.message}", e)
            Result.retry()
        }
    }
}
