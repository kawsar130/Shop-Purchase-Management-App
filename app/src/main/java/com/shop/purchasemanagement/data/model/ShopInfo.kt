package com.shop.purchasemanagement.data.model

data class ShopInfo(
    val spreadsheetId: String,
    val shopName: String,
    val currencySymbol: String = "$",
    val ownerEmail: String = "",
    val isOwner: Boolean = true
)

data class StaffMember(
    val permissionId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "Staff"
)
