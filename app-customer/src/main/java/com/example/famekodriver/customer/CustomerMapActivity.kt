// Fameko Customer Map Activity - Cleaned
package com.example.famekodriver.customer

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.lifecycle.lifecycleScope
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.core.net.toUri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.famekodriver.core.utils.ImageLinks
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.VoiceCallHandler
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.messaging.FirebaseMessaging
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel

import com.example.famekodriver.customer.ui.theme.*

class CustomerMapViewModelFactory(
    private val repository: DriverRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerMapViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

enum class CustomerSheetState {
    IDLE,
    PICKING_ADDRESS,
    SELECTING_SERVICE,
    SEARCHING_FOR_DRIVER,
    ON_TRIP,
    ACTIVE_RENTAL
}

class CustomerMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val sessionManager = SessionManager(this)
        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, CustomerLoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        updateFcmToken(sessionManager)
        setContent {
            FamekoTheme {
                CustomerMapScreen()
            }
        }
    }

    private fun updateFcmToken(sessionManager: SessionManager) {
        val customerId = sessionManager.getCustomerId() ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val repository = DriverRepository()
                lifecycleScope.launch {
                    repository.updateFcmToken(customerId, token, "customer")
                }
            }
        }
    }
}


@Composable
fun FamekoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = FamekoBlue,
            onPrimary = Color.White,
            secondary = BoltGreen,
            onSecondary = Color.White,
            surface = Color.White,
            onSurface = BoltDark,
            background = Color.White,
            onBackground = BoltDark,
            error = BoltOrange
        ),
        typography = Typography(
            headlineMedium = androidx.compose.ui.text.TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp
            ),
            titleLarge = androidx.compose.ui.text.TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.sp
            ),
            bodyLarge = androidx.compose.ui.text.TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            )
        ),
        content = content
    )
}

sealed class CustomerScreen {
    object Landing : CustomerScreen()
    object MainMap : CustomerScreen()
    data class Chat(val orderId: Int, val driverName: String) : CustomerScreen()
    object Trips : CustomerScreen()
    object Rentals : CustomerScreen()
    object RideHistory : CustomerScreen()
    object Promotions : CustomerScreen()
    object Support : CustomerScreen()
    object NotificationSettings : CustomerScreen()
    object FleetBrowse : CustomerScreen()
    data class VehicleDetails(val vehicle: Map<String, Any>) : CustomerScreen()
    data class RentalBooking(val vehicle: Map<String, Any>) : CustomerScreen()
    data class RentalDetails(val rental: Map<String, Any>) : CustomerScreen()
    data class PaystackCheckout(val url: String) : CustomerScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMapScreen() {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    
    val viewModel: CustomerMapViewModel = viewModel(
        modelClass = CustomerMapViewModel::class.java,
        factory = CustomerMapViewModelFactory(repository, sessionManager)
    )

    val scope = rememberCoroutineScope()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler(enabled = viewModel.currentScreen != CustomerScreen.Landing) {
        viewModel.navigateTo(CustomerScreen.Landing)
    }

