package com.shopkeeper.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopkeeper.data.model.Product
import com.shopkeeper.ui.theme.BlueLight
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.GreenLight
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.PureWhite
import com.shopkeeper.ui.theme.Slate100
import com.shopkeeper.ui.theme.Slate200
import com.shopkeeper.ui.theme.Slate500
import com.shopkeeper.ui.theme.Slate700
import com.shopkeeper.ui.theme.Slate900
import com.shopkeeper.ui.util.formatMoney

@Composable
fun CategoryScreen(
    products: List<Product>,
    currencySymbol: String,
    onCategoryClick: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group products by category
    val grouped = remember(products) {
        products.groupBy { it.category.ifBlank { "General" } }.toSortedMap()
    }

    // Keep track of expanded state for each category
    val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate100)
    ) {
        if (grouped.isNotEmpty()) {
            val categoryCount = grouped.keys.size
            val productCount = products.size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (categoryCount == 1) "1 Category" else "$categoryCount Categories",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Slate900
                )
                Text(
                    text = if (productCount == 1) "1 product total" else "$productCount products total",
                    fontSize = 13.sp,
                    color = Slate500
                )
            }
        }

        if (grouped.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
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
                            imageVector = Icons.Default.Category,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Categories Yet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Slate900
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Products you add will automatically be grouped by their category here.",
                        fontSize = 13.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(grouped.keys.toList()) { categoryName ->
                    val categoryProducts = grouped[categoryName] ?: emptyList()
                    val count = categoryProducts.size
                    val totalPurchase = categoryProducts.sumOf { it.totalPurchaseValue }
                    val totalSales = categoryProducts.sumOf { it.totalSalesValue }
                    val isExpanded = expandedStates[categoryName] ?: false

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        border = BorderStroke(1.dp, Slate200),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Category Row Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedStates[categoryName] = !isExpanded
                                    }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(BlueLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Category,
                                            contentDescription = null,
                                            tint = BluePrimary
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = categoryName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Slate900
                                        )
                                        Text(
                                            text = "$count items • Cost: $currencySymbol${formatMoney(totalPurchase)}",
                                            fontSize = 12.sp,
                                            color = Slate500
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(GreenLight)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "$currencySymbol${formatMoney(totalSales)}",
                                            color = GreenSuccess,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }

                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = Slate500,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            // Expanded Product List under this Category
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    HorizontalDivider(color = Slate100)
                                    categoryProducts.forEach { item ->
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
                                                    color = Slate900
                                                )
                                                Text(
                                                    text = "#${item.code} • Qty: ${item.quantity}",
                                                    fontSize = 12.sp,
                                                    color = Slate500
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = "Selling: $currencySymbol${formatMoney(item.salesPrice)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = GreenSuccess
                                                )
                                                Text(
                                                    text = "Cost: $currencySymbol${formatMoney(item.purchasePrice)}",
                                                    fontSize = 12.sp,
                                                    color = Slate700
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = Slate100)
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
