package com.snakesan.overseermobile.data

import androidx.compose.ui.graphics.Color

/** The three built-in behaviors the watch face already knows how to render. */
enum class WedgeFunction(val displayName: String) {
    VITALITY("VITALITY"),
    ACK("ACK"),
    FLUX("FLUX");
}

/**
 * What a wedge is currently doing. A [Function] wedge preserves one of the
 * app's built-in telemetry displays and keeps its watch-driven dynamic
 * color. A [Shortcut] wedge launches an arbitrary app with a user-chosen
 * label and color.
 */
sealed class WedgeContent {
    data class Function(val function: WedgeFunction) : WedgeContent()

    data class Shortcut(
        val packageName: String,
        val label: String,
        val color: Color
    ) : WedgeContent()
}

/** One of the three fixed positions on the watch ring. Position is 0..2. */
data class WedgeSlot(
    val position: Int,
    val content: WedgeContent
)

data class OverseerMobileConfig(
    val wedges: List<WedgeSlot>
) {
    init {
        require(wedges.size == 3) { "OVERSEER always configures exactly 3 wedges" }
    }

    fun slotHolding(function: WedgeFunction): WedgeSlot? =
        wedges.firstOrNull { (it.content as? WedgeContent.Function)?.function == function }

    fun withSlot(position: Int, content: WedgeContent): OverseerMobileConfig =
        copy(wedges = wedges.map { if (it.position == position) it.copy(content = content) else it })

    /**
     * Assigns [function] to [position]. Only one wedge may hold a given
     * function at a time, so if it's currently deployed elsewhere, that
     * wedge trades contents with [position] instead of being left empty.
     */
    fun withFunctionAssigned(position: Int, function: WedgeFunction): OverseerMobileConfig {
        val existing = slotHolding(function) ?: return withSlot(position, WedgeContent.Function(function))
        if (existing.position == position) return this

        val displaced = wedges.first { it.position == position }.content
        return copy(
            wedges = wedges.map {
                when (it.position) {
                    position -> it.copy(content = WedgeContent.Function(function))
                    existing.position -> it.copy(content = displaced)
                    else -> it
                }
            }
        )
    }

    companion object {
        /** Mirrors the watch's current hardcoded layout exactly. */
        fun default() = OverseerMobileConfig(
            wedges = listOf(
                WedgeSlot(0, WedgeContent.Function(WedgeFunction.VITALITY)),
                WedgeSlot(1, WedgeContent.Function(WedgeFunction.ACK)),
                WedgeSlot(2, WedgeContent.Function(WedgeFunction.FLUX))
            )
        )
    }
}
