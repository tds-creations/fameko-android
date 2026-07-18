package com.example.famekodriver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.ui.theme.*
import java.util.Locale

data class MenuItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color = FamekoPrimary
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToRentals: () -> Unit,
    onNavigateToRideHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFleet: () -> Unit = {},
    onNavigateToVehicleReg: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    
    var driverStats by remember { mutableStateOf(com.example.famekodriver.core.domain.model.DriverStats()) }
    val driverId = sessionManager.getDriverId() ?: ""
    val userRole = sessionManager.getUserRole()

    LaunchedEffect(Unit) {
        if (driverId.isNotEmpty()) {
            repository.getDriverStats(driverId).onSuccess { stats -> driverStats = stats }
        }
    }

    val menuItems = remember(userRole) {
        val list = mutableListOf(
            MenuItem("profile", "Profile & Documents", "Manage your account info", Icons.Default.Person),
            MenuItem("earnings", "Earnings & Fees", "Today's stats and daily fee", Icons.Default.Payments),
        )
        if (userRole == "DRIVER" || userRole == "BOTH") {
            list.add(MenuItem("history", "Ride History", "Your completed trips", Icons.Default.History))
            list.add(MenuItem("rentals", "My Rentals", "Assigned rental jobs", Icons.Default.Key))
            list.add(MenuItem("vehicle_reg", "My Vehicle", "Register your primary vehicle", Icons.Default.DirectionsCar))
        }
        if (userRole == "OWNER" || userRole == "BOTH") {
            list.add(MenuItem("fleet", "Fleet Management", "Manage your vehicles", Icons.Default.DirectionsCar))
        }
        list.add(MenuItem("support", "Fameko Support", "Chat with our support team", Icons.Default.SupportAgent, BoltGreen))
        list.add(MenuItem("settings", "App Settings", "Notifications and preferences", Icons.Default.Settings, Color.Gray))
        list
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Main Menu", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            // Header Section
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = FamekoPrimary,
                    shape = RoundedCornerShape(24.dp),
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                modifier = Modifier.size(64.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    sessionManager.getDriverName() ?: "Driver",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                                Surface(
                                    color = BoltGreen,
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        sessionManager.getDriverStatus(),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(Modifier.height(32.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatBox("TODAY", "₵${String.format(Locale.getDefault(), "%.0f", driverStats.earningsToday)}")
                            StatBox("RATING", "${String.format(Locale.getDefault(), "%.1f", driverStats.rating)} ⭐")
                            StatBox("TRIPS", "${driverStats.totalDeliveries}")
                        }
                    }
                }
            }

            // Menu Items (The Recycler Part)
            items(menuItems) { item ->
                MenuListItem(item) {
                    when (item.id) {
                        "profile" -> onNavigateToProfile()
                        "earnings" -> onNavigateToWallet()
                        "rentals" -> onNavigateToRentals()
                        "history" -> onNavigateToRideHistory()
                        "settings" -> onNavigateToSettings()
                        "fleet" -> onNavigateToFleet()
                        "vehicle_reg" -> onNavigateToVehicleReg()
                        "support" -> onNavigateToSupport()
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = BoltLightGray, thickness = 0.5.dp)
            }
            
            item {
                Spacer(Modifier.height(48.dp))
                Text(
                    "Fameko for Drivers v1.2.0",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
fun MenuListItem(item: MenuItem, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        color = Color.White
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = item.color.copy(alpha = 0.1f),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(item.subtitle, color = Color.Gray, fontSize = 13.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}
