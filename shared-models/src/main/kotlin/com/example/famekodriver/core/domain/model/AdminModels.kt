package com.example.famekodriver.core.domain.model

data class Admin(
    val id: Int,
    val username: String,
    val email: String? = null,
    val role: String? = "ADMIN",
    val region: String? = null
)

data class AdminPlatformStats(
    val totalDrivers: Int,
    val pendingDrivers: Int,
    val onlineDrivers: Int,
    val liveDeliveries: Int,
    val totalRevenue: Double,
    val totalDebt: Double,
    val activeSOS: Int
)
