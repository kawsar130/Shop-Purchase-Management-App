package com.shopkeeper.ui.navigation

/**
 * Type-safe navigation routes for ShopKeeper Compose navigation.
 */
sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Setup : Screen("setup")
    data object Products : Screen("products")
    data object Categories : Screen("categories")
    data object Settings : Screen("settings")
    data object Staff : Screen("staff")
}
