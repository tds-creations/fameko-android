package com.example.famekodriver.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.backend.db.DatabaseRepository
import com.google.gson.Gson
import kotlin.time.Duration.Companion.seconds

val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
val activeCalls = ConcurrentHashMap<String, CallParticipants>()
private val gson = Gson()

data class CallParticipants(val initiatorId: String, val recipientId: String)

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/ws/{userId}") {
            val userId = call.parameters["userId"] ?: "anonymous"
            
            // Close old session for this user if any
            sessions[userId]?.let { old ->
                println("WS: Closing old session for $userId")
                try { old.close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Duplicate connection")) } catch (_: Exception) {}
            }

            sessions[userId] = this
            println("WS: User $userId connected. Total sessions: ${sessions.size}")
            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        try {
                            val msg = gson.fromJson(text, WebSocketMessage::class.java)
                            when (msg.type) {
                                "CALL_INITIATE" -> {
                                    val data = gson.fromJson(msg.payload, Map::class.java)
                                    val orderId = (data["order_id"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
                                    val callerName = data["caller_name"]?.toString() ?: "Someone"
                                    val callId = data["call_id"]?.toString() ?: ""
                                    
                                    val recipientId: String? = if (userId.startsWith("DRIVER_")) {
                                        DatabaseRepository.getCustomerIdForOrder(orderId)?.let { "CUSTOMER_$it" }
                                    } else {
                                        DatabaseRepository.getDriverIdForOrder(orderId)?.let { "DRIVER_$it" }
                                    }
                                    
                                    if (recipientId != null) {
                                        activeCalls[callId] = CallParticipants(userId, recipientId)
                                        sendToUser(recipientId, "CALL_INCOMING", mapOf(
                                            "call_id" to callId,
                                            "caller_name" to callerName,
                                            "order_id" to orderId,
                                            "initiator_id" to userId
                                        ))
                                    }
                                }
                                "CALL_ACCEPT" -> {
                                    val data = gson.fromJson(msg.payload, Map::class.java)
                                    val callId = data["call_id"]?.toString() ?: ""
                                    val participants = activeCalls[callId]
                                    if (participants != null) {
                                        sendToUser(participants.initiatorId, "CALL_ACCEPTED", mapOf("call_id" to callId))
                                    }
                                }
                                "CALL_REJECT" -> {
                                    val data = gson.fromJson(msg.payload, Map::class.java)
                                    val callId = data["call_id"]?.toString() ?: ""
                                    val participants = activeCalls.remove(callId)
                                    if (participants != null) {
                                        sendToUser(participants.initiatorId, "CALL_REJECTED", mapOf("call_id" to callId))
                                    }
                                }
                                "CALL_END" -> {
                                    val data = gson.fromJson(msg.payload, Map::class.java)
                                    val callId = data["call_id"]?.toString() ?: ""
                                    val participants = activeCalls.remove(callId)
                                    if (participants != null) {
                                        val otherId = if (userId == participants.initiatorId) participants.recipientId else participants.initiatorId
                                        sendToUser(otherId, "CALL_ENDED", mapOf("call_id" to callId))
                                    }
                                }
                                "WEBRTC_OFFER", "WEBRTC_ANSWER", "WEBRTC_ICE_CANDIDATE" -> {
                                    val data = gson.fromJson(msg.payload, Map::class.java)
                                    val callId = data["call_id"]?.toString() ?: ""
                                    val participants = activeCalls[callId]
                                    if (participants != null) {
                                        val targetId = if (userId == participants.initiatorId) participants.recipientId else participants.initiatorId
                                        sendToUser(targetId, msg.type, data)
                                    }
                                }
                                "LOCATION_UPDATE" -> {
                                    if (userId.startsWith("DRIVER_")) {
                                        val data = gson.fromJson(msg.payload, Map::class.java)
                                        val lat = data["lat"]?.toString()?.toDoubleOrNull() ?: 0.0
                                        val lng = data["lng"]?.toString()?.toDoubleOrNull() ?: 0.0
                                        val bearing = data["bearing"]?.toString()?.toFloatOrNull() ?: 0f
                                        val driverId = userId.removePrefix("DRIVER_")
                                        
                                        // Broadcast to all admins for live tracking
                                        broadcastToAdmins("DRIVER_LOCATION_UPDATE", mapOf(
                                            "driverId" to driverId,
                                            "lat" to lat,
                                            "lng" to lng,
                                            "bearing" to bearing
                                        ))
                                    }
                                }
                            }
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                println("WebSocket error for $userId: ${e.localizedMessage}")
            } finally {
                sessions.remove(userId)
                // If a user disconnects, end their active calls
                activeCalls.entries.removeIf { entry ->
                    if (entry.value.initiatorId == userId || entry.value.recipientId == userId) {
                        val otherId = if (entry.value.initiatorId == userId) entry.value.recipientId else entry.value.initiatorId
                        // We can't easily launch a coroutine to send CALL_ENDED here without a scope, 
                        // but normally sessions being removed is enough for cleanup.
                        true
                    } else false
                }
                println("WS: User $userId disconnected.")
            }
        }
    }
}

