package com.ble_mesh.meshtalk.mesh

import android.content.Context
import android.util.Log
import com.ble_mesh.meshtalk.data.db.AppDatabase
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * MeshManager — core relay / duplicate-prevention engine.
 *
 * Responsibilities:
 *  1. Maintain a bounded LRU cache of seen message IDs (prevents infinite loops).
 *  2. Persist new messages to Room DB.
 *  3. Emit messages to the UI via Flow.
 *  4. Instruct BLEManager to forward messages with TTL > 0.
 *
 * Thread safety: all state mutations happen on [scope] (IO dispatcher).
 */
class MeshManager(private val context: Context, var myNickname: String = "Unknown", var myDeviceId: String = "Unknown") {

    private val tag = "MeshManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = AppDatabase.getInstance(context).messageDao()
    private val encryption = com.ble_mesh.meshtalk.crypto.EncryptionService(context)

    // ── Duplicate-prevention cache ──────────────────────────────────────────
    /** LRU-style bounded set; evicts oldest entry when full. */
    private val seenMessageIds = object : LinkedHashMap<String, Unit>(256, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<String, Unit>) = size > MAX_CACHE_SIZE
    }

    // ── Observable state ────────────────────────────────────────────────────
    /** Emits messages that should be forwarded to peers (after TTL decrement). */
    private val _forwardFlow = MutableSharedFlow<MeshMessage>(extraBufferCapacity = 64)
    val forwardFlow: SharedFlow<MeshMessage> = _forwardFlow

    /** Emits new (non-duplicate) messages for the UI. */
    private val _incomingFlow = MutableSharedFlow<MeshMessage>(extraBufferCapacity = 64)
    val incomingFlow: SharedFlow<MeshMessage> = _incomingFlow

    // ── Stats ───────────────────────────────────────────────────────────────
    private val _cacheHits = MutableStateFlow(0)
    val cacheHits: StateFlow<Int> = _cacheHits

    private val _forwardedCount = MutableStateFlow(0)
    val forwardedCount: StateFlow<Int> = _forwardedCount

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Called when a BLE message is received (from GATT write or advertisement).
     */
    fun onMessageReceived(rawMessage: MeshMessage) {
        scope.launch {
            // Ensure the local database flags this as incoming
            var message = rawMessage.copy(
                isOutgoing = false,
                status = MessageStatus.RECEIVED
            )

            synchronized(seenMessageIds) {
                if (seenMessageIds.containsKey(message.id)) {
                    _cacheHits.value++
                    Log.d(tag, "Duplicate dropped: ${message.id} | Cache hits: ${_cacheHits.value}")
                    return@launch
                }
                seenMessageIds[message.id] = Unit
            }

            Log.d(tag, "New message: ${message.id} | TTL=${message.ttl}")

            // ── Relief Logic + Decryption ────────────────────────────────────
            // If this is a private DM addressed to ME → decrypt, store, do NOT relay
            if (message.isPrivate && message.receiverId == myDeviceId.take(8)) {
                Log.d(tag, "Private DM delivered to me via ID: ${message.id}")
                val decryptedText = message.encryptedPayload?.let { encryption.decrypt(it) } ?: "🔒 [Decryption Failed]"
                message = message.copy(message = decryptedText)

                dao.insert(message)
                _incomingFlow.emit(message)
                return@launch
            }

            // Otherwise, it's either global or a DM for someone else
            dao.insert(message)
            _incomingFlow.emit(message)

            // Relay if hops remain
            if (message.ttl > 0) {
                val relayMsg = message.copy(
                    ttl = message.ttl - 1,
                    status = MessageStatus.RELAYED
                )
                // Persist relay record
                dao.insert(relayMsg.copy(id = "${relayMsg.id}_relay_${System.currentTimeMillis()}"))
                _forwardedCount.value++
                _forwardFlow.emit(relayMsg)
                Log.d(tag, "Relaying: ${relayMsg.id} | TTL left=${relayMsg.ttl}")
            }
        }
    }

    /**
     * Called when this device originates a message.
     * @param airMessage The encrypted/processed message that goes out over the air.
     * @param localMessage The plaintext version exclusively saved to the local database.
     */
    fun onMessageSent(airMessage: MeshMessage, localMessage: MeshMessage = airMessage) {
        scope.launch {
            synchronized(seenMessageIds) {
                seenMessageIds[airMessage.id] = Unit
            }
            dao.insert(localMessage)
            // Emit the air bound message for forwarding
            _forwardFlow.emit(airMessage)
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 200
    }
}
