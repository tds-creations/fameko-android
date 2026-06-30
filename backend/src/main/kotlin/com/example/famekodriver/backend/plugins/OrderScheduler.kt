package com.example.famekodriver.backend.plugins

import com.example.famekodriver.backend.db.DatabaseInitializer
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
    DatabaseInitializer.getDataSource().connection.use { conn ->
        // Find orders that have been PENDING for more than 5 minutes
        // We use orders table created_at.
        val sql = """
            SELECT id, customer_id 
            FROM orders 
            WHERE status = 'PENDING' 
            AND created_at < CURRENT_TIMESTAMP - interval '5 minutes'
        """.trimIndent()
        
        val stmt = conn.prepareStatement(sql)
        val rs = stmt.executeQuery()
        
        val timedOutOrders = mutableListOf<Pair<Int, Int>>()
        while (rs.next()) {
            timedOutOrders.add(rs.getInt("id") to rs.getInt("customer_id"))
        }
        
        for ((orderId, customerId) in timedOutOrders) {
            println("DEBUG: Order $orderId has timed out. Cancelling automatically.")
            
            // 1. Update DB status to CANCELLED (or TIMED_OUT if we want to distinguish)
            // Using CANCELLED to maintain compatibility with existing app logic, 
            // but we will send a specific notification type.
            conn.prepareStatement("UPDATE orders SET status = 'CANCELLED' WHERE id = ?").apply {
                setInt(1, orderId)
                executeUpdate()
            }
            conn.prepareStatement("UPDATE deliveries SET status = 'CANCELLED' WHERE order_id = ?").apply {
                setInt(1, orderId)
                executeUpdate()
            }
            
            // 2. Notify Customer
            // We send a NOTIFICATION_RECEIVED event with type 'ORDER_TIMEOUT'
            // This will show in status bar (via push) and can be handled in app.
            sendToUser("CUSTOMER_$customerId", "NOTIFICATION_RECEIVED", mapOf(
                "id" to (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                "title" to "Ride Request Timed Out",
                "message" to "We couldn't find a driver for your request. Please try again.",
                "type" to "ORDER_TIMEOUT",
                "orderId" to orderId
            ))
            
            // Also send a direct order status update so the app UI refreshes if open
            sendToUser("CUSTOMER_$customerId", "ORDER_STATUS_UPDATE", mapOf(
                "orderId" to orderId,
                "status" to "CANCELLED",
                "reason" to "TIMEOUT"
            ))
        }
    }
}