suspend fun broadcastToDrivers(type: String, payload: Any) {
    val message = WebSocketMessage(type, gson.toJson(payload))
    val json = gson.toJson(message)
    val frame = Frame.Text(json)
    val driverSessions = sessions.filter { it.key.startsWith("DRIVER_") }
    driverSessions.values.forEach { session ->
        try {
            session.send(frame)
        } catch (_: Exception) {}
    }
}

suspend fun broadcastNotificationToDrivers(title: String, message: String, type: String = "GENERAL") {
    // We could store in DB for each driver, but that's expensive.
    // For now, just broadcast to online drivers via WS.
    val payload = mapOf(
        "title" to title,
        "message" to message,
        "type" to type,
        "createdAt" to java.time.LocalDateTime.now().toString()
    )
    broadcastToDrivers("NOTIFICATION_RECEIVED", payload)
}

suspend fun broadcastToAdmins(type: String, payload: Any) {
    val message = WebSocketMessage(type, gson.toJson(payload))
    val json = gson.toJson(message)
    val frame = Frame.Text(json)
    val adminSessions = sessions.filter { it.key.startsWith("ADMIN_") }
    adminSessions.values.forEach { session ->
        try {
            session.send(frame)
        } catch (_: Exception) {}
    }
}

suspend fun broadcastToCustomers(type: String, payload: Any) {
    val message = WebSocketMessage(type, gson.toJson(payload))
    val json = gson.toJson(message)
    val frame = Frame.Text(json)
    val customerSessions = sessions.filter { it.key.startsWith("CUSTOMER_") }
    customerSessions.values.forEach { session ->
        try {
            session.send(frame)
        } catch (_: Exception) {}
    }
}
suspend fun sendToUser(userId: String, type: String, payload: Any) {
    val session = sessions[userId]
    if (session == null) {
        println("WS: Session not found for user $userId. Sending Push Notification...")
        
        // Extract ID and Type from userId (e.g. DRIVER_123, CUSTOMER_456, ADMIN_1)
        val parts = userId.split("_")
        if (parts.size == 2) {
            val userType = parts[0].lowercase() // "driver", "customer", "admin"
            val id = parts[1].toIntOrNull()
            if (id != null && userType != "admin") { // No push for admins for now
                val token = DatabaseRepository.getUserFcmToken(id, userType)
                if (token != null) {
                    val title = when (type) {
                        "CALL_INCOMING" -> "Incoming Call"
                        "NEW_DELIVERY" -> "New Delivery Request"
                        "NEW_MESSAGE" -> {
                            val msg = payload as? Message
                            if (msg != null && msg.conversationId >= 1000000) "Support Message" else "New Message"
                        }
                        "NOTIFICATION_RECEIVED" -> (payload as? Map<*, *>)?.get("title")?.toString() ?: "Fameko"
                        else -> "Fameko Update"
                    }
                    val body = when (type) {
                        "CALL_INCOMING" -> "Someone is calling you"
                        "NEW_DELIVERY" -> {
                            val pickup = (payload as? Delivery)?.pickupLocation ?: (payload as? Map<*, *>)?.get("pickupLocation")?.toString() ?: "Nearby"
                            "A new job is available at $pickup"
                        }
                        "NEW_MESSAGE" -> (payload as? Message)?.body ?: "You have a new message"
                        "NOTIFICATION_RECEIVED" -> (payload as? Map<*, *>)?.get("message")?.toString() ?: "Tap to view"
                        else -> "You have a new update in the app"
                    }
                    
                    PushNotificationHelper.sendNotification(token, title, body, mapOf("type" to type))
                }
            }
        }
        return
    }
    val message = WebSocketMessage(type, gson.toJson(payload))
    try {
        session.send(Frame.Text(gson.toJson(message)))
        println("WS: Successfully sent $type to $userId")
    } catch (e: Exception) {
        println("WS: Failed to send to $userId: ${e.message}")
        sessions.remove(userId)
    }
}
