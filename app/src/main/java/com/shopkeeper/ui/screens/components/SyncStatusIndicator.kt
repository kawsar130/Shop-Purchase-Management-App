package com.shopkeeper.ui.screens.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.theme.AmberWarning
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.Slate500

/**
 * Minimal icon-only sync status indicator.
 * Displays real-time synchronization state without text and without button interactions.
 */
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            is SyncStatus.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = BluePrimary
                )
            }
            is SyncStatus.Success -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Synced",
                    tint = GreenSuccess,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SyncStatus.PendingSync -> {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Pending Sync (${status.pendingChangesCount})",
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SyncStatus.Offline -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = "Offline (${status.pendingChangesCount} pending)",
                    tint = AmberWarning,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SyncStatus.Error -> {
                Icon(
                    imageVector = Icons.Default.SyncProblem,
                    contentDescription = "Sync Error: ${status.errorMessage}",
                    tint = RedAlert,
                    modifier = Modifier.size(20.dp)
                )
            }
            SyncStatus.Idle -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = "Connected",
                    tint = Slate500,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
