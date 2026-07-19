// Fameko Customer Map Activity - Cleaned
package com.example.famekodriver.customer

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
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
import com.example.famekodriver.customer.ui.screens.*
import com.example.famekodriver.customer.ui.theme.*
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.firebase.messaging.FirebaseMessaging
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
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

sealed class CustomerScreen {
    object Landing : CustomerScreen()
    object MainMap : CustomerScreen()
    object Account : CustomerScreen()
    data class Chat(val orderId: Int, val driverName: String) : CustomerScreen()
    object Trips : CustomerScreen()
    object Rentals : CustomerScreen()
    object RideHistory : CustomerScreen()
    object Promotions : CustomerScreen()
    object Support : CustomerScreen()
    object Notifications : CustomerScreen()
    object NotificationSettings : CustomerScreen()
    object FleetBrowse : CustomerScreen()
    data class VehicleDetails(val vehicle: Map<String, Any>) : CustomerScreen()
    data class RentalBooking(val vehicle: Map<String, Any>) : CustomerScreen()
    data class RentalDetails(val rental: Map<String, Any>) : CustomerScreen()
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

    BackHandler(enabled = mapViewModel.currentScreen != CustomerScreen.Landing) {
        mapViewModel.navigateTo(CustomerScreen.Landing)
    }

