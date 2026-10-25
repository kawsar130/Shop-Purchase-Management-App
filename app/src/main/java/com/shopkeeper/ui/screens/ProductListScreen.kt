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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.screens.components.CategoryChips
import com.shopkeeper.ui.screens.components.ProductCard
import com.shopkeeper.ui.screens.components.SearchBarComponent
import com.shopkeeper.ui.screens.components.SyncStatusIndicator
import com.shopkeeper.ui.theme.BlueLight
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.RedLight
import com.shopkeeper.ui.theme.Slate100
import com.shopkeeper.ui.theme.Slate200
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate700
import com.shopkeeper.ui.theme.Slate900
import com.shopkeeper.ui.viewmodel.ProductViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProductListScreen(
    viewModel: ProductViewModel,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val products = uiState.products
    val stats = uiState.stats
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    var productBeingEdited by remember { mutableStateOf<Product?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        viewModel.toastMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = GreenSuccess,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = data.visuals.message,
                            color = PureWhite,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PureWhite)
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
                            text = "ShopKeeper",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Slate900
                        )
                        Text(
                            text = if (stats.totalCount == 1) "1 Product in Stock" else "${stats.totalCount} Products in Stock",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SyncStatusIndicator(
                            status = syncStatus
                        )

                        IconButton(onClick = onNavigateToCategories) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Category View",
                                tint = Slate700
                            )
                        }

                        IconButton(onClick = onNavigateToSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Slate700
                            )
                        }
                    }
                }

                // Instant Search Bar (Searches Name or Code)
                SearchBarComponent(
                    query = searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) }
                )

                // Category Filter Chips
                CategoryChips(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { viewModel.onCategorySelected(it) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                containerColor = BluePrimary,
                contentColor = PureWhite,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Product",
                    tint = PureWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Slate100)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = BluePrimary,
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = BluePrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Products in Shop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Slate900
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Your inventory is currently empty. Add your first product to get started!",
                                fontSize = 13.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { isAddDialogOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BluePrimary,
                                    contentColor = PureWhite
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = PureWhite)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Product", fontWeight = FontWeight.Bold, color = PureWhite)
                            }
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = Slate500,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No matching products found" else "No products in '$selectedCategory'",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Slate700
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty())
                                    "No products match '$searchQuery'. Try checking for typos or searching a different term."
                                else
                                    "There are no products in '$selectedCategory'. Tap the + button to add one.",
                                fontSize = 13.sp,
                                color = Slate500,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { viewModel.onSearchQueryChanged("") },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, Slate200),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Slate700
                                    )
                                ) {
                                    Text("Clear Search", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }
                            }
                        }
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
                        ProductCard(
                            product = product,
                            currencySymbol = currencySymbol,
                            onEditClick = { productBeingEdited = it },
                            onDeleteClick = { productToDelete = it }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
                    }
                }
            }
        }
    }

    // Add / Edit Dialog
    if (isAddDialogOpen || productBeingEdited != null) {
        AddEditProductDialog(
            productToEdit = productBeingEdited,
            currencySymbol = currencySymbol,
            availableCategories = categories,
            onDismiss = {
                isAddDialogOpen = false
                productBeingEdited = null
            },
            onSave = { id, code, name, category, purchasePrice, salesPrice, quantity, onError ->
                viewModel.saveProduct(
                    id = id,
                    code = code,
                    name = name,
                    category = category,
                    purchasePrice = purchasePrice,
                    salesPrice = salesPrice,
                    quantity = quantity,
                    onSuccess = {
                        isAddDialogOpen = false
                        productBeingEdited = null
                    },
                    onError = onError
                )
            }
        )
    }

    // Custom Delete Confirmation Dialog
    productToDelete?.let { prod ->
        Dialog(
            onDismissRequest = { productToDelete = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Delete Icon Badge
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RedLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = RedAlert,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Delete Product?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete this product? This will permanently remove it across all devices.",
                        fontSize = 13.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Product Preview Summary Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate100)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = prod.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Slate900,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Code: #${prod.code} • ${prod.category}",
                                fontSize = 12.sp,
                                color = Slate500
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action Buttons: Cancel and Delete
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { productToDelete = null },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate200),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = Slate700,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.deleteProduct(prod)
                                productToDelete = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAlert,
                                contentColor = PureWhite
                            )
                        ) {
                            Text(
                                text = "Delete",
                                color = PureWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
