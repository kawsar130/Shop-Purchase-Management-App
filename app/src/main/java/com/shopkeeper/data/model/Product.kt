package com.shopkeeper.data.model

import java.util.UUID

/**
 * Domain model representing a product in the shop.
 * All financial amounts are stored as exact Long minor units (e.g. cents, paisa: 1.50 -> 150L).
 */
data class Product(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val category: String = "General",
    val purchasePriceMinor: Long = 0L,
    val salesPriceMinor: Long = 0L,
    val quantity: Int = 0,
    val updatedAt: String = ""
) {
    /**
     * Profit per single unit in minor units (Sales Price - Purchase Price)
     */
    val profitMinor: Long
        get() = salesPriceMinor - purchasePriceMinor

    /**
     * Profit per single unit in major currency units (Double accessor for UI compatibility)
     */
    val profit: Double
        get() = profitMinor / 100.0

    /**
     * Purchase price in major currency units (Double accessor for UI compatibility)
     */
    val purchasePrice: Double
        get() = purchasePriceMinor / 100.0

    /**
     * Sales price in major currency units (Double accessor for UI compatibility)
     */
    val salesPrice: Double
        get() = salesPriceMinor / 100.0

    /**
     * Markup as a percentage of purchase cost: ((Sales Price - Purchase Price) / Purchase Price) * 100
     * Answers: "How much did I mark up the cost?"
     */
    val markupPercentage: Double
        get() = when {
            purchasePriceMinor > 0L -> ((salesPriceMinor - purchasePriceMinor).toDouble() / purchasePriceMinor.toDouble()) * 100.0
            salesPriceMinor > 0L -> 100.0 // Pure profit on promotional or zero-cost stock
            else -> 0.0
        }

    /**
     * Gross Profit Margin as a percentage of sales price: ((Sales Price - Purchase Price) / Sales Price) * 100
     * Answers: "What percentage of the selling price is profit?"
     */
    val profitMarginPercentage: Double
        get() = when {
            salesPriceMinor > 0L -> ((salesPriceMinor - purchasePriceMinor).toDouble() / salesPriceMinor.toDouble()) * 100.0
            else -> 0.0
        }

    /**
     * Low stock flag (quick visual warning in inventory: 1 to 5 units remaining)
     */
    val isLowStock: Boolean
        get() = quantity in 1..5

    /**
     * Total inventory value at purchase cost in minor units
     */
    val totalPurchaseValueMinor: Long
        get() = purchasePriceMinor * quantity.coerceAtLeast(0)

    /**
     * Total inventory value at purchase cost in major currency units
     */
    val totalPurchaseValue: Double
        get() = totalPurchaseValueMinor / 100.0

    /**
     * Total inventory value at sales price in minor units
     */
    val totalSalesValueMinor: Long
        get() = salesPriceMinor * quantity.coerceAtLeast(0)

    /**
     * Total inventory value at sales price in major currency units
     */
    val totalSalesValue: Double
        get() = totalSalesValueMinor / 100.0

    companion object {
        /**
         * Helper to construct a Product from major currency units (Double), automatically
         * rounding to exact integer minor units.
         */
        fun fromMajorUnits(
            id: String = "",
            code: String = "",
            name: String = "",
            category: String = "General",
            purchasePrice: Double = 0.0,
            salesPrice: Double = 0.0,
            quantity: Int = 0,
            updatedAt: String = ""
        ): Product = Product(
            id = id,
            code = code,
            name = name,
            category = category,
            purchasePriceMinor = kotlin.math.round(purchasePrice * 100.0).toLong(),
            salesPriceMinor = kotlin.math.round(salesPrice * 100.0).toLong(),
            quantity = quantity,
            updatedAt = updatedAt
        )
    }
}
