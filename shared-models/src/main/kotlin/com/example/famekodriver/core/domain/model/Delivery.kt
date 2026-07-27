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
    val customerId: Int? = null,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val customerAddress: String? = null,
    val customerProfilePic: String? = null,
    val serviceType: ServiceType = ServiceType.PACKAGE_DELIVERY,
    val pickupEtaMin: Double? = null,
    val totalFare: Double? = null,
    val createdAt: String? = null,
    val packageCategory: String? = null,
    val packageWeightSize: String? = null,
    val isFragile: Boolean = false,
    val recipientName: String? = null,
    val recipientPhone: String? = null,
    val packageNotes: String? = null
)

enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    ARRIVED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
