package com.shopkeeper.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class UiTextTest {

    @Test
    fun `DynamicString preserves exact string value`() {
        val text = UiText.DynamicString("Server timeout error")
        assertEquals("Server timeout error", text.value)
    }

    @Test
    fun `StringResource equality and hashCode contract`() {
        val res1 = UiText.StringResource(101, "arg1", 42)
        val res2 = UiText.StringResource(101, "arg1", 42)
        val res3 = UiText.StringResource(101, "arg2", 42)
        val res4 = UiText.StringResource(202, "arg1", 42)

        assertEquals(res1, res2)
        assertEquals(res1.hashCode(), res2.hashCode())
        assertNotEquals(res1, res3)
        assertNotEquals(res1, res4)
    }
}
