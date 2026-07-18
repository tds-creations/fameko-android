package com.example.famekodriver.core.domain.model

data class Driver(
    val id: Int,
    val fullName: String,
    val email: String,
    val phone: String,
    val region: String,
    val licenseNumber: String,
    val vehicleType: String?,
    val vehicleNumber: String?,
    val status: String,
    val isOnline: Boolean,
    val rating: Double,
    val serviceType: ServiceType = ServiceType.RIDE_HAILING,
    val profilePicture: String? = null,
    val emergencyContact1: String? = null,
    val emergencyContact2: String? = null,
    val userRole: String = "DRIVER", // DRIVER, OWNER, BOTH
    val companyName: String? = null,
    val registrationNumber: String? = null
)

data class Wallet(
    val id: Int,
    val driverId: Int,
    val balance: Double,
    val totalCredited: Double,
    val totalDebitted: Double
)

data class WalletTransaction(
    val id: Int,
    val walletId: Int,
    val deliveryId: Int?,
    val transactionType: String,
    val amount: Double,
    val description: String?,
    val status: String,
    val createdAt: String
)

data class DriverStatusResponse(
    val success: Boolean,
    val status: String, // "PENDING", "APPROVED", "REJECTED"
    val missingDocs: List<String>,
    val emergencyContact1: String? = null,
    val emergencyContact2: String? = null,
    val profilePicture: String? = null,
    val isOnline: Boolean = false,
    val isDailyFeePaid: Boolean = false,
    val dailyFeeAmount: Double = 0.0,
    val dailyFeeExpiryTime: String? = null,
    val vehicleCategory: String? = "Economy",
    val vehicleType: String? = null,
    val plateNumber: String? = null,
    val vehicleModel: String? = null
)
