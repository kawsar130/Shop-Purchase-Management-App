package com.shopkeeper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkeeper.R
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.screens.components.ShopDialog
import com.shopkeeper.ui.screens.components.ShopEmptyState
import com.shopkeeper.ui.screens.components.ShopTextField
import com.shopkeeper.ui.theme.ShopKeeperTheme
import com.shopkeeper.ui.util.formatMoney
import com.shopkeeper.ui.util.formatProductCode

/**
 * Enterprise-grade Category Management Screen providing:
 * - Real-time inventory valuations per category (Buying & Selling valuations)
 * - Standalone Category Creation (Add Category)
 * - Atomic Category Renaming (with auto-merge if target category exists)
 * - Safe Atomic Reassignment on Delete (guaranteeing zero orphaned products)
 * - Visibility of both populated and empty custom categories
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    products: List<Product>,
    categories: List<String>,
    currencySymbol: String,
    onCategoryClick: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onAddCategory: (String) -> Unit,
    onRenameCategory: (oldName: String, newName: String) -> Unit,
    onDeleteCategory: (categoryToDelete: String, reassignTo: String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Derive all distinct category names (combining active products and DataStore custom categories)
    val allCategoryNames = remember(categories, products) {
        val fromCategories = categories.filter { it.isNotBlank() && !it.equals("All", ignoreCase = true) }
        val fromProducts = products.map { it.category.trim() }.filter { it.isNotBlank() }
        val combined = (listOf("General") + fromCategories + fromProducts).distinctBy { it.lowercase() }
        val nonGeneral = combined.filterNot { it.equals("General", ignoreCase = true) }.sortedWith(String.CASE_INSENSITIVE_ORDER)
        listOf("General") + nonGeneral
    }

    // Group products under each category (empty categories receive an empty list)
    val grouped = remember(products, allCategoryNames) {
        val productMap = products.groupBy { it.category.ifBlank { "General" } }
        allCategoryNames.associateWith { catName ->
            productMap[catName] ?: productMap.entries.firstOrNull { it.key.equals(catName, ignoreCase = true) }?.value ?: emptyList()
        }
    }

    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    // Dialog state holders
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var categoryToRename by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryToDelete by rememberSaveable { mutableStateOf<String?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (allCategoryNames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ShopEmptyState(
                        icon = Icons.Default.Category,
                        title = stringResource(R.string.categories_empty),
                        subtitle = stringResource(R.string.categories_empty_subtitle),
                        actionText = stringResource(R.string.category_action_add),
                        actionIcon = Icons.Default.Add,
                        onActionClick = { showAddDialog = true }
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 360.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        val categoryCount = allCategoryNames.size
                        val productCount = products.size
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (categoryCount == 1) {
                                        stringResource(R.string.category_count_single)
                                    } else {
                                        stringResource(R.string.category_count_plural, categoryCount)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.products_total_count, productCount),
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            OutlinedButton(
                                onClick = { showAddDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.category_action_add),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    items(allCategoryNames, key = { it }) { categoryName ->
                        val categoryProducts = grouped[categoryName] ?: emptyList()
                        val count = categoryProducts.size
                        val totalPurchaseMinor = categoryProducts.sumOf { it.totalPurchaseValueMinor }
                        val totalSalesMinor = categoryProducts.sumOf { it.totalSalesValueMinor }
                        val isExpanded = expandedStates[categoryName] ?: false
                        val isGeneral = categoryName.equals("General", ignoreCase = true)

                        @OptIn(ExperimentalFoundationApi::class)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateItemPlacement(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Category Row Header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (count > 0) {
                                                expandedStates[categoryName] = !isExpanded
                                            }
                                        }
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Category,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = categoryName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${stringResource(R.string.category_item_count, count)} • ${stringResource(R.string.buying_price_prefix, "$currencySymbol${formatMoney(totalPurchaseMinor)}")}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "$currencySymbol${formatMoney(totalSalesMinor)}",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Action: Rename Category
                                        IconButton(
                                            onClick = { categoryToRename = categoryName },
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = stringResource(R.string.category_action_rename),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Action: Delete Category (strictly hidden for protected "General")
                                        if (!isGeneral) {
                                            IconButton(
                                                onClick = { categoryToDelete = categoryName },
                                                modifier = Modifier.size(40.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = stringResource(R.string.category_action_delete),
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }

                                        if (count > 0) {
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = stringResource(R.string.category_expand_content_description),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(start = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Expanded Product List under this Category
                                AnimatedVisibility(
                                    visible = isExpanded && count > 0,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                        categoryProducts.forEach { item ->
                                            val displayCode = formatProductCode(item.code)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onProductClick(item) }
                                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.name,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "$displayCode • ${stringResource(R.string.quantity_prefix, item.quantity)}",
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = stringResource(R.string.selling_price_prefix, "$currencySymbol${formatMoney(item.salesPriceMinor)}"),
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.secondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = stringResource(R.string.buying_price_prefix, "$currencySymbol${formatMoney(item.purchasePriceMinor)}"),
                                                        fontSize = 12.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = { onCategoryClick(categoryName) }) {
                                                Icon(
                                                    imageVector = Icons.Default.FilterList,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = stringResource(R.string.category_view_in_products),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // --- DIALOG 1: Add New Category ---
    if (showAddDialog) {
        var newCatInput by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val emptyErrorText = stringResource(R.string.category_name_empty_error)
        val existsErrorText = stringResource(R.string.category_name_exists_error)

        fun submitNewCategory() {
            val trimmed = newCatInput.trim()
            if (trimmed.isBlank()) {
                errorMessage = emptyErrorText
            } else if (allCategoryNames.any { it.equals(trimmed, ignoreCase = true) }) {
                errorMessage = existsErrorText
            } else {
                onAddCategory(trimmed)
                showAddDialog = false
            }
        }

        ShopDialog(onDismissRequest = { showAddDialog = false }) {
            Text(
                text = stringResource(R.string.category_dialog_add_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            ShopTextField(
                value = newCatInput,
                onValueChange = {
                    newCatInput = it
                    errorMessage = null
                },
                label = stringResource(R.string.category_dialog_add_label),
                placeholder = stringResource(R.string.category_dialog_add_placeholder),
                singleLine = true,
                isError = errorMessage != null,
                errorMessage = errorMessage,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitNewCategory() }
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { submitNewCategory() },
                    enabled = newCatInput.trim().isNotBlank(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(stringResource(R.string.action_add))
                }
            }
        }
    }

    // --- DIALOG 2: Rename / Merge Category ---
    categoryToRename?.let { currentOldName ->
        var renameInput by remember(currentOldName) { mutableStateOf(currentOldName) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        val emptyErrorText = stringResource(R.string.category_name_empty_error)
        val isTargetAlreadyExists = remember(renameInput, currentOldName, allCategoryNames) {
            val trimmed = renameInput.trim()
            trimmed.isNotBlank() &&
                !trimmed.equals(currentOldName, ignoreCase = true) &&
                allCategoryNames.any { it.equals(trimmed, ignoreCase = true) }
        }

        fun submitRenameCategory() {
            val trimmed = renameInput.trim()
            if (trimmed.isBlank()) {
                errorMessage = emptyErrorText
            } else if (trimmed.equals(currentOldName, ignoreCase = false)) {
                categoryToRename = null
            } else {
                onRenameCategory(currentOldName, trimmed)
                categoryToRename = null
            }
        }

        ShopDialog(onDismissRequest = { categoryToRename = null }) {
            Text(
                text = stringResource(R.string.category_dialog_rename_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            ShopTextField(
                value = renameInput,
                onValueChange = {
                    renameInput = it
                    errorMessage = null
                },
                label = stringResource(R.string.category_dialog_rename_label),
                placeholder = stringResource(R.string.category_dialog_rename_label),
                singleLine = true,
                isError = errorMessage != null,
                errorMessage = errorMessage,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { submitRenameCategory() }
                )
            )

            // Merge warning indicator if target category already exists
            if (isTargetAlreadyExists) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.category_dialog_rename_merge_warning, renameInput.trim()),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { categoryToRename = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { submitRenameCategory() },
                    enabled = renameInput.trim().isNotBlank() && !renameInput.trim().equals(currentOldName, ignoreCase = false),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isTargetAlreadyExists) stringResource(R.string.category_action_merge) else stringResource(R.string.action_save))
                }
            }
        }
    }

    // --- DIALOG 3: Safe Atomic Delete with Reassignment ---
    categoryToDelete?.let { deletingCat ->
        val productCount = grouped[deletingCat]?.size ?: 0
        val targetOptions = remember(deletingCat, allCategoryNames) {
            allCategoryNames.filterNot { it.equals(deletingCat, ignoreCase = true) }
        }
        var selectedTarget by remember { mutableStateOf("General") }
        var dropdownExpanded by remember { mutableStateOf(false) }

        ShopDialog(onDismissRequest = { categoryToDelete = null }) {
            Text(
                text = stringResource(R.string.category_dialog_delete_title),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (productCount == 0) {
                // Case A: 0 Products (Pure Deletion)
                Text(
                    text = stringResource(R.string.category_dialog_delete_empty_desc, deletingCat),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            } else {
                // Case B: Populated Products (Atomic Reassignment Required)
                Text(
                    text = stringResource(R.string.category_dialog_delete_with_products_desc, deletingCat, productCount),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.category_dialog_reassign_label),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedTarget,
                        onValueChange = {},
                        readOnly = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            disabledContainerColor = MaterialTheme.colorScheme.surface,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        targetOptions.forEach { targetOption ->
                            DropdownMenuItem(
                                text = { Text(targetOption) },
                                onClick = {
                                    selectedTarget = targetOption
                                    dropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onDeleteCategory(deletingCat, selectedTarget)
                        categoryToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (productCount > 0) {
                            stringResource(R.string.category_dialog_reassign_and_delete_confirm)
                        } else {
                            stringResource(R.string.category_action_delete)
                        }
                    )
                }
            }
        }
    }
}

@Preview(name = "Category Screen: Populated & Standalone Categories", widthDp = 380, heightDp = 800, showBackground = true)
@Composable
fun CategoryScreenPreview() {
    ShopKeeperTheme {
        CategoryScreen(
            products = listOf(
                Product(id = "1", code = "101", name = "Miniket Rice 5kg", category = "Groceries", purchasePriceMinor = 36000L, salesPriceMinor = 42000L, quantity = 25),
                Product(id = "2", code = "102", name = "Soybean Cooking Oil 1L", category = "Groceries", purchasePriceMinor = 17500L, salesPriceMinor = 19500L, quantity = 40),
                Product(id = "3", code = "103", name = "LED Bulb 15W", category = "Electronics", purchasePriceMinor = 12000L, salesPriceMinor = 16000L, quantity = 50),
                Product(id = "4", code = "104", name = "Heavy Duty Extension Cord 3M", category = "Electronics", purchasePriceMinor = 28000L, salesPriceMinor = 35000L, quantity = 15),
                Product(id = "5", code = "105", name = "Cotton Bath Towel", category = "Clothing", purchasePriceMinor = 25000L, salesPriceMinor = 32000L, quantity = 20)
            ),
            categories = listOf("General", "Clothing", "Electronics", "Groceries", "Mobile"),
            currencySymbol = "$",
            onCategoryClick = {},
            onProductClick = {},
            onAddCategory = {},
            onRenameCategory = { _, _ -> },
            onDeleteCategory = { _, _ -> }
        )
    }
}
