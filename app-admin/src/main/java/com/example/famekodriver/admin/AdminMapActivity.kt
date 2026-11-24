package com.example.famekodriver.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.famekodriver.core.utils.ImageLinks
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalView
import android.net.Uri
import com.example.famekodriver.core.network.NetworkClient
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.famekodriver.admin.ui.AdminDashboardScreen
import com.example.famekodriver.admin.ui.theme.FamekoPrimary
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.SOSAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.*

sealed class AdminScreen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Dashboard : AdminScreen("dashboard", Icons.Default.Dashboard, "Dashboard")
    object Map : AdminScreen("map", Icons.Default.Map, "Live Map")
    object Approvals : AdminScreen("approvals", Icons.Default.CheckCircle, "Approvals")
    object Customers : AdminScreen("customers", Icons.Default.Group, "Customers")
    object Deliveries : AdminScreen("deliveries", Icons.Default.LocalShipping, "Orders")
    object DbExplorer : AdminScreen("db_explorer", Icons.Default.Storage, "DB Audit")
    object SOS : AdminScreen("sos", Icons.Default.Warning, "SOS")
    object Rentals : AdminScreen("rentals", Icons.Default.CarRental, "Fleet")
    object DriverDetails : AdminScreen("driver_details", Icons.Default.Person, "Driver Details")
    object Financials : AdminScreen("financials", Icons.Default.PieChart, "Financials")
    data class SupportChat(val driverId: Int, val driverName: String, val driverPhone: String = "", val orderId: Int = 0) : AdminScreen("support_chat", Icons.AutoMirrored.Filled.Chat, "Chat")
}

class AdminMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize MapLibre Vector Map Engine
        MapLibre.getInstance(this)

        // Start SOS Monitoring Service
        val serviceIntent = Intent(this, AdminSOSService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        enableEdgeToEdge()
        setContent {
            AdminMainContent()
        }
    }
}

@Composable
fun AdminMainContent() {
    var currentScreen by remember { mutableStateOf<AdminScreen>(AdminScreen.Dashboard) }
    var selectedDriverId by remember { mutableStateOf<Int?>(null) }
    var targetMapLocation by remember { mutableStateOf<LatLng?>(null) }
    val adminViewModel: AdminViewModel = viewModel()
    val context = LocalContext.current
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = FamekoPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val screens = listOf(
                    AdminScreen.Dashboard, 
                    AdminScreen.Map, 
                    AdminScreen.Approvals, 
                    AdminScreen.Deliveries,
                    AdminScreen.DbExplorer,
                    AdminScreen.SOS
                )
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label, fontSize = 10.sp) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = FamekoPrimary,
                            selectedTextColor = FamekoPrimary,
                            indicatorColor = FamekoPrimary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (currentScreen) {
                AdminScreen.Dashboard -> AdminDashboardScreen(
                    viewModel = adminViewModel,
                    onNavigateToCustomers = { currentScreen = AdminScreen.Customers },
                    onNavigateToFleet = { currentScreen = AdminScreen.Rentals }
                )
                AdminScreen.Map -> AdminMapScreen(viewModel = adminViewModel, targetLocation = targetMapLocation)
                AdminScreen.Approvals -> AdminApprovalsScreen(
                    viewModel = adminViewModel,
                    onViewDriver = { id -> 
                        selectedDriverId = id
                        currentScreen = AdminScreen.DriverDetails
                    }
                )
                AdminScreen.Deliveries -> AdminDeliveriesScreen(viewModel = adminViewModel)
                AdminScreen.DbExplorer -> AdminDbExplorerScreen(viewModel = adminViewModel)
                AdminScreen.SOS -> AdminSOSScreen(
                    viewModel = adminViewModel,
                    onLocate = { driverId, alertLat, alertLng ->
                        // Prefer live location if available
                        val liveDriver = adminViewModel.liveDrivers.find { it["id"].toString() == driverId }
                        val targetLat = (liveDriver?.get("lat") as? Double) ?: alertLat
                        val targetLng = (liveDriver?.get("lng") as? Double) ?: alertLng
                        
                        if (targetLat != 0.0) {
                            targetMapLocation = LatLng(targetLat, targetLng)
                            currentScreen = AdminScreen.Map
                        } else {
                            Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onChat = { dId, name, phone ->
                        currentScreen = AdminScreen.SupportChat(dId, name, phone)
                    }
                )
                AdminScreen.Customers -> AdminCustomersScreen(viewModel = adminViewModel)
                AdminScreen.Rentals -> AdminRentalsScreen(viewModel = adminViewModel)
                AdminScreen.DriverDetails -> AdminDriverDetailsScreen(
                    driverId = selectedDriverId ?: 0,
                    viewModel = adminViewModel,
                    onNavigateToChat = { dId, name, phone ->
                        currentScreen = AdminScreen.SupportChat(dId, name, phone)
                    },
                    onBack = { currentScreen = AdminScreen.Approvals }
                )
                AdminScreen.Financials -> AdminFinancialsScreen(viewModel = adminViewModel)
                is AdminScreen.SupportChat -> {
                    val chat = currentScreen as AdminScreen.SupportChat
                    AdminSupportChatScreen(
                        driverId = chat.driverId,
                        driverName = chat.driverName,
                        driverPhone = chat.driverPhone,
                        orderId = chat.orderId,
                        onBack = { currentScreen = AdminScreen.Dashboard }
                    )
                }
            }
        }
    }
}

