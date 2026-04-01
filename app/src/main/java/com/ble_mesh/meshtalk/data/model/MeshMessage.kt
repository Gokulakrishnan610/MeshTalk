package com.ble_mesh.meshtalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Core message unit for the MeshTalk network.
 *
 * @param id          Globally-unique message id = "<senderId>_<timestamp>"
 * @param senderId    UUID string of the originating device
 * @param receiverId  UUID string of the target device, or null for broadcast
 * @param message     Plaintext (or AES-encrypted) message body
 * @param ttl         Remaining hops; decremented by each relay; stop at 0
 * @param timestamp   Unix epoch milliseconds when the message was created
 * @param isOutgoing  true if this device originated the message (for UI bubble direction)
 * @param status      Delivery status: SENT, RELAYED, RECEIVED
 */
@Entity(tableName = "messages")
data class MeshMessage(
    @PrimaryKey
    val id: String,
    val senderId: String,
    val receiverId: String?,          // null = broadcast
    val message: String,
    val ttl: Int,
    val timestamp: Long,
    val isOutgoing: Boolean = false,
    val status: MessageStatus = MessageStatus.RECEIVED
)

enum class MessageStatus {
    SENT,       // This device sent it
    RELAYED,    // This device forwarded it
    RECEIVED    // This device is the final recipient
}
