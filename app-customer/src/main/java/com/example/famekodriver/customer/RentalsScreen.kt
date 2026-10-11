package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.RentalRepository
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsScreen(
    onBack: () -> Unit,
    onNavigateToDetails: (Map<String, Any>) -> Unit,
    onRebook: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RentalRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var rentals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("Active") }

    fun loadRentals() {
        isLoading = true
        scope.launch {
            val customerId = sessionManager.getCustomerId() ?: ""
            repository.getCustomerRentals(customerId).onSuccess {
                rentals = it
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadRentals() }

    val filteredRentals = remember(rentals, selectedFilter) {
        rentals.filter { r ->
            val status = (r["status"]?.toString() ?: "").uppercase()
            when (selectedFilter) {
                "Active" -> status == "ACTIVE" || status == "PENDING" || status == "UNLOCKED"
                "Completed" -> status == "COMPLETED" || status == "FINISHED" || status == "ENDED"
                "Cancelled" -> status == "CANCELLED" || status == "REJECTED"
                else -> true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
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
                            Text("My Rentals", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { Toast.makeText(context, "Rental Support live 24/7", Toast.LENGTH_SHORT).show() }) {
                                Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Support", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Filter Tabs Pill Container
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val filters = listOf("Active", "Completed", "Cancelled")
                            filters.forEach { filterText ->
                                val isSelected = selectedFilter == filterText
                                val count = rentals.count { r ->
                                    val st = (r["status"]?.toString() ?: "").uppercase()
                                    when (filterText) {
                                        "Active" -> st == "ACTIVE" || st == "PENDING" || st == "UNLOCKED"
                                        "Completed" -> st == "COMPLETED" || st == "FINISHED" || st == "ENDED"
                                        "Cancelled" -> st == "CANCELLED" || st == "REJECTED"
                                        else -> true
                                    }
                                }
                                Surface(
                                    onClick = { selectedFilter = filterText },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shadowElevation = if (isSelected) 2.dp else 0.dp
                                ) {
                                    Text(
                                        text = if (count > 0) "$filterText ($count)" else filterText,
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color(0xFF2563EB) else Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading State
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2563EB))
                        }
                    }
                }

                // Empty State if no rentals match filter
                if (!isLoading && filteredRentals.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFEFF6FF),
                                    modifier = Modifier.size(56.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(28.dp))
                                    }
                                }

                                Text("No $selectedFilter Rentals", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                                Text("You currently have no $selectedFilter.lowercase() rental vehicle bookings.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)

                                Button(
                                    onClick = onBack,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                ) {
                                    Text("Browse Available Vehicles", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Dynamic Live Rental Cards
                if (!isLoading && filteredRentals.isNotEmpty()) {
                    items(filteredRentals) { rental ->
                        val vehicleName = rental["vehicle_name"]?.toString() ?: rental["name"]?.toString() ?: "Rental Vehicle"
                        val vehicleType = rental["vehicle_type"]?.toString() ?: "Car"
                        val vehicleNumber = rental["vehicle_number"]?.toString() ?: ""
                        val pickupLocation = rental["pickup_location"]?.toString() ?: "Accra Central Hub"
                        val destinationLocation = rental["destination_location"]?.toString()
                        val bookingCode = rental["booking_code"]?.toString() ?: "FAM-${rental["id"]}"
                        val status = rental["status"]?.toString() ?: "PENDING"
                        val totalPrice = (rental["total_price"] as? Number)?.toDouble() ?: 0.0
                        val imageUrls = rental["image_urls"]?.toString()?.split(",")?.firstOrNull { it.isNotBlank() }
                        val isUnlocked = rental["is_unlocked"] == true

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetails(rental) },
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(2.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Header: Status Badge & Booking Code
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (status == "ACTIVE" || isUnlocked) Color(0xFFECFDF5) else Color(0xFFEFF6FF),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(modifier = Modifier.size(6.dp).background(if (status == "ACTIVE" || isUnlocked) Color(0xFF10B981) else Color(0xFF2563EB), CircleShape))
                                            Text(if (isUnlocked) "UNLOCKED & ONGOING" else status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (status == "ACTIVE" || isUnlocked) Color(0xFF059669) else Color(0xFF2563EB))
                                        }
                                    }

                                    Text("#$bookingCode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                }

                                // Vehicle Details Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(vehicleName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF0F172A))
                                        Spacer(Modifier.height(2.dp))
                                        Text("$vehicleType • Greater Accra", fontSize = 12.sp, color = Color.Gray)
                                        if (vehicleNumber.isNotBlank()) {
                                            Spacer(Modifier.height(4.dp))
                                            Surface(
                                                color = Color(0xFFF1F5F9),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text("Plate: $vehicleNumber", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!imageUrls.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = imageUrls,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(Icons.Default.DirectionsCar, null, tint = Color(0xFF2563EB), modifier = Modifier.size(28.dp))
                                        }
                                    }
                                }

                                // Pickup Location Row
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981), CircleShape))
                                    Text("Pick-up Location:", fontSize = 11.sp, color = Color.Gray)
                                    Text(pickupLocation, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                if (!destinationLocation.isNullOrBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFDC2626), RoundedCornerShape(2.dp)))
                                        Text("Destination:", fontSize = 11.sp, color = Color.Gray)
                                        Text(destinationLocation, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                                // Total Price & Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Total Paid", fontSize = 11.sp, color = Color.Gray)
                                        Text("GH₵ ${String.format(Locale.getDefault(), "%.2f", totalPrice)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                                    }

                                    Button(
                                        onClick = { onNavigateToDetails(rental) },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                                    ) {
                                        Text("View Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Rental Assurance Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.12f),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Security, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("RENTAL ASSURANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("Full insurance coverage and 24/7 roadside assistance included on all Fameko rental bookings.", fontSize = 11.sp, color = Color.LightGray, lineHeight = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
