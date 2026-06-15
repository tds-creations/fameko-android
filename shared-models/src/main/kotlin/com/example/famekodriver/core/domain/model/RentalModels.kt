package com.example.famekodriver.core.domain.model

data class RentalBookRequest(
    val customerId: Int,
    val vehicleId: Int,
    val pickupLocation: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val durationHours: Int,
    val totalPrice: Double,
    val startTime: String? = null,
    val tripNotes: String? = null,
    val stops: String? = null,
    val isSelfDrive: Boolean = false
)

data class Rental(
    val id: Int,
    val customerId: Int,
    val customerName: String,
    val customerPhone: String,
    val vehicleName: String,
    val pickupLocation: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val destinationLocation: String? = null,
    val destinationLat: Double? = null,
    val destinationLng: Double? = null,
    val status: RentalStatus,
    val isUnlocked: Boolean,
    val bookingCode: String? = null
)

data class RentalVehicle(
    val id: Int,
    val ownerId: Int,
    val name: String,
    val model: String?,
    val vehicleType: String,
    val vehicleNumber: String?,
    val dailyRate: Double,
    val description: String? = null,
    val features: List<String> = emptyList(),
    val imageUrls: List<String> = emptyList(),
    val status: String = "AVAILABLE", // AVAILABLE, RENTED, MAINTENANCE
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

data class RentalBookingResponse(
    val success: Boolean,
    val rentalId: Int? = null,
    val bookingCode: String? = null,
    val checkoutUrl: String? = null,
    val accessCode: String? = null,
    val message: String? = null
)
