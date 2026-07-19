package com.example.famekodriver

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val driverId = sessionManager.getDriverId() ?: ""
    val userRole = remember { sessionManager.getUserRole() }

    var status by remember { mutableStateOf(sessionManager.getDriverStatus()) }
    var driverName by remember { mutableStateOf(sessionManager.getDriverName() ?: "Driver") }
    var driverEmail by remember { mutableStateOf("") }
    var driverPhone by remember { mutableStateOf("") }
    var driverRegion by remember { mutableStateOf("") }
    var missingDocs by remember { mutableStateOf<List<String>>(emptyList()) }
    var emergency1 by remember { mutableStateOf("") }
    var emergency2 by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }

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
                        
                        // Refresh status to update checkmarks
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
                driverName = profile["name"]?.toString() ?: "Driver"
                driverEmail = profile["email"]?.toString() ?: ""
                driverPhone = profile["phone"]?.toString() ?: ""
                driverRegion = profile["region"]?.toString() ?: ""
                status = profile["status"]?.toString() ?: "PENDING"
                profilePicUrl = profile["profile_picture"]?.toString()
            }
            isLoading = false
        }.onFailure {
            isLoading = false
        }
        
        repository.getDriverStatus(driverId).onSuccess { resp ->
            sessionManager.updateStatus(resp.status)
            status = resp.status
            missingDocs = resp.missingDocs
            emergency1 = resp.emergencyContact1 ?: ""
            emergency2 = resp.emergencyContact2 ?: ""
            if (profilePicUrl == null) profilePicUrl = resp.profilePicture
        }

        // Polling for approval if pending
        while (status != "APPROVED") {
            delay(10000)
            repository.getDriverStatus(driverId).onSuccess { resp ->
                if (resp.status != status) {
                    status = resp.status
                    sessionManager.updateStatus(resp.status)
                    profilePicUrl = resp.profilePicture
                    if (status == "APPROVED") {
                        Toast.makeText(context, "Account Approved!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF004E89))
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))
            ) {
                item {
                    ProfileHeader(driverName, status, profilePicUrl)
                }

                item {
                    Text(
                        "Personal Details",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            ProfileField(label = "Full Name", value = driverName)
                            ProfileField(label = "Email", value = driverEmail)
                            ProfileField(label = "Phone", value = driverPhone)
                            ProfileField(label = "Region", value = driverRegion)
                        }
                    }
                }

                item {
                    Text(
                        "Verification Documents",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    DocumentItem(
                        title = "Profile Picture",
                        isUploaded = "profile_pic" !in missingDocs,
                        icon = Icons.Default.Person,
                        onUpload = {
                            pendingDocType = "profile_pic"
                            pickImageLauncher.launch("image/*")
                        }
                    )
                }

                if (userRole == "OWNER" || userRole == "BOTH") {
                    item {
                        DocumentItem(
                            title = "Business Certificate",
                            isUploaded = "business_cert" !in missingDocs,
                            icon = Icons.Default.Business,
                            onUpload = {
                                pendingDocType = "business_cert"
                                pickImageLauncher.launch("image/*")
                            }
                        )
                    }
                }

                if (userRole == "DRIVER" || userRole == "BOTH") {
                    item {
                        DocumentItem(
                            title = "Driver's License",
                            isUploaded = "drivers_license" !in missingDocs,
                            icon = Icons.Default.Badge,
                            onUpload = {
                                pendingDocType = "drivers_license"
                                pickImageLauncher.launch("image/*")
                            }
                        )
                    }
                }

                item {
                    DocumentItem(
                        title = "Ghana Card (Front)",
                        isUploaded = "ghana_card" !in missingDocs,
                        icon = Icons.Default.CreditCard,
                        onUpload = {
                            pendingDocType = "ghana_card"
                            pickImageLauncher.launch("image/*")
                        }
                    )
                }

                if (userRole == "OWNER" || userRole == "BOTH") {
                    item {
                        DocumentItem(
                            title = "Ghana Card (Back)",
                            isUploaded = "ghana_card_back" !in missingDocs,
                            icon = Icons.Default.CreditCard,
                            onUpload = {
                                pendingDocType = "ghana_card_back"
                                pickImageLauncher.launch("image/*")
                            }
                        )
                    }
                }

                if (userRole == "DRIVER" || userRole == "BOTH") {
                    item {
                        DocumentItem(
                            title = "Insurance Certificate",
                            isUploaded = "insurance_cert" !in missingDocs,
                            icon = Icons.Default.VerifiedUser,
                            onUpload = {
                                pendingDocType = "insurance_cert"
                                pickImageLauncher.launch("image/*")
                            }
                        )
                    }

                    item {
                        DocumentItem(
                            title = "Roadworthy Certificate",
                            isUploaded = "roadworthy_cert" !in missingDocs,
                            icon = Icons.Default.DirectionsCar,
                            onUpload = {
                                pendingDocType = "roadworthy_cert"
                                pickImageLauncher.launch("image/*")
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Safety & Emergency",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    EmergencyContactSection(
                        contact1 = emergency1,
                        contact2 = emergency2,
                        onContact1Change = { emergency1 = it },
                        onContact2Change = { emergency2 = it },
                        onSave = {
                            if (emergency1.isNotEmpty() && emergency2.isNotEmpty()) {
                                scope.launch {
                                    isLoading = true
                                    repository.updateEmergencyContacts(driverId, emergency1, emergency2).onSuccess {
                                        isLoading = false
                                        Toast.makeText(context, "Contacts updated!", Toast.LENGTH_SHORT).show()
                                    }.onFailure {
                                        isLoading = false
                                        Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Please fill both contacts", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
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
                            CircularProgressIndicator(color = Color(0xFF004E89))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Processing...", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp, color = Color.LightGray)
    }
}

@Composable
fun ProfileHeader(name: String, status: String, profilePicUrl: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF004E89), Color(0xFF00355E))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profilePicUrl != null) {
                    AsyncImage(
                        model = profilePicUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(50.dp), tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = when (status) {
                    "APPROVED" -> Color(0xFF28A745)
                    "REJECTED" -> Color.Red
                    else -> Color(0xFFF39C12)
                },
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    status,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DocumentItem(title: String, isUploaded: Boolean, icon: ImageVector, onUpload: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFF004E89).copy(alpha = 0.1f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = Color(0xFF004E89)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    if (isUploaded) "Uploaded" else "Required",
                    color = if (isUploaded) Color(0xFF28A745) else Color.Red,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onUpload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isUploaded) Color(0xFFE9ECEF) else Color(0xFF004E89),
                    contentColor = if (isUploaded) Color.Black else Color.White
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(if (isUploaded) "Update" else "Upload", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun EmergencyContactSection(
    contact1: String,
    contact2: String,
    onContact1Change: (String) -> Unit,
    onContact2Change: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = contact1,
                onValueChange = onContact1Change,
                label = { Text("Emergency Contact 1") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = contact2,
                onValueChange = onContact2Change,
                label = { Text("Emergency Contact 2") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Phone, null) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89))
            ) {
                Text("Save Emergency Settings", fontWeight = FontWeight.Bold)
            }
        }
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