    when (val screen = viewModel.currentScreen) {
        CustomerScreen.Landing -> {
            BackHandler {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
            CustomerLandingScreen(
                onServiceSelected = { service ->
                    viewModel.setServiceMode(service)
                    if (service == ServiceType.RENTAL) {
                        viewModel.navigateTo(CustomerScreen.FleetBrowse)
                    } else {
                        viewModel.navigateTo(CustomerScreen.MainMap)
                    }
                }
            )
        }
        is CustomerScreen.MainMap -> {
            MainMapContent(
                viewModel = viewModel,
                onNavigateToChat = { orderId, name -> viewModel.navigateTo(CustomerScreen.Chat(orderId, name)) },
                onNavigateToTrips = { viewModel.navigateTo(CustomerScreen.Trips) },
                onNavigateToRentals = { viewModel.navigateTo(CustomerScreen.Rentals) },
                onNavigateToRideHistory = { viewModel.navigateTo(CustomerScreen.RideHistory) },
                onNavigateToPromos = { viewModel.navigateTo(CustomerScreen.Promotions) },
                onNavigateToSupport = { viewModel.navigateTo(CustomerScreen.Support) },
                onNavigateToNotificationSettings = { viewModel.navigateTo(CustomerScreen.NotificationSettings) }
            )
        }
        is CustomerScreen.Chat -> {
            CustomerChatScreen(
                orderId = screen.orderId,
                driverName = screen.driverName,
                onBack = { viewModel.navigateTo(CustomerScreen.MainMap) }
            )
        }
        CustomerScreen.Trips -> {
            TripsScreen(onBack = { viewModel.navigateTo(CustomerScreen.MainMap) })
        }
        CustomerScreen.Rentals -> {
            RentalsScreen(
                onBack = { viewModel.navigateTo(CustomerScreen.MainMap) },
                onNavigateToDetails = { rental -> viewModel.navigateTo(CustomerScreen.RentalDetails(rental)) }
            )
        }
        is CustomerScreen.RentalDetails -> {
            RentalDetailsScreen(
                rental = screen.rental,
                onBack = { viewModel.navigateTo(CustomerScreen.Rentals) },
                onNavigateToMainMap = { viewModel.navigateTo(CustomerScreen.MainMap) }
            )
        }
        CustomerScreen.RideHistory -> {
            RideHistoryScreen(onBack = { viewModel.navigateTo(CustomerScreen.MainMap) })
        }
        CustomerScreen.Promotions -> {
            PromotionsScreen(onBack = { viewModel.navigateTo(CustomerScreen.MainMap) })
        }
        CustomerScreen.Support -> {
            SupportScreen(onBack = { viewModel.navigateTo(CustomerScreen.MainMap) })
        }
        CustomerScreen.NotificationSettings -> {
            CustomerNotificationSettingsScreen(onBack = { viewModel.navigateTo(CustomerScreen.MainMap) })
        }
        CustomerScreen.FleetBrowse -> {
            FleetSelectionScreen(
                onBack = { viewModel.navigateTo(CustomerScreen.MainMap) },
                onVehicleDetails = { vehicle ->
                    viewModel.navigateTo(CustomerScreen.VehicleDetails(vehicle))
                }
            )
        }
        is CustomerScreen.VehicleDetails -> {
            VehicleDetailsScreen(
                vehicle = screen.vehicle,
                onBack = { viewModel.navigateTo(CustomerScreen.FleetBrowse) },
                onBookNow = { vehicle ->
                    viewModel.navigateTo(CustomerScreen.RentalBooking(vehicle))
                }
            )
        }
        is CustomerScreen.RentalBooking -> {
            RentalBookingScreen(
                vehicle = screen.vehicle,
                onBack = { viewModel.navigateTo(CustomerScreen.VehicleDetails(screen.vehicle)) },
                onConfirm = { days, vId, _, totalPrice, scheduledDate, tripNotes, stopsStr, isSelfDrive ->
                    scope.launch {
                        repository.bookRental(
                            customerId = sessionManager.getDriverId()?.toIntOrNull() ?: 1, 
                            vehicleId = vId, 
                            pickupLocation = viewModel.rentalPickupLocation.ifEmpty { "My Location" }, 
                            pickupLat = viewModel.rentalPickupLat ?: 0.0, 
                            pickupLng = viewModel.rentalPickupLng ?: 0.0, 
                            durationHours = days * 24, 
                            totalPrice = totalPrice, 
                            startTime = scheduledDate, 
                            tripNotes = tripNotes, 
                            stops = stopsStr,
                            isSelfDrive = isSelfDrive
                        )
                            .onSuccess { response ->
                                if (response.checkoutUrl != null) {
                                    viewModel.navigateTo(CustomerScreen.PaystackCheckout(response.checkoutUrl!!))
                                } else {
                                    val msg = if (scheduledDate != null) { "Rental scheduled for $scheduledDate\nCode: ${response.bookingCode}" } else { "Rental confirmed!\nCode: ${response.bookingCode}" }
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    viewModel.navigateTo(CustomerScreen.MainMap)
                                }
                            }.onFailure { Toast.makeText(context, "Booking failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                    }
                }
            )
        }
        is CustomerScreen.PaystackCheckout -> {
            PaystackWebViewScreen(
                url = screen.url,
                onBack = { viewModel.navigateTo(CustomerScreen.MainMap) },
                onSuccess = {
                    Toast.makeText(context, "Payment processing... please wait for confirmation.", Toast.LENGTH_SHORT).show()
                    viewModel.navigateTo(CustomerScreen.MainMap)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMapContent(
    viewModel: CustomerMapViewModel,
    onNavigateToChat: (Int, String) -> Unit,
    onNavigateToTrips: () -> Unit,
    onNavigateToRentals: () -> Unit,
    onNavigateToRideHistory: () -> Unit,
    onNavigateToPromos: () -> Unit,
    onNavigateToSupport: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val repository = remember { DriverRepository() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val focusManager = LocalFocusManager.current
    val pickupFocusRequester = remember { FocusRequester() }
    val dropOffFocusRequester = remember { FocusRequester() }

    val voiceCallHandler = remember { VoiceCallHandler { data -> viewModel.sendAudioData(data) } }

    var mapView by remember { mutableStateOf<MapView?>(null) }

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission = it }

    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasAudioPermission = it }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(hasLocationPermission, viewModel.pickupLat, viewModel.pickupLng) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        while (true) {
            if (hasLocationPermission) {
                val targetLat = viewModel.pickupLat
                val targetLng = viewModel.pickupLng
                
                if (targetLat != null && targetLng != null) {
                    viewModel.updateNearbyDrivers(targetLat, targetLng)
                } else {
                    @SuppressLint("MissingPermission")
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                        location?.let {
                            viewModel.updateNearbyDrivers(it.latitude, it.longitude)
                        }
                    }
                }
            }
            delay(5.seconds)
        }
    }

    var isPickupFocused by remember { mutableStateOf(false) }
    var isDropOffFocused by remember { mutableStateOf(false) }

    val pickupLatLng = remember(viewModel.pickupLat, viewModel.pickupLng) {
        if (viewModel.pickupLat != null && viewModel.pickupLng != null) GeoPoint(viewModel.pickupLat!!, viewModel.pickupLng!!) else null
    }
    val dropOffLatLng = remember(viewModel.dropOffLat, viewModel.dropOffLng) {
        if (viewModel.dropOffLat != null && viewModel.dropOffLng != null) GeoPoint(viewModel.dropOffLat!!, viewModel.dropOffLng!!) else null
    }

    BackHandler(enabled = true) {
        when {
            drawerState.isOpen -> { scope.launch { drawerState.close() } }
            viewModel.isSearchMode -> { viewModel.isSearchMode = false; focusManager.clearFocus() }
            viewModel.estimatedFare != null && viewModel.currentOrderId == null -> {
                viewModel.estimatedFare = null
                viewModel.polylinePoints = emptyList()
            }
            else -> viewModel.navigateTo(CustomerScreen.Landing)
        }
    }

    LaunchedEffect(viewModel.ongoingCall, hasAudioPermission) {
        if (viewModel.ongoingCall != null) {
            if (hasAudioPermission) {
                voiceCallHandler.startRecording()
                voiceCallHandler.startPlayback()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            voiceCallHandler.stopRecording()
            voiceCallHandler.stopPlayback()
        }
    }

    LaunchedEffect(Unit) {
        repository.events.collect { event ->
            if (event is FamekoEvent.AudioDataReceived) {
                voiceCallHandler.receiveAudio(event.data)
            } else if (event is FamekoEvent.NotificationReceived) {
                com.example.famekodriver.core.utils.NotificationHelper.showNotification(
                    context,
                    event.title,
                    event.message
                )
                Toast.makeText(context, "${event.title}: ${event.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                location?.let {
                    mapView?.controller?.setCenter(GeoPoint(it.latitude, it.longitude))
                    mapView?.controller?.setZoom(15.0)
                    viewModel.updateNearbyDrivers(it.latitude, it.longitude)
                }
            }
        }
    }

    fun useCurrentLocation(forPickup: Boolean) {
        if (!hasLocationPermission) { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION); return }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        viewModel.isLoading = true
        @SuppressLint("MissingPermission")
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
            location?.let { loc ->
                viewModel.useCurrentLocation(loc, forPickup)
            } ?: run { viewModel.isLoading = false }
        }.addOnFailureListener { viewModel.isLoading = false }
    }

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    val activeMarkers = remember { mutableMapOf<String, Marker>() }

    val currentSheetState = remember(viewModel.orderStatusData?.status, viewModel.estimatedFare, viewModel.currentOrderId, viewModel.isSearchMode, viewModel.pickupLocation, viewModel.dropOffLocation, viewModel.activeServiceMode, viewModel.pickupLat, viewModel.activeRental, viewModel.rentalPickupLat) {
        when {
            viewModel.orderStatusData?.status == "PENDING" -> CustomerSheetState.SEARCHING_FOR_DRIVER
            viewModel.orderStatusData?.status != null && viewModel.orderStatusData?.status != "CANCELLED" && viewModel.orderStatusData?.status != "DELIVERED" -> CustomerSheetState.ON_TRIP
            viewModel.activeRental != null && !viewModel.isSearchMode -> CustomerSheetState.ACTIVE_RENTAL
            (viewModel.activeServiceMode == ServiceType.RIDE_HAILING || viewModel.activeServiceMode == ServiceType.PACKAGE_DELIVERY) && viewModel.estimatedFare != null && viewModel.currentOrderId == null -> CustomerSheetState.SELECTING_SERVICE
            viewModel.isSearchMode || ((viewModel.activeServiceMode == ServiceType.RIDE_HAILING || viewModel.activeServiceMode == ServiceType.PACKAGE_DELIVERY) && (viewModel.pickupLocation.isNotEmpty() || viewModel.dropOffLocation.isNotEmpty())) || (viewModel.activeServiceMode == ServiceType.RENTAL && viewModel.rentalPickupLocation.isNotEmpty()) -> CustomerSheetState.PICKING_ADDRESS
            else -> CustomerSheetState.IDLE
        }
    }

    // Auto-Zoom to fit route
    LaunchedEffect(viewModel.polylinePoints) {
        if (viewModel.polylinePoints.isNotEmpty() && mapView != null) {
            val centerLat = viewModel.polylinePoints.map { it.latitude }.average()
            val centerLng = viewModel.polylinePoints.map { it.longitude }.average()
            mapView?.controller?.animateTo(GeoPoint(centerLat, centerLng))
            mapView?.controller?.setZoom(13.5)
        }
    }

    // Manage Background Location Service for Self-Drive rentals
    LaunchedEffect(viewModel.activeRental) {
        val rental = viewModel.activeRental
        val isSelfDrive = rental?.get("is_self_drive") == true || rental?.get("is_self_drive") == "true"
        val status = rental?.get("status")?.toString()?.uppercase() ?: ""
        val vehicleId = (rental?.get("vehicle_id") as? Double)?.toInt() ?: (rental?.get("vehicle_id") as? Int) ?: 0
        
        val shouldTrack = isSelfDrive && vehicleId != 0 && (status == "ACTIVE" || status == "IN_PROGRESS" || status == "ASSIGNED")

        if (shouldTrack) {
            val serviceIntent = Intent(context, CustomerLocationService::class.java).apply {
                action = CustomerLocationService.ACTION_START
                putExtra("vehicle_id", vehicleId)
            }
            context.startForegroundService(serviceIntent)
        } else {
            val serviceIntent = Intent(context, CustomerLocationService::class.java).apply {
                action = CustomerLocationService.ACTION_STOP
            }
            context.stopService(serviceIntent)
        }
    }

    // Real-time Pricing Refresh (Industry Standard)
    LaunchedEffect(currentSheetState, viewModel.polylinePoints) {
        if (currentSheetState == CustomerSheetState.SELECTING_SERVICE && viewModel.polylinePoints.isNotEmpty()) {
            while (true) {
                delay(30.seconds) // Refresh every 30 seconds
                viewModel.updateNearbyDrivers(viewModel.pickupLat ?: 0.0, viewModel.pickupLng ?: 0.0)
                // We should probably also refresh the route/fare here if we wanted to be super precise
            }
        }
    }

    LaunchedEffect(currentSheetState) {
        when (currentSheetState) {
            CustomerSheetState.SEARCHING_FOR_DRIVER,
            CustomerSheetState.SELECTING_SERVICE -> {
                if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                    sheetScaffoldState.bottomSheetState.expand()
                }
            }
            CustomerSheetState.PICKING_ADDRESS -> {
                if (viewModel.activeServiceMode == ServiceType.RENTAL) {
                    if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                        sheetScaffoldState.bottomSheetState.expand()
                    }
                    delay(300.milliseconds)
                    pickupFocusRequester.requestFocus()
                } else {
                    // Ride search is at the top, keep bottom sheet collapsed
                    if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.PartiallyExpanded) {
                        sheetScaffoldState.bottomSheetState.partialExpand()
                    }
                }
            }
            CustomerSheetState.IDLE, CustomerSheetState.ON_TRIP, CustomerSheetState.ACTIVE_RENTAL -> {
                if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.PartiallyExpanded) {
                    sheetScaffoldState.bottomSheetState.partialExpand()
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color.White,
                drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().background(FamekoBlue).padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 32.dp)) {
                    Column {
                        Surface(
                            shape = CircleShape,
                            modifier = Modifier.size(72.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                        ) {
                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.padding(16.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(sessionManager.getDriverName() ?: "Guest User", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("View Profile", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                DrawerItem("Active Trips", Icons.Default.DirectionsCar) { scope.launch { drawerState.close(); onNavigateToTrips() } }
                DrawerItem("My Rentals", Icons.Default.Key) { scope.launch { drawerState.close(); onNavigateToRentals() } }
                DrawerItem("Ride History", Icons.Default.History) { scope.launch { drawerState.close(); onNavigateToRideHistory() } }
                DrawerItem("Promotions", Icons.Default.LocalOffer) { scope.launch { drawerState.close(); onNavigateToPromos() } }
                DrawerItem("Support", Icons.AutoMirrored.Filled.Help) { scope.launch { drawerState.close(); onNavigateToSupport() } }
                DrawerItem("Settings", Icons.Default.Settings) { scope.launch { drawerState.close(); onNavigateToNotificationSettings() } }
                
                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = BoltLightGray)
                DrawerItem("Log Out", Icons.AutoMirrored.Filled.Logout, tint = Color.Red) { 
                    sessionManager.logout()
                    (context as? Activity)?.finish() 
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    ) {
        BottomSheetScaffold(
            scaffoldState = sheetScaffoldState,
            sheetPeekHeight = if (currentSheetState == CustomerSheetState.IDLE) 180.dp 
                             else if (viewModel.currentOrderId != null || viewModel.activeRental != null || currentSheetState == CustomerSheetState.SELECTING_SERVICE) 140.dp 
                             else 0.dp,
            sheetContainerColor = Color.White,
            sheetShadowElevation = 32.dp,
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetDragHandle = {
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(Color.LightGray.copy(alpha = 0.5f), CircleShape)
                )
            },
            sheetContent = {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding().imePadding()) {
                    when (currentSheetState) {
                        CustomerSheetState.IDLE -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(64.dp)
                                        .clickable { 
                                            viewModel.isSearchMode = true
                                            isDropOffFocused = true
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White,
                                    shadowElevation = 8.dp,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BoltLightGray)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 20.dp)
                                    ) {
                                        Icon(Icons.Default.Search, null, tint = FamekoBlue, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.width(16.dp))
                                        Text(
                                            text = when(viewModel.activeServiceMode) {
                                                ServiceType.RIDE_HAILING -> "Where to?"
                                                ServiceType.RENTAL -> "Set pickup for rental"
                                                ServiceType.PACKAGE_DELIVERY -> "Where are we delivering?"
                                                else -> "Where to?"
                                            },
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                
                                Spacer(Modifier.height(20.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    QuickShortcutItem("Home", Icons.Default.Home, BoltGreen) { 
                                        if (viewModel.savedPlaces.any { it.label.equals("Home", true) }) {
                                            viewModel.applyShortcut("Home")
                                        } else {
                                            Toast.makeText(context, "Set your Home address first", Toast.LENGTH_SHORT).show()
                                            viewModel.isSearchMode = true
                                            isDropOffFocused = true
                                            viewModel.selectedTab = 1
                                        }
                                    }
                                    QuickShortcutItem("Work", Icons.Default.Work, Color(0xFF3B82F6)) { 
                                        if (viewModel.savedPlaces.any { it.label.equals("Work", true) }) {
                                            viewModel.applyShortcut("Work")
                                        } else {
                                            Toast.makeText(context, "Set your Work address first", Toast.LENGTH_SHORT).show()
                                            viewModel.isSearchMode = true
                                            isDropOffFocused = true
                                            viewModel.selectedTab = 1
                                        }
                                    }
                                    QuickShortcutItem("Recent", Icons.Default.History, Color.Gray) { 
                                        viewModel.selectedTab = 0
                                        viewModel.isSearchMode = true
                                        isDropOffFocused = true
                                        viewModel.updateDropOffLocation("")
                                    }
                                    QuickShortcutItem("Saved", Icons.Default.Star, BoltYellow) { 
                                        viewModel.selectedTab = 1
                                        viewModel.isSearchMode = true
                                        isDropOffFocused = true
                                        viewModel.updateDropOffLocation("")
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        CustomerSheetState.PICKING_ADDRESS -> {
                            if (viewModel.activeServiceMode == ServiceType.RENTAL) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { viewModel.isSearchMode = false; focusManager.clearFocus() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = BoltDark) }
                                        Text("Set Pickup for Rental", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    SearchBox(
                                        pickupLocation = viewModel.rentalPickupLocation, 
                                        dropOffLocation = "",
                                        onPickupChange = { viewModel.updatePickupLocation(it) },
                                        onDropOffChange = { },
                                        onPickupFocus = { isPickupFocused = it }, 
                                        onDropOffFocus = { },
                                        onSearch = { focusManager.clearFocus() },
                                        isLoading = viewModel.isLoading, 
                                        pickupFocusRequester = pickupFocusRequester, 
                                        dropOffFocusRequester = dropOffFocusRequester, 
                                        isRentalMode = true
                                    )
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                        CustomerSheetState.SELECTING_SERVICE -> {
                            ServiceSelectionSheet(
                                estimates = viewModel.rideEstimates,
                                activeServiceMode = viewModel.activeServiceMode,
                                discountRate = viewModel.discountRate,
                                peakMultiplier = viewModel.pricingConfig?.peakMultiplier ?: 1.0,
                                selectedType = viewModel.selectedVehicleType,
                                onTypeSelected = { type, _ -> viewModel.setVehicleType(type) },
                                onConfirm = { viewModel.confirmOrder() }, 
                                isPlacing = viewModel.isOrderPlacing
                            )
                        }
                        CustomerSheetState.SEARCHING_FOR_DRIVER -> {
                            SearchingSheetContent(onCancel = { viewModel.cancelOrder() })
                        }
                        CustomerSheetState.ON_TRIP -> {
                            viewModel.orderStatusData?.let { data ->
                                viewModel.currentOrderId?.let { id ->
                                    DriverInfoSheetContent(
                                        data = data,
                                        orderId = id,
                                        onNavigateToChat = onNavigateToChat,
                                        onCancel = { viewModel.cancelOrder() },
                                        onInitiateCall = { viewModel.initiateCall() },
                                        onShareTrip = { viewModel.shareTrip(context) }
                                    )
                                }
                            }
                        }
                        CustomerSheetState.ACTIVE_RENTAL -> {
                            viewModel.activeRental?.let { rental ->
                                ActiveRentalSheetContent(
                                    rental = rental,
                                    onSearchClick = { viewModel.isSearchMode = true; isDropOffFocused = true },
                                    onNavigationClick = { lat, lng, address ->
                                        if (lat != null && lng != null && lat != 0.0) {
                                            val gmmIntentUri = "google.navigation:q=$lat,$lng".toUri()
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            try {
                                                context.startActivity(mapIntent)
                                            } catch (_: Exception) {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                                            }
                                        } else if (!address.isNullOrBlank() && address != "Not set") {
                                            val gmmIntentUri = "google.navigation:q=${Uri.encode(address)}".toUri()
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                            mapIntent.setPackage("com.google.android.apps.maps")
                                            context.startActivity(mapIntent)
                                        } else {
                                            Toast.makeText(context, "Please set a destination first", Toast.LENGTH_SHORT).show()
                                            viewModel.isSearchMode = true
                                            isDropOffFocused = true
                                        }
                                    },
                                    onCancel = {
                                        val id = (rental["id"] as? Double)?.toInt() ?: (rental["id"] as? Int) ?: 0
                                        viewModel.cancelRental(id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                            controller.setZoom(12.0)
                            controller.setCenter(GeoPoint(5.6037, -0.1870))
                            if (hasLocationPermission) {
                                val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                                locationOverlay.enableMyLocation()
                                overlays.add(locationOverlay)
                            }
                            mapView = this
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { mv ->
                        val currentStatus = viewModel.orderStatusData
                        val driversToShow = if (currentStatus?.status == "ASSIGNED" || currentStatus?.status == "IN_TRANSIT" || currentStatus?.status == "ARRIVED") {
                            val dLat = currentStatus.driverLat
                            val dLng = currentStatus.driverLng
                            if (dLat != null && dLng != null) {
                                listOf(DriverLocation(
                                    id = currentStatus.driverId ?: "0",
                                    latitude = dLat,
                                    longitude = dLng,
                                    bearing = currentStatus.driverBearing ?: 0f,
                                    vehicleType = currentStatus.driverVehicle
                                ))
                            } else emptyList()
                        } else {
                            viewModel.drivers
                        }

                        val currentDriverIds = driversToShow.map { it.id }.toSet()
                        activeMarkers.keys.retainAll { id ->
                            if (id !in currentDriverIds) {
                                mv.overlays.remove(activeMarkers[id])
                                false
                            } else true
                        }

                        // Update or add markers
                        driversToShow.forEach { driver ->
                            val id = driver.id
                            val marker = activeMarkers.getOrPut(id) {
                                Marker(mv).apply {
                                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                                    icon = AppCompatResources.getDrawable(context, R.drawable.ic_car)?.apply { setTint(BoltGreen.toArgb()) }
                                    mv.overlays.add(this)
                                }
                            }
                            
                            val startPos = marker.position
                            val endPos = GeoPoint(driver.latitude, driver.longitude)
                            
                            if (startPos == null || (startPos.latitude == 0.0 && startPos.longitude == 0.0)) {
                                marker.position = endPos
                            } else if (startPos.latitude != endPos.latitude || startPos.longitude != endPos.longitude) {
                                animateMarker(marker, mv, startPos, endPos)
                            }
                            marker.rotation = driver.bearing
                        }

                        // Handle polyline and other overlays
                        mv.overlays.removeAll { it is Polyline || (it is Marker && it !in activeMarkers.values) }
                        if (viewModel.polylinePoints.isNotEmpty()) {
                            val line = Polyline()
                            line.setPoints(viewModel.polylinePoints)
                            line.color = BoltGreen.toArgb()
                            line.width = 12f
                            mv.overlays.add(line)
                            
                            if (pickupLatLng != null) { 
                                val pMarker = Marker(mv)
                                pMarker.position = pickupLatLng
                                pMarker.title = "Pickup"
                                pMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mv.overlays.add(pMarker) 
                            }
                            if (dropOffLatLng != null) { 
                                val dMarker = Marker(mv)
                                dMarker.position = dropOffLatLng
                                dMarker.title = "Drop-off"
                                dMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                mv.overlays.add(dMarker) 
                            }
                        }
                        mv.invalidate()
                    }
                )

                // Persistent Floating Menu Button (Top Start)
                FloatingActionButton(
                    onClick = { scope.launch { drawerState.open() } },
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp)
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .size(48.dp),
                    containerColor = Color.White,
                    contentColor = BoltDark,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(Icons.Default.Menu, null, modifier = Modifier.size(24.dp))
                }

                // Immersive Floating Search Bar & Controls
                if (viewModel.currentOrderId == null && viewModel.activeRental == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .statusBarsPadding()
                    ) {
                        // Floating Search Experience
                        if (currentSheetState == CustomerSheetState.PICKING_ADDRESS) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Column {
                                    SearchBox(
                                        pickupLocation = if (viewModel.activeServiceMode == ServiceType.RENTAL) viewModel.rentalPickupLocation else viewModel.pickupLocation,
                                        dropOffLocation = viewModel.dropOffLocation,
                                        stops = viewModel.stops,
                                        onPickupChange = { viewModel.updatePickupLocation(it) },
                                        onDropOffChange = { viewModel.updateDropOffLocation(it) },
                                        onStopChange = { index, value -> viewModel.stops = viewModel.stops.toMutableList().apply { set(index, value) } },
                                        onAddStop = { if (viewModel.stops.size < 5) viewModel.stops += "" },
                                        onRemoveStop = { index -> viewModel.stops = viewModel.stops.toMutableList().apply { removeAt(index) } },
                                        onPickupFocus = { isPickupFocused = it }, 
                                        onDropOffFocus = { 
                                            isDropOffFocused = it
                                            if (it && viewModel.dropOffLocation.isEmpty()) {
                                                viewModel.updateDropOffLocation("")
                                            }
                                        },
                                        onSearch = { 
                                            if (viewModel.activeServiceMode == ServiceType.RENTAL) {
                                                focusManager.clearFocus()
                                            } else {
                                                viewModel.calculateRoute()
                                                focusManager.clearFocus()
                                            }
                                        },
                                        isLoading = viewModel.isLoading, pickupFocusRequester = pickupFocusRequester, dropOffFocusRequester = dropOffFocusRequester, isRentalMode = viewModel.activeServiceMode == ServiceType.RENTAL
                                    )
                                    
                                    // Floating Suggestions Overlay
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = Color.White,
                                        shadowElevation = 16.dp
                                    ) {
                                        Column {
                                            if (isDropOffFocused && viewModel.dropOffLocation.isEmpty()) {
                                                TabRow(
                                                    selectedTabIndex = viewModel.selectedTab,
                                                    containerColor = Color.Transparent,
                                                    contentColor = FamekoBlue,
                                                    indicator = { tabPositions ->
                                                        TabRowDefaults.SecondaryIndicator(
                                                            Modifier.tabIndicatorOffset(tabPositions[viewModel.selectedTab]),
                                                            color = FamekoBlue
                                                        )
                                                    },
                                                    divider = {}
                                                ) {
                                                    Tab(
                                                        selected = viewModel.selectedTab == 0,
                                                        onClick = { viewModel.selectedTab = 0; viewModel.updateDropOffLocation("") },
                                                        text = { Text("Recent", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                                                    )
                                                    Tab(
                                                        selected = viewModel.selectedTab == 1,
                                                        onClick = { viewModel.selectedTab = 1; viewModel.updateDropOffLocation("") },
                                                        text = { Text("Saved", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                                                    )
                                                }
                                            }

                                            LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                                                if (viewModel.dropOffLocation.isEmpty()) {
                                                    item {
                                                        ListItem(
                                                            headlineContent = { Text("Use current location", fontWeight = FontWeight.Bold) },
                                                            leadingContent = { Icon(Icons.Default.MyLocation, null, tint = BoltGreen) },
                                                            modifier = Modifier.clickable { useCurrentLocation(isPickupFocused) }
                                                        )
                                                    }
                                                }

                                                val suggestions = if (isPickupFocused) viewModel.pickupSuggestions else viewModel.dropOffSuggestions
                                                items(suggestions) { suggestion ->
                                                    val icon = when (suggestion.type) {
                                                        "saved" -> Icons.Default.Star
                                                        "recent" -> Icons.Default.History
                                                        else -> Icons.Default.LocationOn
                                                    }
                                                    val tint = when (suggestion.type) {
                                                        "saved" -> BoltYellow
                                                        "recent" -> Color.Gray
                                                        else -> Color.LightGray
                                                    }
                                                    ListItem(
                                                        headlineContent = { Text(suggestion.name ?: suggestion.displayName, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                        supportingContent = { Text(suggestion.displayName, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                                        leadingContent = { Icon(icon, null, tint = tint) },
                                                        modifier = Modifier.clickable {
                                                            viewModel.selectSuggestion(suggestion, isPickupFocused)
                                                            focusManager.clearFocus()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Map Utility Buttons (Recenter, Zoom)
                if (currentSheetState == CustomerSheetState.IDLE) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 200.dp, end = 16.dp), // Pushed up above the bottom sheet
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { 
                                hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasLocationPermission) {
                                    LocationServices.getFusedLocationProviderClient(context).lastLocation.addOnSuccessListener { loc ->
                                        loc?.let { mapView?.controller?.animateTo(GeoPoint(it.latitude, it.longitude)) }
                                    }
                                }
                            },
                            containerColor = Color.White,
                            contentColor = BoltDark,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
                
                if (currentSheetState == CustomerSheetState.SEARCHING_FOR_DRIVER) { RadarPulseAnimation() }
                
                viewModel.incomingCall?.let { call ->
                    AlertDialog(onDismissRequest = { }, title = { Text("Incoming Call") }, text = { Text("Driver ${call.callerName} is calling you...") }, confirmButton = { Button(onClick = { viewModel.acceptCall(call.callId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(8.dp)); Text("Accept") } }, dismissButton = { OutlinedButton(onClick = { viewModel.rejectCall(call.callId) }) { Text("Reject", color = Color.Red) } }, shape = RoundedCornerShape(24.dp))
                }
                viewModel.ongoingCall?.let { call -> CallOverlay(call = call, onEnd = { viewModel.endCall(call.callId) }) }

                if (viewModel.showRatingDialog) {
                    RatingDialog(
                        driverName = viewModel.ratingDriverName ?: "Your Driver",
                        onRate = { rating, comment -> viewModel.submitRating(rating, comment) },
                        onDismiss = { viewModel.showRatingDialog = false }
                    )
                }

                if (viewModel.showTripSummary) {
                    TripSummaryDialog(
                        fare = viewModel.finalFare,
                        onRate = {
                            viewModel.showTripSummary = false
                            viewModel.showRatingDialog = true
                        },
                        onDismiss = { viewModel.showTripSummary = false }
                    )
                }
            }
        }
    }
}

@Composable
fun TripSummaryDialog(
    fare: Double,
    onRate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trip Completed", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, null, tint = BoltGreen, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Total Fare", fontSize = 16.sp, color = Color.Gray)
                Text("₵${fare.toInt()}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = BoltDark)
                Spacer(Modifier.height(8.dp))
                Text("Payment completed via cash/wallet", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onRate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Rate Driver", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}


@Composable
fun RatingDialog(
    driverName: String,
    onRate: (Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = BoltLightGray, modifier = Modifier.size(80.dp)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                }
                Spacer(Modifier.height(16.dp))
                Text("Rate your trip", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("How was your ride with $driverName?", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..5) {
                        val isSelected = rating >= i
                        IconButton(onClick = { rating = i.toFloat() }) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isSelected) BoltYellow else Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Write a comment (optional)", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRate(rating, comment) },
                enabled = rating > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
            ) {
                Text("Submit Rating", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Maybe Later", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}


fun animateMarker(marker: Marker, mv: MapView, startPos: GeoPoint, endPos: GeoPoint, duration: Long = 1500) {
    val handler = Handler(Looper.getMainLooper())
    val start = SystemClock.uptimeMillis()
    val interpolator = LinearInterpolator()

    handler.post(object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - start
            val t = kotlin.math.min(1f, interpolator.getInterpolation(elapsed.toFloat() / duration))

            val lat = t * endPos.latitude + (1 - t) * startPos.latitude
            val lng = t * endPos.longitude + (1 - t) * startPos.longitude

            marker.position = GeoPoint(lat, lng)
            mv.invalidate()

            if (t < 1f) {
                handler.postDelayed(this, 16)
            }
        }
    })
}

@Composable
fun RadarPulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(800.dp)) {
            drawCircle(
                color = BoltGreen,
                radius = radius.dp.toPx(),
                alpha = alpha
            )
        }
        
        // Inner core
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = BoltGreen, modifier = Modifier.size(16.dp)) {}
            }
        }
    }
}


@Composable
fun ActiveRentalSheetContent(
    rental: Map<String, Any>,
    onSearchClick: () -> Unit,
    onNavigationClick: (Double?, Double?, String?) -> Unit,
    onCancel: () -> Unit
) {
    val isUnlocked = rental["is_unlocked"] == true
    val isSelfDrive = rental["is_self_drive"] == true || rental["is_self_drive"] == "true"
    val currentDest = rental["destination_location"]?.toString() ?: "Not set"
    val bookingCode = rental["booking_code"]?.toString() ?: "----"

    val destLat = rental["destination_lat"] as? Double
    val destLng = rental["destination_lng"] as? Double
    val pickupLat = rental["pickup_lat"] as? Double
    val pickupLng = rental["pickup_lng"] as? Double

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isSelfDrive) "Self-Drive Active" else "Active Rental", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Color(0xFF004E89))
            Spacer(Modifier.weight(1f))
            Surface(
                color = if (isUnlocked) BoltGreen.copy(alpha = 0.1f) else Color(0xFFFFF9DB),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = if (isUnlocked) "ON TRIP" else "PENDING PICKUP",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUnlocked) BoltGreen else Color(0xFFF08C00)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BoltLightGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isSelfDrive) {
                    Text("VEHICLE ACCESS CODE", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(bookingCode, fontSize = 28.sp, fontWeight = FontWeight.Black, color = BoltDark, letterSpacing = 2.sp)
                    Text("Use this to unlock the vehicle or keybox", fontSize = 12.sp, color = Color.Gray)
                } else {
                    Text("HANDSHAKE CODE", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(bookingCode, fontSize = 28.sp, fontWeight = FontWeight.Black, color = BoltDark, letterSpacing = 2.sp)
                    Text("Give this to your driver to start the trip", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text(if (isSelfDrive) "Navigate to" else "Current Stop", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchClick() }
                .background(BoltLightGray, RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, null, tint = BoltGreen)
            Spacer(Modifier.width(8.dp))
            Text(currentDest, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.Navigation, null, tint = Color.Gray)
        }

        Spacer(Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isUnlocked) "End Rental" else "Cancel Rental")
            }
            
            Button(
                onClick = {
                    if (isSelfDrive) {
                        if (isUnlocked) {
                            onNavigationClick(destLat, destLng, currentDest)
                        } else {
                            onNavigationClick(pickupLat, pickupLng, rental["pickup_location"]?.toString())
                        }
                    } else {
                        onSearchClick()
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = if (isSelfDrive) BoltGreen else BoltGreen),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isSelfDrive) "Navigation" else "Change Stop")
            }
        }
    }
}




@Composable
fun SearchingSheetContent(onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) { 
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = BoltGreen)
        Spacer(Modifier.height(24.dp))
        Text("Finding your driver...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text("Connecting you to the nearest available Fameko", color = Color.Gray, fontSize = 14.sp)
        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { 
            Text("Cancel Request", color = Color.Red, fontWeight = FontWeight.Bold) 
        } 
    }
}

@Composable
fun DriverInfoSheetContent(
    data: OrderStatusResponse, 
    orderId: Int, 
    onNavigateToChat: (Int, String) -> Unit, 
    onCancel: () -> Unit, 
    onInitiateCall: () -> Unit,
    onShareTrip: () -> Unit
) {
    val context = LocalContext.current
    val pin = data.verificationPin
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = BoltLightGray,
                    modifier = Modifier.size(68.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    if (!data.driverProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = data.driverProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(16.dp), tint = Color.Gray)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(24.dp).offset(x = 4.dp, y = 4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = BoltYellow, modifier = Modifier.padding(4.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(data.driverName ?: "Your Driver", style = MaterialTheme.typography.titleLarge, color = BoltDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${data.driverRating ?: 4.9}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BoltDark)
                    Text(" • ", color = Color.Gray)
                    Text(data.driverVehicle ?: "Fameko Economy", fontSize = 14.sp, color = Color.Gray)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onInitiateCall,
                    modifier = Modifier.size(44.dp).background(FamekoLightBlue, CircleShape)
                ) {
                    Icon(Icons.Default.Call, null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { onNavigateToChat(orderId, data.driverName ?: "Driver") },
                    modifier = Modifier.size(44.dp).background(FamekoLightBlue, CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Surface(
            color = BoltLightGray,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = FamekoBlue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = data.driverVehicleModel ?: "Vehicle Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BoltDark
                    )
                    Text(
                        text = data.driverVehicleNumber ?: "PLATE NUMBER",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                if (data.status == "ASSIGNED" && pin != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PIN", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(pin, fontSize = 20.sp, fontWeight = FontWeight.Black, color = FamekoBlue, letterSpacing = 2.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("Cancel", color = BoltDark, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShareTrip,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoltDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Trip", fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Surface(
            onClick = { context.startActivity(Intent(Intent.ACTION_DIAL, "tel:911".toUri())) },
            color = Color(0xFFFFF1F0),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, null, tint = Color(0xFFCF1322), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Emergency Assistance", color = Color(0xFFCF1322), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}


@Composable
fun CallOverlay(call: FamekoEvent.IncomingCall, onEnd: () -> Unit) {
    var callDuration by remember { mutableIntStateOf(0) }
    val isConnecting = call.callId == "pending"
    LaunchedEffect(isConnecting) { if (!isConnecting) { while (true) { delay(1.seconds); callDuration++ } } }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(64.dp))
            Surface(shape = CircleShape, color = BoltGreen.copy(alpha = 0.2f), modifier = Modifier.size(140.dp)) { 
                Box(contentAlignment = Alignment.Center) { 
                    if (!isConnecting) { 
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "")
                        Surface(shape = CircleShape, color = BoltGreen.copy(alpha = 0.1f), modifier = Modifier.size(140.dp * scale)) {} 
                    }
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(80.dp)) 
                } 
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(call.callerName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (isConnecting) "Connecting..." else { val mins = callDuration / 60; val secs = callDuration % 60; "In-app Call • %02d:%02d".format(Locale.US, mins, secs) }, color = if (isConnecting) Color.Gray else BoltGreen, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                    IconButton(onClick = { }, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.MicOff, null, tint = Color.White) }
                    Text("Mute", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) 
                }
                FloatingActionButton(onClick = onEnd, containerColor = Color(0xFFDC3545), contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(80.dp)) { Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(36.dp)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                    IconButton(onClick = { }, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White) }
                    Text("Speaker", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) 
                } 
            }
        }
    }
}


@Composable
fun SearchBox(
    pickupLocation: String, 
    dropOffLocation: String, 
    stops: List<String> = emptyList(),
    onPickupChange: (String) -> Unit, 
    onDropOffChange: (String) -> Unit, 
    onStopChange: (Int, String) -> Unit = { _, _ -> },
    onAddStop: () -> Unit = {},
    onRemoveStop: (Int) -> Unit = {},
    onPickupFocus: (Boolean) -> Unit, 
    onDropOffFocus: (Boolean) -> Unit, 
    onStopFocus: (Int, Boolean) -> Unit = { _, _ -> },
    onSearch: () -> Unit, 
    isLoading: Boolean, 
    pickupFocusRequester: FocusRequester, 
    dropOffFocusRequester: FocusRequester, 
    isRentalMode: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, BoltLightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pickup
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BoltGreen))
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = pickupLocation, 
                    onValueChange = onPickupChange, 
                    modifier = Modifier.weight(1f).focusRequester(pickupFocusRequester).onFocusChanged { onPickupFocus(it.isFocused) }, 
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BoltDark),
                    decorationBox = { innerTextField -> 
                        if (pickupLocation.isEmpty()) Text(if (isRentalMode) "Pickup for rental" else "Pickup location", color = Color.Gray, fontSize = 16.sp)
                        innerTextField() 
                    }
                )
                if (pickupLocation.isNotEmpty()) {
                    IconButton(onClick = { onPickupChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
                if (isRentalMode && pickupLocation.isNotEmpty() && dropOffLocation.isEmpty() && stops.isEmpty()) { 
                    IconButton(onClick = onSearch, modifier = Modifier.size(36.dp).background(FamekoBlue, CircleShape)) { 
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) 
                        else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp)) 
                    } 
                } 
            }
            
            // Stops
            stops.forEachIndexed { index, stop ->
                Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                    Box(modifier = Modifier.width(10.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Gray))
                    Spacer(modifier = Modifier.width(16.dp))
                    BasicTextField(
                        value = stop,
                        onValueChange = { onStopChange(index, it) },
                        modifier = Modifier.weight(1f).onFocusChanged { onStopFocus(index, it.isFocused) },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = BoltDark),
                        decorationBox = { innerTextField -> 
                            if (stop.isEmpty()) Text("Stop ${index + 1}", color = Color.Gray, fontSize = 16.sp)
                            innerTextField() 
                        }
                    )
                    IconButton(onClick = { onRemoveStop(index) }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color.Gray) }
                }
            }

            // Connection line
            Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                Box(modifier = Modifier.width(10.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
                }
            }

            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Box(modifier = Modifier.size(10.dp).background(BoltOrange, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = dropOffLocation, 
                    onValueChange = onDropOffChange, 
                    modifier = Modifier.weight(1f).focusRequester(dropOffFocusRequester).onFocusChanged { onDropOffFocus(it.isFocused) }, 
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BoltDark),
                    decorationBox = { innerTextField -> 
                        if (dropOffLocation.isEmpty()) Text("Where to?", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        innerTextField() 
                    }
                )
                
                if (dropOffLocation.isNotEmpty()) {
                    IconButton(onClick = { onDropOffChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                if (isRentalMode) {
                    IconButton(onClick = onAddStop) { Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp), tint = FamekoBlue) }
                }

                if (pickupLocation.isNotEmpty() && (dropOffLocation.isNotEmpty() || (isRentalMode && stops.isNotEmpty()))) { 
                    IconButton(onClick = onSearch, modifier = Modifier.size(36.dp).background(FamekoBlue, CircleShape)) { 
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) 
                        else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    } 
                } 
            }
        }
    }
}


@Composable
fun ServiceSelectionSheet(
    estimates: List<RideEstimateResponse>,
    activeServiceMode: ServiceType,
    discountRate: Int,
    peakMultiplier: Double,
    selectedType: String,
    onTypeSelected: (String, Double) -> Unit,
    onConfirm: () -> Unit,
    isPlacing: Boolean
) {
    val filteredEstimates = estimates.filter { estimate ->
        when (activeServiceMode) {
            ServiceType.RIDE_HAILING -> {
                estimate.serviceId in listOf("Economy", "Comfort", "Pragya")
            }
            ServiceType.PACKAGE_DELIVERY -> {
                estimate.serviceId in listOf("Okada", "Bicycle", "bicycle", "Aboboyaa", "Truck")
            }
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (peakMultiplier > 1.0) {
            Surface(
                color = BoltOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = BoltOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Demand is high. Prices are slightly higher.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BoltOrange)
                }
            }
        }
        
        val title = if (activeServiceMode == ServiceType.PACKAGE_DELIVERY) "Choose delivery type" else "Choose a ride"
        Text(title, style = MaterialTheme.typography.titleLarge, color = BoltDark, fontWeight = FontWeight.ExtraBold)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.heightIn(max = 450.dp)
        ) {
            items(filteredEstimates) { estimate ->
                ServiceItem(
                    estimate = estimate,
                    isSelected = selectedType == estimate.serviceId,
                    onSelect = { onTypeSelected(estimate.serviceId, estimate.fare) },
                    discountRate = discountRate
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
            enabled = !isPlacing && filteredEstimates.isNotEmpty()
        ) {
            if (isPlacing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(if (activeServiceMode == ServiceType.PACKAGE_DELIVERY) "Confirm Delivery" else "Confirm $selectedType", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
            }
        }
    }
}



@Composable
fun ServiceItem(
    estimate: RideEstimateResponse,
    isSelected: Boolean,
    onSelect: () -> Unit,
    discountRate: Int
) {
    val backgroundColor = if (isSelected) FamekoBlue.copy(alpha = 0.05f) else Color.White
    
    // Multipliers are already applied by the backend, so we just use the raw fare.
    // However, if there's a discount, we apply it here.
    val finalFare = (estimate.fare * (100 - discountRate) / 100).toInt()
    val originalFare = estimate.fare.toInt()

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(2.dp, FamekoBlue) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                val iconUrl = when(estimate.icon.lowercase()) {
                    "car" -> ImageLinks.IC_CAR_SALOON
                    "comfort" -> ImageLinks.IC_CAR_SALOON
                    "pragya" -> ImageLinks.IC_PRAGYA
                    "okada" -> ImageLinks.IC_OKADA
                    "aboboyaa" -> ImageLinks.IC_ABOBOYAA
                    "truck" -> ImageLinks.IC_TRUCK
                    "bicycle" -> ImageLinks.IC_BICYCLE
                    else -> ImageLinks.IC_CAR_SALOON
                }
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(estimate.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BoltDark)
                    Spacer(Modifier.width(8.dp))
                    if (estimate.pickupEtaMin <= 5) {
                        Surface(
                            color = BoltGreen.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("FAST", color = BoltGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                    if (discountRate > 0) {
                        Spacer(Modifier.width(4.dp))
                        Surface(
                            color = BoltOrange.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text("SAVE $discountRate%", color = BoltOrange, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(estimate.description, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (discountRate > 0) {
                        Text(
                            text = "₵$originalFare",
                            style = androidx.compose.ui.text.TextStyle(
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                            ),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text("₵$finalFare", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = BoltDark)
                }
                Text("${estimate.pickupEtaMin} min", fontSize = 12.sp, color = if (estimate.pickupEtaMin < 5) BoltGreen else Color.Gray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    var trips by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    fun fetchTrips() {
        isLoading = true
        val customerId = sessionManager.getDriverId() ?: "1"
        scope.launch {
            repository.getCustomerTrips(customerId)
                .onSuccess {
                    trips = it
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }
    
    LaunchedEffect(Unit) { fetchTrips() }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Active Trips", fontWeight = FontWeight.Bold) }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (trips.isNotEmpty()) {
                        IconButton(onClick = {
                            val customerId = sessionManager.getDriverId() ?: "1"
                            scope.launch {
                                repository.clearAllTrips(customerId).onSuccess {
                                    Toast.makeText(context, "Trips cleared", Toast.LENGTH_SHORT).show()
                                    fetchTrips()
                                }
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, "Clear All", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            ) 
        }, 
        containerColor = Color.White
    ) { padding ->
        if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BoltGreen) } }
        else if (trips.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.LightGray); Spacer(Modifier.height(16.dp)); Text("No trips yet", color = Color.Gray) } } }
        else { 
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                items(trips) { trip -> 
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BoltLightGray), shape = RoundedCornerShape(12.dp)) { 
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                                Text(trip["date"]?.toString()?.split(" ")?.get(0) ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("GHS ${trip["amount"].toString().toDoubleOrNull()?.toInt() ?: 0}", fontWeight = FontWeight.ExtraBold, color = BoltGreen)
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        val tripId = (trip["id"] as? Double)?.toInt() ?: (trip["id"] as? Int) ?: 0
                                        scope.launch {
                                            repository.deleteTrip(tripId).onSuccess {
                                                Toast.makeText(context, "Trip deleted", Toast.LENGTH_SHORT).show()
                                                fetchTrips()
                                            }
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BoltGreen)); Spacer(modifier = Modifier.width(12.dp)); Text(trip["pickup"].toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp) }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).background(Color.Red)); Spacer(modifier = Modifier.width(12.dp)); Text(trip["dropoff"].toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp) }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Driver: ${trip["driver"] ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(color = BoltGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) { Text(trip["status"].toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = BoltGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) } 
                            } 
                        } 
                    } 
                } 
            } 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    var trips by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    fun fetchTrips() {
        isLoading = true
        val customerId = sessionManager.getDriverId() ?: "1"
        scope.launch {
            repository.getCustomerTrips(customerId)
                .onSuccess {
                    trips = it
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }
    
    LaunchedEffect(Unit) { fetchTrips() }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text("Ride History", fontWeight = FontWeight.Bold) }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (trips.isNotEmpty()) {
                        IconButton(onClick = {
                            val customerId = sessionManager.getDriverId() ?: "1"
                            scope.launch {
                                repository.clearAllTrips(customerId).onSuccess {
                                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                                    fetchTrips()
                                }
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, "Clear History", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            ) 
        }, 
        containerColor = Color.White
    ) { padding ->
        if (isLoading) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = BoltGreen) } }
        else if (trips.isEmpty()) { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.LightGray); Spacer(Modifier.height(16.dp)); Text("No ride history yet", color = Color.Gray) } } }
        else { 
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { 
                items(trips) { trip -> 
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BoltLightGray), shape = RoundedCornerShape(12.dp)) { 
                        Column(modifier = Modifier.padding(16.dp)) { 
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                                Text(trip["date"]?.toString()?.split(" ")?.get(0) ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("GHS ${trip["amount"].toString().toDoubleOrNull()?.toInt() ?: 0}", fontWeight = FontWeight.ExtraBold, color = BoltGreen)
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        val tripId = (trip["id"] as? Double)?.toInt() ?: (trip["id"] as? Int) ?: 0
                                        scope.launch {
                                            repository.deleteTrip(tripId).onSuccess {
                                                Toast.makeText(context, "History entry deleted", Toast.LENGTH_SHORT).show()
                                                fetchTrips()
                                            }
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BoltGreen)); Spacer(modifier = Modifier.width(12.dp)); Text(trip["pickup"].toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp) }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(8.dp).background(Color.Red)); Spacer(modifier = Modifier.width(12.dp)); Text(trip["dropoff"].toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp) }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Driver: ${trip["driver"] ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(color = BoltGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) { Text(trip["status"].toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = BoltGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) } 
                            } 
                        } 
                    } 
                } 
            } 
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(onBack: () -> Unit) {
    val repository = remember { DriverRepository() }
    var promos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        repository.getPromotions().onSuccess {
            promos = it
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Promotions", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BoltGreen)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    TextField(
                        value = "",
                        onValueChange = {},
                        placeholder = { Text("Enter promo code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BoltLightGray,
                            unfocusedContainerColor = BoltLightGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        trailingIcon = {
                            Text(
                                "Apply",
                                color = BoltGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 12.dp).clickable { }
                            )
                        }
                    )
                }
                items(promos) { promo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BoltGreen.copy(alpha = 0.1f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BoltGreen.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(promo["title"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(promo["description"].toString(), fontSize = 12.sp, color = Color.DarkGray)
                                Spacer(Modifier.height(8.dp))
                                Text("Expires: ${promo["expiry"]}", fontSize = 10.sp, color = Color.Gray)
                            }
                            Box(Modifier.background(BoltGreen, RoundedCornerShape(8.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                                Text(promo["code"].toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun QuickShortcutItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = BoltLightGray,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
    }
}

@Composable
fun DrawerItem(label: String, icon: ImageVector, tint: Color = BoltDark, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint.copy(alpha = 0.7f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    var tickets by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    LaunchedEffect(Unit) { val customerId = sessionManager.getDriverId() ?: "1"; repository.getSupportTickets(customerId).onSuccess { tickets = it } }

    Scaffold(topBar = { TopAppBar(title = { Text("Support", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }, containerColor = Color.White) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("How can we help?", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.height(16.dp))
                val categories = listOf("Trip issues" to Icons.Default.DirectionsCar, "Payment & Pricing" to Icons.Default.CreditCard, "Account & Profile" to Icons.Default.Person, "General questions" to Icons.AutoMirrored.Filled.Help)
                categories.forEach { (title, icon) -> Row(modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = BoltGreen); Spacer(Modifier.width(16.dp)); Text(title, modifier = Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray) }; HorizontalDivider(color = BoltLightGray) }
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().background(BoltLightGray).padding(16.dp)) { Text("Recent Tickets", fontWeight = FontWeight.Bold, color = Color.Gray) }
            LazyColumn(Modifier.fillMaxSize()) {
                items(tickets) { ticket -> ListItem(headlineContent = { Text(ticket["subject"].toString(), fontWeight = FontWeight.Bold) }, supportingContent = { Text(ticket["date"].toString(), fontSize = 12.sp) }, trailingContent = { val color = if (ticket["status"] == "OPEN") Color.Blue else Color.Gray; Text(ticket["status"].toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp) }, modifier = Modifier.clickable { }); HorizontalDivider(color = BoltLightGray, modifier = Modifier.padding(horizontal = 16.dp)) }
                item { Spacer(Modifier.height(24.dp)); Button(onClick = { }, modifier = Modifier.fillMaxWidth().padding(16.dp), colors = ButtonDefaults.buttonColors(containerColor = BoltGreen)) { Text("Chat with Support", fontWeight = FontWeight.Bold) } }
            }
        }
    }
}

