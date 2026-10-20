package com.shop.purchasemanagement.data.model

import com.google.gson.annotations.SerializedName

/**
 * Domain model representing a product in the shop.
 */
data class Product(
    @SerializedName("id")
    val id: String = "",

    @SerializedName("code")
    val code: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("category")
    val category: String = "General",

    @SerializedName("purchasePrice")
    val purchasePrice: Double = 0.0,

    @SerializedName("salesPrice")
    val salesPrice: Double = 0.0,

    @SerializedName("quantity")
    val quantity: Int = 0,

    @SerializedName("updatedAt")
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
