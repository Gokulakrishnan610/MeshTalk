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
class MeshManager(context: Context) {

    private val tag = "MeshManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val dao = AppDatabase.getInstance(context).messageDao()

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
     *
     * Algorithm:
     *  1. If id already in cache → drop (log cache hit).
     *  2. else: persist to DB, notify UI, relay if TTL > 0.
     */
    fun onMessageReceived(rawMessage: MeshMessage) {
        scope.launch {
            // Ensure the local database flags this as incoming, regardless of sender's payload
            val message = rawMessage.copy(
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

            // Persist to local DB
            dao.insert(message)

            // Notify UI
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
     * Called when this device originates a message (user typed & sent).
     * Adds to cache immediately (prevents echo) and persists.
     */
    fun onMessageSent(message: MeshMessage) {
        scope.launch {
            synchronized(seenMessageIds) {
                seenMessageIds[message.id] = Unit
            }
            dao.insert(message)
            // Also emit for forwarding (broadcast to all peers)
            _forwardFlow.emit(message)
        }
    }

    companion object {
        private const val MAX_CACHE_SIZE = 200
    }
}
