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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetOwnerSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onSwitchToDriverMode: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val ownerId = sessionManager.getDriverId() ?: ""

    var ownerName by remember { mutableStateOf(sessionManager.getDriverName() ?: "") }
    var ownerPhone by remember { mutableStateOf(sessionManager.getDriverPhone() ?: "") }
    var companyName by remember { mutableStateOf(sessionManager.getCompanyName() ?: "") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var fleetCount by remember { mutableStateOf(0) }

    var dailyMomoRemittance by remember { mutableStateOf(true) }
    var graWithholdingTax by remember { mutableStateOf(true) }
    var selectedDriverSplit by remember { mutableStateOf("70/30 (Standard)") }

    LaunchedEffect(Unit) {
        if (ownerId.isNotEmpty()) {
            repository.getDriverProfile(ownerId, "OWNER").onSuccess { profile ->
                if (profile["success"] == true) {
                    profilePicUrl = profile["profile_picture"]?.toString()
                    ownerName = profile["name"]?.toString() ?: ownerName
                    companyName = profile["company_name"]?.toString() ?: companyName
                    ownerPhone = profile["phone"]?.toString() ?: ownerPhone
                    (profile["fleet_count"] as? Number)?.toInt()?.let { fleetCount = it }
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
                        Text("Fleet Dvla Compliance", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("FLEET OWNER MODE • DVLA Connected", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
                    IconButton(onClick = { Toast.makeText(context, "Fleet Settings", Toast.LENGTH_SHORT).show() }) {
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
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF2563EB), CircleShape))
                            Text("FLEET OWNER MODE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                            Text("DVLA Connected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                    }
                }
            }

            // Fleet Owner Hero Card
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
                        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                        color = Color(0xFF059669),
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
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(ownerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Surface(color = Color(0xFF2563EB), shape = RoundedCornerShape(6.dp)) {
                                        Text("PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text("Verified Fleet Partner • Accra Metro", fontSize = 11.sp, color = Color.LightGray)
                                Text("ID: FK-ACC-8821 • $ownerPhone", fontSize = 11.sp, color = Color.Gray)
                            }
                        }

                        // 3 Enterprise Metrics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FleetOwnerMetricBox("Active Fleet", "$fleetCount Cars", Modifier.weight(1f))
                            FleetOwnerMetricBox("Fleet Health", "98.2%", Modifier.weight(1f))
                            FleetOwnerMetricBox("Compliance", "100%", Modifier.weight(1f))
                        }
                    }
                }
            }

            // Section 1: Fleet Operations & Dispatch Rules
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Tune, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Text("Fleet Operations & Dispatch Rules", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Text("Accra Hub", fontSize = 11.sp, color = Color.Gray)
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            // Driver Split Selector
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Default Driver Split", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                        Text("Partner Default", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text("Applied automatically to new dispatches across all $fleetCount linked vehicles.", fontSize = 11.sp, color = Color.Gray)

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("70/30 (Standard)", "60/40", "Custom Split").forEach { split ->
                                        val isSelected = selectedDriverSplit == split
                                        Surface(
                                            onClick = { selectedDriverSplit = split },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected) Color(0xFF059669) else Color(0xFFF1F5F9)
                                        ) {
                                            Text(
                                                text = split,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else Color(0xFF475569),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerToggleRow(
                                icon = Icons.Default.AccountBalanceWallet,
                                iconColor = Color(0xFF059669),
                                title = "Daily MoMo Remittance",
                                subtitle = "Disburse driver earnings automatically every day at 23:00 GMT",
                                checked = dailyMomoRemittance,
                                onCheckedChange = { dailyMomoRemittance = it }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerNavRow(
                                icon = Icons.Default.GridOn,
                                iconColor = Color(0xFF2563EB),
                                title = "Geofence Boundaries",
                                subtitle = "Greater Accra Metro Only • Trigger alerts on Tema Motorway / Kasoa toll",
                                onClick = { Toast.makeText(context, "Geofence Boundaries Active", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerNavRow(
                                icon = Icons.Default.Speed,
                                iconColor = Color(0xFF2563EB),
                                title = "Speed Limiter Threshold",
                                subtitle = "Instant push & SMS alerts when vehicle crosses 80 km/h",
                                badgeText = "80 km/h",
                                badgeColor = Color(0xFFFEF2F2),
                                badgeTextColor = Color(0xFFDC2626),
                                onClick = { Toast.makeText(context, "Speed threshold set at 80 km/h", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Section 2: DVLA & Vehicle Telemetry
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Text("DVLA & Vehicle Telemetry", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Text("Realtime", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            FleetOwnerNavRow(
                                icon = Icons.Default.Assignment,
                                iconColor = Color(0xFF059669),
                                title = "DVLA Expiry Alerts",
                                subtitle = "Notify 30 days before Roadworthy or Insurance expiration date",
                                badgeText = "30 Days",
                                onClick = { Toast.makeText(context, "DVLA Expiry Alerts Active", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerNavRow(
                                icon = Icons.Default.Build,
                                iconColor = Color(0xFF2563EB),
                                title = "Service & Oil Change",
                                subtitle = "Interval set to 5,000 km • Auto-sync with connected CAN-bus odometers",
                                badgeText = "5,000 km",
                                onClick = { Toast.makeText(context, "Oil Change Reminders Active", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerNavRow(
                                icon = Icons.Default.Wifi,
                                iconColor = Color(0xFF2563EB),
                                title = "GPS Telemetry Rate",
                                subtitle = "High Precision • Refresh polling ping every 5 seconds",
                                badgeText = "5 Sec",
                                onClick = { Toast.makeText(context, "GPS Telemetry 5s Ping Active", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Section 3: Financial & GRA Tax Setup
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                            Text("Financial & GRA Tax Setup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        }
                        Text("GHS Currency", fontSize = 11.sp, color = Color.Gray)
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Designated Payout Accounts", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Surface(shape = CircleShape, color = Color(0xFF2563EB), modifier = Modifier.size(32.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.AccountBalance, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Column {
                                                Text("Stanbic Bank Ghana", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("Corporate Acct •••• 4410", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                            Text("Primary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }

                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(32.dp)) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Default.Smartphone, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Column {
                                                Text("MTN Mobile Money Merchant", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                                Text("$ownerPhone (Fameko Ent)", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        }

                                        Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp)) {
                                            Text("Secondary", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerToggleRow(
                                icon = Icons.Default.ReceiptLong,
                                iconColor = Color(0xFF059669),
                                title = "GRA Withholding Tax",
                                subtitle = "Automatic withholding calculation deducted per driver cycle",
                                checked = graWithholdingTax,
                                onCheckedChange = { graWithholdingTax = it }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            FleetOwnerNavRow(
                                icon = Icons.Default.Mail,
                                iconColor = Color(0xFF2563EB),
                                title = "Weekly CSV Audit Invoices",
                                subtitle = "Auto-dispatched to ${sessionManager.getDriverPhone()}",
                                onClick = { Toast.makeText(context, "Audit invoices active", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }

            // Section 4: Admin & Team Governance
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Admin & Team Governance", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(start = 4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            FleetOwnerNavRow(
                                icon = Icons.Default.Group,
                                iconColor = Color(0xFF2563EB),
                                title = "Sub-Managers & Dispatchers",
                                subtitle = "2 Dispatchers Authorized (Osu & Tema Hubs)",
                                onClick = { Toast.makeText(context, "Dispatcher permissions active", Toast.LENGTH_SHORT).show() }
                            )

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF059669).copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.SupportAgent, null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                    }
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("VIP Fleet Executive", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                        Surface(color = Color(0xFF059669), shape = RoundedCornerShape(6.dp)) {
                                            Text("Direct", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                        }
                                    }
                                    Text("Dedicated Account Manager (Kwame Mensah - Ridge HQ)", fontSize = 11.sp, color = Color.Gray)
                                }
                                Button(
                                    onClick = { Toast.makeText(context, "Calling VIP Account Manager...", Toast.LENGTH_SHORT).show() },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Buttons: Switch to Driver Mode & Logout
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = onSwitchToDriverMode,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFF6FF), contentColor = Color(0xFF2563EB)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Text("Switch to Driver Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Button(
                        onClick = onLogout,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFDC2626)),
                        border = BorderStroke(1.dp, Color(0xFFFECDD3))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Logout, null, tint = Color(0xFFDC2626), modifier = Modifier.size(18.dp))
                            Text("Log Out of Fleet Enterprise", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFDC2626))
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("FAMEKO MOBILITY OS • ENTERPRISE V4.12.0", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("Ghana Cyber Security Authority Compliant • DVLA API v3", fontSize = 9.sp, color = Color.LightGray)
                    }
                }
            }
        }
    }
}

@Composable
fun FleetOwnerMetricBox(label: String, value: String, modifier: Modifier = Modifier) {
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
            Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
fun FleetOwnerNavRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFEFF6FF),
    badgeTextColor: Color = Color(0xFF2563EB),
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
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
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
fun FleetOwnerToggleRow(
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
