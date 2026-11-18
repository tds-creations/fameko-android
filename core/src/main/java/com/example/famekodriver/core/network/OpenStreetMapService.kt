package com.example.famekodriver.core.network

import com.example.famekodriver.core.domain.model.LocationSuggestion
import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Native Android implementation of the Geocoding and Routing logic
 */
interface OpenStreetMapService {
    
    // Direct call to Nominatim (Search)
    @GET("https://nominatim.openstreetmap.org/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countryCodes: String = "gh",
        @Query("viewbox") viewbox: String? = null,
        @Query("bounded") bounded: Int? = null,
        @Header("User-Agent") userAgent: String = "FamekoAndroidApp_Native_v1.0"
    ): List<LocationSuggestion>

    // Direct call to Nominatim (Reverse Geocoding)
    @GET("https://nominatim.openstreetmap.org/reverse")
    suspend fun reverse(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("format") format: String = "json",
        @Header("User-Agent") userAgent: String = "FamekoAndroidApp_Native_v1.0"
    ): LocationSuggestion

    // Direct call to OSRM (Road Routing)
    @GET
    suspend fun getRoute(@Url url: String): OsrmResponse
}

data class OsrmResponse(
    @SerializedName("routes") val routes: List<OsrmRoute>,
    @SerializedName("code") val code: String
)

data class OsrmRoute(
    @SerializedName("geometry") val geometry: OsrmGeometry,
    @SerializedName("distance") val distance: Double, // meters
    @SerializedName("duration") val duration: Double, // seconds
    val legs: List<OsrmLeg>? = null
)

data class OsrmLeg(
    val steps: List<OsrmStep>? = null
)

data class OsrmStep(
    val name: String? = null,
    val ref: String? = null,
    val distance: Double? = null, // meters
    val duration: Double? = null, // seconds
    val maneuver: OsrmManeuver? = null
)

data class OsrmManeuver(
    val type: String? = null, // e.g. turn, depart, arrive, roundabout, rotary, fork, merge
    val modifier: String? = null, // e.g. left, right, sharp left, slight right, straight, uturn
    val instruction: String? = null,
    val location: List<Double>? = null, // [lng, lat]
    @SerializedName("bearing_before") val bearingBefore: Double? = null,
    @SerializedName("bearing_after") val bearingAfter: Double? = null,
    val exit: Int? = null
)

data class OsrmGeometry(
    @SerializedName("coordinates") val coordinates: List<List<Double>>, // [[lng, lat], ...]
    @SerializedName("type") val type: String
)
