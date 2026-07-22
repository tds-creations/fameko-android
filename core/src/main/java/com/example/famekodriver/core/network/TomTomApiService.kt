package com.example.famekodriver.core.network

import com.example.famekodriver.core.domain.model.*
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Service for TomTom Search and Routing APIs
 */
interface TomTomApiService {

    @GET("search/2/search/{query}.json")
    suspend fun fuzzySearch(
        @Path("query") query: String,
        @Query("key") apiKey: String,
        @Query("limit") limit: Int = 5,
        @Query("countrySet") countrySet: String = "GH", // Default to Ghana
        @Query("language") language: String = "en-GB",
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("radius") radius: Int? = null
    ): TomTomSearchResponse

    @GET("search/2/reverseGeocode/{lat},{lon}.json")
    suspend fun reverseGeocode(
        @Path("lat") lat: Double,
        @Path("lon") lon: Double,
        @Query("key") apiKey: String
    ): TomTomSearchResponse

    @GET("routing/1/calculateRoute/{locations}/json")
    suspend fun calculateRoute(
        @Path("locations") locations: String, // format: "lat,lon:lat,lon"
        @Query("key") apiKey: String,
        @Query("travelMode") travelMode: String = "car",
        @Query("routeType") routeType: String = "fastest",
        @Query("traffic") traffic: Boolean = true,
        @Query("instructionsType") instructionsType: String = "text"
    ): TomTomRoutingResponse
}
