package com.example.famekodriver.admin

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.*

class AdminSOSService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository = DriverRepository.getInstance()
    private val activePlayers = mutableMapOf<Int, MediaPlayer>()
    private val CHANNEL_ID = "admin_sos_service"
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRunning) {
            isRunning = true
            startForeground(1, createNotification())
            startMonitoring()
        }
        return START_STICKY
    }

    private fun startMonitoring() {
        serviceScope.launch {
            while (isRunning) {
                repository.getAdminActiveSOS().onSuccess { newAlerts ->
                    val newAlertIds = newAlerts.map { it.id }.toSet()

                    // 1. Stop players for alerts that are no longer active (Resolved)
                    val resolvedIds = activePlayers.keys.filter { !newAlertIds.contains(it) }
                    resolvedIds.forEach { id ->
                        activePlayers[id]?.apply {
                            if (isPlaying) stop()
                            release()
                        }
                        activePlayers.remove(id)
                    }

                    // 2. Start players for new alerts
                    newAlerts.forEach { alert ->
                        if (!activePlayers.containsKey(alert.id)) {
                            playSound(alert.id, alert.type)
                            updateNotification(alert.driverName, alert.type)
                        }
                    }
                }
                delay(5000L) // Poll every 5 seconds
            }
        }
    }

    private fun playSound(alertId: Int, type: String) {
        val soundRes = if (type.uppercase().contains("MEDICAL")) {
            R.raw.sos_medical
        } else {
            R.raw.sos_general
        }
        
        try {
            val mp = MediaPlayer.create(this, soundRes)
            mp.isLooping = true
            mp.start()
            activePlayers[alertId] = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SOS Emergency Monitoring",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(title: String = "Fameko Admin", content: String = "Monitoring emergency alerts..."): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0, Intent(this, AdminMapActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification(driverName: String, type: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification("ACTIVE SOS: $driverName", "Type: $type"))
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        activePlayers.values.forEach {
            if (it.isPlaying) it.stop()
            it.release()
        }
        activePlayers.clear()
        serviceScope.cancel()
        super.onDestroy()
    }
}
