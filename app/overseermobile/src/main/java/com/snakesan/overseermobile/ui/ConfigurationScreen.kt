package com.snakesan.overseermobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.snakesan.overseermobile.data.WedgeSlot
import com.snakesan.overseermobile.ui.components.WedgeCard
import com.snakesan.overseermobile.ui.components.WedgeRingPreview
import java.text.DateFormat
import java.util.Date

@Composable
fun ConfigurationScreen(viewModel: ConfigViewModel = viewModel()) {
    val uiState by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var editingSlot by remember { mutableStateOf<WedgeSlot?>(null) }
    var showPushConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.syncStatus) {
        when (val status = uiState.syncStatus) {
            is SyncStatus.Success -> {
                snackbarHostState.showSnackbar("Synced to watch")
                viewModel.acknowledgeSyncStatus()
            }
            is SyncStatus.Failed -> {
                snackbarHostState.showSnackbar("Sync failed: ${status.message}")
                viewModel.acknowledgeSyncStatus()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = "OVERSEER // WEDGE CONFIG",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WedgeRingPreview(config = uiState.config)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.config.wedges.sortedBy { it.position }, key = { it.position }) { slot ->
                    WedgeCard(slot = slot, onClick = { editingSlot = slot })
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            uiState.lastSyncedMillis?.let { millis ->
                Text(
                    text = "Last synced: ${DateFormat.getDateTimeInstance().format(Date(millis))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = { showPushConfirm = true },
                enabled = uiState.syncStatus !is SyncStatus.InFlight,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(
                    text = if (uiState.syncStatus is SyncStatus.InFlight) "SYNCING…" else "PUSH TO WATCH",
                    color = MaterialTheme.colorScheme.onSecondary
                )
            }
        }
    }

    editingSlot?.let { slot ->
        WedgeEditorDialog(
            slot = slot,
            config = uiState.config,
            onDismiss = { editingSlot = null },
            onSave = { content ->
                viewModel.updateWedge(slot.position, content)
                editingSlot = null
            }
        )
    }

    if (showPushConfirm) {
        AlertDialog(
            onDismissRequest = { showPushConfirm = false },
            title = { Text("Push to watch?") },
            text = { Text("This replaces the wedge configuration currently on your watch with what's shown here.") },
            confirmButton = {
                TextButton(onClick = {
                    showPushConfirm = false
                    viewModel.pushToWatch()
                }) { Text("PUSH") }
            },
            dismissButton = {
                TextButton(onClick = { showPushConfirm = false }) { Text("CANCEL") }
            }
        )
    }
}
