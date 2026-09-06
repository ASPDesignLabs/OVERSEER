package com.snakesan.overseer.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DataRepository(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    /**
     * Replicates "Phase 2" of your guide:
     * 1. Fetches buffered data immediately (Step 4 of guide).
     * 2. Registers a live listener (Step 3 of guide).
     * 3. Automatically unregisters when UI is gone (Step 2 of guide).
     */
    fun observeInt(path: String, key: String, default: Int): Flow<Int> = callbackFlow {
        // 1. Emit default immediately so UI doesn't crash
        trySend(default) 

        // 2. "Fetch on Wake" Logic
        // We check the existing buffer immediately. 
        // This ensures if Vitality updated while screen was off, we see it now.
        try {
            val buffer = dataClient.dataItems.await() // Suspend until result
            for (item in buffer) {
                if (item.uri.path == path) {
                    val map = DataMapItem.fromDataItem(item).dataMap
                    val storedValue = map.getInt(key, default)
                    Log.d("OVERSEER_LINK", ">>> Fetch on Wake: $path = $storedValue")
                    trySend(storedValue)
                }
            }
            buffer.release()
        } catch (e: Exception) {
            Log.e("OVERSEER_LINK", "Failed to fetch initial data", e)
        }
        
        // 3. "Live Listener" Logic
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == path) {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val newValue = map.getInt(key, default)
                    
                    Log.d("OVERSEER_LINK", "!!! LIVE UPDATE: $path = $newValue")
                    trySend(newValue)
                }
            }
            // Important: Compose handles the 'invalidate()' automatically 
            // when a new value is sent down the flow.
        }
        
        // Register
        dataClient.addListener(listener)
        
        // Unregister automatically when the composable leaves the screen (Battery Safe!)
        awaitClose { 
            Log.d("OVERSEER_LINK", "Removing listener for $path")
            dataClient.removeListener(listener) 
        }
    }
    
    // Duplicate logic for String values (Flux/ACK modes)
    fun observeString(path: String, key: String, default: String): Flow<String> = callbackFlow {
        trySend(default)

        // Fetch on Wake
        try {
            val buffer = dataClient.dataItems.await()
            for (item in buffer) {
                if (item.uri.path == path) {
                    val map = DataMapItem.fromDataItem(item).dataMap
                    val storedValue = map.getString(key, default)
                    trySend(storedValue)
                }
            }
            buffer.release()
        } catch (e: Exception) {
             Log.e("OVERSEER_LINK", "Failed to fetch initial data", e)
        }
        
        // Live Listener
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == path) {
                    val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                    val newValue = map.getString(key, default)
                    trySend(newValue)
                }
            }
        }
        
        dataClient.addListener(listener)
        awaitClose { dataClient.removeListener(listener) }
    }
}
