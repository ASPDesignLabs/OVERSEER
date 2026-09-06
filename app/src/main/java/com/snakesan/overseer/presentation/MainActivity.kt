package com.snakesan.overseer.presentation

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.sqrt
import androidx.compose.runtime.Composable
import android.os.Handler
import android.os.Looper
import android.view.WindowManager

class MainActivity : ComponentActivity() {

    private val screenHandler = Handler(Looper.getMainLooper())

    private val clearKeepScreenOn = Runnable {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
    private val isAmbientState = mutableStateOf(false)
    private val burnInScale = mutableFloatStateOf(1f)
    private val isResumedState = mutableStateOf(false)

    private fun holdInteractiveDisplay(seconds: Int) {
        val safeSeconds = seconds.coerceIn(
            MIN_COLOR_HOLD_SECONDS,
            MAX_COLOR_HOLD_SECONDS
        )

        screenHandler.removeCallbacks(clearKeepScreenOn)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        screenHandler.postDelayed(
            clearKeepScreenOn,
            safeSeconds * 1_000L
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onResume(owner: LifecycleOwner) {
                    isResumedState.value = true
                }

                override fun onPause(owner: LifecycleOwner) {
                    isResumedState.value = false
                    screenHandler.removeCallbacks(clearKeepScreenOn)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }
        )


        val ambientCallback =
            object : AmbientLifecycleObserver.AmbientLifecycleCallback {
                override fun onEnterAmbient(
                    ambientDetails: AmbientLifecycleObserver.AmbientDetails
                ) {
                    isAmbientState.value = true
                    burnInScale.floatValue = 1f

                    screenHandler.removeCallbacks(clearKeepScreenOn)
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }

                override fun onExitAmbient() {
                    isAmbientState.value = false
                    burnInScale.floatValue = 1f
                }

                override fun onUpdateAmbient() {
                    if (isAmbientState.value) {
                        burnInScale.floatValue = (85..100).random() / 100f
                    }
                }
            }

        lifecycle.addObserver(AmbientLifecycleObserver(this, ambientCallback))

        setContent {
            OverseerTheme {
                MainScreen(
                    isAmbient = isAmbientState.value,
                    burnInScale = burnInScale.floatValue,
                    isResumed = isResumedState.value,
                    onUserInteraction = { seconds ->
                        holdInteractiveDisplay(seconds)
                    }
                )
            }
        }
    }

    @Composable
    fun MainScreen(
        isAmbient: Boolean,
        burnInScale: Float,
        isResumed: Boolean,
        onUserInteraction: (Int) -> Unit
    ) {
        val context = LocalContext.current
        val haptic = LocalHapticFeedback.current

        /*
     * "Active" means the OVERSEER UI is foregrounded and interactive.
     *
     * Background services remain untouched. This only gates expensive UI
     * animation, sensors, and local hardware polling.
     */
        val isInteractive = isResumed && !isAmbient

        val state = rememberOverseerState(
            context = context,
            isActive = isInteractive
        )

        var showAckOverlay by remember { mutableStateOf(false) }
        var showLauncherOverlay by remember { mutableStateOf(false) }
        var emergencyTarget by remember { mutableStateOf<String?>(null) }

        var currentTime by remember {
            mutableLongStateOf(System.currentTimeMillis())
        }

        /*
     * No timer loop while the activity is backgrounded.
     *
     * Active screen: update each second.
     * Ambient screen: update each minute.
     */
        androidx.compose.runtime.LaunchedEffect(isResumed, isAmbient) {
            if (!isResumed) {
                return@LaunchedEffect
            }

            while (true) {
                currentTime = System.currentTimeMillis()

                delay(
                    if (isAmbient) {
                        60_000L
                    } else {
                        1_000L
                    }
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .scale(if (isAmbient) burnInScale else 1f)
                .pointerInput(isInteractive) {
                    detectTapGestures(
                        onTap = { tapOffset ->
                            if (!isInteractive) {
                                return@detectTapGestures
                            }
                            onUserInteraction(state.colorHoldSeconds)

                            val cx = size.width / 2
                            val cy = size.height / 2

                            val dist = sqrt(
                                Math.pow((tapOffset.x - cx).toDouble(), 2.0) +
                                        Math.pow((tapOffset.y - cy).toDouble(), 2.0)
                            )

                            if (dist > (size.width / 2) * 0.35f) {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )

                                var angle = Math.toDegrees(
                                    atan2(
                                        tapOffset.y - cy,
                                        tapOffset.x - cx
                                    ).toDouble()
                                ) + 90

                                if (angle < 0) {
                                    angle += 360
                                }

                                when {
                                    angle <= 120 -> {
                                        launchApp(context, PKG_VITALITY)
                                    }

                                    angle <= 240 -> {
                                        showAckOverlay = true
                                    }

                                    else -> {
                                        launchApp(context, PKG_NEON)
                                    }
                                }
                            } else {
                                haptic.performHapticFeedback(
                                    HapticFeedbackType.LongPress
                                )
                                showLauncherOverlay = true
                            }
                        },
                        onLongPress = { tapOffset ->
                            if (!isInteractive) {
                                return@detectTapGestures
                            }
                            onUserInteraction(state.colorHoldSeconds)

                            haptic.performHapticFeedback(
                                HapticFeedbackType.LongPress
                            )

                            val cx = size.width / 2
                            val cy = size.height / 2

                            val dist = sqrt(
                                Math.pow((tapOffset.x - cx).toDouble(), 2.0) +
                                        Math.pow((tapOffset.y - cy).toDouble(), 2.0)
                            )

                            if (dist <= (size.width / 2) * 0.25f) {
                                emergencyTarget = "ALL"
                            } else {
                                var angle = Math.toDegrees(
                                    atan2(
                                        tapOffset.y - cy,
                                        tapOffset.x - cx
                                    ).toDouble()
                                ) + 90

                                if (angle < 0) {
                                    angle += 360
                                }

                                when {
                                    angle <= 120 -> {
                                        emergencyTarget = PKG_VITALITY
                                    }

                                    angle <= 240 -> {
                                        emergencyTarget = PKG_ACK
                                    }

                                    else -> {
                                        emergencyTarget = PKG_NEON
                                    }
                                }
                            }
                        }
                    )
                }
        ) {
            OverseerGrid(
                isAmbient = isAmbient,
                isActive = isInteractive,
                state = state,
                currentTime = currentTime
            )

            if (showLauncherOverlay) {
                LauncherOverlay(state = state) {
                    showLauncherOverlay = false
                }
            }

            if (showAckOverlay) {
                AckControlOverlay(
                    currentDeck = state.ackDeckName,
                    currentTarget = state.ackTargetName,
                    targetLabels = state.targetMap,
                    currentColor = state.ackColorInt,
                    isCryo = state.ackDeckName == "SLEEP" ||
                            state.ackDeckName == "CRYO",
                    onDismiss = {
                        showAckOverlay = false
                    }
                )
            }

            if (emergencyTarget != null) {
                EmergencyOverlay(
                    target = emergencyTarget!!,
                    onDismiss = {
                        emergencyTarget = null
                    },
                    onConfirm = {
                        performKill(context, emergencyTarget!!)

                        when (emergencyTarget) {
                            PKG_VITALITY -> state.vitalityOffline = true
                            PKG_ACK -> state.ackOffline = true
                            PKG_NEON -> state.fluxOffline = true
                            "ALL" -> {
                                state.vitalityOffline = true
                                state.ackOffline = true
                                state.fluxOffline = true
                            }
                        }

                        emergencyTarget = null
                    }
                )
            }
        }
    }

    fun performKill(context: Context, target: String) {
        val intent = Intent(ACTION_KILL_SERVICE).apply {
            putExtra("target_package", target)
            setPackage(if (target == "ALL") null else target)
        }

        context.sendBroadcast(intent)
    }

    fun launchApp(context: Context, packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(
                packageName
            )

            if (intent != null) {
                context.startActivity(intent)
            } else {
                Log.e("OVERSEER", "App not found: $packageName")
            }
        } catch (error: Exception) {
            error.printStackTrace()
        }
    }
}