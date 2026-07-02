package com.example.famekodriver.core.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Data class representing a customer's saved place (e.g. Home, Work)
 */
data class SavedPlace(
    @SerializedName("id")
    val id: String = "0",

    @SerializedName("customer_id")
    val customerId: String,

    @SerializedName("label")
    val label: String,

    @SerializedName("address")
    val address: String,

    @SerializedName("latitude")
    val latitude: Double,

    @SerializedName("longitude")
    val longitude: Double
)
