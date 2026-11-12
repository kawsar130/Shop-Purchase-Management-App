package com.shopkeeper.ui.screens.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shopkeeper.ui.theme.ShopKeeperTheme

/**
 * Canonical Empty State component for ShopKeeper.
 * Provides a standardized visual hierarchy across inventory, categories, staff, and search:
 * - 72dp circular icon container in primaryContainer
 * - 36dp primary tinted vector icon
 * - 18sp bold title in onSurface
 * - 13sp centered subtitle in onSurfaceVariant
 * - Optional primary action button (48dp height, 12dp shape)
 * - Optional secondary action button (48dp height, 12dp shape)
 */
@Composable
fun ShopEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    actionText: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    secondaryActionText: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }

        if (!actionText.isNullOrBlank() && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (actionIcon != null) {
                    Icon(
                        imageVector = actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = actionText,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (!secondaryActionText.isNullOrBlank() && onSecondaryActionClick != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onSecondaryActionClick,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = secondaryActionText,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(name = "Empty State: Action Button", showBackground = true)
@Composable
fun ShopEmptyStateWithActionPreview() {
    ShopKeeperTheme {
        ShopEmptyState(
            icon = Icons.Default.Add,
            title = "No products yet",
            subtitle = "Add your first product to get started tracking your shop inventory.",
            actionText = "Add Product",
            actionIcon = Icons.Default.Add,
            onActionClick = {}
        )
    }
}

@Preview(name = "Empty State: Filter Reset", showBackground = true)
@Composable
fun ShopEmptyStateFilterPreview() {
    ShopKeeperTheme {
        ShopEmptyState(
            icon = Icons.Default.SearchOff,
            title = "No matching products",
            subtitle = "No products match your search query or category filter. Try searching with different keywords or clearing filters.",
            secondaryActionText = "Clear Filters",
            onSecondaryActionClick = {}
        )
    }
}
