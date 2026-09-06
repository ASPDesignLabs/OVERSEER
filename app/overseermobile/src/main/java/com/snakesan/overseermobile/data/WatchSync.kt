package com.snakesan.overseermobile.data

import android.content.Context
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

private const val PATH_WEDGE_CONFIG = "/overseer/wedge_config"
private const val SCHEMA_VERSION = 1

private const val TYPE_FUNCTION = "FUNCTION"
private const val TYPE_SHORTCUT = "SHORTCUT"

/**
 * Pushes the current wedge configuration to the watch over the Wearable
 * Data Layer. The watch does not yet read this path — this defines the wire
 * format it will eventually consume.
 */
object WatchSync {
    suspend fun push(context: Context, config: OverseerMobileConfig): Result<Unit> {
        return try {
            val request = PutDataMapRequest.create(PATH_WEDGE_CONFIG).apply {
                dataMap.putInt("schema_version", SCHEMA_VERSION)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putDataMapArrayList("wedges", ArrayList(config.wedges.map(::toDataMap)))
            }

            Wearable.getDataClient(context)
                .putDataItem(request.asPutDataRequest().setUrgent())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun toDataMap(slot: WedgeSlot): DataMap = DataMap().apply {
        putInt("position", slot.position)
        when (val content = slot.content) {
            is WedgeContent.Function -> {
                putString("type", TYPE_FUNCTION)
                putString("function", content.function.name)
            }

            is WedgeContent.Shortcut -> {
                putString("type", TYPE_SHORTCUT)
                putString("package_name", content.packageName)
                putString("label", content.label)
                putInt("color_argb", content.color.toArgb())
            }
        }
    }
}
