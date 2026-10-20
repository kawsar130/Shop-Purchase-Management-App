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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Money
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
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.ui.theme.BluePrimary
import com.shop.purchasemanagement.ui.theme.GreenLight
import com.shop.purchasemanagement.ui.theme.GreenSuccess
import com.shop.purchasemanagement.ui.theme.PureWhite
import com.shop.purchasemanagement.ui.theme.RedAlert
import com.shop.purchasemanagement.ui.theme.RedLight
import com.shop.purchasemanagement.ui.theme.Slate100
import com.shop.purchasemanagement.ui.theme.Slate500
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
        mutableStateOf(if (productToEdit != null) productToEdit.purchasePrice.toString() else "")
    }
    var salesPriceStr by remember {
        mutableStateOf(if (productToEdit != null) productToEdit.salesPrice.toString() else "")
    }
    var quantityStr by remember {
        mutableStateOf(if (productToEdit != null) productToEdit.quantity.toString() else "1")
    }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }

    // Calculated values for live preview
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
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Product" else "Add New Product",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Product Code / ID
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Product Code / ID *") },
                    placeholder = { Text("e.g. P-101, BAR-102") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.QrCode, contentDescription = null, tint = BluePrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Product Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name *") },
                    placeholder = { Text("e.g. Miniket Rice 5kg") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Inventory, contentDescription = null, tint = BluePrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category Selection with Dropdown & custom input
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        placeholder = { Text("e.g. Groceries, Electronics") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = BluePrimary)
                        },
                        trailingIcon = {
                            Text(
                                text = "Suggestions ▼",
                                color = BluePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { categoryDropdownExpanded = true }
                                    .padding(8.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
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
                                    text = { Text(cat) },
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

                // Pricing Row: Purchase Price & Sales Price
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = purchasePriceStr,
                        onValueChange = { purchasePriceStr = it },
                        label = { Text("Purchase ($currencySymbol) *") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    OutlinedTextField(
                        value = salesPriceStr,
                        onValueChange = { salesPriceStr = it },
                        label = { Text("Sales ($currencySymbol) *") },
                        placeholder = { Text("0.00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quantity Field
                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantity in Stock *") },
                    placeholder = { Text("1") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Numbers, contentDescription = null, tint = BluePrimary)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Profit Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (profit >= 0) GreenLight else RedLight)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Estimated Profit Margin:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (profit >= 0) GreenSuccess else RedAlert
                        )
                        Text(
                            text = "${if (profit >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%.2f", profit)} (${String.format(Locale.US, "%.1f", profitPercent)}%)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) GreenSuccess else RedAlert
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Cancel")
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
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text(
                            text = if (isEditing) "Save Changes" else "Add Product",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
