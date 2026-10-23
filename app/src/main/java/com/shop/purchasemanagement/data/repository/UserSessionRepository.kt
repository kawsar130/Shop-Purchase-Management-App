package com.shop.purchasemanagement.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shop.purchasemanagement.data.model.ShopInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shop_user_session")

class UserSessionRepository(private val context: Context) {

    companion object {
        private val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_UID = stringPreferencesKey("user_uid")
        private val KEY_SHOP_ID = stringPreferencesKey("shop_id")
        private val KEY_SPREADSHEET_ID = stringPreferencesKey("spreadsheet_id")
        private val KEY_SHOP_NAME = stringPreferencesKey("shop_name")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_IS_OWNER = booleanPreferencesKey("is_owner")
        private val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
    }

    val userEmailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_EMAIL] ?: ""
    }

    val userNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_NAME] ?: ""
    }

    val userUidFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_USER_UID] ?: ""
    }

    val shopIdFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOP_ID] ?: preferences[KEY_SPREADSHEET_ID] ?: ""
    }

    val spreadsheetIdFlow: Flow<String> = shopIdFlow

    val shopNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_SHOP_NAME] ?: "My Shop"
    }

    val currencySymbolFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_CURRENCY_SYMBOL] ?: "$"
    }

    val isOwnerFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_IS_OWNER] ?: true
    }

    val lastSyncTimeFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[KEY_LAST_SYNC_TIME] ?: 0L
    }

    suspend fun saveUser(email: String, name: String, uid: String = "") {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_EMAIL] = email.trim().lowercase()
            preferences[KEY_USER_NAME] = name
            if (uid.isNotEmpty()) {
                preferences[KEY_USER_UID] = uid
            }
        }
    }

    suspend fun saveShopInfo(shopInfo: ShopInfo) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SHOP_ID] = shopInfo.shopId
            preferences[KEY_SPREADSHEET_ID] = shopInfo.shopId
            preferences[KEY_SHOP_NAME] = shopInfo.shopName
            preferences[KEY_CURRENCY_SYMBOL] = shopInfo.currencySymbol
            preferences[KEY_IS_OWNER] = shopInfo.isOwner
        }
    }

    suspend fun updateCurrencySymbol(symbol: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CURRENCY_SYMBOL] = symbol.trim()
        }
    }

    suspend fun updateLastSyncTime(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LAST_SYNC_TIME] = timestamp
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    suspend fun getShopId(): String = shopIdFlow.first()
    suspend fun getSpreadsheetId(): String = getShopId()
    suspend fun getEmail(): String = userEmailFlow.first()
    suspend fun getUid(): String = userUidFlow.first()
}
