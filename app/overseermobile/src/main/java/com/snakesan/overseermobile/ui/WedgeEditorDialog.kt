package com.snakesan.overseermobile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.snakesan.overseermobile.data.LaunchableApp
import com.snakesan.overseermobile.data.OverseerMobileConfig
import com.snakesan.overseermobile.data.WedgeContent
import com.snakesan.overseermobile.data.WedgeFunction
import com.snakesan.overseermobile.data.WedgeSlot
import com.snakesan.overseermobile.ui.components.AppPickerDialog
import com.snakesan.overseermobile.ui.components.ColorSwatchPicker
import com.snakesan.overseermobile.ui.components.representativeColor
import com.snakesan.overseermobile.ui.theme.CyberFieldShape
import com.snakesan.overseermobile.ui.theme.CyberFont
import com.snakesan.overseermobile.ui.theme.CyberPanelButton
import com.snakesan.overseermobile.ui.theme.DefaultShortcutColor
import com.snakesan.overseermobile.ui.theme.Graphite
import com.snakesan.overseermobile.ui.theme.NeonCyan
import com.snakesan.overseermobile.ui.theme.cyberTextFieldColors

private enum class EditorMode { FUNCTION, SHORTCUT }

private val WedgePositionNames = listOf("Wedge 1", "Wedge 2", "Wedge 3")
private val DialogPanelShape = CutCornerShape(16.dp)

/**
 * Edits a single wedge. Nothing takes effect until Save is pressed —
 * changes made here only update the phone's local config; pushing to the
 * watch is a separate, explicitly confirmed action back on the main screen.
 */
