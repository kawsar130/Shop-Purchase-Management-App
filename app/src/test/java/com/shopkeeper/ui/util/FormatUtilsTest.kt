package com.shopkeeper.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatMoney formats whole numbers with thousands separator`() {
        assertEquals("0", formatMoney(0.0))
        assertEquals("50", formatMoney(50.0))
        assertEquals("1,000", formatMoney(1000.0))
        assertEquals("1,500", formatMoney(1500.0))
        assertEquals("1,000,000", formatMoney(1000000.0))
    }

    @Test
    fun `formatMoney formats decimals with two places`() {
        assertEquals("12.50", formatMoney(12.5))
        assertEquals("1,250.75", formatMoney(1250.75))
        assertEquals("0.99", formatMoney(0.99))
    }

    @Test
    fun `formatMoney handles NaN and Infinity safely`() {
        assertEquals("0", formatMoney(Double.NaN))
        assertEquals("0", formatMoney(Double.POSITIVE_INFINITY))
        assertEquals("0", formatMoney(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `formatInputNumber formats without comma separators`() {
        assertEquals("0", formatInputNumber(0.0))
        assertEquals("1500", formatInputNumber(1500.0))
        assertEquals("1500.50", formatInputNumber(1500.5))
        assertEquals("1000000", formatInputNumber(1000000.0))
    }
}
