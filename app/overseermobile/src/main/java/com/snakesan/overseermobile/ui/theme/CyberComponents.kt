package com.snakesan.overseermobile.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** The cut-corner shape every bordered panel/field in this module shares. */
val CyberFieldShape = CutCornerShape(8.dp)

/** Small, bold, letter-spaced uppercase label for a section within a dialog
 * or panel — matches ACK's `TightSectionLabel`. */
@Composable
fun CyberSectionLabel(text: String, modifier: Modifier = Modifier, color: Color = NeonCyan) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = CyberFont,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        fontSize = 12.sp
    )
}

/** Text field colors matching the void/graphite/neon scheme, for every
 * outlined text field in this module. */
@Composable
fun cyberTextFieldColors(accent: Color = NeonCyan): TextFieldColors = TextFieldDefaults.colors(
    focusedContainerColor = Graphite,
    unfocusedContainerColor = Graphite,
    focusedIndicatorColor = accent,
    unfocusedIndicatorColor = Color.DarkGray,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = accent
)

/**
 * A flat, cut-corner, bordered button matching the shared family look (ACK's
 * `NeonButton`, the watch module's `CyberButton`): a bordered panel rather
 * than a filled Material button, uppercase mono label, dimmed when inactive,
 * haptic feedback on every tap.
 */
@Composable
fun CyberPanelButton(
    text: String,
    modifier: Modifier = Modifier,
    isActive: Boolean = true,
    mainColor: Color = NeonCyan,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val contentColor = if (isActive) mainColor else mainColor.copy(alpha = 0.4f)
    val borderColor = if (isActive) mainColor else mainColor.copy(alpha = 0.2f)

    Button(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.height(44.dp),
        shape = CutCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Graphite, contentColor = contentColor),
        border = BorderStroke(1.dp, borderColor),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        Text(
            text.uppercase(),
            fontFamily = CyberFont,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            fontSize = 13.sp
        )
    }
}
