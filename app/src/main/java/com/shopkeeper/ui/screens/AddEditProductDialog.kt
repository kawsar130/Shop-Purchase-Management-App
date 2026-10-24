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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.screens.components.ShopTextField
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.GreenLight
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.RedLight
import com.shopkeeper.ui.theme.Slate100
import com.shopkeeper.ui.theme.Slate200
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate700
import com.shopkeeper.ui.theme.Slate900
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
        purchasePrice: Double,
        salesPrice: Double,
        quantity: Int,
        onError: (String) -> Unit
    ) -> Unit
) {
    val isEditing = productToEdit != null

    var code by remember { mutableStateOf(productToEdit?.code ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "General") }
    var purchasePriceStr by remember {
        mutableStateOf(if (productToEdit != null) formatNum(productToEdit.purchasePrice) else "")
    }
    var salesPriceStr by remember {
        mutableStateOf(if (productToEdit != null) formatNum(productToEdit.salesPrice) else "")
    }
    var quantityStr by remember {
        mutableStateOf(if (productToEdit != null) productToEdit.quantity.toString() else "1")
    }

    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var salesPriceError by remember { mutableStateOf<String?>(null) }
    var purchasePriceError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var formErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Real-time calculated profit for live feedback
    val purchasePrice = purchasePriceStr.toDoubleOrNull() ?: 0.0
    val salesPrice = salesPriceStr.toDoubleOrNull() ?: 0.0
    val profit = salesPrice - purchasePrice
    val profitPercent = if (purchasePrice > 0.0) (profit / purchasePrice) * 100.0 else 0.0

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
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            border = BorderStroke(1.dp, Slate200),
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
                        text = if (isEditing) "Edit Product" else "Add New Item",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate500,
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
                            .background(RedLight)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = RedAlert,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = formErrorMessage!!,
                                color = RedAlert,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Product Name (Primary identifier for shopkeeper)
                ShopTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (nameError != null) {
                            nameError = null
                            formErrorMessage = null
                        }
                    },
                    label = "Product Name *",
                    placeholder = "e.g. Miniket Rice 5kg",
                    isError = nameError != null,
                    errorMessage = nameError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = null,
                            tint = if (nameError != null) RedAlert else BluePrimary
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
                    label = "Product Code / ID *",
                    placeholder = "e.g. P-101, BAR-102",
                    isError = codeError != null,
                    errorMessage = codeError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            tint = if (codeError != null) RedAlert else BluePrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Category with suggestions dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ShopTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            formErrorMessage = null
                        },
                        label = "Category",
                        placeholder = "e.g. Groceries, Electronics",
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = BluePrimary)
                        },
                        trailingIcon = {
                            Text(
                                text = "Choose ▼",
                                color = BluePrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
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

                    DropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        val filterCategories = availableCategories.filter { it != "All" && it.isNotBlank() }
                        if (filterCategories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("No existing categories yet") },
                                onClick = { categoryDropdownExpanded = false }
                            )
                        } else {
                            filterCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontWeight = FontWeight.SemiBold) },
                                    onClick = {
                                        category = cat
                                        categoryDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Two Big Price Fields: Buying Price & Selling Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ShopTextField(
                            value = purchasePriceStr,
                            onValueChange = {
                                purchasePriceStr = it
                                if (purchasePriceError != null) {
                                    purchasePriceError = null
                                    formErrorMessage = null
                                }
                            },
                            label = "Buying Price ($currencySymbol) *",
                            placeholder = "0.00",
                            isError = purchasePriceError != null,
                            errorMessage = purchasePriceError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        ShopTextField(
                            value = salesPriceStr,
                            onValueChange = {
                                salesPriceStr = it
                                if (salesPriceError != null) {
                                    salesPriceError = null
                                    formErrorMessage = null
                                }
                            },
                            label = "Selling Price ($currencySymbol) *",
                            placeholder = "0.00",
                            isError = salesPriceError != null,
                            errorMessage = salesPriceError,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 5. Quantity In Stock
                ShopTextField(
                    value = quantityStr,
                    onValueChange = {
                        quantityStr = it
                        if (quantityError != null) {
                            quantityError = null
                            formErrorMessage = null
                        }
                    },
                    label = "Stock Quantity (Units) *",
                    placeholder = "1",
                    isError = quantityError != null,
                    errorMessage = quantityError,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Numbers,
                            contentDescription = null,
                            tint = if (quantityError != null) RedAlert else BluePrimary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Informative Profit Margin Metric Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Slate100)
                        .border(1.dp, Slate200, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = "Margin Info",
                                tint = BluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Estimated Profit Margin",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate700
                            )
                        }
                        Text(
                            text = "${if (profit >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%.2f", profit)} (${String.format(Locale.US, "%.1f", profitPercent)}%)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) GreenSuccess else Slate700
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
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Cancel", color = Slate700, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            val trimmedCode = code.trim()
                            val parsedSalesPrice = salesPriceStr.trim().toDoubleOrNull()
                            val parsedPurchasePrice = purchasePriceStr.trim().toDoubleOrNull()
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

                            if (salesPriceStr.isNotBlank() && parsedSalesPrice == null) {
                                salesPriceError = "Invalid price"
                                hasError = true
                            } else if (parsedSalesPrice != null && parsedSalesPrice < 0) {
                                salesPriceError = "Cannot be negative"
                                hasError = true
                            } else {
                                salesPriceError = null
                            }

                            if (purchasePriceStr.isNotBlank() && parsedPurchasePrice == null) {
                                purchasePriceError = "Invalid price"
                                hasError = true
                            } else if (parsedPurchasePrice != null && parsedPurchasePrice < 0) {
                                purchasePriceError = "Cannot be negative"
                                hasError = true
                            } else {
                                purchasePriceError = null
                            }

                            if (quantityStr.isNotBlank() && parsedQty == null) {
                                quantityError = "Invalid quantity"
                                hasError = true
                            } else if (parsedQty != null && parsedQty < 0) {
                                quantityError = "Cannot be negative"
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
                                parsedPurchasePrice ?: 0.0,
                                parsedSalesPrice ?: 0.0,
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
                            containerColor = BluePrimary,
                            contentColor = PureWhite,
                            disabledContainerColor = BluePrimary,
                            disabledContentColor = PureWhite
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = if (isEditing) "Save Changes" else "Add Item to Shop",
                                color = PureWhite,
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

private fun formatNum(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", amount)
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
}
