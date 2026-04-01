package com.ble_mesh.meshtalk.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.repository.MessageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * DebugViewModel — exposes stats for the Debug / Status screen.
 */
class DebugViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MessageRepository(application)

    /** All messages (newest first) for the debug log */
    val allMessages: StateFlow<List<MeshMessage>> = repo.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Number of messages this device relayed */
    val relayedCount: StateFlow<Int> = repo.getRelayedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // These are updated from the Activity when BLEService is available
    private val _cacheHits = MutableStateFlow(0)
    val cacheHits: StateFlow<Int> = _cacheHits

    private val _connectedPeers = MutableStateFlow(0)
    val connectedPeers: StateFlow<Int> = _connectedPeers

    private val _bleStatus = MutableStateFlow("Idle")
    val bleStatus: StateFlow<String> = _bleStatus

    fun updateFromService(service: com.ble_mesh.meshtalk.ble.BLEService) {
        viewModelScope.launch {
            service.meshManager.cacheHits.collect { _cacheHits.value = it }
        }
        viewModelScope.launch {
            service.bleManager.statusFlow.collect { _bleStatus.value = it }
        }
    }

    fun updatePeerCount(count: Int) {
        _connectedPeers.value = count
    }
}
