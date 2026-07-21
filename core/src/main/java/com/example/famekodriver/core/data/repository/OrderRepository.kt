package com.example.famekodriver.core.data.repository

import android.util.Log
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class OrderRepository {

    suspend fun createOrder(request: OrderCreateRequest): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.createOrder(request)
            if (response["success"] == true) {
                val orderId = (response["orderId"] as? Double)?.toInt() ?: 0
                Result.success(orderId)
            } else {
                Result.failure(Exception(response["message"]?.toString() ?: "Failed to create order"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderStatus(orderId: Int): Result<OrderStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getOrderStatus(orderId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveOrder(customerId: String): Result<OrderStatusResponse?> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getActiveOrder(customerId)
            if (response.success && response.status.isNotEmpty()) {
                Result.success(response)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: Int, reason: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.cancelOrder(orderId, reason)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerTrips(customerId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCustomerTrips(customerId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("OrderRepo", "API getCustomerTrips failed", e)
            Result.failure(e)
        }
    }

    suspend fun deleteTrip(tripId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.deleteTrip(tripId)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearAllTrips(customerId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.clearAllTrips(customerId)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPromotions(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getPromotions()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiscountRate(customerId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getDiscountRate(customerId)
            Result.success(response["discount_percentage"] ?: 0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNearbyDrivers(lat: Double, lng: Double): Result<List<DriverLocation>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getNearbyDrivers(lat, lng)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRideEstimates(lat: Double, lng: Double, dist: Double, dur: Double, region: String? = null): Result<List<RideEstimateResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getRideEstimates(lat, lng, dist, dur, region)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPricingConfig(): Result<PricingConfig> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getPricingConfig()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShareableTripLink(driverId: String, deliveryId: String): Result<ShareTripResponse> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getShareableTripLink(driverId, deliveryId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitRating(driverId: String, orderId: Int, rating: Float, comment: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.submitRating(driverId, orderId, rating, comment)
            Result.success(response.success)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateRoute(request: RouteRequest): Result<RouteResponse> = withContext(Dispatchers.IO) {
        // Priority 1: TomTom
        try {
            val points = mutableListOf<String>()
            points.add("${request.start.lat},${request.start.lng}")
            request.stops.forEach { points.add("${it.lat},${it.lng}") }
            points.add("${request.end.lat},${request.end.lng}")

            val locations = points.joinToString(":")
            val tomTomResponse = NetworkClient.tomTomService.calculateRoute(
                locations = locations,
                apiKey = NetworkClient.TOMTOM_API_KEY
            )

            if (!tomTomResponse.routes.isNullOrEmpty()) {
                val route = tomTomResponse.routes!![0]
                val summary = route.summary ?: route.legs?.firstOrNull()?.summary
                
                if (summary != null) {
                    val coords = route.legs?.flatMap { leg ->
                        leg.points?.mapNotNull { 
                            if (it.lat == null || it.lon == null || (it.lat == 0.0 && it.lon == 0.0)) null
                            else listOf(it.lon, it.lat) 
                        } ?: emptyList()
                    } ?: emptyList()

                    val response = RouteResponse(
                        fromCache = false,
                        routeCoords = coords,
                        distanceM = summary.lengthInMeters ?: 0,
                        etaMin = (summary.travelTimeInSeconds ?: 0) / 60.0,
                        vehicleType = request.vehicleType,
                        routeType = request.routeType,
                        waypoints = coords.size,
                        computedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.format(Date()),
                        instructions = emptyList()
                    )
                    return@withContext Result.success(response)
                }
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("OrderRepo", "TomTom Routing failed: ${e.message}")
        }

        // Fallback: Internal Python Routing
        try {
            val response = NetworkClient.routingApi.calculateRoute(request)
            // Handle Python backend nesting
            val primary = response.primary
            if (primary != null) {
                val flatResponse = response.copy(
                    routeCoords = primary.coordinates,
                    distanceM = primary.distanceM.toInt(),
                    etaMin = primary.durationMin
                )
                Result.success(flatResponse)
            } else {
                Result.success(response)
            }
        } catch (e: Exception) {
            Log.e("OrderRepo", "Python Routing failed: ${e.message}")
            Result.failure(e)
        }
    }
}
