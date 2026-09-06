package com.snakesan.overseer.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme

// --- Your Universal Palette ---
val NeonCyan = Color(0xFF00F3FF)   // Primary / ACK
val NeonGreen = Color(0xFF00FF41)  // Tertiary / VITALITY
val NeonAmber = Color(0xFFFF9900)  // Warnings / Extra
val NeonBg = Color(0xFF050505)     // Background
val NeonDark = Color(0xFF121212)   // Surface

// --- The Wear OS Color Scheme ---
val OverseerColorPalette = Colors(
    primary = NeonCyan,
    primaryVariant = NeonPink,
    secondary = NeonGreen,
    background = NeonBg,
    surface = NeonDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onSurface = NeonCyan
)

@Composable
fun OverseerTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = OverseerColorPalette,
        content = content
    )
}
