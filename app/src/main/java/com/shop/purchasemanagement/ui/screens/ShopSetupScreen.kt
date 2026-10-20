package com.shop.purchasemanagement.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.purchasemanagement.ui.theme.BlueLight
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
import com.shop.purchasemanagement.ui.viewmodel.AuthState
import com.shop.purchasemanagement.ui.viewmodel.AuthViewModel

@Composable
fun ShopSetupScreen(
    authViewModel: AuthViewModel,
    onSetupCompleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by authViewModel.authState.collectAsState()

    var shopName by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf("$") }

    if (state is AuthState.Authenticated) {
        onSetupCompleted()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Slate100)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        when (val current = state) {
            is AuthState.SelectExistingShop -> {
                // Show existing shops found in Google Drive
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CloudDone, contentDescription = null, tint = GreenSuccess)
                            Text(
                                text = "Select Your Shop",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Slate900
                            )
                        }

                        Text(
                            text = "We found existing shop databases connected to your Google Account:",
                            fontSize = 13.sp,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().height(200.dp)
                        ) {
                            items(current.availableShops) { shop ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { authViewModel.selectExistingShop(shop) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Slate100)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = shop.shopName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Slate900
                                            )
                                            Text(
                                                text = if (shop.isOwner) "Owner: You" else "Owner: ${shop.ownerEmail}",
                                                fontSize = 12.sp,
                                                color = Slate500
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ArrowForward,
                                            contentDescription = "Select",
                                            tint = BluePrimary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = Slate100)
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { authViewModel.resetToShopSetup() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Create a New Shop Instead")
                        }
                    }
                }
            }

            is AuthState.Loading -> {
                Card(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = BluePrimary, strokeWidth = 3.dp)
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Creating Shop Spreadsheet...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Setting up Products and Config tabs in your Google Drive",
                            fontSize = 13.sp,
                            color = Slate500,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            else -> {
                // New Shop Creation Form
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(22.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Store,
                                    contentDescription = null,
                                    tint = BluePrimary
                                )
                            }
                            Column {
                                Text(
                                    text = "Setup Your Shop",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = Slate900
                                )
                                Text(
                                    text = "1-click automatic database creation",
                                    fontSize = 12.sp,
                                    color = Slate500
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Shop Name input
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Shop Name *") },
                            placeholder = { Text("e.g. Al-Madina Store") },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Store, contentDescription = null, tint = BluePrimary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Currency Selector
                        Text(
                            text = "Select Currency Symbol",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Slate700
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        val currencies = listOf("$", "৳", "₹", "€", "£", "AED")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            currencies.forEach { curr ->
                                val isSelected = curr == selectedCurrency
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedCurrency = curr },
                                    label = { Text(curr, fontWeight = FontWeight.Bold) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BluePrimary,
                                        selectedLabelColor = PureWhite
                                    )
                                )
                            }
                        }

                        if (state is AuthState.Error) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(RedLight)
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = (state as AuthState.Error).message,
                                    color = RedAlert,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                authViewModel.createNewShop(shopName, selectedCurrency)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Text(
                                text = "Create Shop Database & Start",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "✓ Automatically creates a Google Sheet in your Google Drive.\n✓ Ready to use immediately with zero manual configuration.",
                            fontSize = 12.sp,
                            color = Slate500,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}
