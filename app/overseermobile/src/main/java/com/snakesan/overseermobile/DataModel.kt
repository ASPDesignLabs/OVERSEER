package com.snakesan.overseermobile

import androidx.compose.ui.graphics.Color

// Represents a single shortcut slot on the ring
data class ShortcutConfig(
    val id: String,
    val packageName: String,
    val label: String,
    val angleStart: Float,
    val angleEnd: Float,
    val color: Color
)

// The global state for the preview
data class OverseerConfig(
    val fontSizeSp: Float = 14f,
    val shortcuts: List<ShortcutConfig> = listOf(
        ShortcutConfig("vit", "com.snakesan.vitalitysys", "VITALITY", 0f, 120f, Color(0xFF00FF41)),
        ShortcutConfig("ack", "com.example.besu", "ACK", 120f, 240f, Color(0xFF00F3FF)),
        ShortcutConfig("neon", "com.snakesan.neonflux", "NEON", 240f, 360f, Color(0xFFFF0055))
    )
)
