package com.snakesan.overseermobile.ui.theme

import androidx.compose.ui.graphics.Color

// Mirrors the watch's neon palette (app/src/main/java/.../Constants.kt) so
// the phone app visually matches the watch face. Duplicated rather than
// shared because the two Gradle modules don't share a common library module.
val NeonCyan = Color(0xFF00F3FF)
val NeonPurple = Color(0xFFD500F9)
val NeonRed = Color(0xFFFF003C)
val NeonGreen = Color(0xFF00FF41)
val NeonPink = Color(0xFFFF0055)
val NeonOrange = Color(0xFFFF9100)
val NeonBlue = Color(0xFF2962FF)
val NeonGold = Color(0xFFFFD700)
val NeonDark = Color(0xFF121212)

/** Preset swatches offered when a user picks a color for a custom shortcut. */
val ShortcutColorPresets = listOf(
    NeonCyan, NeonPurple, NeonRed, NeonGreen, NeonPink, NeonOrange, NeonBlue, NeonGold
)

/** Default color for a brand-new shortcut, before the user picks one. */
val DefaultShortcutColor = NeonCyan
