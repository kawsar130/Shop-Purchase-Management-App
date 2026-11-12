package com.shopkeeper.ui.screens.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.shopkeeper.R
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.theme.AmberWarning
import com.shopkeeper.ui.theme.AmberWarningDark
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.GreenSuccessDark

/**
 * Minimal icon-only sync status indicator.
 * Displays real-time synchronization state without text and without button interactions.
 */
@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val successTint = if (isDark) GreenSuccessDark else GreenSuccess
    val warningTint = if (isDark) AmberWarningDark else AmberWarning
    val errorTint = MaterialTheme.colorScheme.error

    Box(
        modifier = modifier.size(36.dp),
        contentAlignment = Alignment.Center
    ) {
        when (status) {
            is SyncStatus.Syncing -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is SyncStatus.Success -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = stringResource(R.string.sync_status_synced),
                    tint = successTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SyncStatus.PendingSync -> {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = stringResource(R.string.sync_status_pending, status.pendingChangesCount),
                    tint = warningTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SyncStatus.Offline -> {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = stringResource(R.string.sync_status_offline, status.pendingChangesCount),
                    tint = warningTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            is SyncStatus.Error -> {
                Icon(
                    imageVector = Icons.Default.SyncProblem,
                    contentDescription = stringResource(R.string.sync_status_error, status.errorMessage),
                    tint = errorTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            SyncStatus.Idle -> {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = stringResource(R.string.sync_status_connected),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
