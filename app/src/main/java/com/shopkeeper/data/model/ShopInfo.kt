package com.shopkeeper.data.model

data class ShopInfo(
    val shopId: String = "",
    val shopName: String = "",
    val currencySymbol: String = "$",
    val ownerEmail: String = "",
    val ownerUid: String = "",
    val collaborators: List<String> = emptyList(),
    val isOwner: Boolean = true
)

data class StaffMember(
    val email: String = "",
    val role: String = "Staff",
    val displayName: String = ""
)
