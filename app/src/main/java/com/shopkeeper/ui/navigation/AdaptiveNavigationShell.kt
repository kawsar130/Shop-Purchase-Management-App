package com.shopkeeper.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopkeeper.R
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.screens.components.SyncStatusIndicator

/**
 * Adaptive navigation container that automatically switches between standard mobile navigation
 * (for Compact screens, e.g. phones in portrait < 600dp) and a persistent leading-edge [NavigationRail]
 * (for Medium and Expanded screens, e.g. unfolded foldables, tablets, and desktop >= 600dp).
 */
@Composable
fun AdaptiveNavigationShell(
    currentRoute: String?,
    syncStatus: SyncStatus,
    onNavigateToProducts: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onRefreshSync: () -> Unit,
    modifier: Modifier = Modifier,
    isExpandedOrMedium: Boolean = LocalConfiguration.current.screenWidthDp >= 600,
    content: @Composable () -> Unit
) {
    val showRail = isExpandedOrMedium && (
        currentRoute == Screen.Products.route ||
        currentRoute == Screen.Categories.route ||
        currentRoute == Screen.Settings.route ||
        currentRoute == Screen.Staff.route
    )

    if (showRail) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                header = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Store,
                            contentDescription = stringResource(R.string.app_name),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                val isProducts = currentRoute == Screen.Products.route
                NavigationRailItem(
                    selected = isProducts,
                    onClick = onNavigateToProducts,
                    icon = {
                        Icon(
                            imageVector = if (isProducts) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2,
                            contentDescription = stringResource(R.string.nav_products),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_products),
                            fontSize = 12.sp,
                            fontWeight = if (isProducts) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                val isCategories = currentRoute == Screen.Categories.route
                NavigationRailItem(
                    selected = isCategories,
                    onClick = onNavigateToCategories,
                    icon = {
                        Icon(
                            imageVector = if (isCategories) Icons.Filled.Category else Icons.Outlined.Category,
                            contentDescription = stringResource(R.string.nav_categories),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_categories),
                            fontSize = 12.sp,
                            fontWeight = if (isCategories) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                val isSettings = currentRoute == Screen.Settings.route || currentRoute == Screen.Staff.route
                NavigationRailItem(
                    selected = isSettings,
                    onClick = onNavigateToSettings,
                    icon = {
                        Icon(
                            imageVector = if (isSettings) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.nav_settings),
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_settings),
                            fontSize = 12.sp,
                            fontWeight = if (isSettings) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.weight(1f))

                // Bottom sync status badge with one-tap refresh
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    IconButton(
                        onClick = onRefreshSync,
                        modifier = Modifier.size(48.dp)
                    ) {
                        SyncStatusIndicator(status = syncStatus)
                    }
                }
            }

            VerticalDivider(
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.width(1.dp).fillMaxHeight()
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                content()
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            content()
        }
    }
}
