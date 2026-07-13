package com.example.famekodriver.core.domain.model

data class RideCategory(
    val id: Int = 0,
    val serviceId: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val serviceType: String, // RIDE_HAILING, PACKAGE_DELIVERY
    val baseFare: Double,
    val perKmRate: Double,
    val perMinuteRate: Double,
    val minFare: Double,
    val driverCommissionPercent: Double,
    val isActive: Boolean = true,
    val displayOrder: Int = 0
)
