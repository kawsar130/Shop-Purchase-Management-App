package com.shopkeeper.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.shopkeeper.ui.screens.components.CurrencySelector
import com.shopkeeper.ui.theme.BlueLight
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
import com.shopkeeper.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToStaff: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null
) {
    val context = LocalContext.current
    val shopName by viewModel.shopName.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val email by viewModel.userEmail.collectAsState()
    val isOwner by viewModel.isOwner.collectAsState()
    val lastSync by viewModel.lastSyncTime.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val showSyncSuccess by viewModel.showSyncSuccess.collectAsState()

    var showLogoutConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.message.collectLatest { msg ->
            snackbarHostState?.showSnackbar(msg)
        }
    }

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
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            color = Slate900,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Signed in as: $email",
                            fontSize = 13.sp,
                            color = Slate500,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Slate100)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = { showLogoutConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RedLight),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert)
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
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
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
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    onClick = {
                        viewModel.exportInventory(context.cacheDir) { file ->
                            val chooser = com.shopkeeper.data.export.CsvExporter.createShareIntent(context, file, shopName)
                            context.startActivity(chooser)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Slate200),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BluePrimary)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to Excel / Google Sheets (CSV)", color = BluePrimary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.syncNow() },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BluePrimary,
                        contentColor = PureWhite,
                        disabledContainerColor = BluePrimary.copy(alpha = 0.7f),
                        disabledContentColor = PureWhite.copy(alpha = 0.8f)
                    )
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, tint = PureWhite)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync All Products", color = PureWhite, fontWeight = FontWeight.Bold)
                    if (showSyncSuccess) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Synced",
                            tint = PureWhite,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
            border = BorderStroke(1.dp, Slate200),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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

                if (!isOwner) {
                    Text(
                        text = "Display currency for your device (Base shop currency is set by the owner).",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                CurrencySelector(
                    selectedCurrency = currency,
                    onCurrencySelected = { viewModel.updateCurrencySymbol(it) }
                )
            }
        }
    }

    // Sign Out Confirmation Dialog
    if (showLogoutConfirmation) {
        Dialog(
            onDismissRequest = { showLogoutConfirmation = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                border = BorderStroke(1.dp, Slate200),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(RedLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sign Out",
                            tint = RedAlert,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Sign Out of ShopKeeper?",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate900
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to sign out? Real-time cloud sync will pause on this device until you sign back in.",
                        fontSize = 13.sp,
                        color = Slate500,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmation = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate200),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate700)
                        ) {
                            Text("Cancel", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = {
                                showLogoutConfirmation = false
                                onLogout()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedAlert,
                                contentColor = PureWhite
                            )
                        ) {
                            Text("Sign Out", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