class AdminViewModel : ViewModel() {
    private val repository = DriverRepository.getInstance()
    
    var liveDrivers by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set

    suspend fun getPlatformStats() = repository.getAdminPlatformStats()
    
    suspend fun refreshLiveLocations() {
        repository.getAdminLiveLocations().onSuccess {
            liveDrivers = it
        }
    }

    suspend fun getLiveLocations() = repository.getAdminLiveLocations()
    suspend fun getActiveSOS() = repository.getAdminActiveSOS()
    suspend fun getPendingDrivers() = repository.getAdminPendingDrivers()
    suspend fun getAdminCustomers() = repository.getAdminCustomers()
    suspend fun getAdminDeliveries() = repository.getAdminDeliveries()
    suspend fun getAdminRentals() = repository.getAdminRentals()
    suspend fun getAdminFinancials() = repository.getAdminFinancials()
    suspend fun getAdminDriverDetails(id: Int) = repository.getAdminDriverDetails(id)
    suspend fun getPendingPayments() = repository.getAdminPendingPayments()
    suspend fun approveDriver(id: Int) = repository.approveDriver(id)
    suspend fun rejectDriver(id: Int) = repository.rejectDriver(id)
    suspend fun approvePayment(id: Int) = repository.approvePayment(id)
    suspend fun rejectPayment(id: Int) = repository.rejectPayment(id)
    suspend fun resolveSOS(id: Int) = repository.resolveSOS(id)
    suspend fun getCurrentWeather(lat: Double, lon: Double) = repository.getCurrentWeather(lat, lon)
    suspend fun getAdminDbTables() = repository.getAdminDbTables()
    suspend fun getAdminTableData(tableName: String) = repository.getAdminTableData(tableName)
}

