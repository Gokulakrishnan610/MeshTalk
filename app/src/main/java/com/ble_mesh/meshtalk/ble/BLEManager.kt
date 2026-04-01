package com.ble_mesh.meshtalk.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.ble_mesh.meshtalk.data.model.MeshMessage
import com.ble_mesh.meshtalk.mesh.MeshManager
import com.google.gson.Gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.UUID

/**
 * BLEManager — central BLE controller.
 *
 * Responsibilities:
 *  • Advertise device presence (with MESH_SERVICE_UUID).
 *  • Scan for peers advertising the same UUID.
 *  • Run a GATT Server to accept incoming message writes.
 *  • Act as GATT Client to connect to peers and write messages.
 *  • Integrate with [MeshManager] for relay logic.
 *
 * All BLE operations that require context are annotated @SuppressLint("MissingPermission")
 * because permissions are checked by MainActivity before this class is ever used.
 */
@SuppressLint("MissingPermission")
class BLEManager(
    private val context: Context,
    private val meshManager: MeshManager,
    private val deviceId: String          // This device's UUID string
) {

    private val tag = "BLEManager"
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

    // ── Scan ────────────────────────────────────────────────────────────────
    private var bleScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    /** Mutable map of discovered devices: address → (name, rssi) */
    private val _discoveredDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, DiscoveredDevice>> = _discoveredDevices

    // ── Advertise ───────────────────────────────────────────────────────────
    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    // ── GATT Server ─────────────────────────────────────────────────────────
    private var gattServer: BluetoothGattServer? = null
    private val connectedCentrals = mutableSetOf<BluetoothDevice>()

    // ── Cleanup Job ──────────────────────────────────────────────────────────
    private var cleanupJob: Job? = null

    // ── GATT Client connections ──────────────────────────────────────────────
    private val clientGatts = mutableMapOf<String, BluetoothGatt>()

    // ── Public observable ────────────────────────────────────────────────────
    private val _statusFlow = MutableStateFlow("Idle")
    val statusFlow: StateFlow<String> = _statusFlow

    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════

    fun start() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _statusFlow.value = "Bluetooth not available"
            return
        }
        startGattServer()
        startAdvertising()
        startScanning()
        // Observe messages to forward and broadcast them to all peers
        scope.launch {
            meshManager.forwardFlow.collect { msg -> broadcastMessage(msg) }
        }
        _statusFlow.value = "Running"
    }

    fun stop() {
        stopScanning()
        stopAdvertising()
        cleanupJob?.cancel()
        gattServer?.close()
        clientGatts.values.forEach { it.close() }
        clientGatts.clear()
        scope.cancel()
        _statusFlow.value = "Stopped"
    }

    // ════════════════════════════════════════════════════════════════════════
    // Advertising
    // ════════════════════════════════════════════════════════════════════════

    private fun startAdvertising() {
        advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: run {
            Log.w(tag, "Device does not support BLE advertising")
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)           // allow GATT connections
            .setTimeout(0)                  // advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)    // name can exceed 31B limit
            .addServiceUuid(ParcelUuid(GattAttributes.MESH_SERVICE_UUID))
            .build()

        advertiseCallback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                Log.d(tag, "Advertising started")
            }
            override fun onStartFailure(errorCode: Int) {
                Log.e(tag, "Advertise failed: $errorCode")
            }
        }

        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun stopAdvertising() {
        advertiser?.stopAdvertising(advertiseCallback)
    }

    // ════════════════════════════════════════════════════════════════════════
    // Scanning
    // ════════════════════════════════════════════════════════════════════════

    fun startScanning() {
        bleScanner = bluetoothAdapter?.bluetoothLeScanner ?: return

        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(GattAttributes.MESH_SERVICE_UUID))
            .build()

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setReportDelay(0)
            .build()

        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                handleScanResult(result)
            }
            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { handleScanResult(it) }
            }
            override fun onScanFailed(errorCode: Int) {
                Log.e(tag, "Scan failed: $errorCode")
            }
        }

        bleScanner?.startScan(listOf(filter), settings, scanCallback)
        Log.d(tag, "Scanning started")

        // Prune stale devices every 5 seconds
        cleanupJob = scope.launch {
            while (isActive) {
                delay(5000)
                val now = System.currentTimeMillis()
                val current = _discoveredDevices.value.toMutableMap()
                var changed = false
                val iterator = current.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (now - entry.value.lastSeen > 12000) { // 12 seconds timeout
                        iterator.remove()
                        changed = true
                        clientGatts[entry.key]?.close()
                        clientGatts.remove(entry.key)
                        Log.d(tag, "Removed stale device: ${entry.key}")
                    }
                }
                if (changed) {
                    _discoveredDevices.value = current
                }
            }
        }
    }

    fun stopScanning() {
        cleanupJob?.cancel()
        bleScanner?.stopScan(scanCallback)
        Log.d(tag, "Scanning stopped")
    }

    private fun handleScanResult(result: ScanResult) {
        val device = result.device
        val rssi = result.rssi
        val name = try { device.name } catch (e: Exception) { null }
        val displayName = if (!name.isNullOrBlank()) name else "Unknown (${device.address.takeLast(5)})"

        val current = _discoveredDevices.value.toMutableMap()
        current[device.address] = DiscoveredDevice(device.address, displayName, rssi, device, System.currentTimeMillis())
        _discoveredDevices.value = current

        // Auto-connect to exchange messages via GATT
        if (!clientGatts.containsKey(device.address)) {
            connectToDevice(device)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // GATT Server (peripheral side — receives messages)
    // ════════════════════════════════════════════════════════════════════════

    private fun startGattServer() {
        val serverCallback = object : BluetoothGattServerCallback() {

            override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    connectedCentrals.add(device)
                    Log.d(tag, "Central connected: ${device.address}")
                } else {
                    connectedCentrals.remove(device)
                    Log.d(tag, "Central disconnected: ${device.address}")
                }
            }

            override fun onCharacteristicWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                characteristic: BluetoothGattCharacteristic,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (characteristic.uuid == GattAttributes.MESSAGE_CHARACTERISTIC_UUID) {
                    val json = String(value, Charsets.UTF_8)
                    Log.d(tag, "Received message: $json")
                    try {
                        val msg = gson.fromJson(json, MeshMessage::class.java)
                        meshManager.onMessageReceived(msg)
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to parse message", e)
                    }
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                    }
                }
            }

            override fun onDescriptorWriteRequest(
                device: BluetoothDevice,
                requestId: Int,
                descriptor: BluetoothGattDescriptor,
                preparedWrite: Boolean,
                responseNeeded: Boolean,
                offset: Int,
                value: ByteArray
            ) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
                }
            }
        }

        gattServer = bluetoothManager.openGattServer(context, serverCallback)

        val service = BluetoothGattService(
            GattAttributes.MESH_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Message write characteristic
        val msgChar = BluetoothGattCharacteristic(
            GattAttributes.MESSAGE_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )

        // ACK notify characteristic
        val ackChar = BluetoothGattCharacteristic(
            GattAttributes.ACK_CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        val cccd = BluetoothGattDescriptor(
            GattAttributes.CLIENT_CHARACTERISTIC_CONFIG_UUID,
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        ackChar.addDescriptor(cccd)

        service.addCharacteristic(msgChar)
        service.addCharacteristic(ackChar)
        gattServer?.addService(service)

        Log.d(tag, "GATT Server started")
    }

    // ════════════════════════════════════════════════════════════════════════
    // GATT Client (central side — sends messages)
    // ════════════════════════════════════════════════════════════════════════

    private fun connectToDevice(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(tag, "GATT connected to ${device.address}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d(tag, "GATT disconnected from ${device.address}")
                    clientGatts.remove(device.address)
                    gatt.close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(tag, "Services discovered for ${device.address}")
                    clientGatts[device.address] = gatt
                    // Request higher MTU (up to 512 bytes for larger messages)
                    gatt.requestMtu(512)
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Log.d(tag, "MTU changed to $mtu for ${device.address}")
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                val result = if (status == BluetoothGatt.GATT_SUCCESS) "OK" else "FAIL($status)"
                Log.d(tag, "Write to ${device.address}: $result")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }
    }

    /**
     * Write a [MeshMessage] to all currently connected GATT peers.
     * This is how relay/broadcast forwarding works.
     */
    fun broadcastMessage(message: MeshMessage) {
        val json = gson.toJson(message)
        val bytes = json.toByteArray(Charsets.UTF_8)
        if (bytes.size > GattAttributes.MAX_WRITE_SIZE) {
            Log.w(tag, "Message too large (${bytes.size}B), truncating not supported")
            return
        }

        clientGatts.values.forEach { gatt ->
            val service = gatt.getService(GattAttributes.MESH_SERVICE_UUID)
            val char = service?.getCharacteristic(GattAttributes.MESSAGE_CHARACTERISTIC_UUID)
            if (char != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    gatt.writeCharacteristic(char, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = bytes
                    @Suppress("DEPRECATION")
                    gatt.writeCharacteristic(char)
                }
            }
        }
        Log.d(tag, "Broadcast message '${message.id}' to ${clientGatts.size} peers")
    }

    fun getConnectedPeerCount(): Int = clientGatts.size + connectedCentrals.size
}

/** Model for a discovered BLE peer. */
data class DiscoveredDevice(
    val address: String,
    val name: String,
    val rssi: Int,
    val bluetoothDevice: BluetoothDevice,
    val lastSeen: Long = System.currentTimeMillis()
)
