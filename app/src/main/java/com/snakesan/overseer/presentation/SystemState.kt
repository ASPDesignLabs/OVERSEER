package com.snakesan.overseer.presentation

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

fun openPackage(context: Context, packageName: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)

        if (intent != null) {
            context.startActivity(intent)
        }
    } catch (error: Exception) {
        error.printStackTrace()
    }
}

const val KEY_FLUX_ACTIVE = "cached_flux_active"
const val KEY_FLUX_PROFILE = "cached_flux_profile"
const val KEY_FLUX_CUSTOM_BANK = "cached_flux_custom_bank"
const val KEY_FLUX_CUSTOM_NAME = "cached_flux_custom_name"
const val KEY_FLUX_BPM = "cached_flux_bpm"
const val KEY_FLUX_INTENSITY = "cached_flux_intensity"
const val KEY_FLUX_SLEEP = "cached_flux_sleep"

const val KEY_FLUX_MONO = "cached_flux_mono"
const val KEY_FLUX_HDR = "cached_flux_hdr"
const val KEY_FLUX_PRIMARY_COLOR = "cached_flux_primary_color"
const val KEY_FLUX_SECONDARY_COLOR = "cached_flux_secondary_color"
const val KEY_FLUX_L1_COLOR = "cached_flux_l1_color"
const val KEY_FLUX_L2_COLOR = "cached_flux_l2_color"
const val KEY_FLUX_BG_COLOR = "cached_flux_bg_color"

// A class to hold all reactive state in one place
class OverseerState(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var colorHoldSeconds by mutableIntStateOf(
        prefs.getInt(
            KEY_COLOR_HOLD_SECONDS,
            DEFAULT_COLOR_HOLD_SECONDS
        ).coerceIn(
            MIN_COLOR_HOLD_SECONDS,
            MAX_COLOR_HOLD_SECONDS
        )
    )

    fun updateColorHoldSeconds(seconds: Int) {
        colorHoldSeconds = seconds.coerceIn(
            MIN_COLOR_HOLD_SECONDS,
            MAX_COLOR_HOLD_SECONDS
        )

        prefs.edit()
            .putInt(KEY_COLOR_HOLD_SECONDS, colorHoldSeconds)
            .apply()
    }



    // System Stats
    var powerUsage by mutableFloatStateOf(0f)
    var ramUsage by mutableFloatStateOf(0f)
    var batLevel by mutableFloatStateOf(1f)

    var bgMode by mutableIntStateOf(prefs.getInt("cached_bg_mode", 0))
    
    // Vitality
    var hpLevel by mutableIntStateOf(prefs.getInt(KEY_HP_LEVEL, 100))
    var hydStatus by mutableIntStateOf(prefs.getInt(KEY_HYD_STATUS, 1))
    var mealStatus by mutableIntStateOf(prefs.getInt(KEY_MEAL_STATUS, 0))
    var overcharge by mutableIntStateOf(prefs.getInt(KEY_OVERCHARGE, 0))
    var vitalityOffline by mutableStateOf(false)

    // ACK
    var ackDeckName by mutableStateOf(prefs.getString(KEY_DECK_NAME, "ACK") ?: "ACK")
    var ackColorInt by mutableIntStateOf(prefs.getInt(KEY_DECK_COLOR, NeonCyanVal.toArgb()))
    var ackTargetName by mutableStateOf(prefs.getString(KEY_TARGET_NAME, "NONE") ?: "NONE")
    var ackOffline by mutableStateOf(false)
    val targetMap = mutableStateMapOf<Int, String>()

    // Flux
    var fluxMode by mutableStateOf(
        prefs.getString(KEY_FLUX_MODE, "MODE_REACTOR") ?: "MODE_REACTOR"
    )
    var fluxActive by mutableStateOf(prefs.getBoolean(KEY_FLUX_ACTIVE, false))
    var fluxOffline by mutableStateOf(false)

    var fluxProfile by mutableIntStateOf(prefs.getInt(KEY_FLUX_PROFILE, 0))
    var fluxCustomBank by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_CUSTOM_BANK, 1)
    )
    var fluxCustomName by mutableStateOf(
        prefs.getString(KEY_FLUX_CUSTOM_NAME, "CUSTOM_1") ?: "CUSTOM_1"
    )
    var fluxBpm by mutableIntStateOf(prefs.getInt(KEY_FLUX_BPM, 60))
    var fluxIntensity by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_INTENSITY, 50)
    )
    var fluxSleepMode by mutableStateOf(
        prefs.getBoolean(KEY_FLUX_SLEEP, false)
    )

    var fluxMonochrome by mutableStateOf(
        prefs.getBoolean(KEY_FLUX_MONO, false)
    )
    var fluxHdr by mutableStateOf(prefs.getBoolean(KEY_FLUX_HDR, false))

    var fluxPrimaryColor by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_PRIMARY_COLOR, NeonPink.toArgb())
    )
    var fluxSecondaryColor by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_SECONDARY_COLOR, NeonCyanVal.toArgb())
    )
    var fluxL1Color by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_L1_COLOR, NeonDarkVal.toArgb())
    )
    var fluxL2Color by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_L2_COLOR, Color.Gray.toArgb())
    )
    var fluxBgColor by mutableIntStateOf(
        prefs.getInt(KEY_FLUX_BG_COLOR, Color.Black.toArgb())
    )

    init {
        // Load target cache
        val raw = prefs.getString(KEY_TARGET_CACHE, "") ?: ""
        parseTargetsToMap(raw, targetMap)
    }


    fun parseTargetsToMap(raw: String, map: MutableMap<Int, String>) {
        map.clear()
        if (raw.isEmpty()) return
        try {
            raw.split("|").forEach { entry ->
                val parts = entry.split(":")
                if (parts.size == 2) {
                    val idx = parts[0].toIntOrNull()
                    if (idx != null) map[idx] = parts[1]
                }
            }
        } catch (e: Exception) { Log.e("OVERSEER", "Target Parse Error", e) }
    }
}

