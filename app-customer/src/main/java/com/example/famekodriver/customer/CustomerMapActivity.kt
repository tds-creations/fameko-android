// Fameko Customer Map Activity - Modularized and Cleaned
package com.example.famekodriver.customer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.*
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.network.NetworkClient
import com.example.famekodriver.core.utils.ImageLinks
import com.example.famekodriver.core.utils.NotificationHelper
import com.example.famekodriver.core.utils.VoiceCallHandler
import com.example.famekodriver.customer.ui.components.*
import com.example.famekodriver.customer.ui.modes.*
import com.example.famekodriver.customer.ui.screens.*
import com.example.famekodriver.customer.ui.theme.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds

class CustomerMapViewModelFactory(
    private val repository: DriverRepository,
    private val orderRepository: OrderRepository,
    private val rentalRepository: RentalRepository,
    private val userRepository: UserRepository,
    private val sessionManager: SessionManager,
    private val savedPlaceRepository: SavedPlaceRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CustomerMapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CustomerMapViewModel(repository, orderRepository, rentalRepository, userRepository, sessionManager, savedPlaceRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

enum class CustomerSheetState {
    LANDING,
    IDLE,
    PICKING_ADDRESS,
    SELECTING_SERVICE,
    SEARCHING_FOR_DRIVER,
    ON_TRIP,
    ACTIVE_RENTAL,
    RIDE_SCHEDULED,
    TIMED_OUT
}

sealed class CustomerScreen {
    object Landing : CustomerScreen()
    object MainMap : CustomerScreen()
    object Account : CustomerScreen()
    data class Chat(val orderId: Int, val driverName: String) : CustomerScreen()
    object Trips : CustomerScreen()
    object RideHistory : CustomerScreen()
    object Rentals : CustomerScreen()
    object FleetBrowse : CustomerScreen()
    data class VehicleDetails(val vehicle: Map<String, Any>) : CustomerScreen()
    data class RentalBooking(val vehicle: Map<String, Any>) : CustomerScreen()
    data class RentalDetails(val rental: Map<String, Any>) : CustomerScreen()
    object Promotions : CustomerScreen()
    object Support : CustomerScreen()
    object Notifications : CustomerScreen()
    object NotificationSettings : CustomerScreen()
    data class PaystackCheckout(val url: String) : CustomerScreen()
    object RouteSelection : CustomerScreen()
    object Profile : CustomerScreen()
    object Payment : CustomerScreen()
    object Safety : CustomerScreen()
    object ManagePlaces : CustomerScreen()
    data class SaveLocationSearch(val label: String, val placeId: String? = null) : CustomerScreen()
    object FamilyProfile : CustomerScreen()
    object WorkProfile : CustomerScreen()
    object TermsAndConditions : CustomerScreen()
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
                val repository = DriverRepository.getInstance()
                lifecycleScope.launch {
                    repository.updateFcmToken(customerId, token, "customer")
                }
            }
        }
    }
}


@Composable
fun FamekoTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = FamekoBlue,
        onPrimary = Color.White,
        secondary = FamekoGold,
        onSecondary = Color.White,
        surface = Color.White,
        onSurface = BoltDark,
        background = Color.White,
        onBackground = BoltDark,
        error = BoltOrange
    )
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            @Suppress("DEPRECATION")
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            headlineMedium = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp
            ),
            titleLarge = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                letterSpacing = 0.sp
            ),
            bodyLarge = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                letterSpacing = 0.5.sp
            )
        ),
        content = content
    )
}

