package com.snakesan.overseermobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.snakesan.overseermobile.data.ConfigRepository
import com.snakesan.overseermobile.data.OverseerMobileConfig
import com.snakesan.overseermobile.data.WedgeContent
import com.snakesan.overseermobile.data.WatchSync
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object InFlight : SyncStatus()
    data class Success(val atMillis: Long) : SyncStatus()
    data class Failed(val message: String) : SyncStatus()
}

data class ConfigUiState(
    val config: OverseerMobileConfig,
    val syncStatus: SyncStatus,
    val lastSyncedMillis: Long?
)

class ConfigViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ConfigRepository(application)

    private val _state = MutableStateFlow(
        ConfigUiState(
            config = repository.load(),
            syncStatus = SyncStatus.Idle,
            lastSyncedMillis = repository.lastSyncMillis()
        )
    )
    val state: StateFlow<ConfigUiState> = _state.asStateFlow()

    /** Local edit only — the watch isn't touched until [pushToWatch] runs. */
    fun updateWedge(position: Int, content: WedgeContent) {
        val updated = when (content) {
            is WedgeContent.Function -> _state.value.config.withFunctionAssigned(position, content.function)
            is WedgeContent.Shortcut -> _state.value.config.withSlot(position, content)
        }
        _state.value = _state.value.copy(config = updated)
        repository.save(updated)
    }

    fun pushToWatch() {
        if (_state.value.syncStatus is SyncStatus.InFlight) return
        _state.value = _state.value.copy(syncStatus = SyncStatus.InFlight)

        viewModelScope.launch {
            val result = WatchSync.push(getApplication(), _state.value.config)
            _state.value = if (result.isSuccess) {
                val now = System.currentTimeMillis()
                repository.markSynced(now)
                _state.value.copy(syncStatus = SyncStatus.Success(now), lastSyncedMillis = now)
            } else {
                val message = result.exceptionOrNull()?.message ?: "Unknown error"
                _state.value.copy(syncStatus = SyncStatus.Failed(message))
            }
        }
    }

    fun acknowledgeSyncStatus() {
        _state.value = _state.value.copy(syncStatus = SyncStatus.Idle)
    }
}
