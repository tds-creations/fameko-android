package com.example.famekodriver.core.data.repository

import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.network.NetworkClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import okio.ByteString
import okio.ByteString.Companion.toByteString

/**
 * Repository for driver-related database operations.
 */
class DriverRepository {
    private val gson = Gson()
    private val _events = MutableSharedFlow<FamekoEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<FamekoEvent> = _events

    private var webSocket: WebSocket? = null
    private var currentUserId: String? = null
    private var isReconnectEnabled = false

    fun startWebSocket(userId: String) {
        currentUserId = userId
        isReconnectEnabled = true
        connectWebSocket()
    }

    private fun connectWebSocket() {
        val userId = currentUserId ?: return
        if (!isReconnectEnabled) return

        stopWebSocketOnly() // Close existing if any
        
        Log.d("FamekoWS", "Connecting WebSocket for $userId")
        val request = Request.Builder()
            .url(NetworkClient.getWebSocketUrl(userId))
            .build()
        
        webSocket = NetworkClient.okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("FamekoWS", "WebSocket Opened for $userId")
                _events.tryEmit(FamekoEvent.Ping)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("FamekoWS", "Received text: $text")
                try {
                    val wsMessage = gson.fromJson(text, WebSocketMessage::class.java)
                    Log.d("FamekoWS", "Parsed message type: ${wsMessage.type}")
                    when (wsMessage.type) {
                        "NEW_DELIVERY" -> {
                            val delivery = gson.fromJson(wsMessage.payload, Delivery::class.java)
                            Log.d("FamekoWS", "Emitting NewDeliveryRequest for delivery ${delivery.id}")
                            val emitted = _events.tryEmit(FamekoEvent.NewDeliveryRequest(delivery))
                            Log.d("FamekoWS", "Emission success: $emitted")
                        }
                        "CALL_INCOMING", "call_incoming", "driver_call_incoming" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val callId = data["call_id"]?.toString() ?: ""
                            val name = data["customer_name"]?.toString() ?: data["driver_name"]?.toString() ?: "Someone"
                            _events.tryEmit(FamekoEvent.IncomingCall(callId, name))
                        }
                        "CALL_ACCEPTED", "call_accepted" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val callId = data["call_id"]?.toString() ?: ""
                            _events.tryEmit(FamekoEvent.CallAccepted(callId))
                        }
                        "CALL_REJECTED", "call_rejected" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val callId = data["call_id"]?.toString() ?: ""
                            val reason = data["reason"]?.toString()
                            _events.tryEmit(FamekoEvent.CallRejected(callId, reason))
                        }
                        "CALL_ENDED", "call_ended" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val callId = data["call_id"]?.toString() ?: ""
                            _events.tryEmit(FamekoEvent.CallEnded(callId))
                        }
                        "NEW_DELIVERY" -> {
                            try {
                                val delivery = gson.fromJson(wsMessage.payload, Delivery::class.java)
                                _events.tryEmit(FamekoEvent.NewDeliveryRequest(delivery))
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        "STATUS_CHANGED" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val deliveryId = data["delivery_id"]?.toString() ?: ""
                            val statusStr = data["status"]?.toString() ?: ""
                            val orderId = (data["order_id"] ?: data["orderId"] ?: data["id"])?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            try {
                                val status = DeliveryStatus.valueOf(statusStr.uppercase())
                                _events.tryEmit(FamekoEvent.DeliveryStatusChanged(deliveryId, status))
                                if (orderId != 0) _events.tryEmit(FamekoEvent.OrderAccepted(orderId))
                            } catch (_: Exception) {
                                if (orderId != 0) _events.tryEmit(FamekoEvent.OrderStatusUpdate(orderId))
                            }
                        }
                        "NEW_MESSAGE" -> {
                            val message = gson.fromJson(wsMessage.payload, Message::class.java)
                            _events.tryEmit(FamekoEvent.NewMessage(message))
                        }
                        "ORDER_CANCELLED" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val orderId = (data["orderId"] ?: data["order_id"])?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            if (orderId != 0) {
                                _events.tryEmit(FamekoEvent.OrderCancelled(orderId))
                            }
                        }
                        "ORDER_ACCEPTED" -> {
                            Log.d("FamekoWS", "Order Accepted raw payload: ${wsMessage.payload}")
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val orderId = (data["orderId"] ?: data["order_id"] ?: data["id"])?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            if (orderId != 0) {
                                _events.tryEmit(FamekoEvent.OrderAccepted(orderId))
                            } else {
                                // Try parsing whole object
                                try {
                                    val delivery = gson.fromJson(wsMessage.payload, Delivery::class.java)
                                    _events.tryEmit(FamekoEvent.OrderAccepted(delivery.orderId))
                                } catch (_: Exception) {}
                            }
                        }
                        "ORDER_STATUS_UPDATE" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val orderId = (data["orderId"] ?: data["order_id"])?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            if (orderId != 0) {
                                _events.tryEmit(FamekoEvent.OrderStatusUpdate(orderId))
                            } else {
                                try {
                                    val delivery = gson.fromJson(wsMessage.payload, Delivery::class.java)
                                    _events.tryEmit(FamekoEvent.OrderStatusUpdate(delivery.orderId))
                                } catch (_: Exception) {}
                            }
                        }
                        "DRIVER_LOCATION_UPDATE" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val dId = data["driverId"]?.toString() ?: ""
                            val lat = data["lat"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val lng = data["lng"]?.toString()?.toDoubleOrNull() ?: 0.0
                            val bearing = data["bearing"]?.toString()?.toFloatOrNull() ?: 0f
                            _events.tryEmit(FamekoEvent.DriverLocationUpdate(dId, lat, lng, bearing))
                        }
                        "NEARBY_DRIVERS" -> {
                            val listType = object : com.google.gson.reflect.TypeToken<List<DriverLocation>>() {}.type
                            val drivers = gson.fromJson<List<DriverLocation>>(wsMessage.payload, listType)
                            _events.tryEmit(FamekoEvent.NearbyDriversUpdate(drivers))
                        }
                        "DRIVER_STATS_UPDATE" -> {
                            val stats = gson.fromJson(wsMessage.payload, DriverStats::class.java)
                            _events.tryEmit(FamekoEvent.DriverStatsUpdate(stats))
                        }
                        "NOTIFICATION_RECEIVED" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val id = (data["id"]?.toString()?.toDoubleOrNull()?.toInt() ?: 0)
                            val title = data["title"]?.toString() ?: ""
                            val msg = data["message"]?.toString() ?: ""
                            val type = data["type"]?.toString() ?: "GENERAL"
                            val createdAt = data["createdAt"]?.toString() ?: ""
                            _events.tryEmit(FamekoEvent.NotificationReceived(id, title, msg, type, createdAt))
                        }
                        "RENTAL_DESTINATION_UPDATED" -> {
                            val data = gson.fromJson(wsMessage.payload, Map::class.java)
                            val rentalId = (data["rentalId"] ?: data["rental_id"])?.toString()?.toDoubleOrNull()?.toInt() ?: 0
                            val location = data["location"]?.toString() ?: ""
                            if (rentalId != 0) {
                                _events.tryEmit(FamekoEvent.RentalDestinationUpdated(rentalId, location))
                            }
                        }
                        else -> {
                            try {
                                val data = gson.fromJson(wsMessage.payload, Map::class.java) as Map<String, Any>
                                _events.tryEmit(FamekoEvent.Unknown(wsMessage.type, data))
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _events.tryEmit(FamekoEvent.AudioDataReceived(bytes.toByteArray()))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("FamekoWS", "WebSocket Failure: ${t.localizedMessage}")
                if (isReconnectEnabled) {
                    Log.d("FamekoWS", "Attempting to reconnect in 5 seconds...")
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(5000)
                        connectWebSocket()
                    }
                }
            }
        })
    }

    private fun stopWebSocketOnly() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }

    fun sendAudioData(data: ByteArray) {
        webSocket?.send(data.toByteString())
    }

    suspend fun initiateCall(targetId: String, callerName: String, orderId: Int): Result<Unit> {
        val payload = mapOf(
            "call_id" to UUID.randomUUID().toString(),
            "caller_name" to callerName,
            "order_id" to orderId,
            "target_id" to targetId
        )
        return try {
            webSocket?.send(gson.toJson(WebSocketMessage("CALL_INITIATE", gson.toJson(payload))))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptCall(callId: String): Result<Unit> {
        return try {
            webSocket?.send(gson.toJson(WebSocketMessage("CALL_ACCEPT", gson.toJson(mapOf("call_id" to callId)))))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectCall(callId: String, reason: String? = null): Result<Unit> {
        return try {
            webSocket?.send(gson.toJson(WebSocketMessage("CALL_REJECT", gson.toJson(mapOf("call_id" to callId, "reason" to reason)))))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun endCall(callId: String): Result<Unit> {
        return try {
            webSocket?.send(gson.toJson(WebSocketMessage("CALL_END", gson.toJson(mapOf("call_id" to callId)))))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopWebSocket() {
        isReconnectEnabled = false
        stopWebSocketOnly()
    }

    /**
     * Authenticates a driver by phone and password
     */
    suspend fun login(phone: String, pass: String): Result<Driver?> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginDriver(LoginRequest(phone = phone, password = pass))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Driver(
                    id = userId.toIntOrNull() ?: 0,
                    fullName = response.name ?: "Driver",
                    email = "",
                    phone = phone,
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
            Log.e("FamekoRepo", "API Login failed", e)
            Result.failure(e)
        }
    }





    suspend fun driverGoogleLogin(idToken: String): Result<Driver?> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginDriverGoogle(LoginRequest(googleToken = idToken))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Driver(
                    id = userId.toIntOrNull() ?: 0,
                    fullName = response.name ?: "Driver",
                    email = "",
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
                Result.failure(Exception(response.message ?: "Google login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun customerLoginByPhone(phone: String, pass: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginCustomer(LoginRequest(phone = phone, password = pass))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Pair(userId, response.name ?: "Customer"))
            } else {
                Result.failure(Exception(response.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Log.e("FamekoRepo", "Customer API Login failed", e)
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }





    suspend fun customerGoogleLogin(idToken: String): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginCustomerGoogle(LoginRequest(googleToken = idToken))
            val userId = response.user_id
            if (response.success && userId != null) {
                Result.success(Pair(userId, response.name ?: "Customer"))
            } else {
                Result.failure(Exception(response.message ?: "Google login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REMOVED PASSWORD RESET ---

    suspend fun adminLogin(username: String, pass: String): Result<Admin> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.loginAdmin(LoginRequest(email = username, password = pass))
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
            Log.e("FamekoRepo", "Admin API Login failed", e)
            Result.failure(e)
        }
    }

    /**
     * Fetches driver statistics
     */
    suspend fun getDriverStats(driverId: String): Result<DriverStats> = withContext(Dispatchers.IO) {
        try {
            val stats = NetworkClient.famekoApi.getDriverStats(driverId)
            Result.success(stats)
        } catch (e: Exception) {
            Log.e("FamekoRepo", "API getDriverStats failed", e)
            Result.failure(e)
        }
    }

    /**
     * Updates driver's online status
     */
    suspend fun updateOnlineStatus(driverId: String, isOnline: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateOnlineStatus(driverId, isOnline)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update status via API"))
            }
        } catch (e: Exception) {
            Log.e("FamekoRepo", "API status update failed", e)
            Result.failure(e)
        }
    }

    suspend fun customerRegister(name: String, email: String, phone: String, address: String, password: String, region: String? = null, profilePicture: String? = null, firebaseUid: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = CustomerRegisterRequest(name, email, phone, address, password, region, profilePicture, firebaseUid)
            val response = NetworkClient.famekoApi.registerCustomer(request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Log.e("FamekoRepo", "API Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    /**
     * Register a new driver with documents using the backend API
     */
    suspend fun driverRegister(
        name: String,
        email: String,
        phone: String,
        password: String,
        licenseNumber: String,
        region: String,
        vehicleType: String,
        serviceType: String,
        vehicleNumber: String,
        emergencyContact1: String,
        emergencyContact2: String,
        docs: Map<String, File> = emptyMap(),
        userRole: String = "DRIVER",
        companyName: String? = null,
        registrationNumber: String? = null,
        firebaseUid: String? = null
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
            val uidBody = firebaseUid?.toRequestBody(MultipartBody.FORM)

            fun fileToPart(key: String): MultipartBody.Part? {
                val file = docs[key] ?: return null
                val reqFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                return MultipartBody.Part.createFormData(key, file.name, reqFile)
            }

            val response = NetworkClient.famekoApi.registerDriver(
                nameBody, emailBody, phoneBody, passBody, licenseBody, regionBody, vTypeBody, sTypeBody, vNumBody,
                e1Body, e2Body, roleBody, companyBody, regNumBody, uidBody,
                fileToPart("profile_pic"),
                fileToPart("drivers_license"),
                fileToPart("insurance_cert"),
                fileToPart("roadworthy_cert"),
                fileToPart("ghana_card")
            )

            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Log.e("FamekoRepo", "Driver API Registration failed", e)
            Result.failure(Exception("Registration failed: ${e.localizedMessage}"))
        }
    }

    suspend fun uploadImage(file: File, preset: String = "fameko_docs"): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Both presets use the same cloud name 'df3jnubvy' as seen in Cloudinary dashboard
            val cloudName = "df3jnubvy"
            val cloudinaryUrl = "https://api.cloudinary.com/v1_1/$cloudName/image/upload"
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                .addFormDataPart("upload_preset", preset)
                .build()

            val request = Request.Builder()
                .url(cloudinaryUrl)
                .post(requestBody)
                .build()

            val response = NetworkClient.okHttpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("Cloudinary upload failed: ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response from Cloudinary")
            val jsonResponse = JsonParser.parseString(responseBody).asJsonObject
            val fileUrl = jsonResponse.get("secure_url").asString

            Result.success(fileUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadDocument(driverId: String, docType: String, file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            uploadImage(file).onSuccess { fileUrl ->
                val backendResponse = NetworkClient.famekoApi.uploadDriverDocument(driverId, docType, fileUrl)
                return@withContext if (backendResponse.success) {
                    Result.success(Unit)
                } else {
                    Result.failure(Exception(backendResponse.message ?: "Backend rejected the link"))
                }
            }.onFailure {
                return@withContext Result.failure(it)
            }
            Result.failure(Exception("Unknown error"))
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

    suspend fun updateEmergencyContacts(driverId: String, c1: String, c2: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateEmergencyContacts(driverId, c1, c2)
            if (response.success) Result.success(Unit)
            else Result.failure(Exception(response.message))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDriverVehicle(
        driverId: String,
        type: String,
        number: String,
        model: String,
        service: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateDriverVehicle(driverId, type, number, model, service)
            if (response["success"] == true) Result.success(Unit)
            else Result.failure(Exception("Failed to update vehicle"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableDeliveries(lat: Double = 0.0, lng: Double = 0.0, vehicleType: String? = null, vehicleCategory: String? = null): Result<List<Delivery>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAvailableDeliveries(lat, lng, vehicleType, vehicleCategory)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("FamekoRepo", "API Get Available Deliveries failed", e)
            Result.failure(e)
        }
    }

    suspend fun getMyDeliveries(driverId: String): Result<List<Delivery>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getMyDeliveries(driverId)
            Result.success(response)
        } catch (e: Exception) {
            Log.e("FamekoRepo", "API Get My Deliveries failed", e)
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
            Log.e("FamekoRepo", "Failed to accept delivery via API", e)
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
            Log.e("FamekoRepo", "API status update failed", e)
            Result.failure(e)
        }
    }

    suspend fun updateLocation(driverId: String, lat: Double, lng: Double, bearing: Float): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateLocation(driverId, lat, lng, bearing)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to update location via API"))
            }
        } catch (e: Exception) {
            Log.e("FamekoRepo", "API Location update failed", e)
            Result.failure(e)
        }
    }

    suspend fun payDailyFee(driverId: String): Result<String?> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.payDailyFee(driverId)
            if (response["success"] == true) {
                Result.success(response["checkoutUrl"] as? String)
            } else {
                Result.failure(Exception(response["message"]?.toString() ?: "Failed to initialize payment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calculateRoute(request: RouteRequest): Result<RouteResponse> = withContext(Dispatchers.IO) {
        // Priority 1: OSRM (OpenStreetMap based, often more accurate local road data)
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
            Log.e("FamekoRepo", "OSM Routing failed, falling back to TomTom", e)
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
            Log.e("FamekoRepo", "TomTom Routing failed as well, falling back to backend", e)
            try {
                // Priority 3: Backend as last resort
                val response = NetworkClient.famekoApi.calculateRoute(request)
                Result.success(response)
            } catch (e3: Exception) {
                if (e3 is CancellationException) throw e3
                Result.failure(Exception("Routing failed completely"))
            }
        }
    }

    suspend fun sendMessage(message: Message): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.sendMessage(message)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChatHistory(convId: Int): Result<List<Message>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getChatHistory(convId)
            Result.success(response)
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

    suspend fun triggerSOS(driverId: String, lat: Double, lng: Double, type: String = "GENERAL"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = SOSRequest(driverId, lat, lng, type)
            val response = NetworkClient.famekoApi.triggerSOS(request)
            if (response.success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.message ?: "Failed to trigger SOS"))
            }
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

    suspend fun getHeatmapData(): Result<List<HeatmapPoint>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getHeatmapData()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentSurge(): Result<SurgeInfo> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCurrentSurge()
            Result.success(response)
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

    suspend fun getCustomerTrips(customerId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCustomerTrips(customerId)
            Result.success(response)
        } catch (e: Exception) {
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

    suspend fun getPricingConfig(): Result<PricingConfig> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getPricingConfig()
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

    suspend fun getDiscountRate(customerId: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getDiscountRate(customerId)
            Result.success(response["discount_percentage"] ?: 0)
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

    suspend fun getOrderStatus(orderId: Int): Result<OrderStatusResponse> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getOrderStatus(orderId)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverProfile(id: String): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getDriverProfile(id)
            Result.success(response)
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

    suspend fun getRentalVehicles(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getRentalVehicles()
            Result.success(response)
        } catch (e: Exception) {
            Log.e("FamekoRepo", "Failed to fetch rental vehicles", e)
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

    suspend fun cancelOrder(orderId: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.cancelOrder(orderId)
            if (response["success"] == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response["message"]?.toString() ?: "Cancellation failed"))
            }
        } catch (e: Exception) {
            Log.e("FamekoRepo", "Order cancellation via API failed", e)
            Result.failure(e)
        }
    }

    suspend fun getGeocodeSuggestions(query: String): Result<List<LocationSuggestion>> = withContext(Dispatchers.IO) {
        // Priority 1: OSM Nominatim (Often more detailed local locations in Ghana)
        try {
            val osmResponse = NetworkClient.osmService.search(query)
            val filteredResults = osmResponse.map { suggestion ->
                suggestion.copy(
                    name = suggestion.displayName.split(",")[0],
                    type = "address"
                )
            }
            if (filteredResults.isNotEmpty()) {
                return@withContext Result.success(filteredResults)
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("FamekoRepo", "OSM Geocode failed, falling back to TomTom", e)
        }

        // Priority 2: TomTom
        try {
            val tomTomResponse = NetworkClient.tomTomService.fuzzySearch(
                query = query,
                apiKey = NetworkClient.TOMTOM_API_KEY
            )

            if (!tomTomResponse.results.isNullOrEmpty()) {
                val suggestions = tomTomResponse.results!!.mapNotNull { result ->
                    val addr = result.address?.freeformAddress ?: return@mapNotNull null
                    val rLat = result.position?.lat ?: return@mapNotNull null
                    val rLon = result.position?.lon ?: return@mapNotNull null
                    LocationSuggestion(
                        displayName = addr,
                        latitude = rLat.toString(),
                        longitude = rLon.toString(),
                        name = result.poi?.name ?: addr.split(",")[0],
                        type = result.type ?: "address"
                    )
                }
                if (suggestions.isNotEmpty()) {
                    return@withContext Result.success(suggestions)
                }
            }
            Result.failure(Exception("Geocode failed: No results found"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("FamekoRepo", "TomTom Geocode failed as well", e)
            Result.failure(e)
        }
    }

    suspend fun reverseGeocode(lat: Double, lng: Double): Result<LocationSuggestion> = withContext(Dispatchers.IO) {
        // Priority 1: OSM (Often more detailed in local areas)
        try {
            val response = NetworkClient.osmService.reverse(lat, lng)
            return@withContext Result.success(response.copy(
                name = response.displayName.split(",")[0],
                type = "address"
            ))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("FamekoRepo", "OSM Reverse geocode failed, falling back to TomTom", e)
        }

        // Priority 2: TomTom
        try {
            val tomTomResponse = NetworkClient.tomTomService.reverseGeocode(
                lat = lat,
                lon = lng,
                apiKey = NetworkClient.TOMTOM_API_KEY
            )

            if (!tomTomResponse.results.isNullOrEmpty()) {
                val result = tomTomResponse.results!![0]
                val addr = result.address?.freeformAddress
                if (addr != null) {
                    return@withContext Result.success(LocationSuggestion(
                        displayName = addr,
                        latitude = lat.toString(),
                        longitude = lng.toString(),
                        name = result.poi?.name ?: addr.split(",")[0],
                        type = "address"
                    ))
                }
            }
            Result.failure(Exception("Reverse geocode failed: No results found"))
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Log.e("FamekoRepo", "TomTom reverse geocode failed as well", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new order and delivery request in the database
     */
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
            Log.e("FamekoRepo", "Order creation via API failed", e)
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

    suspend fun updateFcmToken(userId: String, token: String, type: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.updateFcmToken(userId, token, type)
            if (response["success"] == true) Result.success(Unit)
            else Result.failure(Exception("Failed to update FCM token"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminLiveLocations(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getLiveLocations()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminPlatformStats(): Result<AdminPlatformStats> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAdminStats()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminActiveSOS(): Result<List<SOSAlert>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getActiveSOS()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminPendingDrivers(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getPendingDrivers()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminDriverDetails(id: Int): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAdminDriverDetails(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminCustomers(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAdminCustomers()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminDeliveries(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAdminDeliveries()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminRentals(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAdminRentals()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminPendingPayments(): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getPendingPayments()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAdminFinancials(): Result<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getAdminFinancials()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approvePayment(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.approvePayment(id)
            if (response["success"] == true) Result.success(Unit) else Result.failure(Exception("Failed to approve payment"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectPayment(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.rejectPayment(id)
            if (response["success"] == true) Result.success(Unit) else Result.failure(Exception("Failed to reject payment"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun approveDriver(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.approveDriver(id)
            if (response["success"] == true) Result.success(Unit) else Result.failure(Exception("Failed to approve"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectDriver(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.rejectDriver(id)
            if (response["success"] == true) Result.success(Unit) else Result.failure(Exception("Failed to reject"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveSOS(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.resolveSOS(id)
            if (response["success"] == true) Result.success(Unit) else Result.failure(Exception("Failed to resolve SOS"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentWeather(lat: Double, lon: Double): Result<WeatherResponse> = withContext(Dispatchers.IO) {
        try {
            val response = NetworkClient.famekoApi.getCurrentWeather(lat, lon)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