@Composable
fun CustomerMapScreen() {
    val context = LocalContext.current
    val repository = remember { DriverRepository.getInstance() }
    val orderRepository = remember { OrderRepository() }
    val rentalRepository = remember { RentalRepository() }
    val userRepository = remember { UserRepository() }
    val sessionManager = remember { SessionManager(context) }
    val savedPlaceRepository = remember { SavedPlaceRepository(context) }
    
    val mapViewModel: CustomerMapViewModel = viewModel(
        modelClass = CustomerMapViewModel::class.java,
        factory = CustomerMapViewModelFactory(repository, orderRepository, rentalRepository, userRepository, sessionManager, savedPlaceRepository)
    )

    val mapView = remember { MapView(context) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    
    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        
        when (lifecycle.currentState) {
            Lifecycle.State.STARTED -> mapView.onStart()
            Lifecycle.State.RESUMED -> {
                mapView.onStart()
                mapView.onResume()
            }
            else -> {}
        }

        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(mapViewModel.notifications) {
        val lastNotif = mapViewModel.notifications.firstOrNull()
        if (lastNotif != null && lastNotif.id != mapViewModel.lastTriggeredNotificationId) {
            mapViewModel.lastTriggeredNotificationId = lastNotif.id
            NotificationHelper.showNotification(
                context,
                lastNotif.title,
                lastNotif.message
            )
        }
    }

    val scope = rememberCoroutineScope()
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Physical Back Button Logic
    BackHandler(enabled = true) {
        when (mapViewModel.currentScreen) {
            CustomerScreen.Landing -> {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastBackPressTime < 2000) {
                    (context as? Activity)?.finish()
                } else {
                    lastBackPressTime = currentTime
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
            is CustomerScreen.MainMap -> {
                when {
                    mapViewModel.isFullscreenMap -> {
                        mapViewModel.isFullscreenMap = false
                        mapViewModel.polylinePoints = emptyList()
                    }
                    mapViewModel.isSearchMode -> {
                        mapViewModel.isSearchMode = false
                    }
                    else -> mapViewModel.navigateTo(CustomerScreen.Landing)
                }
            }
            CustomerScreen.RouteSelection -> {
                mapViewModel.resetSearch()
                mapViewModel.navigateTo(CustomerScreen.Landing)
            }
            else -> mapViewModel.navigateTo(CustomerScreen.Landing)
        }
    }

    Scaffold(
        bottomBar = {
            val showBottomBar = (mapViewModel.currentScreen == CustomerScreen.Landing || mapViewModel.currentScreen == CustomerScreen.MainMap || mapViewModel.currentScreen == CustomerScreen.Account)
                    && mapViewModel.polylinePoints.isEmpty() && mapViewModel.currentOrderId == null
            
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, null) },
                        label = { Text("Home") },
                        selected = mapViewModel.currentScreen == CustomerScreen.Landing,
                        onClick = { mapViewModel.navigateTo(CustomerScreen.Landing) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FamekoBlue,
                            selectedTextColor = FamekoBlue,
                            indicatorColor = FamekoGold.copy(alpha = 0.3f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.DirectionsCar, null) },
                        label = { Text("Rides") },
                        selected = mapViewModel.currentScreen == CustomerScreen.MainMap,
                        onClick = { mapViewModel.navigateTo(CustomerScreen.MainMap) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FamekoBlue,
                            selectedTextColor = FamekoBlue,
                            indicatorColor = FamekoGold.copy(alpha = 0.3f)
                        )
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Person, null) },
                        label = { Text("Account") },
                        selected = mapViewModel.currentScreen == CustomerScreen.Account,
                        onClick = { mapViewModel.navigateTo(CustomerScreen.Account) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FamekoBlue,
                            selectedTextColor = FamekoBlue,
                            indicatorColor = FamekoGold.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            // Map layer (Always present to avoid recreation and state loss)
            MainMapContent(
                viewModel = mapViewModel,
                mapView = mapView,
                isActive = true, // Always track location for seamless transitions
                onNavigateToChat = { orderId, name -> mapViewModel.navigateTo(CustomerScreen.Chat(orderId, name)) }
            )

            when (val screen = mapViewModel.currentScreen) {
                CustomerScreen.Landing, is CustomerScreen.MainMap -> {
                    // Back handler is now global
                }
                CustomerScreen.Account -> {
                    CustomerAccountScreen(
                        sessionManager = sessionManager,
                        onNavigate = { mapViewModel.navigateTo(it) },
                        onLogout = {
                            sessionManager.logout()
                            val intent = Intent(context, CustomerLoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            (context as? Activity)?.finish()
                        }
                    )
                }
                is CustomerScreen.Chat -> {
                    CustomerChatScreen(
                        orderId = screen.orderId,
                        driverName = screen.driverName,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) }
                    )
                }
                CustomerScreen.Trips -> {
                    CustomerHistoryScreen(
                        title = "Active Trips",
                        emptyMessage = "No trips yet",
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) }
                    )
                }
                CustomerScreen.Rentals -> {
                    RentalsScreen(
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) },
                        onNavigateToDetails = { rental -> mapViewModel.navigateTo(CustomerScreen.RentalDetails(rental)) },
                        onRebook = { }
                    )
                }
                is CustomerScreen.RentalDetails -> {
                    RentalDetailsScreen(
                        rental = screen.rental,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.Rentals) },
                        onNavigateToMainMap = { mapViewModel.navigateTo(CustomerScreen.MainMap) },
                        onStartNavigation = { rental -> mapViewModel.startNavigationForRental(rental) }
                    )
                }
                CustomerScreen.FleetBrowse -> {
                    FleetSelectionScreen(
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) },
                        onVehicleDetails = { vehicle ->
                            mapViewModel.navigateTo(CustomerScreen.VehicleDetails(vehicle))
                        }
                    )
                }
                is CustomerScreen.VehicleDetails -> {
                    VehicleDetailsScreen(
                        vehicle = screen.vehicle,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.FleetBrowse) },
                        onBookNow = { vehicle ->
                            mapViewModel.navigateTo(CustomerScreen.RentalBooking(vehicle))
                        }
                    )
                }
                is CustomerScreen.RentalBooking -> {
                    RentalBookingScreen(
                        vehicle = screen.vehicle,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.VehicleDetails(screen.vehicle)) },
                        onConfirm = { days, vId, _, totalPrice, scheduledDate, tripNotes, stopsStr, isSelfDrive, pMethod ->
                            scope.launch {
                                val cId = sessionManager.getCustomerId()?.toIntOrNull() ?: 1
                                val repo = RentalRepository()
                                repo.bookRental(
                                    customerId = cId,
                                    vehicleId = vId,
                                    pickupLocation = mapViewModel.pickupLocation.ifEmpty { "Current Location" },
                                    pickupLat = mapViewModel.pickupLat ?: 0.0,
                                    pickupLng = mapViewModel.pickupLng ?: 0.0,
                                    durationHours = days * 24,
                                    totalPrice = totalPrice,
                                    startTime = scheduledDate,
                                    tripNotes = tripNotes,
                                    stops = stopsStr,
                                    isSelfDrive = isSelfDrive,
                                    paymentMethod = pMethod
                                ).onSuccess { response ->
                                    if (pMethod == "ELECTRONIC" && response.checkoutUrl != null) {
                                        mapViewModel.navigateTo(CustomerScreen.PaystackCheckout(response.checkoutUrl!!))
                                    } else {
                                        Toast.makeText(context, "Rental booked successfully!", Toast.LENGTH_SHORT).show()
                                        mapViewModel.refreshActiveRental()
                                        mapViewModel.navigateTo(CustomerScreen.MainMap)
                                    }
                                }.onFailure {
                                    Toast.makeText(context, "Booking failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
                CustomerScreen.RideHistory -> {
                    CustomerHistoryScreen(
                        title = "Ride History",
                        emptyMessage = "No ride history yet",
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) }
                    )
                }
                CustomerScreen.Promotions -> {
                    PromotionsScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) })
                }
                CustomerScreen.Support -> {
                    SupportScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) })
                }
                CustomerScreen.Notifications -> {
                    NotificationsScreen(
                        notifications = mapViewModel.notifications,
                        onDelete = { id: Int -> mapViewModel.deleteNotification(id) },
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) }
                    )
                }
                CustomerScreen.NotificationSettings -> {
                    CustomerNotificationSettingsScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) })
                }
                is CustomerScreen.PaystackCheckout -> {
                    PaystackWebViewScreen(
                        url = screen.url,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.MainMap) },
                        onSuccess = {
                            Toast.makeText(context, "Payment processing... please wait for confirmation.", Toast.LENGTH_SHORT).show()
                            mapViewModel.navigateTo(CustomerScreen.MainMap)
                        }
                    )
                }
                CustomerScreen.RouteSelection -> {
                    RouteSelectionScreen(
                        viewModel = mapViewModel,
                        onBack = { 
                            mapViewModel.resetSearch()
                            mapViewModel.navigateTo(CustomerScreen.Landing)
                        },
                        onMapClick = {
                            mapViewModel.navigateTo(CustomerScreen.MainMap)
                        }
                    )
                }
                CustomerScreen.Profile -> {
                    CustomerProfileScreen(
                        viewModel = mapViewModel,
                        profile = mapViewModel.customerProfile,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.Account) }
                    )
                }
                CustomerScreen.Payment -> {
                    CustomerPaymentScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.Account) })
                }
                CustomerScreen.Safety -> {
                    CustomerSafetyScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.Account) })
                }
                CustomerScreen.ManagePlaces -> {
                    CustomerManagePlacesScreen(
                        viewModel = mapViewModel,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.Account) },
                        onAddPlace = { label -> mapViewModel.navigateTo(CustomerScreen.SaveLocationSearch(label)) },
                        onEditPlace = { id, label -> mapViewModel.navigateTo(CustomerScreen.SaveLocationSearch(label, id.toString())) }
                    )
                }
                is CustomerScreen.SaveLocationSearch -> {
                    SaveLocationSearchScreen(
                        label = screen.label,
                        placeId = screen.placeId,
                        viewModel = mapViewModel,
                        onBack = { mapViewModel.navigateTo(CustomerScreen.ManagePlaces) }
                    )
                }
                CustomerScreen.FamilyProfile -> {
                    CustomerFamilyProfileScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.Account) })
                }
                CustomerScreen.WorkProfile -> {
                    CustomerWorkProfileScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.Account) })
                }
                CustomerScreen.TermsAndConditions -> {
                    TermsAndConditionsScreen(onBack = { mapViewModel.navigateTo(CustomerScreen.Account) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
@Composable
fun MainMapContent(
    viewModel: CustomerMapViewModel,
    mapView: MapView,
    isActive: Boolean,
    onNavigateToChat: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val voiceNavManager = remember { com.example.famekodriver.core.utils.VoiceNavigationManager(context) }

    DisposableEffect(voiceNavManager) { onDispose { voiceNavManager.shutdown() } }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission = it }

    var motorbikeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context).data(ImageLinks.IC_OKADA).build()
        val result = (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        if (result != null) motorbikeBitmap = Bitmap.createScaledBitmap(result, 40, 40, false)
    }

    LaunchedEffect(Unit) { if (!hasLocationPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasCentredOnLocation by remember { mutableStateOf(false) }

    LaunchedEffect(hasLocationPermission, isActive, mapLibreMap, viewModel.isFullscreenMap) {
        if (!isActive || mapLibreMap == null) return@LaunchedEffect

        if (hasLocationPermission && viewModel.isFullscreenMap) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000).setMinUpdateIntervalMillis(500).build()
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    if (location.latitude == 0.0 || location.longitude == 0.0) return

                    viewModel.currentLatLng = LatLng(location.latitude, location.longitude)
                    viewModel.updateNearbyDrivers(location.latitude, location.longitude)
                    viewModel.checkOffRoute(location.latitude, location.longitude)
                    
                    if (viewModel.checkArrival(location.latitude, location.longitude)) voiceNavManager.announceArrival()

                    if (viewModel.polylinePoints.isNotEmpty()) {
                        voiceNavManager.updateProgress(
                            currentLat = location.latitude,
                            currentLng = location.longitude,
                            route = viewModel.polylinePoints.map { p -> p.latitude to p.longitude },
                            etaMin = viewModel.durationMin,
                            distanceKm = viewModel.distanceKm
                        )
                    }

                    mapLibreMap?.let { map ->
                        val speedKmh = location.speed.toDouble() * 3.6
                        val targetZoom = when {
                            speedKmh > 80.0 -> 15.0
                            speedKmh > 50.0 -> 16.0
                            speedKmh > 20.0 -> 17.0
                            else -> 18.0
                        }

                        val cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(LatLng(location.latitude, location.longitude))
                            .zoom(targetZoom)
                            .bearing(location.bearing.toDouble()) 
                            .tilt(50.0) 
                            .build()
                        
                        @Suppress("DEPRECATION")
                        map.setPadding(0, 0, 0, (context.resources.displayMetrics.heightPixels * 0.3).toInt())
                        map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 1000)
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
                awaitCancellation()
            } finally {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                mapLibreMap?.setPadding(0, 0, 0, 0)
            }
        } else {
            while (isActive) {
                if (hasLocationPermission) {
                    val targetLat = viewModel.pickupLat
                    val targetLng = viewModel.pickupLng
                    
                    if (targetLat != null && targetLng != null) {
                        viewModel.updateNearbyDrivers(targetLat, targetLng)
                    } else {
                        @SuppressLint("MissingPermission")
                        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                            location?.let {
                                if (it.latitude != 0.0 && it.longitude != 0.0) {
                                    viewModel.currentLatLng = LatLng(it.latitude, it.longitude)
                                    viewModel.updateNearbyDrivers(it.latitude, it.longitude)
                                    
                                    if (viewModel.pickupLat == null && viewModel.pickupLocation.isEmpty()) {
                                        viewModel.pickupLat = it.latitude
                                        viewModel.pickupLng = it.longitude
                                        
                                        if (viewModel.dropOffLat != null && viewModel.polylinePoints.isEmpty() && 
                                            viewModel.currentScreen != CustomerScreen.RouteSelection && !viewModel.isSearchMode) {
                                            viewModel.calculateRoute()
                                        }
                                    }

                                    if (!hasCentredOnLocation && mapLibreMap != null) {
                                        mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0))
                                        hasCentredOnLocation = true
                                    }
                                }
                            }
                        }
                    }
                }
                delay(5.seconds)
            }
        }
    }

    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    val activeMarkers = remember { ConcurrentHashMap<String, org.maplibre.android.annotations.Marker>() }
    val activePolylineRef = remember { object { var value: org.maplibre.android.annotations.Polyline? = null } }
    val activeRouteMarkers = remember { mutableListOf<org.maplibre.android.annotations.Marker>() }
    val animatingMarkerIds = remember { mutableSetOf<String>() }

    val pickupIcon = remember(context) { createMarkerIcon(context, BoltGreen) }
    val dropoffIcon = remember(context) { createMarkerIcon(context, BoltOrange) }

    val currentSheetState = remember(viewModel.orderStatusData?.status, viewModel.estimatedFare, viewModel.currentOrderId, viewModel.isSearchMode, viewModel.pickupLocation, viewModel.dropOffLocation, viewModel.activeServiceMode, viewModel.pickupLat, viewModel.activeRental, viewModel.rentalPickupLat, viewModel.currentScreen, viewModel.isTimedOut, viewModel.polylinePoints) {
        val state = when {
            viewModel.isTimedOut -> CustomerSheetState.TIMED_OUT
            viewModel.orderStatusData?.status == "PENDING" -> CustomerSheetState.SEARCHING_FOR_DRIVER
            viewModel.orderStatusData?.status == "SCHEDULED" -> CustomerSheetState.RIDE_SCHEDULED
            viewModel.orderStatusData?.status != null && viewModel.orderStatusData?.status != "CANCELLED" && viewModel.orderStatusData?.status != "DELIVERED" -> CustomerSheetState.ON_TRIP
            viewModel.currentScreen == CustomerScreen.Landing -> CustomerSheetState.LANDING
            viewModel.isSearchMode -> CustomerSheetState.PICKING_ADDRESS
            viewModel.currentOrderId != null -> CustomerSheetState.ON_TRIP
            viewModel.activeRental != null -> CustomerSheetState.ACTIVE_RENTAL
            (viewModel.activeServiceMode == ServiceType.RIDE_HAILING || viewModel.activeServiceMode == ServiceType.PACKAGE_DELIVERY) && 
                viewModel.polylinePoints.isNotEmpty() && viewModel.currentOrderId == null -> CustomerSheetState.SELECTING_SERVICE
            ((viewModel.activeServiceMode == ServiceType.RIDE_HAILING || viewModel.activeServiceMode == ServiceType.PACKAGE_DELIVERY) && 
                (viewModel.pickupLocation.isNotEmpty() || viewModel.dropOffLocation.isNotEmpty())) -> CustomerSheetState.PICKING_ADDRESS
            else -> CustomerSheetState.IDLE
        }
        state
    }

    LaunchedEffect(mapLibreMap, hasLocationPermission, viewModel.isFullscreenMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (hasLocationPermission) {
            map.getStyle { style ->
                val locationComponent = map.locationComponent
                if (!locationComponent.isLocationComponentActivated) {
                    locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
                }
                locationComponent.isLocationComponentEnabled = true
                if (viewModel.isFullscreenMap) { locationComponent.cameraMode = CameraMode.NONE; locationComponent.renderMode = RenderMode.GPS } 
                else { locationComponent.cameraMode = CameraMode.NONE; locationComponent.renderMode = RenderMode.NORMAL }
            }
        } else { map.locationComponent.isLocationComponentEnabled = false }
    }

    LaunchedEffect(viewModel.isFullscreenMap) {
        if (viewModel.isFullscreenMap && viewModel.dropOffLocation.isNotEmpty()) voiceNavManager.announceTripStart(viewModel.dropOffLocation)
        voiceNavManager.setEnabled(viewModel.isFullscreenMap)
    }

    // Update markers and drivers
    LaunchedEffect(mapLibreMap, viewModel.drivers, viewModel.orderStatusData) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val currentStatus = viewModel.orderStatusData
        val driversToShow = if (currentStatus?.status == "ASSIGNED" || currentStatus?.status == "IN_TRANSIT" || currentStatus?.status == "ARRIVED") {
            val dLat = currentStatus.driverLat; val dLng = currentStatus.driverLng
            if (dLat != null && dLng != null) { listOf(DriverLocation(id = currentStatus.driverId ?: "0", latitude = dLat, longitude = dLng, bearing = currentStatus.driverBearing ?: 0f, vehicleType = currentStatus.driverVehicle)) } else emptyList()
        } else { viewModel.drivers }

        @Suppress("DEPRECATION")
        val currentDriverIds = driversToShow.map { it.id }.toSet()
        val iterator = activeMarkers.entries.iterator()
        while (iterator.hasNext()) { val entry = iterator.next(); if (entry.key !in currentDriverIds) { map.removeMarker(entry.value); iterator.remove() } }

        driversToShow.forEach { driver ->
            val id = driver.id; val marker = activeMarkers[id]; val endPos = LatLng(driver.latitude, driver.longitude)
            val vehicleTypeStr = driver.vehicleType?.lowercase() ?: ""
            val baseBitmap = if (vehicleTypeStr.contains("okada") || vehicleTypeStr.contains("bike") || vehicleTypeStr.contains("motorcycle") || vehicleTypeStr.contains("rider") || vehicleTypeStr.contains("motorbike") || vehicleTypeStr.contains("motor")) {
                motorbikeBitmap ?: ContextCompat.getDrawable(context, R.drawable.ic_car_saloon)?.toBitmap()
            } else { ContextCompat.getDrawable(context, R.drawable.ic_car_saloon)?.toBitmap() }
            
            val carIcon = baseBitmap?.let { 
                val scaled = if (it.width != 40) Bitmap.createScaledBitmap(it, 40, 40, false) else it
                org.maplibre.android.annotations.IconFactory.getInstance(context).fromBitmap(scaled) 
            }

            if (marker == null) {
                @Suppress("DEPRECATION")
                val newMarker = map.addMarker(org.maplibre.android.annotations.MarkerOptions().position(endPos).apply { if (carIcon != null) icon(carIcon) })
                activeMarkers[id] = newMarker
            } else {
                if (carIcon != null) marker.icon = carIcon
                if (!animatingMarkerIds.contains(id)) {
                    val startPos = marker.position
                    val dLat = startPos.latitude - endPos.latitude
                    val dLng = startPos.longitude - endPos.longitude
                    val distanceSq = dLat * dLat + dLng * dLng
                    if (distanceSq > 0.00000001) { 
                        animatingMarkerIds.add(id)
                        animateMarker(marker, startPos, endPos) { animatingMarkerIds.remove(id) } 
                    }
                }
            }
        }
    }

    // Update route and route markers
    LaunchedEffect(mapLibreMap, viewModel.polylinePoints) {
        val map = mapLibreMap ?: return@LaunchedEffect
        @Suppress("DEPRECATION")
        activePolylineRef.value?.let { map.removePolyline(it) }
        activePolylineRef.value = null
        @Suppress("DEPRECATION")
        activeRouteMarkers.forEach { marker -> map.removeMarker(marker) }
        activeRouteMarkers.clear()

        if (viewModel.polylinePoints.isNotEmpty()) {
            @Suppress("DEPRECATION")
            val polyline = map.addPolyline(org.maplibre.android.annotations.PolylineOptions().addAll(viewModel.polylinePoints).color(FamekoBlue.toArgb()).width(8f))
            activePolylineRef.value = polyline
            @Suppress("DEPRECATION")
            val pickupMarker = map.addMarker(org.maplibre.android.annotations.MarkerOptions().position(viewModel.polylinePoints.first()).icon(pickupIcon).title("PICKUP"))
            @Suppress("DEPRECATION")
            val dropoffMarker = map.addMarker(org.maplibre.android.annotations.MarkerOptions().position(viewModel.polylinePoints.last()).icon(dropoffIcon).title("DROPOFF"))
            activeRouteMarkers.add(pickupMarker); activeRouteMarkers.add(dropoffMarker)
            try { val bounds = org.maplibre.android.geometry.LatLngBounds.Builder().include(viewModel.polylinePoints.first()).include(viewModel.polylinePoints.last()).build(); map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150)) } catch (_: Exception) { map.animateCamera(CameraUpdateFactory.newLatLngZoom(viewModel.polylinePoints.first(), 14.0)) }
        }
    }

    BottomSheetScaffold(
        scaffoldState = sheetScaffoldState,
            sheetPeekHeight = if (viewModel.isFullscreenMap) { if (viewModel.activeRental != null) 110.dp else 0.dp }
                             else if (currentSheetState == CustomerSheetState.LANDING) 140.dp
                             else if (currentSheetState == CustomerSheetState.IDLE) 120.dp
                             else if (viewModel.currentOrderId != null || viewModel.activeRental != null || currentSheetState == CustomerSheetState.SELECTING_SERVICE || currentSheetState == CustomerSheetState.PICKING_ADDRESS) 110.dp
                             else 0.dp,
            sheetContainerColor = Color.White,
            sheetShadowElevation = 32.dp,
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetDragHandle = { Box(Modifier.padding(vertical = 12.dp).width(40.dp).height(4.dp).background(Color.LightGray.copy(alpha = 0.5f), CircleShape)) },
            sheetContent = {
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).navigationBarsPadding().imePadding().pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
                ) {
                    when (currentSheetState) {
                        CustomerSheetState.LANDING -> {
                            CustomerLandingScreen(
                                activeRental = viewModel.activeRental,
                                onViewRental = { viewModel.navigateTo(CustomerScreen.RentalDetails(it)) },
                                onServiceSelected = { viewModel.setServiceMode(it); if (it == ServiceType.RENTAL) viewModel.navigateTo(CustomerScreen.FleetBrowse) else viewModel.navigateTo(CustomerScreen.MainMap) },
                                onScheduleClick = { showDatePicker = true },
                                recentPlaces = viewModel.recentPlaces,
                                onSearchClick = { viewModel.navigateTo(CustomerScreen.RouteSelection) },
                                onPlaceClick = { viewModel.setServiceMode(ServiceType.RIDE_HAILING); viewModel.selectSuggestion(it, isPickup = false); viewModel.navigateTo(CustomerScreen.MainMap) }
                            )
                        }
                        CustomerSheetState.IDLE -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                    QuickShortcutItem("Home", Icons.Default.Home, BoltGreen) { if (viewModel.savedPlaces.value.any { it.label.equals("Home", true) }) { viewModel.applyShortcut("Home") { if (hasLocationPermission) { @SuppressLint("MissingPermission") fusedLocationClient.lastLocation.addOnSuccessListener { loc -> loc?.let { viewModel.useCurrentLocation(it, forPickup = true, stopIndex = -1) } } } } } else { Toast.makeText(context, "Please set your Home address in Manage Places", Toast.LENGTH_SHORT).show(); viewModel.navigateTo(CustomerScreen.ManagePlaces) } }
                                    QuickShortcutItem("Work", Icons.Default.Work, FamekoBlue) { if (viewModel.savedPlaces.value.any { it.label.equals("Work", true) }) { viewModel.applyShortcut("Work") { if (hasLocationPermission) { @SuppressLint("MissingPermission") fusedLocationClient.lastLocation.addOnSuccessListener { loc -> loc?.let { viewModel.useCurrentLocation(it, forPickup = true, stopIndex = -1) } } } } } else { Toast.makeText(context, "Please set your Work address in Manage Places", Toast.LENGTH_SHORT).show(); viewModel.navigateTo(CustomerScreen.ManagePlaces) } }
                                    QuickShortcutItem("Recent", Icons.Default.History, Color.Gray) { viewModel.navigateTo(CustomerScreen.RouteSelection); viewModel.selectedTab = 0; viewModel.updateDropOffLocation("") }
                                    QuickShortcutItem("Saved", Icons.Default.Star, BoltYellow) { viewModel.navigateTo(CustomerScreen.RouteSelection); viewModel.selectedTab = 1; viewModel.updateDropOffLocation("") }
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }
                        CustomerSheetState.PICKING_ADDRESS -> { if (viewModel.pickupLat != null && viewModel.dropOffLat != null) { Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = FamekoBlue); Spacer(Modifier.height(12.dp)); Text("Preparing your ride...", color = Color.Gray, fontSize = 14.sp) } } } }
                        CustomerSheetState.SELECTING_SERVICE -> { RideHailingSheetContent(state = currentSheetState, viewModel = viewModel, onNavigateToChat = onNavigateToChat, onScheduleClick = { showDatePicker = true }, onShareTrip = { viewModel.shareTrip(context) }, onCloseScheduled = { viewModel.navigateTo(CustomerScreen.Landing) }, onRetryTimeout = { viewModel.clearActiveOrder() }, onCloseTimeout = { viewModel.clearActiveOrder(); viewModel.navigateTo(CustomerScreen.Landing) }) }
                        CustomerSheetState.SEARCHING_FOR_DRIVER -> { RideHailingSheetContent(state = currentSheetState, viewModel = viewModel, onNavigateToChat = onNavigateToChat, onScheduleClick = { showDatePicker = true }, onShareTrip = { viewModel.shareTrip(context) }, onCloseScheduled = { viewModel.navigateTo(CustomerScreen.Landing) }, onRetryTimeout = { viewModel.clearActiveOrder() }, onCloseTimeout = { viewModel.clearActiveOrder(); viewModel.navigateTo(CustomerScreen.Landing) }) }
                        CustomerSheetState.ON_TRIP -> { RideHailingSheetContent(state = currentSheetState, viewModel = viewModel, onNavigateToChat = onNavigateToChat, onScheduleClick = { showDatePicker = true }, onShareTrip = { viewModel.shareTrip(context) }, onCloseScheduled = { viewModel.navigateTo(CustomerScreen.Landing) }, onRetryTimeout = { viewModel.clearActiveOrder() }, onCloseTimeout = { viewModel.clearActiveOrder(); viewModel.navigateTo(CustomerScreen.Landing) }) }
                        CustomerSheetState.ACTIVE_RENTAL -> { RentalSheetContent(state = currentSheetState, viewModel = viewModel, onDetailsClick = { viewModel.navigateTo(CustomerScreen.RentalDetails(it)) }, onStartNavigation = { val lat = viewModel.dropOffLat; val lng = viewModel.dropOffLng; val address = viewModel.dropOffLocation; if (lat != null && lng != null && lat != 0.0) { if (hasLocationPermission) { fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location -> location?.let { if (it.latitude != 0.0 && it.longitude != 0.0) { viewModel.pickupLat = it.latitude; viewModel.pickupLng = it.longitude; viewModel.dropOffLat = lat; viewModel.dropOffLng = lng; viewModel.dropOffLocation = address; viewModel.isFullscreenMap = true; viewModel.calculateRoute() } } } } else { viewModel.isFullscreenMap = true; viewModel.calculateRoute() } } else { viewModel.navigateTo(CustomerScreen.RouteSelection) } }) }
                        CustomerSheetState.RIDE_SCHEDULED -> { RideHailingSheetContent(state = currentSheetState, viewModel = viewModel, onNavigateToChat = onNavigateToChat, onScheduleClick = { showDatePicker = true }, onShareTrip = { viewModel.shareTrip(context) }, onCloseScheduled = { viewModel.navigateTo(CustomerScreen.Landing) }, onRetryTimeout = { viewModel.clearActiveOrder() }, onCloseTimeout = { viewModel.clearActiveOrder(); viewModel.navigateTo(CustomerScreen.Landing) }) }
                        CustomerSheetState.TIMED_OUT -> { RideHailingSheetContent(state = currentSheetState, viewModel = viewModel, onNavigateToChat = onNavigateToChat, onScheduleClick = { showDatePicker = true }, onShareTrip = { viewModel.shareTrip(context) }, onCloseScheduled = { viewModel.navigateTo(CustomerScreen.Landing) }, onRetryTimeout = { viewModel.clearActiveOrder() }, onCloseTimeout = { viewModel.clearActiveOrder(); viewModel.navigateTo(CustomerScreen.Landing) }) }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier.fillMaxSize(),
                    update = { mv: MapView ->
                        mv.getMapAsync { map: MapLibreMap ->
                            if (mapLibreMap == null) {
                                mapLibreMap = map
                                val styleUrl = "https://api.tomtom.com/style/2/custom/style/dG9tdG9tQEBAZFVDV2NzZ09mRGhEaU9MdDsVGbKlskhOMbwzZ3vdhit8?key=${NetworkClient.TOMTOM_API_KEY}"
                                map.setStyle(styleUrl) { style ->
                                    if (hasLocationPermission) {
                                        val locationComponent = map.locationComponent
                                        if (!locationComponent.isLocationComponentActivated) {
                                            locationComponent.activateLocationComponent(LocationComponentActivationOptions.builder(context, style).build())
                                        }
                                        locationComponent.isLocationComponentEnabled = true
                                        locationComponent.renderMode = RenderMode.NORMAL
                                    }
                                }
                                map.addOnMapClickListener { focusManager.clearFocus(); false }
                            }
                        }
                    }
                )

                if (viewModel.isFullscreenMap) {
                    NavigationOverlay(instruction = viewModel.currentInstruction, currentLatLng = viewModel.currentLatLng, distanceKm = viewModel.distanceKm, durationMin = viewModel.durationMin, onExit = { viewModel.isFullscreenMap = false; viewModel.polylinePoints = emptyList() })
                }

                if (viewModel.currentOrderId == null && currentSheetState != CustomerSheetState.LANDING && !viewModel.isFullscreenMap) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 16.dp, end = 16.dp).statusBarsPadding()) {
                        Surface(modifier = Modifier.fillMaxWidth().height(64.dp).clickable { if (currentSheetState == CustomerSheetState.SELECTING_SERVICE) viewModel.resetSearch() else viewModel.navigateTo(CustomerScreen.RouteSelection) }, shape = RoundedCornerShape(16.dp), color = Color.White, shadowElevation = 8.dp, border = BorderStroke(1.dp, BoltLightGray)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp)) {
                                IconButton(onClick = { if (viewModel.activeRental != null) { if (viewModel.dropOffLocation.isNotEmpty()) viewModel.clearDestination() else viewModel.navigateTo(CustomerScreen.Landing) } else { viewModel.resetSearch(); viewModel.navigateTo(CustomerScreen.Landing) } }) { Icon(Icons.Default.Close, null, tint = BoltDark, modifier = Modifier.size(24.dp)) }
                                val startLabel = if (viewModel.activeServiceMode == ServiceType.RENTAL) viewModel.rentalPickupLocation.ifEmpty { viewModel.pickupLocation } else viewModel.pickupLocation
                                Text(text = if (viewModel.dropOffLocation.isNotEmpty() && startLabel.isNotEmpty()) { "${startLabel.split(",").first()} → ${viewModel.dropOffLocation.split(",").first()}" } else if (startLabel.isNotEmpty()) { "From ${startLabel.split(",").first()}" } else "Where to?", style = MaterialTheme.typography.titleMedium, color = if (viewModel.dropOffLocation.isNotEmpty() || startLabel.isNotEmpty()) FamekoBlue else Color.Gray, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                IconButton(onClick = { viewModel.navigateTo(CustomerScreen.RouteSelection) }) { Icon(Icons.Default.Add, null, tint = BoltDark, modifier = Modifier.size(24.dp)) }
                            }
                        }
                    }
                }

                if (!viewModel.isFullscreenMap && (currentSheetState == CustomerSheetState.IDLE || currentSheetState == CustomerSheetState.LANDING || currentSheetState == CustomerSheetState.SELECTING_SERVICE || currentSheetState == CustomerSheetState.PICKING_ADDRESS)) {
                    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 160.dp, end = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        FloatingActionButton(onClick = { hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED; if (hasLocationPermission) { fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc -> loc?.let { mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))) } } } }, containerColor = Color.White, contentColor = BoltDark, shape = CircleShape, modifier = Modifier.size(48.dp)) { Icon(Icons.Default.MyLocation, null, modifier = Modifier.size(20.dp)) }
                    }
                }
                
                if (currentSheetState == CustomerSheetState.SEARCHING_FOR_DRIVER) { RadarPulseAnimation() }
                
                viewModel.incomingCall?.let { call ->
                    AlertDialog(onDismissRequest = { }, title = { Text("Incoming Call") }, text = { Text("Driver ${call.callerName} is calling you...") }, confirmButton = { Button(onClick = { viewModel.acceptCall(call.callId) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))) { Icon(Icons.Default.Call, null); Spacer(Modifier.width(8.dp)); Text("Accept") } }, dismissButton = { OutlinedButton(onClick = { viewModel.rejectCall(call.callId) }) { Text("Reject", color = Color.Red) } }, shape = RoundedCornerShape(24.dp))
                }
                viewModel.ongoingCall?.let { call -> CallOverlay(call = call, onEnd = { viewModel.endCall(call.callId) }) }

                if (viewModel.showRatingDialog) { RatingDialog(driverName = viewModel.ratingDriverName ?: "Your Driver", driverProfilePic = viewModel.ratingDriverPic, onRate = { rating, comment -> viewModel.submitRating(rating, comment) }, onDismiss = { viewModel.showRatingDialog = false }) }
                if (viewModel.showTripSummary) { TripSummaryDialog(fare = viewModel.finalFare, onRate = { viewModel.showTripSummary = false; viewModel.showRatingDialog = true }, onDismiss = { viewModel.showTripSummary = false }) }

                if (viewModel.showCancelConfirmation) {
                    var selectedReason by remember { mutableStateOf("") }; val reasons = listOf("Driver is too far away", "Wait time is too long", "Changed my mind", "Order placed by mistake", "Incorrect pickup location", "Driver asked me to cancel", "Other")
                    AlertDialog(onDismissRequest = { viewModel.showCancelConfirmation = false }, title = { Text("Cancel Ride?", fontWeight = FontWeight.Bold) }, text = { Column { Text("Please tell us why you're cancelling:"); Spacer(Modifier.height(12.dp)); reasons.forEach { reason -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { selectedReason = reason }.padding(vertical = 4.dp)) { RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason }, colors = RadioButtonDefaults.colors(selectedColor = BoltGreen)); Text(text = reason, modifier = Modifier.padding(start = 8.dp)) } } } }, confirmButton = { Button(onClick = { viewModel.showCancelConfirmation = false; viewModel.cancelOrder(selectedReason.ifEmpty { "Not specified" }) }, enabled = selectedReason.isNotEmpty(), colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Confirm Cancellation") } }, dismissButton = { TextButton(onClick = { viewModel.showCancelConfirmation = false }) { Text("No, Keep Ride") } }, shape = RoundedCornerShape(24.dp), containerColor = Color.White)
                }

                if (showDatePicker) { DatePickerDialog(onDismissRequest = { showDatePicker = false }, confirmButton = { TextButton(onClick = { showDatePicker = false; showTimePicker = true }) { Text("Next") } }, dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }) { DatePicker(state = datePickerState) } }
                if (showTimePicker) { AlertDialog(onDismissRequest = { showTimePicker = false }, confirmButton = { TextButton(onClick = { val dateStr = datePickerState.selectedDateMillis?.let { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(it)) } ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()); val timeStr = "%02d:%02d".format(timePickerState.hour, timePickerState.minute); val scheduledTime = "$dateStr $timeStr"; viewModel.updateScheduledRideTime(scheduledTime); viewModel.setServiceMode(ServiceType.RIDE_HAILING); viewModel.navigateTo(CustomerScreen.MainMap); viewModel.isSearchMode = true; showTimePicker = false }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { showTimePicker = false; showDatePicker = true }) { Text("Back") } }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("Select Time", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp)); TimePicker(state = timePickerState) } } ) }

                viewModel.showRegionalError?.let { error -> AlertDialog(onDismissRequest = { viewModel.showRegionalError = null }, title = { Text("Service Unavailable") }, text = { Text(error) }, confirmButton = { Button(onClick = { viewModel.showRegionalError = null }) { Text("OK") } }, shape = RoundedCornerShape(24.dp)) }
            }
        }
    }
}
