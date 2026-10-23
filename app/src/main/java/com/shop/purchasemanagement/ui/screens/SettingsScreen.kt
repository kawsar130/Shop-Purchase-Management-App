package com.shop.purchasemanagement.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shop.purchasemanagement.ui.theme.BlueLight
import com.shop.purchasemanagement.ui.theme.BluePrimary
import com.shop.purchasemanagement.ui.theme.GreenLight
import com.shop.purchasemanagement.ui.theme.GreenSuccess
import com.shop.purchasemanagement.ui.theme.PureWhite
import com.shop.purchasemanagement.ui.theme.RedAlert
import com.shop.purchasemanagement.ui.theme.Slate100
import com.shop.purchasemanagement.ui.theme.Slate500
import com.shop.purchasemanagement.ui.theme.Slate700
import com.shop.purchasemanagement.ui.theme.Slate900
import com.shop.purchasemanagement.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToStaff: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val shopName by viewModel.shopName.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val spreadsheetId by viewModel.spreadsheetId.collectAsState()
    val lastSync by viewModel.lastSyncTime.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Slate100)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Shop & User Profile
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(BlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = shopName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Signed in as: $email",
                            fontSize = 13.sp,
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Slate100)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = RedAlert)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out Google Account", color = RedAlert, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 2. Multi-User Team Access (Staff Management)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Group, contentDescription = null, tint = GreenSuccess)
                    }
                    Column {
                        Text(
                            text = "Staff & Multi-User Access",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Slate900
                        )
                        Text(
                            text = "Invite staff members using their Gmail",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onNavigateToStaff,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = PureWhite
                    )
                ) {
                    Text("Manage Staff Members", color = PureWhite, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 3. Cloud Database & Real-Time Sync
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = BluePrimary)
                    Text(
                        text = "Cloud Database & Backup",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Your product inventory is securely backed up in Cloud Firestore with instant real-time synchronization between you and your staff.",
                    fontSize = 13.sp,
                    color = Slate700,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { viewModel.exportInventory(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to Excel / Google Sheets (CSV)", color = BluePrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.syncNow() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = PureWhite
                    )
                ) {
                    Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = PureWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Force Sync All Products", color = PureWhite, fontWeight = FontWeight.Bold)
                }

                if (lastSync > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    val dateFormatted = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault()).format(Date(lastSync))
                    Text(
                        text = "Last synced: $dateFormatted",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }
            }
        }

        // 4. Currency Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Payments, contentDescription = null, tint = BluePrimary)
                    Text(
                        text = "Shop Currency",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Slate900
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                val symbols = listOf("$", "৳", "₹", "€", "£", "AED")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    symbols.forEach { sym ->
                        val isSelected = sym == currency
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.updateCurrencySymbol(sym) },
                            label = { Text(sym, fontWeight = FontWeight.Bold) },
                            shape = RoundedCornerShape(10.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BluePrimary,
                                selectedLabelColor = PureWhite
                            )
                        )
                    }
                }
            }
        }
    }
}
