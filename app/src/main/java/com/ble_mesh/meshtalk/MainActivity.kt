package com.ble_mesh.meshtalk

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.ble_mesh.meshtalk.ble.BLEService
import com.ble_mesh.meshtalk.ui.navigation.AppNavigation
import com.ble_mesh.meshtalk.ui.theme.MeshTalkTheme
import com.ble_mesh.meshtalk.viewmodel.ChatViewModel
import com.ble_mesh.meshtalk.viewmodel.DebugViewModel
import com.ble_mesh.meshtalk.viewmodel.MainViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * MainActivity — entry point.
 *
 * Responsibilities:
 *  1. Request all necessary BLE/Location permissions.
 *  2. Start the BLEService (foreground).
 *  3. Bind to BLEService and expose it to ViewModels.
 *  4. Host the Compose NavGraph.
 */
class MainActivity : ComponentActivity() {

    private val tag = "MainActivity"

    private val mainViewModel: MainViewModel by viewModels()
    private val chatViewModel: ChatViewModel by viewModels()
    private val debugViewModel: DebugViewModel by viewModels()

    // ── Permission Launcher ──────────────────────────────────────────────────
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Log.d(tag, "All permissions granted")
            initBLE()
        } else {
            val denied = permissions.filterValues { !it }.keys
            Log.w(tag, "Denied: $denied")
            Toast.makeText(
                this,
                "BLE permissions required for MeshTalk to work",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ── Bluetooth Enable Launcher ─────────────────────────────────────────────
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            checkAndRequestPermissions()
        } else {
            Toast.makeText(this, "Bluetooth must be enabled", Toast.LENGTH_SHORT).show()
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ════════════════════════════════════════════════════════════════════════

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MeshTalkTheme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D1A))
                        // Removed global systemBarsPadding to allow screen-specific handling
                ) {
                    val bleService = if (mainViewModel.isServiceBound.value)
                        mainViewModel.getBleService()
                    else null

                    AppNavigation(
                        mainViewModel = mainViewModel,
                        chatViewModel = chatViewModel,
                        debugViewModel = debugViewModel,
                        bleService = bleService
                    )
                }
            }
        }

        ensureBluetoothEnabled()
    }

    override fun onStart() {
        super.onStart()
        mainViewModel.bindService(this)
    }

    override fun onStop() {
        super.onStop()
        // Don't unbind here — let it persist for background comms
    }

    override fun onDestroy() {
        super.onDestroy()
        mainViewModel.unbindService(this)
    }

    // ════════════════════════════════════════════════════════════════════════
    // BLE Setup
    // ════════════════════════════════════════════════════════════════════════

    private fun ensureBluetoothEnabled() {
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bm.adapter
        if (adapter == null || !adapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(intent)
        } else {
            checkAndRequestPermissions()
        }
    }

    private fun checkAndRequestPermissions() {
        val needed = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
                add(Manifest.permission.BLUETOOTH_ADVERTISE)
            } else {
                add(Manifest.permission.BLUETOOTH)
                add(Manifest.permission.BLUETOOTH_ADMIN)
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            // Location required on all API levels for BLE scan
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val notGranted = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (notGranted.isEmpty()) {
            initBLE()
        } else {
            permissionLauncher.launch(notGranted.toTypedArray())
        }
    }

    private fun initBLE() {
        Log.d(tag, "Initialising BLE")
        BLEService.start(this)
        mainViewModel.bindService(this)
    }
}