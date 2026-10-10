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
fun FleetOwnerMenuScreen(
    onBack: () -> Unit,
    onNavigateToFleetSettings: () -> Unit,
    onNavigateToFleetInventory: () -> Unit,
    onNavigateToDriverRoster: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onSwitchToDriverMode: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val ownerId = sessionManager.getDriverId() ?: ""

    var ownerName by remember { mutableStateOf(sessionManager.getDriverName() ?: "") }
    var companyName by remember { mutableStateOf(sessionManager.getCompanyName() ?: "") }
    var profilePicUrl by remember { mutableStateOf<String?>(sessionManager.getProfilePicture()) }
    var fleetCount by remember { mutableStateOf(0) }
    var activeDriversCount by remember { mutableStateOf(0) }
    var totalDispatches by remember { mutableStateOf(0) }
    var todayGross by remember { mutableStateOf(0.0) }
    var weeklyGross by remember { mutableStateOf(0.0) }

    LaunchedEffect(Unit) {
        if (ownerId.isNotEmpty()) {
            repository.getDriverProfile(ownerId, "OWNER").onSuccess { profile ->
                if (profile["success"] == true) {
                    profilePicUrl = profile["profile_picture"]?.toString()
                    ownerName = profile["name"]?.toString() ?: ownerName
                    companyName = profile["company_name"]?.toString() ?: companyName
                    (profile["fleet_count"] as? Number)?.toInt()?.let { fleetCount = it }
                    (profile["active_rentals_count"] as? Number)?.toInt()?.let { activeDriversCount = it }
                    (profile["total_earnings"] as? Number)?.toDouble()?.let { todayGross = it }
                }
            }
        }
    }

    val initials = ownerName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifEmpty { "NO" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Fleet Vehicle Detail", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("FLEET OWNER CONSOLE • ENTERPRISE", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
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
                    IconButton(onClick = onNavigateToFleetSettings) {
                        Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Settings", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
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
            // Enterprise Status Pill Bar
            item {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Domain, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Text("FLEET OWNER CONSOLE • ENTERPRISE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        }

                        Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                                Text("$activeDriversCount/$fleetCount ONLINE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                        }
                    }
                }
            }

            // Dark Enterprise Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(64.dp).clip(CircleShape)) {
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
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(ownerName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                                    Icon(Icons.Default.Verified, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("Verified Fleet Enterprise Partner", fontSize = 11.sp, color = Color.LightGray)
                            }

                            Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                                    Text("4.98", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        // 3 Fleet Enterprise Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FleetMetricBox("FLEET SIZE", "$fleetCount Units", "● 100% Active", Modifier.weight(1f))
                            FleetMetricBox("ASSIGNED", "$activeDriversCount Drivers", "⟳ All Paired", Modifier.weight(1f))
                            FleetMetricBox("DISPATCHES", "$totalDispatches Trips", "↗ +14.2% Rev", Modifier.weight(1f))
                        }

                        // Fleet Pass Banner
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
                                    Icon(Icons.Default.Shield, null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                                    Text("Fleet Pass: $fleetCount Units Active (Bulk Paid)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(6.dp)) {
                                    Text("ACCRA METRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Fleet Live Financial Yield Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF059669), modifier = Modifier.size(38.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                                Column {
                                    Text("Fleet Live Financial Yield", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF065F46))
                                    Text("Accra Corridors • Auto Aggregated", fontSize = 11.sp, color = Color(0xFF047857))
                                }
                            }

                            Surface(color = Color(0xFF059669), shape = RoundedCornerShape(12.dp)) {
                                Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("TODAY'S GROSS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), letterSpacing = 0.5.sp)
                                Text("GH₵ ${String.format(Locale.getDefault(), "%.2f", todayGross)}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF065F46))
                                Text("36 cash • 12 GhanaPay", fontSize = 11.sp, color = Color(0xFF047857))
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text("WEEKLY ROLLING GROSS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857), letterSpacing = 0.5.sp)
                                Text("GH₵ ${String.format(Locale.getDefault(), "%.2f", weeklyGross)}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF065F46))
                                Text("↗ On track (+8.4%)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                        }

                        Button(
                            onClick = onNavigateToWallet,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Payments, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("Direct Bank / MoMo Withdraw", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Driver 70/30 Net Split Ledger Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Driver 70/30 Net Split Ledger", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text("Automated smart escrow settlements", fontSize = 11.sp, color = Color.Gray)
                            }
                            Text("Breakdown >", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }

                        // Split Progress Bar
                        Row(modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                            Box(modifier = Modifier.weight(0.7f).fillMaxHeight().background(Color(0xFF059669)))
                            Box(modifier = Modifier.weight(0.3f).fillMaxHeight().background(Color(0xFF2563EB)))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("● 70% Driver Shift Share: GH₵ 1,295.00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            Text("● 30% Net Owner: GH₵ 555.00", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }
                    }
                }
            }

            // Section: ENTERPRISE ASSET GOVERNANCE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enterprise Asset Governance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Text("7 MANAGEMENT HUBS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                }
            }

            // Enterprise Hub List
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column {
                        FleetHubRow(
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF2563EB),
                            title = "Fleet Vehicles Inventory",
                            subtitle = "Toyota Vitz, Corolla (2), Camry (2), Tucson",
                            badgeText = "$fleetCount Cars",
                            onClick = onNavigateToFleetInventory
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FleetHubRow(
                            icon = Icons.Default.People,
                            iconColor = Color(0xFF059669),
                            title = "Assigned Driver Roster",
                            subtitle = "E. Sackey, J. Asare, K. Mensah • Telemetry Active",
                            badgeText = "$activeDriversCount Active",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = onNavigateToDriverRoster
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FleetHubRow(
                            icon = Icons.Default.VerifiedUser,
                            iconColor = Color(0xFF2563EB),
                            title = "DVLA Compliance & Insurance",
                            subtitle = "Roadworthiness certs • Comprehensive Policy",
                            badgeText = "100% Up To Date",
                            badgeColor = Color(0xFFEFF6FF),
                            badgeTextColor = Color(0xFF2563EB),
                            onClick = onNavigateToFleetSettings
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FleetHubRow(
                            icon = Icons.Default.LocationOn,
                            iconColor = Color(0xFF2563EB),
                            title = "Live GPS Telemetry & Geofence",
                            subtitle = "Accrazone containment, speed violations",
                            badgeText = "Live Map",
                            badgeColor = Color(0xFFEFF6FF),
                            badgeTextColor = Color(0xFF2563EB),
                            onClick = { Toast.makeText(context, "Opening Live Fleet Map...", Toast.LENGTH_SHORT).show() }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FleetHubRow(
                            icon = Icons.Default.ConfirmationNumber,
                            iconColor = Color(0xFF059669),
                            title = "Fleet Daily Access Passes",
                            subtitle = "Bulk payment discounts & auto-renewals",
                            badgeText = "Bulk Discount",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = { Toast.makeText(context, "Bulk Pass Management", Toast.LENGTH_SHORT).show() }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FleetHubRow(
                            icon = Icons.Default.Description,
                            iconColor = Color(0xFF2563EB),
                            title = "Tax, GRA & Audit CSV Reports",
                            subtitle = "Weekly withholding tax statements, MoMo CSV",
                            badgeText = "Weekly",
                            onClick = { Toast.makeText(context, "Exporting CSV Reports...", Toast.LENGTH_SHORT).show() }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        FleetHubRow(
                            icon = Icons.Default.CarCrash,
                            iconColor = Color(0xFFDC2626),
                            title = "Fleet SOS & Breakdown Dispatch",
                            subtitle = "Roadside towing assistance, certified mechanics",
                            badgeText = "24/7 Rapid",
                            badgeColor = Color(0xFFFEF2F2),
                            badgeTextColor = Color(0xFFDC2626),
                            onClick = { Toast.makeText(context, "Fleet Rapid SOS Active", Toast.LENGTH_SHORT).show() }
                        )
                    }
                }
            }

            // Switch Mode Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Drive One of Your Units?", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            Text("Switch to Personal Driver Mode instantly", fontSize = 12.sp, color = Color.Gray)
                        }

                        Button(
                            onClick = onSwitchToDriverMode,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F))
                        ) {
                            Text("Switch Mode", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }

            // Footer
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Verified, null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                        Text("FAMEKO FLEET ENTERPRISE v1.2.0 • DVLA & GRA CERTIFIED PARTNER", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Text("Republic of Ghana Mobility • Greater Accra Operational Hub", fontSize = 10.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun FleetMetricBox(label: String, value: String, subtext: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
            Spacer(Modifier.height(2.dp))
            Text(subtext, fontSize = 9.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun FleetHubRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFF1F5F9),
    badgeTextColor: Color = Color(0xFF475569),
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
