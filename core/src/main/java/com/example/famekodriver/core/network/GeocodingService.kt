package com.example.famekodriver.core.network

import com.example.famekodriver.core.domain.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

/**
 * Retrofit service for fetching location suggestions and routes from the Python backend
 */
interface FamekoApiService {
    @GET("customer/geocode")
    suspend fun getSuggestions(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5,
        @Query("countrycodes") countryCodes: String = "gh"
    ): List<LocationSuggestion>

    @POST("route/calculate")
    suspend fun calculateRoute(
        @Body request: RouteRequest
    ): RouteResponse

    @POST("customer/register")
    suspend fun registerCustomer(
        @Body request: CustomerRegisterRequest
    ): AuthResponse

    @POST("customer/login")
    suspend fun loginCustomer(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("customer/google-login")
    suspend fun loginCustomerGoogle(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("driver/login")
    suspend fun loginDriver(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("driver/google-login")
    suspend fun loginDriverGoogle(
        @Body request: LoginRequest
    ): AuthResponse

    @POST("admin/api-login")
    suspend fun loginAdmin(
        @Body request: LoginRequest
    ): AuthResponse

    // --- REMOVED PASSWORD RESET ---

    @Multipart
    @POST("driver/register")
    suspend fun registerDriver(
        @Part("full_name") name: RequestBody,
        @Part("email") email: RequestBody,
        @Part("phone") phone: RequestBody,
        @Part("password") password: RequestBody,
        @Part("license_number") licenseNumber: RequestBody,
        @Part("region") region: RequestBody,
        @Part("vehicle_type") vehicleType: RequestBody,
        @Part("service_type") serviceType: RequestBody,
        @Part("vehicle_number") vehicleNumber: RequestBody,
        @Part("emergency_contact_1") emergency1: RequestBody,
        @Part("emergency_contact_2") emergency2: RequestBody,
        @Part("user_role") userRole: RequestBody,
        @Part("company_name") companyName: RequestBody?,
        @Part("registration_number") registrationNumber: RequestBody?,
        @Part("firebase_uid") firebaseUid: RequestBody? = null,
        @Part profile_pic: MultipartBody.Part? = null,
        @Part drivers_license: MultipartBody.Part? = null,
        @Part insurance_cert: MultipartBody.Part? = null,
        @Part roadworthy_cert: MultipartBody.Part? = null,
        @Part ghana_card: MultipartBody.Part? = null
    ): AuthResponse

    @FormUrlEncoded
    @POST("driver/upload-document")
    suspend fun uploadDriverDocument(
        @Field("driver_id") driverId: String,
        @Field("doc_type") docType: String,
        @Field("file_url") fileUrl: String
    ): AuthResponse

    @GET("driver/status/{id}")
    suspend fun getDriverStatus(
        @Path("id") driverId: String
    ): DriverStatusResponse

    @GET("driver/my-deliveries/{id}")
    suspend fun getMyDeliveries(
        @Path("id") driverId: String
    ): List<Delivery>

    @GET("driver/available-deliveries")
    suspend fun getAvailableDeliveries(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("vehicle_type") vehicleType: String? = null,
        @Query("vehicle_category") vehicleCategory: String? = null
    ): List<Delivery>

    @FormUrlEncoded
    @POST("driver/update-emergency-contacts")
    suspend fun updateEmergencyContacts(
        @Field("driver_id") driverId: String,
        @Field("contact1") c1: String,
        @Field("contact2") c2: String
    ): AuthResponse

    @GET("driver/stats/{id}")
    suspend fun getDriverStats(
        @Path("id") driverId: String
    ): DriverStats

    @FormUrlEncoded
    @POST("driver/update-online-status")
    suspend fun updateOnlineStatus(
        @Field("driver_id") driverId: String,
        @Field("is_online") isOnline: Boolean
    ): AuthResponse

    @FormUrlEncoded
    @POST("driver/update-location")
    suspend fun updateLocation(
        @Field("driver_id") driverId: String,
        @Field("latitude") lat: Double,
        @Field("longitude") lng: Double,
        @Field("bearing") bearing: Float
    ): AuthResponse

    @FormUrlEncoded
    @POST("driver/pay-daily-fee")
    suspend fun payDailyFee(
        @Field("driver_id") driverId: String
    ): Map<String, Any>

    @POST("chat/send")
    suspend fun sendMessage(
        @Body message: Message
    ): Message

    @GET("chat/history/{convId}")
    suspend fun getChatHistory(
        @Path("convId") convId: Int
    ): List<Message>

    @GET("wallet/balance/{driverId}")
    suspend fun getWalletBalance(
        @Path("driverId") driverId: String
    ): Map<String, Double>

    @GET("wallet/transactions/{driverId}")
    suspend fun getWalletTransactions(
        @Path("driverId") driverId: String
    ): List<Map<String, Any>>

    @FormUrlEncoded
    @POST("wallet/request-topup")
    suspend fun requestTopUp(
        @Field("driver_id") driverId: String,
        @Field("amount") amount: Int,
        @Field("reference") reference: String
    ): AuthResponse

    @GET("wallet/topup-history/{driverId}")
    suspend fun getTopUpHistory(
        @Path("driverId") driverId: String
    ): List<Map<String, Any>>

    @POST("safety/sos")
    suspend fun triggerSOS(
        @Body request: SOSRequest
    ): AuthResponse

    @GET("safety/share-trip/{driverId}/{deliveryId}")
    suspend fun getShareableTripLink(
        @Path("driverId") driverId: String,
        @Path("deliveryId") deliveryId: String
    ): ShareTripResponse

    @GET("demand/heatmap")
    suspend fun getHeatmapData(): List<HeatmapPoint>

    @GET("demand/surge")
    suspend fun getCurrentSurge(): SurgeInfo

    @POST("orders/create")
    suspend fun createOrder(
        @Body request: OrderCreateRequest
    ): Map<String, Any>

    @GET("orders/status/{orderId}")
    suspend fun getOrderStatus(
        @Path("orderId") orderId: Int
    ): OrderStatusResponse

    @GET("orders/active/{customerId}")
    suspend fun getActiveOrder(
        @Path("customerId") customerId: String
    ): OrderStatusResponse

    @POST("orders/cancel/{orderId}")
    suspend fun cancelOrder(
        @Path("orderId") orderId: Int,
        @Query("reason") reason: String? = null
    ): Map<String, Any>

    @GET("driver/nearby")
    suspend fun getNearbyDrivers(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius") radius: Double = 5.0
    ): List<DriverLocation>

    @FormUrlEncoded
    @POST("driver/accept-delivery")
    suspend fun acceptDelivery(
        @Field("driver_id") driverId: String,
        @Field("delivery_id") deliveryId: String
    ): AuthResponse

    @FormUrlEncoded
    @POST("delivery/update-status")
    suspend fun updateDeliveryStatus(
        @Field("delivery_id") deliveryId: String,
        @Field("status") status: String
    ): AuthResponse

    @GET("customer/saved-places/{customerId}")
    suspend fun getSavedPlaces(
        @Path("customerId") customerId: String
    ): List<SavedPlace>

    @POST("customer/saved-places")
    suspend fun savePlace(
        @Body place: SavedPlace
    ): Map<String, Any>

    @PUT("customer/saved-places/{id}")
    suspend fun updateSavedPlace(
        @Path("id") id: String,
        @Body place: SavedPlace
    ): Map<String, Any>

    @DELETE("customer/saved-places/{id}")
    suspend fun deleteSavedPlace(
        @Path("id") id: String
    ): Map<String, Any>

    @GET("customer/trips/{customerId}")
    suspend fun getCustomerTrips(
        @Path("customerId") customerId: String
    ): List<Map<String, Any>>

    @DELETE("customer/trips/all/{customerId}")
    suspend fun clearAllTrips(
        @Path("customerId") customerId: String
    ): Map<String, Any>

    @DELETE("customer/trips/{tripId}")
    suspend fun deleteTrip(
        @Path("tripId") tripId: Int
    ): Map<String, Any>

    @GET("customer/promotions")
    suspend fun getPromotions(): List<Map<String, Any>>

    @GET("customer/wallet/{customerId}")
    suspend fun getCustomerWallet(
        @Path("customerId") customerId: String
    ): Map<String, Any>

    @GET("customer/wallet/transactions/{customerId}")
    suspend fun getCustomerTransactions(
        @Path("customerId") customerId: String
    ): List<Map<String, Any>>

    @GET("customer/discount-rate/{customerId}")
    suspend fun getDiscountRate(
        @Path("customerId") customerId: String
    ): Map<String, Int>

    @GET("customer/support/tickets/{customerId}")
    suspend fun getSupportTickets(
        @Path("customerId") customerId: String
    ): List<Map<String, Any>>

    @GET("pricing/config")
    suspend fun getPricingConfig(): PricingConfig

    @FormUrlEncoded
    @POST("orders/estimates")
    suspend fun getRideEstimates(
        @Field("lat") lat: Double,
        @Field("lng") lng: Double,
        @Field("dist") distance: Double,
        @Field("dur") duration: Double,
        @Field("region") region: String? = null
    ): List<RideEstimateResponse>

    @GET("rentals/vehicles")
    suspend fun getRentalVehicles(): List<Map<String, Any>>

    @GET("rentals/rates")
    suspend fun getRentalRates(): List<Map<String, Any>>

    @POST("rentals/book")
    suspend fun bookRental(@Body request: RentalBookRequest): Map<String, Any>

    @GET("rentals/active/{customerId}")
    suspend fun getActiveRental(@Path("customerId") customerId: Int): Map<String, Any>

    @GET("customer/rentals/{customerId}")
    suspend fun getCustomerRentals(@Path("customerId") customerId: String): List<Map<String, Any>>

    @GET("customer/profile/{id}")
    suspend fun getCustomerProfile(@Path("id") id: String): Map<String, Any>

    @GET("driver/profile/{id}")
    suspend fun getDriverProfile(@Path("id") id: String): Map<String, Any>

    @POST("rentals/cancel/{id}")
    suspend fun cancelRental(@Path("id") id: Int): Map<String, Any>

    @FormUrlEncoded
    @POST("rentals/update-location")
    suspend fun updateRentalLocation(
        @Field("vehicleId") vehicleId: Int,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double
    ): Map<String, Any>

    @FormUrlEncoded
    @POST("rentals/update-status")
    suspend fun updateRentalStatus(
        @Field("id") id: Int,
        @Field("status") status: String
    ): Map<String, Any>

    @POST("rentals/end/{id}")
    suspend fun endRental(@Path("id") id: Int): Map<String, Any>

    @FormUrlEncoded
    @POST("rentals/update-destination")
    suspend fun updateRentalDestination(
        @Field("rentalId") rentalId: Int,
        @Field("location") location: String,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double,
        @Field("stops") stops: String? = null
    ): Map<String, Any>

    @GET("driver/rentals/{driverId}")
    suspend fun getDriverRentals(@Path("driverId") driverId: String): List<Map<String, Any>>

    @FormUrlEncoded
    @POST("driver/rentals/verify")
    suspend fun verifyRentalHandshake(
        @Field("rentalId") rentalId: Int,
        @Field("code") code: String
    ): Map<String, Any>

    @FormUrlEncoded
    @POST("driver/submit-rating")
    suspend fun submitRating(
        @Field("driver_id") driverId: String,
        @Field("order_id") orderId: Int,
        @Field("rating") rating: Float,
        @Field("comment") comment: String
    ): AuthResponse

    @FormUrlEncoded
    @POST("orders/verify-pin")
    suspend fun verifyOrderPin(
        @Field("orderId") orderId: Int,
        @Field("pin") pin: String
    ): Map<String, Any>

    // --- FLEET MANAGEMENT ---
    @FormUrlEncoded
    @POST("driver/update-vehicle")
    suspend fun updateDriverVehicle(
        @Field("driver_id") driverId: String,
        @Field("vehicle_type") type: String,
        @Field("vehicle_number") number: String,
        @Field("vehicle_model") model: String,
        @Field("service_types") service: String
    ): Map<String, Any>

    @GET("fleet/vehicles")
    suspend fun getFleetVehicles(
        @Query("owner_id") ownerId: Int
    ): List<Map<String, Any>>

    @FormUrlEncoded
    @POST("fleet/add-vehicle")
    suspend fun addFleetVehicle(
        @Field("owner_id") ownerId: Int,
        @Field("name") name: String,
        @Field("model") model: String,
        @Field("type") type: String,
        @Field("number") number: String,
        @Field("rate") rate: Double,
        @Field("description") description: String? = null,
        @Field("features") features: String? = null,
        @Field("image_urls") imageUrls: String? = null,
        @Field("location") location: String? = null,
        @Field("lat") lat: Double? = null,
        @Field("lng") lng: Double? = null,
        @Field("seats") seats: Int? = null,
        @Field("transmission") transmission: String? = null,
        @Field("fuel_type") fuelType: String? = null
    ): Map<String, Any>

    @FormUrlEncoded
    @POST("fleet/update-vehicle")
    suspend fun updateFleetVehicle(
        @Field("vehicle_id") vehicleId: Int,
        @Field("name") name: String,
        @Field("model") model: String,
        @Field("type") type: String,
        @Field("number") number: String,
        @Field("rate") rate: Double,
        @Field("description") description: String? = null,
        @Field("features") features: String? = null,
        @Field("image_urls") imageUrls: String? = null,
        @Field("status") status: String? = null,
        @Field("seats") seats: Int? = null,
        @Field("transmission") transmission: String? = null,
        @Field("fuel_type") fuelType: String? = null
    ): Map<String, Any>

    @GET("api/admin/live-locations")
    suspend fun getLiveLocations(): List<Map<String, Any>>

    @GET("api/admin/stats")
    suspend fun getAdminStats(): AdminPlatformStats

    @GET("api/admin/active-sos")
    suspend fun getActiveSOS(): List<SOSAlert>

    @GET("api/admin/pending-drivers")
    suspend fun getPendingDrivers(): List<Map<String, Any>>

    @GET("api/admin/driver/{id}")
    suspend fun getAdminDriverDetails(@Path("id") id: Int): Map<String, Any>

    @GET("api/admin/customers")
    suspend fun getAdminCustomers(): List<Map<String, Any>>

    @GET("api/admin/deliveries")
    suspend fun getAdminDeliveries(): List<Map<String, Any>>

    @GET("api/admin/rentals")
    suspend fun getAdminRentals(): List<Map<String, Any>>

    @GET("api/admin/daily-payments")
    suspend fun getPendingPayments(): List<Map<String, Any>>

    @GET("api/admin/financials")
    suspend fun getAdminFinancials(): Map<String, Any>

    @GET("api/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double
    ): WeatherResponse

    @POST("api/admin/payments/approve/{id}")
    suspend fun approvePayment(@Path("id") id: Int): Map<String, Any>

    @POST("api/admin/payments/reject/{id}")
    suspend fun rejectPayment(@Path("id") id: Int): Map<String, Any>

    @POST("api/admin/driver/approve/{id}")
    suspend fun approveDriver(@Path("id") id: Int): Map<String, Any>

    @POST("api/admin/driver/reject/{id}")
    suspend fun rejectDriver(@Path("id") id: Int): Map<String, Any>

    @POST("admin/resolve-sos/{id}")
    suspend fun resolveSOS(@Path("id") id: Int): Map<String, Any>

    @POST("update-fcm-token")
    suspend fun updateFcmToken(
        @Field("userId") userId: String,
        @Field("token") token: String,
        @Field("type") type: String
    ): Map<String, Any>
}
