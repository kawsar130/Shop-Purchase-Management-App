package com.shopkeeper.ui.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatMoney Long minor units formats whole numbers with thousands separator`() {
        assertEquals("0", formatMoney(0L))
        assertEquals("50", formatMoney(5000L))
        assertEquals("1,000", formatMoney(100000L))
        assertEquals("1,500", formatMoney(150000L))
        assertEquals("1,000,000", formatMoney(100000000L))
    }

    @Test
    fun `formatMoney Long minor units formats decimals with two places`() {
        assertEquals("12.50", formatMoney(1250L))
        assertEquals("1,250.75", formatMoney(125075L))
        assertEquals("0.99", formatMoney(99L))
        assertEquals("0.05", formatMoney(5L))
        assertEquals("-1,250.50", formatMoney(-125050L))
    }

    @Test
    fun `formatInputNumber Long minor units formats without commas`() {
        assertEquals("0", formatInputNumber(0L))
        assertEquals("1500", formatInputNumber(150000L))
        assertEquals("1500.50", formatInputNumber(150050L))
        assertEquals("1500.05", formatInputNumber(150005L))
        assertEquals("1000000", formatInputNumber(100000000L))
    }

    @Test
    fun `parseToMinorUnits parses valid numeric strings to exact cents`() {
        assertEquals(1250L, parseToMinorUnits("12.50"))
        assertEquals(1250L, parseToMinorUnits("12.5"))
        assertEquals(1250L, parseToMinorUnits("12,50"))
        assertEquals(1200L, parseToMinorUnits("12"))
        assertEquals(1205L, parseToMinorUnits("12.05"))
        assertEquals(0L, parseToMinorUnits("0"))
        assertEquals(0L, parseToMinorUnits("0.00"))
        assertEquals(100000000L, parseToMinorUnits("1000000"))
        assertEquals(1235L, parseToMinorUnits("12.345")) // HALF_UP rounding
    }

    @Test
    fun `parseToMinorUnits returns null on invalid or blank strings`() {
        assertNull(parseToMinorUnits(""))
        assertNull(parseToMinorUnits("   "))
        assertNull(parseToMinorUnits("abc"))
        assertNull(parseToMinorUnits("12..50"))
    }

    @Test
    fun `formatMoney Double overload maintains backward compatibility`() {
        assertEquals("0", formatMoney(0.0))
        assertEquals("50", formatMoney(50.0))
        assertEquals("1,000", formatMoney(1000.0))
        assertEquals("1,500", formatMoney(1500.0))
        assertEquals("12.50", formatMoney(12.5))
        assertEquals("0", formatMoney(Double.NaN))
        assertEquals("0", formatMoney(Double.POSITIVE_INFINITY))
    }
}
