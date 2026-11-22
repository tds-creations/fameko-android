package com.example.famekodriver.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "navigation_routes")
data class NavigationRouteEntity(
    @PrimaryKey val orderId: Int,
    val status: String,
    val pickupLat: Double,
    val pickupLng: Double,
    val dropOffLat: Double,
    val dropOffLng: Double,
    val originalPathJson: String,
    val instructionsJson: String,
    val currentSegmentIndex: Int,
    val remainingDistanceKm: Double,
    val remainingDurationMin: Double,
    val timestamp: Long = System.currentTimeMillis()
)
