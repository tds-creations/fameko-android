package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// Helper: single source-of-truth for vehicle availability derived from status.
// Trust `status` first; fall back to `is_available` only when status is absent.
// ─────────────────────────────────────────────────────────────────────────────
private fun vehicleStatus(vehicle: Map<String, Any>): String =
    vehicle["status"]?.toString()?.uppercase()?.trim() ?: "UNKNOWN"

private fun isVehicleAvailable(vehicle: Map<String, Any>): Boolean {
    val status = vehicleStatus(vehicle)
    // Explicitly unavailable statuses take priority over the is_available flag
    if (status in listOf("BOOKED", "OCCUPIED", "IN_USE", "MAINTENANCE", "UNAVAILABLE")) return false
    // Only mark available when status is explicitly AVAILABLE
    if (status == "AVAILABLE") {
        return vehicle["is_available"] == true || vehicle["is_available"] == "true"
    }
    // Unknown/other statuses — treat as unavailable to be safe
    return false
}

private fun vehicleStatusLabel(vehicle: Map<String, Any>): String =
    when (vehicleStatus(vehicle)) {
        "AVAILABLE"              -> "Available"
        "BOOKED"                 -> "Booked"
        "OCCUPIED", "IN_USE"     -> "On Trip"
        "MAINTENANCE"            -> "Maintenance"
        else                     -> "Unavailable"
    }

