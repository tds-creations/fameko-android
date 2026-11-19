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
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: ""
        val body = remoteMessage.data["message"] ?: remoteMessage.notification?.body ?: ""
        val orderId = (remoteMessage.data["orderId"] ?: remoteMessage.data["order_id"])?.toIntOrNull() ?: 0

        if (type == "OFFER_INVALIDATED") {
            if (orderId != 0) {
                NotificationHelper.cancelNotification(applicationContext, orderId)
            }
            return
        }

        val isNewOrder = type == "NEW_DELIVERY" || type == "NEW_ORDER" || title.contains("New", ignoreCase = true)

        if (title.isNotEmpty() || body.isNotEmpty()) {
            NotificationHelper.showNotification(
                context = applicationContext,
                title = title,
                message = body,
                intent = intent,
                isNewOrder = isNewOrder,
                type = type,
                notificationId = if (orderId != 0) orderId else 0
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