@Composable
fun AdminMapScreen(
    viewModel: AdminViewModel = viewModel(),
    targetLocation: LatLng? = null,
    onInspectDriver: (Int) -> Unit = {}
) {
    var weather by remember { mutableStateOf<com.example.famekodriver.core.domain.model.WeatherResponse?>(null) }
    var lastWeatherUpdate by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    val mapView = remember { org.maplibre.android.maps.MapView(context) }
    var mapLibreMap by remember { mutableStateOf<org.maplibre.android.maps.MapLibreMap?>(null) }
    var selectedDriver by remember { mutableStateOf<Map<String, Any?>?>(null) }

    DisposableEffect(lifecycle, mapView) {
        mapView.onCreate(null)
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    var motorbikeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var carBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(Unit) {
        val localMotorDrawable = ContextCompat.getDrawable(context, R.drawable.ic_okada)
        val resultMotor = localMotorDrawable?.toBitmap() ?: run {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context).data(ImageLinks.IC_OKADA).build()
            (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        }
        if (resultMotor != null) {
            motorbikeBitmap = Bitmap.createScaledBitmap(resultMotor, 32, 32, false)
        }

        val resultCar = run {
            val loader = context.imageLoader
            val request = ImageRequest.Builder(context).data(ImageLinks.IC_CAR_SALOON).build()
            (loader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        }
        if (resultCar != null) {
            carBitmap = Bitmap.createScaledBitmap(resultCar, 32, 32, false)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshLiveLocations()
            viewModel.getCurrentWeather(5.6037, -0.1870)
                .onSuccess {
                    weather = it
                    lastWeatherUpdate = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                }
            delay(10000L)
        }
    }

    // Update live drivers on MapLibre GPU style layer
    LaunchedEffect(mapLibreMap, viewModel.liveDrivers, motorbikeBitmap, carBitmap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        map.getStyle { style ->
            motorbikeBitmap?.let { style.addImage("icon-okada", it) }
            carBitmap?.let { style.addImage("icon-car", it) }
            (carBitmap ?: motorbikeBitmap)?.let { style.addImage("driver-icon", it) }

            val source = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("drivers-source")
            if (source != null) {
                val features = viewModel.liveDrivers.mapNotNull { driver ->
                    val lat = driver["lat"] as? Double ?: 0.0
                    val lng = driver["lng"] as? Double ?: 0.0
                    if (lat == 0.0 || lng == 0.0) return@mapNotNull null

                    val feature = org.maplibre.geojson.Feature.fromGeometry(
                        org.maplibre.geojson.Point.fromLngLat(lng, lat)
                    )
                    val vType = (driver["vehicle"] as? String)?.lowercase() ?: ""
                    val iconId = when {
                        vType.contains("okada") || vType.contains("bike") || vType.contains("motorcycle") || vType.contains("rider") || vType.contains("motorbike") || vType.contains("motor") -> "icon-okada"
                        else -> "icon-car"
                    }
                    feature.addStringProperty("icon-id", iconId)
                    feature.addStringProperty("driver-id", driver["id"].toString())
                    feature.addStringProperty("driver-name", (driver["name"] ?: "Driver").toString())
                    feature.addNumberProperty("bearing", (driver["bearing"] as? Number)?.toFloat() ?: 0f)
                    feature
                }
                source.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeatures(features))
            }
        }
    }

    // Camera animation for target location
    LaunchedEffect(mapLibreMap, targetLocation) {
        val map = mapLibreMap ?: return@LaunchedEffect
        targetLocation?.let { loc ->
            val cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                .target(org.maplibre.android.geometry.LatLng(loc.latitude, loc.longitude))
                .zoom(16.0)
                .build()
            map.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapLibreMap = map
                        val styleUrl = "https://api.tomtom.com/style/2/custom/style/dG9tdG9tQEBAZFVDV2NzZ09mRGhEaU9MdDsVGbKlskhOMbwzZ3vdhit8?key=${NetworkClient.TOMTOM_API_KEY}"
                        map.setStyle(styleUrl) { style ->
                            motorbikeBitmap?.let { style.addImage("icon-okada", it) }
                            carBitmap?.let { style.addImage("icon-car", it) }
                            (carBitmap ?: motorbikeBitmap)?.let { style.addImage("driver-icon", it) }

                            style.addSource(org.maplibre.android.style.sources.GeoJsonSource("drivers-source"))
                            val layer = org.maplibre.android.style.layers.SymbolLayer("drivers-layer", "drivers-source").apply {
                                withProperties(
                                    org.maplibre.android.style.layers.PropertyFactory.iconImage(
                                        org.maplibre.android.style.expressions.Expression.coalesce(
                                            org.maplibre.android.style.expressions.Expression.get("icon-id"),
                                            org.maplibre.android.style.expressions.Expression.literal("icon-car")
                                        )
                                    ),
                                    org.maplibre.android.style.layers.PropertyFactory.iconRotate(
                                        org.maplibre.android.style.expressions.Expression.get("bearing")
                                    ),
                                    org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                                    org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true)
                                )
                            }
                            style.addLayer(layer)
                        }

                        map.addOnMapClickListener { point ->
                            val screenPoint = map.projection.toScreenLocation(point)
                            val features = map.queryRenderedFeatures(screenPoint, "drivers-layer")
                            if (features.isNotEmpty()) {
                                val clickedId = features.first().getStringProperty("driver-id")
                                val driverMatch = viewModel.liveDrivers.find { it["id"].toString() == clickedId }
                                if (driverMatch != null) {
                                    selectedDriver = driverMatch
                                    return@addOnMapClickListener true
                                }
                            }
                            selectedDriver = null
                            false
                        }

                        // Center on Accra
                        val initialPos = org.maplibre.android.camera.CameraPosition.Builder()
                            .target(org.maplibre.android.geometry.LatLng(5.6037, -0.1870))
                            .zoom(12.0)
                            .build()
                        map.moveCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(initialPos))
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Overlay Cards (Active Drivers & Weather)
        Column(
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(
                    "Active Drivers: ${viewModel.liveDrivers.size}",
                    modifier = Modifier.padding(10.dp),
                    fontWeight = FontWeight.ExtraBold,
                    color = FamekoPrimary
                )
            }

            Spacer(Modifier.height(8.dp))

            weather?.let { w ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF3182CE),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("${w.main.temp.toInt()}°C - ${w.weather.firstOrNull()?.main ?: ""}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            lastWeatherUpdate?.let {
                                Text("Accra (Updated: $it)", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }

        // Driver Inspection Sheet
        selectedDriver?.let { driver ->
            val dName = driver["name"] as? String ?: "Driver"
            val dPhone = driver["phone"] as? String ?: driver["phoneNumber"] as? String ?: ""
            val dVehicle = driver["vehicle"] as? String ?: "Okada Motorbike"
            val dPlate = driver["plate"] as? String ?: driver["plateNumber"] as? String ?: "GT-1234-24"
            val dId = driver["id"].toString().toIntOrNull() ?: 0

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = FamekoPrimary.copy(alpha = 0.1f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, null, tint = FamekoPrimary, modifier = Modifier.size(24.dp))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(dName, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Text("$dVehicle • $dPlate", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        IconButton(onClick = { selectedDriver = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (dPhone.isNotEmpty()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$dPhone"))
                                    context.startActivity(intent)
                                }
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FamekoPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Call Driver", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                onInspectDriver(dId)
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.AssignmentInd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Inspect Profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminApprovalsScreen(
    viewModel: AdminViewModel = viewModel(),
    onViewDriver: (Int) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Drivers", "Payments")

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = FamekoPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = FamekoPrimary
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        when (selectedTab) {
            0 -> DriverApprovalsTab(viewModel, onViewDriver)
            1 -> PaymentApprovalsTab(viewModel)
        }
    }
}

@Composable
fun DriverApprovalsTab(viewModel: AdminViewModel, onViewDriver: (Int) -> Unit) {
    var pendingDrivers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            viewModel.getPendingDrivers().onSuccess {
                pendingDrivers = it
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FamekoPrimary)
        }
    } else if (pendingDrivers.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending driver approvals")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pendingDrivers) { driver ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).background(Color(0xFFEDF2F7), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text((driver["name"] as? String ?: "D").take(1), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(driver["name"] as? String ?: "Unknown", fontWeight = FontWeight.Bold)
                                Text(driver["email"] as? String ?: "", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onViewDriver(driver["id"] as Int) }) {
                                Icon(Icons.Default.Visibility, null, tint = FamekoPrimary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text("Region: ${driver["region"] ?: "N/A"}", fontSize = 14.sp)
                        Text("Vehicle: ${driver["vehicle"] ?: "N/A"}", fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.approveDriver(driver["id"] as Int).onSuccess { load() }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Approve")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.rejectDriver(driver["id"] as Int).onSuccess { load() }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentApprovalsTab(viewModel: AdminViewModel) {
    var pendingPayments by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            isLoading = true
            viewModel.getPendingPayments().onSuccess {
                pendingPayments = it
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { load() }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = FamekoPrimary)
        }
    } else if (pendingPayments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No pending payment approvals")
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(pendingPayments) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("₵${payment["amount"]}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF28A745))
                            Box(Modifier.background(Color(0xFFEBF8FF), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(payment["payment_type"] as? String ?: "PAYMENT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3182CE))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(payment["driver_name"] as? String ?: "Driver", fontWeight = FontWeight.Bold)
                        Text("Ref: ${payment["reference"]}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.approvePayment(payment["id"] as Int).onSuccess { load() }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = FamekoPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Approve")
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        viewModel.rejectPayment(payment["id"] as Int).onSuccess { load() }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSOSScreen(
    viewModel: AdminViewModel = viewModel(),
    onLocate: (String, Double, Double) -> Unit = { _, _, _ -> },
    onChat: (Int, String, String) -> Unit = { _, _, _ -> }
) {
    var alerts by remember { mutableStateOf<List<SOSAlert>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var resolvingId by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // Keep track of which alerts we've already played sound for
    val playedAlerts = remember { mutableSetOf<Int>() }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.getActiveSOS().onSuccess { newAlerts ->
                // Sound logic is now handled in AdminSOSService for background support
                alerts = newAlerts
                isLoading = false
            }
            delay(5000L)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Active SOS Alerts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.Red)
            if (alerts.isNotEmpty()) {
                Badge(containerColor = Color.Red) {
                    Text("${alerts.size}", color = Color.White)
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.Red)
            }
        } else if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Shield, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No active emergency alerts", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(alerts) { alert ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Red)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(32.dp))
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(alert.type, fontWeight = FontWeight.Black, color = Color.Red, fontSize = 20.sp)
                                    Text(alert.driverName, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.weight(1f))
                                Text(
                                    java.text.SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(alert.time)),
                                    fontSize = 12.sp,
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Color.Red.copy(alpha = 0.1f))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Phone", fontSize = 10.sp, color = Color.Gray)
                                    Text(alert.driverPhone, fontWeight = FontWeight.Bold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Vehicle", fontSize = 10.sp, color = Color.Gray)
                                    Text("${alert.vehicleModel ?: "N/A"} (${alert.plateNumber ?: "N/A"})", fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(Modifier.height(12.dp))
                            
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onLocate(alert.driverId, alert.lat, alert.lng) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Map, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Locate", fontWeight = FontWeight.Bold)
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            resolvingId = alert.id
                                            viewModel.resolveSOS(alert.id).onSuccess {
                                                alerts = alerts.filter { it.id != alert.id }
                                                Toast.makeText(context, "SOS Alert Resolved", Toast.LENGTH_SHORT).show()
                                                resolvingId = null
                                            }.onFailure {
                                                Toast.makeText(context, "Failed to resolve: ${it.message}", Toast.LENGTH_LONG).show()
                                                resolvingId = null
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    enabled = resolvingId != alert.id
                                ) {
                                    if (resolvingId == alert.id) {
                                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.Red, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Resolve")
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        val dId = alert.driverId.toIntOrNull() ?: 0
                                        onChat(dId, alert.driverName, alert.driverPhone)
                                    },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color.White, CircleShape)
                                        .border(1.dp, Color.Red.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Chat,
                                        contentDescription = "Chat",
                                        tint = Color.Red,
                                        modifier = Modifier.size(20.dp)
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

@Composable
fun AdminCustomersScreen(viewModel: AdminViewModel = viewModel()) {
    var customers by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getAdminCustomers().onSuccess {
            customers = it
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Customer Base", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(customers) { customer ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(40.dp).background(Color(0xFFE2E8F0), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text((customer["name"] as? String ?: "C").take(1), fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(customer["name"] as? String ?: "Unknown", fontWeight = FontWeight.Bold)
                                Text(customer["phone"] as? String ?: "No Phone", fontSize = 12.sp, color = Color.Gray)
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
fun AdminDeliveriesScreen(viewModel: AdminViewModel = viewModel()) {
    val context = LocalContext.current
    var deliveries by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    val tabs = listOf("ALL", "ACTIVE", "PENDING", "DELIVERED", "CANCELLED")

    LaunchedEffect(Unit) {
        viewModel.getAdminDeliveries().onSuccess {
            deliveries = it
            isLoading = false
        }
    }

    val filteredDeliveries = deliveries.filter { order ->
        val status = (order["status"] as? String ?: "PENDING").uppercase()
        when (selectedTab) {
            1 -> status.contains("ASSIGN") || status.contains("ARRIV") || status.contains("TRANSIT") || status.contains("ON_TRIP")
            2 -> status.contains("PENDING") || status.contains("SCHEDULE")
            3 -> status.contains("DELIVER") || status.contains("COMPLET")
            4 -> status.contains("CANCEL")
            else -> true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Customer Orders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text("${deliveries.size} Total Platform Orders", fontSize = 12.sp, color = Color.Gray)
            }

            IconButton(onClick = {
                isLoading = true
                scope.launch {
                    viewModel.getAdminDeliveries().onSuccess {
                        deliveries = it
                        isLoading = false
                    }
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = FamekoPrimary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Filter Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Black else FontWeight.Bold,
                            color = if (selectedTab == index) FamekoPrimary else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else if (filteredDeliveries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Inbox, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No orders match this filter", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredDeliveries) { order ->
                    AdminOrderCard(
                        order = order,
                        onCallPhone = { phone ->
                            if (!phone.isNullOrBlank()) {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                context.startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminOrderCard(
    order: Map<String, Any>,
    onCallPhone: (String?) -> Unit
) {
    val orderId = order["id"]?.toString() ?: "0"
    val status = (order["status"] as? String ?: "PENDING").uppercase()
    val amount = (order["amount"] as? Number)?.toDouble() ?: 0.0
    val pickup = order["pickup"] as? String ?: "Pickup Spot"
    val dropoff = order["dropoff"] as? String ?: "Dropoff Spot"
    val customerName = order["customer_name"] as? String ?: "Customer"
    val customerPhone = order["customer_phone"] as? String ?: ""
    val driverName = order["driver_name"] as? String ?: "Unassigned"
    val driverPhone = order["driver_phone"] as? String ?: ""
    val vehicleType = order["vehicle_type"] as? String ?: "Ride"
    val recipientName = order["recipient_name"] as? String
    val recipientPhone = order["recipient_phone"] as? String

    val (statusColor, statusBg) = when (status) {
        "DELIVERED", "COMPLETED" -> Pair(Color(0xFF059669), Color(0xFFECFDF5))
        "ASSIGNED", "ARRIVED", "IN_TRANSIT", "ON_TRIP" -> Pair(Color(0xFF2563EB), Color(0xFFEFF6FF))
        "CANCELLED" -> Pair(Color(0xFFDC2626), Color(0xFFFEF2F2))
        else -> Pair(Color(0xFFD97706), Color(0xFFFEF3C7))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header: ID + Status + Fare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("#$orderId", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                    Surface(color = statusBg, shape = RoundedCornerShape(6.dp)) {
                        Text(status, fontSize = 10.sp, fontWeight = FontWeight.Black, color = statusColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }
                Text(com.example.famekodriver.core.utils.CurrencyUtils.formatCedi(amount), fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF0F172A))
            }

            Spacer(Modifier.height(8.dp))

            Surface(color = Color(0xFFF8FAFC), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Category: $vehicleType",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF475569),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Spacer(Modifier.height(10.dp))

            // Route Details
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pickup, fontSize = 12.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(dropoff, fontSize = 12.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFF1F5F9))

            // Participants Details
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Customer Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("CUSTOMER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(customerName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    if (customerPhone.isNotEmpty()) {
                        Text(customerPhone, fontSize = 11.sp, color = Color(0xFF2563EB), modifier = Modifier.clickable { onCallPhone(customerPhone) })
                    }
                }

                // Driver Column
                Column(modifier = Modifier.weight(1f)) {
                    Text("DRIVER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Text(driverName, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    if (driverPhone.isNotEmpty()) {
                        Text(driverPhone, fontSize = 11.sp, color = Color(0xFF2563EB), modifier = Modifier.clickable { onCallPhone(driverPhone) })
                    }
                }
            }

            // Recipient Info if available
            if (!recipientName.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PersonPin, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Recipient: $recipientName (${recipientPhone ?: ""})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDbExplorerScreen(viewModel: AdminViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    var tables by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTable by remember { mutableStateOf("orders") }
    var tableData by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRow by remember { mutableStateOf<Map<String, Any?>?>(null) }

    val defaultTables = listOf("orders", "deliveries", "customers", "drivers", "driver_stats", "fleet_owners", "rentals", "rental_vehicles", "payments", "chat_messages", "movement_logs", "pricing_config")

    // Fetch Tables
    LaunchedEffect(Unit) {
        viewModel.getAdminDbTables().onSuccess { fetched ->
            if (fetched.isNotEmpty()) {
                tables = fetched
                selectedTable = fetched.firstOrNull { it.lowercase() == "orders" } ?: fetched.first()
            } else {
                tables = defaultTables
            }
        }.onFailure {
            tables = defaultTables
        }
    }

    // Fetch Table Data when selectedTable changes
    LaunchedEffect(selectedTable) {
        if (selectedTable.isNotEmpty()) {
            isLoading = true
            viewModel.getAdminTableData(selectedTable).onSuccess { data ->
                tableData = data
                isLoading = false
            }.onFailure {
                tableData = emptyList()
                isLoading = false
            }
        }
    }

    val displayTables = if (tables.isNotEmpty()) tables else defaultTables

    val filteredRows = if (searchQuery.isBlank()) {
        tableData
    } else {
        tableData.filter { row ->
            row.values.any { valStr ->
                valStr?.toString()?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("DB Audit Explorer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                Text("${filteredRows.size} Records in $selectedTable", fontSize = 11.sp, color = Color.Gray)
            }

            IconButton(onClick = {
                isLoading = true
                scope.launch {
                    viewModel.getAdminTableData(selectedTable).onSuccess { data ->
                        tableData = data
                        isLoading = false
                    }
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = FamekoPrimary)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Table Selection Chips
        ScrollableTabRow(
            selectedTabIndex = displayTables.indexOf(selectedTable).coerceAtLeast(0),
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {}
        ) {
            displayTables.forEach { tableName ->
                val isSelected = selectedTable.equals(tableName, ignoreCase = true)
                Tab(
                    selected = isSelected,
                    onClick = { selectedTable = tableName },
                    text = {
                        Text(
                            text = tableName,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                            color = if (isSelected) FamekoPrimary else Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search by ID, phone, status, PIN...", fontSize = 12.sp, color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = FamekoPrimary) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                    }
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FamekoPrimary,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(12.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else if (filteredRows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No records found in $selectedTable", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredRows) { rowData ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRow = rowData },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(color = FamekoPrimary.copy(alpha = 0.1f), shape = RoundedCornerShape(6.dp)) {
                                    Text(
                                        text = "ID: ${rowData["id"] ?: rowData["order_id"] ?: rowData["driver_id"] ?: rowData["customer_id"] ?: "ROW"}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = FamekoPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Icon(Icons.Default.ChevronRight, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }

                            Spacer(Modifier.height(8.dp))

                            // Display first 6 key-value pairs
                            rowData.entries.take(6).forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(key, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    Text(
                                        value?.toString() ?: "null",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full Row Audit Inspection Dialog
    selectedRow?.let { row ->
        AlertDialog(
            onDismissRequest = { selectedRow = null },
            title = { Text("Audit Record Detail ($selectedTable)", fontWeight = FontWeight.Black, fontSize = 16.sp) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    row.forEach { (key, value) ->
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Column(Modifier.padding(10.dp)) {
                                Text(key.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FamekoPrimary)
                                Spacer(Modifier.height(2.dp))
                                Text(value?.toString() ?: "null", fontSize = 12.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedRow = null }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun AdminRentalsScreen(viewModel: AdminViewModel = viewModel()) {
    var rentals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getAdminRentals().onSuccess {
            rentals = it
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Fleet Rentals", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else if (rentals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No rental requests")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rentals) { rental ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rental["vehicle_name"] as? String ?: "Vehicle", fontWeight = FontWeight.Bold)
                                Text(rental["status"] as? String ?: "PENDING", fontWeight = FontWeight.Bold, color = Color(0xFF28A745), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Customer: ${rental["customer_name"]}", fontSize = 12.sp, color = Color.Gray)
                            Text("Price: ₵${rental["total_price"]}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDriverDetailsScreen(
    driverId: Int,
    viewModel: AdminViewModel = viewModel(),
    onNavigateToChat: (Int, String, String) -> Unit = { _, _, _ -> },
    onBack: () -> Unit
) {
    var driver by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(driverId) {
        viewModel.getAdminDriverDetails(driverId).onSuccess {
            driver = it
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else {
            driver?.let { d ->
                Column(Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(100.dp).background(Color(0xFFF0F2F5), CircleShape), contentAlignment = Alignment.Center) {
                                Text((d["full_name"] as? String ?: "D").take(1), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(d["full_name"] as? String ?: "Unknown", fontWeight = FontWeight.Black, fontSize = 20.sp)
                            Text(d["email"] as? String ?: "", color = Color.Gray)

                            Spacer(Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    val name = d["full_name"] as? String ?: "Driver"
                                    val phone = d["phone"] as? String ?: ""
                                    onNavigateToChat(driverId, name, phone)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = FamekoPrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Open Live Support Chat", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))
                    Text("Identity & Documents", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    
                    DocItem("Phone", d["phone"] as? String ?: "N/A")
                    DocItem("Region", d["region"] as? String ?: "N/A")
                    DocItem("License #", d["license_number"] as? String ?: "N/A")
                    DocItem("Vehicle Type", d["vehicle_type"] as? String ?: "N/A")
                    DocItem("Vehicle Category", d["vehicle_category"] as? String ?: "N/A")
                    DocItem("Plate Number", d["vehicle_number"] as? String ?: "N/A")
                    
                    Spacer(Modifier.height(24.dp))
                    // Here we'd show document images using Coil if available
                }
            }
        }
    }
}

@Composable
fun DocItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AdminFinancialsScreen(viewModel: AdminViewModel = viewModel()) {
    var stats by remember { mutableStateOf<Map<String, Any>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getAdminFinancials().onSuccess {
            stats = it
            isLoading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Financial Analytics", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = FamekoPrimary)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Total Revenue", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                    Text("₵${stats["totalRevenue"] ?: "0.00"}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                }
            }
            
            Spacer(Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FinStatCard("Daily Fees", "₵${stats["dailyFees"] ?: "0"}", Color(0xFF38A169), Modifier.weight(1f))
                FinStatCard("Rental Comms", "₵${stats["rentalIncome"] ?: "0"}", Color(0xFF3182CE), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun FinStatCard(label: String, value: String, color: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun AdminDriverSearchScreen() {
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text("Driver Management", style = MaterialTheme.typography.headlineMedium)
        Text("Search and verify driver status")
    }
}
