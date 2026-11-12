package com.shopkeeper.ui.screenshot

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.navigation.AdaptiveNavigationShell
import com.shopkeeper.ui.navigation.Screen
import com.shopkeeper.ui.screens.CategoryScreen
import com.shopkeeper.ui.theme.ShopKeeperTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = RobolectricDeviceQualifiers.Pixel7)
class ScreensScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val sampleProducts = listOf(
        Product(
            id = "1",
            code = "101",
            name = "Organic Green Tea (500g)",
            category = "Beverages",
            purchasePriceMinor = 12000L,
            salesPriceMinor = 18000L,
            quantity = 45,
            unit = "pcs"
        ),
        Product(
            id = "2",
            code = "102",
            name = "Ceramic Coffee Mug",
            category = "Kitchen",
            purchasePriceMinor = 5000L,
            salesPriceMinor = 7500L,
            quantity = 3,
            unit = "pcs"
        ),
        Product(
            id = "3",
            code = "103",
            name = "Wireless Optical Mouse",
            category = "Electronics",
            purchasePriceMinor = 25000L,
            salesPriceMinor = 22000L,
            quantity = 0,
            unit = "pcs"
        )
    )

    private val sampleCategories = listOf("All", "Beverages", "Kitchen", "Electronics")

    @Test
    fun categoryScreen_populated_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                CategoryScreen(
                    products = sampleProducts,
                    categories = sampleCategories,
                    currencySymbol = "$",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    fun categoryScreen_populated_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                CategoryScreen(
                    products = sampleProducts,
                    categories = sampleCategories,
                    currencySymbol = "$",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    @Config(qualifiers = "bn-" + RobolectricDeviceQualifiers.Pixel7)
    fun categoryScreen_populated_bengali() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                CategoryScreen(
                    products = sampleProducts,
                    categories = sampleCategories,
                    currencySymbol = "৳",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    @Config(qualifiers = "es-" + RobolectricDeviceQualifiers.Pixel7)
    fun categoryScreen_populated_spanish() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                CategoryScreen(
                    products = sampleProducts,
                    categories = sampleCategories,
                    currencySymbol = "€",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    fun categoryScreen_empty_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                CategoryScreen(
                    products = emptyList(),
                    categories = listOf("All", "General"),
                    currencySymbol = "$",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    fun categoryScreen_empty_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                CategoryScreen(
                    products = emptyList(),
                    categories = listOf("All", "General"),
                    currencySymbol = "$",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    @Config(qualifiers = DeviceQualifiers.TABLET)
    fun categoryScreen_expanded_tablet_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                CategoryScreen(
                    products = sampleProducts,
                    categories = sampleCategories,
                    currencySymbol = "$",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    @Config(qualifiers = DeviceQualifiers.FOLDABLE)
    fun categoryScreen_medium_foldable_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                CategoryScreen(
                    products = sampleProducts,
                    categories = sampleCategories,
                    currencySymbol = "$",
                    onCategoryClick = {},
                    onProductClick = {},
                    onAddCategory = {},
                    onRenameCategory = { _, _ -> },
                    onDeleteCategory = { _, _ -> }
                )
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }

    @Test
    @Config(qualifiers = DeviceQualifiers.TABLET)
    fun adaptiveNavigationShell_tablet_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                AdaptiveNavigationShell(
                    currentRoute = Screen.Categories.route,
                    syncStatus = SyncStatus.Success(),
                    onNavigateToProducts = {},
                    onNavigateToCategories = {},
                    onNavigateToSettings = {},
                    onRefreshSync = {}
                ) {
                    CategoryScreen(
                        products = sampleProducts,
                        categories = sampleCategories,
                        currencySymbol = "$",
                        onCategoryClick = {},
                        onProductClick = {},
                        onAddCategory = {},
                        onRenameCategory = { _, _ -> },
                        onDeleteCategory = { _, _ -> }
                    )
                }
            }
        }
        composeTestRule.onRoot().captureScreenshot()
    }
}
