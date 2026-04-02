package com.ble_mesh.meshtalk.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ble_mesh.meshtalk.crypto.EncryptionService
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus
import com.ble_mesh.meshtalk.repository.MessageRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ChatViewModel — handles both Global Mesh Chat and Personal 1-on-1 DM Chat.
 *
 * @param peerId     "global" for the global broadcast chat, or a peer device address for DM
 * @param chatType   "global" or "dm"
 * @param peerName   Display name of peer (used as nickname for DM routing)
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = MessageRepository(application)
    private val encryption = EncryptionService(application)

    private val _peerId = MutableStateFlow<String?>(null)
    private val _chatType = MutableStateFlow("global")   // "global" or "dm"
    private val _peerNickname = MutableStateFlow<String?>(null)

    private val prefs = application.getSharedPreferences("meshtalk", Context.MODE_PRIVATE)

    val deviceId: String = (prefs.getString("device_id", "Unknown") ?: "Unknown").take(8)

    private val _myNickname = MutableStateFlow(
        prefs.getString("nickname", "Unknown") ?: "Unknown"
    )
    val myNickname: String get() = _myNickname.value

    fun refreshNickname() {
        _myNickname.value = prefs.getString("nickname", "Unknown") ?: "Unknown"
    }

    /** Messages for the current conversation */
    val messages: StateFlow<List<MeshMessage>> = combine(_peerId, _chatType, _peerNickname, _myNickname) { peerId, chatType, peerNickname, myNick ->
        // Return a packed class/data object to avoid Tuple 4
        arrayOf(peerId, chatType, peerNickname, myNick)
    }
        .filterNot { args -> args[0] == null }
        .flatMapLatest { args ->
            val peerId = args[0] as String
            val chatType = args[1] as String
            val peerNickname = args[2] as String?

            when {
                chatType == "dm" ->
                    repo.getPrivateConversation(deviceId, peerId)
                else ->
                    repo.getGlobalChat()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var bleServiceRef: com.ble_mesh.meshtalk.ble.BLEService? = null

    fun setConversation(peerId: String, chatType: String = "global", peerNickname: String? = null) {
        _peerId.value = peerId
        _chatType.value = chatType
        _peerNickname.value = peerNickname
    }

    fun attachBleService(service: com.ble_mesh.meshtalk.ble.BLEService) {
        bleServiceRef = service
        viewModelScope.launch {
            service.meshManager.incomingFlow.collect { /* DB-driven via Room Flow */ }
        }
    }

    /**
     * Send a message.
     * - If chatType == "global" → broadcast plaintext to everyone
     * - If chatType == "dm" → encrypt, mark isPrivate, route by recipientNickname
     */
    fun sendMessage(text: String, peerId: String?) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            val isGlobal = _chatType.value != "dm"
            val targetNickname = _peerNickname.value

            val encryptedPayload = if (!isGlobal && targetNickname != null) {
                encryption.encrypt(text)
            } else null

            val localMsg = MeshMessage(
                id = "${deviceId}_${timestamp}",
                senderId = deviceId,
                receiverId = if (isGlobal) null else peerId,
                message = text,  // Always store plaintext locally for the sender's own view
                ttl = DEFAULT_TTL,
                timestamp = timestamp,
                isOutgoing = true,
                status = MessageStatus.SENT,
                isPrivate = !isGlobal,
                senderNickname = myNickname,
                recipientNickname = targetNickname,
                encryptedPayload = encryptedPayload
            )

            // The message sent over the air should NOT contain the plaintext if it's a DM
            val airMsg = if (!isGlobal) {
                localMsg.copy(message = "🔒 [Encrypted Content]")
            } else {
                localMsg
            }

            // Register with mesh manager (persists localMsg, broadcasts airMsg)
            bleServiceRef?.meshManager?.onMessageSent(airMsg, localMsg)
        }
    }

    companion object {
        const val DEFAULT_TTL = 4
    }
}
