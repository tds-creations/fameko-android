package com.example.famekodriver.backend.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.backend.db.DatabaseRepository
import com.example.famekodriver.backend.services.RedisManager
import com.google.gson.Gson
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

val sessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
val activeCalls = ConcurrentHashMap<String, CallParticipants>()
private val gson = Gson()

data class CallParticipants(val initiatorId: String, val recipientId: String)

@OptIn(DelicateCoroutinesApi::class)
fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // Initialize Cluster-wide Redis Pub/Sub Subscriber for Horizontal Scaling
    RedisManager.startWsSubscriber { targetUserId, type, payloadJson ->
        val localSession = sessions[targetUserId]
        if (localSession != null) {
            val message = WebSocketMessage(type, payloadJson)
            GlobalScope.launch {
                try {
                    localSession.send(Frame.Text(gson.toJson(message)))
                    println("WS [Cluster Bus]: Relayed $type to local user $targetUserId")
                } catch (_: Exception) {}
            }
        }
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
            println("WS: User $userId connected. Total sessions on this node: ${sessions.size}")
            try {
                for (frame in incoming) {
                    when (frame) {
                        is Frame.Binary -> {
                            val data = frame.readBytes()
                            val localCall = activeCalls.values.find { it.initiatorId == userId || it.recipientId == userId }
                            if (localCall != null) {
                                val targetId = if (userId == localCall.initiatorId) localCall.recipientId else localCall.initiatorId
                                sessions[targetId]?.send(Frame.Binary(true, data))
                            }
                        }
                        is Frame.Text -> {
                            val text = frame.readText()
                            try {
                                val msg = gson.fromJson(text, WebSocketMessage::class.java)
                                when (msg.type) {
                                    "CALL_INITIATE" -> {
                                        val data = gson.fromJson(msg.payload, Map::class.java)
                                        val orderId = (data["order_id"] ?: 0).toString().toDoubleOrNull()?.toInt() ?: 0
                                        val callId = data["call_id"]?.toString() ?: ""
                                        
                                        val recipientId: String?
                                        val resolvedCallerName: String
                                        
                                        if (userId.startsWith("DRIVER_")) {
                                            val driverIdInt = userId.removePrefix("DRIVER_").toIntOrNull() ?: 0
                                            val driverDetails = DatabaseRepository.getDriverDetails(driverIdInt)
                                            resolvedCallerName = driverDetails?.get("name")?.toString() ?: data["caller_name"]?.toString() ?: "Driver"
                                            recipientId = DatabaseRepository.getCustomerIdForOrder(orderId)?.let { "CUSTOMER_$it" }
                                        } else {
                                            val custIdInt = userId.removePrefix("CUSTOMER_").toIntOrNull() ?: 0
                                            val custName = DatabaseRepository.getCustomerName(custIdInt)
                                            resolvedCallerName = custName ?: data["caller_name"]?.toString() ?: "Customer"
                                            recipientId = DatabaseRepository.getDriverIdForOrder(orderId)?.let { "DRIVER_$it" }
                                        }
                                        
                                        val finalRecipientId = recipientId ?: data["target_id"]?.toString()
                                        if (finalRecipientId != null) {
                                            activeCalls[callId] = CallParticipants(userId, finalRecipientId)
                                            RedisManager.setCallParticipants(callId, userId, finalRecipientId)
                                            sendToUser(finalRecipientId, "CALL_INCOMING", mapOf(
                                                "call_id" to callId,
                                                "caller_name" to resolvedCallerName,
                                                "order_id" to orderId,
                                                "initiator_id" to userId
                                            ))
                                        }
                                    }
                                    "CALL_ACCEPT" -> {
                                        val data = gson.fromJson(msg.payload, Map::class.java)
                                        val callId = data["call_id"]?.toString() ?: ""
                                        val participants = activeCalls[callId] ?: RedisManager.getCallParticipants(callId)?.let { CallParticipants(it.first, it.second) }
                                        if (participants != null) {
                                            sendToUser(participants.initiatorId, "CALL_ACCEPTED", mapOf("call_id" to callId))
                                        }
                                    }
                                    "CALL_REJECT" -> {
                                        val data = gson.fromJson(msg.payload, Map::class.java)
                                        val callId = data["call_id"]?.toString() ?: ""
                                        val participants = activeCalls.remove(callId) ?: RedisManager.getCallParticipants(callId)?.let { CallParticipants(it.first, it.second) }
                                        RedisManager.removeCall(callId)
                                        if (participants != null) {
                                            sendToUser(participants.initiatorId, "CALL_REJECTED", mapOf("call_id" to callId))
                                        }
                                    }
                                    "CALL_END" -> {
                                        val data = gson.fromJson(msg.payload, Map::class.java)
                                        val callId = data["call_id"]?.toString() ?: ""
                                        val participants = activeCalls.remove(callId) ?: RedisManager.getCallParticipants(callId)?.let { CallParticipants(it.first, it.second) }
                                        RedisManager.removeCall(callId)
                                        if (participants != null) {
                                            val otherId = if (userId == participants.initiatorId) participants.recipientId else participants.initiatorId
                                            sendToUser(otherId, "CALL_ENDED", mapOf("call_id" to callId))
                                        }
                                    }
                                    "LOCATION_UPDATE" -> {
                                        if (userId.startsWith("DRIVER_")) {
                                            val data = gson.fromJson(msg.payload, Map::class.java)
                                            val lat = data["lat"]?.toString()?.toDoubleOrNull() ?: 0.0
                                            val lng = data["lng"]?.toString()?.toDoubleOrNull() ?: 0.0
                                            val bearing = data["bearing"]?.toString()?.toFloatOrNull() ?: 0f
                                            val driverId = userId.removePrefix("DRIVER_")
                                            
                                            // Store in Redis Geospatial index for cluster-wide search
                                            RedisManager.updateDriverLocation(driverId, lat, lng, bearing)

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
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                println("WebSocket error for $userId: ${e.localizedMessage}")
            } finally {
                sessions.remove(userId)
                activeCalls.entries.removeIf { entry ->
                    entry.value.initiatorId == userId || entry.value.recipientId == userId
                }
                println("WS: User $userId disconnected.")
            }
        }
    }
}

suspend fun broadcastToDrivers(type: String, payload: Any) {
    val payloadJson = if (payload is String) payload else gson.toJson(payload)
    val message = WebSocketMessage(type, payloadJson)
    val json = gson.toJson(message)
    val frame = Frame.Text(json)
    
    // Broadcast locally on this node
    sessions.filter { it.key.startsWith("DRIVER_") }.values.forEach { session ->
        try { session.send(frame) } catch (_: Exception) {}
    }
}

suspend fun broadcastNotificationToDrivers(title: String, message: String, type: String = "GENERAL") {
    val payload = mapOf(
        "title" to title,
        "message" to message,
        "type" to type,
        "createdAt" to java.time.LocalDateTime.now().toString()
    )
    broadcastToDrivers("NOTIFICATION_RECEIVED", payload)
}

suspend fun broadcastToAdmins(type: String, payload: Any) {
    val payloadJson = if (payload is String) payload else gson.toJson(payload)
    val message = WebSocketMessage(type, payloadJson)
    val json = gson.toJson(message)
    val frame = Frame.Text(json)
    
    sessions.filter { it.key.startsWith("ADMIN_") }.values.forEach { session ->
        try { session.send(frame) } catch (_: Exception) {}
    }
}

suspend fun broadcastToCustomers(type: String, payload: Any) {
    val payloadJson = if (payload is String) payload else gson.toJson(payload)
    val message = WebSocketMessage(type, payloadJson)
    val json = gson.toJson(message)
    val frame = Frame.Text(json)
    
    sessions.filter { it.key.startsWith("CUSTOMER_") }.values.forEach { session ->
        try { session.send(frame) } catch (_: Exception) {}
    }
}

suspend fun sendToUser(userId: String, type: String, payload: Any) {
    val session = sessions[userId]
    val payloadJson = if (payload is String) payload else gson.toJson(payload)

    if (session != null) {
        // User is connected directly to THIS server node
        val message = WebSocketMessage(type, payloadJson)
        try {
            session.send(Frame.Text(gson.toJson(message)))
            println("WS [Local]: Successfully sent $type to $userId")
            return
        } catch (e: Exception) {
            println("WS: Failed local send to $userId: ${e.message}")
            sessions.remove(userId)
        }
    }

    // If user is NOT connected locally to this node, publish via Redis Pub/Sub Bus across the cluster!
    RedisManager.publishWsEvent(userId, type, payloadJson)

    // Fallback: Send Push Notification if user has an FCM token registered
    val parts = userId.split("_")
    if (parts.size == 2) {
        val userType = parts[0].lowercase() // "driver", "customer", "admin"
        val id = parts[1].toIntOrNull()
        if (id != null && userType != "admin") {
            val token = DatabaseRepository.getUserFcmToken(id, userType)
            if (token != null) {
                val orderIdVal = (payload as? Map<*, *>)?.get("orderId")?.toString() ?: ""
                val title = when (type) {
                    "CALL_INCOMING" -> "Incoming Call"
                    "NEW_DELIVERY" -> "New Delivery Request"
                    "ORDER_CANCELLED" -> "Order Cancelled"
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
                    "ORDER_CANCELLED" -> if (orderIdVal.isNotBlank()) "Order #$orderIdVal was cancelled by customer." else "Your accepted order was cancelled by customer."
                    "NEW_MESSAGE" -> (payload as? Message)?.body ?: "You have a new message"
                    "NOTIFICATION_RECEIVED" -> (payload as? Map<*, *>)?.get("message")?.toString() ?: "Tap to view"
                    else -> "You have a new update in the app"
                }
                
                PushNotificationHelper.sendNotification(token, title, body, mapOf("type" to type, "orderId" to orderIdVal))
            }
        }
    }
}
