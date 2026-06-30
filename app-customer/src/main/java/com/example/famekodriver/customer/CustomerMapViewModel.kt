package com.example.famekodriver.customer

import android.location.Location
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.*
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.RegionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CustomerMapViewModel(
    private val repository: DriverRepository, // Kept for WebSockets and general tasks
    private val orderRepository: OrderRepository,
    private val rentalRepository: RentalRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
) : ViewModel() {

    // --- Screen State ---
    var currentScreen by mutableStateOf<CustomerScreen>(CustomerScreen.Landing)
        private set

    // --- Map & Location State ---
    var drivers by mutableStateOf<List<DriverLocation>>(emptyList())
        private set
    
    var pickupLocation by mutableStateOf("")
    var dropOffLocation by mutableStateOf("")
    var pickupLat by mutableStateOf<Double?>(null)
    var pickupLng by mutableStateOf<Double?>(null)
    var dropOffLat by mutableStateOf<Double?>(null)
    var dropOffLng by mutableStateOf<Double?>(null)
    var stops by mutableStateOf<List<String>>(emptyList())

    var rentalPickupLocation by mutableStateOf("")
    var rentalPickupLat by mutableStateOf<Double?>(null)
    var rentalPickupLng by mutableStateOf<Double?>(null)

    // --- Ride & Pricing State ---
    var polylinePoints by mutableStateOf<List<GeoPoint>>(emptyList())
    var isLoading by mutableStateOf(false)
    var estimatedFare by mutableStateOf<Double?>(null)
    var rideEstimates by mutableStateOf<List<RideEstimateResponse>>(emptyList())
    var distanceKm by mutableDoubleStateOf(0.0)
    var durationMin by mutableDoubleStateOf(0.0)
    var pickupEtaMin by mutableStateOf<Double?>(null)
    var isOrderPlacing by mutableStateOf(false)
    var selectedVehicleType by mutableStateOf("Economy")
    var discountRate by mutableIntStateOf(0)
    var pricingConfig by mutableStateOf<PricingConfig?>(null)
    var rentalRates by mutableStateOf<List<Map<String, Any>>>(emptyList())
    var scheduledRideTime by mutableStateOf<String?>(null)
        private set

    // --- Order & Call State ---
    var currentOrderId by mutableStateOf(sessionManager.getActiveOrderId())
    var orderStatusData by mutableStateOf<OrderStatusResponse?>(null)
    var incomingCall by mutableStateOf<FamekoEvent.IncomingCall?>(null)
    var ongoingCall by mutableStateOf<FamekoEvent.IncomingCall?>(null)
    
    var activeServiceMode by mutableStateOf(ServiceType.RIDE_HAILING)
    var isServiceModeSelected by mutableStateOf(true)
    var isSearchMode by mutableStateOf(false)
    var activeRental by mutableStateOf<Map<String, Any>?>(null)
    var savedPlaces by mutableStateOf<List<SavedPlace>>(emptyList())
    var recentPlaces by mutableStateOf<List<LocationSuggestion>>(emptyList())
    var selectedTab by mutableIntStateOf(0) // 0 for Recent, 1 for Saved

    // --- Rating State ---
    var showRatingDialog by mutableStateOf(false)
    var showTripSummary by mutableStateOf(false)
    var finalFare by mutableDoubleStateOf(0.0)
    var ratingDriverId by mutableStateOf<String?>(null)
    var ratingDriverName by mutableStateOf<String?>(null)
    var ratingDriverPic by mutableStateOf<String?>(null)
    var ratingOrderId by mutableStateOf<Int?>(null)

    // --- Suggestions ---
    var pickupSuggestions by mutableStateOf<List<LocationSuggestion>>(emptyList())
    var dropOffSuggestions by mutableStateOf<List<LocationSuggestion>>(emptyList())
    
    var currentRegion by mutableStateOf<String?>(null)
    var showRegionalError by mutableStateOf<String?>(null)
    
    var notifications by mutableStateOf<List<FamekoEvent.NotificationReceived>>(emptyList())
    var lastTriggeredNotificationId by mutableIntStateOf(-1)
    var isTimedOut by mutableStateOf(false)
    var retryAttempt by mutableIntStateOf(0)
    var maxRetryAttempts by mutableIntStateOf(3)
    var searchRadiusKm by mutableDoubleStateOf(3.0)
    var searchMessage by mutableStateOf("Connecting you to the nearest available Fameko")
    var showCancelConfirmation by mutableStateOf(false)

    private var pollingJob: Job? = null
    private var rentalPollingJob: Job? = null
    private var pickupSearchJob: Job? = null
    private var dropOffSearchJob: Job? = null
    private var isSelectingSuggestion = false

    init {
        loadInitialData()
        startWebSocket()
        startActiveRentalPolling()
        startPricingPolling()
    }

    private fun startPricingPolling() {
        viewModelScope.launch {
            while (true) {
                if (estimatedFare != null && currentOrderId == null) {
                    orderRepository.getPricingConfig().onSuccess { newConfig ->
                        if (newConfig != pricingConfig) {
                            pricingConfig = newConfig
                            updateEstimatedFare()
                        }
                    }
                }
                delay(10.seconds)
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val customerId = sessionManager.getDriverId() ?: "1"
            userRepository.getSavedPlaces(customerId).onSuccess { savedPlaces = it }
            orderRepository.getPricingConfig().onSuccess { pricingConfig = it }
            rentalRepository.getRentalRates().onSuccess { rentalRates = it }
            orderRepository.getDiscountRate(customerId).onSuccess { discountRate = it }
            
            orderRepository.getCustomerTrips(customerId).onSuccess { trips ->
                recentPlaces = trips.mapNotNull { trip ->
                    val dropOffLabel = trip["dropoff"]?.toString()
                    val lat = trip["dropoff_lat"]?.toString()
                    val lon = trip["dropoff_lng"]?.toString()
                    if (dropOffLabel != null && lat != null && lon != null) {
                        LocationSuggestion(displayName = dropOffLabel, latitude = lat, longitude = lon, type = "recent")
                    } else null
                }.distinctBy { it.displayName }.take(5)
            }

            currentOrderId?.let { id ->
                orderRepository.getOrderStatus(id).onSuccess { response ->
                    orderStatusData = response
                    if (response.status != "DELIVERED" && response.status != "CANCELLED") {
                        currentScreen = CustomerScreen.MainMap
                        startStatusPolling(id)
                    }
                }
            }
        }
    }

    private fun startStatusPolling(orderId: Int) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                delay(5.seconds) // Poll every 5 seconds as fallback
                orderRepository.getOrderStatus(orderId).onSuccess { response ->
                    orderStatusData = response
                    if (response.status == "DELIVERED") {
                        ratingDriverId = response.driverId
                        ratingDriverName = response.driverName ?: "Your Driver"
                        ratingDriverPic = response.driverProfilePic
                        ratingOrderId = orderId
                        finalFare = response.fare ?: 0.0
                        showTripSummary = true
                        clearActiveOrder()
                    } else if (response.status == "CANCELLED") {
                        clearActiveOrder()
                    }
                }
            }
        }
    }

    private fun startWebSocket() {
        val customerId = sessionManager.getDriverId() ?: "1"
        repository.startWebSocket("CUSTOMER_$customerId")
        viewModelScope.launch {
            repository.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: FamekoEvent) {
        when (event) {
            is FamekoEvent.IncomingCall -> incomingCall = event
            is FamekoEvent.CallAccepted -> {
                ongoingCall = incomingCall ?: FamekoEvent.IncomingCall(event.callId, "Driver")
                incomingCall = null
            }
            is FamekoEvent.CallEnded, is FamekoEvent.CallRejected -> {
                incomingCall = null
                ongoingCall = null
            }
            is FamekoEvent.OrderAccepted -> {
                viewModelScope.launch {
                    orderRepository.getOrderStatus(event.orderId).onSuccess { response ->
                        orderStatusData = response
                        currentOrderId = event.orderId
                        sessionManager.setActiveOrderId(event.orderId)
                        startStatusPolling(event.orderId)
                    }
                }
            }
            is FamekoEvent.DeliveryStatusChanged, is FamekoEvent.OrderStatusUpdate -> {
                val id = currentOrderId ?: (event as? FamekoEvent.OrderStatusUpdate)?.orderId
                if (id != null) {
                    viewModelScope.launch {
                        orderRepository.getOrderStatus(id).onSuccess { response ->
                            orderStatusData = response
                            if (response.status == "DELIVERED") {
                                ratingDriverId = response.driverId
                                ratingDriverName = response.driverName ?: "Your Driver"
                                ratingDriverPic = response.driverProfilePic
                                ratingOrderId = id
                                finalFare = response.fare ?: 0.0
                                showTripSummary = true
                                clearActiveOrder()
                            } else if (response.status == "CANCELLED") {
                                if (!isTimedOut) {
                                    clearActiveOrder()
                                } else {
                                    // If timed out, we stop polling but keep isTimedOut true to show the screen
                                    pollingJob?.cancel()
                                    currentOrderId = null
                                    sessionManager.setActiveOrderId(null)
                                }
                            } else {
                                startStatusPolling(id)
                            }
                        }
                    }
                }
            }
            is FamekoEvent.DriverLocationUpdate -> {
                val status = orderStatusData
                if (status != null && status.status != "DELIVERED" && status.status != "CANCELLED") {
                    orderStatusData = status.copy(
                        driverId = event.driverId,
                        driverLat = event.lat,
                        driverLng = event.lng,
                        driverBearing = event.bearing
                    )
                }
            }
            is FamekoEvent.NotificationReceived -> {
                notifications = (listOf(event) + notifications).take(50)
                if (event.type == "ORDER_TIMEOUT") {
                    isTimedOut = true
                    retryAttempt = 0
                }
            }
            is FamekoEvent.NearbyDriversUpdate -> {
                if (currentOrderId == null) {
                    drivers = event.drivers
                }
            }
            // Using a generic handle for extra data from backend
            else -> {
                if (event is FamekoEvent.Unknown) {
                    val type = event.data["type"]?.toString()
                    if (type == "ORDER_RETRY") {
                        retryAttempt = (event.data["attempt"] as? Double)?.toInt() ?: 0
                        maxRetryAttempts = (event.data["maxAttempts"] as? Double)?.toInt() ?: 3
                        searchRadiusKm = (event.data["radius"] as? Double) ?: searchRadiusKm
                        searchMessage = event.data["message"]?.toString() ?: searchMessage
                    }
                }
            }
        }
    }

    fun clearActiveOrder() {
        pollingJob?.cancel()
        currentOrderId = null
        sessionManager.setActiveOrderId(null)
        polylinePoints = emptyList()
        estimatedFare = null
        orderStatusData = null
        scheduledRideTime = null
        isTimedOut = false
    }

    private fun startActiveRentalPolling() {
        rentalPollingJob?.cancel()
        rentalPollingJob = viewModelScope.launch {
            val customerId = sessionManager.getDriverId() ?: "1"
            while (true) {
                rentalRepository.getActiveRental(customerId.toInt()).onSuccess { activeRental = it }
                delay(10.seconds)
            }
        }
    }

    fun navigateTo(screen: CustomerScreen) {
        currentScreen = screen
    }

    fun setServiceMode(mode: ServiceType) {
        activeServiceMode = mode
        isServiceModeSelected = true
        sessionManager.setFirstLogin(false)
        if (mode == ServiceType.PACKAGE_DELIVERY) {
            selectedVehicleType = "Okada" // Valid default for delivery
        } else if (mode == ServiceType.RIDE_HAILING) {
            selectedVehicleType = "Economy"
        } else if (mode == ServiceType.RENTAL) {
            scheduledRideTime = null
        }
    }

    fun updatePickupLocation(query: String) {
        if (isSelectingSuggestion) return
        if (activeServiceMode == ServiceType.RENTAL) {
            if (query == rentalPickupLocation) return
            rentalPickupLocation = query
            rentalPickupLat = null
            rentalPickupLng = null
        } else {
            if (query == pickupLocation) return
            pickupLocation = query
            pickupLat = null
            pickupLng = null
            estimatedFare = null
            polylinePoints = emptyList()
        }
        fetchPickupSuggestions(query)
    }

    fun updateDropOffLocation(query: String) {
        if (isSelectingSuggestion) return
        if (query == dropOffLocation) return
        dropOffLocation = query
        dropOffLat = null
        dropOffLng = null
        estimatedFare = null
        polylinePoints = emptyList()
        fetchDropOffSuggestions(query)
    }

    private fun fetchPickupSuggestions(query: String) {
        pickupSearchJob?.cancel()
        if (query.length > 2) {
            pickupSearchJob = viewModelScope.launch {
                delay(500.milliseconds)
                repository.getGeocodeSuggestions(query).onSuccess { 
                    pickupSuggestions = it 
                }.onFailure {
                    pickupSuggestions = emptyList()
                }
            }
        } else {
            pickupSuggestions = emptyList()
        }
    }

    private fun fetchDropOffSuggestions(query: String) {
        dropOffSearchJob?.cancel()
        if (query.isBlank()) {
            val suggestions = if (selectedTab == 0) {
                recentPlaces
            } else {
                savedPlaces.map { 
                    LocationSuggestion(displayName = it.address, latitude = it.latitude.toString(), longitude = it.longitude.toString(), name = it.label, type = "saved")
                }
            }
            dropOffSuggestions = suggestions
            return
        }
        if (query.length > 2) {
            dropOffSearchJob = viewModelScope.launch {
                delay(500.milliseconds)
                repository.getGeocodeSuggestions(query).onSuccess { 
                    dropOffSuggestions = it 
                }.onFailure {
                    dropOffSuggestions = emptyList()
                }
            }
        } else {
            dropOffSuggestions = emptyList()
        }
    }

    fun applyShortcut(label: String) {
        val place = savedPlaces.find { it.label.equals(label, ignoreCase = true) }
        if (place != null) {
            selectSavedPlace(place)
        } else {
            isSearchMode = true
            // Could set a temporary hint or trigger focus
        }
    }

    fun selectSavedPlace(place: SavedPlace) {
        isSelectingSuggestion = true
        dropOffLocation = place.address
        dropOffLat = place.latitude
        dropOffLng = place.longitude
        isSearchMode = false
        
        viewModelScope.launch {
            delay(500.milliseconds)
            isSelectingSuggestion = false
        }
        
        if (pickupLat != null) {
            calculateRoute()
        }
    }

    fun selectSuggestion(suggestion: LocationSuggestion, isPickup: Boolean) {
        isSelectingSuggestion = true
        if (isPickup) pickupSearchJob?.cancel() else dropOffSearchJob?.cancel()

        val lat = suggestion.latitude.toDoubleOrNull() ?: 0.0
        val lng = suggestion.longitude.toDoubleOrNull() ?: 0.0
        
        // Safety check: Avoid ocean (0,0) or invalid coords
        if (lat == 0.0 && lng == 0.0) {
            isSelectingSuggestion = false
            return
        }

        val selectedText = suggestion.name ?: suggestion.displayName.split(",").firstOrNull() ?: suggestion.displayName

        if (isPickup) {
            currentRegion = RegionUtils.extractRegion(suggestion.displayName)
            if (activeServiceMode == ServiceType.RENTAL) {
                rentalPickupLocation = selectedText
                rentalPickupLat = lat
                rentalPickupLng = lng
            } else {
                pickupLocation = selectedText
                pickupLat = lat
                pickupLng = lng
            }
            pickupSuggestions = emptyList()
        } else {
            dropOffLocation = selectedText
            dropOffLat = lat
            dropOffLng = lng
            dropOffSuggestions = emptyList()
            
            if (activeRental != null) {
                updateRentalDestinationInternal()
            }
        }
        
        // Brief delay to allow keyboard "commit" events to be ignored
        viewModelScope.launch {
            delay(500.milliseconds)
            isSelectingSuggestion = false
        }
        
        if (pickupLat != null && (dropOffLat != null || (activeServiceMode == ServiceType.RENTAL)) && (activeServiceMode == ServiceType.RIDE_HAILING || activeServiceMode == ServiceType.PACKAGE_DELIVERY)) {
            calculateRoute()
        }
    }

    private fun updateRentalDestinationInternal() {
        val rental = activeRental ?: return
        val id = (rental["id"] as? Double)?.toInt() ?: (rental["id"] as? Int) ?: 0
        val dLat = dropOffLat ?: return
        val dLng = dropOffLng ?: return
        val stopsStr = if (stops.isNotEmpty()) stops.filter { it.isNotBlank() }.joinToString("|") else null
        viewModelScope.launch {
            rentalRepository.updateRentalDestination(id, dropOffLocation, dLat, dLng, stopsStr).onSuccess {
                isSearchMode = false
                rentalRepository.getActiveRental(sessionManager.getDriverId()?.toIntOrNull() ?: 1).onSuccess { activeRental = it }
            }
        }
    }

    fun calculateRoute() {
        if (pickupLat == null || dropOffLat == null) {
            // Requirement: Use suggested locations only. 
            // Do not attempt to resolve coordinates from raw text.
            isLoading = false
            return
        }
        performRouteCalculation()
    }

    private fun performRouteCalculation() {
        val pLat = pickupLat ?: return
        val pLng = pickupLng ?: return
        val dLat = dropOffLat ?: return
        val dLng = dropOffLng ?: return

        isLoading = true
        viewModelScope.launch {
            orderRepository.calculateRoute(RouteRequest(RouteLocation(pLat, pLng), RouteLocation(dLat, dLng), "car"))
                .onSuccess { response ->
                    polylinePoints = response.routeCoords.map { GeoPoint(it[1], it[0]) }
                    distanceKm = response.distanceM / 1000.0
                    durationMin = response.etaMin
                    updateEstimatedFare()
                    isSearchMode = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }

    private fun updateEstimatedFare() {
        val pLat = pickupLat ?: return
        val pLng = pickupLng ?: return
        isLoading = true
        viewModelScope.launch {
            // Be more flexible with regions: if extraction failed, try a wider search or just pass null to let backend decide
            val regionToPass = currentRegion ?: "Nationwide" 
            orderRepository.getRideEstimates(pLat, pLng, distanceKm, durationMin, regionToPass)
                .onSuccess { list ->
                    rideEstimates = list
                    if (list.isNotEmpty()) {
                        // Find the selected vehicle in the new list
                        val currentSelected = list.find { it.serviceId == selectedVehicleType }
                        
                        if (currentSelected != null) {
                            estimatedFare = currentSelected.fare
                            pickupEtaMin = currentSelected.pickupEtaMin.toDouble()
                        } else {
                            // If current selection is unavailable, pick the first available one
                            val firstAvailable = list.firstOrNull()
                            if (firstAvailable != null) {
                                selectedVehicleType = firstAvailable.serviceId
                                estimatedFare = firstAvailable.fare
                                pickupEtaMin = firstAvailable.pickupEtaMin.toDouble()
                            }
                        }
                    }
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }

    fun confirmOrder() {
        val pLat = pickupLat
        val pLng = pickupLng
        val eFare = estimatedFare
        
        if (pLat == null || pLng == null || pLat == 0.0 || pLng == 0.0 || eFare == null) {
            // Should show error to user
            return
        }
        isOrderPlacing = true
        viewModelScope.launch {
            val customerId = sessionManager.getDriverId() ?: return@launch
            
            // The estimatedFare already includes vehicle-specific multipliers from the backend.
            // We only need to apply the customer's personal discount rate here.
            val finalFare = (eFare * (100 - discountRate) / 100).toInt().toDouble()
            
            val serviceType = if (activeServiceMode == ServiceType.PACKAGE_DELIVERY) 
                ServiceType.PACKAGE_DELIVERY else ServiceType.RIDE_HAILING
            
            orderRepository.createOrder(
                customerId, pickupLocation, dropOffLocation,
                pLat, pLng, dropOffLat ?: 0.0, dropOffLng ?: 0.0,
                distanceKm, finalFare, durationMin, serviceType,
                selectedVehicleType, scheduledRideTime
            ).onSuccess { orderIdStr ->
                val newId = orderIdStr.toIntOrNull()
                orderStatusData = OrderStatusResponse(success = true, status = "PENDING")
                currentOrderId = newId
                sessionManager.setActiveOrderId(newId)
                scheduledRideTime = null
                isTimedOut = false
            }.onFailure {
                isOrderPlacing = false
            }
        }
    }

    fun cancelOrder() {
        viewModelScope.launch {
            currentOrderId?.let { orderRepository.cancelOrder(it) }
            clearActiveOrder()
        }
    }

    fun shareTrip(context: android.content.Context) {
        val order = orderStatusData ?: return
        
        // Clean up numeric IDs (e.g., 8.0 -> 8) to avoid 404s on the tracking URL
        val driverId = order.driverId?.toDoubleOrNull()?.toInt()?.toString() ?: order.driverId ?: return
        val deliveryId = order.deliveryId?.toDoubleOrNull()?.toInt()?.toString() ?: order.deliveryId ?: driverId
        
        viewModelScope.launch {
            orderRepository.getShareableTripLink(driverId, deliveryId).onSuccess { response ->
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "I'm on a Fameko trip! Track my ride here: ${response.shareUrl}")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }.onFailure {
                // Fallback text if backend fails
                val sendIntent: android.content.Intent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "I'm on a Fameko trip from $pickupLocation to $dropOffLocation. Driver: ${order.driverName}")
                    type = "text/plain"
                }
                val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                context.startActivity(shareIntent)
            }
        }
    }

    fun initiateCall() {
        val data = orderStatusData ?: return
        val orderId = currentOrderId ?: return
        viewModelScope.launch {
            val target = if (data.driverId != null) "DRIVER_${data.driverId}" else "DRIVER_UNKNOWN"
            repository.initiateCall(
                targetId = target,
                callerName = sessionManager.getDriverName() ?: "Customer",
                orderId = orderId
            )
        }
    }

    fun acceptCall(callId: String) {
        viewModelScope.launch {
            repository.acceptCall(callId)
            ongoingCall = incomingCall
            incomingCall = null
        }
    }

    fun rejectCall(callId: String) {
        viewModelScope.launch {
            repository.rejectCall(callId)
            incomingCall = null
        }
    }

    fun endCall(callId: String) {
        viewModelScope.launch {
            repository.endCall(callId)
            ongoingCall = null
        }
    }

    fun updateScheduledRideTime(time: String?) {
        scheduledRideTime = time
    }

    fun submitRating(rating: Float, comment: String) {
        val dId = ratingDriverId ?: return
        val oId = ratingOrderId ?: return
        viewModelScope.launch {
            orderRepository.submitRating(dId, oId, rating, comment).onSuccess {
                showRatingDialog = false
            }.onFailure {
                // Show error?
            }
        }
    }

    fun updateNearbyDrivers(lat: Double, lng: Double) {
        viewModelScope.launch {
            orderRepository.getNearbyDrivers(lat, lng).onSuccess { list ->
                drivers = list
                pickupEtaMin = list.minOfOrNull { it.pickupEtaMin ?: 99.0 }
            }
        }
    }

    fun useCurrentLocation(location: Location, forPickup: Boolean) {
        isLoading = true
        isSelectingSuggestion = true
        viewModelScope.launch {
            repository.reverseGeocode(location.latitude, location.longitude).onSuccess { suggestion ->
                if (forPickup || activeServiceMode == ServiceType.RENTAL) {
                    currentRegion = RegionUtils.extractRegion(suggestion.displayName)
                }
                if (activeServiceMode == ServiceType.RENTAL) {
                    rentalPickupLocation = suggestion.displayName
                    rentalPickupLat = location.latitude
                    rentalPickupLng = location.longitude
                } else {
                    if (forPickup) {
                        pickupLocation = suggestion.displayName
                        pickupLat = location.latitude
                        pickupLng = location.longitude
                    } else {
                        dropOffLocation = suggestion.displayName
                        dropOffLat = location.latitude
                        dropOffLng = location.longitude
                    }
                }
            }.onFailure {
                val name = "My Location"
                if (activeServiceMode == ServiceType.RENTAL) {
                    rentalPickupLocation = name
                    rentalPickupLat = location.latitude
                    rentalPickupLng = location.longitude
                } else {
                    if (forPickup) {
                        pickupLocation = name
                        pickupLat = location.latitude
                        pickupLng = location.longitude
                    } else {
                        dropOffLocation = name
                        dropOffLat = location.latitude
                        dropOffLng = location.longitude
                    }
                }
            }
            isLoading = false
            delay(500.milliseconds)
            isSelectingSuggestion = false
            
            if (pickupLat != null && dropOffLat != null) {
                calculateRoute()
            }
        }
    }
    
    fun addLocalNotification(title: String, message: String, type: String) {
        val newNotif = FamekoEvent.NotificationReceived(
            id = (System.currentTimeMillis() % 1000000).toInt(),
            title = title,
            message = message,
            type = type,
            createdAt = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        )
        notifications = (listOf(newNotif) + notifications).take(50)
    }

    fun deleteNotification(id: Int) {
        notifications = notifications.filter { it.id != id }
    }

    fun sendAudioData(data: ByteArray) {
        repository.sendAudioData(data)
    }

    fun setSelectedVehicleFromFleet(vehicle: Map<String, Any>?) {
        if (vehicle != null) {
            activeServiceMode = ServiceType.RENTAL
            if (rentalPickupLat == null) {
                isSearchMode = true
            }
        }
    }

    fun updateActiveRentalState(rental: Map<String, Any>?) {
        if (rental != null) {
            activeRental = rental
            isSearchMode = false
            activeServiceMode = ServiceType.RENTAL
            rentalPickupLocation = rental["pickup_location"]?.toString() ?: ""
            rentalPickupLat = rental["pickup_lat"] as? Double
            rentalPickupLng = rental["pickup_lng"] as? Double
            
            dropOffLocation = rental["destination_location"]?.toString() ?: ""
            val stopsStr = rental["stops"]?.toString() ?: ""
            stops = if (stopsStr.isNotEmpty()) stopsStr.split("|") else emptyList()
        }
    }

    fun cancelRental(id: Int) {
        viewModelScope.launch {
            rentalRepository.cancelRental(id).onSuccess {
                activeRental = null
            }
        }
    }

    fun savePlace(suggestion: LocationSuggestion) {
        viewModelScope.launch {
            val cId = sessionManager.getDriverId()?.toIntOrNull() ?: 1
            val label = if (savedPlaces.none { it.label == "Home" }) "Home" else if (savedPlaces.none { it.label == "Work" }) "Work" else "Favorite"
            userRepository.savePlace(SavedPlace(customerId = cId, label = label, address = suggestion.displayName, latitude = suggestion.latitude.toDouble(), longitude = suggestion.longitude.toDouble()))
                .onSuccess { 
                    userRepository.getSavedPlaces(cId.toString()).onSuccess { savedPlaces = it }
                }
        }
    }
    
    fun setVehicleType(type: String) {
        selectedVehicleType = type
        rideEstimates.find { it.serviceId == type }?.let {
            estimatedFare = it.fare
            pickupEtaMin = it.pickupEtaMin.toDouble()
        }
    }

    fun handleInitialStates(rental: Map<String, Any>?, vehicle: Map<String, Any>?) {
        if (rental != null) updateActiveRentalState(rental)
        if (vehicle != null) setSelectedVehicleFromFleet(vehicle)
    }
}