    Scaffold(
        bottomBar = {
            val showBottomBar = (mapViewModel.currentScreen == CustomerScreen.Landing || mapViewModel.currentScreen == CustomerScreen.MainMap || mapViewModel.currentScreen == CustomerScreen.Account)
                    && mapViewModel.polylinePoints.isEmpty() && mapViewModel.currentOrderId == null && mapViewModel.activeRental == null
            
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
            val showMap = mapViewModel.currentScreen == CustomerScreen.Landing || mapViewModel.currentScreen is CustomerScreen.MainMap || mapViewModel.currentScreen == CustomerScreen.RouteSelection || mapViewModel.isFullscreenMap
            
            // Map layer (Always present to avoid recreation and state loss)
            MainMapContent(
                viewModel = mapViewModel,
                mapView = mapView,
                isBackHandlerEnabled = showMap,
                isActive = true, // Always track location for seamless transitions
                onNavigateToChat = { orderId, name -> mapViewModel.navigateTo(CustomerScreen.Chat(orderId, name)) }
            )

            when (val screen = mapViewModel.currentScreen) {
                CustomerScreen.Landing, is CustomerScreen.MainMap -> {
                    if (mapViewModel.currentScreen == CustomerScreen.Landing) {
                        BackHandler {
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastBackPressTime < 2000) {
                                (context as? Activity)?.finish()
                            } else {
                                lastBackPressTime = currentTime
                                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
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
                        onRebook = { rental ->
                            val vehicle = mapOf(
                                "id" to (rental["vehicle_id"] ?: 1),
                                "name" to (rental["vehicle_name"] ?: ""),
                                "model" to (rental["vehicle_model"] ?: ""),
                                "vehicle_type" to (rental["vehicle_type"] ?: ""),
                                "image_urls" to (rental["vehicle_image"] ?: ""),
                                "daily_rate" to (rental["daily_rate"] ?: 400.0) 
                            )
                            mapViewModel.navigateTo(CustomerScreen.RentalBooking(vehicle))
                        }
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
                                repository.bookRental(
                                    customerId = sessionManager.getCustomerId()?.toIntOrNull() ?: 1,
                                    vehicleId = vId, 
                                    pickupLocation = mapViewModel.rentalPickupLocation.ifEmpty { "My Location" }, 
                                    pickupLat = mapViewModel.rentalPickupLat ?: 0.0, 
                                    pickupLng = mapViewModel.rentalPickupLng ?: 0.0, 
                                    durationHours = days * 24, 
                                    totalPrice = totalPrice, 
                                    startTime = scheduledDate, 
                                    tripNotes = tripNotes, 
                                    stops = stopsStr,
                                    isSelfDrive = isSelfDrive,
                                    paymentMethod = pMethod
                                )
                                    .onSuccess { response ->
                                        if (response.checkoutUrl != null) {
                                            mapViewModel.navigateTo(CustomerScreen.PaystackCheckout(response.checkoutUrl!!))
                                        } else {
                                            val msg = if (scheduledDate != null) { "Rental scheduled for $scheduledDate\nCode: ${response.bookingCode}" } else { "Rental confirmed!\nCode: ${response.bookingCode}" }
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                            mapViewModel.navigateTo(CustomerScreen.MainMap)
                                        }
                                    }.onFailure { Toast.makeText(context, "Booking failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    )
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
                        },
                        onComplete = {
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
    isBackHandlerEnabled: Boolean,
    isActive: Boolean,
    onNavigateToChat: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DriverRepository.getInstance() }
    val focusManager = LocalFocusManager.current
    val pickupFocusRequester = remember { FocusRequester() }
    val dropOffFocusRequester = remember { FocusRequester() }

    val voiceCallHandler = remember { VoiceCallHandler { data -> viewModel.sendAudioData(data) } }
    val voiceNavManager = remember { com.example.famekodriver.core.utils.VoiceNavigationManager(context) }

    DisposableEffect(voiceNavManager) {
        onDispose { voiceNavManager.shutdown() }
    }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val timePickerState = rememberTimePickerState()

    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasLocationPermission = it }

    var hasAudioPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasAudioPermission = it }

    val scope = rememberCoroutineScope()
    var motorbikeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(Unit) {
        val loader = context.imageLoader
        val request = ImageRequest.Builder(context)
            .data(ImageLinks.IC_OKADA)
            .build()
        val result = (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        if (result != null) {
            motorbikeBitmap = Bitmap.createScaledBitmap(result, 40, 40, false)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasCentredOnLocation by remember { mutableStateOf(false) }

    LaunchedEffect(hasLocationPermission, viewModel.pickupLat, viewModel.pickupLng, isActive, mapLibreMap, viewModel.isFullscreenMap) {
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
                                viewModel.updateNearbyDrivers(it.latitude, it.longitude)
                                
                                // Ensure ViewModel has current location for navigation and other tasks
                                if (!viewModel.isSearchMode && viewModel.pickupLat == null) {
                                    viewModel.pickupLat = it.latitude
                                    viewModel.pickupLng = it.longitude
                                    
                                    // Auto-calculate route if waiting for navigation to start
                                    if (viewModel.isFullscreenMap && viewModel.dropOffLat != null) {
                                        viewModel.calculateRoute()
                                    }
                                }

                                if (viewModel.isFullscreenMap && viewModel.polylinePoints.isNotEmpty()) {
                                    voiceNavManager.updateProgress(
                                        currentLat = it.latitude,
                                        currentLng = it.longitude,
                                        route = viewModel.polylinePoints.map { p -> p.latitude to p.longitude },
                                        etaMin = viewModel.durationMin,
                                        distanceKm = viewModel.distanceKm
                                    )
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
            delay(if (viewModel.isFullscreenMap) 2.seconds else 5.seconds)
        }
    }

    var isPickupFocused by remember { mutableStateOf(false) }
    var isDropOffFocused by remember { mutableStateOf(false) }

    BackHandler(enabled = isBackHandlerEnabled) {
        when {
            viewModel.isFullscreenMap -> { viewModel.isFullscreenMap = false; viewModel.polylinePoints = emptyList() }
            viewModel.isSearchMode -> { viewModel.isSearchMode = false; focusManager.clearFocus() }
            viewModel.currentOrderId != null -> {
                viewModel.showCancelConfirmation = true
            }
            viewModel.estimatedFare != null && viewModel.currentOrderId == null -> {
                viewModel.resetSearch()
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
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                location?.let {
                    mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0))
                    viewModel.updateNearbyDrivers(it.latitude, it.longitude)
                }
            }
        }
    }


    val sheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded, skipHiddenState = true)
    )

    val activeMarkers = remember { ConcurrentHashMap<String, Marker>() }
    val activePolylineRef = remember { object { var value: Polyline? = null } }
    val activeRouteMarkers = remember { mutableListOf<Marker>() }
    val animatingMarkerIds = remember { mutableSetOf<String>() }

    val currentSheetState = remember(viewModel.orderStatusData?.status, viewModel.estimatedFare, viewModel.currentOrderId, viewModel.isSearchMode, viewModel.pickupLocation, viewModel.dropOffLocation, viewModel.activeServiceMode, viewModel.pickupLat, viewModel.activeRental, viewModel.rentalPickupLat, viewModel.currentScreen, viewModel.isTimedOut, viewModel.polylinePoints) {
        when {
            viewModel.isTimedOut -> CustomerSheetState.TIMED_OUT
            viewModel.orderStatusData?.status == "PENDING" -> CustomerSheetState.SEARCHING_FOR_DRIVER
            viewModel.orderStatusData?.status == "SCHEDULED" -> CustomerSheetState.RIDE_SCHEDULED
            viewModel.orderStatusData?.status != null && viewModel.orderStatusData?.status != "CANCELLED" && viewModel.orderStatusData?.status != "DELIVERED" -> CustomerSheetState.ON_TRIP
            viewModel.activeRental != null && !viewModel.isSearchMode -> CustomerSheetState.ACTIVE_RENTAL
            (viewModel.activeServiceMode == ServiceType.RIDE_HAILING || viewModel.activeServiceMode == ServiceType.PACKAGE_DELIVERY) && viewModel.polylinePoints.isNotEmpty() && viewModel.currentOrderId == null -> CustomerSheetState.SELECTING_SERVICE
            viewModel.isSearchMode || ((viewModel.activeServiceMode == ServiceType.RIDE_HAILING || viewModel.activeServiceMode == ServiceType.PACKAGE_DELIVERY) && (viewModel.pickupLocation.isNotEmpty() || viewModel.dropOffLocation.isNotEmpty())) || (viewModel.activeServiceMode == ServiceType.RENTAL && (viewModel.rentalPickupLocation.isNotEmpty() || viewModel.dropOffLocation.isNotEmpty())) -> CustomerSheetState.PICKING_ADDRESS
            viewModel.currentScreen == CustomerScreen.Landing -> CustomerSheetState.LANDING
            else -> CustomerSheetState.IDLE
        }
    }

    LaunchedEffect(mapLibreMap, hasLocationPermission, viewModel.isFullscreenMap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (hasLocationPermission) {
            map.getStyle { style ->
                val locationComponent = map.locationComponent
                if (!locationComponent.isLocationComponentActivated) {
                    locationComponent.activateLocationComponent(
                        LocationComponentActivationOptions.builder(context, style).build()
                    )
                }
                locationComponent.isLocationComponentEnabled = true
                
                if (viewModel.isFullscreenMap) {
                    locationComponent.cameraMode = CameraMode.TRACKING_GPS
                    locationComponent.renderMode = RenderMode.GPS
                } else {
                    locationComponent.cameraMode = CameraMode.NONE
                    locationComponent.renderMode = RenderMode.COMPASS
                }
            }
        } else {
            map.locationComponent.isLocationComponentEnabled = false
        }
    }

    LaunchedEffect(viewModel.isFullscreenMap) {
        if (viewModel.isFullscreenMap) {
            if (viewModel.dropOffLocation.isNotEmpty()) {
                voiceNavManager.announceTripStart(viewModel.dropOffLocation)
            }
            voiceNavManager.setEnabled(true)
        } else {
            voiceNavManager.setEnabled(false)
        }
    }

    // Update markers and drivers
    LaunchedEffect(mapLibreMap, viewModel.drivers, viewModel.orderStatusData) {
        val map = mapLibreMap ?: return@LaunchedEffect
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

        @Suppress("DEPRECATION")
        val currentDriverIds = driversToShow.map { it.id }.toSet()
        val iterator = activeMarkers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.key !in currentDriverIds) {
                map.removeMarker(entry.value)
                iterator.remove()
            }
        }

        driversToShow.forEach { driver ->
            val id = driver.id
            val marker = activeMarkers[id]
            val endPos = LatLng(driver.latitude, driver.longitude)
            val bearing = driver.bearing

            val vehicleType = driver.vehicleType?.lowercase() ?: ""
            val baseBitmap = if (vehicleType.contains("okada") || vehicleType.contains("bike") || vehicleType.contains("motorcycle") || vehicleType.contains("rider") || vehicleType.contains("motorbike") || vehicleType.contains("motor")) {
                motorbikeBitmap ?: null // Wait for network bitmap, don't use car fallback
            } else {
                ContextCompat.getDrawable(context, R.drawable.ic_car_saloon)?.toBitmap()
            }

            val carIcon = baseBitmap?.let { 
                val matrix = android.graphics.Matrix()
                matrix.postRotate(bearing)
                val scaled = if (it.width != 40) Bitmap.createScaledBitmap(it, 40, 40, false) else it
                val rotated = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
                IconFactory.getInstance(context).fromBitmap(rotated) 
            }

            if (marker == null) {
                @Suppress("DEPRECATION")
                val newMarker = map.addMarker(MarkerOptions()
                    .position(endPos)
                    .apply { if (carIcon != null) icon(carIcon) }
                )
                activeMarkers[id] = newMarker
            } else {
                if (carIcon != null) marker.icon = carIcon
                
                if (!animatingMarkerIds.contains(id)) {
                    val startPos = marker.position
                    val distanceSq = (startPos.latitude - endPos.latitude) * (startPos.latitude - endPos.latitude) + 
                                   (startPos.longitude - endPos.longitude) * (startPos.longitude - endPos.longitude)
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
        // Clean up
        activePolylineRef.value?.let { map.removePolyline(it) }
        activePolylineRef.value = null
        activeRouteMarkers.forEach { map.removeMarker(it) }
        activeRouteMarkers.clear()

        if (viewModel.polylinePoints.isNotEmpty()) {
            val polyline = map.addPolyline(PolylineOptions()
                .addAll(viewModel.polylinePoints)
                .color(FamekoBlue.toArgb())
                .width(6f)
            )
            activePolylineRef.value = polyline
            
            val pLat = if (viewModel.activeServiceMode == ServiceType.RENTAL) {
                viewModel.rentalPickupLat ?: viewModel.pickupLat
            } else {
                viewModel.pickupLat
            }
            val pLng = if (viewModel.activeServiceMode == ServiceType.RENTAL) {
                viewModel.rentalPickupLng ?: viewModel.pickupLng
            } else {
                viewModel.pickupLng
            }
            
            val pickupPos = LatLng(pLat ?: 0.0, pLng ?: 0.0)
            val dropoffPos = LatLng(viewModel.dropOffLat ?: 0.0, viewModel.dropOffLng ?: 0.0)
            
            if (pickupPos.latitude != 0.0) {
                val m = map.addMarker(MarkerOptions()
                    .position(pickupPos)
                    .title("PICKUP")
                    .snippet("${viewModel.pickupEtaMin?.toInt() ?: 5} min")
                )
                activeRouteMarkers.add(m)
            }
            if (dropoffPos.latitude != 0.0) {
                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                val dropoffTime = sdf.format(Date(System.currentTimeMillis() + (viewModel.durationMin * 60000).toLong()))
                val m = map.addMarker(MarkerOptions()
                    .position(dropoffPos)
                    .title("DROPOFF")
                    .snippet(dropoffTime)
                )
                activeRouteMarkers.add(m)
            }
        }
    }

    // Auto-Zoom to fit route
    LaunchedEffect(viewModel.polylinePoints, mapLibreMap) {
        val status = viewModel.orderStatusData?.status
        val isOnTrip = status != null && status != "PENDING" && status != "CANCELLED" && status != "DELIVERED"
        
        // If on trip, we only zoom if the camera was recently reset or if it's the first route load
        if (isOnTrip && hasCentredOnLocation) {
            // Don't auto-zoom during trip to avoid annoying the user if they've manually panned
            return@LaunchedEffect
        }

        if (viewModel.polylinePoints.isNotEmpty() && mapLibreMap != null) {
            val validPoints = viewModel.polylinePoints.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            if (validPoints.isEmpty()) return@LaunchedEffect

            try {
                val boundsBuilder = org.maplibre.android.geometry.LatLngBounds.Builder()
                validPoints.forEach { boundsBuilder.include(it) }
                val bounds = boundsBuilder.build()
                mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
            } catch (e: Exception) {
                // Fallback to simple average if bounds building fails (e.g. single point)
                val centerLat = validPoints.map { it.latitude }.average()
                val centerLng = validPoints.map { it.longitude }.average()
                mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(centerLat, centerLng), 13.5))
            }
        }
    }

    // Manage Background Location Service for Self-Drive rentals
    LaunchedEffect(viewModel.activeRental) {
        val rental = viewModel.activeRental
        val isSelfDrive = rental?.let { it["is_self_drive"] == true || it["is_self_drive"] == "true" } ?: false
        val status = rental?.let { it["status"]?.toString()?.uppercase() ?: "" } ?: ""
        val vehicleId = rental?.let { (it["vehicle_id"] as? Double)?.toInt() ?: (it["vehicle_id"] as? Int) ?: 0 } ?: 0
        
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
    LaunchedEffect(currentSheetState, viewModel.polylinePoints, isActive) {
        if (currentSheetState == CustomerSheetState.SELECTING_SERVICE && viewModel.polylinePoints.isNotEmpty()) {
            while (isActive) {
                delay(30.seconds) // Refresh every 30 seconds
                viewModel.updateNearbyDrivers(viewModel.pickupLat ?: 0.0, viewModel.pickupLng ?: 0.0)
            }
        }
    }

    LaunchedEffect(currentSheetState, viewModel.isFullscreenMap) {
        if (viewModel.isFullscreenMap) {
            if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.PartiallyExpanded) {
                sheetScaffoldState.bottomSheetState.partialExpand()
            }
            return@LaunchedEffect
        }
        when (currentSheetState) {
            CustomerSheetState.LANDING -> {
                if (sheetScaffoldState.bottomSheetState.currentValue != SheetValue.Expanded) {
                    sheetScaffoldState.bottomSheetState.expand()
                }
            }
            CustomerSheetState.SEARCHING_FOR_DRIVER,
            CustomerSheetState.SELECTING_SERVICE,
            CustomerSheetState.RIDE_SCHEDULED,
            CustomerSheetState.TIMED_OUT -> {
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

    BottomSheetScaffold(
        scaffoldState = sheetScaffoldState,
            sheetPeekHeight = if (viewModel.isFullscreenMap) 0.dp
                             else if (currentSheetState == CustomerSheetState.LANDING) 140.dp
                             else if (currentSheetState == CustomerSheetState.IDLE) 120.dp
                             else if (viewModel.currentOrderId != null || viewModel.activeRental != null || currentSheetState == CustomerSheetState.SELECTING_SERVICE || currentSheetState == CustomerSheetState.PICKING_ADDRESS) 110.dp
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
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { focusManager.clearFocus() })
                    }
                ) {
                    when (currentSheetState) {
                        CustomerSheetState.LANDING -> {
                            CustomerLandingScreen(
                                activeRental = viewModel.activeRental,
                                onViewRental = { viewModel.navigateTo(CustomerScreen.RentalDetails(it)) },
                                onServiceSelected = { service ->
                                    viewModel.setServiceMode(service)
                                    if (service == ServiceType.RENTAL) {
                                        viewModel.navigateTo(CustomerScreen.FleetBrowse)
                                    } else {
                                        viewModel.navigateTo(CustomerScreen.MainMap)
                                    }
                                },
                                onScheduleClick = { showDatePicker = true },
                                recentPlaces = viewModel.recentPlaces,
                                onSearchClick = {
                                    viewModel.navigateTo(CustomerScreen.RouteSelection)
                                },
                                onPlaceClick = { suggestion ->
                                    viewModel.setServiceMode(ServiceType.RIDE_HAILING)
                                    viewModel.selectSuggestion(suggestion, isPickup = false)
                                    viewModel.navigateTo(CustomerScreen.MainMap)
                                }
                            )
                        }
                        CustomerSheetState.IDLE -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    QuickShortcutItem("Home", Icons.Default.Home, BoltGreen) { 
                                        if (viewModel.savedPlaces.value.any { it.label.equals("Home", true) }) {
                                            viewModel.applyShortcut("Home") {
                                                if (hasLocationPermission) {
                                                    @SuppressLint("MissingPermission")
                                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                                        location?.let { viewModel.useCurrentLocation(it, forPickup = true) }
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please set your Home address in Manage Places", Toast.LENGTH_SHORT).show()
                                            viewModel.navigateTo(CustomerScreen.ManagePlaces)
                                        }
                                    }
                                    QuickShortcutItem("Work", Icons.Default.Work, FamekoBlue) {
                                        if (viewModel.savedPlaces.value.any { it.label.equals("Work", true) }) {
                                            viewModel.applyShortcut("Work") {
                                                if (hasLocationPermission) {
                                                    @SuppressLint("MissingPermission")
                                                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                                                        location?.let { viewModel.useCurrentLocation(it, forPickup = true) }
                                                    }
                                                }
                                            }
                                        } else {
                                            Toast.makeText(context, "Please set your Work address in Manage Places", Toast.LENGTH_SHORT).show()
                                            viewModel.navigateTo(CustomerScreen.ManagePlaces)
                                        }
                                    }
                                    QuickShortcutItem("Recent", Icons.Default.History, Color.Gray) { 
                                        viewModel.navigateTo(CustomerScreen.RouteSelection)
                                        viewModel.selectedTab = 0
                                        viewModel.updateDropOffLocation("")
                                    }
                                    QuickShortcutItem("Saved", Icons.Default.Star, BoltYellow) { 
                                        viewModel.navigateTo(CustomerScreen.RouteSelection)
                                        viewModel.selectedTab = 1
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
                                        Text("Set Route for Rental", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    SearchBox(
                                        pickupLocation = viewModel.rentalPickupLocation.ifEmpty { viewModel.pickupLocation }, 
                                        dropOffLocation = viewModel.dropOffLocation,
                                        stops = viewModel.stops,
                                        onPickupChange = { viewModel.updatePickupLocation(it) },
                                        onDropOffChange = { viewModel.updateDropOffLocation(it) },
                                        onStopChange = { idx, value -> 
                                            viewModel.updateStopLocation(idx, value)
                                        },
                                        onAddStop = { if (viewModel.stops.size < 5) viewModel.stops = viewModel.stops + "" },
                                        onRemoveStop = { idx -> 
                                            val newStops = viewModel.stops.toMutableList()
                                            newStops.removeAt(idx)
                                            viewModel.stops = newStops
                                        },
                                        onPickupFocus = { if (it) { isPickupFocused = true; isDropOffFocused = false; viewModel.focusedStopIndex = -1 } }, 
                                        onDropOffFocus = { if (it) { isPickupFocused = false; isDropOffFocused = true; viewModel.focusedStopIndex = -1 } },
                                        onStopFocus = { idx, focused -> if (focused) { isPickupFocused = false; isDropOffFocused = false; viewModel.focusedStopIndex = idx } },
                                        onSearch = { 
                                            focusManager.clearFocus()
                                            if (viewModel.pickupLat != null || viewModel.rentalPickupLat != null) {
                                                viewModel.calculateRoute()
                                            }
                                        },
                                        isLoading = viewModel.isLoading, 
                                        pickupFocusRequester = pickupFocusRequester, 
                                        dropOffFocusRequester = dropOffFocusRequester, 
                                        isRentalMode = true
                                    )

                                    val suggestions = when {
                                        isPickupFocused -> viewModel.pickupSuggestions
                                        isDropOffFocused -> viewModel.dropOffSuggestions
                                        viewModel.focusedStopIndex != -1 -> viewModel.stopSuggestions
                                        else -> emptyList()
                                    }

                                    if (suggestions.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                            items(suggestions) { suggestion ->
                                                LocationSuggestionItem(suggestion) {
                                                    when {
                                                        isPickupFocused -> viewModel.selectSuggestion(suggestion, true)
                                                        isDropOffFocused -> viewModel.selectSuggestion(suggestion, false)
                                                        viewModel.focusedStopIndex != -1 -> viewModel.selectStopSuggestion(viewModel.focusedStopIndex, suggestion)
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                                onScheduleClick = { showDatePicker = true },
                                isPlacing = viewModel.isOrderPlacing,
                                onUnavailableClick = { _ ->
                                    // Temporarily disabled
                                }
                            )
                        }
                        CustomerSheetState.SEARCHING_FOR_DRIVER -> {
                            SearchingSheetContent(viewModel = viewModel, onCancel = { viewModel.showCancelConfirmation = true })
                        }
                        CustomerSheetState.ON_TRIP -> {
                            viewModel.orderStatusData?.let { data ->
                                viewModel.currentOrderId?.let { id ->
                                    DriverInfoSheetContent(
                                        data = data,
                                        orderId = id,
                                        onNavigateToChat = onNavigateToChat,
                                        onCancel = { viewModel.showCancelConfirmation = true },
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
                                    viewModel = viewModel,
                                    onDetailsClick = { viewModel.navigateTo(CustomerScreen.RentalDetails(rental)) },
                                    onUpdateRoute = { 
                                        viewModel.calculateRoute()
                                    },
                                    onStartNavigation = {
                                        val lat = viewModel.dropOffLat
                                        val lng = viewModel.dropOffLng
                                        val address = viewModel.dropOffLocation
                                        if (lat != null && lng != null && lat != 0.0) {
                                            if (hasLocationPermission) {
                                                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { location ->
                                                    location?.let {
                                                        if (it.latitude != 0.0 && it.longitude != 0.0) {
                                                            viewModel.pickupLat = it.latitude
                                                            viewModel.pickupLng = it.longitude
                                                            viewModel.dropOffLat = lat
                                                            viewModel.dropOffLng = lng
                                                            viewModel.dropOffLocation = address
                                                            viewModel.isFullscreenMap = true
                                                            viewModel.calculateRoute()
                                                        }
                                                    }
                                                }
                                            } else {
                                                viewModel.isFullscreenMap = true
                                                viewModel.calculateRoute()
                                            }
                                        } else {
                                            Toast.makeText(context, "Please set a destination first", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                        CustomerSheetState.RIDE_SCHEDULED -> {
                            ScheduledRideSheetContent(
                                onCancel = { viewModel.showCancelConfirmation = true },
                                onClose = { viewModel.navigateTo(CustomerScreen.Landing) }
                            )
                        }
                        CustomerSheetState.TIMED_OUT -> {
                            TimedOutSheetContent(
                                onRetry = { viewModel.clearActiveOrder() },
                                onClose = { 
                                    viewModel.clearActiveOrder()
                                    viewModel.navigateTo(CustomerScreen.Landing) 
                                }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                val polylinePoints = viewModel.polylinePoints
                val drivers = viewModel.drivers
                val orderStatusData = viewModel.orderStatusData
                
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
                                            locationComponent.activateLocationComponent(
                                                LocationComponentActivationOptions.builder(context, style).build()
                                            )
                                        }
                                        locationComponent.isLocationComponentEnabled = true
                                        locationComponent.renderMode = RenderMode.COMPASS
                                    }
                                }

                                map.addOnMapClickListener {
                                    focusManager.clearFocus()
                                    false
                                }
                            }
                        }
                    }
                )

                if (viewModel.isFullscreenMap) {
                    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().padding(16.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            shadowElevation = 4.dp,
                            modifier = Modifier.size(48.dp).clickable { 
                                viewModel.isFullscreenMap = false
                                viewModel.polylinePoints = emptyList()
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = BoltDark)
                            }
                        }
                        
                        Card(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Navigation, null, tint = BoltGreen, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Navigating to", fontSize = 12.sp, color = Color.Gray)
                                    Text(viewModel.dropOffLocation, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${viewModel.durationMin.toInt()} min", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BoltGreen)
                                    Text("ETA", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Immersive Floating Search Bar & Controls
                if (viewModel.currentOrderId == null && currentSheetState != CustomerSheetState.LANDING && !viewModel.isFullscreenMap) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                            .statusBarsPadding()
                    ) {
                        // Floating Search Experience
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp)
                                .clickable { 
                                    if (currentSheetState == CustomerSheetState.SELECTING_SERVICE) {
                                        viewModel.resetSearch()
                                    } else {
                                        viewModel.navigateTo(CustomerScreen.RouteSelection)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 8.dp,
                            border = BorderStroke(1.dp, BoltLightGray)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            ) {
                                IconButton(onClick = { 
                                    if (viewModel.activeRental != null) {
                                        viewModel.navigateTo(CustomerScreen.Landing)
                                    } else {
                                        viewModel.resetSearch()
                                        viewModel.navigateTo(CustomerScreen.Landing)
                                    }
                                }) {
                                    Icon(Icons.Default.Close, null, tint = BoltDark, modifier = Modifier.size(24.dp))
                                }
                                
                                val startLabel = if (viewModel.activeServiceMode == ServiceType.RENTAL) {
                                    viewModel.rentalPickupLocation.ifEmpty { viewModel.pickupLocation }
                                } else {
                                    viewModel.pickupLocation
                                }

                                Text(
                                    text = if (viewModel.dropOffLocation.isNotEmpty() && startLabel.isNotEmpty()) {
                                        "${startLabel.split(",").first()} → ${viewModel.dropOffLocation.split(",").first()}"
                                    } else if (startLabel.isNotEmpty()) {
                                        "From ${startLabel.split(",").first()}"
                                    } else "Where to?",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (viewModel.dropOffLocation.isNotEmpty() || startLabel.isNotEmpty()) FamekoBlue else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                
                                IconButton(onClick = { viewModel.navigateTo(CustomerScreen.RouteSelection) }) {
                                    Icon(Icons.Default.Add, null, tint = BoltDark, modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                // Map Utility Buttons (Recenter, Zoom)
                if (!viewModel.isFullscreenMap && (currentSheetState == CustomerSheetState.IDLE || currentSheetState == CustomerSheetState.LANDING || currentSheetState == CustomerSheetState.SELECTING_SERVICE || currentSheetState == CustomerSheetState.PICKING_ADDRESS)) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 160.dp, end = 16.dp), // Pushed up above the bottom sheet
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FloatingActionButton(
                            onClick = { 
                                hasLocationPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                if (hasLocationPermission) {
                                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                                        loc?.let { mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude))) }
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
                        driverProfilePic = viewModel.ratingDriverPic,
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

                if (viewModel.showCancelConfirmation) {
                    var selectedReason by remember { mutableStateOf("") }
                    val reasons = listOf(
                        "Driver is too far away",
                        "Wait time is too long",
                        "Changed my mind",
                        "Order placed by mistake",
                        "Incorrect pickup location",
                        "Driver asked me to cancel",
                        "Other"
                    )

                    AlertDialog(
                        onDismissRequest = { viewModel.showCancelConfirmation = false },
                        title = { Text("Cancel Ride?", fontWeight = FontWeight.Bold) },
                        text = {
                            Column {
                                Text("Please tell us why you're cancelling:")
                                Spacer(Modifier.height(12.dp))
                                reasons.forEach { reason ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { selectedReason = reason }
                                            .padding(vertical = 4.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedReason == reason,
                                            onClick = { selectedReason = reason },
                                            colors = RadioButtonDefaults.colors(selectedColor = BoltGreen)
                                        )
                                        Text(text = reason, modifier = Modifier.padding(start = 8.dp))
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.showCancelConfirmation = false
                                    viewModel.cancelOrder(selectedReason.ifEmpty { "Not specified" })
                                },
                                enabled = selectedReason.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Confirm Cancellation")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { viewModel.showCancelConfirmation = false }) {
                                Text("No, Keep Ride")
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        containerColor = Color.White
                    )
                }

                if (showDatePicker) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                showDatePicker = false
                                showTimePicker = true
                            }) { Text("Next") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                if (showTimePicker) {
                    AlertDialog(
                        onDismissRequest = { showTimePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                val dateStr = datePickerState.selectedDateMillis?.let {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
                                } ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                
                                val timeStr = "%02d:%02d".format(timePickerState.hour, timePickerState.minute)
                                val scheduledTime = "$dateStr $timeStr"
                                
                                viewModel.updateScheduledRideTime(scheduledTime)
                                viewModel.setServiceMode(ServiceType.RIDE_HAILING)
                                viewModel.navigateTo(CustomerScreen.MainMap)
                                viewModel.isSearchMode = true
                                showTimePicker = false
                            }) { Text("Confirm") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showTimePicker = false; showDatePicker = true }) { Text("Back") }
                        },
                        text = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Select Time", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                                TimePicker(state = timePickerState)
                            }
                        }
                    )
                }

                viewModel.showRegionalError?.let { error ->
                    AlertDialog(
                        onDismissRequest = { viewModel.showRegionalError = null },
                        title = { Text("Service Unavailable") },
                        text = { Text(error) },
                        confirmButton = {
                            Button(onClick = { viewModel.showRegionalError = null }) {
                                Text("OK")
                            }
                        },
                        shape = RoundedCornerShape(24.dp)
                    )
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
    driverProfilePic: String? = null,
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
                    if (!driverProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = driverProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                    }
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
    viewModel: CustomerMapViewModel,
    onDetailsClick: () -> Unit,
    onUpdateRoute: () -> Unit,
    onStartNavigation: () -> Unit
) {
    val isUnlocked = rental["is_unlocked"] == true
    val isSelfDrive = rental["is_self_drive"] == true || rental["is_self_drive"] == "true"
    val bookingCode = rental["booking_code"]?.toString() ?: "----"
    
    val pickupFocusRequester = remember { FocusRequester() }
    val dropOffFocusRequester = remember { FocusRequester() }
    
    var isPickupFocused by remember { mutableStateOf(false) }
    var isDropOffFocused by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isSelfDrive) "Self-Drive Active" else "Active Rental", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FamekoBlue)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDetailsClick) {
                Text("Details", fontWeight = FontWeight.Bold, color = FamekoBlue)
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(12.dp).padding(start = 4.dp), FamekoBlue)
            }
        }
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
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

        if (viewModel.polylinePoints.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStartNavigation,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
            ) {
                Icon(Icons.Default.Navigation, null)
                Spacer(Modifier.width(12.dp))
                Text("Start Navigation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun TimedOutSheetContent(onRetry: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Icon(Icons.Default.Error, null, tint = BoltOrange, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("No Driver Found", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Sorry, all drivers are currently busy in your area. Please try again in a few minutes.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
            ) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScheduledRideSheetContent(onCancel: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Icon(Icons.Default.CheckCircle, null, tint = BoltGreen, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Ride Scheduled!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Your driver will be assigned closer to your pickup time.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Cancel Ride", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoltDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SearchingSheetContent(viewModel: CustomerMapViewModel, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) { 
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = BoltGreen)
        Spacer(Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Finding your driver...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (viewModel.retryAttempt > 0) {
                Spacer(Modifier.width(8.dp))
                Badge(containerColor = BoltGreen, contentColor = Color.White) {
                    Text("Retry ${viewModel.retryAttempt}/${viewModel.maxRetryAttempts}", modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
        
        Text(viewModel.searchMessage, color = Color.Gray, fontSize = 14.sp)
        if (viewModel.retryAttempt > 0) {
            Text("Search radius: ${String.format("%.1f", viewModel.searchRadiusKm)} km", color = BoltGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

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
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    if (!data.driverProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = data.driverProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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
                    modifier = Modifier.size(44.dp).background(FamekoGold.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Call, null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { onNavigateToChat(orderId, data.driverName ?: "Driver") },
                    modifier = Modifier.size(44.dp).background(FamekoGold.copy(alpha = 0.2f), CircleShape)
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.driverVehicleNumber ?: "PLATE NUMBER",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        val phone = data.driverPhone
                        if (!phone.isNullOrEmpty()) {
                            Text(" • ", color = Color.Gray)
                            Text(
                                text = phone,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
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
                border = BorderStroke(1.dp, Color.LightGray)
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
fun SearchBox(
    pickupLocation: String, 
    dropOffLocation: String, 
    stops: List<String> = emptyList(),
    onPickupChange: (String) -> Unit, 
    onDropOffChange: (String) -> Unit, 
    onStopChange: (Int, String) -> Unit = { _, _ -> },
    onAddStop: () -> Unit = {},
    onRemoveStop: (Int) -> Unit = {},
    onPickupFocus: (Boolean) -> Unit = {}, 
    onDropOffFocus: (Boolean) -> Unit = {}, 
    onStopFocus: (Int, Boolean) -> Unit = { _, _ -> },
    onSearch: () -> Unit = {}, 
    isLoading: Boolean = false, 
    pickupFocusRequester: FocusRequester = remember { FocusRequester() }, 
    dropOffFocusRequester: FocusRequester = remember { FocusRequester() }, 
    isRentalMode: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, BoltLightGray)
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
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BoltDark),
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
                        textStyle = TextStyle(fontSize = 16.sp, color = BoltDark),
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
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BoltDark),
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
    onScheduleClick: () -> Unit,
    isPlacing: Boolean,
    onUnavailableClick: (String) -> Unit = {}
) {
    val filteredEstimates = estimates.filter { estimate ->
        when (activeServiceMode) {
            ServiceType.RIDE_HAILING -> estimate.serviceType == "RIDE_HAILING"
            ServiceType.PACKAGE_DELIVERY -> estimate.serviceType == "PACKAGE_DELIVERY"
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (discountRate > 0) {
            Surface(
                color = FamekoBlue,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth().offset(y = (-8).dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("$discountRate% promo applied", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
            }
        }
        
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
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 400.dp, min = if (filteredEstimates.isEmpty()) 100.dp else 0.dp)
        ) {
            if (filteredEstimates.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FamekoBlue)
                    }
                }
            } else {
                items(filteredEstimates) { estimate ->
                    ServiceItem(
                        estimate = estimate,
                        isSelected = selectedType == estimate.serviceId,
                        onSelect = { onTypeSelected(estimate.serviceId, estimate.fare) },
                        discountRate = discountRate,
                        onUnavailableClick = onUnavailableClick
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Money, null, tint = BoltGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cash", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BoltDark)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
                enabled = !isPlacing && filteredEstimates.isNotEmpty()
            ) {
                if (isPlacing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Select $selectedType", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            Box {
                FloatingActionButton(
                    onClick = onScheduleClick,
                    containerColor = FamekoBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(28.dp))
                }
                
                // Tooltip mock
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-45).dp)
                ) {
                    Text("Schedule a ride", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}



@Composable
fun ServiceItem(
    estimate: RideEstimateResponse,
    isSelected: Boolean,
    onSelect: () -> Unit,
    discountRate: Int,
    onUnavailableClick: (String) -> Unit = {}
) {
    val isAvailable = estimate.isAvailableInRegion
    val status = estimate.availabilityStatus
    val backgroundColor = if (isSelected) Color.Transparent else Color.Transparent
    
    val finalFare = (estimate.fare * (100 - discountRate) / 100).toInt()
    val originalFare = estimate.fare.toInt()

    Surface(
        onClick = {
            if (isAvailable) {
                onSelect()
            } else {
                onUnavailableClick(estimate.name)
            }
        },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(2.dp, FamekoBlue) else null,
        modifier = Modifier.fillMaxWidth(),
        enabled = true
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).alpha(if (isAvailable && status != "BUSY") 1f else 0.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val iconUrl = estimate.icon.lowercase().let { lowIcon ->
                    when {
                        lowIcon.contains("okada") || lowIcon.contains("motorcycle") || lowIcon.contains("rider") || lowIcon.contains("bike") || lowIcon.contains("motorbike") || lowIcon.contains("motor") -> ImageLinks.IC_OKADA
                        lowIcon.contains("pragya") -> ImageLinks.IC_PRAGYA
                        lowIcon.contains("aboboyaa") -> ImageLinks.IC_ABOBOYAA
                        lowIcon.contains("truck") -> ImageLinks.IC_TRUCK
                        lowIcon.contains("bicycle") -> ImageLinks.IC_BICYCLE
                        else -> ImageLinks.IC_CAR_SALOON
                    }
                }
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(estimate.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
                    if (estimate.name == "Comfort") {
                         Icon(Icons.Default.KeyboardDoubleArrowUp, null, tint = Color.Gray, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${estimate.pickupEtaMin} min", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(" 4", fontSize = 13.sp, color = Color.Gray)
                }
                
                if (estimate.pickupEtaMin <= 5) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = FamekoBlue,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("FASTER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("GH₵$finalFare", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BoltDark)
                if (discountRate > 0) {
                    Text(
                        text = "GH₵$originalFare",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
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

@Suppress("DEPRECATION")
fun animateMarker(marker: Marker, startPos: LatLng, endPos: LatLng, duration: Long = 1500, onEnd: () -> Unit = {}) {
    val handler = Handler(Looper.getMainLooper())
    val start = SystemClock.uptimeMillis()
    val interpolator = LinearInterpolator()

    handler.post(object : Runnable {
        override fun run() {
            val elapsed = SystemClock.uptimeMillis() - start
            val t = kotlin.math.min(1f, interpolator.getInterpolation(elapsed.toFloat() / duration))

            val lat = t * endPos.latitude + (1 - t) * startPos.latitude
            val lng = t * endPos.longitude + (1 - t) * startPos.longitude

            try {
                marker.position = LatLng(lat, lng)
            } catch (_: Exception) {}

            if (t < 1f) {
                handler.postDelayed(this, 16)
            } else {
                onEnd()
            }
        }
    })
}

@Composable
fun SaveLocationSearchScreen(
    label: String,
    placeId: String? = null,
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf(label) }
    val suggestions = viewModel.dropOffSuggestions
    val recentPlaces = viewModel.recentPlaces
    val isDefaultLabel = label == "Home" || label == "Work"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                text = if (placeId != null) "Edit $label" else "Set $label",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (!isDefaultLabel) {
            OutlinedTextField(
                value = customLabel,
                onValueChange = { customLabel = it },
                label = { Text("Name this place") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.updateDropOffLocation(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search location", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { 
                    IconButton(onClick = { 
                        searchQuery = ""
                        viewModel.updateDropOffLocation("")
                    }) { 
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray) 
                    } 
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FamekoBlue,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = BoltLightGray,
                unfocusedContainerColor = BoltLightGray
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val listToUse = if (searchQuery.isEmpty()) recentPlaces else suggestions
            
            items(listToUse) { suggestion ->
                LocationSearchItem(
                    suggestion = suggestion,
                    onClick = {
                        if (placeId != null) {
                            viewModel.updateSavedPlace(placeId, customLabel, suggestion)
                        } else {
                            viewModel.savePlace(suggestion, customLabel)
                        }
                        onBack()
                    }
                )
                HorizontalDivider(color = BoltLightGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun LocationSearchItem(suggestion: LocationSuggestion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (suggestion.type == "recent") Icons.Default.History else Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name ?: suggestion.displayName.split(",").first(),
                fontWeight = FontWeight.Medium,
                color = BoltDark
            )
            Text(
                text = suggestion.displayName,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        // Distance placeholder if needed
    }
}

@Composable
fun RouteSelectionScreen(
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit,
    onMapClick: () -> Unit = {},
    onComplete: () -> Unit = onBack
) {
    val focusManager = LocalFocusManager.current
    val pickupFocusRequester = remember { FocusRequester() }
    val dropOffFocusRequester = remember { FocusRequester() }
    
    var isPickupFocused by remember { mutableStateOf(true) }
    var isDropOffFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.pickupLocation.isEmpty()) {
            pickupFocusRequester.requestFocus()
        } else if (viewModel.dropOffLocation.isEmpty()) {
            dropOffFocusRequester.requestFocus()
        } else {
            dropOffFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Route",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search Box Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BoltLightGray)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Pickup Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (isPickupFocused) BoltGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .border(2.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = viewModel.pickupLocation,
                            onValueChange = { viewModel.updatePickupLocation(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(pickupFocusRequester)
                                .onFocusChanged { isPickupFocused = it.isFocused },
                            textStyle = TextStyle(fontSize = 16.sp, color = BoltDark, fontWeight = FontWeight.Medium),
                            decorationBox = { innerTextField ->
                                if (viewModel.pickupLocation.isEmpty()) {
                                    Text("Pickup location", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )

                        if (viewModel.pickupLocation.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updatePickupLocation("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (isPickupFocused) {
                            Surface(
                                modifier = Modifier.clickable { onMapClick() },
                                color = FamekoBlue,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Map", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stops
                    viewModel.stops.forEachIndexed { index, stop ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Gray))
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = stop,
                                onValueChange = { newValue ->
                                    viewModel.updateStopLocation(index, newValue)
                                },
                                modifier = Modifier.weight(1f).onFocusChanged { 
                                    if (it.isFocused) {
                                        isPickupFocused = false
                                        isDropOffFocused = false
                                        viewModel.focusedStopIndex = index
                                    }
                                },
                                textStyle = TextStyle(fontSize = 14.sp, color = BoltDark),
                                decorationBox = { innerTextField ->
                                    if (stop.isEmpty()) Text("Stop ${index + 1}", color = Color.Gray, fontSize = 14.sp)
                                    innerTextField()
                                },
                                singleLine = true
                            )
                            IconButton(onClick = {
                                val newStops = viewModel.stops.toMutableList()
                                newStops.removeAt(index)
                                viewModel.stops = newStops
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (viewModel.stops.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))

                    // Dropoff Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (isDropOffFocused) BoltGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = BoltDark
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = viewModel.dropOffLocation,
                            onValueChange = { viewModel.updateDropOffLocation(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(dropOffFocusRequester)
                                .onFocusChanged { isDropOffFocused = it.isFocused },
                            textStyle = TextStyle(fontSize = 16.sp, color = BoltDark, fontWeight = FontWeight.Medium),
                            decorationBox = { innerTextField ->
                                if (viewModel.dropOffLocation.isEmpty()) {
                                    Text("Dropoff location", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )
                        
                        if (viewModel.dropOffLocation.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateDropOffLocation("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        if (isDropOffFocused) {
                            Surface(
                                modifier = Modifier.clickable { onMapClick() },
                                color = FamekoBlue,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Map", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Action Icons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { if (viewModel.stops.size < 5) viewModel.stops += "" }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, null, tint = BoltDark)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = {
                        val tempLoc = viewModel.pickupLocation
                        val tempLat = viewModel.pickupLat
                        val tempLng = viewModel.pickupLng
                        
                        viewModel.pickupLocation = viewModel.dropOffLocation
                        viewModel.pickupLat = viewModel.dropOffLat
                        viewModel.pickupLng = viewModel.dropOffLng
                        
                        viewModel.dropOffLocation = tempLoc
                        viewModel.dropOffLat = tempLat
                        viewModel.dropOffLng = tempLng
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.SwapVert, null, tint = BoltDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggestions List
            val suggestions = when {
                isPickupFocused -> viewModel.pickupSuggestions
                isDropOffFocused -> viewModel.dropOffSuggestions
                viewModel.focusedStopIndex != -1 -> viewModel.stopSuggestions
                else -> emptyList()
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val currentText = when {
                    isPickupFocused -> viewModel.pickupLocation
                    isDropOffFocused -> viewModel.dropOffLocation
                    viewModel.focusedStopIndex != -1 -> viewModel.stops.getOrElse(viewModel.focusedStopIndex) { "" }
                    else -> ""
                }
                
                if (currentText.isEmpty()) {
                    // ... My Location item ...
                }

                items(suggestions) { suggestion ->
                    LocationSuggestionItem(suggestion) {
                        when {
                            isPickupFocused -> viewModel.selectSuggestion(suggestion, true)
                            isDropOffFocused -> viewModel.selectSuggestion(suggestion, false)
                            viewModel.focusedStopIndex != -1 -> viewModel.selectStopSuggestion(viewModel.focusedStopIndex, suggestion)
                        }
                        val pLat = if (viewModel.activeServiceMode == ServiceType.RENTAL) viewModel.rentalPickupLat ?: viewModel.pickupLat else viewModel.pickupLat
                        if (pLat != null && viewModel.dropOffLat != null) {
                            viewModel.calculateRoute()
                            onComplete()
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }

        // Removed Confirm Button as per request
    }
}

@Composable
fun LocationSuggestionItem(
    suggestion: LocationSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BoltLightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (suggestion.type == "recent") Icons.Default.History else Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name ?: suggestion.displayName.split(",").firstOrNull() ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = suggestion.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Random distance to match image
        val dist = remember { "${(1..15).random()}.${(0..9).random()} km" }
        Text(
            text = dist,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}

