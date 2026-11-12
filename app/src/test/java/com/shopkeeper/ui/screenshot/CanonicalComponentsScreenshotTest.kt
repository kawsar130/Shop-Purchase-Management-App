package com.shopkeeper.ui.screenshot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import com.shopkeeper.data.model.Product
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.screens.components.ProductCard
import com.shopkeeper.ui.screens.components.QuantityUnitRow
import com.shopkeeper.ui.screens.components.SearchBarComponent
import com.shopkeeper.ui.screens.components.ShopConfirmationDialog
import com.shopkeeper.ui.screens.components.ShopEmptyState
import com.shopkeeper.ui.screens.components.SyncStatusIndicator
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
class CanonicalComponentsScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // -------------------------------------------------------------------------
    // ShopEmptyState Tests
    // -------------------------------------------------------------------------

    @Test
    fun shopEmptyState_inventory_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface {
                    ShopEmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = "No products yet",
                        subtitle = "Add your first product to begin tracking stock.",
                        actionText = "Add Product",
                        actionIcon = Icons.Default.Add,
                        onActionClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun shopEmptyState_inventory_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface {
                    ShopEmptyState(
                        icon = Icons.Outlined.Inventory2,
                        title = "No products yet",
                        subtitle = "Add your first product to begin tracking stock.",
                        actionText = "Add Product",
                        actionIcon = Icons.Default.Add,
                        onActionClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun shopEmptyState_search_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface {
                    ShopEmptyState(
                        icon = Icons.Outlined.Search,
                        title = "No matching products",
                        subtitle = "No products match your search query or category filter.",
                        actionText = "Clear Filters",
                        actionIcon = Icons.Default.Refresh,
                        onActionClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun shopEmptyState_search_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface {
                    ShopEmptyState(
                        icon = Icons.Outlined.Search,
                        title = "No matching products",
                        subtitle = "No products match your search query or category filter.",
                        actionText = "Clear Filters",
                        actionIcon = Icons.Default.Refresh,
                        onActionClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun shopEmptyState_staff_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface {
                    ShopEmptyState(
                        icon = Icons.Outlined.People,
                        title = "No staff members yet",
                        subtitle = "Invite team members by email to help manage your store inventory."
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun shopEmptyState_staff_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface {
                    ShopEmptyState(
                        icon = Icons.Outlined.People,
                        title = "No staff members yet",
                        subtitle = "Invite team members by email to help manage your store inventory."
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    // -------------------------------------------------------------------------
    // SearchBarComponent Tests
    // -------------------------------------------------------------------------

    @Test
    fun searchBar_empty_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SearchBarComponent(
                        query = "",
                        onQueryChange = {},
                        placeholderText = "Search products or enter #code..."
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun searchBar_empty_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SearchBarComponent(
                        query = "",
                        onQueryChange = {},
                        placeholderText = "Search products or enter #code..."
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun searchBar_populated_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SearchBarComponent(
                        query = "Cotton Bath Towel",
                        onQueryChange = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun searchBar_populated_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SearchBarComponent(
                        query = "Cotton Bath Towel",
                        onQueryChange = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    // -------------------------------------------------------------------------
    // ProductCard Tests
    // -------------------------------------------------------------------------

    private val sampleInStockProduct = Product(
        id = "prod_1",
        code = "101",
        name = "Organic Green Tea (500g)",
        category = "Beverages",
        purchasePriceMinor = 12000L,
        salesPriceMinor = 18000L,
        quantity = 45,
        unit = "pcs"
    )

    private val sampleLowStockProduct = Product(
        id = "prod_2",
        code = "102",
        name = "Ceramic Coffee Mug",
        category = "Kitchen",
        purchasePriceMinor = 5000L,
        salesPriceMinor = 7500L,
        quantity = 3,
        unit = "pcs"
    )

    private val sampleOutOfStockProduct = Product(
        id = "prod_3",
        code = "103",
        name = "Wireless Optical Mouse",
        category = "Electronics",
        purchasePriceMinor = 25000L,
        salesPriceMinor = 22000L,
        quantity = 0,
        unit = "pcs"
    )

    @Test
    fun productCard_inStock_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ProductCard(
                        product = sampleInStockProduct,
                        currencySymbol = "$",
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun productCard_inStock_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ProductCard(
                        product = sampleInStockProduct,
                        currencySymbol = "$",
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun productCard_lowStock_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ProductCard(
                        product = sampleLowStockProduct,
                        currencySymbol = "$",
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun productCard_lowStock_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ProductCard(
                        product = sampleLowStockProduct,
                        currencySymbol = "$",
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun productCard_outOfStock_loss_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ProductCard(
                        product = sampleOutOfStockProduct,
                        currencySymbol = "$",
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun productCard_outOfStock_loss_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    ProductCard(
                        product = sampleOutOfStockProduct,
                        currencySymbol = "$",
                        onEditClick = {},
                        onDeleteClick = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    // -------------------------------------------------------------------------
    // QuantityUnitRow Tests
    // -------------------------------------------------------------------------

    @Test
    fun quantityUnitRow_normal_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    QuantityUnitRow(
                        quantityStr = "25",
                        onQuantityChange = {},
                        selectedUnit = "pcs",
                        onUnitSelected = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun quantityUnitRow_normal_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    QuantityUnitRow(
                        quantityStr = "25",
                        onQuantityChange = {},
                        selectedUnit = "pcs",
                        onUnitSelected = {}
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun quantityUnitRow_error_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    QuantityUnitRow(
                        quantityStr = "0",
                        onQuantityChange = {},
                        selectedUnit = "kg",
                        onUnitSelected = {},
                        errorMessage = "Quantity cannot be zero."
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun quantityUnitRow_error_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    QuantityUnitRow(
                        quantityStr = "0",
                        onQuantityChange = {},
                        selectedUnit = "kg",
                        onUnitSelected = {},
                        errorMessage = "Quantity cannot be zero."
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    // -------------------------------------------------------------------------
    // SyncStatusIndicator Tests
    // -------------------------------------------------------------------------

    @Test
    fun syncStatusIndicator_synced_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SyncStatusIndicator(
                        status = SyncStatus.Success("Synced with Cloud Database")
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun syncStatusIndicator_offlinePending_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SyncStatusIndicator(
                        status = SyncStatus.Offline(pendingChangesCount = 4)
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun syncStatusIndicator_error_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                Surface(modifier = Modifier.padding(16.dp)) {
                    SyncStatusIndicator(
                        status = SyncStatus.Error("Network failure")
                    )
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    // -------------------------------------------------------------------------
    // ShopConfirmationDialog Tests
    // -------------------------------------------------------------------------

    @Test
    fun shopConfirmationDialog_delete_light() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = false) {
                ShopConfirmationDialog(
                    onDismissRequest = {},
                    title = "Delete Product?",
                    subtitle = "Are you sure you want to delete this product? This action cannot be undone.",
                    icon = Icons.Outlined.Delete,
                    confirmButtonText = "Delete",
                    cancelButtonText = "Cancel",
                    onConfirm = {},
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Organic Green Tea",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Code: #101 • Beverages",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }

    @Test
    fun shopConfirmationDialog_delete_dark() {
        composeTestRule.setContent {
            ShopKeeperTheme(darkTheme = true) {
                ShopConfirmationDialog(
                    onDismissRequest = {},
                    title = "Delete Product?",
                    subtitle = "Are you sure you want to delete this product? This action cannot be undone.",
                    icon = Icons.Outlined.Delete,
                    confirmButtonText = "Delete",
                    cancelButtonText = "Cancel",
                    onConfirm = {},
                    content = {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Organic Green Tea",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Code: #101 • Beverages",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage()
    }
}
