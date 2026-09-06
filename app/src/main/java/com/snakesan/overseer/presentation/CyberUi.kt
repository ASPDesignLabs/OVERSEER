package com.snakesan.overseer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- CYBERPUNK SHAPE DEFINITION ---
// A shape with 45-degree cuts on the Top-Left and Bottom-Right corners
class CyberCutShape(private val cornerSize: Float = 20f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            moveTo(0f, cornerSize)
            lineTo(cornerSize, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - cornerSize)
            lineTo(size.width - cornerSize, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun CyberButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonCyanVal,
    text: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    // Visual feedback: dim/brighten on press
    val baseAlpha = if (isPressed) 0.4f else 0.15f
    val borderAlpha = if (isPressed) 1.0f else 0.6f
    
    Box(
        modifier = modifier
            .clip(CyberCutShape(25f)) // The physical clip for click target
            .background(color.copy(alpha = baseAlpha))
            .border(1.dp, color.copy(alpha = borderAlpha), CyberCutShape(25f))
            .clickable(
                interactionSource = interactionSource, 
                indication = null, // We handle visual state manually
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp), // Inner padding
        contentAlignment = Alignment.Center
    ) {
        // Decorative corner lines (tech bits)
        Box(
            modifier = Modifier
                .matchParentSize()
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val s = 10f // line length
                    // Top Left Accent
                    drawLine(color, Offset(0f, 25f), Offset(0f, 25f+s), 3f)
                    // Bottom Right Accent
                    drawLine(color, Offset(w, h-25f), Offset(w, h-25f-s), 3f)
                }
        )
        text()
    }
}
