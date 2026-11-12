package com.shopkeeper.ui.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

/**
 * Standard monetary amount formatter for UI display using exact Long minor units.
 * Adds thousands comma separators:
 * - 150000L -> "1,500"
 * - 150050L -> "1,500.50"
 * - 150005L -> "1,500.05"
 * - 0L -> "0"
 * - -50L -> "-0.50"
 */
fun formatMoney(amountMinor: Long): String {
    if (amountMinor == 0L) return "0"
    val isNegative = amountMinor < 0
    val absMinor = kotlin.math.abs(amountMinor)
    val major = absMinor / 100
    val cents = absMinor % 100
    val sign = if (isNegative) "-" else ""
    val formattedMajor = String.format(Locale.US, "%,d", major)
    return if (cents == 0L) {
        "$sign$formattedMajor"
    } else {
        "$sign$formattedMajor.${String.format(Locale.US, "%02d", cents)}"
    }
}

/**
 * Clean numeric formatter for input editing fields using exact Long minor units (no commas).
 * - 150000L -> "1500"
 * - 150050L -> "1500.50"
 */
fun formatInputNumber(amountMinor: Long): String {
    if (amountMinor == 0L) return "0"
    val isNegative = amountMinor < 0
    val absMinor = kotlin.math.abs(amountMinor)
    val major = absMinor / 100
    val cents = absMinor % 100
    val sign = if (isNegative) "-" else ""
    return if (cents == 0L) {
        "$sign$major"
    } else {
        "$sign$major.${String.format(Locale.US, "%02d", cents)}"
    }
}

/**
 * Safely parses user-entered price text into integer minor units (e.g. cents).
 * Normalizes commas to decimal points, trims whitespace, and uses BigDecimal
 * with HALF_UP rounding to 2 decimal places to eliminate floating-point artifacts.
 * Returns null if the string is empty or invalid.
 */
fun parseToMinorUnits(input: String): Long? {
    val clean = input.trim().replace(',', '.')
    if (clean.isEmpty()) return null
    return try {
        val bd = BigDecimal(clean)
        bd.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    } catch (e: Exception) {
        null
    }
}

/**
 * Canonical product code display formatter.
 * Ensures that product codes are consistently prefixed with '#' across all UI surfaces
 * (ProductCard, ProductListScreen, CategoryScreen, DeleteDialog, etc.) without duplication.
 * Blank or empty strings are returned as empty.
 */
fun formatProductCode(code: String): String {
    val trimmed = code.trim()
    if (trimmed.isEmpty()) return ""
    return if (trimmed.startsWith("#")) trimmed else "#$trimmed"
}
