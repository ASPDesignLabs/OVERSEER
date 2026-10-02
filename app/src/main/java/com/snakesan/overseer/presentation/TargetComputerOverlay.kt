package com.snakesan.overseer.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text

// One screen of the module's navigation stack: either the top-level category
// picker (only shown when more than one Target Computer category is synced --
// see below) or a specific position within one category's tree, identified by
// its parentId ("" = that category's own top level). Mirrors the shape ACK
// Wear's own ComputerTargetFlyout already proved out for this exact data.
private sealed class ComputerLevel {
    object CategoryPicker : ComputerLevel()
    data class NodeLevel(
        val categoryId: String,
        val parentId: String,
        val levelLabel: String
    ) : ComputerLevel()
}

private data class ComputerRow(
    val label: String,
    val categoryId: String,
    val nodeId: String?, // null only for a top-level category row from the picker
    val isCategory: Boolean,
    val isActive: Boolean
)

// OVERSEER's own styling for browsing ACK's Target Computer categories, fed
// via ACK Wear's relay (SystemState.kt's ACK_COMPUTER_SYNC branch). Unlike ACK
// Wear's radial-ring flyout, this is a plain rotary+tap scrollable list --
// matches AckControlOverlay's existing crown/tap feel. Each row is its own
// clickable target (a real list, not hit-tested screen coordinates), so a tap
// commits immediately -- no separate confirm gesture needed. The crown just
// scrolls the list.
@Composable
fun TargetComputerOverlay(
    categories: Map<String, SyncedComputerCategory>,
    onPick: (categoryId: String, nodeId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val scrollState = rememberScrollState()

    val stack = remember {
        mutableStateListOf<ComputerLevel>().apply {
            val values = categories.values.toList()
            add(
                if (values.size == 1) {
                    val only = values.first()
                    ComputerLevel.NodeLevel(only.id, "", only.label)
                } else {
                    ComputerLevel.CategoryPicker
                }
            )
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    if (categories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "NO TARGET COMPUTER DATA",
                color = Color.Gray,
                fontFamily = CyberFont,
                fontSize = 10.sp,
                letterSpacing = 1.sp
            )
        }
        return
    }

    val currentLevel = stack.last()
    val isRoot = stack.size == 1

    val rows: List<ComputerRow> = when (val level = currentLevel) {
        is ComputerLevel.CategoryPicker -> categories.values.map { category ->
            ComputerRow(
                label = category.label,
                categoryId = category.id,
                nodeId = null,
                isCategory = true,
                isActive = false
            )
        }
        is ComputerLevel.NodeLevel -> {
            val category = categories[level.categoryId]
            category?.childrenOf(level.parentId)?.map { node ->
                ComputerRow(
                    label = node.label,
                    categoryId = level.categoryId,
                    nodeId = node.id,
                    isCategory = node.isCategory,
                    isActive = !node.isCategory && node.id == category.activeNodeId
                )
            } ?: emptyList()
        }
    }

    val levelLabel = when (val level = currentLevel) {
        is ComputerLevel.CategoryPicker -> "TARGET COMPUTER"
        is ComputerLevel.NodeLevel -> level.levelLabel
    }

    fun drillOrCommit(row: ComputerRow) {
        if (row.isCategory) {
            stack.add(ComputerLevel.NodeLevel(row.categoryId, row.nodeId ?: "", row.label))
        } else if (row.nodeId != null) {
            onPick(row.categoryId, row.nodeId)
        }
    }

    fun goBack() {
        if (isRoot) {
            onDismiss()
        } else {
            stack.removeAt(stack.lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .onRotaryScrollEvent {
                scrollState.dispatchRawDelta(it.verticalScrollPixels)
                true
            }
            .focusRequester(focusRequester)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Text(
                text = levelLabel.uppercase(),
                color = NeonCyanVal,
                fontFamily = CyberFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 14.dp, bottom = 6.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ComputerListRow(
                    label = if (isRoot) "EXIT" else "‹ BACK",
                    color = NeonOrange,
                    isActive = false
                ) { goBack() }

                Spacer(modifier = Modifier.height(6.dp))

                rows.forEach { row ->
                    val color = if (row.isCategory) NeonCyanVal else NeonGreenVal
                    ComputerListRow(
                        label = row.label,
                        color = color,
                        isActive = row.isActive
                    ) { drillOrCommit(row) }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ComputerListRow(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderAlpha = if (isActive) 1.0f else 0.5f
    val bgAlpha = if (isActive) 0.25f else 0.12f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CyberCutShape(14f))
            .background(color.copy(alpha = bgAlpha))
            .border(1.dp, color.copy(alpha = borderAlpha), CyberCutShape(14f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = if (isActive) "• ${label.uppercase()}" else label.uppercase(),
            color = color,
            fontFamily = CyberFont,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            fontSize = 11.sp,
            letterSpacing = 0.5.sp
        )
    }
}
