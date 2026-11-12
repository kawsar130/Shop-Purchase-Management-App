package com.shopkeeper.ui.util

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.shopkeeper.R

/**
 * Supported display languages for ShopKeeper.
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String,
    @StringRes val labelRes: Int
) {
    SYSTEM("", "System Default", R.string.language_system_default),
    ENGLISH("en", "English", R.string.language_english),
    BENGALI("bn", "বাংলা", R.string.language_bengali),
    SPANISH("es", "Español", R.string.language_spanish);

    companion object {
        fun fromCode(code: String): AppLanguage {
            val normalized = code.lowercase().split("-", "_")[0]
            return entries.find { it.code == normalized } ?: SYSTEM
        }
    }
}

/**
 * Centralized manager for in-app and per-app language preferences.
 * Integrates with Android 13+ native LocaleManager and AppCompat backport.
 */
object LanguageManager {

    /**
     * Retrieves the currently active application language.
     * Returns [AppLanguage.SYSTEM] if following the system default.
     */
    fun getCurrentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        val firstLang = locales.toLanguageTags()
        return AppLanguage.fromCode(firstLang)
    }

    /**
     * Applies the chosen [AppLanguage].
     * Passing [AppLanguage.SYSTEM] clears the app override and reverts to system default.
     */
    fun setLanguage(language: AppLanguage) {
        val locales = if (language == AppLanguage.SYSTEM) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(language.code)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
