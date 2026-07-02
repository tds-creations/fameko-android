package com.example.famekodriver.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncOperationEntity(
    @PrimaryKey(autoGenerate = true) val queueId: Int = 0,
    val operation: String, // CREATE, UPDATE, DELETE
    val placeId: String,
    val data: String? = null, // JSON of SavedPlace for CREATE/UPDATE
    val retries: Int = 0,
    val status: String = "PENDING",
    val createdAt: Long = System.currentTimeMillis()
)
