package com.example.famekodriver.core.domain.model

import com.google.gson.annotations.SerializedName

data class RouteRequest(
    val start: RouteLocation,
    val end: RouteLocation,
    val stops: List<RouteLocation> = emptyList(),
    @SerializedName("vehicle_type") val vehicleType: String = "car",
    @SerializedName("route_type") val routeType: String = "fastest"
)

data class RouteLocation(
    val lat: Double,
    val lng: Double
)

data class RouteResponse(
    @SerializedName("from_cache") val fromCache: Boolean = false,
    @SerializedName("route_coords") val routeCoords: List<List<Double>> = emptyList(),
    @SerializedName("distance_m") val distanceM: Int = 0,
    @SerializedName("eta_min") val etaMin: Double = 0.0,
    @SerializedName("vehicle_type") val vehicleType: String = "",
    @SerializedName("route_type") val routeType: String = "",
    val waypoints: Int = 0,
    @SerializedName("computed_at") val computedAt: String = "",
    val instructions: List<RouteInstruction> = emptyList(),
    // Fields for Python backend compatibility
    val primary: PythonRoute? = null,
    val alternatives: List<PythonRoute>? = null
)

data class PythonRoute(
    val coordinates: List<List<Double>> = emptyList(),
    @SerializedName("distance_m") val distanceM: Double = 0.0,
    @SerializedName("duration_minutes") val durationMin: Double = 0.0
)

enum class ManeuverType {
    DEPART,
    ARRIVE,
    TURN_LEFT,
    TURN_RIGHT,
    SLIGHT_LEFT,
    SLIGHT_RIGHT,
    SHARP_LEFT,
    SHARP_RIGHT,
    STRAIGHT,
    KEEP_LEFT,
    KEEP_RIGHT,
    UTURN,
    ROUNDABOUT,
    RAMP_LEFT,
    RAMP_RIGHT,
    MERGE,
    UNKNOWN
}

data class RouteInstruction(
    val text: String? = "",
    val distance: Double? = 0.0, // meters for/until this step
    val point: List<Double>? = emptyList(), // [lng, lat]
    val duration: Double? = 0.0, // seconds
    @SerializedName("street_name") val streetName: String? = "",
    @SerializedName("maneuver_type") val maneuverType: ManeuverType = ManeuverType.STRAIGHT,
    @SerializedName("turn_direction") val turnDirection: String? = "",
    @SerializedName("bearing_before") val bearingBefore: Float? = null,
    @SerializedName("bearing_after") val bearingAfter: Float? = null,
    @SerializedName("roundabout_exit") val roundaboutExit: Int? = null
)
