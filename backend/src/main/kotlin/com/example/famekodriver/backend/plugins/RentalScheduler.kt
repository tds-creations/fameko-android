package com.example.famekodriver.backend.plugins

import com.example.famekodriver.backend.db.DatabaseInitializer
import com.example.famekodriver.backend.db.DatabaseRepository
import io.ktor.server.application.*
import kotlinx.coroutines.*
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

fun Application.configureRentalScheduler() {
    launch(Dispatchers.IO) {
        while (isActive) {
            try {
                checkAndEndExpiredRentals()
            } catch (e: Exception) {
                log.error("Error in rental expiry scheduler: ${e.message}", e)
            }
            delay(5.minutes) // Check every 5 minutes
        }
    }
}

private suspend fun checkAndEndExpiredRentals() {
    val expiredRentals = DatabaseRepository.getExpiredRentals()
    
    for (rental in expiredRentals) {
        val rentalId = rental["id"] as Int
        val startTime = (rental["start_time"] as java.sql.Timestamp).toLocalDateTime()
        val durationHours = rental["duration_hours"] as Int
        
        val expiryTime = startTime.plusHours(durationHours.toLong())
        val now = LocalDateTime.now()
        
        if (now.isAfter(expiryTime)) {
            println("DEBUG: Rental $rentalId has expired. Ending it automatically.")
            DatabaseRepository.updateRentalStatus(rentalId, "COMPLETED")
            notifyRentalEnded(rentalId, isManual = false)
        }
    }
}

suspend fun notifyRentalEnded(rentalId: Int, isManual: Boolean) {
    val participants = DatabaseRepository.getRentalParticipants(rentalId) ?: return
    
    val title = if (isManual) "Rental Ended" else "Rental Period Over"
    val body = if (isManual) 
        "The rental for ${participants.vehicleName} has been ended. The vehicle is now available for the next booking."
    else 
        "The rental period for ${participants.vehicleName} has ended. The vehicle is now available for the next booking."
    
    // Notify Customer
    sendToUser("CUSTOMER_${participants.customerId}", "NOTIFICATION_RECEIVED", mapOf(
        "title" to title,
        "message" to body,
        "type" to "RENTAL_ENDED",
        "rentalId" to rentalId,
        "isManual" to isManual
    ))
    
    // Notify Vehicle Owner (Driver)
    sendToUser("DRIVER_${participants.ownerId}", "NOTIFICATION_RECEIVED", mapOf(
        "title" to title,
        "message" to body,
        "type" to "RENTAL_ENDED",
        "rentalId" to rentalId,
        "isManual" to isManual
    ))
    
    // Notify Admin via topic
    val adminMsg = if (isManual) "Rental #$rentalId has been ended manually." else "Rental #$rentalId has ended automatically (expired)."
    PushNotificationHelper.broadcastToTopic("admins", "$title: ${participants.vehicleName}", adminMsg)
}
