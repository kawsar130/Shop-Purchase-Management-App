package com.shopkeeper.util

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized utility for application observability, error telemetry, and crash logging.
 * In production release builds, logs non-fatal exceptions and custom analytics keys to Firebase Crashlytics.
 * In debug builds, outputs to Android logcat.
 */
object CrashLogger {
    private const val TAG = "ShopKeeper"

    fun log(message: String) {
        try {
            Log.d(TAG, message)
            FirebaseCrashlytics.getInstance().log(message)
        } catch (_: Exception) {
            // Firebase may not be initialized in unit tests
        }
    }

    fun recordException(throwable: Throwable, customMessage: String? = null) {
        try {
            Log.e(TAG, customMessage ?: throwable.message ?: "Non-fatal error", throwable)
            customMessage?.let {
                FirebaseCrashlytics.getInstance().log(it)
            }
            FirebaseCrashlytics.getInstance().recordException(throwable)
        } catch (_: Exception) {
            // Fallback for JVM unit tests where Firebase is absent
        }
    }

    fun setShopContext(shopId: String, isOwner: Boolean) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("current_shop_id", shopId)
            crashlytics.setCustomKey("is_owner", isOwner)
        } catch (_: Exception) {
            // Ignore in unit test environments
        }
    }

    fun clearShopContext() {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.setCustomKey("current_shop_id", "")
            crashlytics.setCustomKey("is_owner", false)
        } catch (_: Exception) {
            // Ignore in unit test environments
        }
    }
}
