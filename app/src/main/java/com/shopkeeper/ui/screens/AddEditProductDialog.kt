package com.shopkeeper.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.shopkeeper.ui.screens.components.CategoryPickerBottomSheet
import com.shopkeeper.ui.screens.components.LowStockAlertRow
import com.shopkeeper.ui.screens.components.QuantityUnitRow
import com.shopkeeper.ui.screens.components.ShopTextField
import com.shopkeeper.ui.screens.components.SpecCodeInputRow
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.util.formatInputNumber
import com.shopkeeper.ui.util.formatMoney
import com.shopkeeper.ui.util.parseToMinorUnits
import java.util.Locale

/**
 * Sprint 4.5: Frictionless Product Creation & Editing Dialog.
 *
 * Implements forgiving validation:
 * - Code is optional (auto-generates #1001 if left empty).
 * - Buying price is optional (defaults to $0.00).
 * - Category defaults smoothly to "General".
 * - Only Product Name and Selling Price are strictly required.
 * - Dynamic Live Profit & Loss Preview warns in red if selling below cost.
 */
@Composable
fun AddEditProductDialog(
    productToEdit: Product?,
    currencySymbol: String,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    totalProductsCount: Int = 0,
    categoryItemCounts: Map<String, Int> = emptyMap(),
    onScanBarcodeClick: (() -> Unit)? = null,
    onSave: (
        id: String?,
        code: String,
        name: String,
        category: String,
        purchasePriceMinor: Long,
        salesPriceMinor: Long,
        quantity: Int,
        unit: String,
        lowStockThreshold: Int,
        onError: (String) -> Unit
    ) -> Unit
) {
    val isEditing = productToEdit != null
    val autoCodeFallback = remember(totalProductsCount) {
        (1001 + totalProductsCount).toString()
    }

    var code by remember { mutableStateOf(productToEdit?.code ?: "") }
    var name by remember { mutableStateOf(productToEdit?.name ?: "") }
    var category by remember { mutableStateOf(productToEdit?.category ?: "General") }
    var purchasePriceStr by remember {
        mutableStateOf(if (productToEdit != null && productToEdit.purchasePriceMinor > 0L) formatInputNumber(productToEdit.purchasePriceMinor) else "")
    }
    var salesPriceStr by remember {
        mutableStateOf(if (productToEdit != null) formatInputNumber(productToEdit.salesPriceMinor) else "")
    }
    var quantityStr by remember {
        mutableStateOf(if (productToEdit != null) productToEdit.quantity.toString() else "1")
    }
    var unit by remember { mutableStateOf(productToEdit?.unit ?: "pcs") }
    var lowStockThreshold by remember {
        mutableIntStateOf(if (productToEdit != null && productToEdit.lowStockThreshold > 0) productToEdit.lowStockThreshold else 5)
    }
    var isLowStockAlertEnabled by remember {
        mutableStateOf((productToEdit?.lowStockThreshold ?: 0) > 0)
    }

    var nameError by remember { mutableStateOf<String?>(null) }
    var salesPriceError by remember { mutableStateOf<String?>(null) }
    var purchasePriceError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var formErrorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val purchasePriceMinor = parseToMinorUnits(purchasePriceStr) ?: 0L
    val salesPriceMinor = parseToMinorUnits(salesPriceStr) ?: 0L
    val profitMinor = salesPriceMinor - purchasePriceMinor
    val markupPercent = when {
        purchasePriceMinor > 0L -> ((profitMinor.toDouble()) / purchasePriceMinor.toDouble()) * 100.0
        salesPriceMinor > 0L -> 100.0
        else -> 0.0
    }

    val nameRequiredErrorMsg = stringResource(R.string.name_required_error)
    val sellingPriceRequiredErrorMsg = stringResource(R.string.selling_price_required_error)
    val invalidPriceErrorMsg = stringResource(R.string.invalid_price_error)
    val priceNegativeErrorMsg = stringResource(R.string.price_negative_error)
    val priceTooLargeErrorMsg = stringResource(R.string.price_too_large_error)
    val quantityInvalidErrorMsg = stringResource(R.string.quantity_invalid_error)
    val quantityNegativeErrorMsg = stringResource(R.string.quantity_negative_error)
    val quantityTooLargeErrorMsg = stringResource(R.string.quantity_too_large_error)
    val formFixErrorsMsg = stringResource(R.string.form_fix_errors_message)

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
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                                contentDescription = null,
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

                // 1. Product Name (Strictly Required)
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
                    placeholder = stringResource(R.string.product_name_placeholder),
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

                // 2. Spec & Code Input (Progressive Disclosure - Optional)
                SpecCodeInputRow(
                    code = code,
                    onCodeChange = {
                        code = it
                        formErrorMessage = null
                    },
                    autoGeneratedPlaceholder = autoCodeFallback,
                    onScanBarcodeClick = {
                        onScanBarcodeClick?.invoke()
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Category Selector (2-Tier Quick Chips + Search Sheet - Defaults to General)
                CategoryPickerBottomSheet(
                    selectedCategory = category,
                    availableCategories = availableCategories,
                    onCategorySelected = { category = it },
                    categoryItemCounts = categoryItemCounts
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Pricing Row (Buying Price Optional, Selling Price Required)
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
                        label = stringResource(R.string.buying_price_optional_label),
                        placeholder = stringResource(R.string.buying_price_optional_hint),
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
                        placeholder = stringResource(R.string.price_zero_placeholder),
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

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Quantity Stepper & Unit Selector
                QuantityUnitRow(
                    quantityStr = quantityStr,
                    onQuantityChange = {
                        quantityStr = it
                        if (quantityError != null) quantityError = null
                    },
                    selectedUnit = unit,
                    onUnitSelected = { unit = it },
                    errorMessage = quantityError
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 6. Low Stock Alert Toggle & Threshold Stepper
                LowStockAlertRow(
                    enabled = isLowStockAlertEnabled,
                    onEnabledChange = { isLowStockAlertEnabled = it },
                    threshold = lowStockThreshold,
                    onThresholdChange = { lowStockThreshold = it },
                    unit = unit
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 7. Live Profit & Loss Preview Box
                val isLoss = profitMinor < 0L
                val isBreakEven = profitMinor == 0L && salesPriceMinor > 0L
                val containerColor = if (isLoss) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
                val borderColor = if (isLoss) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(containerColor)
                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 11.dp)
                ) {
                    if (isLoss) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.profit_loss_alert),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "-$currencySymbol${formatMoney(kotlin.math.abs(profitMinor))}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
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
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(R.string.profit_markup_label),
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
                                text = if (isBreakEven) {
                                    stringResource(R.string.profit_break_even_label)
                                } else {
                                    stringResource(
                                        R.string.profit_markup_format,
                                        profitSign,
                                        currencySymbol,
                                        profitVal,
                                        markupSign,
                                        String.format(Locale.US, "%.1f", markupPercent)
                                    )
                                },
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    profitMinor > 0 -> MaterialTheme.colorScheme.secondary
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                            val parsedSalesPriceMinor = parseToMinorUnits(salesPriceStr)
                            val parsedPurchasePriceMinor = if (purchasePriceStr.isBlank()) 0L else parseToMinorUnits(purchasePriceStr)
                            val parsedQty = quantityStr.trim().toIntOrNull()

                            var hasError = false

                            // 1. Name is strictly required
                            if (trimmedName.isEmpty()) {
                                nameError = nameRequiredErrorMsg
                                hasError = true
                            } else {
                                nameError = null
                            }

                            // 2. Code is optional: fallback to auto-generated #1001 if left blank
                            val finalCode = if (code.trim().isEmpty()) {
                                "#$autoCodeFallback"
                            } else {
                                code.trim()
                            }

                            // 3. Selling price is strictly required
                            if (salesPriceStr.isBlank()) {
                                salesPriceError = sellingPriceRequiredErrorMsg
                                hasError = true
                            } else if (parsedSalesPriceMinor == null) {
                                salesPriceError = invalidPriceErrorMsg
                                hasError = true
                            } else if (parsedSalesPriceMinor < 0L) {
                                salesPriceError = priceNegativeErrorMsg
                                hasError = true
                            } else if (parsedSalesPriceMinor > 100_000_000_000L) {
                                salesPriceError = priceTooLargeErrorMsg
                                hasError = true
                            } else {
                                salesPriceError = null
                            }

                            // 4. Buying price is optional (defaults to $0.00 if blank)
                            if (purchasePriceStr.isNotBlank()) {
                                if (parsedPurchasePriceMinor == null) {
                                    purchasePriceError = invalidPriceErrorMsg
                                    hasError = true
                                } else if (parsedPurchasePriceMinor < 0L) {
                                    purchasePriceError = priceNegativeErrorMsg
                                    hasError = true
                                } else if (parsedPurchasePriceMinor > 100_000_000_000L) {
                                    purchasePriceError = priceTooLargeErrorMsg
                                    hasError = true
                                } else {
                                    purchasePriceError = null
                                }
                            } else {
                                purchasePriceError = null
                            }

                            // 5. Quantity validation
                            if (quantityStr.isNotBlank() && parsedQty == null) {
                                quantityError = quantityInvalidErrorMsg
                                hasError = true
                            } else if (parsedQty != null && parsedQty < 0) {
                                quantityError = quantityNegativeErrorMsg
                                hasError = true
                            } else if (parsedQty != null && parsedQty > 1_000_000) {
                                quantityError = quantityTooLargeErrorMsg
                                hasError = true
                            } else {
                                quantityError = null
                            }

                            if (hasError) {
                                formErrorMessage = formFixErrorsMsg
                                return@Button
                            }

                            isSubmitting = true
                            formErrorMessage = null

                            val finalCategory = category.trim().ifEmpty { "General" }
                            val finalUnit = unit.ifBlank { "pcs" }
                            val finalThreshold = if (isLowStockAlertEnabled) lowStockThreshold.coerceAtLeast(1) else 0

                            onSave(
                                productToEdit?.id,
                                finalCode,
                                trimmedName,
                                finalCategory,
                                parsedPurchasePriceMinor ?: 0L,
                                parsedSalesPriceMinor ?: 0L,
                                parsedQty ?: 1,
                                finalUnit,
                                finalThreshold
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