@Composable
fun WedgeEditorDialog(
    slot: WedgeSlot,
    config: OverseerMobileConfig,
    onDismiss: () -> Unit,
    onSave: (WedgeContent) -> Unit
) {
    var mode by remember {
        mutableStateOf(if (slot.content is WedgeContent.Function) EditorMode.FUNCTION else EditorMode.SHORTCUT)
    }

    var selectedFunction by remember {
        mutableStateOf((slot.content as? WedgeContent.Function)?.function ?: WedgeFunction.VITALITY)
    }

    val existingShortcut = slot.content as? WedgeContent.Shortcut
    var shortcutPackage by remember { mutableStateOf(existingShortcut?.packageName) }
    var shortcutLabel by remember { mutableStateOf(existingShortcut?.label ?: "") }
    var shortcutColor by remember { mutableStateOf(existingShortcut?.color ?: DefaultShortcutColor) }

    var showAppPicker by remember { mutableStateOf(false) }
    var pendingSwap by remember { mutableStateOf<Pair<WedgeFunction, WedgeSlot>?>(null) }

    val canSave = mode == EditorMode.FUNCTION || (shortcutPackage != null && shortcutLabel.isNotBlank())

    fun attemptSave() {
        if (mode == EditorMode.FUNCTION) {
            val occupant = config.slotHolding(selectedFunction)
            if (occupant != null && occupant.position != slot.position) {
                pendingSwap = selectedFunction to occupant
            } else {
                onSave(WedgeContent.Function(selectedFunction))
            }
        } else {
            val pkg = shortcutPackage ?: return
            onSave(WedgeContent.Shortcut(pkg, shortcutLabel.trim(), shortcutColor))
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .background(Graphite, DialogPanelShape)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), DialogPanelShape)
                .padding(24.dp)
        ) {
            Text(
                text = "EDIT ${WedgePositionNames.getOrElse(slot.position) { "WEDGE" }.uppercase()}",
                fontFamily = CyberFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(20.dp))

            ModeToggle(mode = mode, onModeChange = { mode = it })

            Spacer(modifier = Modifier.height(20.dp))

            if (mode == EditorMode.FUNCTION) {
                Text(
                    text = "Preserves this app's built-in display for that function. Only one wedge can run each function at a time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                WedgeFunction.entries.forEach { function ->
                    val occupant = config.slotHolding(function)
                    FunctionOptionRow(
                        function = function,
                        isSelected = function == selectedFunction,
                        occupiedElsewhere = occupant != null && occupant.position != slot.position,
                        occupiedLabel = occupant?.let { WedgePositionNames.getOrElse(it.position) { "another wedge" } },
                        onClick = { selectedFunction = function }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                Text(
                    text = "Launches any app you choose, in a color you pick.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                CyberPanelButton(
                    text = shortcutPackage?.let { "APP: $shortcutLabel" } ?: "CHOOSE APP",
                    modifier = Modifier.fillMaxWidth(),
                    mainColor = shortcutColor,
                    onClick = { showAppPicker = true }
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = shortcutLabel,
                    onValueChange = { shortcutLabel = it },
                    label = { Text("Label shown on the watch") },
                    singleLine = true,
                    shape = CyberFieldShape,
                    colors = cyberTextFieldColors(shortcutColor),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                ColorSwatchPicker(
                    selected = shortcutColor,
                    onColorSelected = { shortcutColor = it }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CyberPanelButton(
                    text = "CANCEL",
                    modifier = Modifier.weight(1f),
                    isActive = false,
                    mainColor = NeonCyan,
                    onClick = onDismiss
                )
                CyberPanelButton(
                    text = "SAVE",
                    modifier = Modifier.weight(1f),
                    isActive = canSave,
                    mainColor = NeonCyan,
                    onClick = { if (canSave) attemptSave() }
                )
            }
        }
    }

    if (showAppPicker) {
        AppPickerDialog(
            onAppSelected = { app: LaunchableApp ->
                shortcutPackage = app.packageName
                if (shortcutLabel.isBlank()) shortcutLabel = app.label.uppercase()
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    pendingSwap?.let { (function, occupant) ->
        val occupantName = WedgePositionNames.getOrElse(occupant.position) { "another wedge" }
        val thisName = WedgePositionNames.getOrElse(slot.position) { "this wedge" }
        AlertDialog(
            onDismissRequest = { pendingSwap = null },
            title = { Text("Move ${function.displayName}?") },
            text = {
                Text(
                    "${function.displayName} is currently on $occupantName. Saving will move it here " +
                        "and give $occupantName what's currently on $thisName."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingSwap = null
                    onSave(WedgeContent.Function(function))
                }) { Text("SWAP") }
            },
            dismissButton = {
                TextButton(onClick = { pendingSwap = null }) { Text("CANCEL") }
            }
        )
    }
}

@Composable
private fun ModeToggle(mode: EditorMode, onModeChange: (EditorMode) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(EditorMode.FUNCTION to "FUNCTION", EditorMode.SHORTCUT to "SHORTCUT").forEach { (value, label) ->
            val isSelected = value == mode
            val interactionSource = remember { MutableInteractionSource() }
            Text(
                text = label,
                fontFamily = CyberFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontSize = 12.sp,
                color = if (isSelected) NeonCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .background(if (isSelected) NeonCyan.copy(alpha = 0.1f) else Graphite, CyberFieldShape)
                    .border(1.dp, if (isSelected) NeonCyan else androidx.compose.ui.graphics.Color.DarkGray, CyberFieldShape)
                    .clickable(interactionSource = interactionSource, indication = null) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onModeChange(value)
                    }
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
private fun FunctionOptionRow(
    function: WedgeFunction,
    isSelected: Boolean,
    occupiedElsewhere: Boolean,
    occupiedLabel: String?,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Graphite, CyberFieldShape)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) function.representativeColor() else androidx.compose.ui.graphics.Color.DarkGray,
                shape = CyberFieldShape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(16.dp)
                .background(function.representativeColor(), CutCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = function.displayName,
                fontFamily = CyberFont,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (occupiedElsewhere && occupiedLabel != null) {
                Text(
                    text = "Currently on $occupiedLabel — saving will move it here",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
