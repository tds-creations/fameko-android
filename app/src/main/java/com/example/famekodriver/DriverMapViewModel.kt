package com.example.famekodriver

import android.app.Application
import android.content.Context
import android.os.PowerManager
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.LocationUtils
import com.example.famekodriver.core.utils.VoiceNavigationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

class DriverMapViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DriverRepository()
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
    var incomingCall by mutableStateOf<FamekoEvent.IncomingCall?>(null)
    var ongoingCall by mutableStateOf<FamekoEvent.IncomingCall?>(null)
    var isAccepting by mutableStateOf(false)
    var navigationPath by mutableStateOf<List<LatLng>>(emptyList())
    var driverLatLng by mutableStateOf<LatLng?>(null)
    var driverBearing by mutableStateOf(0f)
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

    init {
        fetchDriverStatus()
        startWebSocket()
        observeEvents()
    }

    fun fetchDriverStatus() {
        val driverId = sessionManager.getDriverId() ?: return
        viewModelScope.launch {
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
            }
            if (isOnline) {
                refreshHeatmap()
            }
        }
    }

    private fun startDailyFeeCountdown() {
        countdownJob?.cancel()
        val expiry = dailyFeeExpiryTime ?: return
        
        countdownJob = viewModelScope.launch {
            while (true) {
                try {
                    // Try multiple formats for PostgreSQL timestamp (e.g. 2023-10-27 10:00:00 or ISO-8601)
                    val expiryDate = try {
                        // Standard format with milliseconds
                        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S")
                        LocalDateTime.parse(expiry, fmt)
                    } catch (e1: Exception) {
                        try {
                            // Standard format without milliseconds
                            val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            LocalDateTime.parse(expiry, fmt)
                        } catch (e2: Exception) {
                            // ISO-8601 Fallback
                            LocalDateTime.parse(expiry.replace(" ", "T"))
                        }
                    }
                    
                    val now = LocalDateTime.now()
                    val secondsRemaining = ChronoUnit.SECONDS.between(now, expiryDate)
                    
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
                }
            }
            is FamekoEvent.IncomingCall -> incomingCall = event
            is FamekoEvent.CallAccepted -> {
                ongoingCall = incomingCall ?: FamekoEvent.IncomingCall(event.callId, "Customer")
                incomingCall = null
            }
            is FamekoEvent.CallEnded, is FamekoEvent.CallRejected -> {
                incomingCall = null
                ongoingCall = null
            }
            is FamekoEvent.OrderCancelled -> {
                if ((currentDelivery?.orderId == event.orderId) || (activeRequest?.orderId == event.orderId)) {
                    currentDelivery = null
                    activeRequest = null
                    navigationPath = emptyList()
                    timerJob?.cancel()
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

    private fun refreshDeliveries() {
        val driverId = sessionManager.getDriverId() ?: return
        viewModelScope.launch {
            repository.getMyDeliveries(driverId).onSuccess { list ->
                currentDelivery = list.firstOrNull { it.status != DeliveryStatus.DELIVERED && it.status != DeliveryStatus.CANCELLED }
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
                currentDelivery = delivery.copy(driverId = driverId, status = DeliveryStatus.ASSIGNED)
            }.onFailure {
                isAccepting = false
            }
        }
    }

    fun rejectDelivery() {
        activeRequest = null
        timerJob?.cancel()
    }

    fun cancelActiveTrip() {
        val delivery = currentDelivery ?: return
        viewModelScope.launch {
            repository.updateDeliveryStatus(delivery.id, DeliveryStatus.CANCELLED).onSuccess {
                currentDelivery = null
                navigationPath = emptyList()
            }
        }
    }

    fun updateDeliveryStatus(nextStatus: DeliveryStatus) {
        val delivery = currentDelivery ?: return
        viewModelScope.launch {
            repository.updateDeliveryStatus(delivery.id, nextStatus).onSuccess {
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
                }
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

    fun initiateCall() {
        val delivery = currentDelivery ?: return
        val driverName = sessionManager.getDriverName() ?: "Driver"
        viewModelScope.launch {
            repository.initiateCall(
                targetId = "CUSTOMER_${delivery.id.split("_").getOrNull(0) ?: 0}", // Placeholder logic for customer ID
                callerName = driverName,
                orderId = delivery.orderId
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

    fun sendAudioData(data: ByteArray) {
        repository.sendAudioData(data)
    }

    fun calculateRoute(start: LatLng, end: LatLng) {
        // If we already calculated for this position (within 50m), don't spam
        lastRouteCalcLatLng?.let { last ->
            val dist = LocationUtils.calculateDistance(start.latitude, start.longitude, last.latitude, last.longitude)
            if (dist < 50.0 && navigationPath.isNotEmpty()) return
        }

        routeJob?.cancel()
        routeJob = viewModelScope.launch {
            repository.calculateRoute(RouteRequest(RouteLocation(start.latitude, start.longitude), RouteLocation(end.latitude, end.longitude)))
                .onSuccess { response ->
                    val isFirstCalc = navigationPath.isEmpty()
                    navigationPath = response.routeCoords.map { LatLng(it[1], it[0]) }
                    lastRouteCalcLatLng = start
                    
                    if (isFirstCalc) {
                        val destName = if (currentDelivery?.status == DeliveryStatus.ASSIGNED) "pickup" else "destination"
                        voiceNavManager.announceTripStart(destName)
                    }
                }
        }
    }

    fun updateDriverLocation(lat: Double, lng: Double, bearing: Float) {
        driverLatLng = LatLng(lat, lng)
        driverBearing = bearing
        
        currentDelivery?.let { delivery ->
            if (navigationPath.isNotEmpty()) {
                voiceNavManager.updateProgress(
                    lat, lng, 
                    navigationPath.map { it.latitude to it.longitude },
                    delivery.pickupEtaMin ?: 5.0, // Placeholder ETA
                    delivery.distanceKm
                )
            }
        }
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
