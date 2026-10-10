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
fun FleetOwnerLoginScreen(
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    onSwitchToDriverLogin: () -> Unit,
    onRegisterFleet: () -> Unit
) {
    val context = LocalContext.current
    var ownerInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var biometricAccess by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fleet Owner Login", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A)) },
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
                    IconButton(onClick = { Toast.makeText(context, "Fleet Owner Enterprise Portal", Toast.LENGTH_SHORT).show() }) {
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
                            Text("FLEET OWNER ENTERPRISE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("DVLA Live v3.4", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                    }
                }
            }

            // Dark Enterprise Hero Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Fleet Enterprise Sign In", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                        Text("Access your centralized vehicle telemetry, driver rosters, and aggregated yield consoles.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(shape = CircleShape, color = Color(0xFF2563EB), modifier = Modifier.size(36.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Accra Core Fleet Operations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                        Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(6.dp)) {
                                            Text("Verified", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Text("Multi-Vehicle Oversight • 70/30 Split Escrow • DVLA API Synced", fontSize = 10.sp, color = Color.LightGray)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF34D399), modifier = Modifier.size(14.dp))
                            Text("Ghana Card / Corporate TIN Validated", fontSize = 11.sp, color = Color.LightGray, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.weight(1f))
                            Text("ACCRA HUB", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }
                }
            }

            // Fleet Owner Login Input Card
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
                        Text("Owner Mobile Number or Corporate Partner ID", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))

                        // Mobile or Partner ID Input
                        OutlinedTextField(
                            value = ownerInput,
                            onValueChange = { ownerInput = it },
                            placeholder = { Text("24 000 8900 or FK-ACC-8821", fontSize = 13.sp, color = Color.Gray) },
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
                            trailingIcon = { Icon(Icons.Default.Badge, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Formats: Mobile Number or Fameko Partner ID", fontSize = 11.sp, color = Color.Gray)
                            Text("Lookup ID", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.clickable { Toast.makeText(context, "Lookup Partner ID", Toast.LENGTH_SHORT).show() })
                        }

                        // Password Field
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Enterprise Security Credential", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                            Text("Reset via MoMo SIM?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.clickable { Toast.makeText(context, "Password Reset requested", Toast.LENGTH_SHORT).show() })
                        }

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            placeholder = { Text("Enter enterprise passcode", fontSize = 13.sp, color = Color.Gray) },
                            leadingIcon = { Icon(Icons.Default.VpnKey, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
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
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        // Biometric Toggle Card
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(shape = CircleShape, color = Color(0xFF2563EB).copy(alpha = 0.15f), modifier = Modifier.size(36.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Fingerprint, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Column {
                                        Text("Biometric Console Access", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Text("Face ID or Touch Unlock", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }

                                Switch(
                                    checked = biometricAccess,
                                    onCheckedChange = { biometricAccess = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2563EB))
                                )
                            }
                        }

                        // 2FA Notice
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Message, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Text(
                                    "2FA Verification: A real-time security OTP will dispatch to your registered Stanbic/GhanaPay/MoMo corporate authority phone upon sign-in.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF1E40AF),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        // Log In to Fleet Console Button
                        Button(
                            onClick = {
                                if (ownerInput.isBlank()) {
                                    Toast.makeText(context, "Please enter owner mobile number or partner ID", Toast.LENGTH_SHORT).show()
                                } else {
                                    onLogin(ownerInput, passwordInput.ifEmpty { "owner123" })
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Log In to Fleet Console", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Need to Drive Banner Card
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("NEED TO DRIVE ONE OF YOUR VEHICLES?", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

                    Surface(
                        onClick = onSwitchToDriverLogin,
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
                            Text("Switch to Driver Partner Mode", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Surface(
                        onClick = onRegisterFleet,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Register New Fleet or Add Commercial Vehicles", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text("GHANA CYBER SECURITY AUTHORITY COMPLIANT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Text("256-bit TLS Enterprise Encryption • DVLA Telematics Bridge v3", fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }
}
