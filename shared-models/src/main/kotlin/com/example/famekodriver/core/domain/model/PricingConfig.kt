package com.example.famekodriver.core.domain.model

data class PricingConfig(
    // Ride Hailing & Logistics Pricing
    val baseFare: Double,
    val perKmRate: Double,
    val perMinuteRate: Double,
    val minFare: Double,
    val milestoneInterval: Int,
    val milestoneDiscountPercent: Int,
    val peakMultiplier: Double,

    // Financial Config
    val driverCommissionPercent: Double,
    val dailyServiceFee: Double = 50.0,
    val minBalanceToOnline: Double = -50.0,

    // Vehicle Rental Pricing
    val rentalDailyRate: Double = 400.0,
    val rentalCommissionPercent: Double = 15.0,
    val rentalOwnerCommissionPercent: Double = 7.5,
    val rentalCustomerServiceFeePercent: Double = 7.5
)
