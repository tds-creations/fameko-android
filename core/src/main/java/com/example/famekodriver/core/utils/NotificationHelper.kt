package com.example.famekodriver.core.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_NEW_ORDER_ID = "fameko_new_order_requests_v2"
    const val CHANNEL_NEW_ORDER_NAME = "Fameko — New Order Requests"

    const val CHANNEL_GENERAL_ID = "fameko_notifications"
    const val CHANNEL_GENERAL_NAME = "Fameko Notifications"

    fun showNotification(
        context: Context,
        title: String,
        message: String,
        intent: Intent? = null,
        isNewOrder: Boolean = false
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = if (isNewOrder) CHANNEL_NEW_ORDER_ID else CHANNEL_GENERAL_ID

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (isNewOrder) {
                if (notificationManager.getNotificationChannel(CHANNEL_NEW_ORDER_ID) == null) {
                    val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/fameko_new_order")
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .build()

                    val channel = NotificationChannel(
                        CHANNEL_NEW_ORDER_ID,
                        CHANNEL_NEW_ORDER_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        setSound(soundUri, audioAttributes)
                        enableVibration(true)
                        vibrationPattern = longArrayOf(0, 500, 200, 500)
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            } else {
                if (notificationManager.getNotificationChannel(CHANNEL_GENERAL_ID) == null) {
                    val channel = NotificationChannel(
                        CHANNEL_GENERAL_ID,
                        CHANNEL_GENERAL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        val pendingIntent = if (intent != null) {
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        } else null

        val soundUri = if (isNewOrder) Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/raw/fameko_new_order") else null

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .apply {
                if (soundUri != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) setSound(soundUri)
                if (pendingIntent != null) setContentIntent(pendingIntent)
            }

        notificationManager.notify(if (isNewOrder) 9999 else System.currentTimeMillis().toInt(), builder.build())
    }
}