private fun vehicleStatusColor(vehicle: Map<String, Any>): Color =
    when (vehicleStatus(vehicle)) {
        "AVAILABLE"              -> BoltGreen
        "BOOKED"                 -> Color(0xFFF08C00)
        "OCCUPIED", "IN_USE"     -> FamekoBlue
        else                     -> Color.Red
    }

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetSelectionScreen(
    onBack: () -> Unit,
    onVehicleDetails: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DriverRepository.getInstance() }
    val scope = rememberCoroutineScope()

    var vehicles by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Filtering States
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var maxPrice by remember { mutableStateOf(5000f) }
    var selectedTransmission by remember { mutableStateOf("All") }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val categories = listOf("All", "Sedan", "SUV", "Luxury", "Truck", "Pickup Truck", "Van", "Minivan")

    fun loadVehicles() {
        isLoading = true
        errorMessage = null
        scope.launch {
            repository.getRentalVehicles().onSuccess {
                vehicles = it
                isLoading = false
            }.onFailure { e ->
                isLoading = false
                errorMessage = "Failed to load fleet: ${e.message}"
                Toast.makeText(context, "Connection error. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) { loadVehicles() }

    val filteredVehicles = remember(vehicles, searchQuery, selectedCategory, maxPrice, selectedTransmission) {
        vehicles.filter { vehicle ->
            val name = vehicle["name"]?.toString() ?: ""
            val model = vehicle["model"]?.toString() ?: ""
            val type = vehicle["vehicle_type"]?.toString() ?: ""
            val rate = vehicle["daily_rate"]?.toString()?.toDoubleOrNull() ?: 0.0
            val trans = vehicle["transmission"]?.toString() ?: "Auto"

            val matchesSearch = name.contains(searchQuery, ignoreCase = true) ||
                    model.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || type.equals(selectedCategory, ignoreCase = true)
            val matchesPrice = rate <= maxPrice
            val matchesTrans = selectedTransmission == "All" || trans.equals(selectedTransmission, ignoreCase = true)

            matchesSearch && matchesCategory && matchesPrice && matchesTrans
        }.sortedByDescending { isVehicleAvailable(it) } // available vehicles float to top
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Fleet", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        BadgedBox(badge = {
                            if (maxPrice < 5000f || selectedTransmission != "All") Badge { Text("!") }
                        }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                    IconButton(onClick = { loadVehicles() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Search Bar
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Category Selector
            CategorySelector(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { selectedCategory = it }
            )

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = BoltGreen)
                            Spacer(Modifier.height(16.dp))
                            Text("Fetching available vehicles...", color = Color.Gray, fontSize = 14.sp)
                        }
                    }

                    errorMessage != null -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(64.dp), Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text(errorMessage!!, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { loadVehicles() },
                                colors = ButtonDefaults.buttonColors(containerColor = BoltGreen)
                            ) { Text("Retry") }
                        }
                    }

                    filteredVehicles.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, Modifier.size(80.dp), Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text("No vehicles match your filters", fontWeight = FontWeight.Bold, color = BoltDark)
                            Text(
                                "Try adjusting your search, category, or price range",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            TextButton(onClick = {
                                searchQuery = ""
                                selectedCategory = "All"
                                maxPrice = 5000f
                                selectedTransmission = "All"
                            }) {
                                Text("Reset all filters", color = BoltGreen)
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "${filteredVehicles.size} vehicles available",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray
                                )
                            }
                            items(filteredVehicles) { vehicle ->
                                CustomerVehicleCard(
                                    vehicle = vehicle,
                                    onSelect = { onVehicleDetails(vehicle) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState,
                containerColor = Color.White
            ) {
                FilterSheetContent(
                    currentMaxPrice = maxPrice,
                    onPriceChange = { maxPrice = it },
                    currentTrans = selectedTransmission,
                    onTransChange = { selectedTransmission = it },
                    onApply = {
                        scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false }
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FilterSheetContent(
    currentMaxPrice: Float,
    onPriceChange: (Float) -> Unit,
    currentTrans: String,
    onTransChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(24.dp)
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Text("Filter Vehicles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(24.dp))

        Text("Max Price per Day (₵)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(8.dp))
        Text("Up to ₵${currentMaxPrice.toInt()}", color = BoltGreen, fontWeight = FontWeight.Bold)
        Slider(
            value = currentMaxPrice,
            onValueChange = onPriceChange,
            valueRange = 100f..5000f,
            steps = 49,
            colors = SliderDefaults.colors(thumbColor = BoltGreen, activeTrackColor = BoltGreen)
        )

        Spacer(Modifier.height(24.dp))

        Text("Transmission", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Automatic", "Manual").forEach { type ->
                FilterChip(
                    selected = currentTrans == type,
                    onClick = { onTransChange(type) },
                    label = { Text(type) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BoltGreen.copy(alpha = 0.1f),
                        selectedLabelColor = BoltGreen
                    )
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BoltGreen)
        ) {
            Text("Apply Filters", fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text("Search brand or model", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null, tint = Color.Gray) } }
        } else null,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = BoltGreen,
            unfocusedIndicatorColor = Color.LightGray.copy(alpha = 0.5f)
        ),
        singleLine = true
    )
}

@Composable
fun CategorySelector(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = BoltGreen.copy(alpha = 0.15f),
                    selectedLabelColor = BoltGreen
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CustomerVehicleCard(
    vehicle: Map<String, Any>,
    onSelect: () -> Unit
) {
    // ── DEBUG LOG ────────────────────────────────────────────────────────────
    LaunchedEffect(vehicle["id"]) {
        println("DEBUG_CUSTOMER: Vehicle [${vehicle["id"]}] name=${vehicle["name"]} status=${vehicle["status"]} is_avail=${vehicle["is_available"]}")
    }
    // ─────────────────────────────────────────────────────────────────────────
    val name = vehicle["name"]?.toString() ?: "Unknown Vehicle"
    val model = vehicle["model"]?.toString() ?: ""
    val type = vehicle["vehicle_type"]?.toString() ?: "Car"
    val rate = vehicle["daily_rate"]?.toString() ?: "0"
    val imageUrls = vehicle["image_urls"]?.toString()
        ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    val seats = vehicle["seats"]?.toString()?.toDoubleOrNull()?.toInt() ?: 5
    val transmission = vehicle["transmission"]?.toString() ?: "Auto"
    val fuelType = vehicle["fuel_type"]?.toString() ?: "Petrol"

    // ── FIX: derive all display state from the helper functions ──────────────
    val available = isVehicleAvailable(vehicle)
    val statusLabel = vehicleStatusLabel(vehicle)
    val statusDotColor = vehicleStatusColor(vehicle)
    // ─────────────────────────────────────────────────────────────────────────

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Column {
            // ── Hero image + overlays ─────────────────────────────────────────
            Box(modifier = Modifier.height(200.dp).fillMaxWidth()) {
                if (imageUrls.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrls.first(),
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DirectionsCar, null, Modifier.size(48.dp), Color.Gray)
                    }
                }

                // Price badge (top-left)
                Surface(
                    color = BoltGreen,
                    shape = RoundedCornerShape(bottomEnd = 16.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("₵$rate", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        Text(" / day", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)
                    }
                }

                // Status badge (bottom-right) — driven by statusLabel & statusDotColor
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusDotColor)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = statusLabel,
                            color = if (available) BoltDark else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Card body ─────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            name,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = if (available) BoltDark else Color.Gray
                        )
                        Text("$model • $type", color = Color.Gray, fontSize = 14.sp)
                    }

                    // Rating chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF9DB))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Star, null, tint = Color(0xFFF08C00), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("4.8", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE67700))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    MiniSpecItem(Icons.Default.People, "$seats")
                    MiniSpecItem(Icons.Default.Settings, transmission.take(1).uppercase())
                    MiniSpecItem(Icons.Default.LocalGasStation, fuelType.take(1).uppercase())
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onSelect,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (available) BoltGreen else Color(0xFFF08C00)
                    )
                ) {
                    Text(
                        // Show contextual label based on real status
                        text = when (vehicleStatus(vehicle)) {
                            "AVAILABLE"          -> "View Details"
                            "BOOKED"             -> "View Schedule"
                            "OCCUPIED", "IN_USE" -> "Currently On Trip"
                            else                 -> "View Details"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MiniSpecItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}
