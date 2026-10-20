package com.shop.purchasemanagement.data.model

import com.google.gson.annotations.SerializedName

/**
 * Common response wrapper from Google Apps Script Web App
 */
data class ApiResponse<T>(
    @SerializedName("status")
    val status: String = "",

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("authorized")
    val authorized: Boolean = false,

    @SerializedName("products")
    val products: List<Product>? = null,

    @SerializedName("product")
    val product: Product? = null,

    @SerializedName("categories")
    val categories: List<String>? = null,

    @SerializedName("user")
    val user: AuthorizedUser? = null,

    @SerializedName("serverTime")
    val serverTime: String? = null
) {
    val isSuccess: Boolean
        get() = status.equals("success", ignoreCase = true)

    val isUnauthorized: Boolean
        get() = status.equals("unauthorized", ignoreCase = true)
}

/**
 * Request payload for POST calls to Apps Script
 */
data class PostPayload(
    @SerializedName("action")
    val action: String,

    @SerializedName("email")
    val email: String,

    @SerializedName("product")
    val product: Product? = null,

    @SerializedName("id")
    val id: String? = null,

    @SerializedName("code")
    val code: String? = null
)
