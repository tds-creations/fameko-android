package com.example.famekodriver.customer

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
        val intent = Intent(applicationContext, CustomerSplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Fameko"
            val message = remoteMessage.data["message"] ?: ""
            NotificationHelper.showNotification(applicationContext, title, message, intent)
        }

        remoteMessage.notification?.let {
            NotificationHelper.showNotification(
                applicationContext,
                it.title ?: "Fameko",
                it.body ?: "",
                intent
            )
        }
    }

    override fun onNewToken(token: String) {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getCustomerId()
        if (userId != null) {
            CoroutineScope(Dispatchers.IO).launch {
                DriverRepository.getInstance().updateFcmToken(userId, token, "customer")
            }
        }
    }
}
