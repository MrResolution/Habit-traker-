package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

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
    val colorScheme = if (darkTheme) DarkColorScheme else DarkColorScheme // Force dark theme by default to ensure premium aesthetic

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
