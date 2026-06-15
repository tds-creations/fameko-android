package com.example.famekodriver.customer

import android.location.Location
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CustomerMapViewModel(
    private val repository: DriverRepository,
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

    private var pollingJob: Job? = null
    private var rentalPollingJob: Job? = null
    private var pickupSearchJob: Job? = null
    private var dropOffSearchJob: Job? = null

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
                    repository.getPricingConfig().onSuccess { newConfig ->
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
            repository.getSavedPlaces(customerId).onSuccess { savedPlaces = it }
            repository.getPricingConfig().onSuccess { pricingConfig = it }
            repository.getRentalRates().onSuccess { rentalRates = it }
            repository.getDiscountRate(customerId).onSuccess { discountRate = it }
            
            repository.getCustomerTrips(customerId).onSuccess { trips ->
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
                repository.getOrderStatus(id).onSuccess { response ->
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
                repository.getOrderStatus(orderId).onSuccess { response ->
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
                    repository.getOrderStatus(event.orderId).onSuccess { response ->
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
                        repository.getOrderStatus(id).onSuccess { response ->
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
                                clearActiveOrder()
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
            is FamekoEvent.NearbyDriversUpdate -> {
                if (currentOrderId == null) {
                    drivers = event.drivers
                }
            }
            else -> {}
        }
    }

    private fun clearActiveOrder() {
        pollingJob?.cancel()
        currentOrderId = null
        sessionManager.setActiveOrderId(null)
        polylinePoints = emptyList()
        estimatedFare = null
        orderStatusData = null
    }

    private fun startActiveRentalPolling() {
        rentalPollingJob?.cancel()
        rentalPollingJob = viewModelScope.launch {
            val customerId = sessionManager.getDriverId() ?: "1"
            while (true) {
                repository.getActiveRental(customerId.toInt()).onSuccess { activeRental = it }
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
        }
    }

    fun updatePickupLocation(query: String) {
        if (activeServiceMode == ServiceType.RENTAL) {
            rentalPickupLocation = query
            rentalPickupLat = null
            rentalPickupLng = null
        } else {
            pickupLocation = query
            pickupLat = null
            pickupLng = null
            estimatedFare = null
            polylinePoints = emptyList()
        }
        fetchPickupSuggestions(query)
    }

    fun updateDropOffLocation(query: String) {
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
        dropOffLocation = place.address
        dropOffLat = place.latitude
        dropOffLng = place.longitude
        isSearchMode = false
        
        if (pickupLat != null) {
            calculateRoute()
        } else {
            // If pickup is not set, maybe use current location automatically?
            // For now, let the user set pickup if it's missing
        }
    }

    fun selectSuggestion(suggestion: LocationSuggestion, isPickup: Boolean) {
        if (isPickup) {
            if (activeServiceMode == ServiceType.RENTAL) {
                rentalPickupLocation = suggestion.displayName
                rentalPickupLat = suggestion.latitude.toDoubleOrNull()
                rentalPickupLng = suggestion.longitude.toDoubleOrNull()
            } else {
                pickupLocation = suggestion.displayName
                pickupLat = suggestion.latitude.toDoubleOrNull()
                pickupLng = suggestion.longitude.toDoubleOrNull()
            }
            pickupSuggestions = emptyList()
        } else {
            dropOffLocation = suggestion.displayName
            dropOffLat = suggestion.latitude.toDoubleOrNull()
            dropOffLng = suggestion.longitude.toDoubleOrNull()
            dropOffSuggestions = emptyList()
            
            if (activeRental != null) {
                updateRentalDestinationInternal()
            }
        }
        
        if (pickupLat != null && dropOffLat != null && (activeServiceMode == ServiceType.RIDE_HAILING || activeServiceMode == ServiceType.PACKAGE_DELIVERY)) {
            calculateRoute()
        }
    }

    private fun updateRentalDestinationInternal() {
        val rental = activeRental ?: return
        val id = (rental["id"] as? Double)?.toInt() ?: (rental["id"] as? Int) ?: 0
        val stopsStr = if (stops.isNotEmpty()) stops.filter { it.isNotBlank() }.joinToString("|") else null
        viewModelScope.launch {
            repository.updateRentalDestination(id, dropOffLocation, dropOffLat!!, dropOffLng!!, stopsStr).onSuccess {
                isSearchMode = false
                repository.getActiveRental(sessionManager.getDriverId()?.toIntOrNull() ?: 1).onSuccess { activeRental = it }
            }
        }
    }

    fun calculateRoute() {
        if (pickupLat == null || dropOffLat == null) {
            // Attempt to resolve coordinates if missing
            viewModelScope.launch {
                isLoading = true
                if (pickupLat == null && pickupLocation.isNotBlank()) {
                    repository.getGeocodeSuggestions(pickupLocation).onSuccess { suggestions ->
                        suggestions.firstOrNull()?.let {
                            pickupLat = it.latitude.toDoubleOrNull()
                            pickupLng = it.longitude.toDoubleOrNull()
                        }
                    }
                }
                if (dropOffLat == null && dropOffLocation.isNotBlank()) {
                    repository.getGeocodeSuggestions(dropOffLocation).onSuccess { suggestions ->
                        suggestions.firstOrNull()?.let {
                            dropOffLat = it.latitude.toDoubleOrNull()
                            dropOffLng = it.longitude.toDoubleOrNull()
                        }
                    }
                }
                
                // Retry calculation if coordinates were found
                if (pickupLat != null && dropOffLat != null) {
                    performRouteCalculation()
                } else {
                    isLoading = false
                    // Optionally show error: "Please select valid locations from the list"
                }
            }
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
            repository.calculateRoute(RouteRequest(RouteLocation(pLat, pLng), RouteLocation(dLat, dLng), "car"))
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
        isLoading = true
        viewModelScope.launch {
            repository.getRideEstimates(pickupLat!!, pickupLng!!, distanceKm, durationMin)
                .onSuccess { list ->
                    rideEstimates = list
                    if (list.isNotEmpty()) {
                        estimatedFare = list.find { it.serviceId == selectedVehicleType }?.fare ?: list[0].fare
                        pickupEtaMin = list.find { it.serviceId == selectedVehicleType }?.pickupEtaMin?.toDouble() ?: list[0].pickupEtaMin.toDouble()
                    }
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }

    fun confirmOrder() {
        if (pickupLat == null || pickupLng == null || pickupLat == 0.0 || pickupLng == 0.0) {
            // Should show error to user
            return
        }
        isOrderPlacing = true
        viewModelScope.launch {
            val customerId = sessionManager.getDriverId() ?: return@launch
            
            // The estimatedFare already includes vehicle-specific multipliers from the backend.
            // We only need to apply the customer's personal discount rate here.
            val finalFare = (estimatedFare!! * (100 - discountRate) / 100).toInt().toDouble()
            
            val serviceType = if (activeServiceMode == ServiceType.PACKAGE_DELIVERY) 
                ServiceType.PACKAGE_DELIVERY else ServiceType.RIDE_HAILING
            
            repository.createOrder(
                customerId, pickupLocation, dropOffLocation,
                pickupLat ?: 0.0, pickupLng ?: 0.0, dropOffLat ?: 0.0, dropOffLng ?: 0.0,
                distanceKm, finalFare, durationMin, serviceType,
                selectedVehicleType
            ).onSuccess { orderIdStr ->
                val newId = orderIdStr.toIntOrNull()
                orderStatusData = OrderStatusResponse(success = true, status = "PENDING")
                currentOrderId = newId
                sessionManager.setActiveOrderId(newId)
            }.onFailure {
                isOrderPlacing = false
            }
        }
    }

    fun cancelOrder() {
        viewModelScope.launch {
            currentOrderId?.let { repository.cancelOrder(it) }
            clearActiveOrder()
        }
    }

    fun shareTrip(context: android.content.Context) {
        val order = orderStatusData ?: return
        
        // Clean up numeric IDs (e.g., 8.0 -> 8) to avoid 404s on the tracking URL
        val driverId = order.driverId?.toDoubleOrNull()?.toInt()?.toString() ?: order.driverId ?: return
        val deliveryId = order.deliveryId?.toDoubleOrNull()?.toInt()?.toString() ?: order.deliveryId ?: driverId
        
        viewModelScope.launch {
            repository.getShareableTripLink(driverId, deliveryId).onSuccess { response ->
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

    fun submitRating(rating: Float, comment: String) {
        val dId = ratingDriverId ?: return
        val oId = ratingOrderId ?: return
        viewModelScope.launch {
            repository.submitRating(dId, oId, rating, comment).onSuccess {
                showRatingDialog = false
            }.onFailure {
                // Show error?
            }
        }
    }

    fun updateNearbyDrivers(lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.getNearbyDrivers(lat, lng).onSuccess { list ->
                drivers = list
                pickupEtaMin = list.minOfOrNull { it.pickupEtaMin ?: 99.0 }
            }
        }
    }

    fun useCurrentLocation(location: Location, forPickup: Boolean) {
        isLoading = true
        viewModelScope.launch {
            repository.reverseGeocode(location.latitude, location.longitude).onSuccess { suggestion ->
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
        }
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
            repository.cancelRental(id).onSuccess {
                activeRental = null
            }
        }
    }

    fun savePlace(suggestion: LocationSuggestion) {
        viewModelScope.launch {
            val cId = sessionManager.getDriverId()?.toIntOrNull() ?: 1
            val label = if (savedPlaces.none { it.label == "Home" }) "Home" else if (savedPlaces.none { it.label == "Work" }) "Work" else "Favorite"
            repository.savePlace(SavedPlace(customerId = cId, label = label, address = suggestion.displayName, latitude = suggestion.latitude.toDouble(), longitude = suggestion.longitude.toDouble()))
                .onSuccess { 
                    repository.getSavedPlaces(cId.toString()).onSuccess { savedPlaces = it }
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
