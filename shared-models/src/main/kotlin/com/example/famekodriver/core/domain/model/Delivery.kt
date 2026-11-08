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
    val packageNotes: String? = null,
    val isSending: Boolean = true
) {
    val currentContactName: String
        get() {
            if (serviceType != ServiceType.PACKAGE_DELIVERY) {
                return customerName ?: "Customer"
            }
            val isPickupPhase = status == DeliveryStatus.ASSIGNED || status == DeliveryStatus.ARRIVED || status == DeliveryStatus.PENDING
            return if (isSending) {
                if (isPickupPhase) (customerName ?: "Sender") else (recipientName?.takeIf { it.isNotBlank() } ?: customerName ?: "Recipient")
            } else {
                if (isPickupPhase) (recipientName?.takeIf { it.isNotBlank() } ?: "Sender") else (customerName ?: "Recipient")
            }
        }

    val currentContactPhone: String
        get() {
            if (serviceType != ServiceType.PACKAGE_DELIVERY) {
                return customerPhone ?: ""
            }
            val isPickupPhase = status == DeliveryStatus.ASSIGNED || status == DeliveryStatus.ARRIVED || status == DeliveryStatus.PENDING
            return if (isSending) {
                if (isPickupPhase) (customerPhone ?: "") else (recipientPhone?.takeIf { it.isNotBlank() } ?: customerPhone ?: "")
            } else {
                if (isPickupPhase) (recipientPhone?.takeIf { it.isNotBlank() } ?: customerPhone ?: "") else (customerPhone ?: "")
            }
        }

    val currentContactRole: String
        get() {
            if (serviceType != ServiceType.PACKAGE_DELIVERY) {
                return "PASSENGER"
            }
            val isPickupPhase = status == DeliveryStatus.ASSIGNED || status == DeliveryStatus.ARRIVED || status == DeliveryStatus.PENDING
            return if (isSending) {
                if (isPickupPhase) "SENDER (PICKUP)" else "RECEIVER (DROP-OFF)"
            } else {
                if (isPickupPhase) "SENDER (PICKUP)" else "RECEIVER (DROP-OFF)"
            }
        }
}

enum class DeliveryStatus {
    PENDING,
    ASSIGNED,
    ARRIVED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
