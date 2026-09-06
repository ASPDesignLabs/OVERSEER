package com.snakesan.overseermobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.snakesan.overseermobile.data.WedgeContent
import com.snakesan.overseermobile.data.WedgeSlot

private val WedgePositionNames = listOf("WEDGE 1", "WEDGE 2", "WEDGE 3")

/** One row summarizing a wedge's current assignment; tap to edit it. */
@Composable
fun WedgeCard(slot: WedgeSlot, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val positionName = WedgePositionNames.getOrElse(slot.position) { "WEDGE ${slot.position + 1}" }
    val kindLabel = when (slot.content) {
        is WedgeContent.Function -> "BUILT-IN FUNCTION"
        is WedgeContent.Shortcut -> "APP SHORTCUT"
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$positionName, $kindLabel, ${slot.content.previewLabel()}. Tap to edit." },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ColorDot(color = slot.content.previewColor())

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = positionName,
                    style = MaterialTheme.typography.titleMedium,
                    color = slot.content.previewColor()
                )
                Text(
                    text = kindLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = slot.content.previewLabel(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "EDIT",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ColorDot(color: androidx.compose.ui.graphics.Color) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
    )
}
