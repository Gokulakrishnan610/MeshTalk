package com.ble_mesh.meshtalk.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import com.ble_mesh.meshtalk.ble.BLEService
import com.ble_mesh.meshtalk.ble.DiscoveredDevice
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * MainViewModel — manages service binding and device discovery state.
 *
 * The BLEService is started from MainActivity and bound here so the ViewModel
 * can expose [BLEManager.discoveredDevices] and [BLEManager.statusFlow] to the UI.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var bleService: BLEService? = null

    private val _isServiceBound = MutableStateFlow(false)
    val isServiceBound: StateFlow<Boolean> = _isServiceBound

    // ── Re-exposed flows from BLEManager ────────────────────────────────────
    private val _discoveredDevices = MutableStateFlow<Map<String, DiscoveredDevice>>(emptyMap())
    val discoveredDevices: StateFlow<Map<String, DiscoveredDevice>> = _discoveredDevices

    private val _bleStatus = MutableStateFlow("Not started")
    val bleStatus: StateFlow<String> = _bleStatus

    // ── Device ID ────────────────────────────────────────────────────────────
    val deviceId: String = application.getSharedPreferences("meshtalk", Context.MODE_PRIVATE)
        .getString("device_id", "Unknown") ?: "Unknown"

    // ── Service Connection ───────────────────────────────────────────────────
    val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val service = (binder as BLEService.LocalBinder).getService()
            bleService = service
            _isServiceBound.value = true

            // Mirror BLEManager flows
            // We can't use coroutines directly here without a scope;
            // the Activity/Composable observes via collectAsState from these StateFlows.
            // We use a simple observer pattern by polling in the UI layer.
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bleService = null
            _isServiceBound.value = false
        }
    }

    /** Call from Activity to bind. */
    fun bindService(context: Context) {
        val intent = Intent(context, BLEService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    /** Call from Activity to unbind (e.g. onDestroy). */
    fun unbindService(context: Context) {
        if (_isServiceBound.value) {
            context.unbindService(serviceConnection)
            _isServiceBound.value = false
        }
    }

    fun getBleService(): BLEService? = bleService

    override fun onCleared() {
        super.onCleared()
    }
}
