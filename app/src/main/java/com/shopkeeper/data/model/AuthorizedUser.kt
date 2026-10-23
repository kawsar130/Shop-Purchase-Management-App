package com.shopkeeper.data.model

import com.google.gson.annotations.SerializedName

/**
 * Model representing an authorized shop staff member / owner.
 */
data class AuthorizedUser(
    @SerializedName("email")
    val email: String = "",

    @SerializedName("name")
    val name: String = "",

    @SerializedName("role")
    val role: String = "Staff"
) {
    val isAdmin: Boolean
        get() = role.equals("Admin", ignoreCase = true) || role.equals("Owner", ignoreCase = true)
}
