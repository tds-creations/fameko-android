package com.example.famekodriver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.*
import com.google.android.gms.location.*
import kotlinx.coroutines.*

class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val repository = DriverRepository.getInstance()
    private lateinit var sessionManager: SessionManager

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sessionManager = SessionManager(this)

        locationCallback = object : LocationCallback() {
            // ... (keep original logic)
            override fun onLocationResult(locationResult: LocationResult) {
                if (locationResult.lastLocation == null) {
                    Log.d("LocationService", "Location fix lost or null")
                }
                locationResult.lastLocation?.let { location ->
                    val driverId = sessionManager.getDriverId() ?: return
                    
                    // Send via WebSocket for real-time tracking
                    repository.sendLocationUpdateWs(
                        location.latitude,
                        location.longitude,
                        location.bearing
                    )

                    serviceScope.launch {
                        repository.updateLocation(
                            driverId,
                            location.latitude,
                            location.longitude,
                            location.bearing
                        )
                        Log.d("LocationService", "Updated server with location: ${location.latitude}, ${location.longitude}")
                    }
                }
            }
        }

        // Listen for new delivery requests in background
        serviceScope.launch {
            repository.events.collect { event ->
                if (event is FamekoEvent.NewDeliveryRequest) {
                    showNewDeliveryNotification(event.delivery)
                }
            }
        }
    }

    private fun showNewDeliveryNotification(delivery: Delivery) {
        val channelId = "delivery_request_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Delivery Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new delivery requests"
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("New Ride Request!")
            .setContentText("Pickup: ${delivery.pickupLocation}")
            .setSmallIcon(R.drawable.ic_fameko_driver_logo)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setCategory(NotificationCompat.CATEGORY_CALL) // Treat it as important as a call
            .setFullScreenIntent(pendingIntent, true)
            .build()

        notificationManager.notify(100, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return START_STICKY
    }

    private fun start() {
        val channelId = "location_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val driverId = sessionManager.getDriverId()
        if (driverId != null && sessionManager.isOnline()) {
            repository.startWebSocket("DRIVER_$driverId")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Driver is Active")
            .setContentText("Your location is being updated in the background.")
            .setSmallIcon(R.drawable.ic_fameko_driver_logo)
            .setOngoing(true)
            .build()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            Log.e("LocationService", "Permission missing for location updates", e)
        }

        startForeground(1, notification)
    }

    private fun stop() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (e: Exception) {
            Log.e("LocationService", "Error removing updates", e)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        try {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        } catch (_: Exception) {}
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }
}
