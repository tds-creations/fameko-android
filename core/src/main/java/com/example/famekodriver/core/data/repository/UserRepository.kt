package com.example.famekodriver.core.data.repository

import android.util.Log
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class UserRepository {

    suspend fun login(email: String, pass: String): Result<Driver?> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginDriver(LoginRequest(email, pass))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Driver(
                    id = userId.toIntOrNull() ?: 0,
                    fullName = response.name ?: "Driver",
                    email = email,
                    phone = "",
                    region = "",
                    licenseNumber = "",
                    vehicleType = "Car",
                    vehicleNumber = "",
                    status = response.status ?: "PENDING",
                    isOnline = false,
                    rating = 5.0,
                    serviceType = ServiceType.RIDE_HAILING,
                    userRole = response.user_role ?: "DRIVER",
                    companyName = response.company_name
                ))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Log.e("UserRepo", "API Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun customerLogin(email: String, pass: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginCustomer(LoginRequest(email, pass))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Pair(userId, response.name ?: "Customer"))
            } else {
                Result.failure(Exception(response.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepo", "Customer API Login failed", e)
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    suspend fun adminLogin(username: String, pass: String): Result<Admin> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginAdmin(LoginRequest(username, pass))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Admin(
                    id = userId.toIntOrNull() ?: 0,
                    username = response.name ?: username,
                    email = null,
                    role = response.user_role ?: "ADMIN"
                ))
            } else {
                Result.failure(Exception(response.message ?: "Admin login failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepo", "Admin API Login failed", e)
            Result.failure(e)
        }
    }

    suspend fun customerRegister(name: String, email: String, phone: String, address: String, password: String, region: String? = null, profilePicture: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = CustomerRegisterRequest(name, email, phone, address, password, region, profilePicture)
            val response = NetworkClient.famekoApi.registerCustomer(request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Log.e("UserRepo", "API Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    suspend fun driverRegister(
        name: String, email: String, phone: String, password: String, licenseNumber: String,
        region: String, vehicleType: String, serviceType: String, vehicleNumber: String,
        emergencyContact1: String, emergencyContact2: String,
        docs: Map<String, File> = emptyMap(), userRole: String = "DRIVER",
        companyName: String? = null, registrationNumber: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val nameBody = name.toRequestBody(MultipartBody.FORM)
            val emailBody = email.toRequestBody(MultipartBody.FORM)
            val phoneBody = phone.toRequestBody(MultipartBody.FORM)
            val passBody = password.toRequestBody(MultipartBody.FORM)
            val licenseBody = licenseNumber.toRequestBody(MultipartBody.FORM)
            val regionBody = region.toRequestBody(MultipartBody.FORM)
            val vTypeBody = vehicleType.toRequestBody(MultipartBody.FORM)
            val sTypeBody = serviceType.toRequestBody(MultipartBody.FORM)
            val vNumBody = vehicleNumber.toRequestBody(MultipartBody.FORM)
            val e1Body = emergencyContact1.toRequestBody(MultipartBody.FORM)
            val e2Body = emergencyContact2.toRequestBody(MultipartBody.FORM)
            val roleBody = userRole.toRequestBody(MultipartBody.FORM)
            val companyBody = companyName?.toRequestBody(MultipartBody.FORM)
            val regNumBody = registrationNumber?.toRequestBody(MultipartBody.FORM)

            fun fileToPart(key: String): MultipartBody.Part? {
                val file = docs[key] ?: return null
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                return MultipartBody.Part.createFormData(key, file.name, reqFile)
            }

            val response = NetworkClient.famekoApi.registerDriver(
                nameBody, emailBody, phoneBody, passBody, licenseBody, regionBody, vTypeBody, sTypeBody, vNumBody,
                e1Body, e2Body, roleBody, companyBody, regNumBody,
                fileToPart("profile_pic"),
                fileToPart("drivers_license"),
                fileToPart("insurance_cert"),
                fileToPart("roadworthy_cert"),
                fileToPart("ghana_card")
            )

            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Registration failed"))
        } catch (e: Exception) {
            Log.e("UserRepo", "Driver API Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.localizedMessage}"))
        }
    }

    suspend fun updateFcmToken(userId: String, token: String, type: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateFcmToken(userId, token, type)
            if (response["success"] == true) Result.success(Unit)
            else Result.failure(Exception("Failed to update FCM token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverStatus(driverId: String): Result<DriverStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getDriverStatus(driverId)
            Result.success(response)
        } catch (_: Exception) {
            Result.failure(Exception("Failed to get driver status"))
        }
    }

    suspend fun updateOnlineStatus(driverId: String, isOnline: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateOnlineStatus(driverId, isOnline)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message ?: "Failed to update status"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverStats(driverId: String): Result<DriverStats> = withContext(Dispatchers.IO) {
        try {
            val stats = NetworkClient.famekoApi.getDriverStats(driverId)
            Result.success(stats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEmergencyContacts(driverId: String, c1: String, c2: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateEmergencyContacts(driverId, c1, c2)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDriverVehicle(driverId: String, type: String, number: String, model: String, service: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateDriverVehicle(driverId, type, number, model, service)
            if (response["success"] == true) Result.success(Unit)
            else Result.failure(Exception("Failed to update vehicle"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSavedPlaces(customerId: String): Result<List<SavedPlace>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getSavedPlaces(customerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun savePlace(place: SavedPlace): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.savePlace(place)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSavedPlace(id: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.deleteSavedPlace(id)
            Result.success(response["success"] == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWalletBalance(driverId: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getWalletBalance(driverId)
            Result.success(response["balance"] ?: 0.0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWalletTransactions(driverId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getWalletTransactions(driverId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun requestTopUp(driverId: String, amount: Int, reference: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.requestTopUp(driverId, amount, reference)
            if (response.success) Result.success(Unit) else Result.failure(Exception(response.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTopUpHistory(driverId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getTopUpHistory(driverId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerWallet(customerId: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCustomerWallet(customerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerTransactions(customerId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCustomerTransactions(customerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSupportTickets(customerId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getSupportTickets(customerId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
