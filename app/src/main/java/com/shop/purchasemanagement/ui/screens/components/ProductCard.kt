package com.shop.purchasemanagement.ui.screens.components

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.purchasemanagement.data.model.Product
import com.shop.purchasemanagement.ui.theme.AmberLight
import com.shop.purchasemanagement.ui.theme.AmberWarning
import com.shop.purchasemanagement.ui.theme.BlueLight
import com.shop.purchasemanagement.ui.theme.BluePrimary
import com.shop.purchasemanagement.ui.theme.GreenLight
import com.shop.purchasemanagement.ui.theme.GreenSuccess
import com.shop.purchasemanagement.ui.theme.PureWhite
import com.shop.purchasemanagement.ui.theme.RedAlert
import com.shop.purchasemanagement.ui.theme.RedLight
import com.shop.purchasemanagement.ui.theme.Slate100
import com.shop.purchasemanagement.ui.theme.Slate200
import com.shop.purchasemanagement.ui.theme.Slate500
import com.shop.purchasemanagement.ui.theme.Slate700
import com.shop.purchasemanagement.ui.theme.Slate900
import java.util.Locale

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
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onEditClick(product) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Row 1: Code Badge & Category Badge + Edit/Delete actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Product Code Badge (e.g. #P-101)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BlueLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "#${product.code}",
                            color = BluePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Slate100)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = product.category,
                            color = Slate700,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }

                // Action icons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onEditClick(product) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit product",
                            tint = Slate500,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { onDeleteClick(product) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete product",
                            tint = RedAlert,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Product Name
            Text(
                text = product.name,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Slate900,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Pricing Panel (Sales Price, Purchase Price, Profit)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate100)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sales Price (Big & Customer-facing)
                Column {
                    Text(
                        text = "Sales Price",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = "$currencySymbol${formatMoney(product.salesPrice)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GreenSuccess
                    )
                }

                // Purchase Price (Clearly visible as requested)
                Column {
                    Text(
                        text = "Purchase Price",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Text(
                        text = "$currencySymbol${formatMoney(product.purchasePrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate700
                    )
                }

                // Profit & Margin Badge
                val profit = product.profit
                val margin = product.profitMarginPercentage
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Profit Margin",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (profit >= 0) GreenLight else RedLight)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${if (profit >= 0) "+" else ""}$currencySymbol${formatMoney(profit)} (${String.format(Locale.US, "%.1f", margin)}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (profit >= 0) GreenSuccess else RedAlert
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 4: Stock Quantity
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stockBg = when {
                    product.quantity <= 0 -> RedLight
                    product.isLowStock -> AmberLight
                    else -> Slate100
                }
                val stockColor = when {
                    product.quantity <= 0 -> RedAlert
                    product.isLowStock -> AmberWarning
                    else -> Slate700
                }
                val stockLabel = when {
                    product.quantity <= 0 -> "Out of Stock"
                    product.isLowStock -> "Low Stock: ${product.quantity} left"
                    else -> "In Stock: ${product.quantity} units"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(stockBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stockLabel,
                        color = stockColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "Total Val: $currencySymbol${formatMoney(product.totalPurchaseValue)}",
                    color = Slate500,
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatMoney(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        String.format(Locale.US, "%,.0f", amount)
    } else {
        String.format(Locale.US, "%,.2f", amount)
    }
}
