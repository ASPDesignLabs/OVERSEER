package com.snakesan.overseer.presentation

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.snakesan.overseer.R
import kotlin.math.sin

// --- ACTIONS & PACKAGES ---
const val ACTION_UPDATE_STATUS = "com.snakesan.overseer.UPDATE_STATUS"
const val ACTION_KILL_SERVICE = "com.snakesan.overseer.KILL_COMMAND"
const val ACTION_SYNC_TARGETS = "com.snakesan.overseer.SYNC_TARGETS"

const val PKG_VITALITY = "com.snakesan.vitalitysys"
const val PKG_ACK = "com.example.besu"
const val PKG_NEON = "com.snakesan.neonflux"
const val PKG_NEURO = "com.snakesan.cyberquake"

// --- PREFERENCES ---
const val PREFS_NAME = "overseer_cache"
const val KEY_DECK_NAME = "cached_deck_name"
const val KEY_DECK_COLOR = "cached_deck_color"
const val KEY_FLUX_MODE = "cached_flux_mode"
const val KEY_HP_LEVEL = "cached_hp_level"
const val KEY_HYD_STATUS = "cached_hyd_status"
const val KEY_MEAL_STATUS = "cached_meal_status"

const val KEY_OVERCHARGE = "cached_overcharge"
const val KEY_TARGET_NAME = "cached_target_name"
const val KEY_TARGET_CACHE = "cached_target_list_raw"

const val KEY_COLOR_HOLD_SECONDS = "color_hold_seconds"

const val DEFAULT_COLOR_HOLD_SECONDS = 30
const val MIN_COLOR_HOLD_SECONDS = 20
const val MAX_COLOR_HOLD_SECONDS = 30
const val COLOR_HOLD_STEP_SECONDS = 5

// --- PALETTE ---
val NeonCyanVal = Color(0xFF00F3FF)
val NeonPurple = Color(0xFFD500F9)
val NeonRed = Color(0xFFFF003C)
val NeonGreenVal = Color(0xFF00FF41)
val NeonPink = Color(0xFFFF0055)
val NeonOrange = Color(0xFFFF9100) 
val NeonBlue = Color(0xFF2962FF) 
val NeonDarkVal = Color(0xFF121212)

val CyberFont = FontFamily(Font(R.font.cyberfont))

// --- COLOR UTIL ---
fun lerpColor(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f
    )
}

// --- AUDIO ENGINE ---
object MiniSynth {
    private const val SAMPLE_RATE = 44100
    fun playTone(frequency: Float, durationMs: Int) {
        Thread {
            try {
                val numSamples = (SAMPLE_RATE * durationMs) / 1000
                val buffer = FloatArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val signal = sin(2.0 * Math.PI * frequency * t)
                    // Simple envelope to prevent clicking
                    val envelope = if (i < numSamples * 0.2) i / (numSamples * 0.2f) else 1.0f - ((i - numSamples * 0.2f) / (numSamples * 0.8f))
                    buffer[i] = (signal * envelope).toFloat()
                }
                val pcmBuffer = ShortArray(buffer.size)
                for (i in buffer.indices) {
                    pcmBuffer[i] = (buffer[i].coerceIn(-1.0f, 1.0f) * 0.5f * Short.MAX_VALUE).toInt().toShort()
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(pcmBuffer.size * 2)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                    track.write(pcmBuffer, 0, pcmBuffer.size)
                    track.play()
                }
            } catch (e: Exception) { e.printStackTrace() }
        }.start()
    }
}
