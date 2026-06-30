package com.example.famekodriver.core.domain.model

import com.google.gson.annotations.SerializedName

/**
 * Models for TomTom Search and Routing APIs
 */

data class TomTomSearchResponse(
    val results: List<TomTomSearchResult>? = emptyList()
)

data class TomTomSearchResult(
    val id: String?,
    val type: String?,
    val score: Double?,
    val address: TomTomAddress?,
    val position: TomTomPosition?,
    val poi: TomTomPoi? = null
)

data class TomTomAddress(
    val freeformAddress: String?,
    val municipality: String?,
    val country: String?
)

data class TomTomPosition(
    val lat: Double?,
    val lon: Double?
)

data class TomTomPoi(
    val name: String?
)

data class TomTomRoutingResponse(
    val routes: List<TomTomRoute>? = emptyList()
)

data class TomTomRoute(
    val summary: TomTomSummary?,
    val legs: List<TomTomLeg>? = emptyList()
)

data class TomTomSummary(
    val lengthInMeters: Int?,
    val travelTimeInSeconds: Int?,
    val departureTime: String?,
    val arrivalTime: String?
)

data class TomTomLeg(
    val summary: TomTomSummary?,
    val points: List<TomTomPosition>? = emptyList()
)
