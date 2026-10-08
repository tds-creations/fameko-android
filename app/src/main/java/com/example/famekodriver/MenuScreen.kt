package com.example.famekodriver

import android.content.Intent
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
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.text.style.TextAlign
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
    
    var driverStats by remember { mutableStateOf(com.example.famekodriver.core.domain.model.DriverStats()) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    val driverId = sessionManager.getDriverId() ?: ""
    var cashTripsAccepted by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (driverId.isNotEmpty()) {
            repository.getDriverStats(driverId).onSuccess { stats -> driverStats = stats }
            repository.getDriverProfile(driverId).onSuccess { profile ->
                if (profile["success"] == true) {
                    profilePicUrl = profile["profile_picture"]?.toString()
                }
            }
        }
    }

    val driverName = sessionManager.getDriverName() ?: "Nii Odartei"
    val driverStatus = sessionManager.getDriverStatus().ifEmpty { "APPROVED" }
    val vehicleInfo = (sessionManager.getVehicleType() ?: "").ifEmpty { "Toyota Vitz • Roadworthiness valid" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Main Menu", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("Fameko Partner Active", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
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
                    IconButton(onClick = { /* Notifications */ }) {
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
                        // Driver Info Row
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
                                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(32.dp))
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
                                            "Pro",
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
                                    Text(vehicleInfo, fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }

                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }

                        Spacer(Modifier.height(20.dp))

                        // 3 Stats Cards Inside Hero Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            HeroStatBox("TODAY", "GH₵ ${String.format(Locale.getDefault(), "%.0f", driverStats.earningsToday)}", Modifier.weight(1f))
                            HeroStatBox("RATING", "${String.format(Locale.getDefault(), "%.2f", driverStats.rating)} ★", Modifier.weight(1f))
                            HeroStatBox("TRIPS", "${driverStats.completedToday.takeIf { it > 0 } ?: driverStats.totalDeliveries}", Modifier.weight(1f))
                        }

                        Spacer(Modifier.height(16.dp))

                        // Daily Access Fee Banner
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                                    Text("Daily Access Fee Active", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
                                }
                                Text("Valid till 11:59 PM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Quick Action Cards Row (Safety Center & Cash Trips)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Safety Center Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToSupport() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
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
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Safety Center", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("24/7 Road SOS", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }

                    // Cash Trips Card
                    Card(
                        modifier = Modifier
                            .weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Cash Trips", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text(if (cashTripsAccepted) "Accepted" else "Disabled", fontSize = 11.sp, color = if (cashTripsAccepted) Color(0xFF059669) else Color.Gray)
                            }
                            Switch(
                                checked = cashTripsAccepted,
                                onCheckedChange = { cashTripsAccepted = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
                            )
                        }
                    }
                }
            }

            // Section Header: ACCOUNT & ACTIVITIES
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "ACCOUNT & ACTIVITIES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Driver Portal",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }
            }

            // Menu Cards List 1 (Account & Activities)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        MenuRowItem(
                            title = "Profile & Documents",
                            subtitle = "Manage license, permit & account info",
                            icon = Icons.Default.Person,
                            iconColor = Color(0xFF2563EB),
                            badge = "All Verified",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = onNavigateToProfile
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuRowItem(
                            title = "Earnings & Fees",
                            subtitle = "Today's stats, daily pass & payouts",
                            icon = Icons.Default.Payments,
                            iconColor = Color(0xFF059669),
                            badge = "Ghana Pay",
                            badgeColor = Color(0xFFEFF6FF),
                            badgeTextColor = Color(0xFF2563EB),
                            onClick = onNavigateToWallet
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuRowItem(
                            title = "Ride History",
                            subtitle = "Your completed trips & route logs",
                            icon = Icons.Default.History,
                            iconColor = Color(0xFF7C3AED),
                            badge = "${driverStats.totalDeliveries} trips",
                            badgeColor = Color(0xFFF1F5F9),
                            badgeTextColor = Color(0xFF475569),
                            onClick = onNavigateToRideHistory
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuRowItem(
                            title = "My Rentals",
                            subtitle = "Assigned rental jobs & scheduled shifts",
                            icon = Icons.Default.Key,
                            iconColor = Color(0xFFD97706),
                            onClick = onNavigateToRentals
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuRowItem(
                            title = "My Vehicle",
                            subtitle = "Vehicle specs & documentation",
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF0284C7),
                            onClick = onNavigateToVehicleReg
                        )
                    }
                }
            }

            // Section Header: SUPPORT & PREFERENCES
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "SUPPORT & PREFERENCES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Menu Cards List 2 (Support & Preferences)
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        MenuRowItem(
                            title = "Fameko Support",
                            subtitle = "Chat with driver priority support team",
                            icon = Icons.Default.SupportAgent,
                            iconColor = Color(0xFF10B981),
                            badge = "24/7 Live",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = onNavigateToSupport
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        MenuRowItem(
                            title = "App Settings",
                            subtitle = "Google Maps, sound, dark mode & alerts",
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF64748B),
                            onClick = onNavigateToSettings
                        )
                    }
                }
            }

            // Sign Out Button
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        sessionManager.logout()
                        val intent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2)),
                    elevation = ButtonDefaults.buttonElevation(0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Sign Out",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Version Footer
            item {
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Fameko for Drivers v1.2.0 (Build 248)",
                        textAlign = TextAlign.Center,
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Accra, Ghana • Licensed Transport Network",
                        textAlign = TextAlign.Center,
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun HeroStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun MenuRowItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    badge: String? = null,
    badgeColor: Color = Color.LightGray,
    badgeTextColor: Color = Color.DarkGray,
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
                badge?.let {
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
