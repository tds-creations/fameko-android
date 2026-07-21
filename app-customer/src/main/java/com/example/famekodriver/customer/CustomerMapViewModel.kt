package com.example.famekodriver.customer

import android.location.Location
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.*
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.LocationUtils
import com.example.famekodriver.core.utils.RegionUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import java.util.UUID
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CustomerMapViewModel(
    private val repository: DriverRepository,
    private val orderRepository: OrderRepository,
    private val rentalRepository: RentalRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val savedPlaceRepository: SavedPlaceRepository,
) : ViewModel() {

    sealed class SavedPlacesUiState {
        object Loading : SavedPlacesUiState()
        data class Success(val places: List<SavedPlace>) : SavedPlacesUiState()
        data class Error(val message: String) : SavedPlacesUiState()
    }

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
    var stopPoints by mutableStateOf<Map<Int, LatLng>>(emptyMap())

    // --- Rental Specific State ---
    var activeRental by mutableStateOf<Map<String, Any>?>(null)
    var rentalRates by mutableStateOf<List<Map<String, Any>>>(emptyList())
    var rentalPickupLocation by mutableStateOf("")
    var rentalPickupLat by mutableStateOf<Double?>(null)
    var rentalPickupLng by mutableStateOf<Double?>(null)

    // --- Ride & Pricing State ---
    var polylinePoints by mutableStateOf<List<LatLng>>(emptyList())
    var instructions by mutableStateOf<List<RouteInstruction>>(emptyList())
    var currentInstruction by mutableStateOf<RouteInstruction?>(null)
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
    var isFullscreenMap by mutableStateOf(false)
    var customerProfile by mutableStateOf<Map<String, Any>?>(null)
    
    val savedPlaces: StateFlow<List<SavedPlace>> = savedPlaceRepository.savedPlaces
    private val _savedPlacesUiState = MutableStateFlow<SavedPlacesUiState>(SavedPlacesUiState.Loading)
    val savedPlacesUiState: StateFlow<SavedPlacesUiState> = _savedPlacesUiState.asStateFlow()

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
    var stopSuggestions by mutableStateOf<List<LocationSuggestion>>(emptyList())
    var managePlacesSuggestions by mutableStateOf<List<LocationSuggestion>>(emptyList())
    
    var focusedStopIndex by mutableIntStateOf(-1)
    
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
    private var managePlacesSearchJob: Job? = null
    private var routeJob: Job? = null
    private var lastRouteCalcLatLng: LatLng? = null

    init {
        loadInitialData()
        loadCustomerProfile()
        startWebSocket()
        startPricingPolling()
        startActiveRentalPolling()
    }

    private fun loadCustomerProfile() {
        viewModelScope.launch {
            val customerId = sessionManager.getCustomerId() ?: return@launch
            userRepository.getCustomerProfile(customerId).onSuccess { profile ->
                if (profile["success"] == true) {
                    customerProfile = profile
                }
            }
        }
    }

    fun updateProfile(name: String, email: String, phone: String, address: String, region: String, profilePic: java.io.File? = null, onResult: (Boolean, String?) -> Unit) {
        val customerId = sessionManager.getCustomerId() ?: return
        viewModelScope.launch {
            userRepository.updateCustomerProfile(customerId, name, email, phone, address, region, profilePic)
                .onSuccess {
                    if (it.success) {
                        loadCustomerProfile()
                        onResult(true, it.message)
                    } else {
                        onResult(false, it.message)
                    }
                }
                .onFailure {
                    onResult(false, it.message ?: "Network error")
                }
        }
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

    fun fetchSavedPlaces() {
        viewModelScope.launch {
            val customerId = sessionManager.getCustomerId() ?: run {
                _savedPlacesUiState.value = SavedPlacesUiState.Success(savedPlaces.value)
                return@launch
            }
            _savedPlacesUiState.value = SavedPlacesUiState.Loading
            savedPlaceRepository.fetchSavedPlaces(customerId)
                .onSuccess {
                    _savedPlacesUiState.value = SavedPlacesUiState.Success(savedPlaces.value)
                }
                .onFailure { _savedPlacesUiState.value = SavedPlacesUiState.Error(it.message ?: "Unknown error") }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            val customerId = sessionManager.getCustomerId() ?: "1"
            
            // Listen to local changes
            viewModelScope.launch {
                savedPlaceRepository.savedPlaces.collect { places ->
                    _savedPlacesUiState.value = SavedPlacesUiState.Success(places)
                }
            }

            fetchSavedPlaces()
            orderRepository.getPricingConfig().onSuccess { pricingConfig = it }
            orderRepository.getDiscountRate(customerId).onSuccess { discountRate = it }
            rentalRepository.getRentalRates().onSuccess { rentalRates = it }

            // Restore active rental from backend
            rentalRepository.getActiveRental(customerId.toIntOrNull() ?: 1).onSuccess { 
                updateActiveRentalState(it)
                if (it != null) {
                    currentScreen = CustomerScreen.MainMap
                }
            }
            
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

            // Restore active session from backend (Redis-backed on server)
            orderRepository.getActiveOrder(customerId).onSuccess { response ->
                if (response?.success == true && response.status != "DELIVERED" && response.status != "CANCELLED") {
                    val orderId = response.orderId
                    if (orderId != null) {
                        currentOrderId = orderId
                        orderStatusData = response
                        sessionManager.setActiveOrderId(orderId)
                        
                        // Restore locations for map reconstruction
                        response.pickupLocation?.let { pickupLocation = it }
                        response.pickupLat?.let { pickupLat = it }
                        response.pickupLng?.let { pickupLng = it }
                        response.dropOffLocation?.let { dropOffLocation = it }
                        response.dropOffLat?.let { dropOffLat = it }
                        response.dropOffLng?.let { dropOffLng = it }
                        
                        if (pickupLat != null && dropOffLat != null) {
                            calculateRoute()
                        }

                        currentScreen = CustomerScreen.MainMap
                        startStatusPolling(orderId)
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
        val customerId = sessionManager.getCustomerId() ?: "1"
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
                val eventFare = when (event) {
                    is FamekoEvent.DeliveryStatusChanged -> event.fare
                    is FamekoEvent.OrderStatusUpdate -> event.fare
                    else -> null
                }
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
                                finalFare = eventFare ?: response.fare ?: 0.0
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
                    calculateRouteForActiveTrip(event.lat, event.lng, status.status)
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
                if (currentOrderId == null || orderStatusData?.status == "PENDING") {
                    drivers = event.drivers
                }
            }
            // Using a generic handle for extra data from backend
            else -> {
                if (event is FamekoEvent.Unknown) {
                    val type = event.type
                    if (type == "ORDER_RETRY") {
                        retryAttempt = (event.data["attempt"] as? Double)?.toInt() ?: 0
                        maxRetryAttempts = (event.data["maxAttempts"] as? Double)?.toInt() ?: 3
                        searchRadiusKm = (event.data["radius"] as? Double) ?: searchRadiusKm
                        searchMessage = event.data["message"]?.toString() ?: searchMessage
                    } else if (type == "RENTAL_STATUS_UPDATED") {
                        refreshActiveRental()
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
        isOrderPlacing = false
    }

    fun resetSearch() {
        pickupLocation = ""
        pickupLat = null
        pickupLng = null
        dropOffLocation = ""
        dropOffLat = null
        dropOffLng = null
        polylinePoints = emptyList()
        estimatedFare = null
        rideEstimates = emptyList()
        isSearchMode = false
        rentalPickupLocation = ""
        rentalPickupLat = null
        rentalPickupLng = null
        isOrderPlacing = false
    }

    fun clearDestination() {
        dropOffLocation = ""
        dropOffLat = null
        dropOffLng = null
        stops = emptyList()
        stopPoints = emptyMap()
        polylinePoints = emptyList()
        estimatedFare = null
        isFullscreenMap = false
        
        val rental = activeRental
        if (rental != null) {
            val id = (rental["id"] as? Number)?.toInt() ?: 0
            viewModelScope.launch {
                rentalRepository.updateRentalDestination(id, "", 0.0, 0.0, null).onSuccess {
                    refreshActiveRental()
                }
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
        }
    }

    fun updatePickupLocation(query: String) {
        if (activeServiceMode == ServiceType.RENTAL) {
            if (query == rentalPickupLocation) return
            rentalPickupLocation = query
            pickupLocation = query
            rentalPickupLat = null
            rentalPickupLng = null
            pickupLat = null
            pickupLng = null
        } else {
            if (query == pickupLocation) return
            pickupLocation = query
            pickupLat = null
            pickupLng = null
        }
        estimatedFare = null
        polylinePoints = emptyList()
        fetchPickupSuggestions(query)
    }

    fun updateDropOffLocation(query: String) {
        if (query == dropOffLocation) return
        dropOffLocation = query
        dropOffLat = null
        dropOffLng = null
        estimatedFare = null
        polylinePoints = emptyList()
        fetchDropOffSuggestions(query)
    }

    fun updateStopLocation(index: Int, query: String) {
        val newStops = stops.toMutableList()
        if (newStops[index] == query) return
        newStops[index] = query
        stops = newStops
        
        // Clear point if text is edited manually
        if (stopPoints.containsKey(index)) {
            stopPoints = stopPoints.toMutableMap().apply { remove(index) }
        }
        
        focusedStopIndex = index
        fetchStopSuggestions(query)
    }

    fun addStop() {
        if (stops.size < 5) {
            stops = stops + ""
        }
    }

    fun removeStop(index: Int) {
        val newStops = stops.toMutableList()
        newStops.removeAt(index)
        stops = newStops
        
        // Shift stop points
        val newPoints = mutableMapOf<Int, LatLng>()
        stopPoints.forEach { (idx, latLng) ->
            when {
                idx < index -> newPoints[idx] = latLng
                idx > index -> newPoints[idx - 1] = latLng
            }
        }
        stopPoints = newPoints
        
        if (pickupLat != null && dropOffLat != null) {
            if (pickupLat != null && dropOffLat != null) {
                isSearchMode = false
                currentScreen = CustomerScreen.MainMap
                calculateRoute()
            }
        }
    }

    private fun fetchStopSuggestions(query: String) {
        if (query.length > 2) {
            viewModelScope.launch {
                delay(500.milliseconds)
                repository.getGeocodeSuggestions(query).onSuccess { 
                    stopSuggestions = it 
                }.onFailure {
                    stopSuggestions = emptyList()
                }
            }
        } else {
            stopSuggestions = emptyList()
        }
    }

    private fun fetchPickupSuggestions(query: String) {
        pickupSearchJob?.cancel()
        if (query.isBlank()) {
            val suggestions = if (selectedTab == 0) {
                recentPlaces
            } else {
                savedPlaces.value.map { 
                    LocationSuggestion(displayName = it.address, latitude = it.latitude.toString(), longitude = it.longitude.toString(), name = it.label, type = "saved")
                }
            }
            pickupSuggestions = suggestions
            return
        }
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

    fun updateSaveSearchQuery(query: String) {
        managePlacesSearchJob?.cancel()
        if (query.isBlank()) {
            managePlacesSuggestions = recentPlaces
            return
        }
        if (query.length > 2) {
            managePlacesSearchJob = viewModelScope.launch {
                delay(500.milliseconds)
                repository.getGeocodeSuggestions(query).onSuccess { 
                    managePlacesSuggestions = it 
                }.onFailure {
                    managePlacesSuggestions = emptyList()
                }
            }
        } else {
            managePlacesSuggestions = emptyList()
        }
    }

    private fun fetchDropOffSuggestions(query: String) {
        dropOffSearchJob?.cancel()
        if (query.isBlank()) {
            val suggestions = if (selectedTab == 0) {
                recentPlaces
            } else {
                savedPlaces.value.map { 
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

    fun applyShortcut(label: String, onGetCurrentLocation: (() -> Unit)? = null) {
        val place = savedPlaces.value.find { it.label.equals(label, ignoreCase = true) }
        if (place != null) {
            selectSavedPlace(place)
            onGetCurrentLocation?.invoke()
        } else {
            isSearchMode = true
        }
    }

    fun selectSavedPlace(place: SavedPlace) {
        if (place.latitude == 0.0 || place.longitude == 0.0) return
        
        dropOffLocation = place.address
        dropOffLat = place.latitude
        dropOffLng = place.longitude
        isSearchMode = false
        
        if (pickupLat != null) {
            if (pickupLat != null && dropOffLat != null) {
                isSearchMode = false
                currentScreen = CustomerScreen.MainMap
                calculateRoute()
            }
        }
    }

    fun selectSuggestion(suggestion: LocationSuggestion, isPickup: Boolean) {
        if (isPickup) pickupSearchJob?.cancel() else dropOffSearchJob?.cancel()

        val lat = suggestion.latitude.toDoubleOrNull() ?: 99.0
        val lng = suggestion.longitude.toDoubleOrNull() ?: 99.0
        
        android.util.Log.d("GeocodeDiag", "Selected suggestion: ${suggestion.displayName}, Lat: ${suggestion.latitude} ($lat), Lng: ${suggestion.longitude} ($lng)")

        if (lat == 0.0 || lng == 0.0 || lat == 99.0 || lng == 99.0) {
            addLocalNotification("Location Error", "Selected location has invalid coordinates.", "error")
            return
        }

        val selectedText = suggestion.name ?: suggestion.displayName.split(",").firstOrNull() ?: suggestion.displayName

        if (isPickup) {
            currentRegion = RegionUtils.extractRegion(suggestion.displayName)
            if (activeServiceMode == ServiceType.RENTAL) {
                rentalPickupLocation = selectedText
                rentalPickupLat = lat
                rentalPickupLng = lng
            }
            pickupLocation = selectedText
            pickupLat = lat
            pickupLng = lng
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
        
        if (pickupLat != null && dropOffLat != null) {
            isSearchMode = false
            currentScreen = CustomerScreen.MainMap
            calculateRoute()
        } else {
            isSearchMode = true
        }
    }

    fun selectStopSuggestion(index: Int, suggestion: LocationSuggestion) {
        val lat = suggestion.latitude.toDoubleOrNull() ?: 0.0
        val lng = suggestion.longitude.toDoubleOrNull() ?: 0.0
        
        if (lat == 0.0 || lng == 0.0) return

        stopPoints = stopPoints.toMutableMap().apply { put(index, LatLng(lat, lng)) }

        val selectedText = suggestion.name ?: suggestion.displayName.split(",").firstOrNull() ?: suggestion.displayName
        val newStops = stops.toMutableList()
        newStops[index] = selectedText
        stops = newStops
        stopSuggestions = emptyList()
        focusedStopIndex = -1
        
        calculateRoute()
    }

    fun calculateRoute() {
        val isUnlocked = activeRental?.let { it["is_unlocked"] == true || it["is_unlocked"] == "true" } ?: false
        
        val pLat = if (activeServiceMode == ServiceType.RENTAL) {
            if (isUnlocked) pickupLat ?: rentalPickupLat else rentalPickupLat ?: pickupLat
        } else {
            pickupLat
        }
        
        val pLng = if (activeServiceMode == ServiceType.RENTAL) {
            if (isUnlocked) pickupLng ?: rentalPickupLng else rentalPickupLng ?: rentalPickupLng
        } else {
            pickupLng
        }

        val dLat = dropOffLat
        val dLng = dropOffLng

        if (pLat == null || pLng == null || dLat == null || dLng == null || pLat == 0.0 || dLat == 0.0) {
            isLoading = false
            return
        }

        android.util.Log.d("RouteDiag", "Calculating route from ($pLat, $pLng) to ($dLat, $dLng)")
        performRouteCalculation(pLat, pLng, dLat, dLng)
    }

    private fun calculateRouteForActiveTrip(driverLat: Double, driverLng: Double, status: String) {
        val destLat = if (status == "ASSIGNED" || status == "ARRIVED") pickupLat else dropOffLat
        val destLng = if (status == "ASSIGNED" || status == "ARRIVED") pickupLng else dropOffLng

        if (destLat == null || destLng == null) return

        // Threshold to avoid spamming
        lastRouteCalcLatLng?.let { last ->
            val dist = LocationUtils.calculateDistance(driverLat, driverLng, last.latitude, last.longitude)
            if (dist < 50.0 && polylinePoints.isNotEmpty()) return
        }

        performRouteCalculation(driverLat, driverLng, destLat, destLng, isUpdate = true)
    }

    private fun performRouteCalculation(pLat: Double, pLng: Double, dLat: Double, dLng: Double, isUpdate: Boolean = false) {
        if (!isUpdate) {
            isLoading = true
            rideEstimates = emptyList()
        }
        
        val waypoints = stops.indices.mapNotNull { idx -> 
            stopPoints[idx]?.let { RouteLocation(it.latitude, it.longitude) }
        }

        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            val request = RouteRequest(
                start = RouteLocation(pLat, pLng), 
                end = RouteLocation(dLat, dLng), 
                stops = waypoints,
                vehicleType = "car"
            )
            android.util.Log.d("RouteDiag", "Sending RouteRequest: Start=(${request.start.lat}, ${request.start.lng}), End=(${request.end.lat}, ${request.end.lng})")
            
            orderRepository.calculateRoute(request)
                .onSuccess { response ->
                    android.util.Log.d("RouteDiag", "Route calculation success: ${response.routeCoords.size} points received.")
                    
                    val coords = if (response.routeCoords.isNotEmpty()) {
                        response.routeCoords
                    } else {
                        // Fallback to straight line if no route coords but we have start/end
                        Log.w("RouteDiag", "No route coords received, using straight line fallback")
                        listOf(listOf(pLng, pLat), listOf(dLng, dLat))
                    }

                    polylinePoints = coords.filter { it.size >= 2 }.map { LatLng(it[1], it[0]) }
                    
                    if (polylinePoints.isNotEmpty()) {
                        val first = polylinePoints.first()
                        android.util.Log.d("RouteDiag", "Polyline points set: ${polylinePoints.size}, Start: ${first.latitude}, ${first.longitude}")
                    }
                    
                    instructions = response.instructions
                    currentInstruction = instructions.firstOrNull()

                    if (!isUpdate) {
                        distanceKm = if (response.distanceM > 0) response.distanceM / 1000.0 
                                     else com.example.famekodriver.core.utils.LocationUtils.calculateDistance(pLat, pLng, dLat, dLng) / 1000.0
                        durationMin = if (response.etaMin > 0) response.etaMin 
                                      else (distanceKm * 2.0) // rough estimate: 2 mins per km
                        
                        if (activeServiceMode != ServiceType.RENTAL) {
                            updateEstimatedFare()
                        }
                    }
                    lastRouteCalcLatLng = LatLng(pLat, pLng)
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                    Log.e("RouteDiag", "Route calculation failed completely", it)
                    
                    // Even on complete failure, try to show a straight line if we have coordinates
                    // so the user isn't stuck without a pricing card
                    if (!isUpdate && pLat != 0.0 && dLat != 0.0) {
                        polylinePoints = listOf(LatLng(pLat, pLng), LatLng(dLat, dLng))
                        distanceKm = com.example.famekodriver.core.utils.LocationUtils.calculateDistance(pLat, pLng, dLat, dLng) / 1000.0
                        durationMin = distanceKm * 2.0
                        updateEstimatedFare()
                    }
                }
        }
    }

    private fun updateEstimatedFare() {
        val pLat = pickupLat ?: return
        val pLng = pickupLng ?: return
        Log.d("PricingDiag", "Updating estimated fare for ($pLat, $pLng), Dist: $distanceKm, Dur: $durationMin, Region: $currentRegion")
        isLoading = true
        viewModelScope.launch {
            // Be more flexible with regions: if extraction failed, the backend should handle null/empty by returning default rates
            val regionToPass = currentRegion?.ifEmpty { null } ?: "Greater Accra" // Default to a known active region if unknown
            orderRepository.getRideEstimates(pLat, pLng, distanceKm, durationMin, regionToPass)
                .onSuccess { list ->
                    Log.d("PricingDiag", "Ride estimates received: ${list.size} options found.")
                    rideEstimates = list
                    if (list.isNotEmpty()) {
                        // Find the selected vehicle in the new list
                        val currentSelected = list.find { it.serviceId == selectedVehicleType }
                        
                        if (currentSelected != null) {
                            estimatedFare = currentSelected.fare
                            pickupEtaMin = currentSelected.pickupEtaMin.toDouble()
                            Log.d("PricingDiag", "Selected vehicle $selectedVehicleType found: fare=${currentSelected.fare}")
                        } else {
                            // If current selection is unavailable, pick the first available one
                            val firstAvailable = list.firstOrNull()
                            if (firstAvailable != null) {
                                selectedVehicleType = firstAvailable.serviceId
                                estimatedFare = firstAvailable.fare
                                pickupEtaMin = firstAvailable.pickupEtaMin.toDouble()
                                Log.d("PricingDiag", "Current selection unavailable, switched to ${firstAvailable.serviceId}: fare=${firstAvailable.fare}")
                            }
                        }
                    } else {
                        Log.w("PricingDiag", "Ride estimates list is empty from backend!")
                    }
                    isLoading = false
                }
                .onFailure {
                    Log.e("PricingDiag", "Failed to fetch ride estimates", it)
                    isLoading = false
                }
        }
    }

    fun confirmOrder() {
        val pLat = pickupLat
        val pLng = pickupLng
        val eFare = estimatedFare
        
        if (pLat == null || pLng == null || eFare == null) {
            // Should show error to user
            return
        }
        isOrderPlacing = true
        viewModelScope.launch {
            val customerId = sessionManager.getCustomerId() ?: run {
                isOrderPlacing = false
                return@launch
            }
            
            // The estimatedFare already includes vehicle-specific multipliers from the backend.
            // We only need to apply the customer's personal discount rate here.
            val finalFare = (eFare * (100 - discountRate) / 100).toInt().toDouble()
            
            val serviceType = if (activeServiceMode == ServiceType.PACKAGE_DELIVERY) 
                ServiceType.PACKAGE_DELIVERY else ServiceType.RIDE_HAILING
            
            orderRepository.createOrder(
                OrderCreateRequest(
                    customerId = customerId,
                    pickupLocation = pickupLocation,
                    dropOffLocation = dropOffLocation,
                    pickupLat = pLat,
                    pickupLng = pLng,
                    dropOffLat = dropOffLat ?: 0.0,
                    dropOffLng = dropOffLng ?: 0.0,
                    distanceKm = distanceKm,
                    estimatedFare = finalFare,
                    durationMin = durationMin,
                    serviceType = serviceType,
                    requestedVehicleType = selectedVehicleType,
                    scheduledTime = scheduledRideTime
                )
            ).onSuccess { newId ->
                orderStatusData = OrderStatusResponse(success = true, status = "PENDING")
                currentOrderId = newId
                sessionManager.setActiveOrderId(newId)
                scheduledRideTime = null
                isTimedOut = false
                isOrderPlacing = false
            }.onFailure {
                isOrderPlacing = false
            }
        }
    }

    fun cancelOrder(reason: String? = null) {
        viewModelScope.launch {
            currentOrderId?.let { orderRepository.cancelOrder(it, reason) }
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
        if (lat == 0.0 && lng == 0.0) return

        viewModelScope.launch {
            orderRepository.getNearbyDrivers(lat, lng).onSuccess { list ->
                drivers = list
                pickupEtaMin = list.minOfOrNull { it.pickupEtaMin ?: 99.0 }
            }
        }
    }

    fun useCurrentLocation(location: android.location.Location, forPickup: Boolean, stopIndex: Int = -1) {
        if (location.latitude == 0.0 || location.longitude == 0.0) return

        isLoading = true
        viewModelScope.launch {
            repository.reverseGeocode(location.latitude, location.longitude).onSuccess { suggestion ->
                val displayName = suggestion.displayName
                if (forPickup || activeServiceMode == ServiceType.RENTAL) {
                    currentRegion = RegionUtils.extractRegion(displayName)
                }
                
                if (stopIndex != -1) {
                    val newStops = stops.toMutableList()
                    if (stopIndex < newStops.size) {
                        newStops[stopIndex] = suggestion.name ?: displayName
                        stops = newStops
                        stopPoints = stopPoints.toMutableMap().apply { put(stopIndex, LatLng(location.latitude, location.longitude)) }
                    }
                } else {
                    if (activeServiceMode == ServiceType.RENTAL && forPickup) {
                        rentalPickupLocation = displayName
                        rentalPickupLat = location.latitude
                        rentalPickupLng = location.longitude
                    }
                    
                    if (forPickup) {
                        pickupLocation = displayName
                        pickupLat = location.latitude
                        pickupLng = location.longitude
                    } else {
                        dropOffLocation = displayName
                        dropOffLat = location.latitude
                        dropOffLng = location.longitude
                    }
                }
                
                if (pickupLat != null && dropOffLat != null) {
                    isSearchMode = false
                    currentScreen = CustomerScreen.MainMap
                    calculateRoute()
                }
                isLoading = false
            }.onFailure {
                val name = "My Location"
                if (stopIndex != -1) {
                    val newStops = stops.toMutableList()
                    if (stopIndex < newStops.size) {
                        newStops[stopIndex] = name
                        stops = newStops
                        stopPoints = stopPoints.toMutableMap().apply { put(stopIndex, LatLng(location.latitude, location.longitude)) }
                    }
                } else {
                    if (activeServiceMode == ServiceType.RENTAL && forPickup) {
                        rentalPickupLocation = name
                        rentalPickupLat = location.latitude
                        rentalPickupLng = location.longitude
                    }

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
                
                if (pickupLat != null && dropOffLat != null) {
                    isSearchMode = false
                    currentScreen = CustomerScreen.MainMap
                    calculateRoute()
                }
                isLoading = false
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

    fun updateSavedPlace(id: String, label: String, suggestion: LocationSuggestion) {
        viewModelScope.launch {
            val cId = sessionManager.getCustomerId() ?: return@launch
            val lat = suggestion.latitude.toDoubleOrNull() ?: 0.0
            val lng = suggestion.longitude.toDoubleOrNull() ?: 0.0
            savedPlaceRepository.updateSavedPlace(SavedPlace(id = id, customerId = cId, label = label, address = suggestion.displayName, latitude = lat, longitude = lng))
        }
    }

    fun deleteSavedPlace(id: String) {
        viewModelScope.launch {
            savedPlaceRepository.deleteSavedPlace(id)
        }
    }

    fun savePlace(suggestion: LocationSuggestion, customLabel: String? = null) {
        viewModelScope.launch {
            val cId = sessionManager.getCustomerId() ?: return@launch
            val currentList = savedPlaces.value
            val label = customLabel ?: if (currentList.none { it.label == "Home" }) "Home" else if (currentList.none { it.label == "Work" }) "Work" else "Favorite"
            
            savedPlaceRepository.savePlace(SavedPlace(
                id = UUID.randomUUID().toString(),
                customerId = cId, 
                label = label, 
                address = suggestion.displayName, 
                latitude = suggestion.latitude.toDoubleOrNull() ?: 0.0, 
                longitude = suggestion.longitude.toDoubleOrNull() ?: 0.0
            ))
            dropOffSuggestions = emptyList()
        }
    }

    fun setVehicleType(type: String) {
        selectedVehicleType = type
        rideEstimates.find { it.serviceId == type }?.let {
            estimatedFare = it.fare
            pickupEtaMin = it.pickupEtaMin.toDouble()
        }
    }

    // --- New Rental Scratch Logic ---

    fun refreshActiveRental() {
        viewModelScope.launch {
            val customerId = sessionManager.getCustomerId() ?: return@launch
            rentalRepository.getActiveRental(customerId.toInt()).onSuccess { 
                updateActiveRentalState(it)
            }
        }
    }

    fun updateActiveRentalState(rental: Map<String, Any>?) {
        if (rental != null) {
            activeRental = rental
            activeServiceMode = ServiceType.RENTAL
            
            if (!isSearchMode) {
                rentalPickupLocation = rental["pickup_location"]?.toString() ?: ""
                rentalPickupLat = rental["pickup_lat"]?.toString()?.toDoubleOrNull()
                rentalPickupLng = rental["pickup_lng"]?.toString()?.toDoubleOrNull()
                
                dropOffLocation = rental["destination_location"]?.toString() ?: ""
                dropOffLat = rental["destination_lat"]?.toString()?.toDoubleOrNull()
                dropOffLng = rental["destination_lng"]?.toString()?.toDoubleOrNull()
                
                val stopsStr = rental["stops"]?.toString() ?: ""
                stops = if (stopsStr.isNotEmpty()) stopsStr.split("|") else emptyList()

                if (dropOffLat != null && dropOffLat != 0.0) {
                    calculateRoute()
                }
            }
        } else {
            activeRental = null
        }
    }

    private fun startActiveRentalPolling() {
        rentalPollingJob?.cancel()
        rentalPollingJob = viewModelScope.launch {
            val customerId = sessionManager.getCustomerId() ?: return@launch
            while (true) {
                rentalRepository.getActiveRental(customerId.toInt()).onSuccess { 
                    updateActiveRentalState(it)
                }
                delay(15.seconds)
            }
        }
    }

    fun startNavigationForRental(rental: Map<String, Any>) {
        val isUnlocked = rental["is_unlocked"] == true || rental["is_unlocked"] == "true"
        val dLat = rental["destination_lat"]?.toString()?.toDoubleOrNull()
        val dLng = rental["destination_lng"]?.toString()?.toDoubleOrNull()
        val pLat = rental["pickup_lat"]?.toString()?.toDoubleOrNull()
        val pLng = rental["pickup_lng"]?.toString()?.toDoubleOrNull()

        val targetLat = if (isUnlocked) dLat else pLat
        val targetLng = if (isUnlocked) dLng else pLng
        val targetLabel = if (isUnlocked) (rental["destination_location"]?.toString() ?: "Destination") else (rental["pickup_location"]?.toString() ?: "Pickup")

        if (targetLat != null && targetLng != null) {
            dropOffLat = targetLat
            dropOffLng = targetLng
            dropOffLocation = targetLabel
            
            pickupLat = null // Force current location fetch
            pickupLng = null
            
            isFullscreenMap = true
            currentScreen = CustomerScreen.MainMap
            isLoading = true
        } else {
            currentScreen = CustomerScreen.MainMap
            isSearchMode = true
        }
    }

    private fun updateRentalDestinationInternal() {
        val rental = activeRental ?: return
        val id = (rental["id"] as? Number)?.toInt() ?: 0
        val dLat = dropOffLat ?: return
        val dLng = dropOffLng ?: return
        val stopsStr = if (stops.isNotEmpty()) stops.filter { it.isNotBlank() }.joinToString("|") else null
        viewModelScope.launch {
            rentalRepository.updateRentalDestination(id, dropOffLocation, dLat, dLng, stopsStr).onSuccess {
                isSearchMode = false
                refreshActiveRental()
            }
        }
    }

    fun cancelRental(id: Int) {
        viewModelScope.launch {
            rentalRepository.cancelRental(id).onSuccess {
                activeRental = null
            }
        }
    }

    fun endRental(id: Int) {
        viewModelScope.launch {
            rentalRepository.endRental(id).onSuccess {
                activeRental = null
            }
        }
    }

    /**
     * Checks if the user is off-route and triggers recalculation if needed.
     */
    fun checkOffRoute(currentLat: Double, currentLng: Double) {
        if (polylinePoints.size < 2 || isLoading) return
        
        var minDistance = Double.MAX_VALUE
        for (i in 0 until polylinePoints.size - 1) {
            val p1 = polylinePoints[i]
            val p2 = polylinePoints[i + 1]
            val dist = LocationUtils.distanceToSegment(
                currentLat, currentLng,
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude
            )
            if (dist < minDistance) minDistance = dist
        }
        
        // If deviating more than 50 meters, recalculate
        if (minDistance > 50.0) {
            Log.w("NavDiag", "Off-route detected: ${minDistance.toInt()}m from path. Recalculating...")
            calculateRoute()
        }
    }

    /**
     * Checks if the user has arrived at the destination.
     * Returns true if arrived.
     */
    fun checkArrival(currentLat: Double, currentLng: Double): Boolean {
        val dLat = dropOffLat ?: return false
        val dLng = dropOffLng ?: return false
        
        val distToDest = LocationUtils.calculateDistance(currentLat, currentLng, dLat, dLng)
        
        if (distToDest < 30.0) { // Within 30 meters
            Log.d("NavDiag", "Arrival detected! Distance: ${distToDest.toInt()}m")
            isFullscreenMap = false
            // Reset routing state
            polylinePoints = emptyList()
            instructions = emptyList()
            currentInstruction = null
            
            if (currentOrderId != null) {
                // If it was a real order, final fare should be set
                showTripSummary = true
            }
            return true
        }
        return false
    }
}
