package com.snakesan.overseermobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.snakesan.overseermobile.data.OverseerMobileConfig
import com.snakesan.overseermobile.data.WedgeContent
import com.snakesan.overseermobile.data.WedgeFunction
import com.snakesan.overseermobile.ui.theme.NeonCyan
import com.snakesan.overseermobile.ui.theme.NeonGreen
import com.snakesan.overseermobile.ui.theme.NeonPink

// The same start/sweep angles the watch face draws in MainGrid.kt's
// drawOuterElements, indexed by wedge position, so this preview lines up
// with the real ring instead of a naive 0/120/240 split.
private val WedgeArcAngles = listOf(-87f to 114f, 33f to 114f, 153f to 114f)

/** The watch's own default color for a function, before any live telemetry. */
fun WedgeFunction.representativeColor(): Color = when (this) {
    WedgeFunction.VITALITY -> NeonGreen
    WedgeFunction.ACK -> NeonCyan
    WedgeFunction.FLUX -> NeonPink
}

fun WedgeContent.previewColor(): Color = when (this) {
    is WedgeContent.Function -> function.representativeColor()
    is WedgeContent.Shortcut -> color
}

fun WedgeContent.previewLabel(): String = when (this) {
    is WedgeContent.Function -> function.displayName
    is WedgeContent.Shortcut -> label
}

@Composable
fun WedgeRingPreview(config: OverseerMobileConfig, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(220.dp)) {
        val strokeWidth = 22.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset((size.width - radius * 2) / 2, (size.height - radius * 2) / 2)
        val arcSize = Size(radius * 2, radius * 2)

        config.wedges.sortedBy { it.position }.forEach { slot ->
            val (start, sweep) = WedgeArcAngles.getOrElse(slot.position) { 0f to 0f }
            drawArc(
                color = slot.content.previewColor(),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}
