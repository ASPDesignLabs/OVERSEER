package com.snakesan.overseermobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// A fixed dark/neon scheme, deliberately not using Android 12+ dynamic
// color: this is a companion app to a themed watch face, so its look should
// stay stable and match the watch rather than shift with the user's
// wallpaper.
private val OverseerColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    secondary = NeonPink,
    onSecondary = Color.Black,
    tertiary = NeonGreen,
    onTertiary = Color.Black,
    background = VoidBlack,
    onBackground = Color.White,
    surface = Graphite,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color.LightGray,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun OverseerMobileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OverseerColorScheme,
        typography = Typography,
        content = content
    )
}
