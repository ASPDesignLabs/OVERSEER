package com.snakesan.overseermobile.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

// --- DATA MODELS ---
data class ShortcutConfig(
    val id: String,
    val packageName: String,
    val label: String,
    var angleStart: Float,
    var angleEnd: Float,
    val color: Color
)

data class OverseerConfig(
    var fontSizeSp: Float = 14f,
    var shortcuts: List<ShortcutConfig> = listOf(
        ShortcutConfig("vit", "com.snakesan.vitalitysys", "VITALITY", 0f, 120f, Color(0xFF00FF41)),
        ShortcutConfig("ack", "com.example.besu", "ACK", 120f, 240f, Color(0xFF00F3FF)),
        ShortcutConfig("neon", "com.snakesan.neonflux", "NEON", 240f, 360f, Color(0xFFFF0055))
    )
)

// --- MAIN UI ---
@Composable
fun ConfigurationScreen() {
    val context = LocalContext.current
    var config by remember { mutableStateOf(OverseerConfig()) }
    
    // Force recomposition when we tweak internal list items
    var trigger by remember { mutableIntStateOf(0) } 

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        Text(
            text = "// OVERSEER TERMINAL //",
            color = Color(0xFF00F3FF),
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Live Preview
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.DarkGray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            LivePreviewCanvas(config, trigger)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Basic Editor (We'll expand this later for drag/colors)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Global Font Size: ${config.fontSizeSp.toInt()}", color = Color.White)
                Slider(
                    value = config.fontSizeSp,
                    onValueChange = { 
                        config = config.copy(fontSizeSp = it) 
                        trigger++
                    },
                    valueRange = 8f..32f
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { pushConfigToWatch(context, config) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
        ) {
            Text(">>> PUSH TO WATCH", color = Color.White)
        }
    }
}

// --- CANVAS PREVIEW ---
@Composable
fun LivePreviewCanvas(config: OverseerConfig, trigger: Int) {
    Canvas(modifier = Modifier.size(250.dp)) {
        val strokeWidth = 12.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val topLeft = Offset(
            (size.width - radius * 2) / 2,
            (size.height - radius * 2) / 2
        )
        val arcSize = Size(radius * 2, radius * 2)

        config.shortcuts.forEach { shortcut ->
            val sweep = shortcut.angleEnd - shortcut.angleStart
            drawArc(
                color = shortcut.color,
                startAngle = shortcut.angleStart - 90f, 
                sweepAngle = sweep - 4f, 
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

// --- WATCH SYNC LOGIC ---
fun pushConfigToWatch(context: Context, config: OverseerConfig) {
    val dataClient = Wearable.getDataClient(context)
    val request = PutDataMapRequest.create("/overseer/ui_config").apply {
        dataMap.putFloat("font_size", config.fontSizeSp)
        
        val shortcutsPayload = config.shortcuts.joinToString("|") {
            "${it.id},${it.packageName},${it.label},${it.angleStart},${it.angleEnd},${it.color.value}"
        }
        dataMap.putString("shortcuts_data", shortcutsPayload)
        dataMap.putLong("timestamp", System.currentTimeMillis())
    }
    
    val putDataReq = request.asPutDataRequest().setUrgent()
    
    dataClient.putDataItem(putDataReq)
        .addOnSuccessListener { Toast.makeText(context, "Synced to OVERSEER", Toast.LENGTH_SHORT).show() }
        .addOnFailureListener { Toast.makeText(context, "Sync Failed", Toast.LENGTH_SHORT).show() }
}
