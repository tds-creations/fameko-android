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
import androidx.compose.ui.layout.ContentScale
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
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val driverId = sessionManager.getDriverId() ?: ""
    
    var userRole by remember { mutableStateOf(sessionManager.getUserRole()) }
    var driverName by remember { mutableStateOf(sessionManager.getDriverName() ?: "Emmanuel Sackey") }
    var companyName by remember { mutableStateOf(sessionManager.getCompanyName() ?: "Sackey's Rentals") }
    var driverPhone by remember { mutableStateOf(sessionManager.getDriverPhone() ?: "+233 24 971 2254") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var fleetCount by remember { mutableStateOf(1) }
    var driverStats by remember { mutableStateOf(com.example.famekodriver.core.domain.model.DriverStats()) }
    var voiceNavEnabled by remember { mutableStateOf(true) }

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
                    
                    val fetchedRole = profile["user_role"]?.toString()
                    if (!fetchedRole.isNullOrEmpty()) {
                        userRole = fetchedRole
                        sessionManager.setUserRole(fetchedRole)
                    }
                }
            }
        }
    }

    val vehicleInfo = (sessionManager.getVehicleType() ?: "").ifEmpty { "Toyota Vitz" }
    val initials = driverName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifEmpty { "ES" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("App Settings", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text(
                                if (isFleetOwner) "FLEET OWNER ACTIVE" else "DRIVER ACTIVE",
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
                    IconButton(onClick = { Toast.makeText(context, "Fameko Support: 24/7 Helpline (+233 30 212 3456)", Toast.LENGTH_LONG).show() }) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF2563EB),
                            modifier = Modifier.size(52.dp)
                        ) {
                            if (!profilePicUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = profilePicUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(initials, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(driverName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        if (isFleetOwner) "Fleet Partner" else "Verified Pro",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(2.dp))
                            Text(
                                if (isFleetOwner) "$driverPhone • $companyName" else "$driverPhone • $vehicleInfo",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(if (isFleetOwner) "Fleet Size" else "Rating", fontSize = 10.sp, color = Color.Gray)
                            if (isFleetOwner) {
                                Text("$fleetCount Vehicles", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(String.format(Locale.US, "%.2f", if (driverStats.rating > 0) driverStats.rating else 4.95), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("★", fontSize = 12.sp, color = Color(0xFFFFC107))
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: ACCOUNT PREFERENCES
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ACCOUNT PREFERENCES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            SettingsOptionRow(
                                icon = Icons.Default.Notifications,
                                iconColor = Color(0xFF2563EB),
                                title = "Notification Settings",
                                subtitle = "Trip alerts, surge updates & sounds",
                                badgeText = "All Active",
                                badgeColor = Color(0xFFEFF6FF),
                                badgeTextColor = Color(0xFF2563EB),
                                onClick = onNavigateToNotificationSettings
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsOptionRow(
                                icon = Icons.Default.Language,
                                iconColor = Color(0xFF0284C7),
                                title = "Language",
                                subtitle = "Ghana (English), Twi audio prompts",
                                badgeText = "English",
                                badgeColor = Color(0xFFF1F5F9),
                                badgeTextColor = Color(0xFF475569),
                                onClick = { Toast.makeText(context, "Ghana English & Twi active", Toast.LENGTH_SHORT).show() }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsToggleRow(
                                icon = Icons.Default.VolumeUp,
                                iconColor = Color(0xFF7C3AED),
                                title = "Voice Navigation Audio",
                                subtitle = "Read turn directions at max volume during trips",
                                checked = voiceNavEnabled,
                                onCheckedChange = { voiceNavEnabled = it }
                            )
                        }
                    }
                }
            }

            // Section 2: DRIVING & APP BEHAVIOR
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DRIVING & APP BEHAVIOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            SettingsOptionRow(
                                icon = Icons.Default.Map,
                                iconColor = Color(0xFF10B981),
                                title = "Navigation App",
                                subtitle = "Integrated TomTom / MapLibre",
                                badgeText = "In-App GPS",
                                badgeColor = Color(0xFFECFDF5),
                                badgeTextColor = Color(0xFF059669),
                                onClick = { Toast.makeText(context, "Integrated In-App Navigation Active", Toast.LENGTH_SHORT).show() }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsOptionRow(
                                icon = Icons.Default.GpsFixed,
                                iconColor = Color(0xFFD97706),
                                title = "High-Precision Location",
                                subtitle = "Keep GPS active in background",
                                badgeText = "Optimized",
                                badgeColor = Color(0xFFFEF3C7),
                                badgeTextColor = Color(0xFFD97706),
                                onClick = { Toast.makeText(context, "Background GPS High Precision Active", Toast.LENGTH_SHORT).show() }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsOptionRow(
                                icon = Icons.Default.GetApp,
                                iconColor = Color(0xFF2563EB),
                                title = "Accra Map Offline Cache",
                                subtitle = "68 MB saved for zero-latency routes",
                                badgeText = "Updated",
                                badgeColor = Color(0xFFEFF6FF),
                                badgeTextColor = Color(0xFF2563EB),
                                onClick = { Toast.makeText(context, "Accra map cache is up to date (68 MB)", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Section 3: ABOUT & LEGAL
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("ABOUT & LEGAL", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            SettingsOptionRow(
                                icon = Icons.Default.Description,
                                iconColor = Color(0xFF64748B),
                                title = "Terms of Service",
                                subtitle = "Fameko Ghana Partner Agreement",
                                onClick = onNavigateToTerms
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsOptionRow(
                                icon = Icons.Default.PrivacyTip,
                                iconColor = Color(0xFF64748B),
                                title = "Privacy Policy",
                                subtitle = "How we collect, protect & use location data",
                                onClick = onNavigateToPrivacy
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            SettingsOptionRow(
                                icon = Icons.Default.Info,
                                iconColor = Color(0xFF64748B),
                                title = "App Version",
                                subtitle = "Fameko Partner v4.26.1 (Build 2024.9)",
                                badgeText = "Latest",
                                badgeColor = Color(0xFFECFDF5),
                                badgeTextColor = Color(0xFF059669),
                                onClick = {}
                            )
                        }
                    }
                }
            }

            // Log Out Button
            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                ) {
                    Text("Log Out of Account", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsOptionRow(
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
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
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

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981)
            )
        )
    }
}
