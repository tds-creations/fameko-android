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
                autoCompleteReachedTrips()
            } catch (e: Exception) {
                log.error("Error in order scheduler: ${e.message}", e)
            }
            delay(1.minutes) // Check every minute
        }
    }
}

private suspend fun autoCompleteReachedTrips() {
    val reached = DatabaseRepository.getReachedDeliveries()
    
    for (trip in reached) {
        val deliveryId = trip["deliveryId"] as Int
        val orderId = trip["orderId"] as Int
        val driverId = trip["driverId"] as Int
        val customerId = trip["customerId"] as Int

        println("DEBUG: Auto-completing Trip $deliveryId for Order $orderId (Destination Reached)")
        
        DatabaseRepository.updateDeliveryStatus(deliveryId, "DELIVERED")

        // Notify Driver
        sendToUser("DRIVER_$driverId", "ORDER_STATUS_UPDATE", mapOf(
            "orderId" to orderId,
            "status" to "DELIVERED",
            "message" to "Trip completed automatically (Destination Reached)"
        ))

        // Notify Customer
        sendToUser("CUSTOMER_$customerId", "ORDER_STATUS_UPDATE", mapOf(
            "orderId" to orderId,
            "status" to "DELIVERED",
            "message" to "Your trip has been completed."
        ))
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
