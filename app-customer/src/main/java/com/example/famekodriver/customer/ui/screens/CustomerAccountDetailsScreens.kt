package com.example.famekodriver.customer.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import java.io.File
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import com.example.famekodriver.customer.ui.theme.FamekoBlue
import com.example.famekodriver.core.domain.model.SavedPlace

@Composable
fun AccountDetailScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                }

                content()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    viewModel: CustomerMapViewModel,
    profile: Map<String, Any>?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var name by remember(profile) { mutableStateOf(profile?.get("name")?.toString() ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.get("email")?.toString() ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.get("phone")?.toString() ?: "") }
    var address by remember(profile) { mutableStateOf(profile?.get("address")?.toString() ?: "") }
    var region by remember(profile) { mutableStateOf(profile?.get("region")?.toString() ?: "") }
    var profilePicUrl by remember(profile) { mutableStateOf(profile?.get("profile_picture")?.toString() ?: "") }
    
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var showRegionDropdown by remember { mutableStateOf(false) }

    val ghanaRegions = listOf(
        "Greater Accra",
        "Ashanti (Kumasi)",
        "Western (Takoradi)",
        "Central (Cape Coast)",
        "Eastern (Koforidua)",
        "Volta (Ho)",
        "Northern (Tamale)",
        "Upper East",
        "Upper West",
        "Bono"
    )

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    if (showRegionDropdown) {
        AlertDialog(
            onDismissRequest = { showRegionDropdown = false },
            title = { Text("Select Region", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ghanaRegions.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    region = r
                                    showRegionDropdown = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = region == r, onClick = { region = r; showRegionDropdown = false })
                            Spacer(Modifier.width(8.dp))
                            Text(r, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoltDark)
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionDropdown = false }) {
                    Text("Close")
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        if (profile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header Row at absolute top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Edit Profile", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                }

                // Avatar Header
                Box(
                    modifier = Modifier.clickable { launcher.launch("image/*") }
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BoltLightGray,
                        modifier = Modifier.size(96.dp),
                        border = BorderStroke(2.dp, Color(0xFFE2E8F0))
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else if (profilePicUrl.isNotEmpty()) {
                            AsyncImage(
                                model = profilePicUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                            }
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = FamekoBlue,
                        modifier = Modifier.size(32.dp).align(Alignment.BottomEnd),
                        border = BorderStroke(2.dp, Color.White)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Text("Tap photo to change", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(4.dp))

                // Card Input Fields
                ModernEditableField(label = "FULL NAME", value = name, onValueChange = { name = it }, icon = Icons.Default.Person)
                ModernEditableField(label = "PHONE NUMBER", value = phone, onValueChange = { phone = it }, icon = Icons.Default.Phone, showVerified = true)
                ModernEditableField(label = "EMAIL ADDRESS", value = email, onValueChange = { email = it }, icon = Icons.Default.Email, showVerified = true)
                ModernEditableField(label = "STREET ADDRESS / HOME LOCATION", value = address, onValueChange = { address = it }, icon = Icons.Default.LocationOn)
                ModernEditableField(label = "REGION", value = region, onValueChange = { region = it }, icon = Icons.Default.Public, showDropdown = true, onClick = { showRegionDropdown = true })

                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = { Toast.makeText(context, "Delete Account requested", Toast.LENGTH_SHORT).show() }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.DeleteForever, null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                        Text("Delete Account", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        isSaving = true
                        val file = selectedImageUri?.let { getFileFromUri(context, it) }
                        viewModel.updateProfile(name, email, phone, address, region, file) { success, message ->
                            isSaving = false
                            if (success) {
                                Toast.makeText(context, message ?: "Profile updated successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, message ?: "Failed to update profile", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = !isSaving,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
                    elevation = ButtonDefaults.buttonElevation(4.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("Save Changes", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun ModernEditableField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    showVerified: Boolean = false,
    showDropdown: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                if (showVerified) {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "✓ Verified",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    enabled = onClick == null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold, color = BoltDark)
                )
                if (showDropdown) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun getFileFromUri(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "profile_pic_${System.currentTimeMillis()}.jpg")
        file.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        file
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun CustomerPaymentScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Payment", onBack = onBack) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Payment Methods", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                PaymentMethodItem(icon = Icons.Default.Money, title = "Cash", isSelected = true)
                HorizontalDivider(color = Color(0xFFF1F5F9))
                PaymentMethodItem(icon = Icons.Default.Wallet, title = "Fameko Balance", subtitle = "₵142.50")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Add Payment Method", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
        Spacer(modifier = Modifier.height(8.dp))
        
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                AddPaymentItem(icon = Icons.Default.CreditCard, title = "Credit/Debit Card")
                HorizontalDivider(color = Color(0xFFF1F5F9))
                AddPaymentItem(icon = Icons.Default.Smartphone, title = "Mobile Money")
            }
        }
    }
}

@Composable
fun PaymentMethodItem(icon: ImageVector, title: String, subtitle: String? = null, isSelected: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = FamekoBlue.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, color = BoltDark, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF10B981))
        }
    }
}

@Composable
fun AddPaymentItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = BoltDark, fontSize = 15.sp)
        Icon(Icons.Default.Add, contentDescription = null, tint = FamekoBlue)
    }
}

