package com.shopkeeper.data.model

/**
 * Domain model representing a product in the shop.
 */
data class Product(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val category: String = "General",
    val purchasePrice: Double = 0.0,
    val salesPrice: Double = 0.0,
    val quantity: Int = 0,
    val updatedAt: String = ""
) {
    /**
     * Profit per single unit (Sales Price - Purchase Price)
     */
    val profit: Double
        get() = salesPrice - purchasePrice

    /**
     * Markup / Profit Margin as a percentage of purchase price
     */
    val profitMarginPercentage: Double
        get() = if (purchasePrice > 0.0) {
            ((salesPrice - purchasePrice) / purchasePrice) * 100.0
        } else {
            0.0
        }

    /**
     * Low stock flag (useful for quick visual warning in inventory)
     */
    val isLowStock: Boolean
        get() = quantity in 0..5

    /**
     * Total inventory value at purchase cost
     */
    val totalPurchaseValue: Double
        get() = purchasePrice * quantity

    /**
     * Total inventory value at sales price
     */
    val totalSalesValue: Double
        get() = salesPrice * quantity
}
