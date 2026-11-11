package com.shopkeeper.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
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
    error = RedAlert,
    onError = PureWhite,
    errorContainer = RedLight,
    onErrorContainer = RedAlert,
    background = Slate50,
    surface = PureWhite,
    onBackground = Slate900,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    surfaceTint = Color.Transparent
)

private val DarkColorScheme = darkColorScheme(
    primary = BlueSecondary,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = GreenSuccessDark,
    onSecondary = PureWhite,
    secondaryContainer = GreenContainerDark,
    onSecondaryContainer = Color(0xFFBBF7D0),
    error = RedAlertDark,
    onError = PureWhite,
    errorContainer = RedContainerDark,
    onErrorContainer = Color(0xFFFECACA),
    background = Slate900,
    surface = Slate800,
    onBackground = Slate50,
    onSurface = Slate50,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate600,
    surfaceTint = Color.Transparent
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
            val insetsController = WindowCompat.getInsetsController(window, view)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            window.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(colorScheme.background.toArgb()))
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ShopShapes,
        content = content
    )
}
