package com.snakesan.overseermobile.data

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "overseermobile_config"
private const val KEY_CONFIG_JSON = "wedge_config_json"
private const val KEY_LAST_SYNC = "last_sync_millis"

private const val TYPE_FUNCTION = "FUNCTION"
private const val TYPE_SHORTCUT = "SHORTCUT"

/** Persists the wedge configuration locally on the phone (not the watch). */
class ConfigRepository(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): OverseerMobileConfig {
        val raw = prefs.getString(KEY_CONFIG_JSON, null) ?: return OverseerMobileConfig.default()
        return try {
            parse(raw)
        } catch (e: Exception) {
            Log.e("OVERSEER_MOBILE", "Failed to parse saved config, using defaults", e)
            OverseerMobileConfig.default()
        }
    }

    fun save(config: OverseerMobileConfig) {
        prefs.edit().putString(KEY_CONFIG_JSON, serialize(config)).apply()
    }

    fun lastSyncMillis(): Long? = prefs.getLong(KEY_LAST_SYNC, -1L).takeIf { it > 0 }

    fun markSynced(millis: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, millis).apply()
    }

    private fun serialize(config: OverseerMobileConfig): String {
        val wedgesArray = JSONArray()
        config.wedges.forEach { slot ->
            val obj = JSONObject().put("position", slot.position)
            when (val content = slot.content) {
                is WedgeContent.Function -> obj
                    .put("type", TYPE_FUNCTION)
                    .put("function", content.function.name)

                is WedgeContent.Shortcut -> obj
                    .put("type", TYPE_SHORTCUT)
                    .put("package_name", content.packageName)
                    .put("label", content.label)
                    .put("color_argb", content.color.toArgb())
            }
            wedgesArray.put(obj)
        }
        return JSONObject().put("wedges", wedgesArray).toString()
    }

    private fun parse(raw: String): OverseerMobileConfig {
        val wedgesArray = JSONObject(raw).getJSONArray("wedges")
        val slots = (0 until wedgesArray.length()).map { i ->
            val obj = wedgesArray.getJSONObject(i)
            val position = obj.getInt("position")
            val content = when (obj.getString("type")) {
                TYPE_FUNCTION -> WedgeContent.Function(WedgeFunction.valueOf(obj.getString("function")))
                TYPE_SHORTCUT -> WedgeContent.Shortcut(
                    packageName = obj.getString("package_name"),
                    label = obj.getString("label"),
                    color = Color(obj.getInt("color_argb"))
                )
                else -> error("Unknown wedge type: ${obj.getString("type")}")
            }
            WedgeSlot(position, content)
        }
        return OverseerMobileConfig(slots.sortedBy { it.position })
    }
}
