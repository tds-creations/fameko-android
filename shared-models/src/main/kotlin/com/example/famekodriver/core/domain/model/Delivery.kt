package com.example.famekodriver.core.domain.model

data class Delivery(
    val id: String,
    val orderId: Int,
    val driverId: String?,
    val pickupLocation: String,
    val dropOffLocation: String,
    val pickupLat: Double? = null,
    val pickupLng: Double? = null,
    val dropOffLat: Double? = null,
    val dropOffLng: Double? = null,
    val status: DeliveryStatus,
    val distanceKm: Double,
    val estimatedEarnings: Double,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerAddress: String? = null,
    val serviceType: ServiceType = ServiceType.PACKAGE_DELIVERY,
    val pickupEtaMin: Double? = null,
    val totalFare: Double? = null,
    val createdAt: String? = null
)

enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    ARRIVED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
