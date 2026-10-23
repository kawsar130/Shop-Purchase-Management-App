package com.shop.purchasemanagement.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.ui.screens.components.ShopTextField
import com.shop.purchasemanagement.ui.theme.BluePrimary
import com.shop.purchasemanagement.ui.theme.GreenLight
import com.shop.purchasemanagement.ui.theme.GreenSuccess
import com.shop.purchasemanagement.ui.theme.PureWhite
import com.shop.purchasemanagement.ui.theme.RedAlert
import com.shop.purchasemanagement.ui.theme.RedLight
import com.shop.purchasemanagement.ui.theme.Slate100
import com.shop.purchasemanagement.ui.theme.Slate500
import com.shop.purchasemanagement.ui.theme.Slate700
import com.shop.purchasemanagement.ui.theme.Slate900
import java.util.Locale

@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    currencySymbol: String,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (id: String?, code: String, name: String, category: String, purchasePrice: Double, salesPrice: Double, quantity: Int) -> Unit
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
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Product Name (Primary identifier for shopkeeper)
                ShopTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Product Name *",
                    placeholder = "e.g. Miniket Rice 5kg",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Inventory, contentDescription = null, tint = BluePrimary)
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
                    onValueChange = { code = it },
                    label = "Product Code / ID *",
                    placeholder = "e.g. P-101, BAR-102",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = BluePrimary)
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
                        onValueChange = { category = it },
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

                // 4. Two Big Price Fields: Selling Price & Cost Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ShopTextField(
                            value = salesPriceStr,
                            onValueChange = { salesPriceStr = it },
                            label = "Selling Price ($currencySymbol) *",
                            placeholder = "0.00",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal,
                                imeAction = ImeAction.Next
                            )
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        ShopTextField(
                            value = purchasePriceStr,
                            onValueChange = { purchasePriceStr = it },
                            label = "Cost Price ($currencySymbol) *",
                            placeholder = "0.00",
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
                    onValueChange = { quantityStr = it },
                    label = "Stock Quantity (Units) *",
                    placeholder = "1",
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Numbers, contentDescription = null, tint = BluePrimary)
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
                        .background(if (profit >= 0) GreenLight else RedLight)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Profit Margin:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) GreenSuccess else RedAlert
                        )
                        Text(
                            text = "${if (profit >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%.2f", profit)} (${String.format(Locale.US, "%.1f", profitPercent)}%)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (profit >= 0) GreenSuccess else RedAlert
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
                            val qty = quantityStr.toIntOrNull() ?: 0
                            onSave(
                                productToEdit?.id,
                                code,
                                name,
                                category,
                                purchasePrice,
                                salesPrice,
                                qty
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BluePrimary,
                            contentColor = PureWhite
                        )
                    ) {
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

private fun formatNum(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", amount)
    } else {
        String.format(Locale.US, "%.2f", amount)
    }
}
