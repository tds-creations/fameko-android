package com.example.famekodriver

import android.app.Application
import android.content.Context
import android.os.PowerManager
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.LocationUtils
import com.example.famekodriver.core.utils.MapCacheManager
import com.example.famekodriver.core.utils.VoiceNavigationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

class DriverMapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DriverRepository.getInstance()
    private val sessionManager = SessionManager(application)

    // --- Screen State ---
    var isOnline by mutableStateOf(sessionManager.isOnline())
    var isDailyFeePaid by mutableStateOf(value = true)
    var dailyFeeAmount by mutableDoubleStateOf(0.0)
    var vehicleCategory by mutableStateOf<String?>(null)
    var showPaymentDialog by mutableStateOf(false)
    var isPaying by mutableStateOf(false)
    var payDailyFeeCheckoutUrl by mutableStateOf<String?>(null)
    var dailyFeeExpiryTime by mutableStateOf<String?>(null)
    var dailyFeeRemainingSeconds by mutableLongStateOf(0L)

    var activeRequest by mutableStateOf<Delivery?>(null)
    var currentDelivery by mutableStateOf<Delivery?>(null)
    var driverStats by mutableStateOf(DriverStats())
    var incomingCall by mutableStateOf<FamekoEvent.IncomingCall?>(null)
    var ongoingCall by mutableStateOf<FamekoEvent.IncomingCall?>(null)
    var isAccepting by mutableStateOf(false)
    var navigationPath by mutableStateOf<List<LatLng>>(emptyList())
    var originalNavigationPath by mutableStateOf<List<LatLng>>(emptyList())
        private set
    var remainingNavigationPath by mutableStateOf<List<LatLng>>(emptyList())
        private set

    private var currentRouteSegmentIndex = 0
    private var originalRouteDistanceMeters = 0.0
    private var originalRouteEtaMin = 0.0
    var instructions by mutableStateOf<List<RouteInstruction>>(emptyList())
    var currentInstruction by mutableStateOf<RouteInstruction?>(null)
    var nextInstruction by mutableStateOf<RouteInstruction?>(null)
    var distanceToNextManeuverMeters by mutableDoubleStateOf(0.0)
    var distanceKm by mutableDoubleStateOf(0.0)
    var durationMin by mutableDoubleStateOf(0.0)
    enum class NavigationCameraMode {
        FOLLOWING,
        SUSPENDED,
        OVERVIEW
    }

    var navCameraMode by mutableStateOf(NavigationCameraMode.FOLLOWING)
    var isFullscreenMap by mutableStateOf(false)
    var driverLatLng by mutableStateOf<LatLng?>(null)
    var rawDriverLatLng by mutableStateOf<LatLng?>(null)
    var visualDriverLatLng by mutableStateOf<LatLng?>(null)
    var visualDriverBearing by mutableFloatStateOf(0f)
    var driverBearing by mutableStateOf(0f)
    var isOfflineMode by mutableStateOf(false)
    private var latestProjectionResult: com.example.famekodriver.core.utils.ProjectionResult? = null
    var heatmapPoints by mutableStateOf<List<HeatmapPoint>>(emptyList())
    var currentSurge by mutableStateOf<SurgeInfo?>(null)
    var requestTimer by mutableIntStateOf(30)
    
    var showPinDialog by mutableStateOf(false)
    var enteredPin by mutableStateOf("")
    var isVerifyingPin by mutableStateOf(false)

    var showSOSDialog by mutableStateOf(false)
    var sosType by mutableStateOf("")
    var otherSOSType by mutableStateOf("")
    var isSendingSOS by mutableStateOf(false)

    var showBatteryOptimizationDialog by mutableStateOf(false)

    var showTripSummary by mutableStateOf(false)
    var finalFare by mutableDoubleStateOf(0.0)
    var finalTotalFare by mutableDoubleStateOf(0.0)
    var isFinalTripRental by mutableStateOf(false)

    var isVoiceEnabled by mutableStateOf(true)
    private val voiceNavManager = VoiceNavigationManager(application)

    private var timerJob: Job? = null
    private var countdownJob: Job? = null
    private var routeJob: Job? = null
    private var lastRouteCalcLatLng: LatLng? = null
    private var lastAcceptanceTime = 0L

    init {
        fetchDriverStatus()
        startWebSocket()
        observeEvents()
    }

    fun fetchDriverStatus() {
        val driverId = sessionManager.getDriverId() ?: return
        viewModelScope.launch {
            repository.getDriverStats(driverId).onSuccess { driverStats = it }
            repository.getDriverStatus(driverId).onSuccess { resp ->
                isDailyFeePaid = resp.isDailyFeePaid
                dailyFeeAmount = resp.dailyFeeAmount
                vehicleCategory = resp.vehicleCategory
                dailyFeeExpiryTime = resp.dailyFeeExpiryTime
                
                // Sync online status with backend
                if (isOnline != resp.isOnline) {
                    isOnline = resp.isOnline
                    sessionManager.setOnline(resp.isOnline)
                    if (resp.isOnline) {
                        repository.startWebSocket("DRIVER_$driverId")
                    } else {
                        repository.stopWebSocket()
                    }
                }

                startDailyFeeCountdown()
                
                if (isOnline) {
                    refreshDeliveries()
                    refreshHeatmap()
                }
            }
        }
    }

    private fun startDailyFeeCountdown() {
        countdownJob?.cancel()
        val expiry = dailyFeeExpiryTime ?: return
        
        countdownJob = viewModelScope.launch {
            while (true) {
                try {
                    val expiryMillis = try {
                        java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).parse(expiry)?.time
                            ?: java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S", java.util.Locale.getDefault()).parse(expiry)?.time
                            ?: 0L
                    } catch (_: Exception) { 0L }
                    
                    val secondsRemaining = (expiryMillis - System.currentTimeMillis()) / 1000
                    
                    if (secondsRemaining <= 0) {
                        dailyFeeRemainingSeconds = 0
                        isDailyFeePaid = false
                        if (isOnline) toggleOnlineStatus(false)
                        break
                    }
                    
                    dailyFeeRemainingSeconds = secondsRemaining
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
                delay(1.seconds)
            }
        }
    }

    private fun startWebSocket() {
        val driverId = sessionManager.getDriverId() ?: return
        // We connect even if offline to receive status updates (like daily fee paid)
        repository.startWebSocket("DRIVER_$driverId")
    }

    private fun observeEvents() {
        viewModelScope.launch {
            repository.events.collect { event ->
                handleEvent(event)
            }
        }
    }

    private fun handleEvent(event: FamekoEvent) {
        when (event) {
            is FamekoEvent.NewDeliveryRequest -> {
                if (currentDelivery == null) {
                    activeRequest = event.delivery
                    startRequestTimer()
                    val intent = android.content.Intent(getApplication(), MainActivity::class.java).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    com.example.famekodriver.core.utils.NotificationHelper.showNotification(
                        getApplication(),
                        "New Order Request!",
                        "New job at ${event.delivery.pickupLocation}",
                        intent,
                        isNewOrder = true
                    )
                }
            }
            is FamekoEvent.IncomingCall -> incomingCall = event
            is FamekoEvent.CallAccepted -> {
                val currentPending = ongoingCall
                ongoingCall = if (currentPending != null && currentPending.callId == "pending") {
                    currentPending.copy(callId = event.callId)
                } else {
                    FamekoEvent.IncomingCall(event.callId, "Customer")
                }
                incomingCall = null
            }
            is FamekoEvent.CallEnded, is FamekoEvent.CallRejected -> {
                incomingCall = null
                ongoingCall = null
            }
            is FamekoEvent.OrderCancelled -> {
                val acceptedDelivery = currentDelivery
                if (acceptedDelivery != null && (acceptedDelivery.orderId == event.orderId || event.orderId == 0)) {
                    currentDelivery = null
                    activeRequest = null
                    clearRouteAndNavigation()
                    timerJob?.cancel()
                    com.example.famekodriver.core.utils.NotificationHelper.cancelNotification(getApplication(), event.orderId)
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Order #${event.orderId} was cancelled by customer",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } else if (activeRequest?.orderId == event.orderId) {
                    // Driver was viewing unaccepted request sheet when customer cancelled before acceptance
                    activeRequest = null
                    timerJob?.cancel()
                    com.example.famekodriver.core.utils.NotificationHelper.cancelNotification(getApplication(), event.orderId)
                }
            }
            is FamekoEvent.OfferInvalidated -> {
                if (activeRequest?.orderId == event.orderId) {
                    activeRequest = null
                    timerJob?.cancel()
                    com.example.famekodriver.core.utils.NotificationHelper.cancelNotification(getApplication(), event.orderId)
                }
            }
            is FamekoEvent.OrderStatusUpdate -> {
                refreshDeliveries()
            }
            is FamekoEvent.NotificationReceived -> {
                if (event.type == "DAILY_FEE_PAID") {
                    fetchDriverStatus()
                }
            }
            else -> {}
        }
    }

    private fun startRequestTimer() {
        timerJob?.cancel()
        requestTimer = 30
        timerJob = viewModelScope.launch {
            while (requestTimer > 0) {
                delay(1.seconds)
                requestTimer--
            }
            activeRequest = null
        }
    }

    fun simulateTestIncomingRequest(isDelivery: Boolean = false) {
        activeRequest = Delivery(
            id = "test_req_001",
            orderId = 101,
            driverId = null,
            pickupLocation = "2, Temple Street, Santa Maria, Sowutuom Rd",
            dropOffLocation = "Trako Street, Nyamekye, Abeka, Accra",
            pickupLat = 5.6037,
            pickupLng = -0.1870,
            dropOffLat = 5.6100,
            dropOffLng = -0.1900,
            status = DeliveryStatus.PENDING,
            distanceKm = 2.5,
            estimatedEarnings = 30.00,
            customerId = 42,
            customerName = "Joseph Lamptey",
            customerPhone = "0240000000",
            serviceType = if (isDelivery) ServiceType.PACKAGE_DELIVERY else ServiceType.RIDE_HAILING,
            pickupEtaMin = 2.0,
            totalFare = 30.00,
            recipientName = "Kofi Mensah",
            packageCategory = "Electronics"
        )
        startRequestTimer()
    }

    fun checkAndGoOnline(status: String) {
        if (isOnline) {
            // Already online, go offline
            toggleOnlineStatus(online = false)
            return
        }

        viewModelScope.launch {
            // Wait for fresh status
            val driverId = sessionManager.getDriverId() ?: return@launch
            repository.getDriverStatus(driverId).onSuccess { resp ->
                isDailyFeePaid = resp.isDailyFeePaid
                dailyFeeAmount = resp.dailyFeeAmount
                dailyFeeExpiryTime = resp.dailyFeeExpiryTime
                
                if (status == "APPROVED") {
                    if (!isOnline && !resp.isDailyFeePaid) {
                        showPaymentDialog = true
                    } else {
                        val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
                        if (!powerManager.isIgnoringBatteryOptimizations(getApplication<Application>().packageName)) {
                            showBatteryOptimizationDialog = true
                        } else {
                            toggleOnlineStatus(online = true)
                        }
                    }
                }
            }
        }
    }

    fun toggleOnlineStatus(online: Boolean) {
        val driverId = sessionManager.getDriverId() ?: return
        viewModelScope.launch {
            repository.updateOnlineStatus(driverId, online).onSuccess {
                isOnline = online
                sessionManager.setOnline(online)
                if (online) {
                    repository.startWebSocket("DRIVER_$driverId")
                    refreshDeliveries()
                    refreshHeatmap()
                } else {
                    repository.stopWebSocket()
                    activeRequest = null
                    heatmapPoints = emptyList()
                    currentSurge = null
                }
            }
        }
    }

    fun refreshDeliveries() {
        val driverId = sessionManager.getDriverId() ?: return
        viewModelScope.launch {
            repository.getMyDeliveries(driverId).onSuccess { list ->
                currentDelivery = list.firstOrNull { it.status != DeliveryStatus.DELIVERED && it.status != DeliveryStatus.CANCELLED }
                if (currentDelivery == null && activeRequest == null) {
                    clearRouteAndNavigation()
                }
            }
            if (isOnline && currentDelivery == null) {
                repository.getAvailableDeliveries(
                    driverLatLng?.latitude ?: 0.0, 
                    driverLatLng?.longitude ?: 0.0, 
                    sessionManager.getVehicleType(),
                    vehicleCategory
                ).onSuccess { list ->
                    if (list.isNotEmpty()) {
                        activeRequest = list[0]
                        startRequestTimer()
                    }
                }
            }
        }
    }

    fun clearRouteAndNavigation() {
        originalNavigationPath = emptyList()
        remainingNavigationPath = emptyList()
        navigationPath = emptyList()
        currentRouteSegmentIndex = 0
        originalRouteDistanceMeters = 0.0
        originalRouteEtaMin = 0.0
        instructions = emptyList()
        currentInstruction = null
        nextInstruction = null
        distanceToNextManeuverMeters = 0.0
        isFullscreenMap = false
        routeJob?.cancel()
    }

    private fun refreshHeatmap() {
        viewModelScope.launch {
            repository.getHeatmapData().onSuccess { heatmapPoints = it }
            repository.getCurrentSurge().onSuccess { currentSurge = it }
        }
    }

    fun acceptDelivery() {
        val delivery = activeRequest ?: return
        val driverId = sessionManager.getDriverId() ?: return
        isAccepting = true
        viewModelScope.launch {
            repository.acceptDelivery(driverId, delivery.id).onSuccess {
                isAccepting = false
                activeRequest = null
                timerJob?.cancel()
                lastAcceptanceTime = System.currentTimeMillis()
                currentDelivery = delivery.copy(driverId = driverId, status = DeliveryStatus.ASSIGNED)
            }.onFailure {
                isAccepting = false
            }
        }
    }

    fun rejectDelivery() {
        activeRequest = null
        clearRouteAndNavigation()
        timerJob?.cancel()
    }

    fun cancelActiveTrip() {
        val delivery = currentDelivery ?: return
        viewModelScope.launch {
            repository.updateDeliveryStatus(delivery.id, DeliveryStatus.CANCELLED).onSuccess {
                currentDelivery = null
                clearRouteAndNavigation()
            }
        }
    }

    fun updateDeliveryStatus(nextStatus: DeliveryStatus) {
        val delivery = currentDelivery ?: return
        Log.d("TripStatus", "Updating status for delivery ${delivery.id} to $nextStatus")
        viewModelScope.launch {
            repository.updateDeliveryStatus(delivery.id, nextStatus).onSuccess {
                Log.d("TripStatus", "Status update SUCCESS for ${delivery.id}")
                if (nextStatus == DeliveryStatus.DELIVERED) {
                    voiceNavManager.announceArrival()
                    finalFare = delivery.estimatedEarnings
                    isFinalTripRental = delivery.serviceType == ServiceType.RENTAL
                    finalTotalFare = delivery.totalFare ?: if (isFinalTripRental) {
                        delivery.estimatedEarnings / 0.85
                    } else {
                        delivery.estimatedEarnings
                    }
                    showTripSummary = true
                    currentDelivery = null
                    navigationPath = emptyList()
                } else {
                    currentDelivery = delivery.copy(status = nextStatus)
                    if (nextStatus == DeliveryStatus.ARRIVED) {
                        clearRouteAndNavigation()
                        showPinDialog = true
                    }
                }
            }.onFailure {
                Log.e("TripStatus", "Status update FAILED for ${delivery.id}", it)
            }
        }
    }

    fun verifyPin() {
        val delivery = currentDelivery ?: return
        isVerifyingPin = true
        viewModelScope.launch {
            repository.verifyTripPin(delivery.orderId, enteredPin).onSuccess { success ->
                if (success) {
                    updateDeliveryStatus(DeliveryStatus.IN_TRANSIT)
                    showPinDialog = false
                    enteredPin = ""

                    // Draw actual trip route to destination AFTER PIN has been entered & verified
                    val pLat = delivery.pickupLat ?: 0.0
                    val pLng = delivery.pickupLng ?: 0.0
                    val dLat = delivery.dropOffLat ?: 0.0
                    val dLng = delivery.dropOffLng ?: 0.0
                    if (pLat != 0.0 && dLat != 0.0) {
                        calculateRoute(LatLng(pLat, pLng), LatLng(dLat, dLng))
                    }
                }
                isVerifyingPin = false
            }.onFailure {
                isVerifyingPin = false
            }
        }
    }

    fun triggerSOS(lat: Double, lng: Double) {
        val type = if (sosType == "Other") otherSOSType else sosType
        if (type.isEmpty()) return
        isSendingSOS = true
        viewModelScope.launch {
            val driverId = sessionManager.getDriverId() ?: ""
            repository.triggerSOS(driverId, lat, lng, type).onSuccess {
                showSOSDialog = false
                sosType = ""
                otherSOSType = ""
            }
            isSendingSOS = false
        }
    }

    fun payDailyFee() {
        val driverId = sessionManager.getDriverId() ?: return
        isPaying = true
        viewModelScope.launch {
            repository.payDailyFee(driverId).onSuccess { resp ->
                // If resp is null but success was true, it means it was auto-approved (Test Mode)
                if (resp == null) {
                    isDailyFeePaid = true
                    showPaymentDialog = false
                    toggleOnlineStatus(true)
                } else {
                    payDailyFeeCheckoutUrl = resp
                    showPaymentDialog = false
                }
            }
            isPaying = false
        }
    }

    fun initiateCall(context: Context) {
        val delivery = currentDelivery ?: activeRequest ?: return
        val phone = delivery.currentContactPhone.ifBlank { delivery.customerPhone ?: delivery.recipientPhone }
        val label = delivery.currentContactName
        com.example.famekodriver.core.utils.PhoneNumberUtils.openDialer(context, phone, label)
    }

    fun sendAudioData(data: ByteArray) {
        repository.sendAudioData(data)
    }

    fun calculateRoute(start: LatLng, end: LatLng, context: Context? = null) {
        // If we already calculated for this position (within 50m), don't spam
        lastRouteCalcLatLng?.let { last ->
            val dist = LocationUtils.calculateDistance(start.latitude, start.longitude, last.latitude, last.longitude)
            if (dist < 50.0 && originalNavigationPath.isNotEmpty()) return
        }

        routeJob?.cancel()
        routeJob = viewModelScope.launch(Dispatchers.Default) {
            repository.calculateRoute(RouteRequest(RouteLocation(start.latitude, start.longitude), RouteLocation(end.latitude, end.longitude)))
                .onSuccess { response ->
                    val path = response.routeCoords.map { LatLng(it[1], it[0]) }
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        val isFirstCalc = originalNavigationPath.isEmpty()
                        originalNavigationPath = path
                        remainingNavigationPath = path
                        navigationPath = path
                        currentRouteSegmentIndex = 0
                        originalRouteDistanceMeters = response.distanceM.toDouble()
                        originalRouteEtaMin = response.etaMin
                        instructions = response.instructions
                        currentInstruction = instructions.firstOrNull()
                        nextInstruction = instructions.getOrNull(1)
                        distanceKm = response.distanceM / 1000.0
                        durationMin = response.etaMin
                        lastRouteCalcLatLng = start
                        isOfflineMode = false
                        
                        if (isFirstCalc) {
                            val destName = if (currentDelivery?.status == DeliveryStatus.ASSIGNED) "pickup" else "destination"
                            voiceNavManager.announceTripStart(destName)
                        }

                        // Phase 1: Save route to Room DB
                        context?.let { ctx -> saveCurrentRouteToRoom(ctx) }
                        // Phase 2: Route corridor tile prefetching
                        context?.let { ctx -> MapCacheManager.prefetchRouteCorridor(ctx, path, "Order_${currentDelivery?.id ?: 0}") }
                    }
                }
                .onFailure {
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        isOfflineMode = true
                    }
                }
        }
    }

    fun updateDriverLocation(lat: Double, lng: Double, bearing: Float, context: Context? = null) {
        // Phase 3: Separate Raw vs Visual Location
        rawDriverLatLng = LatLng(lat, lng)

        // Low-pass EMA filter for visual smoothing
        val prevVisual = visualDriverLatLng
        val alpha = 0.35
        val smoothedLat = if (prevVisual != null) prevVisual.latitude + alpha * (lat - prevVisual.latitude) else lat
        val smoothedLng = if (prevVisual != null) prevVisual.longitude + alpha * (lng - prevVisual.longitude) else lng

        // Low-pass bearing filter
        if (bearing != 0f) {
            val diff = (bearing - visualDriverBearing + 540) % 360 - 180
            visualDriverBearing = (visualDriverBearing + 0.35f * diff + 360) % 360
            driverBearing = visualDriverBearing
        }
        
        val delivery = currentDelivery ?: activeRequest
        val activePath = remainingNavigationPath.ifEmpty { originalNavigationPath.ifEmpty { navigationPath } }
        if (delivery != null && activePath.isNotEmpty()) {
            // Update remaining distance, ETA, turn instructions and trim polyline
            updateRouteProgression(lat, lng)

            // Visual road snapping if close to route segment (<= 30m)
            val proj = latestProjectionResult
            if (proj != null && proj.distanceMeters <= 30.0) {
                visualDriverLatLng = LatLng(proj.projectedLat, proj.projectedLng)
            } else {
                visualDriverLatLng = LatLng(smoothedLat, smoothedLng)
            }
            driverLatLng = visualDriverLatLng ?: rawDriverLatLng

            // Off-route detection using original full route geometry (only if online)
            if (!isOfflineMode) {
                checkOffRoute(lat, lng)
            }
            
            // Arrival detection (Voice triggers inside checkArrival - uses raw coordinates)
            checkArrival(lat, lng)

            val progressPath = remainingNavigationPath.ifEmpty { activePath }
            voiceNavManager.updateProgress(
                lat, lng, 
                progressPath.map { it.latitude to it.longitude },
                durationMin,
                distanceKm,
                currentInstruction = currentInstruction,
                distToManeuverMeters = distanceToNextManeuverMeters,
                instructionIndex = instructions.indexOf(currentInstruction)
            )

            // Periodically save navigation progress to Room
            if (currentRouteSegmentIndex % 3 == 0 && context != null) {
                saveCurrentRouteToRoom(context)
            }
        } else {
            visualDriverLatLng = LatLng(smoothedLat, smoothedLng)
            driverLatLng = visualDriverLatLng ?: rawDriverLatLng
        }
    }

    private fun updateRouteProgression(currentLat: Double, currentLng: Double) {
        val origPath = originalNavigationPath.ifEmpty { navigationPath }
        if (origPath.size < 2) return

        val searchEndIndex = minOf(currentRouteSegmentIndex + 5, origPath.size - 2)
        var bestSegmentIdx = currentRouteSegmentIndex
        var minDistance = Double.MAX_VALUE
        var bestProjection: com.example.famekodriver.core.utils.ProjectionResult? = null

        for (i in currentRouteSegmentIndex..searchEndIndex) {
            val p1 = origPath[i]
            val p2 = origPath[i + 1]
            val proj = LocationUtils.projectPointOnSegment(
                currentLat, currentLng,
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude
            )
            if (proj.distanceMeters < minDistance) {
                minDistance = proj.distanceMeters
                bestSegmentIdx = i
                bestProjection = proj
            }
        }

        latestProjectionResult = bestProjection

        if (minDistance <= 50.0 && bestProjection != null) {
            if (bestSegmentIdx > currentRouteSegmentIndex || (bestSegmentIdx == currentRouteSegmentIndex && bestProjection.fraction > 0.05)) {
                currentRouteSegmentIndex = bestSegmentIdx

                val remaining = mutableListOf<LatLng>()
                remaining.add(LatLng(bestProjection.projectedLat, bestProjection.projectedLng))
                for (k in (bestSegmentIdx + 1) until origPath.size) {
                    remaining.add(origPath[k])
                }

                remainingNavigationPath = remaining
                navigationPath = remaining
            }
        }

        updateCurrentInstruction(currentLat, currentLng)

        val activeRemaining = remainingNavigationPath.ifEmpty { origPath }
        var remDistMeters = 0.0
        if (activeRemaining.size >= 2) {
            for (i in 0 until activeRemaining.size - 1) {
                val p1 = activeRemaining[i]
                val p2 = activeRemaining[i + 1]
                remDistMeters += LocationUtils.calculateDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude)
            }
        }

        distanceKm = remDistMeters / 1000.0
        durationMin = if (originalRouteDistanceMeters > 0.0 && originalRouteEtaMin > 0.0) {
            maxOf(1.0, (remDistMeters / originalRouteDistanceMeters) * originalRouteEtaMin)
        } else {
            maxOf(1.0, (distanceKm / 30.0) * 60.0)
        }
    }

    // Phase 1 Room Persistence
    fun saveCurrentRouteToRoom(context: Context) {
        val delivery = currentDelivery ?: return
        val orig = originalNavigationPath
        if (orig.size < 2) return
        viewModelScope.launch(Dispatchers.IO) {
            val gson = com.google.gson.Gson()
            val entity = com.example.famekodriver.core.data.local.entity.NavigationRouteEntity(
                orderId = delivery.id.toIntOrNull() ?: delivery.id.hashCode(),
                status = delivery.status.name,
                pickupLat = delivery.pickupLat ?: 0.0,
                pickupLng = delivery.pickupLng ?: 0.0,
                dropOffLat = delivery.dropOffLat ?: 0.0,
                dropOffLng = delivery.dropOffLng ?: 0.0,
                originalPathJson = gson.toJson(orig.map { listOf(it.longitude, it.latitude) }),
                instructionsJson = gson.toJson(instructions),
                currentSegmentIndex = currentRouteSegmentIndex,
                remainingDistanceKm = distanceKm,
                remainingDurationMin = durationMin
            )
            repository.saveNavigationRoute(context, entity)
        }
    }

    fun restoreRouteFromRoom(context: Context, orderId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = repository.getNavigationRoute(context, orderId) ?: return@launch
            val gson = com.google.gson.Gson()
            try {
                val rawCoords: List<List<Double>> = gson.fromJson(entity.originalPathJson, object : com.google.gson.reflect.TypeToken<List<List<Double>>>() {}.type)
                val path = rawCoords.map { LatLng(it[1], it[0]) }
                val parsedInstructions: List<RouteInstruction> = gson.fromJson(entity.instructionsJson, object : com.google.gson.reflect.TypeToken<List<RouteInstruction>>() {}.type)
                
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    if (path.size >= 2) {
                        originalNavigationPath = path
                        currentRouteSegmentIndex = entity.currentSegmentIndex.coerceIn(0, (path.size - 2).coerceAtLeast(0))
                        remainingNavigationPath = path.subList(currentRouteSegmentIndex, path.size)
                        navigationPath = remainingNavigationPath
                        instructions = parsedInstructions
                        currentInstruction = instructions.getOrNull(entity.currentSegmentIndex)
                        nextInstruction = instructions.getOrNull(entity.currentSegmentIndex + 1)
                        distanceKm = entity.remainingDistanceKm
                        durationMin = entity.remainingDurationMin
                        Log.d("RouteRestore", "Restored Room route for order $orderId successfully.")
                    }
                }
            } catch (e: Exception) {
                Log.e("RouteRestore", "Failed to parse cached Room route", e)
            }
        }
    }

    fun deleteRouteFromRoom(context: Context, orderId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteNavigationRoute(context, orderId)
        }
    }

    private fun updateCurrentInstruction(currentLat: Double, currentLng: Double) {
        if (instructions.isEmpty()) {
            currentInstruction = null
            nextInstruction = null
            distanceToNextManeuverMeters = 0.0
            return
        }
        
        var currentIndex = instructions.indexOf(currentInstruction)
        if (currentIndex == -1) {
            currentInstruction = instructions.first()
            nextInstruction = instructions.getOrNull(1)
            currentIndex = 0
        }

        val activeInstruction = currentInstruction ?: instructions.first()
        val pt = activeInstruction.point
        if (pt != null && pt.size >= 2) {
            val instrLng = pt[0]
            val instrLat = pt[1]
            val dist = LocationUtils.calculateDistance(currentLat, currentLng, instrLat, instrLng)
            distanceToNextManeuverMeters = dist

            if (dist < 30.0 && currentIndex < instructions.size - 1) {
                currentInstruction = instructions[currentIndex + 1]
                nextInstruction = instructions.getOrNull(currentIndex + 2)
            }
        } else {
            distanceToNextManeuverMeters = 0.0
        }
    }

    /**
     * Checks if the user is off-route and triggers recalculation if needed.
     */
    fun checkOffRoute(currentLat: Double, currentLng: Double) {
        val origPath = originalNavigationPath.ifEmpty { navigationPath }
        if (origPath.size < 2 || routeJob?.isActive == true) return
        
        var minDistance = Double.MAX_VALUE
        for (i in 0 until origPath.size - 1) {
            val p1 = origPath[i]
            val p2 = origPath[i + 1]
            val dist = LocationUtils.distanceToSegment(
                currentLat, currentLng,
                p1.latitude, p1.longitude,
                p2.latitude, p2.longitude
            )
            if (dist < minDistance) minDistance = dist
        }
        
        // If deviating more than 50 meters, recalculate
        if (minDistance > 50.0) {
            val delivery = currentDelivery ?: return
            val dest = if (delivery.status == DeliveryStatus.ASSIGNED || delivery.status == DeliveryStatus.ARRIVED) {
                LatLng(delivery.pickupLat ?: 0.0, delivery.pickupLng ?: 0.0)
            } else {
                LatLng(delivery.dropOffLat ?: 0.0, delivery.dropOffLng ?: 0.0)
            }
            if (dest.latitude != 0.0) {
                calculateRoute(LatLng(currentLat, currentLng), dest)
            }
        }
    }

    /**
     * Checks if the driver has arrived at the next stop (pickup or dropoff).
     */
    fun checkArrival(currentLat: Double, currentLng: Double): Boolean {
        val delivery = currentDelivery ?: return false
        
        // 2-second grace period after acceptance to avoid immediate "snap" to Arrived state
        if (System.currentTimeMillis() - lastAcceptanceTime < 2000) return false

        val isAtPickup = delivery.status == DeliveryStatus.ASSIGNED
        val destLat = if (isAtPickup) delivery.pickupLat else delivery.dropOffLat
        val destLng = if (isAtPickup) delivery.pickupLng else delivery.dropOffLng
        
        if (destLat == null || destLng == null || destLat == 0.0) return false
        
        val distToDest = LocationUtils.calculateDistance(currentLat, currentLng, destLat, destLng)
        
        if (distToDest < 30.0) { // Within 30 meters
            if (isAtPickup) {
                // Arrived at pickup, auto-trigger "Arrived" state
                voiceNavManager.announceArrivedAtPickup()
                updateDeliveryStatus(DeliveryStatus.ARRIVED)
                showPinDialog = true // Prompt for pickup PIN
                
                isFullscreenMap = false
                navigationPath = emptyList()
                instructions = emptyList()
                currentInstruction = null
                return true
            } else if (delivery.status == DeliveryStatus.IN_TRANSIT) {
                // Arrived at final destination
                voiceNavManager.announceArrival()
                // We don't auto-complete the trip, just announce and let driver tap COMPLETE
                return true
            }
        }
        return false
    }

    fun toggleVoice(enabled: Boolean) {
        isVoiceEnabled = enabled
        voiceNavManager.setEnabled(enabled)
    }

    override fun onCleared() {
        super.onCleared()
        voiceNavManager.shutdown()
    }
}
