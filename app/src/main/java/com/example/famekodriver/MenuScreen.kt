package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import java.util.Locale

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
    val driverId = sessionManager.getDriverId() ?: ""

    var userRole by remember { mutableStateOf(sessionManager.getUserRole()) }
    var driverName by remember { mutableStateOf(sessionManager.getDriverName() ?: "Emmanuel Sackey") }
    var companyName by remember { mutableStateOf(sessionManager.getCompanyName() ?: "Sackey's Rentals") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var driverStats by remember { mutableStateOf(com.example.famekodriver.core.domain.model.DriverStats()) }
    var fleetCount by remember { mutableStateOf(1) }
    var activeRentalsCount by remember { mutableStateOf(0) }
    var totalEarnings by remember { mutableStateOf(0.0) }
    var cashTripsAccepted by remember { mutableStateOf(true) }

    val isFleetOwner = userRole == "OWNER" || userRole == "BOTH"

    LaunchedEffect(Unit) {
        if (driverId.isNotEmpty()) {
            repository.getDriverStats(driverId).onSuccess { stats -> driverStats = stats }
            repository.getDriverProfile(driverId, userRole).onSuccess { profile ->
                if (profile["success"] == true) {
                    profilePicUrl = profile["profile_picture"]?.toString()
                    driverName = profile["name"]?.toString() ?: driverName
                    companyName = profile["company_name"]?.toString()?.ifEmpty { companyName } ?: companyName
                    (profile["fleet_count"] as? Number)?.toInt()?.let { if (it > 0) fleetCount = it }
                    (profile["active_rentals_count"] as? Number)?.toInt()?.let { activeRentalsCount = it }
                    (profile["total_earnings"] as? Number)?.toDouble()?.let { totalEarnings = it }

                    val fetchedRole = profile["user_role"]?.toString()
                    if (!fetchedRole.isNullOrEmpty()) {
                        userRole = fetchedRole
                        sessionManager.setUserRole(fetchedRole)
                    }
                }
            }
        }
    }

    val driverStatus = sessionManager.getDriverStatus().ifEmpty { "APPROVED" }
    val vehicleInfo = (sessionManager.getVehicleType() ?: "").ifEmpty { "Toyota Vitz • Roadworthiness valid" }
    val initials = driverName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Main Menu", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text(
                                if (isFleetOwner) "Fameko Fleet Partner Active" else "Fameko Partner Active",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show() }) {
                        Box {
                            Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(Color.Red, CircleShape)
                                    .align(Alignment.TopEnd)
                            )
                        }
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
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToProfile() },
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        // Driver / Owner Info Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                            ) {
                                if (!profilePicUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = profilePicUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(initials.ifEmpty { "ES" }, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.BottomEnd),
                                    border = BorderStroke(2.dp, Color(0xFF0A192F))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = driverName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                    Surface(
                                        color = Color(0xFF2563EB),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = if (isFleetOwner) "Fleet Partner" else "Pro",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = Color(0xFF065F46),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                            Text(driverStatus, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                        }
                                    }
                                    Text(
                                        text = if (isFleetOwner) companyName else vehicleInfo,
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }

                        Spacer(Modifier.height(20.dp))

                        // 3 Stats Cards Inside Hero Card
                        if (isFleetOwner) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroStatBox("FLEET SIZE", "$fleetCount Vehicles", Modifier.weight(1f))
                                HeroStatBox("RENTALS", "$activeRentalsCount Active", Modifier.weight(1f))
                                HeroStatBox("EARNINGS", "GH₵ ${String.format(Locale.getDefault(), "%.0f", totalEarnings)}", Modifier.weight(1f))
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroStatBox("TODAY", "GH₵ ${String.format(Locale.getDefault(), "%.0f", driverStats.earningsToday)}", Modifier.weight(1f))
                                HeroStatBox("RATING", "${String.format(Locale.getDefault(), "%.2f", driverStats.rating)} ★", Modifier.weight(1f))
                                HeroStatBox("TRIPS", "${driverStats.completedToday.takeIf { it > 0 } ?: driverStats.totalDeliveries}", Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Daily Access Fee Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text("Daily Access Fee Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Text("Valid till 11:59 PM", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Safety Center & Cash Trips Toggle
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp)
                            .clickable { onNavigateToSupport() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEF2F2),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Warning, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Safety Center", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("24/7 Road SOS", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(96.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cash Trips", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("Accepted", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }
                            Switch(
                                checked = cashTripsAccepted,
                                onCheckedChange = { cashTripsAccepted = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }
                    }
                }
            }

            // Section: ACCOUNT & ACTIVITIES / FLEET MANAGEMENT
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isFleetOwner) "FLEET & ACCOUNT ACTIVITIES" else "ACCOUNT & ACTIVITIES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                    Text(if (isFleetOwner) "Fleet Portal" else "Driver Portal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                }
            }

            // Menu Items Card Container
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        MenuItemRow(
                            icon = Icons.Default.Person,
                            iconColor = Color(0xFF2563EB),
                            title = "Profile & Documents",
                            subtitle = "Manage license, permit & account info",
                            badgeText = "All Verified",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = onNavigateToProfile
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuItemRow(
                            icon = Icons.Default.AccountBalanceWallet,
                            iconColor = Color(0xFF10B981),
                            title = "Earnings & Fees",
                            subtitle = "Today's stats, daily pass & payouts",
                            badgeText = "Ghana Pay",
                            badgeColor = Color(0xFFEFF6FF),
                            badgeTextColor = Color(0xFF2563EB),
                            onClick = onNavigateToWallet
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuItemRow(
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF0284C7),
                            title = "Fameko Rentals & Fleet",
                            subtitle = "List vehicles, manage fleet & track rentals",
                            badgeText = "Fleet Portal",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = onNavigateToFleet
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuItemRow(
                            icon = Icons.Default.History,
                            iconColor = Color(0xFF7C3AED),
                            title = "Ride History",
                            subtitle = "Your completed trips & route logs",
                            badgeText = "${driverStats.totalDeliveries} trips",
                            badgeColor = Color(0xFFF1F5F9),
                            badgeTextColor = Color(0xFF475569),
                            onClick = onNavigateToRideHistory
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuItemRow(
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF64748B),
                            title = "Settings",
                            subtitle = "App preferences, audio & navigation",
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HeroStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
        }
    }
}

@Composable
fun MenuItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFECFDF5),
    badgeTextColor: Color = Color(0xFF059669),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                badgeText?.let {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
    }
}
