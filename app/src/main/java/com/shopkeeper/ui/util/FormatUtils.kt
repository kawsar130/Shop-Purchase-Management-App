package com.shopkeeper.ui.util

import java.util.Locale

/**
 * Standard monetary amount formatter for UI display.
 * Adds thousands comma separators:
 * - 1500.0 -> "1,500"
 * - 1500.5 -> "1,500.50"
 */
fun formatMoney(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite() || kotlin.math.abs(amount) < 0.0001) return "0"
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", amount)
    } else {
        String.format(Locale.US, "%,.2f", amount)
    }
}

/**
 * Clean numeric formatter for input editing fields (no commas to prevent text parsing issues).
 */
fun formatInputNumber(amount: Double): String {
    if (amount.isNaN() || amount.isInfinite() || kotlin.math.abs(amount) < 0.0001) return "0"
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", amount)
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
}
