package com.example.famekodriver.core.data.repository

import android.util.Log
import com.example.famekodriver.core.domain.model.RentalBookRequest
import com.example.famekodriver.core.domain.model.RentalBookingResponse
import com.example.famekodriver.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RentalRepository {

    suspend fun getRentalVehicles(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getRentalVehicles()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("RentalRepo", "Failed to fetch rental vehicles", e)
            Result.failure(e)
        }
    }

    suspend fun getRentalRates(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getRentalRates()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun bookRental(
        customerId: Int,
        vehicleId: Int,
        pickupLocation: String,
        pickupLat: Double,
        pickupLng: Double,
        durationHours: Int,
        totalPrice: Double,
        startTime: String? = null,
        tripNotes: String? = null,
        stops: String? = null,
        isSelfDrive: Boolean = false
    ): Result<RentalBookingResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RentalBookRequest(
                customerId = customerId,
                vehicleId = vehicleId,
                pickupLocation = pickupLocation,
                pickupLat = pickupLat,
                pickupLng = pickupLng,
                durationHours = durationHours,
                totalPrice = totalPrice,
                startTime = startTime,
                tripNotes = tripNotes,
                stops = stops,
                isSelfDrive = isSelfDrive
            )
            
            val response = NetworkClient.famekoApi.bookRental(request)
            val success = response["success"] as? Boolean ?: false
            if (success) {
                Result.success(RentalBookingResponse(
                    success = true,
                    rentalId = (response["rentalId"] as? Double)?.toInt(),
                    bookingCode = response["bookingCode"] as? String,
                    checkoutUrl = response["checkoutUrl"] as? String,
                    accessCode = response["accessCode"] as? String
                ))
            } else {
                Result.failure(Exception(response["message"]?.toString() ?: "Booking failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveRental(customerId: Int): Result<Map<String, Any>?> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getActiveRental(customerId)
            if (response["success"] == true) {
                Result.success(response["rental"] as? Map<String, Any>)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRentalLocation(vehicleId: Int, lat: Double, lng: Double): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateRentalLocation(vehicleId, lat, lng)
            if (response["success"] == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update rental location"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRentalStatus(rentalId: Int, status: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateRentalStatus(rentalId, status)
            if (response["success"] == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update rental status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerRentals(customerId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCustomerRentals(customerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelRental(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.cancelRental(id)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endRental(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.endRental(id)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRentalDestination(rentalId: Int, location: String, lat: Double, lng: Double, stops: String? = null): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateRentalDestination(rentalId, location, lat, lng, stops)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverRentals(driverId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getDriverRentals(driverId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyRentalCode(rentalId: Int, code: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.verifyRentalHandshake(rentalId, code)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFleetVehicles(ownerId: Int): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getFleetVehicles(ownerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addFleetVehicle(
        ownerId: Int, name: String, model: String, type: String, number: String, rate: Double,
        description: String? = null, features: String? = null, imageUrls: String? = null,
        location: String? = null, lat: Double? = null, lng: Double? = null,
        seats: Int? = null, transmission: String? = null, fuelType: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.addFleetVehicle(
                ownerId, name, model, type, number, rate, description, features, imageUrls, location, lat, lng, seats, transmission, fuelType
            )
            if (response["success"] == true) Result.success(Unit)
            else Result.failure(Exception("Failed to add vehicle"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFleetVehicle(
        vehicleId: Int, name: String, model: String, type: String, number: String, rate: Double,
        description: String? = null, features: String? = null, imageUrls: String? = null,
        status: String? = null, seats: Int? = null, transmission: String? = null, fuelType: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateFleetVehicle(
                vehicleId, name, model, type, number, rate, description, features, imageUrls, status, seats, transmission, fuelType
            )
            if (response["success"] == true) Result.success(Unit)
            else Result.failure(Exception("Failed to update vehicle"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
