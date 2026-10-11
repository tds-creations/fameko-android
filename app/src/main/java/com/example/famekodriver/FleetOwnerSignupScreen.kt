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
fun FleetOwnerSignupScreen(
    onBack: () -> Unit,
    onSwitchToDriverSignup: () -> Unit,
    onLoginConsole: () -> Unit,
    onSubmit: (Map<String, String>) -> Unit
) {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var ghanaCardPin by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var rgdNumber by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var fleetSizeCategory by remember { mutableStateOf(1) } // 0 = 1-3, 1 = 4-10, 2 = 10+
    var revenueSplitOption by remember { mutableStateOf(0) } // 0 = 70/30 Split, 1 = Daily Lease
    var certDvlaAgreed by remember { mutableStateOf(false) }
    var certMasterAgreed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Phone Verification", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Text("FAMEKO PARTNER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
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
                    Surface(color = Color(0xFFEFF6FF), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF2563EB), CircleShape))
                            Text("ACCRA NODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
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
            // Dark Hero Enterprise Card
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
                            Surface(color = Color(0xFF2563EB), shape = RoundedCornerShape(8.dp)) {
                                Text("FLEET OWNER ENTERPRISE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                            }
                            Surface(color = Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text("ACCRA HUB", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }
                        }

                        Text("Create Fleet Owner Account", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                        Text("Centralize vehicle telemetry, assign verified drivers, and automate 70/30 escrow yield.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Momo & GHIPSS", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                Text("DAILY PAYOUTS", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("70/30 Net", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF34D399))
                                Text("ESCROW YIELD", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                            Box(modifier = Modifier.width(1.dp).height(28.dp).background(Color.White.copy(alpha = 0.15f)))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Realtime", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                Text("DVLA AUTO-SYNC", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
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
                            onClick = onSwitchToDriverSignup,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent
                        ) {
                            Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.DirectionsCar, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Driver Partner →", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray)
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0A192F)
                        ) {
                            Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Fleet Owner", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            // Section 1: Executive Identity
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
                                Icon(Icons.Default.Badge, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Text("Executive Identity", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Text("PRINCIPAL LEGAL SIGNATORY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name (Principal Owner)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        OutlinedTextField(
                            value = ghanaCardPin,
                            onValueChange = { ghanaCardPin = it },
                            label = { Text("Ghana Card PIN (Principal)") },
                            trailingIcon = {
                                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                    Text("NIA CERTIFIED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )
                    }
                }
            }

            // Section 2: Entity & Regulatory Clearance
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
                                Icon(Icons.Default.Domain, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Text("Entity & Regulatory Clearance", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Text("RGD & GRA WITHHOLDING REGISTRY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Registered Business / Fleet Entity Name") },
                            leadingIcon = { Icon(Icons.Default.Business, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        OutlinedTextField(
                            value = rgdNumber,
                            onValueChange = { rgdNumber = it },
                            label = { Text("Registrar General's Dept (RGD) Number") },
                            leadingIcon = { Icon(Icons.Default.Description, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        Text("Verified against Ghana RGD portal within 60 seconds", fontSize = 11.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Official Fleet Governance Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Corporate Dispatch / Mobile Money Line") },
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
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )

                        Text("Used for instant 70% automated Momo or GHIPSS remittance", fontSize = 11.sp, color = Color.Gray)

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Create Enterprise Passcode") },
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
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2563EB))
                        )
                    }
                }
            }

            // Section 3: Fleet Architecture
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
                                Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                                Text("Fleet Architecture", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            }
                            Text("SCALE & COMMERCIAL MODEL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        }

                        Text("Initial Fleet Size Deployment", fontSize = 12.sp, color = Color.Gray)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("1–3 Vehicles", "4–10 Vehicles", "10+ Fleet Core").forEachIndexed { idx, label ->
                                val isSelected = fleetSizeCategory == idx
                                Surface(
                                    onClick = { fleetSizeCategory = idx },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) Color(0xFF0A192F) else Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, if (isSelected) Color(0xFF0A192F) else Color(0xFFBFDBFE))
                                ) {
                                    Text(label, modifier = Modifier.padding(vertical = 10.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else Color(0xFF2563EB), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                        }

                        Text("Preferred Revenue & Escrow Governance", fontSize = 12.sp, color = Color.Gray)

                        // Option 1: Automated 70/30 Net Split
                        Card(
                            onClick = { revenueSplitOption = 0 },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (revenueSplitOption == 0) Color(0xFFECFDF5) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (revenueSplitOption == 0) Color(0xFF059669) else Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = revenueSplitOption == 0, onClick = { revenueSplitOption = 0 }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF059669)))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("Automated 70/30 Net Split", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Surface(color = Color(0xFF059669), shape = RoundedCornerShape(6.dp)) {
                                            Text("ACCRA BENCHMARK", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                        }
                                    }
                                    Text("70% dispatched direct to owner vault per ride; 30% driver operational retention with integrated fuel offset.", fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                                }
                            }
                        }

                        // Option 2: Daily Flat Remittance / Lease
                        Card(
                            onClick = { revenueSplitOption = 1 },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (revenueSplitOption == 1) Color(0xFFECFDF5) else Color(0xFFF8FAFC)),
                            border = BorderStroke(1.dp, if (revenueSplitOption == 1) Color(0xFF059669) else Color(0xFFE2E8F0))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = revenueSplitOption == 1, onClick = { revenueSplitOption = 1 }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF059669)))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Daily Flat Remittance / Lease", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Fixed contractual daily quota (GH₵ 220–350/day) with auto-lock engine telemetry on missed settlement.", fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Compliance & Submit Button
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { certDvlaAgreed = !certDvlaAgreed }) {
                        Checkbox(checked = certDvlaAgreed, onCheckedChange = { certDvlaAgreed = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB)))
                        Spacer(Modifier.width(6.dp))
                        Text("I certify that all commercial fleet units maintain active DVLA Roadworthiness Certification and comprehensive commercial passenger insurance under Ghana National Insurance Commission (NIC) guidelines.", fontSize = 11.sp, color = Color(0xFF0F172A), lineHeight = 15.sp)
                    }

                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.clickable { certMasterAgreed = !certMasterAgreed }) {
                        Checkbox(checked = certMasterAgreed, onCheckedChange = { certMasterAgreed = it }, colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB)))
                        Spacer(Modifier.width(6.dp))
                        Text("I agree to the Fameko Enterprise Master Services & Escrow Settlement Agreement and authorize GRA Withholding Tax deduction where legally mandated.", fontSize = 11.sp, color = Color(0xFF0F172A), lineHeight = 15.sp)
                    }

                    Button(
                        onClick = {
                            if (fullName.isBlank() || phoneInput.isBlank() || passwordInput.isBlank()) {
                                Toast.makeText(context, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                            } else if (!certMasterAgreed) {
                                Toast.makeText(context, "Please accept the Master Services Agreement to continue", Toast.LENGTH_SHORT).show()
                            } else {
                                onSubmit(
                                    mapOf(
                                        "full_name" to fullName,
                                        "phone" to phoneInput,
                                        "email" to emailInput,
                                        "password" to passwordInput,
                                        "company_name" to companyName,
                                        "registration_number" to rgdNumber,
                                        "ghana_card" to ghanaCardPin,
                                        "user_role" to "OWNER"
                                    )
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0A192F))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Continue to Fleet Verification", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Text("Already an enterprise partner? ", fontSize = 12.sp, color = Color.Gray)
                        Text("Log In to Console", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB), modifier = Modifier.clickable { onLoginConsole() })
                    }

                    // Solo Driver Switch Banner
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(modifier = Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(22.dp))
                                Column {
                                    Text("Want to drive a single car yourself?", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    Text("Switch to Fameko Solo Partner Onboarding.", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            Button(
                                onClick = onSwitchToDriverSignup,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF2563EB)),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Text("Register Driver", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("CYBER SECURITY AUTHORITY (CSA) CERTIFIED  •  GRA WITHHOLDING TAX COMPLIANT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                }
            }
        }
    }
}
