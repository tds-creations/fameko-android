package com.example.famekodriver.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val customerId: String,
    val label: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val updatedAt: Long = System.currentTimeMillis()
)
