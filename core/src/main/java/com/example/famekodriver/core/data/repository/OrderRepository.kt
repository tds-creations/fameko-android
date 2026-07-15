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

    suspend fun getAvailableDeliveries(lat: Double = 0.0, lng: Double = 0.0, vehicleType: String? = null, vehicleCategory: String? = null): Result<List<Delivery>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAvailableDeliveries(lat, lng, vehicleType, vehicleCategory)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("OrderRepo", "API Get Available Deliveries failed", e)
            Result.failure(e)
        }
    }

    suspend fun getMyDeliveries(driverId: String): Result<List<Delivery>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getMyDeliveries(driverId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("OrderRepo", "API Get My Deliveries failed", e)
            Result.failure(e)
        }
    }

    suspend fun acceptDelivery(driverId: String, deliveryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.acceptDelivery(driverId, deliveryId)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to accept delivery"))
            }
        } catch (e: Exception) {
            Log.e("OrderRepo", "Failed to accept delivery via API", e)
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryStatus(deliveryId: String, status: DeliveryStatus): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateDeliveryStatus(deliveryId, status.name)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update status"))
            }
        } catch (e: Exception) { 
            Log.e("OrderRepo", "API status update failed", e)
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        customerId: String,
        pickupLocation: String,
        dropOffLocation: String,
        pickupLat: Double,
        pickupLng: Double,
        dropOffLat: Double,
        dropOffLng: Double,
        distanceKm: Double,
        estimatedFare: Double,
        durationMin: Double,
        serviceType: ServiceType = ServiceType.RIDE_HAILING,
        requestedVehicleType: String? = null,
        scheduledTime: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = OrderCreateRequest(
                customerId, pickupLocation, dropOffLocation,
                pickupLat, pickupLng, dropOffLat, dropOffLng,
                distanceKm, estimatedFare, durationMin, serviceType,
                requestedVehicleType, scheduledTime
            )
            val response = NetworkClient.famekoApi.createOrder(request)
            if (response["success"] == true) {
                Result.success(response["orderId"].toString())
            } else {
                Result.failure(Exception(response["message"]?.toString() ?: "Unknown error"))
            }
        } catch (e: Exception) {
            Log.e("OrderRepo", "Order creation via API failed", e)
            Result.failure(e)
        }
    }

    suspend fun cancelOrder(orderId: Int, reason: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.cancelOrder(orderId, reason)
            if (response["success"] == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response["message"]?.toString() ?: "Cancellation failed"))
            }
        } catch (e: Exception) {
            Log.e("OrderRepo", "Order cancellation via API failed", e)
            Result.failure(e)
        }
    }

    suspend fun getActiveOrder(customerId: String): Result<OrderStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getActiveOrder(customerId)
            if (response.success) {
                Result.success(response)
            } else {
                Result.failure(Exception("No active order"))
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

    suspend fun verifyTripPin(orderId: Int, pin: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.verifyOrderPin(orderId, pin)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitRating(driverId: String, orderId: Int, rating: Float, comment: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.submitRating(driverId, orderId, rating, comment)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.message))
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

    suspend fun calculateRoute(request: RouteRequest): Result<RouteResponse> = withContext(Dispatchers.IO) {
        // Priority 1: OSRM
        try {
            val url = "https://router.project-osrm.org/route/v1/driving/" +
                    "${request.start.lng},${request.start.lat};" +
                    "${request.end.lng},${request.end.lat}" +
                    "?overview=full&geometries=geojson"
            
            val osrmResponse = NetworkClient.osmService.getRoute(url)
            
            if (osrmResponse.code == "Ok" && osrmResponse.routes.isNotEmpty()) {
                val route = osrmResponse.routes[0]
                val response = RouteResponse(
                    fromCache = false,
                    routeCoords = route.geometry.coordinates,
                    distanceM = route.distance.toInt(),
                    etaMin = route.duration / 60.0,
                    vehicleType = request.vehicleType,
                    routeType = request.routeType,
                    waypoints = route.geometry.coordinates.size,
                    computedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }.format(Date())
                )
                return@withContext Result.success(response)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("OrderRepo", "OSM Routing failed, falling back to TomTom", e)
        }

        // Priority 2: TomTom
        try {
            val locations = "${request.start.lat},${request.start.lng}:${request.end.lat},${request.end.lng}"
            val tomTomResponse = NetworkClient.tomTomService.calculateRoute(
                locations = locations,
                apiKey = NetworkClient.TOMTOM_API_KEY
            )

            if (!tomTomResponse.routes.isNullOrEmpty()) {
                val route = tomTomResponse.routes!![0]
                val summary = route.summary ?: route.legs?.firstOrNull()?.summary
                
                if (summary != null) {
                    val coords = route.legs?.flatMap { leg ->
                        leg.points?.map { listOf(it.lon ?: 0.0, it.lat ?: 0.0) } ?: emptyList()
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
                        }.format(Date())
                    )
                    return@withContext Result.success(response)
                }
            }
            throw Exception("TomTom returned no routes")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("OrderRepo", "TomTom Routing failed as well, falling back to backend", e)
            try {
                val response = NetworkClient.famekoApi.calculateRoute(request)
                Result.success(response)
            } catch (e3: Exception) {
                if (e3 is CancellationException) throw e3
                Result.failure(Exception("Routing failed completely"))
            }
        }
    }
}
