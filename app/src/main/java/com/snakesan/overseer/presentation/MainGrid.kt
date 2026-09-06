package com.snakesan.overseer.presentation

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.CurvedLayout
import androidx.wear.compose.foundation.CurvedModifier
import androidx.wear.compose.foundation.curvedColumn
import androidx.wear.compose.foundation.curvedRow
import androidx.wear.compose.foundation.padding
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.curvedText
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val ENABLE_PCB_LAYOUT = false

@Composable
fun OverseerGrid(
    isAmbient: Boolean,
    isActive: Boolean,
    state: OverseerState,
    currentTime: Long
) {
    val shouldAnimate = isActive && !isAmbient

    val renderedBgMode = when {
        !ENABLE_PCB_LAYOUT && state.bgMode == 1 -> 0
        else -> state.bgMode
    }
    // Animation Setup
    val infiniteTransition = rememberInfiniteTransition(
        label = "grid_animations"
    )

    val gridOffsetSlow by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 40f,
            animationSpec = infiniteRepeatable(
                animation = tween(8_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "grid_offset_slow"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val gridOffsetFast by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 40f,
            animationSpec = infiniteRepeatable(
                animation = tween(3_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "grid_offset_fast"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val pulseAlpha by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing,
                    delayMillis = 100
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse_alpha"
        )
    } else {
        remember { mutableFloatStateOf(1f) }
    }

    val timeFloat by if (shouldAnimate) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1000f,
            animationSpec = infiniteRepeatable(
                animation = tween(100_000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "grid_time"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val NeonGold = Color(0xFFFFD700)
    val rawHpColor = when {
        state.overcharge >= 50 -> NeonGold
        state.overcharge >= 25 -> NeonCyanVal
        state.hpLevel > 75 -> NeonGreenVal
        state.hpLevel > 50 -> NeonOrange
        else -> NeonRed
    }

    val currentHpColor = if (isAmbient) {
        if (state.vitalityOffline) Color.Transparent else Color.Gray
    } else {
        if (state.vitalityOffline) Color.DarkGray
        else if (state.hpLevel <= 50) rawHpColor.copy(alpha = pulseAlpha)
        else rawHpColor
    }

    val currentAckColor = if (isAmbient) {
        if (state.ackOffline) Color.Transparent else Color.Gray
    } else {
        if (state.ackOffline) Color.DarkGray else Color(state.ackColorInt)
    }

    val activeFluxColor = Color(state.fluxPrimaryColor)

    val standbyFluxColor = if (state.fluxMonochrome) {
        Color(state.fluxL2Color)
    } else {
        Color(state.fluxSecondaryColor)
    }

    val currentFluxColor = if (isAmbient) {
        if (state.fluxOffline) {
            Color.Transparent
        } else {
            Color.Gray
        }
    } else {
        when {
            state.fluxOffline -> Color.DarkGray
            state.fluxActive -> activeFluxColor
            else -> standbyFluxColor.copy(alpha = 0.6f)
        }
    }

    // Sensors
    val context = LocalContext.current
    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }

    if (shouldAnimate) {
        DisposableEffect(Unit) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)

            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    tiltX = event.values[0]
                    tiltY = event.values[1]
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            sensorManager.registerListener(
                listener,
                sensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    // Depth Effects
    // Depth Effects
//
// Parallax belongs exclusively to the PCB experience. Grid and starfield
// keep the clock and internal meters locked to the face center.
    val isPcbMode = shouldAnimate && renderedBgMode == 1

    val bgParallaxX = if (isPcbMode) {
        -tiltX * 1.5f
    } else {
        0f
    }

    val bgParallaxY = if (isPcbMode) {
        -tiltY * 1.5f
    } else {
        0f
    }

    val fgParallaxX = if (isPcbMode) {
        tiltX * 2.5f
    } else {
        0f
    }

    val fgParallaxY = if (isPcbMode) {
        tiltY * 2.5f
    } else {
        0f
    }

    val shadowOffsetX = if (isPcbMode) {
        tiltX * 4f
    } else {
        0f
    }

    val shadowOffsetY = if (isPcbMode) {
        tiltY * 4f
    } else {
        0f
    }

    val calendar = Calendar.getInstance().apply { timeInMillis = currentTime }
    val timeString = String.format(Locale.US, "%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    val secString = String.format(Locale.US, "%02d", calendar.get(Calendar.SECOND))

    val rainbowShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader("""
                uniform float2 resolution;
                uniform float time;
                uniform float2 tilt;
                uniform float currentTranslate;
                
                half3 hsv2rgb(half3 c) {
                    half4 K = half4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
                    half3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
                    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
                }
                
                half4 main(float2 fragCoord) {
                    float2 screenCoord = fragCoord;
                    screenCoord.x += currentTranslate;
                    float2 uv = screenCoord / resolution;
                    
                    float2 center = float2(0.5, 0.5) + (tilt * 0.12);
                    float2 cUv = uv - center;
                    float2 rUv = cUv; 
                    
                    float hue = (rUv.x * 0.4); 
                    half3 baseColor = hsv2rgb(half3(hue, 1.0, 0.5));
                    
                    return half4(baseColor, 0.7);
                }
            """.trimIndent())
        } else null
    }

    val shimmerShader = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            RuntimeShader("""
                uniform float2 resolution;
                uniform float2 tilt;
                uniform float currentTranslate;
                uniform float time;
                
                half4 main(float2 fragCoord) {
                    float2 screenCoord = fragCoord;
                    screenCoord.x += currentTranslate;
                    float2 uv = screenCoord / resolution;
                    
                    float2 sCenter = float2(0.5, 0.5) - (tilt * 0.08);
                    
                    float sAngle = 0.785 + (tilt.x * 0.06); 
                    float ss = sin(sAngle);
                    float sc = cos(sAngle);
                    float2 sCUv = uv - sCenter;
                    float2 sRUv = float2(sCUv.x * sc - sCUv.y * ss, sCUv.x * ss + sCUv.y * sc);
                    
                    float sAngle2 = -0.5 - (tilt.x * 0.04); 
                    float ss2 = sin(sAngle2);
                    float sc2 = cos(sAngle2);
                    float2 sRUv2 = float2(sCUv.x * sc2 - sCUv.y * ss2, sCUv.x * ss2 + sCUv.y * sc2);
                    
                    float beam1 = 1.0 - smoothstep(0.0, 0.25, abs(sRUv.x));
                    float beam2 = 1.0 - smoothstep(0.0, 0.4, abs(sRUv2.x + sin(time * 0.05) * 0.2));
                    
                    float caustic = beam1 * (0.4 + (1.0 - beam2) * 0.6);
                    caustic *= 0.95; 
                    
                    return half4(caustic, caustic, caustic, caustic * 0.8);
                }
            """.trimIndent())
        } else null
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ==========================================
        // LAYER 1: BACKGROUND (Blurred PCB Base)
        // ==========================================
        val colorLayerBlurRadius = 1.dp

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (!isAmbient && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && colorLayerBlurRadius > 0.dp) {
                        Modifier.blur(colorLayerBlurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                    } else Modifier
                )
        ) {
            val w = size.width
            val h = size.height
            val cx = w / 2 + bgParallaxX
            val cy = h / 2 + bgParallaxY

            if (!isAmbient) {
                drawCircle(color = NeonDarkVal)
                when (renderedBgMode) {
                    0 -> {
                        val g = 40f
                        for (i in -1..12) {
                            val p = (i * g) + gridOffsetSlow
                            val x = i * g
                            drawLine(Color.DarkGray.copy(0.25f), Offset(x, 0f), Offset(x, h), 1f)
                            drawLine(Color.DarkGray.copy(0.25f), Offset(0f, p), Offset(w, p), 1f)
                        }
                        for (i in -2..12) {
                            val m = (i * g) + gridOffsetFast
                            drawLine(NeonCyanVal.copy(0.2f), Offset(m, 0f), Offset(m, h), 1f)
                            drawLine(NeonCyanVal.copy(0.2f), Offset(0f, m), Offset(w, m), 1f)
                        }
                    }
                    1 -> {
                        val traceDensityMultiplier = 1.5f
                        val localPcbPath = Path()

                        if (w > 0f && h > 0f) {
                            if (PcbPathCache.cachedBasePath != null && PcbPathCache.cachedTraces != null &&
                                PcbPathCache.lastW == w && PcbPathCache.lastH == h && PcbPathCache.lastDensity == traceDensityMultiplier) {
                                localPcbPath.addPath(PcbPathCache.cachedBasePath!!)
                            } else {
                                val traceCount = (25 * traceDensityMultiplier).toInt()
                                val gridSize = 16f
                                var seed = 8675309
                                fun fastRand(max: Int): Int {
                                    seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                                    return seed % max
                                }
                                val dirX = floatArrayOf(1f, 1f, 0f, -1f, -1f, -1f, 0f, 1f)
                                val dirY = floatArrayOf(0f, -1f, -1f, -1f, 0f, 1f, 1f, 1f)
                                val turns = intArrayOf(-2, -1, 1, 2)
                                val colors = listOf(NeonCyanVal, NeonRed, NeonGreenVal, NeonOrange, NeonGold, NeonPink, NeonPurple)

                                val overflowW = w * 1.5f
                                val overflowH = h * 1.5f
                                val wInt = (overflowW / gridSize).toInt().coerceAtLeast(1)
                                val hInt = (overflowH / gridSize).toInt().coerceAtLeast(1)

                                val generatedTraces = mutableListOf<PcbTraceData>()
                                val pathMeasure = androidx.compose.ui.graphics.PathMeasure()

                                for (i in 0 until traceCount) {
                                    val individualTracePath = Path()
                                    var pX = (fastRand(wInt) * gridSize) - (w * 0.25f)
                                    var pY = (fastRand(hInt) * gridSize) - (h * 0.25f)

                                    val startNode = androidx.compose.ui.geometry.Rect(pX - 5f, pY - 5f, pX + 5f, pY + 5f)
                                    individualTracePath.addOval(startNode)
                                    localPcbPath.addOval(startNode)

                                    individualTracePath.moveTo(pX, pY)
                                    localPcbPath.moveTo(pX, pY)

                                    var dirIndex = (fastRand(4) * 2)
                                    val segments = 4 + fastRand(9)

                                    for (s in 0 until segments) {
                                        val length = (2 + fastRand(5)) * gridSize
                                        val nX = pX + (dirX[dirIndex] * length)
                                        val nY = pY + (dirY[dirIndex] * length)

                                        individualTracePath.lineTo(nX, nY)
                                        localPcbPath.lineTo(nX, nY)

                                        pX = nX
                                        pY = nY

                                        val turn = turns[fastRand(4)]
                                        dirIndex = (dirIndex + turn + 8) % 8
                                    }

                                    val endNode = androidx.compose.ui.geometry.Rect(pX - 5f, pY - 5f, pX + 5f, pY + 5f)
                                    individualTracePath.addOval(endNode)
                                    localPcbPath.addOval(endNode)

                                    // Measure the exact path length to set gap correctly
                                    pathMeasure.setPath(individualTracePath, false)
                                    val exactPathLength = pathMeasure.length

                                    val tColor = colors[fastRand(colors.size)]
                                    val tSpeed = 1f + (fastRand(15) / 5f) // Truly cinematic slow speeds
                                    val tDashLength = 20f + fastRand(40)

                                    // Make gap length longer than the entire path to prevent early repeating
                                    val tGapLength = exactPathLength + 100f

                                    val headPaint = Paint().apply {
                                        color = tColor
                                        strokeWidth = 4.5f
                                        strokeCap = StrokeCap.Round
                                        strokeJoin = StrokeJoin.Round
                                        style = PaintingStyle.Stroke
                                        blendMode = BlendMode.Screen
                                        asFrameworkPaint().maskFilter = BlurMaskFilter(1f, BlurMaskFilter.Blur.NORMAL)
                                    }

                                    val tailPaint = Paint().apply {
                                        color = tColor
                                        strokeWidth = 3f
                                        strokeCap = StrokeCap.Round
                                        strokeJoin = StrokeJoin.Round
                                        style = PaintingStyle.Stroke
                                        blendMode = BlendMode.Screen
                                        asFrameworkPaint().maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
                                    }

                                    // Seed offset randomized against the total pattern length to scatter them
                                    val randomSeedOffset = fastRand(1000).toFloat()

                                    generatedTraces.add(
                                        PcbTraceData(individualTracePath, tColor, tSpeed, tDashLength, tGapLength, headPaint, tailPaint, randomSeedOffset)
                                    )
                                }
                                PcbPathCache.cachedBasePath = Path().apply { addPath(localPcbPath) }
                                PcbPathCache.cachedTraces = generatedTraces
                                PcbPathCache.lastW = w
                                PcbPathCache.lastH = h
                                PcbPathCache.lastDensity = traceDensityMultiplier
                            }
                        }

                        translate(left = bgParallaxX, top = bgParallaxY) {
                            // 1. Draw static background paths
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && rainbowShader != null) {
                                rainbowShader.setFloatUniform("resolution", w, h)
                                rainbowShader.setFloatUniform("time", timeFloat)
                                rainbowShader.setFloatUniform("tilt", tiltX, tiltY)
                                rainbowShader.setFloatUniform("currentTranslate", 0f)
                                drawPath(localPcbPath, ShaderBrush(rainbowShader), style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            } else {
                                val colorTiltOffsetMultiplier = 60f
                                val angle = 0f
                                val gradientSpread = w * 0.8f
                                val rainbowCenterX = (w / 2f) + (tiltX * colorTiltOffsetMultiplier)
                                val rainbowCenterY = (h / 2f) + (tiltY * colorTiltOffsetMultiplier)

                                val pcbBrush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF800020), Color(0xFF806B00), Color(0xFF008000), Color(0xFF008080), Color(0xFF451571), Color(0xFF800020)),
                                    start = Offset(rainbowCenterX - cos(angle) * gradientSpread, rainbowCenterY - sin(angle) * gradientSpread),
                                    end = Offset(rainbowCenterX + cos(angle) * gradientSpread, rainbowCenterY + sin(angle) * gradientSpread)
                                )
                                drawPath(localPcbPath, pcbBrush, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round), alpha = 0.7f)
                            }

                            // 2. Cinematic Flowing Pulses
                            drawIntoCanvas { canvas ->
                                PcbPathCache.cachedTraces?.forEach { trace ->
                                    val patternLength = trace.dashLength + trace.gapLength

                                    val rawDistance = (timeFloat / 0.5f * trace.speed) + trace.seedOffset
                                    val cycleCount = (rawDistance / patternLength).toInt()
                                    val cycleFraction = (rawDistance % patternLength) / patternLength

                                    val easeFraction = cycleFraction * cycleFraction * (3f - 2f * cycleFraction)

                                    val rampedPhase = -((cycleCount + easeFraction) * patternLength)

                                    val pulseOpacity = (sin(timeFloat * 5f + trace.seedOffset) * 0.5f + 0.5f).coerceIn(0f, 1f)

                                    trace.headPaint.pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(trace.dashLength, trace.gapLength),
                                        phase = rampedPhase
                                    )
                                    trace.headPaint.color = trace.color.copy(alpha = pulseOpacity)
                                    canvas.drawPath(trace.path, trace.headPaint)

                                    trace.tailPaint.pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(trace.dashLength * 0.8f, trace.gapLength + (trace.dashLength * 0.2f)),
                                        phase = rampedPhase + (trace.dashLength * 0.6f)
                                    )
                                    trace.tailPaint.color = trace.color.copy(alpha = pulseOpacity * 0.45f)
                                    canvas.drawPath(trace.path, trace.tailPaint)
                                }
                            }
                        }
                    }
                    2 -> {
                        val starCount = 60
                        for (i in 0 until starCount) {
                            val z = ((i * 10f) - (timeFloat * 5f)) % 600f
                            val actualZ = if (z < 0) z + 600f else z
                            if (actualZ < 10f) continue

                            val sx = ((i * 73f) % w) - (w / 2)
                            val sy = ((i * 113f) % h) - (h / 2)

                            val px = cx + (sx / (actualZ / 100f))
                            val py = cy + (sy / (actualZ / 100f))

                            val size = (1f - (actualZ / 600f)) * 4f
                            val alpha = (1f - (actualZ / 600f)).coerceIn(0f, 1f)
                            drawCircle(Color.White.copy(alpha = alpha), radius = size, center = Offset(px, py))
                        }
                    }
                }
            } else { drawCircle(color = Color.Black) }
        }

        // ==========================================
        // LAYER 2: SHIMMER, DUST, AND VIGNETTE
        // ==========================================
        val shimmerLayerBlurRadius = 0.dp

        if (!isAmbient && state.bgMode == 1) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && shimmerLayerBlurRadius > 0.dp) {
                            Modifier.blur(shimmerLayerBlurRadius, edgeTreatment = BlurredEdgeTreatment.Unbounded)
                        } else Modifier
                    )
            ) {
                val w = size.width
                val h = size.height

                translate(left = bgParallaxX, top = bgParallaxY) {
                    val pcbPathToDraw = PcbPathCache.cachedBasePath ?: Path()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && shimmerShader != null) {
                        shimmerShader.setFloatUniform("resolution", w, h)
                        shimmerShader.setFloatUniform("tilt", tiltX, tiltY)
                        shimmerShader.setFloatUniform("time", timeFloat)
                        shimmerShader.setFloatUniform("currentTranslate", 0f)
                        drawPath(pcbPathToDraw, ShaderBrush(shimmerShader), style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Plus)
                    } else {
                        val shimmerTiltOffsetMultiplier = 40f
                        val shimmerCenterX = (w / 2f) - (tiltX * shimmerTiltOffsetMultiplier)
                        val shimmerCenterY = (h / 2f) - (tiltY * shimmerTiltOffsetMultiplier)
                        val beamAngle = (Math.PI / 4) + (tiltX / 15f)
                        val beamSpread = w * 1.2f

                        val shimmerBrush = Brush.linearGradient(
                            0.0f to Color.Transparent, 0.45f to Color(0x1AFFFFFF),
                            0.5f to Color(0xE6FFFFFF), 0.55f to Color(0x1AFFFFFF), 1.0f to Color.Transparent,
                            start = Offset(shimmerCenterX - (cos(beamAngle) * beamSpread).toFloat(), shimmerCenterY - (sin(beamAngle) * beamSpread).toFloat()),
                            end = Offset(shimmerCenterX + (cos(beamAngle) * beamSpread).toFloat(), shimmerCenterY + (sin(beamAngle) * beamSpread).toFloat())
                        )
                        drawPath(pcbPathToDraw, shimmerBrush, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.Plus)
                    }
                }

                // Data Dust
                val midParallaxX = bgParallaxX * 0.5f
                val midParallaxY = bgParallaxY * 0.5f
                val dustSpeed = 1.2f
                val dustOffset = -(timeFloat * dustSpeed) % w

                translate(left = midParallaxX, top = midParallaxY) {
                    var seed = 12345
                    fun dRand(max: Float): Float {
                        seed = (seed * 1103515245 + 12345) and 0x7FFFFFFF
                        return (seed % 1000) / 1000f * max
                    }
                    for (i in 0..15) {
                        val dx = (dRand(w) + dustOffset + (i * 30f)) % w
                        val dy = dRand(h)
                        val actualX = if (dx < 0) dx + w else dx
                        val size = 1.5f + dRand(1.5f)
                        drawCircle(NeonCyanVal.copy(alpha = 0.4f), radius = size, center = Offset(actualX, dy), blendMode = BlendMode.Plus)
                    }
                }

                val vignetteCenter = Offset((w / 2f) + (tiltX * 10f), (h / 2f) + (tiltY * 10f))
                val vignetteBrush = Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                    center = vignetteCenter,
                    radius = w * 0.55f
                )
                drawRect(brush = vignetteBrush)
            }
        }

        // ==========================================
        // LAYER 3: FOREGROUND (Sharp UI Elements + Shadows)
        // ==========================================
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val sw = if (isAmbient) 4f else 18f
            val sweepHp = (114f * (state.hpLevel / 100f)).coerceIn(0f, 114f)

            fun DrawScope.drawOuterElements(
                hpColor: Color, ackColor: Color, fluxColor: Color,
                baseAlpha: Float = 1f, isShadow: Boolean = false
            ) {
                if (!isAmbient && !isShadow) {
                    drawArc(NeonGreenVal.copy(0.15f * baseAlpha), -87f, 114f, false, style = Stroke(sw, cap = StrokeCap.Butt))
                    if (state.overcharge >= 25 && !state.vitalityOffline) {
                        val glowBaseColor = if (state.overcharge >= 50) NeonGold else NeonCyanVal
                        val glowColor = glowBaseColor.copy(alpha = 0.5f * pulseAlpha * baseAlpha)
                        val glowWidth = sw + 46f
                        drawArc(glowColor, -87f, sweepHp, false, style = Stroke(glowWidth, cap = StrokeCap.Butt))
                    }
                }
                drawArc(hpColor.copy(alpha = hpColor.alpha * baseAlpha), -87f, sweepHp, false, style = Stroke(sw, cap = StrokeCap.Butt))
                drawArc(ackColor.copy(alpha = ackColor.alpha * baseAlpha), 33f, 114f, false, style = Stroke(sw, cap = StrokeCap.Butt))
                drawArc(fluxColor.copy(alpha = fluxColor.alpha * baseAlpha), 153f, 114f, false, style = Stroke(sw, cap = StrokeCap.Butt))
            }

            fun DrawScope.drawInnerElements(
                ramColor: Color, batColor: Color, pwrColor: Color,
                baseAlpha: Float = 1f, isShadow: Boolean = false
            ) {
                if (!isAmbient) {
                    val ir = (size.minDimension / 2) * 0.55f
                    val ms = 80f
                    val tl = Offset(w / 2 - ir, h / 2 - ir)
                    val sz = Size(ir * 2, ir * 2)

                    val bgStyleColor = if(isShadow) Color.Transparent else Color.Gray.copy(0.3f * baseAlpha)
                    drawArc(bgStyleColor, 230f, ms, false, topLeft = tl, size = sz, style = Stroke(6f))
                    drawArc(ramColor.copy(alpha = ramColor.alpha * baseAlpha), 230f, ms * state.ramUsage, false, topLeft = tl, size = sz, style = Stroke(6f))

                    drawArc(bgStyleColor, 350f, ms, false, topLeft = tl, size = sz, style = Stroke(6f))
                    drawArc(batColor.copy(alpha = batColor.alpha * baseAlpha), 350f, ms * state.batLevel, false, topLeft = tl, size = sz, style = Stroke(6f))

                    drawArc(bgStyleColor, 110f, ms, false, topLeft = tl, size = sz, style = Stroke(6f))
                    drawArc(pwrColor.copy(alpha = pwrColor.alpha * baseAlpha), 110f, ms * state.powerUsage, false, topLeft = tl, size = sz, style = Stroke(6f))

                    val r = 20f
                    drawLine(NeonCyanVal.copy(0.5f * baseAlpha), Offset(w / 2 - r, h / 2), Offset(w / 2 + r, h / 2), 2f)
                    drawLine(NeonCyanVal.copy(0.5f * baseAlpha), Offset(w / 2, h / 2 - r), Offset(w / 2, h / 2 + r), 2f)
                } else {
                    drawCircle(Color.Gray.copy(alpha = baseAlpha), radius = (size.minDimension / 2) * 0.55f, style = Stroke(2f))
                }
            }

            val ramColor = lerpColor(NeonGreenVal, NeonRed, state.ramUsage)
            val batColor = lerpColor(NeonRed, NeonGreenVal, state.batLevel)
            val pwrColor = lerpColor(NeonGreenVal, NeonRed, state.powerUsage)

            if (isPcbMode) {
                translate(left = -shadowOffsetX, top = -shadowOffsetY) {
                    drawOuterElements(Color.Black, Color.Black, Color.Black, baseAlpha = 0.6f, isShadow = true)
                }
                translate(left = fgParallaxX - shadowOffsetX, top = fgParallaxY - shadowOffsetY) {
                    drawInnerElements(Color.Black, Color.Black, Color.Black, baseAlpha = 0.6f, isShadow = true)
                }
            }

            drawOuterElements(currentHpColor, currentAckColor, currentFluxColor)

            translate(left = fgParallaxX, top = fgParallaxY) {
                if (isPcbMode && (abs(tiltX) > 4f || abs(tiltY) > 4f)) {
                    val caOffset = 1.5f
                    translate(left = -caOffset, top = 0f) { drawInnerElements(Color.Red, Color.Red, Color.Red, baseAlpha = 0.5f) }
                    translate(left = caOffset, top = 0f) { drawInnerElements(Color.Cyan, Color.Cyan, Color.Cyan, baseAlpha = 0.5f) }
                    drawInnerElements(ramColor, batColor, pwrColor)
                } else {
                    drawInnerElements(ramColor, batColor, pwrColor)
                }
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {

            CurvedLayout(anchor = 330f) {
                curvedRow(modifier = CurvedModifier.padding(radial = 5.dp)) {
                    if (!state.vitalityOffline) {
                        val hydText = when (state.hydStatus) { 0 -> "LOW"; 2 -> "HI"; else -> "NOM" }
                        val unifiedHydColor = when (state.hydStatus) { 0 -> NeonRed; 2 -> NeonOrange; else -> NeonCyanVal }
                        curvedText(text = "H2O ", fontSize = 8.sp, color = if (isAmbient) Color.Gray else unifiedHydColor, fontWeight = FontWeight.Bold)
                        curvedText(text = hydText, color = if (isAmbient) Color.Gray else unifiedHydColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    curvedColumn(modifier = CurvedModifier.padding(angular = 12.dp)) {
                        curvedText(text = "HP ${state.hpLevel}", color = currentHpColor.let { if (it == Color.Transparent) Color.Gray else it }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (state.vitalityOffline) curvedText(text = "OFFLINE", color = if (isAmbient) Color.White else NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    if (!state.vitalityOffline && state.mealStatus != 0) {
                        val mealText = if (state.mealStatus == 1) "PREP" else "REQ"
                        val mealColor = if (state.mealStatus == 1) NeonCyanVal else NeonOrange
                        curvedText(text = mealText, color = if (isAmbient) Color.Gray else mealColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        curvedText(text = " MEAL", fontSize = 12.sp, color = if (isAmbient) Color.Gray else mealColor, fontWeight = FontWeight.Bold)
                    }
                }
            }

            CurvedLayout(anchor = 90f) {
                curvedColumn(modifier = CurvedModifier.padding(radial = 5.dp)) {
                    curvedText(state.ackDeckName, color = currentAckColor.let { if (it == Color.Transparent) Color.Gray else it }, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    if (state.ackTargetName != "NONE" && state.ackTargetName != "-1") {
                        val wedgeColor = if (isAmbient) Color.Gray else NeonGreenVal
                        curvedText(text = ">> ${state.ackTargetName}", color = wedgeColor, fontSize = 10.sp, fontWeight = FontWeight.Normal)
                    }
                    if (state.ackOffline) curvedText("OFFLINE", color = if (isAmbient) Color.White else NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            CurvedLayout(anchor = 210f) {
                curvedColumn(modifier = CurvedModifier.padding(radial = 5.dp)) {
                    val fluxLabel = when {
                        !state.fluxActive -> "STBY"
                        state.fluxProfile == 0 -> "PULSE"
                        state.fluxProfile == 1 -> "GEIGER"
                        state.fluxProfile == 2 -> "THROB"
                        state.fluxProfile == 3 -> state.fluxCustomName.take(12)
                        else -> "UNKNOWN"
                    }

                    curvedText(
                        text = fluxLabel,
                        color = currentFluxColor.let {
                            if (it == Color.Transparent) Color.Gray else it
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (state.fluxOffline) curvedText("OFFLINE", color = if (isAmbient) Color.White else NeonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (!state.fluxOffline && state.fluxActive) {
                    val sleepMarker = if (state.fluxSleepMode) " // SLP" else ""

                    curvedText(modifier = CurvedModifier.padding(radial = 8.dp, angular = 5.dp),
                        text = "${state.fluxBpm} BPM // ${state.fluxIntensity}%$sleepMarker",
                        color = if (isAmbient) {
                            Color.Gray
                        } else {
                            Color(state.fluxPrimaryColor)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp)
                    .offset { IntOffset(fgParallaxX.roundToInt(), fgParallaxY.roundToInt()) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timeString,
                        style = if (isAmbient) TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Normal, color = Color.White, fontFamily = CyberFont, letterSpacing = (-2).sp, drawStyle = Stroke(width = 2f))
                        else TextStyle(fontSize = 38.sp, fontWeight = FontWeight.Normal, color = NeonCyanVal, fontFamily = CyberFont, letterSpacing = (-2).sp)
                    )
                    if (!isAmbient) {
                        Text(
                            text = secString,
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Normal, color = NeonCyanVal.copy(alpha = 0.5f), fontFamily = CyberFont, letterSpacing = 2.sp)
                        )
                    }
                }
            }
        }
    }
}

// Data class to store individual trace properties
data class PcbTraceData(
    val path: Path,
    val color: Color,
    val speed: Float,
    val dashLength: Float,
    val gapLength: Float,
    val headPaint: Paint,
    val tailPaint: Paint,
    val seedOffset: Float
)

// Cache the generated paths and objects to prevent recalculating
object PcbPathCache {
    var cachedBasePath: Path? = null
    var cachedTraces: List<PcbTraceData>? = null
    var lastW: Float = 0f
    var lastH: Float = 0f
    var lastDensity: Float = 0f
}