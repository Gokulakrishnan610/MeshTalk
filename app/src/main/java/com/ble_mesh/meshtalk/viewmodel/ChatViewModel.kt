package com.ble_mesh.meshtalk.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus
import com.ble_mesh.meshtalk.repository.MessageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ChatViewModel — message list and send action for a specific conversation peer.
 *
 * @param peerId The remote device UUID we're chatting with (null = broadcast)
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MessageRepository(application)

    private val _peerId = MutableStateFlow<String?>(null)

    val deviceId: String = application.getSharedPreferences("meshtalk", Context.MODE_PRIVATE)
        .getString("device_id", "Unknown") ?: "Unknown"

    /** Messages for the current conversation */
    val messages: StateFlow<List<MeshMessage>> = _peerId
        .filterNotNull()
        .flatMapLatest { peerId ->
            if (peerId == "global") repo.getGlobalChat()
            else repo.getConversation(peerId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var bleServiceRef: com.ble_mesh.meshtalk.ble.BLEService? = null

    fun setConversation(peerId: String) {
        _peerId.value = peerId
    }

    fun attachBleService(service: com.ble_mesh.meshtalk.ble.BLEService) {
        bleServiceRef = service
        // Collect incoming messages from MeshManager and persist them
        viewModelScope.launch {
            service.meshManager.incomingFlow.collect { msg ->
                // The MeshManager already persisted it; this triggers UI recompose via Room Flow
            }
        }
    }

    /**
     * Send a message to [peerId]. Persists locally, adds to MeshManager cache,
     * and broadcasts via BLEManager.
     */
    fun sendMessage(text: String, peerId: String?) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val actualReceiverId = if (peerId == "global") null else peerId
            val msg = MeshMessage(
                id = "${deviceId}_${timestamp}",
                senderId = deviceId,
                receiverId = actualReceiverId,
                message = text,
                ttl = DEFAULT_TTL,
                timestamp = timestamp,
                isOutgoing = true,
                status = MessageStatus.SENT
            )
            bleServiceRef?.meshManager?.onMessageSent(msg)
            bleServiceRef?.bleManager?.broadcastMessage(msg)
        }
    }

    companion object {
        const val DEFAULT_TTL = 4
    }
}
