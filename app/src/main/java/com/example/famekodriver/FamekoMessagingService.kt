package com.example.famekodriver

import android.content.Intent
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FamekoMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val intent = Intent(applicationContext, DriverSplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val type = remoteMessage.data["type"] ?: ""
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Fameko Driver"
        val body = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""

        val isNewOrder = type == "NEW_DELIVERY" || type == "NEW_ORDER" || title.contains("New", ignoreCase = true)

        if (title.isNotEmpty() || body.isNotEmpty()) {
            NotificationHelper.showNotification(
                applicationContext,
                title,
                body,
                intent,
                isNewOrder = isNewOrder
            )
        }
    }

    override fun onNewToken(token: String) {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getDriverId()
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                DriverRepository.getInstance().updateFcmToken(userId, token, "driver")
            }
        }
    }
}
