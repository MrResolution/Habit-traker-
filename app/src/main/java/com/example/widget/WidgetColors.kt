package com.example.widget

import androidx.compose.ui.graphics.Color

/**
 * Color constants for the widget UI, matching the app's premium dark palette.
 * Using direct Color values here because Glance ColorProvider requires them.
 */
object WidgetColors {
    // Backgrounds
    val SurfaceDark = Color(0xFF18181B)      // Zinc-900
    val SurfaceLight = Color(0xFF27272A)     // Zinc-800
    val CardSurface = Color(0xFF1F1F23)      // Slightly lighter than surface

    // Accents
    val PurpleAccent = Color(0xFF8B5CF6)     // Matches NeonPurple
    val TealAccent = Color(0xFF06B6D4)       // Matches CyberTeal
    val StreakOrange = Color(0xFFF97316)      // Warm streak color
    val CompletedChipBg = Color(0xFF064E3B)  // Deep green tint

    // Text
    val TextWhite = Color(0xFFFAFAFA)
    val TextMuted = Color(0xFFA1A1AA)        // Zinc-400
}
