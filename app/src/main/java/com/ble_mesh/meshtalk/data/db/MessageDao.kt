package com.ble_mesh.meshtalk.data.db

import androidx.room.*
import com.ble_mesh.meshtalk.data.model.MeshMessage
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for MeshMessage.
 */
@Dao
interface MessageDao {

    /** Insert a message; replace on conflict (idempotent for relay duplicates). */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: MeshMessage)

    @Query(
        "SELECT * FROM messages WHERE (senderId = :peerId OR receiverId = :peerId) AND status != 'RELAYED' ORDER BY timestamp ASC"
    )
    fun getConversation(peerId: String): Flow<List<MeshMessage>>

    /** Observe all messages (broadcast + all conversations), for the debug screen. */
    @Query("SELECT * FROM messages ORDER BY timestamp DESC LIMIT 200")
    fun getAllMessages(): Flow<List<MeshMessage>>

    /** Observe all messages for the global mesh chat, ordered ASC for UI. */
    @Query("SELECT * FROM messages WHERE status != 'RELAYED' ORDER BY timestamp ASC LIMIT 500")
    fun getGlobalChat(): Flow<List<MeshMessage>>

    /** Count of messages relayed by this device (status = RELAYED). */
    @Query("SELECT COUNT(*) FROM messages WHERE status = 'RELAYED'")
    fun getRelayedCount(): Flow<Int>

    /** Delete messages older than [cutoffMs] — lightweight cleanup. */
    @Query("DELETE FROM messages WHERE timestamp < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
