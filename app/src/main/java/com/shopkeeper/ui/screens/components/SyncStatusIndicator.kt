package com.shopkeeper.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopkeeper.data.model.SyncStatus
import com.shopkeeper.ui.theme.AmberLight
import com.shopkeeper.ui.theme.AmberWarning
import com.shopkeeper.ui.theme.BlueLight
import com.shopkeeper.ui.theme.BluePrimary
import com.shopkeeper.ui.theme.GreenLight
import com.shopkeeper.ui.theme.GreenSuccess
import com.shopkeeper.ui.theme.RedAlert
import com.shopkeeper.ui.theme.RedLight

@Composable
fun SyncStatusIndicator(
    status: SyncStatus,
    onSyncClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (status) {
        is SyncStatus.Syncing -> Quadruple(
            BlueLight,
            BluePrimary,
            Icons.Default.Sync,
            "Syncing..."
        )
        is SyncStatus.Success -> Quadruple(
            GreenLight,
            GreenSuccess,
            Icons.Default.CloudDone,
            "Sheet Synced"
        )
        is SyncStatus.Offline -> Quadruple(
            AmberLight,
            AmberWarning,
            Icons.Default.CloudOff,
            if (status.pendingChangesCount > 0) "${status.pendingChangesCount} Pending" else "Offline"
        )
        is SyncStatus.Error -> Quadruple(
            RedLight,
            RedAlert,
            Icons.Default.CloudOff,
            "Sync Error"
        )
        SyncStatus.Idle -> Quadruple(
            BlueLight,
            BluePrimary,
            Icons.Default.Sync,
            "Sync"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable { onSyncClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status is SyncStatus.Syncing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 2.dp,
                    color = textColor
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = textColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
