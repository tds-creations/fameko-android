package com.example.famekodriver.backend.plugins

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.ktor.server.application.*
import java.io.ByteArrayInputStream

fun Application.configureFirebase() {
    val serviceAccountJson = System.getenv("FIREBASE_SERVICE_ACCOUNT")
    
    if (serviceAccountJson != null) {
        try {
            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(serviceAccountJson.toByteArray())))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
                log.info("Firebase Admin SDK initialized successfully")
            }
        } catch (e: Exception) {
            log.error("Failed to initialize Firebase Admin SDK", e)
        }
    } else {
        log.warn("FIREBASE_SERVICE_ACCOUNT environment variable not found. Push notifications will be disabled.")
    }
}

object PushNotificationHelper {
    fun sendNotification(token: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        if (FirebaseApp.getApps().isEmpty()) return

        try {
            val message = Message.builder()
                .setToken(token)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build()

            FirebaseMessaging.getInstance().send(message)
        } catch (e: Exception) {
            println("Error sending FCM message: ${e.message}")
        }
    }

    fun broadcastToTopic(topic: String, title: String, body: String, data: Map<String, String> = emptyMap()) {
        if (FirebaseApp.getApps().isEmpty()) return

        try {
            val message = Message.builder()
                .setTopic(topic)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data)
                .build()

            FirebaseMessaging.getInstance().send(message)
        } catch (e: Exception) {
            println("Error broadcasting FCM message: ${e.message}")
        }
    }
}
