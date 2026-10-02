package com.snakesan.overseer.presentation

import android.util.Log
import org.json.JSONArray

// Mirrors ACK Wear's own copy of this DTO
// (wear/src/main/java/com/snakesan/wear/presentation/ComputerCategoryCache.kt in the
// ACK repo) field-for-field -- this is what ACK Wear relays onward as raw JSON via
// the SYNC_COMPUTER broadcast (see relayComputerCategoriesToOverseer on that side).
// OVERSEER has no kotlinx.serialization dependency (unlike ACK/ACK Wear); every other
// synced payload here is hand-parsed already (OverseerState.parseTargetsToMap,
// FluxLinkRepository's manual ByteBuffer parsing), so this uses org.json -- built
// into the Android SDK -- instead of adding a new Gradle plugin/dependency for it.
data class SyncedComputerNode(
    val id: String,
    val label: String,
    val isCategory: Boolean,
    val parentId: String
)

data class SyncedComputerCategory(
    val id: String,
    val label: String,
    val nodes: List<SyncedComputerNode>,
    // Null/blank means no active pick, same meaning as the phone-side field
    // it mirrors (computer/ComputerModels.kt's ComputerCategory.activeNodeId).
    val activeNodeId: String?
)

// Direct children of parentId within this category's tree ("" = its own top
// level), in the original phone-side order -- mirrors ComputerCategoryCache.childrenOf.
fun SyncedComputerCategory.childrenOf(parentId: String): List<SyncedComputerNode> =
    nodes.filter { it.parentId == parentId }

fun SyncedComputerCategory.activeNodeLabel(): String? {
    val activeId = activeNodeId?.takeIf { it.isNotBlank() } ?: return null
    return nodes.find { it.id == activeId }?.label
}

fun parseComputerCategories(raw: String): Map<String, SyncedComputerCategory> {
    if (raw.isBlank()) return emptyMap()

    return try {
        val array = JSONArray(raw)
        val result = LinkedHashMap<String, SyncedComputerCategory>()

        for (i in 0 until array.length()) {
            val categoryObj = array.getJSONObject(i)
            val nodesArray = categoryObj.optJSONArray("nodes") ?: JSONArray()
            val nodes = mutableListOf<SyncedComputerNode>()

            for (j in 0 until nodesArray.length()) {
                val nodeObj = nodesArray.getJSONObject(j)
                nodes.add(
                    SyncedComputerNode(
                        id = nodeObj.getString("id"),
                        label = nodeObj.getString("label"),
                        isCategory = nodeObj.getBoolean("isCategory"),
                        parentId = nodeObj.getString("parentId")
                    )
                )
            }

            val activeNodeId = if (categoryObj.has("activeNodeId") && !categoryObj.isNull("activeNodeId")) {
                categoryObj.getString("activeNodeId")
            } else {
                null
            }

            val category = SyncedComputerCategory(
                id = categoryObj.getString("id"),
                label = categoryObj.getString("label"),
                nodes = nodes,
                activeNodeId = activeNodeId
            )
            result[category.id] = category
        }

        result
    } catch (e: Exception) {
        Log.e("OVERSEER_COMPUTER", "Failed to parse computer categories", e)
        emptyMap()
    }
}
