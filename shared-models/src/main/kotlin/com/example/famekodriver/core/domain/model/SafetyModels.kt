package com.example.famekodriver.core.domain.model

data class SOSRequest(
    val driverId: String,
    val latitude: Double,
    val longitude: Double,
    val type: String
)

data class SOSAlert(
    val id: Int,
    val driverId: String,
    val driverName: String,
    val driverPhone: String,
    val lat: Double,
    val lng: Double,
    val time: Long,
    val type: String,
    val plateNumber: String? = null,
    val vehicleModel: String? = null
)

data class ShareTripResponse(
    val shareUrl: String,
    val deliveryId: String,
    val expiresAt: Long
)
