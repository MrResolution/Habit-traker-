package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity

// Defining temporary light theme values to avoid compile errors
private val ColorLightBorder = androidx.compose.ui.graphics.Color(0xFFE5E7EB)
private val ColorDarkText = androidx.compose.ui.graphics.Color(0xFF111827)
private val ColorDarkMuted = androidx.compose.ui.graphics.Color(0xFF6B7280)

private val DarkColorScheme = darkColorScheme(
    primary = NeonPurple,
    secondary = CyberTeal,
    tertiary = StreakRose,
    background = ObsidianBlack,
    surface = SurfaceZinc,
    surfaceVariant = SurfaceZincLight,
    onPrimary = TextWhite,
    onSecondary = ObsidianBlack,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextMuted
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    secondary = SecondaryLight,
    tertiary = StreakRose,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = ColorLightBorder,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = ColorDarkText,
    onSurface = ColorDarkText,
    onSurfaceVariant = ColorDarkMuted
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Force dark theme as requested for clean UI with dark mode support by default,
    // but keep system dark theme fallback capability
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            window.navigationBarColor = androidx.compose.ui.graphics.Color.Transparent.toArgb()
            
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
