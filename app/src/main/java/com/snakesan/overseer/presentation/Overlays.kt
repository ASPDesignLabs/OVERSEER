package com.snakesan.overseer.presentation

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Text
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape

// --- LAUNCHER OVERLAY ---
@Composable
fun LauncherOverlay(
    state: OverseerState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(
        Context.VIBRATOR_SERVICE
    ) as Vibrator

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                }
            ) {
                onDismiss()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "> OPTIONS",
                style = TextStyle(
                    fontFamily = CyberFont,
                    color = NeonGreenVal,
                    fontSize = 12.sp,
                    letterSpacing = 2.sp
                ),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                LauncherButton(
                    label = "NEURO",
                    color = NeonCyanVal
                ) {
                    openPackage(context, PKG_NEURO)
                    onDismiss()
                }

                Spacer(modifier = Modifier.width(16.dp))

                LauncherButton(
                    label = "CYCLE BG",
                    color = NeonPurple
                ) {
                    state.bgMode = (state.bgMode + 1) % 3

                    context.getSharedPreferences(
                        PREFS_NAME,
                        Context.MODE_PRIVATE
                    ).edit()
                        .putInt("cached_bg_mode", state.bgMode)
                        .apply()
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            ColorHoldSlider(
                seconds = state.colorHoldSeconds,
                onSecondsChanged = state::updateColorHoldSeconds,
                onStepChanged = {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            15L,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )

                    MiniSynth.playTone(1_100f, 24)
                }
            )
        }
    }
}

