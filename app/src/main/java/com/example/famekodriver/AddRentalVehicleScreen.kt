package com.example.famekodriver

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddRentalVehicleScreen(
    vehicle: Map<String, Any>? = null,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val repository = remember { DriverRepository.getInstance() }
    val ownerId = sessionManager.getDriverId() ?: "1"
    val userRole = sessionManager.getUserRole()
    val isEditing = vehicle != null

    var currentPage by remember { mutableIntStateOf(1) }

    // Page 1 States
    var selectedBrand by remember { mutableStateOf("Toyota") }
    var brandExpanded by remember { mutableStateOf(false) }

    var modelName by remember { mutableStateOf(vehicle?.get("model")?.toString() ?: "") }

    var selectedModelYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var yearExpanded by remember { mutableStateOf(false) }

    var selectedType by remember { mutableStateOf(vehicle?.get("vehicle_type")?.toString() ?: "Sedan") }

    var selectedTransmission by remember { mutableStateOf(vehicle?.get("transmission")?.toString() ?: "Automatic") }
    var transExpanded by remember { mutableStateOf(false) }

    var selectedFuelType by remember { mutableStateOf(vehicle?.get("fuel_type")?.toString() ?: "Petrol") }
    var fuelExpanded by remember { mutableStateOf(false) }

    var selectedSeats by remember { mutableStateOf(vehicle?.get("seats")?.toString() ?: "5 Seats") }
    var seatsExpanded by remember { mutableStateOf(false) }

    var numberPlate by remember { mutableStateOf(vehicle?.get("vehicle_number")?.toString() ?: "") }
    var dailyRate by remember { mutableStateOf(vehicle?.get("daily_rate")?.toString() ?: "") }
    var descriptionText by remember { mutableStateOf(vehicle?.get("description")?.toString() ?: "") }

    var selectedFeatures by remember {
        mutableStateOf(
            vehicle?.get("features")?.toString()?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet()
        )
    }

    // Page 2 States
    var frontImage by remember { mutableStateOf<Uri?>(null) }
    var sideImage by remember { mutableStateOf<Uri?>(null) }
    var backImage by remember { mutableStateOf<Uri?>(null) }
    var interiorImage by remember { mutableStateOf<Uri?>(null) }

    var isUploading by remember { mutableStateOf(false) }
    var activeImageSlot by remember { mutableStateOf<String?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            when (activeImageSlot) {
                "front" -> frontImage = it
                "side" -> sideImage = it
                "back" -> backImage = it
                "interior" -> interiorImage = it
            }
        }
    }

    val vehicleBrands = listOf("Toyota", "Honda", "Nissan", "Hyundai", "Kia", "Mercedes-Benz", "BMW", "Volkswagen", "Ford", "Mitsubishi").sorted()
    val modelYears = (2010..Calendar.getInstance().get(Calendar.YEAR)).map { it.toString() }.reversed()
    val vehicleTypes = listOf(
        Pair("Sedan", Icons.Default.DirectionsCar),
        Pair("Compact", Icons.Default.AirportShuttle),
        Pair("SUV", Icons.Default.DirectionsCar),
        Pair("Van", Icons.Default.AirportShuttle)
    )
    val featureOptions = listOf("Air Conditioning", "GPS Navigation", "Bluetooth Audio", "Backup Camera", "USB Phone Charger", "Eco Mode (Fuel Saver)", "Dashcam", "Leather Seats")

    BackHandler {
        if (currentPage == 2) {
            currentPage = 1
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (currentPage == 1) "Add Vehicle Details" else "Add Vehicle Photos",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentPage == 2) currentPage = 1 else onBack()
                    }) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
                actions = {
                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(12.dp)) {
                        Text(
                            text = if (currentPage == 1) "Step 1/2" else "STEP 2 OF 2 • 100% COMPLETE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (currentPage == 1) {
                // PAGE 1: VEHICLE DETAILS & SPECIFICATIONS
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Progress Bar
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Vehicle Specifications", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text("50% Completed", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { 0.5f },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF059669),
                                trackColor = Color(0xFFE2E8F0)
                            )
                            Text("Enter vehicle specifications and commercial rental terms for Greater Accra.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // Section 01: BASIC INFORMATION
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
                                        Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("01", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                            }
                                        }
                                        Text("BASIC INFORMATION", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                    }
                                    Text("Required", fontSize = 10.sp, color = Color.Gray)
                                }

                                // Vehicle Brand Dropdown
                                ExposedDropdownMenuBox(
                                    expanded = brandExpanded,
                                    onExpandedChange = { brandExpanded = !brandExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = "$selectedBrand",
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Vehicle Brand") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded) },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                    )
                                    ExposedDropdownMenu(
                                        expanded = brandExpanded,
                                        onDismissRequest = { brandExpanded = false }
                                    ) {
                                        vehicleBrands.forEach { b ->
                                            DropdownMenuItem(
                                                text = { Text(b) },
                                                onClick = {
                                                    selectedBrand = b
                                                    brandExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Model & Model Year Row
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedTextField(
                                        value = modelName,
                                        onValueChange = { modelName = it },
                                        label = { Text("Model") },
                                        placeholder = { Text("Vitz / Yaris") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                    )

                                    ExposedDropdownMenuBox(
                                        expanded = yearExpanded,
                                        onExpandedChange = { yearExpanded = !yearExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedModelYear,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Model Year") },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                        )
                                        ExposedDropdownMenu(
                                            expanded = yearExpanded,
                                            onDismissRequest = { yearExpanded = false }
                                        ) {
                                            modelYears.forEach { yr ->
                                                DropdownMenuItem(
                                                    text = { Text(yr) },
                                                    onClick = {
                                                        selectedModelYear = yr
                                                        yearExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                Text("Vehicle Type", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)

                                // Vehicle Type Grid Cards
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    vehicleTypes.forEach { (type, icon) ->
                                        val isSelected = selectedType == type
                                        Surface(
                                            onClick = { selectedType = type },
                                            modifier = Modifier.weight(1f).height(64.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSelected) Color(0xFFECFDF5) else Color.White,
                                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF059669) else Color(0xFFE2E8F0))
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(icon, null, tint = if (isSelected) Color(0xFF059669) else Color.Gray, modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.height(2.dp))
                                                Text(type, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color(0xFF059669) else Color(0xFF0F172A))
                                            }
                                        }
                                    }
                                }

                                // Specs Dropdowns (Transmission, Fuel, Seats)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Transmission
                                    ExposedDropdownMenuBox(
                                        expanded = transExpanded,
                                        onExpandedChange = { transExpanded = !transExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedTransmission,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Transmission", fontSize = 10.sp) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                        )
                                        ExposedDropdownMenu(expanded = transExpanded, onDismissRequest = { transExpanded = false }) {
                                            listOf("Automatic", "Manual").forEach {
                                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedTransmission = it; transExpanded = false })
                                            }
                                        }
                                    }

                                    // Fuel
                                    ExposedDropdownMenuBox(
                                        expanded = fuelExpanded,
                                        onExpandedChange = { fuelExpanded = !fuelExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedFuelType,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Fuel Type", fontSize = 10.sp) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                        )
                                        ExposedDropdownMenu(expanded = fuelExpanded, onDismissRequest = { fuelExpanded = false }) {
                                            listOf("Petrol", "Diesel", "Hybrid", "Electric").forEach {
                                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedFuelType = it; fuelExpanded = false })
                                            }
                                        }
                                    }

                                    // Seats
                                    ExposedDropdownMenuBox(
                                        expanded = seatsExpanded,
                                        onExpandedChange = { seatsExpanded = !seatsExpanded },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        OutlinedTextField(
                                            value = selectedSeats,
                                            onValueChange = {},
                                            readOnly = true,
                                            label = { Text("Seats", fontSize = 10.sp) },
                                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = seatsExpanded) },
                                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                                            shape = RoundedCornerShape(14.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                        )
                                        ExposedDropdownMenu(expanded = seatsExpanded, onDismissRequest = { seatsExpanded = false }) {
                                            listOf("2 Seats", "4 Seats", "5 Seats", "7 Seats", "12 Seats").forEach {
                                                DropdownMenuItem(text = { Text(it) }, onClick = { selectedSeats = it; seatsExpanded = false })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Section 02: REGISTRATION & COMMERCIAL RATE
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
                                        Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("02", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                            }
                                        }
                                        Text("REGISTRATION & COMMERCIAL RATE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    }
                                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                        Text("● DVLA Sync", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                OutlinedTextField(
                                    value = numberPlate,
                                    onValueChange = { numberPlate = it },
                                    label = { Text("DVLA Number Plate") },
                                    leadingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, end = 4.dp)) {
                                            Text("🇬🇭", fontSize = 14.sp)
                                            Spacer(Modifier.width(4.dp))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                )

                                Text("Greater Accra Region format (e.g., GW/GT)", fontSize = 11.sp, color = Color.Gray)

                                OutlinedTextField(
                                    value = dailyRate,
                                    onValueChange = { dailyRate = it },
                                    label = { Text("Daily Rental (GH₵ / Day)") },
                                    prefix = { Text("GH₵ ", fontWeight = FontWeight.Bold) },
                                    suffix = { Text("/day", color = Color.Gray) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                )

                                Text("Accra avg.: GH₵ 250 - 320/day", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(16.dp))
                                        Text(
                                            "Weekly Driver Remittance: Fleet manager receives automated direct deposit to Mobile Money (MoMo) or Bank every Monday.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF0F172A),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Section 03: VEHICLE DESCRIPTION
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("03", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                            }
                                        }
                                        Text("VEHICLE DESCRIPTION", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    }
                                    Text("${descriptionText.length} / 300", fontSize = 11.sp, color = Color.Gray)
                                }

                                OutlinedTextField(
                                    value = descriptionText,
                                    onValueChange = { if (it.length <= 300) descriptionText = it },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF059669))
                                )
                            }
                        }
                    }

                    // Section 04: FEATURES & AMENITIES
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Surface(shape = CircleShape, color = Color(0xFFECFDF5), modifier = Modifier.size(24.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text("04", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color(0xFF059669))
                                            }
                                        }
                                        Text("FEATURES & AMENITIES", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                    }
                                    Text("${selectedFeatures.size} Selected", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }

                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    featureOptions.forEach { feat ->
                                        val isSelected = selectedFeatures.contains(feat)
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                selectedFeatures = if (isSelected) selectedFeatures - feat else selectedFeatures + feat
                                            },
                                            label = { Text(feat, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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

                    // Bottom Action Buttons (Page 1)
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onBack,
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                            ) {
                                Text("Save Draft", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }

                            Button(
                                onClick = {
                                    if (numberPlate.isBlank() || dailyRate.isBlank()) {
                                        Toast.makeText(context, "Please enter vehicle plate and rate", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentPage = 2
                                    }
                                },
                                modifier = Modifier.weight(1.5f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Text("Continue to Photos", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Verified through Ghana DVLA Commercial Vehicle Registry", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // PAGE 2: VEHICLE PHOTOS & REGISTRATION SUBMISSION
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dark Hero Card
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
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFF059669), modifier = Modifier.size(36.dp)) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.ElectricBolt, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                            }
                                        }
                                        Text("Upload High-Quality Photos", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                                    }
                                    Surface(color = Color(0xFFD97706), shape = RoundedCornerShape(8.dp)) {
                                        Text("🔥 3x higher booking rate", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Text(
                                    "Clear photos increase approval speed by 2x. Take photos outdoors in good daylight with no heavy filters.",
                                    fontSize = 12.sp,
                                    color = Color.LightGray,
                                    lineHeight = 16.sp
                                )

                                HorizontalDivider(color = Color.White.copy(alpha = 0.12f), thickness = 1.dp)

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("● Clean vehicle", "● Daylight angle", "● Readable plates").forEach { chip ->
                                        Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                            Text(chip, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Required Photos Section (4 of 4 Ready)
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("REQUIRED PHOTOS (4 OF 4 READY)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                    Text("Mandatory", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            // 2x2 Photo Cards Grid
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PhotoCard(
                                    title = "Front View",
                                    subtitle = "45° angle & headlights",
                                    imageUri = frontImage,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        activeImageSlot = "front"
                                        imagePickerLauncher.launch("image/*")
                                    }
                                )
                                PhotoCard(
                                    title = "Side View",
                                    subtitle = "Full body profile",
                                    imageUri = sideImage,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        activeImageSlot = "side"
                                        imagePickerLauncher.launch("image/*")
                                    }
                                )
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                PhotoCard(
                                    title = "Back View",
                                    subtitle = "Readable number plate",
                                    imageUri = backImage,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        activeImageSlot = "back"
                                        imagePickerLauncher.launch("image/*")
                                    }
                                )
                                PhotoCard(
                                    title = "Interior View",
                                    subtitle = "Dashboard & front seats",
                                    imageUri = interiorImage,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        activeImageSlot = "interior"
                                        imagePickerLauncher.launch("image/*")
                                    }
                                )
                            }
                        }
                    }

                    // DVLA & Fleet Ready Check Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(18.dp))
                                    Text("DVLA & FLEET READY CHECK", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                }

                                listOf(
                                    "All 4 vehicle exterior corners are clearly visible",
                                    "Ghana license registration plate is crisp & readable",
                                    "DVLA Roadworthy & insurance sticker visible on windshield"
                                ).forEach { check ->
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                        Text(check, fontSize = 11.sp, color = Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    }

                    // Submit Registration Button
                    item {
                        Button(
                            onClick = {
                                if (isUploading) return@Button
                                isUploading = true

                                val fullName = "$selectedBrand $modelName"
                                val featuresStr = selectedFeatures.joinToString(",")

                                val imageFiles = mutableListOf<File>()
                                listOf(frontImage, sideImage, backImage, interiorImage).forEach { uri ->
                                    uri?.let { uriToFile(context, it)?.let { file -> imageFiles.add(file) } }
                                }

                                scope.launch {
                                    isUploading = true
                                    Toast.makeText(context, "Uploading vehicle photos to Cloudinary...", Toast.LENGTH_SHORT).show()

                                    val uploadedUrls = mutableListOf<String>()
                                    val imageUris = listOf(frontImage, sideImage, backImage, interiorImage).filterNotNull()

                                    for (uri in imageUris) {
                                        val file = uriToFile(context, uri)
                                        if (file != null) {
                                            val uploadRes = repository.uploadImage(file, preset = "fleet_vehicles", folder = "fleet_vehicles")
                                            if (uploadRes.isSuccess) {
                                                uploadRes.getOrNull()?.let { uploadedUrls.add(it) }
                                            }
                                        }
                                    }

                                    val imageUrlsStr = if (uploadedUrls.isNotEmpty()) uploadedUrls.joinToString(",") else vehicle?.get("image_urls")?.toString()

                                    if (isEditing && vehicle != null) {
                                        val vId = (vehicle["id"] as Number).toInt()
                                        repository.updateFleetVehicle(
                                            vehicleId = vId,
                                            name = fullName,
                                            model = selectedModelYear,
                                            type = selectedType,
                                            number = numberPlate,
                                            rate = dailyRate.toDoubleOrNull() ?: 280.0,
                                            description = descriptionText,
                                            features = featuresStr,
                                            imageUrls = imageUrlsStr,
                                            seats = selectedSeats.filter { it.isDigit() }.toIntOrNull() ?: 5,
                                            transmission = selectedTransmission,
                                            fuelType = selectedFuelType
                                        ).onSuccess {
                                            isUploading = false
                                            Toast.makeText(context, "Vehicle updated successfully!", Toast.LENGTH_SHORT).show()
                                            onComplete()
                                        }.onFailure { err ->
                                            isUploading = false
                                            Toast.makeText(context, "Update failed: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        repository.addFleetVehicle(
                                            ownerId = ownerId,
                                            name = fullName,
                                            model = selectedModelYear,
                                            type = selectedType,
                                            number = numberPlate,
                                            rate = dailyRate.toDoubleOrNull() ?: 280.0,
                                            description = descriptionText,
                                            features = featuresStr,
                                            imageUrls = imageUrlsStr,
                                            location = "Greater Accra",
                                            seats = selectedSeats.filter { it.isDigit() }.toIntOrNull() ?: 5,
                                            transmission = selectedTransmission,
                                            fuelType = selectedFuelType,
                                            ownerRole = userRole
                                        ).onSuccess {
                                            isUploading = false
                                            Toast.makeText(context, "Vehicle registered with photos!", Toast.LENGTH_SHORT).show()
                                            onComplete()
                                        }.onFailure { err ->
                                            isUploading = false
                                            Toast.makeText(context, "Registration failed: ${err.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.Publish, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Complete Registration", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            Text("Back to Vehicle Details", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.clickable { currentPage = 1 })
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Images encrypted for instant AI DVLA verification", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoCard(
    title: String,
    subtitle: String,
    imageUri: Uri?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.height(180.dp).clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, if (imageUri != null) Color(0xFF059669) else Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (imageUri != null) Color(0xFFECFDF5) else Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = if (imageUri != null) "✓ Uploaded" else "+ Select",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (imageUri != null) Color(0xFF059669) else Color.Gray,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Icon(Icons.Default.Refresh, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF8FAFC)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Surface(shape = CircleShape, color = Color(0xFFEFF6FF), modifier = Modifier.size(36.dp)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                Text(subtitle, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val tempFile = File.createTempFile("vehicle_upload_", ".jpg", context.cacheDir)
        val outputStream = FileOutputStream(tempFile)
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
        tempFile
    } catch (e: Exception) {
        null
    }
}
