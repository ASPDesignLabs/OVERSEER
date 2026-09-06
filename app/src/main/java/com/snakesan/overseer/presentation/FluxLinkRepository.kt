package com.snakesan.overseer.presentation

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.nio.ByteBuffer

private const val PATH_FLUX_OVERSEER_STATE = "/flux_overseer_state"

class FluxLinkRepository(
    private val context: Context,
    private val state: OverseerState
) : MessageClient.OnMessageReceivedListener {

    private val messageClient = Wearable.getMessageClient(context)

    fun start() {
        messageClient.addListener(this)
    }

    fun stop() {
        messageClient.removeListener(this)
    }

    override fun onMessageReceived(event: MessageEvent) {
        if (event.path != PATH_FLUX_OVERSEER_STATE) {
            return
        }

        try {
            applyFluxState(event.data)
        } catch (error: Exception) {
            Log.e("OVERSEER_FLUX", "Invalid Flux state packet", error)
        }
    }

    private fun applyFluxState(payload: ByteArray) {
        val buffer = ByteBuffer.wrap(payload)

        val minimumPacketSize = 33
        if (buffer.remaining() < minimumPacketSize) {
            Log.w(
                "OVERSEER_FLUX",
                "Ignoring short Flux packet: ${payload.size} bytes"
            )
            return
        }

        val version = buffer.get().toInt()
        if (version != 1) {
            Log.w("OVERSEER_FLUX", "Unsupported Flux protocol: $version")
            return
        }

        val isRunning = buffer.get().toInt() == 1
        val profile = buffer.get().toInt()
        val customBank = buffer.get().toInt()
        val bpm = buffer.int
        val intensity = buffer.get().toInt()
        val sleepMode = buffer.get().toInt() == 1
        val monochrome = buffer.get().toInt() == 1
        val hdr = buffer.get().toInt() == 1

        val primaryColor = buffer.int
        val secondaryColor = buffer.int
        val layer1Color = buffer.int
        val layer2Color = buffer.int
        val backgroundColor = buffer.int

        val nameLength = buffer.get().toInt() and 0xFF
        if (nameLength > buffer.remaining()) {
            Log.w("OVERSEER_FLUX", "Invalid Flux custom-name length")
            return
        }

        val nameBytes = ByteArray(nameLength)
        buffer.get(nameBytes)

        val customName = nameBytes.decodeToString()
            .ifBlank { "CUSTOM_$customBank" }

        state.fluxOffline = false
        state.fluxActive = isRunning
        state.fluxProfile = profile
        state.fluxCustomBank = customBank
        state.fluxCustomName = customName
        state.fluxBpm = bpm
        state.fluxIntensity = intensity
        state.fluxSleepMode = sleepMode
        state.fluxMonochrome = monochrome
        state.fluxHdr = hdr
        state.fluxPrimaryColor = primaryColor
        state.fluxSecondaryColor = secondaryColor
        state.fluxL1Color = layer1Color
        state.fluxL2Color = layer2Color
        state.fluxBgColor = backgroundColor

        state.fluxMode = when (profile) {
            0 -> "MODE_PULSE"
            1 -> "MODE_GEIGER"
            2 -> "MODE_THROB"
            3 -> "MODE_CUSTOM"
            else -> "MODE_UNKNOWN"
        }

        persist()
    }

    private fun persist() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FLUX_MODE, state.fluxMode)
            .putBoolean(KEY_FLUX_ACTIVE, state.fluxActive)
            .putInt(KEY_FLUX_PROFILE, state.fluxProfile)
            .putInt(KEY_FLUX_CUSTOM_BANK, state.fluxCustomBank)
            .putString(KEY_FLUX_CUSTOM_NAME, state.fluxCustomName)
            .putInt(KEY_FLUX_BPM, state.fluxBpm)
            .putInt(KEY_FLUX_INTENSITY, state.fluxIntensity)
            .putBoolean(KEY_FLUX_SLEEP, state.fluxSleepMode)
            .putBoolean(KEY_FLUX_MONO, state.fluxMonochrome)
            .putBoolean(KEY_FLUX_HDR, state.fluxHdr)
            .putInt(KEY_FLUX_PRIMARY_COLOR, state.fluxPrimaryColor)
            .putInt(KEY_FLUX_SECONDARY_COLOR, state.fluxSecondaryColor)
            .putInt(KEY_FLUX_L1_COLOR, state.fluxL1Color)
            .putInt(KEY_FLUX_L2_COLOR, state.fluxL2Color)
            .putInt(KEY_FLUX_BG_COLOR, state.fluxBgColor)
            .apply()
    }
}
