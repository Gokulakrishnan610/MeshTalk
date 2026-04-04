package com.ble_mesh.meshtalk.ble

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ble_mesh.meshtalk.MainActivity
import com.ble_mesh.meshtalk.R
import com.ble_mesh.meshtalk.mesh.MeshManager
import com.ble_mesh.meshtalk.data.model.MeshMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * BLEService — Foreground Service that keeps BLE alive in the background.
 *
 * Clients bind via [LocalBinder] to access [BLEManager] and [MeshManager].
 * The service posts a persistent notification so Android won't kill it.
 */
class BLEService : Service() {

    private val tag = "BLEService"

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    lateinit var meshManager: MeshManager
        private set

    lateinit var bleManager: BLEManager
        private set

    // ── Binder ───────────────────────────────────────────────────────────────
    inner class LocalBinder : Binder() {
        fun getService(): BLEService = this@BLEService
    }

    private val binder = LocalBinder()

    override fun onBind(intent: Intent): IBinder = binder

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "onCreate")

        val prefs = getSharedPreferences("meshtalk", Context.MODE_PRIVATE)
        val deviceId = (prefs.getString("device_id", null)
            ?: java.util.UUID.randomUUID().toString().also { newId ->
                prefs.edit().putString("device_id", newId).apply()
            }).take(8)

        // Load persisted nickname (set by user in settings; default to "User_<shortId>")
        val nickname = prefs.getString("nickname", null)
            ?: "User_${deviceId.take(5)}".also { defaultNick ->
                prefs.edit().putString("nickname", defaultNick).apply()
            }

        meshManager = MeshManager(this, nickname, deviceId)
        bleManager = BLEManager(this, meshManager, deviceId, nickname)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        bleManager.start()
        Log.d(tag, "BLE started with deviceId=$deviceId")

        // Listen for new incoming messages to trigger push notifications
        serviceScope.launch {
            meshManager.incomingFlow.collect { msg ->
                if (!msg.isOutgoing) {
                    // Sync nickname if it's different from what we know
                    bleManager.updatePeerNickname(msg.senderId, msg.senderNickname)
                    showNewMessageNotification(msg)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        bleManager.stop()
        super.onDestroy()
        Log.d(tag, "onDestroy")
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Foreground service keep-alive channel (Silent)
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "MeshTalk Background Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps MeshTalk running in background"
            }
            nm.createNotificationChannel(serviceChannel)

            // High priority alerts for incoming chat messages (Sound + Pop-up)
            val messageChannel = NotificationChannel(
                MESSAGE_CHANNEL_ID,
                "Incoming Mesh Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pop-up alerts for new incoming offline messages"
            }
            nm.createNotificationChannel(messageChannel)
        }
    }

    private fun showNewMessageNotification(msg: MeshMessage) {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, MESSAGE_CHANNEL_ID)
            .setContentTitle("New Mesh Message")
            .setContentText(msg.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(msg.id.hashCode(), notification)
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MeshTalk Active")
            .setContentText("Mesh network is running…")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "meshtalk_ble_channel"
        const val MESSAGE_CHANNEL_ID = "meshtalk_messages_channel"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, BLEService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BLEService::class.java))
        }
    }
}
