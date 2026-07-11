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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.famekodriver.admin.ui.AdminDashboardScreen
import com.example.famekodriver.admin.ui.theme.FamekoPrimary
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.SOSAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

sealed class AdminScreen(val route: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String) {
    object Dashboard : AdminScreen("dashboard", Icons.Default.Dashboard, "Dashboard")
    object Map : AdminScreen("map", Icons.Default.Map, "Live Map")
    object Approvals : AdminScreen("approvals", Icons.Default.CheckCircle, "Approvals")
    object Customers : AdminScreen("customers", Icons.Default.Group, "Customers")
    object Deliveries : AdminScreen("deliveries", Icons.Default.LocalShipping, "Orders")
    object SOS : AdminScreen("sos", Icons.Default.Warning, "SOS")
    object Rentals : AdminScreen("rentals", Icons.Default.CarRental, "Fleet")
    object DriverDetails : AdminScreen("driver_details", Icons.Default.Person, "Driver Details")
    object Financials : AdminScreen("financials", Icons.Default.PieChart, "Financials")
}

class AdminMapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

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
    var targetMapLocation by remember { mutableStateOf<GeoPoint?>(null) }
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
                AdminScreen.SOS -> AdminSOSScreen(
                    viewModel = adminViewModel,
                    onLocate = { driverId, alertLat, alertLng ->
                        // Prefer live location if available
                        val liveDriver = adminViewModel.liveDrivers.find { it["id"].toString() == driverId }
                        val targetLat = (liveDriver?.get("lat") as? Double) ?: alertLat
                        val targetLng = (liveDriver?.get("lng") as? Double) ?: alertLng
                        
                        if (targetLat != 0.0) {
                            targetMapLocation = GeoPoint(targetLat, targetLng)
                            currentScreen = AdminScreen.Map
                        } else {
                            Toast.makeText(context, "Location unavailable", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                AdminScreen.Customers -> AdminCustomersScreen(viewModel = adminViewModel)
                AdminScreen.Rentals -> AdminRentalsScreen(viewModel = adminViewModel)
                AdminScreen.DriverDetails -> AdminDriverDetailsScreen(
                    driverId = selectedDriverId ?: 0,
                    viewModel = adminViewModel,
                    onBack = { currentScreen = AdminScreen.Approvals }
                )
                AdminScreen.Financials -> AdminFinancialsScreen(viewModel = adminViewModel)
            }
        }
    }
}

class AdminViewModel : ViewModel() {
    private val repository = DriverRepository()
    
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
}

@Composable
fun AdminMapScreen(
    viewModel: AdminViewModel = viewModel(),
    targetLocation: GeoPoint? = null
) {
    var weather by remember { mutableStateOf<com.example.famekodriver.core.domain.model.WeatherResponse?>(null) }
    var lastWeatherUpdate by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    
    // Ensure OSMDroid is configured
    Configuration.getInstance().userAgentValue = context.packageName

    val mapView = remember { MapView(context) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshLiveLocations()
            
            // Fetch weather for Accra center
            viewModel.getCurrentWeather(5.6037, -0.1870)
                .onSuccess {
                    weather = it
                    lastWeatherUpdate = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                }
            
            delay(10000L) // Refresh every 10 seconds
        }
    }

    LaunchedEffect(targetLocation) {
        targetLocation?.let {
            mapView.controller.animateTo(it)
            mapView.controller.setZoom(16.0)
        }
    }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT)
                    controller.setZoom(12.0)
                    controller.setCenter(GeoPoint(5.6037, -0.1870)) // Accra center
                    setBuiltInZoomControls(true)
                }
            },
            modifier = Modifier.fillMaxSize().background(Color.LightGray),
            update = { mv ->
                mv.overlays.clear()
                viewModel.liveDrivers.forEach { driver ->
                    val lat = driver["lat"] as? Double ?: 0.0
                    val lng = driver["lng"] as? Double ?: 0.0
                    if (lat != 0.0) {
                        val marker = Marker(mv).apply {
                            position = GeoPoint(lat, lng)
                            title = driver["name"] as? String ?: "Driver"
                            snippet = "Vehicle: ${driver["vehicle"] ?: "N/A"}"
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        mv.overlays.add(marker)
                    }
                }
                mv.invalidate()
            }
        )
        
        Column(
            modifier = Modifier.padding(16.dp).align(Alignment.TopEnd),
            horizontalAlignment = Alignment.End
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
            ) {
                Text(
                    "Active Drivers: ${viewModel.liveDrivers.size}",
                    modifier = Modifier.padding(8.dp),
                    fontWeight = FontWeight.Bold,
                    color = FamekoPrimary
                )
            }
            
            Spacer(Modifier.height(8.dp))
            
            weather?.let { w ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
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
                            Text(
                                "${w.main.temp.toInt()}°C - ${w.weather.firstOrNull()?.main ?: ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                "${w.name} (Updated: $lastWeatherUpdate)",
                                fontSize = 10.sp,
                                color = Color.Gray
                            )
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
    onLocate: (String, Double, Double) -> Unit = { _, _, _ -> }
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

@Composable
fun AdminDeliveriesScreen(viewModel: AdminViewModel = viewModel()) {
    var deliveries by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getAdminDeliveries().onSuccess {
            deliveries = it
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Live Orders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else if (deliveries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active orders")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(deliveries) { delivery ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("#${delivery["id"]}", fontWeight = FontWeight.Bold, color = FamekoPrimary)
                                Text(delivery["status"] as? String ?: "PENDING", fontWeight = FontWeight.Bold, color = Color(0xFF3182CE), fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("From: ${delivery["pickup"]}", fontSize = 12.sp)
                            Text("To: ${delivery["dropoff"]}", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
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
fun AdminDriverDetailsScreen(driverId: Int, viewModel: AdminViewModel = viewModel(), onBack: () -> Unit) {
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
