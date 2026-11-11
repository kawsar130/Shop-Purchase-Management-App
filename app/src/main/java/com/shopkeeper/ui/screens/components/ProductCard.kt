package com.shopkeeper.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.shopkeeper.ui.theme.ShopKeeperTheme
import com.shopkeeper.R
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.theme.AmberContainerDark
import com.shopkeeper.ui.theme.AmberLight
import com.shopkeeper.ui.theme.AmberWarning
import com.shopkeeper.ui.theme.AmberWarningDark
import com.shopkeeper.ui.theme.GreenContainerDark
import com.shopkeeper.ui.theme.GreenLight
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.GreenSuccessDark
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.RedAlertDark
import com.shopkeeper.ui.theme.RedContainerDark
import com.shopkeeper.ui.theme.RedLight
import com.shopkeeper.ui.util.formatMoney
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductCard(
    product: Product,
    currencySymbol: String,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Category Badge + Spec Badge + Edit / Delete Actions
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = product.category,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (product.code.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = product.code,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditClick(product) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.action_edit),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = { onDeleteClick(product) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = RedAlert,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Product Title (Full Card Width, Independent)
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 23.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Price Tag Box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // SELLING PRICE
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.selling_price_uppercase),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol${formatMoney(product.salesPriceMinor)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .width(1.dp)
                            .height(42.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    // BUYING PRICE
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.buying_price_uppercase),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$currencySymbol${formatMoney(product.purchasePriceMinor)}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stock Status & Profit Badges with Units
            val isDark = isSystemInDarkTheme()
            val isLow = product.isLowStock
            val isOut = product.quantity <= 0
            val stockBg = when {
                isOut -> if (isDark) RedContainerDark else RedLight
                isLow -> if (isDark) AmberContainerDark else AmberLight
                else -> if (isDark) GreenContainerDark else GreenLight
            }
            val stockTextColor = when {
                isOut -> if (isDark) RedAlertDark else RedAlert
                isLow -> if (isDark) AmberWarningDark else AmberWarning
                else -> if (isDark) GreenSuccessDark else GreenSuccess
            }
            val stockText = when {
                isOut -> stringResource(R.string.stock_out_of_stock)
                isLow -> stringResource(R.string.stock_low_warning, product.quantity, product.unit)
                else -> stringResource(R.string.stock_in_stock, product.quantity, product.unit)
            }

            val profitMinor = product.profitMinor
            val markup = product.markupPercentage

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stockBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stockText,
                        color = stockTextColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                val profitBg = when {
                    profitMinor > 0 -> if (isDark) GreenContainerDark else GreenLight
                    profitMinor < 0 -> if (isDark) RedContainerDark else RedLight
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
                val profitTextColor = when {
                    profitMinor > 0 -> if (isDark) GreenSuccessDark else GreenSuccess
                    profitMinor < 0 -> if (isDark) RedAlertDark else RedAlert
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val profitSign = when {
                    profitMinor > 0 -> "+"
                    profitMinor < 0 -> "-"
                    else -> ""
                }
                val profitVal = formatMoney(kotlin.math.abs(profitMinor))
                val markupSign = if (markup > 0) "+" else ""
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(profitBg)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.product_profit_format,
                            profitSign,
                            currencySymbol,
                            profitVal,
                            markupSign,
                            String.format(Locale.US, "%.1f", markup)
                        ),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = profitTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Preview(name = "Product Card: In Stock with Spec Badge", widthDp = 360, showBackground = true)
@Composable
fun ProductCardInStockPreview() {
    ShopKeeperTheme {
        ProductCard(
            product = Product(
                id = "1",
                code = "400MFD",
                name = "ABS Motor Capacitor",
                category = "Electrical",
                purchasePriceMinor = 12000L,
                salesPriceMinor = 18000L,
                quantity = 45,
                unit = "pcs",
                lowStockThreshold = 10
            ),
            currencySymbol = "$",
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(name = "Product Card: Low Stock Alert", widthDp = 360, showBackground = true)
@Composable
fun ProductCardLowStockPreview() {
    ShopKeeperTheme {
        ProductCard(
            product = Product(
                id = "2",
                code = "15W",
                name = "Philips LED Bulb",
                category = "Electronics",
                purchasePriceMinor = 45000L,
                salesPriceMinor = 60000L,
                quantity = 3,
                unit = "pcs",
                lowStockThreshold = 5
            ),
            currencySymbol = "$",
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}

@Preview(name = "Product Card: Out of Stock", widthDp = 360, showBackground = true)
@Composable
fun ProductCardOutOfStockPreview() {
    ShopKeeperTheme {
        ProductCard(
            product = Product(
                id = "3",
                code = "MINIKET",
                name = "Premium Miniket Rice 25kg",
                category = "Groceries",
                purchasePriceMinor = 220000L,
                salesPriceMinor = 260000L,
                quantity = 0,
                unit = "kg",
                lowStockThreshold = 5
            ),
            currencySymbol = "$",
            onEditClick = {},
            onDeleteClick = {}
        )
    }
}
