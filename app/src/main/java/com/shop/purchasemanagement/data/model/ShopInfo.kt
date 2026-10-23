package com.shop.purchasemanagement.data.model

data class ShopInfo(
    val shopId: String = "",
    val shopName: String = "",
    val currencySymbol: String = "$",
    val ownerEmail: String = "",
    val ownerUid: String = "",
    val collaborators: List<String> = emptyList(),
    val isOwner: Boolean = true
) {
    // Backwards-compatibility alias
    val spreadsheetId: String get() = shopId
}

data class StaffMember(
    val email: String = "",
    val role: String = "Staff",
    val displayName: String = "",
    val permissionId: String = email
)
