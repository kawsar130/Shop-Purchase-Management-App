package com.shopkeeper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standardized 4-tier Shape Design Token hierarchy for ShopKeeper retail assistant.
 * Enforces geometric consistency across all cards, dialogs, inputs, buttons, and chips.
 */
val ShopShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),   // Minimal status badges / product tags
    small = RoundedCornerShape(8.dp),        // Small badges, category indicator chips
    medium = RoundedCornerShape(12.dp),      // Standard: All text fields (ShopTextField), buttons, filter chips, search bars
    large = RoundedCornerShape(16.dp),       // Standard: List cards (ProductCard, CategoryCard, ShopSetupCard)
    extraLarge = RoundedCornerShape(24.dp)   // Standard: Modal dialogs, Bottom sheets
)
