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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverSignupScreen(
    onBack: () -> Unit,
    onSwitchToFleetSignup: () -> Unit,
    onLogin: () -> Unit,
    onSubmit: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var ghanaCardPin by remember { mutableStateOf("") }
    var dvlaLicenseNo by remember { mutableStateOf("") }
    var vehicleStatusOption by remember { mutableStateOf(0) } // 0 = Own vehicle, 1 = Need car
    var referralCode by remember { mutableStateOf("") }
    var certLicenseAgreed by remember { mutableStateOf(false) }
    var certTermsAgreed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Phone Verification", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Text("FAMEKO PARTNER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
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
                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("ACCRA NODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Hero Header Card
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(color = Color(0xFF059669), shape = RoundedCornerShape(8.dp)) {
                                Text("FAMEKO GH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text("LIVE DISPATCHES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }
                        }

                        Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Bolt, null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                Text("INSTANT GHANAPAY / MOMO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }

                        Text("Create Driver Partner Account", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                        Text("Drive when you want. Cash out instantly to MTN MoMo, Telecel Cash, or AT Money anywhere across Greater Accra.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("0%", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                                Text("SIGNUP FEE", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GH₵ 3,850", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF34D399))
                                Text("AVG. WKLY EARNINGS", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("< 15 min", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                                Text("ACCOUNT REVIEW", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Role Switcher Pill
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF059669)
                        ) {
                            Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Driver Partner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }

                        Surface(
                            onClick = onSwitchToFleetSignup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent
                        ) {
                            Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Domain, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Fleet Owner →", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Signing up as an individual vehicle driver. Looking to list 2+ cars? Switch to Fleet Owner above.", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
            }

            // Section 1: Driver Identity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Badge, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Text("Driver Identity", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                                Text("STEP 1 OF 3", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Legal Name") },
                            placeholder = { Text("e.g. Emmanuel Kofi Sackey") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Mobile Number (MoMo Payout Wallet)") },
                            placeholder = { Text("24 123 4567") },
                            leadingIcon = {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
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

                        Text("Must be registered for MTN MoMo, Telecel Cash, or AT Money.", fontSize = 11.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("emmanuel.sackey@gmail.com") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Create Password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                    Icon(imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = Color.Gray)
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
                }
            }

            // Section 2: Ghana Compliance & License
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.VerifiedUser, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                Text("Ghana Compliance & License", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                                Text("NIA & DVLA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }

                        OutlinedTextField(
                            value = ghanaCardPin,
                            onValueChange = { ghanaCardPin = it },
                            label = { Text("Ghana Card PIN") },
                            placeholder = { Text("GHA-719284912-3") },
                            trailingIcon = {
                                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                    Text("NIA ACTIVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = dvlaLicenseNo,
                            onValueChange = { dvlaLicenseNo = it },
                            label = { Text("DVLA Driver's License Number") },
                            placeholder = { Text("DL-2024-ACC-9821") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )
                    }
                }
            }

            // Section 3: Vehicle Operation Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                            Text("Vehicle Operation Status", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                        }

                        // Option 1: Own vehicle
                        Card(
                            onClick = { vehicleStatusOption = 0 },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (vehicleStatusOption == 0) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (vehicleStatusOption == 0) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = vehicleStatusOption == 0, onClick = { vehicleStatusOption = 0 }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB)))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("I have my own vehicle", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("You will submit your DVLA Roadworthiness and Comprehensive Insurance in the next screen.", fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                                }
                            }
                        }

                        // Option 2: Need car from Fleet Owner
                        Card(
                            onClick = { vehicleStatusOption = 1 },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (vehicleStatusOption == 1) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (vehicleStatusOption == 1) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = vehicleStatusOption == 1, onClick = { vehicleStatusOption = 1 }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF2563EB)))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("I need a car from a Fleet Owner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(6.dp)) {
                                            Text("Rental Pool", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Text("Match directly with vetted Accra fleet proprietors offering work-and-pay or daily rental arrangements.", fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = referralCode,
                            onValueChange = { referralCode = it },
                            label = { Text("Referral / Station Code (Optional)") },
                            placeholder = { Text("E.G. CIRCLE-VIP OR ACCRA-2025") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )
                    }
                }
            }

            // Section 4: Certifications & Submit Button
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { certLicenseAgreed = !certLicenseAgreed }) {
                        Checkbox(checked = certLicenseAgreed, onCheckedChange = { certLicenseAgreed = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669)))
                        Spacer(Modifier.width(6.dp))
                        Text("I certify that my Ghana Card and DVLA Driver's License are original, fully active, and registered under my name.", fontSize = 11.sp, color = Color(0xFF0F172A), lineHeight = 15.sp)
                    }

                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { certTermsAgreed = !certTermsAgreed }) {
                        Checkbox(checked = certTermsAgreed, onCheckedChange = { certTermsAgreed = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF059669)))
                        Spacer(Modifier.width(6.dp))
                        Text("I agree to the Fameko Partner Terms of Service and the daily access fee schedule.", fontSize = 11.sp, color = Color(0xFF0F172A), lineHeight = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (fullName.isBlank() || phoneInput.isBlank() || passwordInput.isBlank()) {
                                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                            } else if (!certTermsAgreed) {
                                Toast.makeText(context, "Please accept the Terms of Service to continue", Toast.LENGTH_SHORT).show()
                            } else {
                                onSubmit(
                                    mapOf(
                                        "full_name" to fullName,
                                        "phone" to phoneInput,
                                        "email" to emailInput,
                                        "password" to passwordInput,
                                        "license_number" to dvlaLicenseNo,
                                        "ghana_card" to ghanaCardPin,
                                        "user_role" to "DRIVER"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Text("Continue to License Verification", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Already have a driver account? ", fontSize = 12.sp, color = Color.Gray)
                        Text("Log In", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF059669), modifier = Modifier.clickable { onLogin() })
                    }

                    // Fleet Owner Switch Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.Domain, null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Registering 2+ Vehicles?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Access executive fleet governance and batch telematics.", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Button(
                                onClick = onSwitchToFleetSignup,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2563EB)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Text("Fleet Owner", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("DVLA GHANA REGISTERED  •  DATA PROTECTION ACT 2012", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }
    }
}
