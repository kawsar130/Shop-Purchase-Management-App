package com.shopkeeper.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BluePrimary,
    onPrimary = PureWhite,
    primaryContainer = BlueLight,
    onPrimaryContainer = BluePrimary,
    secondary = GreenSuccess,
    onSecondary = PureWhite,
    secondaryContainer = GreenLight,
    onSecondaryContainer = GreenSuccess,
    background = Slate50,
    surface = PureWhite,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueSecondary,
    onPrimary = PureWhite,
    primaryContainer = BluePrimary,
    onPrimaryContainer = PureWhite,
    secondary = GreenSuccess,
    onSecondary = PureWhite,
    background = Slate900,
    surface = Slate700,
    onBackground = Slate50,
    onSurface = Slate50
)

@Composable
fun ShopKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
