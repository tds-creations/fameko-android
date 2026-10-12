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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VehicleRegistrationScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DriverRepository.getInstance() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var vehicleName by remember { mutableStateOf(sessionManager.getVehicleType() ?: "") }
    var vehicleModel by remember { mutableStateOf("") }
    var plateNumber by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("car") }
    var selectedService by remember { mutableStateOf("BOTH") }
    var isSubmitting by remember { mutableStateOf(false) }

    val vehicleTypes = listOf(
        "car" to "Taxi / Car",
        "okada" to "Motor Okada",
        "aboboyaa" to "Abobo Yaa",
        "praggia" to "Praggia",
        "truck" to "Commercial Truck",
        "bicycle" to "Bicycle Courier"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register Your Vehicle", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A)) },
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                Text("🚘 VEHICLE REGISTRATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }

                        Text("Register Commercial Vehicle", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color.White)
                        Text("Enter your official DVLA registered vehicle specs to start accepting ride requests and package deliveries in Accra.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)

                        HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("● DVLA Connected", "● Auto Insurance Check", "● Instant Dispatch").forEach { chip ->
                                Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                    Text(chip, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: VEHICLE SPECIFICATIONS
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("VEHICLE SPECIFICATIONS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

                        OutlinedTextField(
                            value = vehicleName,
                            onValueChange = { vehicleName = it },
                            label = { Text("Vehicle Make & Name") },
                            placeholder = { Text("e.g. Toyota Vitz Silver") },
                            leadingIcon = { Icon(Icons.Default.DirectionsCar, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = vehicleModel,
                                onValueChange = { vehicleModel = it },
                                label = { Text("Model Year") },
                                placeholder = { Text("2022") },
                                leadingIcon = { Icon(Icons.Default.Event, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                            )

                            OutlinedTextField(
                                value = plateNumber,
                                onValueChange = { plateNumber = it },
                                label = { Text("Plate Number") },
                                placeholder = { Text("GT-4005-24") },
                                leadingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
                                        Text("🇬🇭", fontSize = 12.sp)
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                            )
                        }

                        Text("Select Vehicle Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            vehicleTypes.forEach { (id, label) ->
                                val isSelected = selectedType == id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedType = id },
                                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = {
                                        if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(14.dp))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFECFDF5),
                                        selectedLabelColor = Color(0xFF059669)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: SERVICE DISPATCH GOVERNANCE
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("SERVICE DISPATCH GOVERNANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

                        listOf(
                            Triple("BOTH", "⚡ Both Services", "Maximum daily earnings • Auto-matched ride requests & courier packages"),
                            Triple("RIDE_HAILING", "🚖 Ride Hailing Only", "Accept passenger passenger trips across Greater Accra corridor"),
                            Triple("PACKAGE_DELIVERY", "📦 Package Delivery Only", "Accept freight, parcel, and e-commerce deliveries")
                        ).forEach { (code, title, desc) ->
                            val isSelected = selectedService == code
                            Surface(
                                onClick = { selectedService = code },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF059669) else Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { selectedService = code },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF059669))
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                        Spacer(Modifier.height(2.dp))
                                        Text(desc, fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Action Button
            item {
                Button(
                    onClick = {
                        if (vehicleName.isBlank() || plateNumber.isBlank() || selectedType.isBlank()) {
                            Toast.makeText(context, "Please fill in vehicle name and plate number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isSubmitting = true
                        scope.launch {
                            val driverId = sessionManager.getDriverId() ?: return@launch
                            repository.updateDriverVehicle(
                                driverId = driverId,
                                type = selectedType,
                                number = plateNumber,
                                model = "$vehicleName $vehicleModel".trim(),
                                service = selectedService
                            ).onSuccess {
                                isSubmitting = false
                                Toast.makeText(context, "Vehicle registered successfully!", Toast.LENGTH_SHORT).show()
                                sessionManager.saveSession(
                                    driverId = driverId,
                                    driverName = sessionManager.getDriverName() ?: "",
                                    status = sessionManager.getDriverStatus(),
                                    phone = sessionManager.getDriverPhone() ?: "",
                                    role = sessionManager.getUserRole(),
                                    vehicleType = "$vehicleName ($plateNumber)"
                                )
                                onComplete()
                            }.onFailure {
                                isSubmitting = false
                                Toast.makeText(context, "Registration failed: ${it.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Icon(Icons.Default.Lock, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Complete Vehicle Registration", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Verified through Republic of Ghana DVLA Commercial Transport Registry", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
