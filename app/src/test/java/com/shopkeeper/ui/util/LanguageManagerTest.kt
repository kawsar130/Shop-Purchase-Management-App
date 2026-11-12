package com.shopkeeper.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageManagerTest {

    @Test
    fun `fromCode resolves normalized language tags correctly`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromCode("en-US"))
        assertEquals(AppLanguage.BENGALI, AppLanguage.fromCode("bn"))
        assertEquals(AppLanguage.BENGALI, AppLanguage.fromCode("bn-BD"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromCode("es-ES"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromCode("fr"))
    }
}
