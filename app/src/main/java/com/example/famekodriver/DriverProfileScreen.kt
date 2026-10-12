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
    var driverName by remember { mutableStateOf(sessionManager.getDriverName() ?: "") }
    var driverEmail by remember { mutableStateOf("") }
    var driverPhone by remember { mutableStateOf(sessionManager.getDriverPhone() ?: "") }
    var driverRegion by remember { mutableStateOf("Greater Accra, Ghana") }
    var companyName by remember { mutableStateOf(sessionManager.getCompanyName() ?: "") }
    var regNumber by remember { mutableStateOf("") }
    var fleetCount by remember { mutableStateOf(0) }
    var vehicleModel by remember { mutableStateOf("") }
    var vehiclePlate by remember { mutableStateOf("") }
    var missingDocs by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var profilePicUrl by remember { mutableStateOf<String?>(sessionManager.getProfilePicture()) }
    var driverStats by remember { mutableStateOf(com.example.famekodriver.core.domain.model.DriverStats()) }

    var docUrls by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var viewingDoc by remember { mutableStateOf<Triple<String, String, String>?>(null) } // Title, DocType, ImageUrl

    var isEditDialogVisible by remember { mutableStateOf(false) }
    var pendingDocType by remember { mutableStateOf<String?>(null) }

    fun refreshProfile() {
        isLoading = true
        scope.launch {
            repository.getDriverProfile(driverId, userRole).onSuccess { profile ->
                if (profile["success"] == true) {
                    driverName = profile["name"]?.toString()?.ifEmpty { driverName } ?: driverName
                    var rawEmail = profile["email"]?.toString() ?: ""
                    var rawPhone = profile["phone"]?.toString() ?: ""

                    if (rawEmail.isBlank() && rawPhone.contains("@")) {
                        rawEmail = rawPhone
                        rawPhone = ""
                    }

                    if (rawEmail.isNotBlank()) driverEmail = rawEmail
                    if (rawPhone.isNotBlank()) driverPhone = rawPhone

                    driverRegion = profile["region"]?.toString()?.ifEmpty { driverRegion } ?: driverRegion
                    val fetchedStatus = profile["status"]?.toString()
                    if (!fetchedStatus.isNullOrEmpty()) {
                        status = fetchedStatus
                        sessionManager.updateStatus(fetchedStatus)
                    }

                    val pic = profile["profile_picture"]?.toString()
                    if (!pic.isNullOrEmpty()) {
                        profilePicUrl = pic
                        sessionManager.updateProfilePicture(pic)
                    }

                    docUrls = mapOf(
                        "profile_pic" to (profile["profile_picture"]?.toString() ?: ""),
                        "ghana_card" to (profile["id_front_image"]?.toString() ?: ""),
                        "ghana_card_back" to (profile["id_back_image"]?.toString() ?: ""),
                        "drivers_license" to (profile["license_image"]?.toString() ?: ""),
                        "roadworthy_cert" to (profile["roadworthy_cert"]?.toString() ?: ""),
                        "insurance_cert" to (profile["insurance_cert"]?.toString() ?: ""),
                        "vehicle_image" to (profile["vehicle_image"]?.toString() ?: "")
                    )

                    val fetchedRole = profile["user_role"]?.toString()
                    if (!fetchedRole.isNullOrEmpty()) {
                        userRole = fetchedRole
                        sessionManager.setUserRole(fetchedRole)
                    }

                    companyName = profile["company_name"]?.toString()?.ifEmpty { companyName } ?: companyName
                    regNumber = profile["registration_number"]?.toString()?.ifEmpty { regNumber } ?: regNumber
                    (profile["fleet_count"] as? Number)?.toInt()?.let { fleetCount = it }
                    vehicleModel = profile["vehicle_model"]?.toString() ?: vehicleModel
                    vehiclePlate = profile["vehicle_number"]?.toString() ?: vehiclePlate

                    docUrls = mapOf(
                        "profile_pic" to (profile["profile_picture"]?.toString() ?: ""),
                        "ghana_card" to (profile["id_front_image"]?.toString() ?: ""),
                        "ghana_card_back" to (profile["id_back_image"]?.toString() ?: ""),
                        "drivers_license" to (profile["license_image"]?.toString() ?: ""),
                        "roadworthy_cert" to (profile["roadworthy_cert"]?.toString() ?: ""),
                        "insurance_cert" to (profile["insurance_cert"]?.toString() ?: ""),
                        "vehicle_image" to (profile["vehicle_image"]?.toString() ?: "")
                    )
                }
            }

            repository.getDriverStatus(driverId).onSuccess { resp ->
                status = resp.status
                sessionManager.updateStatus(resp.status)
                missingDocs = resp.missingDocs
                if (!resp.profilePicture.isNullOrEmpty()) {
                    profilePicUrl = resp.profilePicture
                    sessionManager.updateProfilePicture(resp.profilePicture)
                }
            }

            if (driverId.isNotEmpty()) {
                repository.getDriverStats(driverId).onSuccess { stats -> driverStats = stats }
            }

            isLoading = false
        }
    }

    val pickImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            scope.launch {
                isLoading = true
                val file = uriToFile(context, it)
                if (file != null) {
                    val docType = pendingDocType ?: "profile_pic"
                    repository.uploadDocument(driverId, docType, file).onSuccess {
                        isLoading = false
                        Toast.makeText(context, "Upload successful! Refreshing profile...", Toast.LENGTH_SHORT).show()
                        refreshProfile()
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
        refreshProfile()
    }

    fun onDocClick(docType: String, title: String) {
        val url = docUrls[docType]
        if (!url.isNullOrBlank()) {
            viewingDoc = Triple(title, docType, url)
        } else {
            pendingDocType = docType
            pickImageLauncher.launch("image/*")
        }
    }

    val isFleetOwner = userRole == "OWNER" || userRole == "BOTH"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isFleetOwner) "Fleet Owner Profile" else "My Profile", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
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
                    IconButton(onClick = { refreshProfile(); Toast.makeText(context, "Refreshing profile...", Toast.LENGTH_SHORT).show() }) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh Profile", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    IconButton(onClick = { isEditDialogVisible = true }) {
                        Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
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
                            // Avatar with glowing ring & camera badge
                            val initials = driverName.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString("").uppercase().ifEmpty { "DR" }
                            val hasValidPic = !profilePicUrl.isNullOrBlank() && profilePicUrl != "null" && profilePicUrl != "undefined"

                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clickable {
                                        pendingDocType = "profile_pic"
                                        pickImageLauncher.launch("image/*")
                                    }
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.fillMaxSize(),
                                    border = BorderStroke(2.5.dp, Color(0xFF10B981))
                                ) {
                                    if (hasValidPic) {
                                        AsyncImage(
                                            model = profilePicUrl,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(initials, color = Color.White, fontWeight = FontWeight.Black, fontSize = 28.sp)
                                        }
                                    }
                                }
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier
                                        .size(26.dp)
                                        .align(Alignment.BottomEnd),
                                    border = BorderStroke(2.dp, Color(0xFF0A192F))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = driverName,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White
                            )

                            Spacer(Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                        Text(status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                    }
                                }

                                Surface(
                                    color = Color(0xFF2563EB),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = if (isFleetOwner) "Fleet Enterprise Partner" else "Verified Pro",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (isFleetOwner) "FLEET SIZE" else "RATING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(Modifier.height(2.dp))
                                        Text(if (isFleetOwner) "$fleetCount Cars" else String.format(Locale.US, "%.2f ★", if (driverStats.rating > 0) driverStats.rating else 4.95), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("REGION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(Modifier.height(2.dp))
                                        Text(driverRegion.take(12), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    }
                                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.White.copy(alpha = 0.2f)))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                        Spacer(Modifier.height(2.dp))
                                        Text(status.take(8), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF34D399))
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 1: PERSONAL & COMPANY INFORMATION
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isFleetOwner) "ENTERPRISE DETAILS" else "PERSONAL INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Text("Edit", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.clickable { isEditDialogVisible = true })
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
                                        badgeText = "Verified Fleet",
                                        badgeColor = Color(0xFFECFDF5),
                                        badgeTextColor = Color(0xFF059669),
                                        onClick = { isEditDialogVisible = true }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                    ProfileDetailRow(
                                        icon = Icons.Default.Description,
                                        label = "RGD Reg Number",
                                        value = regNumber,
                                        badgeText = "RGD Valid",
                                        badgeColor = Color(0xFFEFF6FF),
                                        badgeTextColor = Color(0xFF2563EB),
                                        onClick = { isEditDialogVisible = true }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                }

                                ProfileDetailRow(
                                    icon = Icons.Default.Person,
                                    label = if (isFleetOwner) "Principal Owner" else "Full Name",
                                    value = driverName,
                                    badgeText = if (isFleetOwner) "Principal" else "Primary",
                                    onClick = { isEditDialogVisible = true }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                ProfileDetailRow(
                                    icon = Icons.Default.Email,
                                    label = "Email Address",
                                    value = driverEmail,
                                    badgeText = "Active",
                                    badgeColor = Color(0xFFECFDF5),
                                    badgeTextColor = Color(0xFF059669),
                                    onClick = { isEditDialogVisible = true }
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
                                    onClick = { isEditDialogVisible = true }
                                )
                            }
                        }
                    }
                }

                // Section 2: VERIFICATION & VEHICLE DOCUMENTS
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("VERIFICATION & VEHICLE DOCUMENTS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp))

                        Card(
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column {
                                DocumentUploadRow(
                                    title = "Profile Photo",
                                    subtitle = "Official avatar displayed to partners & clients",
                                    isUploaded = !docUrls["profile_pic"].isNullOrBlank() || !profilePicUrl.isNullOrEmpty(),
                                    onClick = { onDocClick("profile_pic", "Profile Photo") }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                DocumentUploadRow(
                                    title = "Ghana Card (Front)",
                                    subtitle = "National Identification Authority (NIA) Card Front",
                                    isUploaded = !docUrls["ghana_card"].isNullOrBlank(),
                                    onClick = { onDocClick("ghana_card", "Ghana Card (Front)") }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                DocumentUploadRow(
                                    title = "Ghana Card (Back)",
                                    subtitle = "National Identification Authority (NIA) Card Back",
                                    isUploaded = !docUrls["ghana_card_back"].isNullOrBlank(),
                                    onClick = { onDocClick("ghana_card_back", "Ghana Card (Back)") }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                DocumentUploadRow(
                                    title = "DVLA Driver's License",
                                    subtitle = "Valid Class B/C Driver License Document",
                                    isUploaded = !docUrls["drivers_license"].isNullOrBlank(),
                                    onClick = { onDocClick("drivers_license", "DVLA Driver's License") }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                DocumentUploadRow(
                                    title = "DVLA Roadworthy Certificate",
                                    subtitle = "Commercial Roadworthiness Sticker",
                                    isUploaded = !docUrls["roadworthy_cert"].isNullOrBlank(),
                                    onClick = { onDocClick("roadworthy_cert", "DVLA Roadworthy Certificate") }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                                DocumentUploadRow(
                                    title = "Vehicle Insurance Policy",
                                    subtitle = "Comprehensive Passenger Commercial Insurance",
                                    isUploaded = !docUrls["insurance_cert"].isNullOrBlank(),
                                    onClick = { onDocClick("insurance_cert", "Vehicle Insurance Policy") }
                                )
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }

    // Document Viewer Dialog
    if (viewingDoc != null) {
        val (title, docType, imageUrl) = viewingDoc!!
        AlertDialog(
            onDismissRequest = { viewingDoc = null },
            title = { Text(title, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Text("Uploaded Document Image", fontSize = 11.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dt = docType
                        viewingDoc = null
                        pendingDocType = dt
                        pickImageLauncher.launch("image/*")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Re-upload / Update")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { viewingDoc = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Edit Profile Dialog
    if (isEditDialogVisible) {
        var editName by remember { mutableStateOf(driverName) }
        var editEmail by remember { mutableStateOf(driverEmail) }
        var editPhone by remember { mutableStateOf(driverPhone) }
        var editCompany by remember { mutableStateOf(companyName) }
        var editRegNum by remember { mutableStateOf(regNumber) }
        var editRegion by remember { mutableStateOf(driverRegion) }
        var editPlate by remember { mutableStateOf(vehiclePlate) }
        var editModel by remember { mutableStateOf(vehicleModel) }
        var isSubmitting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSubmitting) isEditDialogVisible = false },
            title = {
                Text(
                    text = if (isFleetOwner) "Edit Fleet Owner Profile" else "Edit Driver Profile",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (isFleetOwner) {
                        OutlinedTextField(
                            value = editCompany,
                            onValueChange = { editCompany = it },
                            label = { Text("Company / Fleet Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editRegNum,
                            onValueChange = { editRegNum = it },
                            label = { Text("RGD Registration Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    OutlinedTextField(
                        value = editEmail,
                        onValueChange = { editEmail = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = editRegion,
                        onValueChange = { editRegion = it },
                        label = { Text("Operating Region") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!isFleetOwner) {
                        OutlinedTextField(
                            value = editPlate,
                            onValueChange = { editPlate = it },
                            label = { Text("Vehicle Plate Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        OutlinedTextField(
                            value = editModel,
                            onValueChange = { editModel = it },
                            label = { Text("Vehicle Model") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        val data = mutableMapOf(
                            "id" to driverId,
                            "name" to editName,
                            "email" to editEmail,
                            "phone" to editPhone,
                            "region" to editRegion,
                            "company_name" to editCompany,
                            "registration_number" to editRegNum,
                            "vehicle_number" to editPlate,
                            "vehicle_model" to editModel,
                            "user_role" to userRole
                        )
                        scope.launch {
                            repository.updateProfile(data).onSuccess {
                                isSubmitting = false
                                isEditDialogVisible = false
                                Toast.makeText(context, "Profile updated successfully!", Toast.LENGTH_SHORT).show()

                                sessionManager.saveSession(
                                    driverId = driverId,
                                    driverName = editName,
                                    status = status,
                                    phone = editPhone,
                                    role = userRole,
                                    company = editCompany,
                                    vehicleType = if (isFleetOwner) "Fleet" else "Car",
                                    profilePicture = profilePicUrl
                                )
                                driverName = editName
                                driverEmail = editEmail
                                driverPhone = editPhone
                                companyName = editCompany
                                regNumber = editRegNum
                                driverRegion = editRegion
                                vehiclePlate = editPlate
                                vehicleModel = editModel
                            }.onFailure { err ->
                                isSubmitting = false
                                Toast.makeText(context, "Update failed: ${err.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F))
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { if (!isSubmitting) isEditDialogVisible = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileDetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    badgeText: String? = null,
    badgeColor: Color = Color(0xFFF1F5F9),
    badgeTextColor: Color = Color(0xFF475569),
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
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(value.ifEmpty { "Not set" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        badgeText?.let {
            Surface(color = badgeColor, shape = RoundedCornerShape(6.dp)) {
                Text(it, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
fun DocumentUploadRow(
    title: String,
    subtitle: String,
    isUploaded: Boolean,
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
            color = if (isUploaded) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isUploaded) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                    contentDescription = null,
                    tint = if (isUploaded) Color(0xFF059669) else Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            color = if (isUploaded) Color(0xFFECFDF5) else Color(0xFFEFF6FF),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = if (isUploaded) "View / Edit" else "Upload",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUploaded) Color(0xFF059669) else Color(0xFF2563EB),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
