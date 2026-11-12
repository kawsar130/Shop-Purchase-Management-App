package com.shopkeeper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shopkeeper.R
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.ShopSnackbarHost
import com.shopkeeper.ui.screens.components.CategoryChips
import com.shopkeeper.ui.screens.components.ProductCard
import com.shopkeeper.ui.screens.components.SearchBarComponent
import com.shopkeeper.ui.screens.components.ShopConfirmationDialog
import com.shopkeeper.ui.screens.components.ShopEmptyState
import com.shopkeeper.ui.screens.components.SyncStatusIndicator
import com.shopkeeper.ui.util.formatProductCode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.tooling.preview.Preview
import com.shopkeeper.ui.theme.ShopKeeperTheme
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val products = uiState.products
    val stats = uiState.stats
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val currencySymbol by viewModel.currencySymbol.collectAsStateWithLifecycle()
    val unfilteredProducts by viewModel.unfilteredProducts.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var productBeingEdited by remember { mutableStateOf<Product?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg.asString(context))
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            ShopSnackbarHost(snackbarHostState)
        },
        topBar = {
            val isFiltered = selectedCategory != "All" || searchQuery.isNotEmpty()
            ProductTopBarContent(
                totalCount = stats.totalCount,
                filteredCount = products.size,
                isFiltered = isFiltered,
                syncStatus = syncStatus,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { viewModel.onCategorySelected(it) },
                onRefreshSync = { viewModel.refresh() },
                onNavigateToCategories = onNavigateToCategories,
                onNavigateToSettings = onNavigateToSettings
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.products_add_product),
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(36.dp)
                    )
                }
            } else if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (stats.totalCount == 0) {
                        ShopEmptyState(
                            icon = Icons.Default.Add,
                            title = stringResource(R.string.products_empty_title),
                            subtitle = stringResource(R.string.products_empty_subtitle),
                            actionText = stringResource(R.string.products_add_product),
                            actionIcon = Icons.Default.Add,
                            onActionClick = { isAddDialogOpen = true }
                        )
                    } else {
                        ShopEmptyState(
                            icon = Icons.Default.SearchOff,
                            title = stringResource(R.string.search_no_results_title),
                            subtitle = stringResource(R.string.search_no_results_subtitle),
                            secondaryActionText = stringResource(R.string.search_clear_filters),
                            onSecondaryActionClick = {
                                viewModel.onSearchQueryChanged("")
                                viewModel.onCategorySelected("All")
                            }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(products, key = { it.id }) { product ->
                        @OptIn(ExperimentalFoundationApi::class)
                        ProductCard(
                            product = product,
                            currencySymbol = currencySymbol,
                            onEditClick = { productBeingEdited = it },
                            onDeleteClick = { productToDelete = it },
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddDialogOpen || productBeingEdited != null) {
        val categoryCounts = remember(unfilteredProducts) {
            unfilteredProducts.groupingBy { it.category }.eachCount()
        }
        val suggestedCodeNumber = remember(unfilteredProducts) {
            val maxExisting = unfilteredProducts
                .mapNotNull { it.code.trim().removePrefix("#").toIntOrNull() }
                .maxOrNull() ?: 1000
            (maxExisting + 1).coerceAtLeast(1001)
        }
        val barcodeScannerComingSoonMsg = stringResource(R.string.barcode_scanner_coming_soon)
        AddEditProductDialog(
            productToEdit = productBeingEdited,
            currencySymbol = currencySymbol,
            availableCategories = categories,
            totalProductsCount = suggestedCodeNumber - 1001,
            categoryItemCounts = categoryCounts,
            onScanBarcodeClick = {
                viewModel.emitToastMessage(barcodeScannerComingSoonMsg)
            },
            onCategoryCreated = { newCategory ->
                viewModel.addCategory(newCategory)
            },
            onDismiss = {
                isAddDialogOpen = false
                productBeingEdited = null
            },
            onSave = { id, code, name, category, purchasePriceMinor, salesPriceMinor, quantity, unit, lowStockThreshold, onError ->
                viewModel.saveProduct(
                    id = id,
                    code = code,
                    name = name,
                    category = category,
                    purchasePriceMinor = purchasePriceMinor,
                    salesPriceMinor = salesPriceMinor,
                    quantity = quantity,
                    unit = unit,
                    lowStockThreshold = lowStockThreshold,
                    onSuccess = {
                        isAddDialogOpen = false
                        productBeingEdited = null
                    },
                    onError = onError
                )
            }
        )
    }

    // Delete Confirmation Dialog
    productToDelete?.let { prod ->
        ShopConfirmationDialog(
            onDismissRequest = { productToDelete = null },
            title = stringResource(R.string.action_delete) + "?",
            subtitle = stringResource(R.string.staff_remove_confirm, prod.name),
            icon = Icons.Default.Delete,
            iconTint = MaterialTheme.colorScheme.error,
            iconContainerColor = MaterialTheme.colorScheme.errorContainer,
            confirmButtonText = stringResource(R.string.action_delete),
            confirmButtonColor = MaterialTheme.colorScheme.error,
            confirmButtonContentColor = MaterialTheme.colorScheme.onError,
            onConfirm = {
                viewModel.deleteProduct(prod)
                productToDelete = null
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = prod.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    val displayCode = formatProductCode(prod.code)
                    Text(
                        text = "${stringResource(R.string.code_prefix, displayCode)} • ${prod.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ProductTopBarContent(
    totalCount: Int,
    filteredCount: Int,
    isFiltered: Boolean,
    syncStatus: SyncStatus,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onRefreshSync: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val stockSubtitle = if (isFiltered) {
                    stringResource(R.string.products_total_count, filteredCount)
                } else {
                    stringResource(R.string.products_total_count, totalCount)
                }
                Text(
                    text = stockSubtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                IconButton(onClick = onRefreshSync) {
                    SyncStatusIndicator(status = syncStatus)
                }

                IconButton(onClick = onNavigateToCategories) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = stringResource(R.string.products_view_categories),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.products_settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Search Bar & Category Filter Strip (Animated Transition)
        AnimatedVisibility(
            visible = totalCount > 0,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                // Instant Search Bar
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = onSearchQueryChange
                )

                // Category Filter Chips
                CategoryChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = onCategorySelected,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Preview(name = "TopBar: Empty Inventory (0 Products)", widthDp = 360, showBackground = true)
@Composable
fun ProductTopBarEmptyPreview() {
    ShopKeeperTheme {
        ProductTopBarContent(
            totalCount = 0,
            filteredCount = 0,
            isFiltered = false,
            syncStatus = SyncStatus.Idle,
            searchQuery = "",
            onSearchQueryChange = {},
            categories = emptyList(),
            selectedCategory = "All",
            onCategorySelected = {},
            onRefreshSync = {},
            onNavigateToCategories = {},
            onNavigateToSettings = {}
        )
    }
}

@Preview(name = "TopBar: With Products (Populated)", widthDp = 360, showBackground = true)
@Composable
fun ProductTopBarWithProductsPreview() {
    ShopKeeperTheme {
        ProductTopBarContent(
            totalCount = 12,
            filteredCount = 12,
            isFiltered = false,
            syncStatus = SyncStatus.Idle,
            searchQuery = "",
            onSearchQueryChange = {},
            categories = listOf("All", "Groceries", "Electronics", "Clothing"),
            selectedCategory = "All",
            onCategorySelected = {},
            onRefreshSync = {},
            onNavigateToCategories = {},
            onNavigateToSettings = {}
        )
    }
}
