package com.ble_mesh.meshtalk.repository

import android.content.Context
import com.ble_mesh.meshtalk.data.db.AppDatabase
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.data.model.MessageStatus
import kotlinx.coroutines.flow.Flow

/**
 * MessageRepository — single source of truth for messages.
 *
 * Bridges the ViewModel layer with Room DB.
 * BLEManager → MeshManager persists directly; this repo is used by ViewModels for reads.
 */
class MessageRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).messageDao()

    /** All messages for a specific conversation */
    fun getConversation(peerId: String): Flow<List<MeshMessage>> =
        dao.getConversation(peerId)

    /** All messages sorted chronologically (for the global mesh chat UI) */
    fun getGlobalChat(): Flow<List<MeshMessage>> =
        dao.getGlobalChat()

    /** Private 1-on-1 DM thread between this device ID and a peer's device ID */
    fun getPrivateConversation(myDeviceId: String, peerDeviceId: String): Flow<List<MeshMessage>> =
        dao.getPrivateConversation(myDeviceId, peerDeviceId)

    /** All messages (for debug screen) */
    fun getAllMessages(): Flow<List<MeshMessage>> =
        dao.getAllMessages()

    /** Live count of relayed messages */
    fun getRelayedCount(): Flow<Int> =
        dao.getRelayedCount()

    /** Insert a message (used when sending outgoing) */
    suspend fun insert(message: MeshMessage) =
        dao.insert(message)

    /** Cleanup old messages (call occasionally) */
    suspend fun cleanup(olderThanMs: Long = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L) =
        dao.deleteOlderThan(olderThanMs)
}
