package com.example.famekodriver

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val driverId = sessionManager.getDriverId() ?: ""

    var userRole by remember { mutableStateOf(sessionManager.getUserRole()) }
    var status by remember { mutableStateOf(sessionManager.getDriverStatus()) }
    var driverName by remember { mutableStateOf(sessionManager.getDriverName() ?: "Nii Odartei") }
    var driverEmail by remember { mutableStateOf("niiodartei24@gmail.com") }
    var driverPhone by remember { mutableStateOf(sessionManager.getDriverPhone() ?: "+233 53 818 8056") }
    var driverRegion by remember { mutableStateOf("Greater Accra, Ghana") }
    var companyName by remember { mutableStateOf(sessionManager.getCompanyName() ?: "Fameko Fleet Operations") }
    var regNumber by remember { mutableStateOf("REG-2024-8891") }
    var fleetCount by remember { mutableStateOf(1) }
    var vehicleModel by remember { mutableStateOf("Toyota Vitz (2018) • Silver") }
    var vehiclePlate by remember { mutableStateOf("GT-4821-22") }
    var missingDocs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var driverStats by remember { mutableStateOf(com.example.famekodriver.core.domain.model.DriverStats()) }

    var pendingDocType by remember { mutableStateOf<String?>(null) }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                val file = uriToFile(context, it)
                if (file != null) {
                    val docType = pendingDocType ?: "profile_pic"
                    repository.uploadDocument(driverId, docType, file).onSuccess {
                        isLoading = false
                        Toast.makeText(context, "Upload successful!", Toast.LENGTH_SHORT).show()
                        
                        repository.getDriverStatus(driverId).onSuccess { resp ->
                            missingDocs = resp.missingDocs
                            status = resp.status
                            sessionManager.updateStatus(resp.status)
                            if (docType == "profile_pic") {
                                profilePicUrl = resp.profilePicture
                            }
                        }
                    }.onFailure { err ->
                        isLoading = false
                        Toast.makeText(context, "Upload failed: ${err.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    isLoading = false
                    Toast.makeText(context, "Failed to process image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        repository.getDriverProfile(driverId).onSuccess { profile ->
            if (profile["success"] == true) {
                driverName = profile["name"]?.toString() ?: driverName
                driverEmail = profile["email"]?.toString() ?: driverEmail
                driverPhone = profile["phone"]?.toString() ?: driverPhone
                driverRegion = profile["region"]?.toString() ?: driverRegion
                status = profile["status"]?.toString() ?: "APPROVED"
                profilePicUrl = profile["profile_picture"]?.toString()

                val fetchedRole = profile["user_role"]?.toString()
                if (!fetchedRole.isNullOrEmpty()) {
                    userRole = fetchedRole
                    sessionManager.setUserRole(fetchedRole)
                }

                companyName = profile["company_name"]?.toString()?.ifEmpty { companyName } ?: companyName
                regNumber = profile["registration_number"]?.toString()?.ifEmpty { regNumber } ?: regNumber
                (profile["fleet_count"] as? Number)?.toInt()?.let { if (it > 0) fleetCount = it }
            }
            isLoading = false
        }.onFailure {
            isLoading = false
        }

        if (driverId.isNotEmpty()) {
            repository.getDriverStats(driverId).onSuccess { stats -> driverStats = stats }
        }
        
        repository.getDriverStatus(driverId).onSuccess { resp ->
            sessionManager.updateStatus(resp.status)
            status = resp.status
            missingDocs = resp.missingDocs
            if (profilePicUrl == null) profilePicUrl = resp.profilePicture
        }
    }

    val isFleetOwner = userRole == "OWNER" || userRole == "BOTH"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isFleetOwner) "Fleet Partner Profile" else "My Profile", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Text(if (isFleetOwner) "Fleet & operator account details" else "Manage license, permit & account info", fontSize = 12.sp, color = Color.Gray)
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
                    IconButton(onClick = { Toast.makeText(context, "Edit profile mode active", Toast.LENGTH_SHORT).show() }) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Hero Profile Card
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
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Avatar with glowing ring & checkmark badge
                            Box(
                                modifier = Modifier.size(88.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxSize(),
                                    border = BorderStroke(2.dp, Color(0xFF10B981))
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
                                            Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(44.dp))
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.BottomEnd),
                                    border = BorderStroke(2.dp, Color(0xFF0A192F))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(
                                text = if (isFleetOwner) companyName else driverName,
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = Color.White
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (isFleetOwner) "Fleet Partner ID: #FMK-FLEET-${Math.abs(driverId.hashCode() % 90000) + 10000}" else "Partner ID: #FMK-${Math.abs(driverId.hashCode() % 90000) + 10000}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )

                            Spacer(Modifier.height(12.dp))

                            // Approved / Role Badge Pill
                            Surface(
                                color = Color(0xFF065F46),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text(
                                        text = if (isFleetOwner) "FLEET OWNER • $status" else status.ifEmpty { "APPROVED" },
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // 3 Stats Columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                if (isFleetOwner) {
                                    ProfileStatCol("FLEET SIZE", "$fleetCount Vehicles", Modifier.weight(1f))
                                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))
                                    ProfileStatCol("OPERATING ZONE", "Accra Urban", Modifier.weight(1f))
                                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))
                                    ProfileStatCol("STATUS", "Verified", Modifier.weight(1f))
                                } else {
                                    ProfileStatCol("RATING", "${String.format(Locale.US, "%.2f", if (driverStats.rating > 0) driverStats.rating else 4.95)} ★", Modifier.weight(1f))
                                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))
                                    ProfileStatCol("TRIPS", "${driverStats.totalDeliveries.takeIf { it > 0 } ?: 1240}+", Modifier.weight(1f))
                                    Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color.White.copy(alpha = 0.15f)))
                                    ProfileStatCol("ACCEPTANCE", "${if (driverStats.completionRate > 0) driverStats.completionRate else 98}%", Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                // Section 1: DETAILS CARD
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isFleetOwner) "FLEET & BUSINESS DETAILS" else "PERSONAL DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Text("Tier 1 Verified", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column {
                                if (isFleetOwner) {
                                    ProfileDetailRow(
                                        icon = Icons.Default.Business,
                                        label = "Company / Fleet Name",
                                        value = companyName,
                                        badgeText = "Fleet Partner",
                                        onClick = {}
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileDetailRow(
                                        icon = Icons.Default.Description,
                                        label = "Business Reg Number",
                                        value = regNumber,
                                        badgeText = "Verified",
                                        badgeColor = Color(0xFFECFDF5),
                                        badgeTextColor = Color(0xFF059669),
                                        onClick = {}
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }

                                ProfileDetailRow(
                                    icon = Icons.Default.Person,
                                    label = if (isFleetOwner) "Fleet Representative" else "Full Name",
                                    value = driverName,
                                    badgeText = if (isFleetOwner) "Owner" else "Primary",
                                    onClick = {}
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                ProfileDetailRow(
                                    icon = Icons.Default.Email,
                                    label = "Email Address",
                                    value = driverEmail.ifEmpty { "niiodartei24@gmail.com" },
                                    badgeText = "✓ Active",
                                    badgeColor = Color(0xFFECFDF5),
                                    badgeTextColor = Color(0xFF059669),
                                    onClick = {}
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                ProfileDetailRow(
                                    icon = Icons.Default.Phone,
                                    label = "Phone Number",
                                    value = driverPhone,
                                    badgeText = "GH +233",
                                    badgeColor = Color(0xFFFEF3C7),
                                    badgeTextColor = Color(0xFFD97706),
                                    showChevron = true,
                                    onClick = { Toast.makeText(context, "Phone number settings", Toast.LENGTH_SHORT).show() }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                ProfileDetailRow(
                                    icon = Icons.Default.LocationOn,
                                    label = "Operating Region",
                                    value = driverRegion,
                                    badgeText = "Urban Zone",
                                    onClick = {}
                                )

                                if (!isFleetOwner) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileDetailRow(
                                        icon = Icons.Default.DirectionsCar,
                                        label = "Registered Vehicle",
                                        value = vehicleModel,
                                        badgeText = vehiclePlate,
                                        badgeColor = Color(0xFFEFF6FF),
                                        badgeTextColor = Color(0xFF2563EB),
                                        showChevron = true,
                                        onClick = { Toast.makeText(context, "Vehicle management", Toast.LENGTH_SHORT).show() }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: VERIFICATION & COMPLIANCE
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("VERIFICATION & COMPLIANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Text("All Verified", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column {
                                if (isFleetOwner) {
                                    ComplianceRowItem(
                                        title = "Business Certificate",
                                        subtitle = "Registrar General • Approved",
                                        badge = "VERIFIED",
                                        icon = Icons.Default.Business,
                                        onClick = { pendingDocType = "business_cert"; pickImageLauncher.launch("image/*") }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }

                                ComplianceRowItem(
                                    title = "Ghana Card (National ID)",
                                    subtitle = "GHA-724194012-4 • Authenticated",
                                    badge = "VERIFIED",
                                    icon = Icons.Default.CreditCard,
                                    onClick = { pendingDocType = "ghana_card"; pickImageLauncher.launch("image/*") }
                                )

                                if (!isFleetOwner) {
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    ComplianceRowItem(
                                        title = "Driver's License (Class B)",
                                        subtitle = "DVLA Ghana • Expires Dec 2026",
                                        badge = "VERIFIED",
                                        icon = Icons.Default.Badge,
                                        onClick = { pendingDocType = "drivers_license"; pickImageLauncher.launch("image/*") }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    ComplianceRowItem(
                                        title = "Roadworthy Certificate",
                                        subtitle = "Active • Valid till Oct 2026",
                                        badge = "ACTIVE",
                                        icon = Icons.Default.VerifiedUser,
                                        onClick = { pendingDocType = "roadworthy_cert"; pickImageLauncher.launch("image/*") }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    ComplianceRowItem(
                                        title = "Comprehensive Insurance",
                                        subtitle = "Enterprise Insurance Ltd",
                                        badge = "INSURED",
                                        icon = Icons.Default.Security,
                                        onClick = { pendingDocType = "insurance_cert"; pickImageLauncher.launch("image/*") }
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 3: SAFETY & EMERGENCY CONTACTS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SAFETY & EMERGENCY CONTACTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Text("Manage", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                        }

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFEF2F2),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text("MO", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Mary Odartei", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                        Text("Spouse • +233 24 123 4567", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.size(38.dp).clickable { Toast.makeText(context, "Calling emergency contact...", Toast.LENGTH_SHORT).show() }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Phone, null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                                        Text("Fameko 24/7 Rapid SOS Support", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    }
                                    Surface(
                                        color = Color(0xFFFEF2F2),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Connected", color = Color(0xFFDC2626), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { Toast.makeText(context, "Edit Profile mode active", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Edit Profile Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                            }
                        }

                        OutlinedButton(
                            onClick = { Toast.makeText(context, "Downloading Partner Credential Pack (.PDF)...", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0F172A))
                        ) {
                            Text("Download Partner Credential Pack (.PDF)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFF0F172A))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Processing...", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileStatCol(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFECFDF5),
    badgeTextColor: Color = Color(0xFF059669),
    showChevron: Boolean = false,
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
            color = Color(0xFF2563EB).copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        badgeText?.let {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = it,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeTextColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        if (showChevron) {
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun ComplianceRowItem(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
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
            color = Color(0xFF10B981).copy(alpha = 0.1f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Surface(
            color = Color(0xFFECFDF5),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF059669),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        Spacer(Modifier.width(6.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        null
    }
}
