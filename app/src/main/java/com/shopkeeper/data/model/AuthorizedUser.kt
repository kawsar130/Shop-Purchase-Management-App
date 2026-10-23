package com.shopkeeper.data.model

/**
 * Model representing an authorized shop staff member / owner.
 */
data class AuthorizedUser(
    val email: String = "",
    val name: String = "",
    val role: String = "Staff"
) {
    val isAdmin: Boolean
        get() = role.equals("Admin", ignoreCase = true) || role.equals("Owner", ignoreCase = true)
}
