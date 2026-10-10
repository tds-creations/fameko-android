package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.repository.RentalRepository
import kotlinx.coroutines.launch
import java.util.Locale

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

    val categories = listOf("All (14)", "Sedan", "SUV", "Luxury", "Truck")

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

            val baseCat = selectedCategory.substringBefore(" (")
            val matchesSearch = name.contains(searchQuery, ignoreCase = true) || model.contains(searchQuery, ignoreCase = true)
            val matchesCategory = baseCat == "All" || type.equals(baseCat, ignoreCase = true)
            val matchesPrice = rate <= maxPrice
            val matchesTrans = selectedTransmission == "All" || trans.equals(selectedTransmission, ignoreCase = true)

            matchesSearch && matchesCategory && matchesPrice && matchesTrans
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Available Fleet", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                                Text("ACCRA • SELF-DRIVE & CHAUFFEUR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Tune, contentDescription = "Filter", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            IconButton(onClick = { loadVehicles() }) {
                                Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Search Bar
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search brand, model, or category...", fontSize = 13.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp)) },
                        trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) } },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color(0xFF2563EB),
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        )
                    )
                }

                // Category Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            val isSelected = selectedCategory == category
                            Surface(
                                onClick = { selectedCategory = category },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = category,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Booking Date & Location Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF2563EB),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column {
                                    Text("Today, 10:00 AM → Tomorrow, 10:00 AM", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(5.dp).background(Color(0xFF2563EB), CircleShape))
                                        Text("Accra Central (Self-pickup or Delivery)", fontSize = 11.sp, color = Color.Gray)
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = { Toast.makeText(context, "Changing rental duration...", Toast.LENGTH_SHORT).show() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.height(32.dp),
                                border = BorderStroke(1.dp, Color(0xFF2563EB))
                            ) {
                                Text("Change", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }

                // Vehicle List Items
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    }
                } else if (filteredVehicles.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, Modifier.size(64.dp), Color.LightGray)
                            Spacer(Modifier.height(16.dp))
                            Text("No vehicles found", fontWeight = FontWeight.Bold, color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredVehicles) { vehicle ->
                        CustomerVehicleCard(
                            vehicle = vehicle,
                            onSelect = { onVehicleDetails(vehicle) }
                        )
                    }
                }

                // Custom Fleet Request Banner
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Need a custom vehicle or fleet?", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.height(4.dp))
                                Text("Weddings, escorts, VIP convoys, or monthly corporate leases.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Request Custom Quotation →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF38BDF8),
                                    modifier = Modifier.clickable { Toast.makeText(context, "Opening Custom Quotation Request...", Toast.LENGTH_SHORT).show() }
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.1f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
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
    val context = LocalContext.current
    val name = vehicle["name"]?.toString() ?: "Vehicle"
    val model = (vehicle["model"]?.toString() ?: "").ifEmpty { (vehicle["vehicle_type"]?.toString() ?: "Car") }
    val type = vehicle["vehicle_type"]?.toString() ?: "Car"
    val rateDouble = vehicle["daily_rate"]?.toString()?.toDoubleOrNull() ?: 450.0
    val rate = String.format(Locale.US, "%.0f", rateDouble)
    val imageUrls = vehicle["image_urls"]?.toString() ?: ""
    val imageUrl = imageUrls.split(",").firstOrNull { it.isNotBlank() } ?: ""
    val seats = vehicle["seats"]?.toString() ?: "5"
    val transmission = vehicle["transmission"]?.toString() ?: "Automatic"
    val fuelType = vehicle["fuel_type"]?.toString() ?: "Petrol"
    val location = vehicle["location"]?.toString() ?: "Accra Central"
    val isAvailable = vehicle["is_available"] as? Boolean ?: true
    var isFavorite by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onSelect() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(Color(0xFFF1F5F9)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.DirectionsCar, null, Modifier.size(56.dp), Color.Gray)
                    }
                }
                
                // Top Tag Badge
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
                    color = if (isAvailable) Color(0xFF059669) else Color(0xFFDC2626),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isAvailable) "● INSTANT BOOKING" else "● RESERVED",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                // Heart Favorite Button
                Surface(
                    onClick = { isFavorite = !isFavorite },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                    shape = CircleShape,
                    color = Color.White
                ) {
                    Icon(
                        if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.Gray,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                        Text("$model • $location", color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    
                    Surface(color = Color(0xFFFEF3C7), shape = RoundedCornerShape(10.dp)) {
                        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("★ 4.9", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFD97706))
                        }
                    }
                }
                
                // Spec Tags Chips
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SpecChip(transmission)
                    SpecChip("$seats Seats")
                    SpecChip(fuelType)
                    SpecChip(type)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Price and Book Now Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Rate", fontSize = 11.sp, color = Color.Gray)
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("GH₵ $rate", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                            Text("/ day", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = onSelect,
                        enabled = isAvailable,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(if (isAvailable) "Book Now" else "Unavailable", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SpecChip(text: String) {
    Surface(
        color = Color(0xFFF1F5F9),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
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
    var selectedBudgetPill by remember { mutableStateOf("GH₵ 500 - 1,500") }
    var selectedVehicleTypeCard by remember { mutableStateOf("Sedan") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Row: Title, Subtitle, Reset All Link
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Filter Vehicles", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                Text("Customise your rental experience in Accra", fontSize = 12.sp, color = Color.Gray)
            }

            TextButton(onClick = {
                onPriceChange(5000f)
                onTransChange("All")
                selectedBudgetPill = "GH₵ 500 - 1,500"
                selectedVehicleTypeCard = "Sedan"
            }) {
                Text("Reset All", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        // Section 1: DAILY RATE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("DAILY RATE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Text("Up to GH₵ ${currentMaxPrice.toInt()}/ day", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }

            Slider(
                value = currentMaxPrice,
                onValueChange = onPriceChange,
                valueRange = 200f..10000f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF2563EB),
                    activeTrackColor = Color(0xFF2563EB),
                    inactiveTrackColor = Color(0xFFE2E8F0)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("GH₵ 200", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("Average: GH₵ 1,200", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("GH₵ 10,000+", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }

            // Budget Pill Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Under GH₵ 500", "GH₵ 500 - 1,500", "Luxury (1,500+)").forEach { budgetText ->
                    val isSelected = selectedBudgetPill == budgetText
                    Surface(
                        onClick = {
                            selectedBudgetPill = budgetText
                            when (budgetText) {
                                "Under GH₵ 500" -> onPriceChange(500f)
                                "GH₵ 500 - 1,500" -> onPriceChange(1500f)
                                else -> onPriceChange(10000f)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color.Transparent)
                    ) {
                        Text(
                            text = budgetText,
                            modifier = Modifier.padding(vertical = 10.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF475569)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        // Section 2: TRANSMISSION
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("TRANSMISSION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Text("Any option selected", fontSize = 11.sp, color = Color.Gray)
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF1F5F9),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("All", "Auto", "Manual").forEach { transText ->
                        val isSelected = currentTrans.equals(transText, ignoreCase = true) || (transText == "Auto" && currentTrans.equals("Automatic", ignoreCase = true))
                        Surface(
                            onClick = { onTransChange(if (transText == "Auto") "Automatic" else transText) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) Color.White else Color.Transparent,
                            shadowElevation = if (isSelected) 2.dp else 0.dp
                        ) {
                            Text(
                                text = transText,
                                modifier = Modifier.padding(vertical = 10.dp),
                                textAlign = TextAlign.Center,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF0F172A) else Color.Gray
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

        // Section 3: VEHICLE TYPE
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("VEHICLE TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)

            val vehicleTypeCards = listOf(
                Triple("Sedan", "4 - 5 Seats • Compact", "Sedan"),
                Triple("SUV & 4x4", "Spacious • All-terrain", "SUV"),
                Triple("Executive Luxury", "Mercedes, BMW, etc.", "Luxury"),
                Triple("Minivan / Bus", "7 - 15 Passengers", "Van")
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vehicleTypeCards.chunked(2).forEach { rowCards ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCards.forEach { (title, subtitle, typeKey) ->
                            val isSelected = selectedVehicleTypeCard == typeKey
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedVehicleTypeCard = typeKey },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFEFF6FF) else Color.White),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                        Spacer(Modifier.height(2.dp))
                                        Text(subtitle, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color(0xFF2563EB) else Color.Transparent,
                                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFCBD5E1)) else null,
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        if (isSelected) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Apply Button
        Button(
            onClick = onApply,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
            elevation = ButtonDefaults.buttonElevation(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Apply Filters", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                Text(" • 14 Vehicles Available", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
