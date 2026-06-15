package com.example.famekodriver.core.domain.model

data class Order(
    val id: Int,
    val customerId: Int,
    val totalAmount: Double,
    val orderDate: String,
    val status: String,
    val shippingName: String,
    val shippingAddress: String,
    val shippingPhone: String,
    val latitude: Double?,
    val longitude: Double?,
    val paymentMethod: String,
    val paymentStatus: String
)

data class OrderItem(
    val id: Int,
    val orderId: Int,
    val itemName: String,
    val itemDescription: String?,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val status: String
)

data class OrderCreateRequest(
    val customerId: String,
    val pickupLocation: String,
    val dropOffLocation: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropOffLat: Double,
    val dropOffLng: Double,
    val distanceKm: Double,
    val estimatedFare: Double,
    val durationMin: Double,
    val serviceType: ServiceType = ServiceType.RIDE_HAILING,
    val requestedVehicleType: String? = null
)

data class OrderStatusResponse(
    val success: Boolean,
    val status: String,
    val driverId: String? = null,
    val driverName: String? = null,
    val driverPhone: String? = null,
    val driverVehicle: String? = null,
    val driverVehicleModel: String? = null,
    val driverVehicleNumber: String? = null,
    val driverProfilePic: String? = null,
    val driverLat: Double? = null,
    val driverLng: Double? = null,
    val driverRating: Double? = null,
    val driverBearing: Float? = null,
    val verificationPin: String? = null,
    val deliveryId: String? = null,
    val fare: Double? = null
)

data class RideEstimateResponse(
    val serviceId: String,
    val name: String,
    val description: String,
    val fare: Double,
    val pickupEtaMin: Int,
    val icon: String // Icon key
)
