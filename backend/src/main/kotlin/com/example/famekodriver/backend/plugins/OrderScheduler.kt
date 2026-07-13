package com.example.famekodriver.backend.plugins

import com.example.famekodriver.backend.db.DatabaseRepository
import io.ktor.server.application.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.minutes

fun Application.configureOrderScheduler() {
    launch(Dispatchers.IO) {
        while (isActive) {
            try {
                checkAndTimeoutOrders()
            } catch (e: Exception) {
                log.error("Error in order timeout scheduler: ${e.message}", e)
            }
            delay(1.minutes) // Check every minute
        }
    }
}

private suspend fun checkAndTimeoutOrders() {
    val timedOutOrders = DatabaseRepository.getTimedOutOrders()
    
    for ((orderId, customerId) in timedOutOrders) {
        println("DEBUG: Order $orderId has timed out. Cancelling automatically.")
        
        DatabaseRepository.cancelOrder(orderId)
        
        sendToUser("CUSTOMER_$customerId", "NOTIFICATION_RECEIVED", mapOf(
            "id" to (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            "title" to "Ride Request Timed Out",
            "message" to "We couldn't find a driver for your request. Please try again.",
            "type" to "ORDER_TIMEOUT",
            "orderId" to orderId
        ))
        
        sendToUser("CUSTOMER_$customerId", "ORDER_STATUS_UPDATE", mapOf(
            "orderId" to orderId,
            "status" to "CANCELLED",
            "reason" to "TIMEOUT"
        ))
    }
}
