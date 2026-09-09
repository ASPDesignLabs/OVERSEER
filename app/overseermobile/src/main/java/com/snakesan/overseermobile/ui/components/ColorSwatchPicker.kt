package com.snakesan.overseermobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snakesan.overseermobile.ui.theme.CyberSectionLabel
import com.snakesan.overseermobile.ui.theme.ShortcutColorPresets

private val PresetNames = listOf("CYAN", "PURPLE", "RED", "GREEN", "PINK", "ORANGE", "BLUE", "GOLD")

/**
 * A fixed grid of named neon swatches for a custom shortcut's wedge color.
 * Discrete, labeled choices are easier to pick precisely than a continuous
 * color wheel, and every option is guaranteed legible against the app's
 * dark theme.
 */
@Composable
fun ColorSwatchPicker(
    selected: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        CyberSectionLabel(text = "WEDGE COLOR", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.size(8.dp))

        val rows = ShortcutColorPresets.mapIndexed { index, color -> index to color }.chunked(4)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    row.forEach { (index, color) ->
                        val name = PresetNames.getOrElse(index) { "COLOR" }
                        val isSelected = color == selected
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(color) }
                                .semantics {
                                    contentDescription = if (isSelected) "$name, selected" else name
                                }
                        )
                    }
                }
            }
        }
    }
}
