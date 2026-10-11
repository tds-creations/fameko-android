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
    var licenseNumber by remember { mutableStateOf("") }
    var regionInput by remember { mutableStateOf("Greater Accra") }
    var vehicleTypeInput by remember { mutableStateOf("Car") }
    var vehicleNumberInput by remember { mutableStateOf("") }
    var vehicleModelInput by remember { mutableStateOf("") }
    var emergencyContactInput by remember { mutableStateOf("") }

    val vehicleTypes = listOf("Car", "Pragya", "Okada", "Truck", "Aboboyaa", "Bicycle")
    val regions = listOf("Greater Accra", "Ashanti", "Western", "Central", "Eastern", "Northern", "Volta")

    var expandedVehicleTypeDropdown by remember { mutableStateOf(false) }
    var expandedRegionDropdown by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Registration", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
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
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dark Header Card
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
                        Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                Text("🚖 CREATE DRIVER ACCOUNT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }

                        Text("Driver Registration", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                        Text("Provide your legal details and vehicle information to register for driving shifts.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)
                    }
                }
            }

            // Driver Details Form Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("PERSONAL & ACCOUNT DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text("Enter your full legal name") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Mobile Number (Phone)") },
                            placeholder = { Text("024 123 4567") },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            placeholder = { Text("driver@example.com") },
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
                            label = { Text("Password") },
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

                        OutlinedTextField(
                            value = licenseNumber,
                            onValueChange = { licenseNumber = it },
                            label = { Text("DVLA License Number") },
                            placeholder = { Text("DL-2024-ACC-9821") },
                            leadingIcon = { Icon(Icons.Default.Badge, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        // Region Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedRegionDropdown,
                            onExpandedChange = { expandedRegionDropdown = !expandedRegionDropdown }
                        ) {
                            OutlinedTextField(
                                value = regionInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Operating Region") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRegionDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                            )
                            ExposedDropdownMenu(
                                expanded = expandedRegionDropdown,
                                onDismissRequest = { expandedRegionDropdown = false }
                            ) {
                                regions.forEach { reg ->
                                    DropdownMenuItem(
                                        text = { Text(reg) },
                                        onClick = {
                                            regionInput = reg
                                            expandedRegionDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                        Text("VEHICLE DETAILS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

                        // Vehicle Type Dropdown
                        ExposedDropdownMenuBox(
                            expanded = expandedVehicleTypeDropdown,
                            onExpandedChange = { expandedVehicleTypeDropdown = !expandedVehicleTypeDropdown }
                        ) {
                            OutlinedTextField(
                                value = vehicleTypeInput,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Vehicle Type") },
                                leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVehicleTypeDropdown) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                            )
                            ExposedDropdownMenu(
                                expanded = expandedVehicleTypeDropdown,
                                onDismissRequest = { expandedVehicleTypeDropdown = false }
                            ) {
                                vehicleTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            vehicleTypeInput = type
                                            expandedVehicleTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = vehicleNumberInput,
                            onValueChange = { vehicleNumberInput = it },
                            label = { Text("Vehicle Plate Number (vehicle_number)") },
                            placeholder = { Text("e.g. GT-4821-24") },
                            leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = vehicleModelInput,
                            onValueChange = { vehicleModelInput = it },
                            label = { Text("Vehicle Model (vehicle_model)") },
                            placeholder = { Text("e.g. Toyota Vitz Silver") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        OutlinedTextField(
                            value = emergencyContactInput,
                            onValueChange = { emergencyContactInput = it },
                            label = { Text("Emergency Contact Number (Optional)") },
                            placeholder = { Text("024 000 0000") },
                            leadingIcon = { Icon(Icons.Default.ContactPhone, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        Spacer(Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (fullName.isBlank() || phoneInput.isBlank() || emailInput.isBlank() || passwordInput.isBlank() || licenseNumber.isBlank()) {
                                    Toast.makeText(context, "Please fill in all required driver details", Toast.LENGTH_SHORT).show()
                                } else {
                                    onSubmit(
                                        mapOf(
                                            "full_name" to fullName,
                                            "phone" to phoneInput,
                                            "email" to emailInput,
                                            "password" to passwordInput,
                                            "license_number" to licenseNumber,
                                            "region" to regionInput,
                                            "vehicle_type" to vehicleTypeInput,
                                            "vehicle_number" to vehicleNumberInput,
                                            "vehicle_model" to vehicleModelInput,
                                            "emergency_contact_1" to emergencyContactInput,
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
                                Text("Register Driver Account", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Role Switcher & Login Links
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        onClick = onSwitchToFleetSignup,
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
                            Icon(Icons.Default.Domain, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Switch to Fleet Owner Registration", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Row(
                        modifier = Modifier.clickable { onLogin() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text("Already registered? ", fontSize = 13.sp, color = Color.Gray)
                        Text("Log In", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF059669))
                    }
                }
            }
        }
    }
}