@Composable
fun CustomerSafetyScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Safety", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                }

                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                        Text("Protected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                }
            }

            // Hero Banner Card: Fameko Shield 24/7
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = Color.White.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("◗ ALWAYS ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Fameko Shield 24/7", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("Continuous GPS tracking, vetted drivers, and encrypted trip telemetry for every mile.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }
                }
            }

            // Section: TRIP PROTECTION & TOOLS
            Text("TRIP PROTECTION & TOOLS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column {
                    SafetyRowItem(
                        icon = Icons.Default.Shield,
                        iconColor = Color(0xFF2563EB),
                        title = "Safety Toolkit",
                        subtitle = "Quickly share your ride details or contact emergency services with one tap.",
                        badgeText = "Quick Access",
                        badgeColor = Color(0xFFF1F5F9),
                        badgeTextColor = Color(0xFF475569),
                        onClick = { Toast.makeText(context, "Opening Safety Toolkit...", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SafetyRowItem(
                        icon = Icons.Default.Group,
                        iconColor = Color(0xFF7C3AED),
                        title = "Trusted Contacts",
                        subtitle = "Share your trip status with friends and family automatically when you ride after dark.",
                        badgeText = "3 Linked",
                        badgeColor = Color(0xFFEFF6FF),
                        badgeTextColor = Color(0xFF2563EB),
                        onClick = { Toast.makeText(context, "3 Trusted Contacts active", Toast.LENGTH_SHORT).show() }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    SafetyRowItem(
                        icon = Icons.Default.Lock,
                        iconColor = Color(0xFF0284C7),
                        title = "Ride Check",
                        subtitle = "We'll proactively check in if your trip stops unexpectedly, delays, or deviates from the planned route.",
                        badgeText = "● Active",
                        badgeColor = Color(0xFFECFDF5),
                        badgeTextColor = Color(0xFF059669),
                        onClick = { Toast.makeText(context, "Ride Check telemetry active", Toast.LENGTH_SHORT).show() }
                    )
                }
            }

            // Emergency Assistance Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFECDD3))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PriorityHigh, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text("Emergency Assistance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF991B1B))
                            Text("Need urgent help right now?", fontSize = 12.sp, color = Color(0xFFDC2626))
                        }
                    }

                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:112"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Call 112 Emergency Dispatch", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SafetyRowItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    badgeTextColor: Color,
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
                Surface(
                    color = badgeColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(badgeText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagePlacesScreen(
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit,
    onAddPlace: (String) -> Unit,
    onEditPlace: (Int, String) -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.savedPlacesUiState.collectAsState()
    val savedPlacesLocal by viewModel.savedPlaces.collectAsState()

    val homePlace = savedPlacesLocal.find { it.label.equals("Home", ignoreCase = true) }
    val workPlace = savedPlacesLocal.find { it.label.equals("Work", ignoreCase = true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Manage Places", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                }

                IconButton(onClick = { onAddPlace("Favorite") }) {
                    Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // Info Banner Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2563EB).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Quickly access and set your favorite pickup and drop-off destinations for 1-tap bookings.",
                        fontSize = 12.sp,
                        color = Color(0xFF1E40AF),
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 1: QUICK ACCESS & FAVORITES
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF2563EB), CircleShape))
                    Text("QUICK ACCESS & FAVORITES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                }
                Text("${savedPlacesLocal.size} Saved", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column {
                    // Home
                    SavedPlaceRowItem(
                        icon = Icons.Default.Home,
                        iconColor = Color(0xFF2563EB),
                        title = "Home",
                        subtitle = homePlace?.address ?: "Temple Street, Santa Maria, Sowutuom",
                        badge = "Primary",
                        subBadge = "● Set as routine pickup",
                        onClick = { if (homePlace != null) onEditPlace(homePlace.id.toInt(), "Home") else onAddPlace("Home") }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                    // Work
                    SavedPlaceRowItem(
                        icon = Icons.Default.Work,
                        iconColor = Color(0xFF7C3AED),
                        title = "Work",
                        subtitle = workPlace?.address ?: "Trako Street, Nyamekye, Abeka",
                        subBadge = "Usual hours: 8:00 AM - 5:30 PM",
                        onClick = { if (workPlace != null) onEditPlace(workPlace.id.toInt(), "Work") else onAddPlace("Work") }
                    )
                }
            }

            // Section 2: OTHER PLACES
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color.Gray, CircleShape))
                    Text("OTHER PLACES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                }

                Surface(
                    onClick = { onAddPlace("Favorite") },
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text("+ Add new", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            // Add new saved place card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAddPlace("Favorite") },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF2563EB),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Add a new saved place", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                        Text("Save airport, gym, school or any spot", fontSize = 12.sp, color = Color.Gray)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                }
            }

            // Suggested categories
            Text("SUGGESTED CATEGORIES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestedCategoryChip("💪 Gym") { onAddPlace("Gym") }
                SuggestedCategoryChip("❤️ Partner's Place") { onAddPlace("Partner") }
                SuggestedCategoryChip("✈️ Airport (Accra)") { onAddPlace("Airport") }
            }

            // Footer Status
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                    Text("Accra GPS sync active", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Text("Privacy & Sharing", fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun SuggestedCategoryChip(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Text(text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}

@Composable
fun SavedPlaceRowItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badge: String? = null,
    subBadge: String? = null,
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
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                badge?.let {
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(it, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            subBadge?.let {
                Spacer(Modifier.height(2.dp))
                Text(it, fontSize = 11.sp, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
            }
        }
        Icon(Icons.Default.DeleteOutline, null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
    }
}

@Composable
fun CustomerFamilyProfileScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Family Profile", onBack = onBack) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FamilyRestroom, null, tint = Color(0xFF2563EB), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Family Profile", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.height(4.dp))
                Text("Manage and pay for your family's rides with shared billing and live trip tracking.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun CustomerWorkProfileScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Work Profile", onBack = onBack) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.BusinessCenter, null, tint = Color(0xFF2563EB), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Work Profile", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                Spacer(Modifier.height(4.dp))
                Text("Separate work rides, automate business expense receipts, and connect company billing.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}
