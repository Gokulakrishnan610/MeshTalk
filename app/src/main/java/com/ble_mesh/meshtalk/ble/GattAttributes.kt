package com.ble_mesh.meshtalk.ble

import java.util.UUID

/**
 * Centralized UUID constants for the MeshTalk GATT service.
 *
 * Protocol:
 *  - Devices advertise their presence with the MESH_SERVICE_UUID in the AD record.
 *  - On GATT connection, the central writes a JSON-encoded MeshMessage to
 *    MESSAGE_CHARACTERISTIC_UUID.
 *  - The peripheral echoes an ACK back on ACK_CHARACTERISTIC_UUID (notify).
 */
object GattAttributes {

    /** Primary GATT service exposed by every MeshTalk device */
    val MESH_SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789012")

    /** Writable characteristic: peer writes JSON-encoded MeshMessage here */
    val MESSAGE_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789013")

    /** Notifiable characteristic: peripheral ACKs with the message id */
    val ACK_CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789014")

    /** Standard Bluetooth CCCD descriptor UUID (required for notifications) */
    val CLIENT_CHARACTERISTIC_CONFIG_UUID: UUID =
        UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    /** Advertising manufacturer-specific company ID (chosen arbitrarily) */
    const val MANUFACTURER_ID = 0xFFFF

    /** Max bytes we attempt to write in a single GATT request */
    const val MAX_WRITE_SIZE = 512
}
