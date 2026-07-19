package com.example.famekodriver

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
    val ownerId = sessionManager.getDriverId()?.toIntOrNull() ?: 0
    val isEditing = vehicle != null

    var currentPage by remember { mutableIntStateOf(1) }

    // Page 1 States
    var selectedBrand by remember { mutableStateOf(vehicle?.get("name")?.toString() ?: "Toyota") }
    var brandExpanded by remember { mutableStateOf(false) }
    
    var selectedModelYear by remember { mutableStateOf(vehicle?.get("model")?.toString() ?: Calendar.getInstance().get(Calendar.YEAR).toString()) }
    var yearExpanded by remember { mutableStateOf(false) }
    
    var selectedType by remember { mutableStateOf(vehicle?.get("vehicle_type")?.toString() ?: "Sedan") }
    var typeExpanded by remember { mutableStateOf(false) }

    var selectedTransmission by remember { mutableStateOf(vehicle?.get("transmission")?.toString() ?: "Automatic") }
    var transExpanded by remember { mutableStateOf(false) }

    var selectedFuelType by remember { mutableStateOf(vehicle?.get("fuel_type")?.toString() ?: "Petrol") }
    var fuelExpanded by remember { mutableStateOf(false) }

    var selectedSeats by remember { mutableStateOf(vehicle?.get("seats")?.toString() ?: "5") }
    var seatsExpanded by remember { mutableStateOf(false) }
    
    var number by remember { mutableStateOf(vehicle?.get("vehicle_number")?.toString() ?: "") }
    var rate by remember { mutableStateOf(vehicle?.get("daily_rate")?.toString() ?: "") }
    var description by remember { mutableStateOf(vehicle?.get("description")?.toString() ?: "") }
    var selectedFeatures by remember { 
        mutableStateOf(
            vehicle?.get("features")?.toString()?.split(",")?.filter { it.isNotBlank() }?.toSet() ?: emptySet<String>()
        ) 
    }
    
    // Page 2 States
    val existingImageUrls = vehicle?.get("image_urls")?.toString()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    var frontImage by remember { mutableStateOf<Uri?>(null) }
    var sideImage by remember { mutableStateOf<Uri?>(null) }
    var backImage by remember { mutableStateOf<Uri?>(null) }
    var interiorImage by remember { mutableStateOf<Uri?>(null) }
    
    var isUploading by remember { mutableStateOf(false) }

    val vehicleBrands = listOf(
        "Toyota", "Honda", "Nissan", "Hyundai", "Kia", "Mercedes-Benz", 
        "BMW", "Volkswagen", "Ford", "Chevrolet", "Mitsubishi", "Mazda", 
        "Suzuki", "Lexus", "Audi", "Land Rover", "Range Rover", "Jeep", "Tesla"
    ).sorted()

    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val years = (currentYear downTo 1995).map { it.toString() }

    val vehicleTypes = listOf("Sedan", "SUV", "Hatchback", "Pickup Truck", "Minivan", "Luxury", "Van", "Truck", "Motorcycle")
    val transmissions = listOf("Automatic", "Manual")
    val fuelTypes = listOf("Petrol", "Diesel", "Electric", "Hybrid")
    val seatOptions = listOf("2", "4", "5", "7", "8", "12+")
    val commonFeatures = listOf("Air Conditioning", "GPS Navigation", "Bluetooth", "Backup Camera", "Leather Seats", "Fuel Efficient", "USB Charger")

    val frontLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { frontImage = it }
    val sideLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { sideImage = it }
    val backLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { backImage = it }
    val interiorLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { interiorImage = it }

    BackHandler {
        if (currentPage > 1) currentPage-- else onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    val baseTitle = if (isEditing) "Edit Vehicle" else "Add Vehicle"
                    Text(if (currentPage == 1) "$baseTitle Details" else "$baseTitle Photos", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = { if (currentPage > 1) currentPage-- else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Step Indicator
            LinearProgressIndicator(
                progress = { if (currentPage == 1) 0.5f else 1.0f },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = Color(0xFF004E89),
                trackColor = Color(0xFFE9ECEF)
            )

            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "PageTransition"
            ) { page ->
                if (page == 1) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(vertical = 20.dp)
                    ) {
                        item {
                            Text("Basic Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }

                        item {
                            ExposedDropdownMenuBox(
                                expanded = brandExpanded,
                                onExpandedChange = { brandExpanded = !brandExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedBrand,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Vehicle Brand") },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = brandExpanded)
                                    },
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = brandExpanded,
                                    onDismissRequest = { brandExpanded = false },
                                    modifier = Modifier.background(Color.White)
                                ) {
                                    vehicleBrands.forEach { brand ->
                                        DropdownMenuItem(
                                            text = { Text(brand) },
                                            onClick = {
                                                selectedBrand = brand
                                                brandExpanded = false
                                            },
                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded)
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = yearExpanded,
                                        onDismissRequest = { yearExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        years.forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text(year) },
                                                onClick = {
                                                    selectedModelYear = year
                                                    yearExpanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = typeExpanded,
                                    onExpandedChange = { typeExpanded = !typeExpanded },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Vehicle Type") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded)
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = typeExpanded,
                                        onDismissRequest = { typeExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        vehicleTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    selectedType = type
                                                    typeExpanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                ExposedDropdownMenuBox(
                                    expanded = transExpanded,
                                    onExpandedChange = { transExpanded = !transExpanded },
                                    modifier = Modifier.weight(1.2f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedTransmission,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Transmission") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = transExpanded)
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = transExpanded,
                                        onDismissRequest = { transExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        transmissions.forEach { trans ->
                                            DropdownMenuItem(
                                                text = { Text(trans) },
                                                onClick = {
                                                    selectedTransmission = trans
                                                    transExpanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = fuelExpanded,
                                    onExpandedChange = { fuelExpanded = !fuelExpanded },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedFuelType,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Fuel Type") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fuelExpanded)
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = fuelExpanded,
                                        onDismissRequest = { fuelExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        fuelTypes.forEach { fuel ->
                                            DropdownMenuItem(
                                                text = { Text(fuel) },
                                                onClick = {
                                                    selectedFuelType = fuel
                                                    fuelExpanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }

                                ExposedDropdownMenuBox(
                                    expanded = seatsExpanded,
                                    onExpandedChange = { seatsExpanded = !seatsExpanded },
                                    modifier = Modifier.weight(0.8f)
                                ) {
                                    OutlinedTextField(
                                        value = selectedSeats,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Seats") },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = seatsExpanded)
                                        },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = seatsExpanded,
                                        onDismissRequest = { seatsExpanded = false },
                                        modifier = Modifier.background(Color.White)
                                    ) {
                                        seatOptions.forEach { seats ->
                                            DropdownMenuItem(
                                                text = { Text(seats) },
                                                onClick = {
                                                    selectedSeats = seats
                                                    seatsExpanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = number,
                                    onValueChange = { number = it },
                                    label = { Text("Plate Number") },
                                    modifier = Modifier.weight(1.2f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = rate,
                                    onValueChange = { rate = it },
                                    label = { Text("Rate (₵/Day)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        item {
                            Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = description,
                                onValueChange = { description = it },
                                label = { Text("Tell customers about this vehicle...") },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3
                            )
                        }

                        item {
                            Text("Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                commonFeatures.forEach { feature ->
                                    val isSelected = feature in selectedFeatures
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            selectedFeatures = if (isSelected) {
                                                selectedFeatures - feature
                                            } else {
                                                selectedFeatures + feature
                                            }
                                        },
                                        label = { Text(feature) },
                                        leadingIcon = if (isSelected) {
                                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    if (selectedBrand.isBlank() || number.isBlank() || rate.isBlank()) {
                                        Toast.makeText(context, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                                    } else {
                                        currentPage = 2
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89))
                            ) {
                                Text("Next: Add Photos", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                            }
                        }
                        
                        item { Spacer(Modifier.height(40.dp)) }
                    }
                } else {
                    // Page 2: Photos
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Upload High-Quality Photos",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF004E89)
                        )
                        Text(
                            "Good photos increase your rental chances by 3x.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                val frontPreview = frontImage ?: if (existingImageUrls.isNotEmpty()) Uri.parse(existingImageUrls[0]) else null
                                ImageSlot(Modifier.weight(1f).height(140.dp), "Front View", frontPreview, onAction = { frontLauncher.launch("image/*") })
                                
                                val sidePreview = sideImage ?: if (existingImageUrls.size > 1) Uri.parse(existingImageUrls[1]) else null
                                ImageSlot(Modifier.weight(1f).height(140.dp), "Side View", sidePreview, onAction = { sideLauncher.launch("image/*") })
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                val backPreview = backImage ?: if (existingImageUrls.size > 2) Uri.parse(existingImageUrls[2]) else null
                                ImageSlot(Modifier.weight(1f).height(140.dp), "Back View", backPreview, onAction = { backLauncher.launch("image/*") })
                                
                                val interiorPreview = interiorImage ?: if (existingImageUrls.size > 3) Uri.parse(existingImageUrls[3]) else null
                                ImageSlot(Modifier.weight(1f).height(140.dp), "Interior", interiorPreview, onAction = { interiorLauncher.launch("image/*") })
                            }
                        }

                        Spacer(Modifier.weight(1f))

                        Button(
                            onClick = {
                                if (frontImage == null && !isEditing) {
                                    Toast.makeText(context, "At least a Front View is required", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                scope.launch {
                                    isUploading = true
                                    val uploadedUrls = mutableListOf<String>()
                                    
                                    // Handle images: Keep existing if new one not picked
                                    val imageSources = listOf(
                                        Pair(frontImage, if (existingImageUrls.isNotEmpty()) existingImageUrls[0] else null),
                                        Pair(sideImage, if (existingImageUrls.size > 1) existingImageUrls[1] else null),
                                        Pair(backImage, if (existingImageUrls.size > 2) existingImageUrls[2] else null),
                                        Pair(interiorImage, if (existingImageUrls.size > 3) existingImageUrls[3] else null)
                                    )

                                    var uploadError = false
                                    for ((newUri, existingUrl) in imageSources) {
                                        if (newUri != null) {
                                            val file = uriToFile(context, newUri)
                                            if (file != null) {
                                                repository.uploadImage(file, "fameko_rentals").onSuccess { url ->
                                                    uploadedUrls.add(url)
                                                }.onFailure {
                                                    uploadError = true
                                                }
                                            }
                                        } else if (existingUrl != null) {
                                            uploadedUrls.add(existingUrl)
                                        }
                                    }

                                    if (uploadError) {
                                        Toast.makeText(context, "Some images failed to upload", Toast.LENGTH_SHORT).show()
                                    }

                                    val finalImageUrls = uploadedUrls.joinToString(",")
                                    
                                    if (isEditing) {
                                        repository.updateFleetVehicle(
                                            vehicleId = (vehicle?.get("id") as? Double)?.toInt() ?: 0,
                                            name = selectedBrand,
                                            model = selectedModelYear,
                                            type = selectedType,
                                            number = number,
                                            rate = rate.toDoubleOrNull() ?: 0.0,
                                            description = description,
                                            features = selectedFeatures.joinToString(","),
                                            imageUrls = finalImageUrls,
                                            status = vehicle?.get("status")?.toString(),
                                            seats = selectedSeats.replace("+", "").toIntOrNull() ?: 5,
                                            transmission = selectedTransmission,
                                            fuelType = selectedFuelType
                                        ).onSuccess {
                                            isUploading = false
                                            Toast.makeText(context, "Vehicle updated successfully!", Toast.LENGTH_SHORT).show()
                                            onComplete()
                                        }.onFailure {
                                            isUploading = false
                                            Toast.makeText(context, "Update Error: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        repository.addFleetVehicle(
                                            ownerId = ownerId,
                                            name = selectedBrand,
                                            model = selectedModelYear,
                                            type = selectedType,
                                            number = number,
                                            rate = rate.toDoubleOrNull() ?: 0.0,
                                            description = description,
                                            features = selectedFeatures.joinToString(","),
                                            imageUrls = finalImageUrls,
                                            seats = selectedSeats.replace("+", "").toIntOrNull() ?: 5,
                                            transmission = selectedTransmission,
                                            fuelType = selectedFuelType
                                        ).onSuccess {
                                            isUploading = false
                                            Toast.makeText(context, "Vehicle added successfully!", Toast.LENGTH_SHORT).show()
                                            onComplete()
                                        }.onFailure {
                                            isUploading = false
                                            Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isEditing) Color(0xFF004E89) else Color(0xFF28A745)),
                            enabled = !isUploading
                        ) {
                            if (isUploading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(if (isEditing) Icons.Default.Save else Icons.Default.CloudUpload, null)
                                Spacer(Modifier.width(12.dp))
                                Text(if (isEditing) "Save Changes" else "Complete Registration", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        
                        TextButton(
                            onClick = { currentPage = 1 },
                            modifier = Modifier.padding(top = 8.dp),
                            enabled = !isUploading
                        ) {
                            Text("Back to Details", color = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImageSlot(modifier: Modifier, label: String, uri: Uri?, onAction: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Gray.copy(alpha = 0.05f))
            .border(
                width = 1.5.dp,
                color = if (uri != null) Color(0xFF28A745) else Color.LightGray.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onAction() },
        contentAlignment = Alignment.Center
    ) {
        if (uri != null) {
            AsyncImage(
                model = uri,
                contentDescription = label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Surface(
                color = Color(0xFF28A745),
                shape = CircleShape,
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).size(24.dp)
            ) {
                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.padding(4.dp))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp),
                    shadowElevation = 2.dp
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = Color(0xFF004E89), modifier = Modifier.padding(8.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(label, fontSize = 12.sp, color = Color.DarkGray, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun uriToFile(context: android.content.Context, uri: Uri): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val file = File(context.cacheDir, "fleet_${System.currentTimeMillis()}.jpg")
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