// Hook for Compose to register receivers
@Composable
fun rememberOverseerState(
    context: Context,
    isActive: Boolean
): OverseerState {
    val state = remember { OverseerState(context) }
    
    // Hardware Polling (RAM/Batt)
    LaunchedEffect(isActive) {
        if (!isActive) {
            return@LaunchedEffect
        }

        while (true) {
            withContext(Dispatchers.IO) {
                state.ramUsage = getRamUsage(context)
                state.batLevel = getBatteryLevel(context)
                state.powerUsage = getPowerUsage(context)
            }

            delay(5_000L)
        }
    }

    DisposableEffect(context, state) {
        val fluxLink = FluxLinkRepository(context, state)
        fluxLink.start()

        onDispose {
            fluxLink.stop()
        }
    }

    // Broadcast Receiver
    DisposableEffect(context) {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    intent?.let {
                        val source = it.getStringExtra("source_app") ?: return
                        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        val editor = prefs.edit()
                        editor.putInt("cached_bg_mode", state.bgMode).apply()

                        when (source) {
                            "VITALITY" -> {
                                state.vitalityOffline = false

                                // 1. Safely extract HP with a fallback to current state
                                state.hpLevel = it.getIntExtra("hp", state.hpLevel)

                                // 2. Safely extract other statuses
                                val inHyd = it.getIntExtra("hyd_status", -1)
                                if (inHyd != -1) state.hydStatus = inHyd

                                val inMeal = it.getIntExtra("meal_status", -1)
                                if (inMeal != -1) state.mealStatus = inMeal

                                // 3. The Overcharge check using -1 as the "missing" flag
                                val inOvercharge = it.getIntExtra("overcharge", -1)
                                if (inOvercharge != -1) {
                                    state.overcharge = inOvercharge
                                }

                                // 4. Hard-enforce the system rule locally:
                                // Overcharge shatters if HP ever drops below 100
                                if (state.hpLevel < 100) {
                                    state.overcharge = 0
                                }

                                editor.putInt(KEY_HP_LEVEL, state.hpLevel)
                                    .putInt(KEY_HYD_STATUS, state.hydStatus)
                                    .putInt(KEY_MEAL_STATUS, state.mealStatus)
                                    .putInt(KEY_OVERCHARGE, state.overcharge).apply()
                            }

                            "ACK" -> {
                                state.ackOffline = false
                                state.ackDeckName =
                                    it.getStringExtra("active_deck") ?: state.ackDeckName
                                state.ackColorInt = it.getIntExtra("deck_color", state.ackColorInt)
                                state.ackTargetName =
                                    it.getStringExtra("active_target") ?: state.ackTargetName
                                editor.putString(KEY_DECK_NAME, state.ackDeckName)
                                    .putInt(KEY_DECK_COLOR, state.ackColorInt)
                                    .putString(KEY_TARGET_NAME, state.ackTargetName).apply()
                            }

                            "ACK_SENSOR" -> {
                                state.ackOffline = false
                                val s = it.getStringExtra("sensor_state") ?: "IDLE"
                                state.ackColorInt = when (s) {
                                    "ARMED" -> Color.Red.toArgb()
                                    "LOCKED" -> Color.Green.toArgb()
                                    "CRYO" -> NeonBlue.toArgb()
                                    else -> prefs.getInt(KEY_DECK_COLOR, NeonCyanVal.toArgb())
                                }
                                state.ackDeckName = when {
                                    s == "CRYO" -> "SLEEP"
                                    s != "IDLE" -> (it.getStringExtra("sensor_pose")
                                        ?: "NONE").takeIf { p -> p != "NONE" } ?: s

                                    else -> prefs.getString(KEY_DECK_NAME, "ACK") ?: "ACK"
                                }
                                state.ackTargetName =
                                    it.getStringExtra("active_target") ?: state.ackTargetName
                            }

                            "ACK_LIST_SYNC" -> {
                                val rawList = it.getStringExtra("raw_targets") ?: ""
                                editor.putString(KEY_TARGET_CACHE, rawList).apply()
                                state.parseTargetsToMap(rawList, state.targetMap)
                            }

                            "FLUX" -> {
                                state.fluxOffline = false

                                state.fluxMode = it.getStringExtra("flux_mode") ?: state.fluxMode
                                state.fluxActive = it.getBooleanExtra("is_active", state.fluxActive)

                                state.fluxProfile = it.getIntExtra(
                                    "flux_profile",
                                    state.fluxProfile
                                )
                                state.fluxCustomBank = it.getIntExtra(
                                    "flux_custom_bank",
                                    state.fluxCustomBank
                                )
                                state.fluxCustomName = it.getStringExtra("flux_custom_name")
                                    ?: state.fluxCustomName

                                state.fluxBpm = it.getIntExtra("flux_bpm", state.fluxBpm)
                                state.fluxIntensity = it.getIntExtra(
                                    "flux_intensity",
                                    state.fluxIntensity
                                )
                                state.fluxSleepMode = it.getBooleanExtra(
                                    "flux_sleep_mode",
                                    state.fluxSleepMode
                                )
                                state.fluxMonochrome = it.getBooleanExtra(
                                    "flux_monochrome",
                                    state.fluxMonochrome
                                )
                                state.fluxHdr = it.getBooleanExtra("flux_hdr", state.fluxHdr)

                                state.fluxPrimaryColor = it.getIntExtra(
                                    "flux_primary_color",
                                    state.fluxPrimaryColor
                                )
                                state.fluxSecondaryColor = it.getIntExtra(
                                    "flux_secondary_color",
                                    state.fluxSecondaryColor
                                )
                                state.fluxL1Color = it.getIntExtra(
                                    "flux_l1_color",
                                    state.fluxL1Color
                                )
                                state.fluxL2Color = it.getIntExtra(
                                    "flux_l2_color",
                                    state.fluxL2Color
                                )
                                state.fluxBgColor = it.getIntExtra(
                                    "flux_bg_color",
                                    state.fluxBgColor
                                )

                                editor
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

                                Log.d(
                                    "OVERSEER_FLUX",
                                    "Flux relay received: active=${state.fluxActive}, " +
                                            "primary=${
                                                state.fluxPrimaryColor.toUInt().toString(16)
                                            }, " +
                                            "secondary=${
                                                state.fluxSecondaryColor.toUInt().toString(16)
                                            }"
                                )
                            }
                        }
                    }
                }
            }
        val filter = IntentFilter().apply {
            addAction(ACTION_UPDATE_STATUS)
            addAction(ACTION_SYNC_TARGETS)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Context.RECEIVER_EXPORTED
        } else {
            0
        }

        context.registerReceiver(receiver, filter, flags)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    return state
}

// --- HARDWARE UTILS ---
fun getBatteryLevel(context: Context): Float {
    val s = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { context.registerReceiver(null, it) }
    val l = s?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val sc = s?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return if (l != -1 && sc != -1) l / sc.toFloat() else 0.5f
}

fun getRamUsage(context: Context): Float {
    val a = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val m = ActivityManager.MemoryInfo()
    a.getMemoryInfo(m)
    return if (m.totalMem == 0L) 0f else (m.totalMem - m.availMem).toFloat() / m.totalMem.toFloat()
}

fun getPowerUsage(context: Context): Float {
    val b = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    return (abs(b.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)) / 1000f / 600f).coerceIn(0f, 1f)
}