@Composable
fun LauncherButton(label: String, color: Color, onClick: () -> Unit) {
    // Used inside LauncherOverlay
    CyberButton(
        onClick = onClick,
        color = color,
        modifier = Modifier.size(width = 80.dp, height = 50.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(6.dp).background(color, shape = CircleShape))
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label, 
                color = color, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold, 
                fontFamily = CyberFont,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun ColorHoldSlider(
    seconds: Int,
    onSecondsChanged: (Int) -> Unit,
    onStepChanged: () -> Unit
) {
    val steps = remember {
        (MIN_COLOR_HOLD_SECONDS..MAX_COLOR_HOLD_SECONDS step
                COLOR_HOLD_STEP_SECONDS).toList()
    }

    val selectedIndex = steps.indexOf(seconds)
        .coerceAtLeast(0)

    fun setFromFraction(fraction: Float) {
        val rawIndex = (
                fraction.coerceIn(0f, 1f) * (steps.size - 1)
                ).roundToInt()

        val nextSeconds = steps[rawIndex]

        if (nextSeconds != seconds) {
            onSecondsChanged(nextSeconds)
            onStepChanged()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(170.dp)
            .clickable(
                indication = null,
                interactionSource = remember {
                    MutableInteractionSource()
                }
            ) {}
    ) {
        Text(
            text = "FULL COLOR HOLD",
            color = NeonOrange,
            fontFamily = CyberFont,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 1.sp
        )

        Text(
            text = "$seconds SEC",
            color = Color.White,
            fontFamily = CyberFont,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        setFromFraction(
                            offset.x / size.width.toFloat()
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            setFromFraction(
                                offset.x / size.width.toFloat()
                            )
                        },
                        onDrag = { change, _ ->
                            setFromFraction(
                                change.position.x / size.width.toFloat()
                            )
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                val centerY = size.height / 2f
                val startX = 4f
                val endX = size.width - 4f
                val segmentWidth = (endX - startX) / (steps.size - 1)

                drawLine(
                    color = Color.DarkGray,
                    start = Offset(startX, centerY),
                    end = Offset(endX, centerY),
                    strokeWidth = 3f
                )

                val selectedX = startX + segmentWidth * selectedIndex

                drawLine(
                    color = NeonOrange,
                    start = Offset(startX, centerY),
                    end = Offset(selectedX, centerY),
                    strokeWidth = 3f
                )

                steps.forEachIndexed { index, value ->
                    val x = startX + segmentWidth * index
                    val isSelected = index == selectedIndex
                    val isPassed = index <= selectedIndex

                    drawCircle(
                        color = when {
                            isSelected -> Color.White
                            isPassed -> NeonOrange
                            else -> Color.DarkGray
                        },
                        radius = if (isSelected) 6f else 4f,
                        center = Offset(x, centerY)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "5",
                color = Color.Gray,
                fontFamily = CyberFont,
                fontSize = 8.sp
            )

            Text(
                text = "30 SEC",
                color = Color.Gray,
                fontFamily = CyberFont,
                fontSize = 8.sp
            )
        }

        Text(
            text = "SYSTEM MAY OVERRIDE",
            color = Color.DarkGray,
            fontFamily = CyberFont,
            fontSize = 7.sp,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// --- EMERGENCY OVERLAY ---
@Composable
fun EmergencyOverlay(target: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val targetName = if (target == "ALL") "SYSTEM" else target.substringAfterLast('.')
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled=false){}, // absorb clicks
        contentAlignment = Alignment.Center
    ) {
        // Warning stripes
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stripeWidth = 20f
            val gap = 20f
            for(i in -10..20) {
                val x = i * (stripeWidth + gap)
                drawLine(
                    color = NeonRed.copy(alpha=0.1f),
                    start = Offset(x, 0f),
                    end = Offset(x - 100f, size.height),
                    strokeWidth = stripeWidth
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
            Text("// EMERGENCY STOP //", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = CyberFont)
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("TERMINATE:", color = Color.Gray, fontSize = 10.sp, fontFamily = CyberFont)
            Text(targetName, color = Color.White, fontSize = 16.sp, fontFamily = CyberFont, fontWeight = FontWeight.Bold)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CyberButton(
                    onClick = { onDismiss() },
                    color = Color.Gray,
                    modifier = Modifier.height(40.dp).width(70.dp)
                ) {
                    Text("ABORT", fontSize = 10.sp, color = Color.White, fontFamily = CyberFont)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                CyberButton(
                    onClick = { onConfirm() },
                    color = NeonRed,
                    modifier = Modifier.height(40.dp).width(70.dp)
                ) {
                    Text("EXECUTE", fontSize = 10.sp, color = NeonRed, fontFamily = CyberFont, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- ACK CONTROL OVERLAY ---
@Composable
fun AckControlOverlay(
    currentDeck: String,
    currentTarget: String,
    targetLabels: Map<Int, String>,
    currentColor: Int,
    isCryo: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    val focusRequester = remember { FocusRequester() }

    var controlMode by remember { mutableStateOf("DECK") }
    var localTargetIndex by remember { mutableIntStateOf(0) }
    val primaryColor = if(controlMode == "DECK") Color(currentColor) else NeonGreenVal
    var scrollAccumulator by remember { mutableFloatStateOf(0f) }
    val crownThreshold = 50f

    // Helper to send commands to Besu (ACK)
    fun sendCmd(cmd: String) {
        val intent = Intent("com.snakesan.overseer.ACK_CONTROL")
        intent.putExtra("CMD", cmd)
        intent.setPackage(PKG_ACK)
        context.sendBroadcast(intent)
    }

    // Helper for UI sounds
    fun playFeedback(mode: String, index: Int) {
        if (mode == "DECK") MiniSynth.playTone(2000f, 30)
        else {
            val freq = if (index >= 8) 1500f else 400f + (index * 100f)
            MiniSynth.playTone(freq, 40)
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .onRotaryScrollEvent {
                scrollAccumulator += it.verticalScrollPixels
                if (abs(scrollAccumulator) > crownThreshold) {
                    val direction = if (scrollAccumulator > 0) 1 else -1
                    scrollAccumulator = 0f

                    if (controlMode == "DECK") {
                        val cmd = if(direction > 0) "NEXT_DECK" else "PREV_DECK"
                        sendCmd(cmd)
                        playFeedback("DECK", 0)
                    } else {
                        val next = localTargetIndex + direction
                        localTargetIndex = if(next > 8) 0 else if(next < 0) 8 else next
                        
                        val payload = if(localTargetIndex == 8) -1 else localTargetIndex
                        sendCmd("SET_TARGET:$payload")
                        playFeedback("TARGET", localTargetIndex)
                    }
                    vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                }
                true
            }
            .focusRequester(focusRequester)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        val midX = size.width / 2
                        val isRight = offset.x > midX
                        val direction = if(isRight) 1 else -1

                        if (offset.y < size.height * 0.7f) {
                            if (controlMode == "DECK") {
                                val cmd = if(direction > 0) "NEXT_DECK" else "PREV_DECK"
                                sendCmd(cmd)
                                playFeedback("DECK", 0)
                            } else {
                                val next = localTargetIndex + direction
                                localTargetIndex = if(next > 8) 0 else if(next < 0) 8 else next
                                val payload = if(localTargetIndex == 8) -1 else localTargetIndex
                                sendCmd("SET_TARGET:$payload")
                            }
                            vibrator.vibrate(VibrationEffect.createOneShot(10, VibrationEffect.DEFAULT_AMPLITUDE))
                        }
                    },
                    onLongPress = {
                        controlMode = if(controlMode == "DECK") "TARGET" else "DECK"
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                        MiniSynth.playTone(1800f, 80)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 20f
            for(i in 0..10) {
                val x = i * step + 30
                drawLine(primaryColor.copy(alpha=0.1f), Offset(x, 0f), Offset(x, size.height), 1f)
            }
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 4.dp)
                        .background(if(controlMode=="DECK") primaryColor else Color.DarkGray)
                        .border(0.5.dp, primaryColor, RectangleShape) 
                )
                Spacer(modifier = Modifier.width(4.dp))
                
                Text(
                    if(controlMode=="DECK") "DECK // CONTROL" else "TARGET // LINK", 
                    color = primaryColor, 
                    fontSize = 10.sp, 
                    fontFamily = CyberFont,
                    letterSpacing = 1.sp
                )
                
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 8.dp, height = 4.dp)
                        .background(if(controlMode=="TARGET") primaryColor else Color.DarkGray)
                        .border(0.5.dp, primaryColor, RectangleShape)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (controlMode == "DECK") {
                Text(
                    text = if(isCryo) "SLEEPING" else currentDeck,
                    style = TextStyle(
                        color = primaryColor, 
                        fontSize = 24.sp, 
                        fontWeight = FontWeight.Bold, 
                        fontFamily = CyberFont, 
                        textAlign = TextAlign.Center,
                        letterSpacing = 2.sp
                    ),
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .border(1.dp, primaryColor.copy(alpha=0.3f), CyberCutShape(10f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            } else {
                val isClear = localTargetIndex == 8
                val rawLabel = targetLabels[localTargetIndex]
                val nameText = if (isClear) "CLEAR" else (rawLabel ?: "EMPTY")
                val subText = if (isClear) "DISENGAGE" else "SLOT [ ${localTargetIndex + 1} ]"

                Text(
                    text = nameText.uppercase(), 
                    style = TextStyle(
                        color = NeonGreenVal, 
                        fontSize = if(nameText.length > 8) 20.sp else 28.sp, 
                        fontWeight = FontWeight.Black, 
                        fontFamily = CyberFont, 
                        textAlign = TextAlign.Center
                    )
                )
                Text(
                    text = subText, 
                    color = Color.Gray, 
                    fontSize = 10.sp, 
                    fontFamily = CyberFont, 
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                if (currentTarget != "NONE" && currentTarget != "-1") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "LINKED: $currentTarget", 
                        color = Color.DarkGray, 
                        fontSize = 8.sp, 
                        fontFamily = CyberFont
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.width(100.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("<", color = primaryColor.copy(alpha=0.5f), fontFamily = CyberFont)
                Text(">", color = primaryColor.copy(alpha=0.5f), fontFamily = CyberFont)
            }
        }

        Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).clickable { onDismiss() }) {
            Text("▼", color = Color.Gray.copy(alpha=0.5f), fontSize = 10.sp)
        }

        val cryoColor = if(isCryo) NeonBlue else NeonRed
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)) {
            CyberButton(
                onClick = { 
                    sendCmd("CRYO_TOGGLE")
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    MiniSynth.playTone(150f, 200)
                },
                color = cryoColor,
                modifier = Modifier.height(35.dp).width(90.dp)
            ) {
                 Text(
                     if(isCryo) "WAKE SYS" else "INIT CRYO", 
                     color = if(isCryo) NeonBlue else Color.White, 
                     fontSize = 10.sp, 
                     fontWeight = FontWeight.Bold, 
                     fontFamily = CyberFont
                 )
            }
        }
    }
}
