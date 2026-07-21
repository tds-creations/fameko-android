package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.repository.RentalRepository
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetSelectionScreen(
    onBack: () -> Unit,
    onVehicleDetails: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RentalRepository() }
    val scope = rememberCoroutineScope()

    var vehicles by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var maxPrice by remember { mutableStateOf(5000f) }
    var selectedTransmission by remember { mutableStateOf("All") }

    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val categories = listOf("All", "Sedan", "SUV", "Luxury", "Truck", "Van")

    fun loadVehicles() {
        isLoading = true
        scope.launch {
            repository.getRentalVehicles().onSuccess {
                vehicles = it
                isLoading = false
            }.onFailure {
                isLoading = false
                Toast.makeText(context, "Failed to load fleet", Toast.LENGTH_SHORT).show()
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

            val matchesSearch = name.contains(searchQuery, ignoreCase = true) || model.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == "All" || type.equals(selectedCategory, ignoreCase = true)
            val matchesPrice = rate <= maxPrice
            val matchesTrans = selectedTransmission == "All" || trans.equals(selectedTransmission, ignoreCase = true)

            matchesSearch && matchesCategory && matchesPrice && matchesTrans
        }
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
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Search brand or model") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                items(categories) { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FamekoBlue.copy(alpha = 0.1f), selectedLabelColor = FamekoBlue)
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FamekoBlue)
                }
            } else if (filteredVehicles.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.DirectionsCar, null, Modifier.size(64.dp), Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No vehicles found", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredVehicles) { vehicle ->
                        CustomerVehicleCard(
                            vehicle = vehicle,
                            onSelect = { onVehicleDetails(vehicle) }
                        )
                    }
                }
            }
        }

        if (showFilterSheet) {
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                sheetState = sheetState
            ) {
                FilterSheetContent(
                    currentMaxPrice = maxPrice,
                    onPriceChange = { maxPrice = it },
                    currentTrans = selectedTransmission,
                    onTransChange = { selectedTransmission = it },
                    onApply = { scope.launch { sheetState.hide() }.invokeOnCompletion { showFilterSheet = false } }
                )
            }
        }
    }
}

@Composable
fun CustomerVehicleCard(
    vehicle: Map<String, Any>,
    onSelect: () -> Unit
) {
    val name = vehicle["name"]?.toString() ?: "Vehicle"
    val model = vehicle["model"]?.toString() ?: ""
    val type = vehicle["vehicle_type"]?.toString() ?: "Car"
    val rate = vehicle["daily_rate"]?.toString() ?: "0"
    val imageUrl = vehicle["image_urls"]?.toString()?.split(",")?.firstOrNull() ?: ""

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(BoltLightGray), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsCar, null, Modifier.size(48.dp), Color.Gray)
                    }
                }
                
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    color = FamekoBlue,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "₵$rate/day",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(name, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = BoltDark)
                        Text("$model • $type", color = Color.Gray, fontSize = 14.sp)
                    }
                    
                    Surface(color = Color(0xFFFFF9DB), shape = RoundedCornerShape(8.dp)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFF08C00), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("4.8", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE67700))
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MiniSpec(Icons.Default.People, vehicle["seats"]?.toString() ?: "5")
                    MiniSpec(Icons.Default.Settings, (vehicle["transmission"]?.toString() ?: "Auto").take(1))
                    MiniSpec(Icons.Default.LocalGasStation, (vehicle["fuel_type"]?.toString() ?: "Gas").take(1))
                }
            }
        }
    }
}

@Composable
fun MiniSpec(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun FilterSheetContent(
    currentMaxPrice: Float,
    onPriceChange: (Float) -> Unit,
    currentTrans: String,
    onTransChange: (String) -> Unit,
    onApply: () -> Unit
) {
    Column(modifier = Modifier.padding(24.dp).fillMaxWidth().navigationBarsPadding()) {
        Text("Filter Vehicles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        
        Text("Max Daily Rate: GH₵${currentMaxPrice.toInt()}", fontWeight = FontWeight.Medium)
        Slider(
            value = currentMaxPrice,
            onValueChange = onPriceChange,
            valueRange = 100f..5000f,
            colors = SliderDefaults.colors(thumbColor = FamekoBlue, activeTrackColor = FamekoBlue)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text("Transmission", fontWeight = FontWeight.Medium)
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Automatic", "Manual").forEach { type ->
                FilterChip(
                    selected = currentTrans == type,
                    onClick = { onTransChange(type) },
                    label = { Text(type) }
                )
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
        ) {
            Text("Apply Filters", fontWeight = FontWeight.Bold)
        }
    }
}
