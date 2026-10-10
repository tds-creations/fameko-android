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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSwitchToFleetConsole: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val driverId = sessionManager.getDriverId() ?: ""

    var driverName by remember { mutableStateOf(sessionManager.getDriverName() ?: "Emmanuel Sackey") }
    var driverPhone by remember { mutableStateOf(sessionManager.getDriverPhone() ?: "+233 24 971 2254") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    
    var voiceNavEnabled by remember { mutableStateOf(true) }
    var autoAcceptTrips by remember { mutableStateOf(false) }
    var tripRadarEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English (Ghana)") }

    LaunchedEffect(Unit) {
        if (driverId.isNotEmpty()) {
            repository.getDriverProfile(driverId, "DRIVER").onSuccess { profile ->
                if (profile["success"] == true) {
                    profilePicUrl = profile["profile_picture"]?.toString()
                    driverName = profile["name"]?.toString() ?: driverName
                    driverPhone = profile["phone"]?.toString() ?: driverPhone
                }
            }
        }
    }

    val vehicleInfo = (sessionManager.getVehicleType() ?: "").ifEmpty { "Toyota Vitz (GW-4129-22)" }
    val initials = driverName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifEmpty { "ES" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Active Trip Tracking", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("DRIVER ON-DUTY • Accra Central Corridor", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
                    IconButton(onClick = { Toast.makeText(context, "Driver Settings Profile", Toast.LENGTH_SHORT).show() }) {
                        Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Driver Profile Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(56.dp).clip(CircleShape)) {
                                if (!profilePicUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = profilePicUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(driverName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(6.dp)) {
                                        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("$driverPhone • ⭐ 4.92 • Top Tier Partner", fontSize = 12.sp, color = Color.LightGray)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("SHIFT TIME", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("5h 42m", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                            }
                        }

                        // Vehicle Info Pill
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Text(vehicleInfo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(6.dp)) {
                                    Text("Compliant", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: DRIVING & NAVIGATION
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Driving & Navigation", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("ERGONOMICS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            DriverToggleRow(
                                icon = Icons.Default.VolumeUp,
                                iconColor = Color(0xFF059669),
                                title = "Voice Navigation Audio",
                                subtitle = "Read turn directions at max volume",
                                checked = voiceNavEnabled,
                                onCheckedChange = { voiceNavEnabled = it }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            // Voice Language Prompt Selector
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Voice Language Prompt", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Accra Dialects", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("English (Gha...", "Twi (Akan)", "Ga").forEach { lang ->
                                        val isSelected = selectedLanguage.startsWith(lang.take(5))
                                        Surface(
                                            onClick = { selectedLanguage = lang },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF059669) else Color.Transparent)
                                        ) {
                                            Text(
                                                text = lang,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF059669) else Color(0xFF475569),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            DriverNavRow(
                                icon = Icons.Default.Navigation,
                                iconColor = Color(0xFF0284C7),
                                title = "In-App GPS Engine",
                                subtitle = "TomTom & MapLibre Vector Engine",
                                onClick = { Toast.makeText(context, "Integrated GPS Active", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            DriverNavRow(
                                icon = Icons.Default.GpsFixed,
                                iconColor = Color(0xFF2563EB),
                                title = "High-Precision GPS",
                                subtitle = "Active (Optimized for battery)",
                                badgeText = "Active",
                                onClick = { Toast.makeText(context, "High-Precision GPS Active", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            DriverNavRow(
                                icon = Icons.Default.GetApp,
                                iconColor = Color(0xFF059669),
                                title = "Accra Offline Map Cache",
                                subtitle = "68 MB Downloaded • Zero-late...",
                                badgeText = "Update",
                                badgeColor = Color(0xFFA7F3D0),
                                badgeTextColor = Color(0xFF065F46),
                                onClick = { Toast.makeText(context, "Map Cache Updated", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Section 2: DISPATCH & SAFETY
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dispatch & Safety", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("SHIFT CONTROLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            DriverToggleRow(
                                icon = Icons.Default.TouchApp,
                                iconColor = Color(0xFF2563EB),
                                title = "Auto-Accept Trips",
                                subtitle = "Manual review of trip destinations",
                                checked = autoAcceptTrips,
                                onCheckedChange = { autoAcceptTrips = it }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            DriverToggleRow(
                                icon = Icons.Default.Radar,
                                iconColor = Color(0xFF059669),
                                title = "Consecutive Trip Radar",
                                subtitle = "Queue next request before drop-off",
                                checked = tripRadarEnabled,
                                onCheckedChange = { tripRadarEnabled = it }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            DriverNavRow(
                                icon = Icons.Default.Speed,
                                iconColor = Color(0xFF2563EB),
                                title = "Speed Limit Audio Alerts",
                                subtitle = "Warn at 70 km/h in Accra Metro",
                                onClick = { Toast.makeText(context, "Speed Alert set at 70 km/h", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            // SOS Emergency Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { Toast.makeText(context, "Calling 112 Ghana Emergency...", Toast.LENGTH_SHORT).show() }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFDC2626), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("SOS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 11.sp)
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Emergency 112 SOS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF991B1B))
                                        Surface(color = Color(0xFFDC2626), shape = RoundedCornerShape(6.dp)) {
                                            Text("24/7", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text("Ghana Police Service & Rapid Tow", fontSize = 11.sp, color = Color(0xFFDC2626))
                                }
                                Icon(Icons.Default.Phone, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Section 3: ACCOUNT & SETTLEMENT
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Account & Settlement", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Text("GHANA REGULATED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            DriverNavRow(
                                icon = Icons.Default.Badge,
                                iconColor = Color(0xFF059669),
                                title = "Ghana Card Verification",
                                subtitle = "Linked & Active (GHA-71928491-3)",
                                showCheck = true,
                                onClick = { Toast.makeText(context, "Ghana Card Verified", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            DriverNavRow(
                                icon = Icons.Default.AccountBalanceWallet,
                                iconColor = Color(0xFFD97706),
                                title = "MTN MoMo Disbursement",
                                subtitle = "•••• 9812 (Instant Settlement)",
                                onClick = { Toast.makeText(context, "MoMo Settlement active", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Action Buttons: Switch to Fleet Owner Mode & Logout
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSwitchToFleetConsole,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Domain, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Column {
                                Text("Switch to Fleet Owner Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                Text("Manage 3 registered vehicles", fontSize = 10.sp, color = Color.LightGray)
                            }
                            Spacer(Modifier.weight(1f))
                            Icon(Icons.Default.SwapHoriz, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3)),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = Color(0xFFDC2626))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.PowerSettingsNew, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Text("End Shift & Logout", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(
                        "FAMEKO TRANSIT OS V4.2.0 • ACCRA FLEET CORE",
                        fontSize = 10.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }
}

@Composable
fun DriverNavRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFEFF6FF),
    badgeTextColor: Color = Color(0xFF2563EB),
    showCheck: Boolean = false,
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
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                if (showCheck) {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        badgeText?.let {
            Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                Text(it, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
    }
}

@Composable
fun DriverToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF10B981))
        )
    }
}
