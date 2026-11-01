package com.shopkeeper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkeeper.R
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.screens.components.ShopTextField
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.RedLight
import com.shopkeeper.ui.util.formatInputNumber
import com.shopkeeper.ui.util.formatMoney
import com.shopkeeper.ui.util.parseToMinorUnits
import java.util.Locale

@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    currencySymbol: String,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (
        id: String?,
        code: String,
        name: String,
        category: String,
        purchasePriceMinor: Long,
        salesPriceMinor: Long,
        quantity: Int,
        onError: (String) -> Unit
    ) -> Unit
) {
    val isEditing = productToEdit != null

    var code by remember { mutableStateOf(productToEdit?.code ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "General") }
    var purchasePriceStr by remember {
        mutableStateOf(if (productToEdit != null) formatInputNumber(productToEdit.purchasePriceMinor) else "")
    }
    var salesPriceStr by remember {
        mutableStateOf(if (productToEdit != null) formatInputNumber(productToEdit.salesPriceMinor) else "")
    }
    var quantityStr by remember {
        mutableStateOf(if (productToEdit != null) productToEdit.quantity.toString() else "1")
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var salesPriceError by remember { mutableStateOf<String?>(null) }
    var purchasePriceError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var formErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    val purchasePriceMinor = parseToMinorUnits(purchasePriceStr) ?: 0L
    val salesPriceMinor = parseToMinorUnits(salesPriceStr) ?: 0L
    val profitMinor = salesPriceMinor - purchasePriceMinor
    val markupPercent = when {
        purchasePriceMinor > 0L -> ((profitMinor.toDouble()) / purchasePriceMinor.toDouble()) * 100.0
        salesPriceMinor > 0L -> 100.0
        else -> 0.0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .imePadding()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) {
                            stringResource(R.string.dialog_edit_title)
                        } else {
                            stringResource(R.string.dialog_add_title)
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // In-Dialog Error Banner
                if (formErrorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = formErrorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Product Name
                ShopTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError != null) {
                            nameError = null
                            formErrorMessage = null
                        }
                    },
                    label = stringResource(R.string.product_name_label) + " *",
                    placeholder = "e.g. Miniket Rice 5kg",
                    isError = nameError != null,
                    errorMessage = nameError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = if (nameError != null) RedAlert else MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Product Code / ID
                ShopTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        if (codeError != null) {
                            codeError = null
                            formErrorMessage = null
                        }
                    },
                    label = stringResource(R.string.product_code_label) + " *",
                    placeholder = "e.g. P-101, BAR-102",
                    isError = codeError != null,
                    errorMessage = codeError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = if (codeError != null) RedAlert else MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Category Selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    ShopTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            formErrorMessage = null
                        },
                        label = stringResource(R.string.product_category_label) + " *",
                        placeholder = "e.g. Rice, Grocery",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "▾",
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clickable { categoryDropdownExpanded = true }
                                    .padding(8.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    val filteredSuggestions = availableCategories.filter {
                        it != "All" && it.contains(category, ignoreCase = true)
                    }

                    if (categoryDropdownExpanded && filteredSuggestions.isNotEmpty()) {
                        DropdownMenu(
                            expanded = categoryDropdownExpanded,
                            onDismissRequest = { categoryDropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            filteredSuggestions.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, color = MaterialTheme.colorScheme.onSurface) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Prices
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShopTextField(
                        value = purchasePriceStr,
                        onValueChange = {
                            purchasePriceStr = it
                            if (purchasePriceError != null) purchasePriceError = null
                        },
                        label = stringResource(R.string.buying_price_label) + " *",
                        placeholder = "0.00",
                        isError = purchasePriceError != null,
                        errorMessage = purchasePriceError,
                        leadingIcon = {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    ShopTextField(
                        value = salesPriceStr,
                        onValueChange = {
                            salesPriceStr = it
                            if (salesPriceError != null) salesPriceError = null
                        },
                        label = stringResource(R.string.selling_price_label) + " *",
                        placeholder = "0.00",
                        isError = salesPriceError != null,
                        errorMessage = salesPriceError,
                        leadingIcon = {
                            Text(
                                text = currencySymbol,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Quantity
                ShopTextField(
                    value = quantityStr,
                    onValueChange = {
                        quantityStr = it
                        if (quantityError != null) quantityError = null
                    },
                    label = stringResource(R.string.quantity_label) + " *",
                    placeholder = "1",
                    isError = quantityError != null,
                    errorMessage = quantityError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Profit Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = "Margin Info",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Estimated Profit & Markup",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        val profitSign = if (profitMinor >= 0) "+" else "-"
                        val profitVal = formatMoney(kotlin.math.abs(profitMinor))
                        val markupSign = if (markupPercent > 0) "+" else ""
                        Text(
                            text = "$profitSign$currencySymbol$profitVal ($markupSign${String.format(Locale.US, "%.1f", markupPercent)}% markup)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                profitMinor > 0 -> MaterialTheme.colorScheme.secondary
                                profitMinor < 0 -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.action_cancel),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedCode = code.trim().removePrefix("#")
                            val parsedSalesPriceMinor = parseToMinorUnits(salesPriceStr)
                            val parsedPurchasePriceMinor = parseToMinorUnits(purchasePriceStr)
                            val parsedQty = quantityStr.trim().toIntOrNull()

                            var hasError = false

                            if (trimmedName.isEmpty()) {
                                nameError = "Product name is required"
                                hasError = true
                            } else {
                                nameError = null
                            }

                            if (trimmedCode.isEmpty()) {
                                codeError = "Product code / ID is required"
                                hasError = true
                            } else {
                                codeError = null
                            }

                            if (salesPriceStr.isBlank()) {
                                salesPriceError = "Selling price is required (enter 0 if free)"
                                hasError = true
                            } else if (parsedSalesPriceMinor == null) {
                                salesPriceError = "Invalid price"
                                hasError = true
                            } else if (parsedSalesPriceMinor < 0L) {
                                salesPriceError = "Cannot be negative"
                                hasError = true
                            } else if (parsedSalesPriceMinor > 100_000_000_000L) {
                                salesPriceError = "Price is too large (max 1,000,000,000)"
                                hasError = true
                            } else {
                                salesPriceError = null
                            }

                            if (purchasePriceStr.isBlank()) {
                                purchasePriceError = "Buying price is required (enter 0 if free)"
                                hasError = true
                            } else if (parsedPurchasePriceMinor == null) {
                                purchasePriceError = "Invalid price"
                                hasError = true
                            } else if (parsedPurchasePriceMinor < 0L) {
                                purchasePriceError = "Cannot be negative"
                                hasError = true
                            } else if (parsedPurchasePriceMinor > 100_000_000_000L) {
                                purchasePriceError = "Price is too large (max 1,000,000,000)"
                                hasError = true
                            } else {
                                purchasePriceError = null
                            }

                            if (quantityStr.isBlank()) {
                                quantityError = "Stock quantity is required"
                                hasError = true
                            } else if (parsedQty == null) {
                                quantityError = "Invalid quantity"
                                hasError = true
                            } else if (parsedQty < 0) {
                                quantityError = "Cannot be negative"
                                hasError = true
                            } else if (parsedQty > 1_000_000) {
                                quantityError = "Quantity is too large (max 1,000,000)"
                                hasError = true
                            } else {
                                quantityError = null
                            }

                            if (hasError) {
                                formErrorMessage = "Please fix the highlighted fields below."
                                return@Button
                            }

                            isSubmitting = true
                            formErrorMessage = null

                            onSave(
                                productToEdit?.id,
                                trimmedCode,
                                trimmedName,
                                category.trim().ifEmpty { "General" },
                                parsedPurchasePriceMinor ?: 0L,
                                parsedSalesPriceMinor ?: 0L,
                                parsedQty ?: 0
                            ) { errorMsg ->
                                isSubmitting = false
                                formErrorMessage = errorMsg
                            }
                        },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary,
                            disabledContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isEditing) {
                                    stringResource(R.string.action_save)
                                } else {
                                    stringResource(R.string.action_add)
                                },
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
