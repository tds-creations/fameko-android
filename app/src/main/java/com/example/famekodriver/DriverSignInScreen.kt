package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverSignInScreen(
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSwitchToFleetLogin: () -> Unit,
    onContactSupport: () -> Unit
) {
    val context = LocalContext.current
    var selectedAuthTab by remember { mutableStateOf(1) } // 0 = OTP, 1 = Password
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var rememberDevice by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Login", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A)) },
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
                    IconButton(onClick = { Toast.makeText(context, "Driver Profile Portal", Toast.LENGTH_SHORT).show() }) {
                        Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Dark Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text("🚖 DRIVER PORTAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }

                            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.12f), modifier = Modifier.size(36.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Speed, null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Text("Driver Sign In", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                        Text("Enter your registered phone number or Ghana Card ID to start your shift.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.ElectricBolt, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                            Text("Accra Metro Corridor • Shift Telemetry & Instant Mo...", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Driver Login Input Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Auth Tab Selector (SMS OTP vs Password)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Surface(
                                    onClick = { selectedAuthTab = 0 },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedAuthTab == 0) Color.White else Color.Transparent
                                ) {
                                    Text("SMS Code (OTP)", modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedAuthTab == 0) Color(0xFF059669) else Color.Gray)
                                }

                                Surface(
                                    onClick = { selectedAuthTab = 1 },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedAuthTab == 1) Color.White else Color.Transparent
                                ) {
                                    Text("Driver PIN / Password", modifier = Modifier.padding(vertical = 10.dp), textAlign = TextAlign.Center, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedAuthTab == 1) Color(0xFF059669) else Color.Gray)
                                }
                            }
                        }

                        // Input Header
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Registered Mobile Number", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("GHANA TELECOM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }

                        // Phone Number Input
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = { Text("024 497 1225", fontSize = 14.sp, color = Color.Gray) },
                            leadingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                                ) {
                                    Text("🇬🇭 +233", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Spacer(Modifier.width(4.dp))
                                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(Color.LightGray))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        Text("Registered with MTN MoMo, Telecel Cash, or AT Money", fontSize = 11.sp, color = Color.Gray)

                        if (selectedAuthTab == 1) {
                            OutlinedTextField(
                                value = passwordInput,
                                onValueChange = { passwordInput = it },
                                label = { Text("Driver PIN or Password") },
                                leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                        Icon(
                                            imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = "Toggle password",
                                            tint = Color.Gray
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                            )
                        }

                        // Remember Device Checkbox
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { rememberDevice = !rememberDevice }) {
                            Checkbox(
                                checked = rememberDevice,
                                onCheckedChange = { rememberDevice = it },
                                colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Remember this device for instant shift start", fontSize = 12.sp, color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                        }

                        // Sign In & Go Online Button
                        Button(
                            onClick = {
                                if (phoneInput.isBlank()) {
                                    Toast.makeText(context, "Please enter your mobile number", Toast.LENGTH_SHORT).show()
                                } else {
                                    onLogin(phoneInput, passwordInput.ifEmpty { "driver123" })
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("Sign In & Go Online", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // WhatsApp Verification Support Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFF2563EB).copy(alpha = 0.15f), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Need to verify Ghana Card or DVLA Roadworthy?", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("Driver onboarding desk is live 24/7 for shift activations.", fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.clickable { onContactSupport() },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Chat, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                Text("Contact Accra Hub on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF059669))
                            }
                        }
                    }
                }
            }

            // Wrong Role Switcher Link
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("WRONG ROLE?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

                    Surface(
                        onClick = onSwitchToFleetLogin,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Switch to Fleet Owner Login", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Emergency Road SOS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text("•", fontSize = 11.sp, color = Color.Gray)
                        Text("WhatsApp Support", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                        Text("•", fontSize = 11.sp, color = Color.Gray)
                        Text("Circle & Kaneshie Hub", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
