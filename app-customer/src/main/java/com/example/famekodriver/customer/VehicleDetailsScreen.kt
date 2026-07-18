package com.example.famekodriver.customer

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.PricingConfig
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltGreen
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VehicleDetailsScreen(
    vehicle: Map<String, Any>,
    onBack: () -> Unit,
    onBookNow: (Map<String, Any>) -> Unit,
) {
    val repository = remember { DriverRepository.getInstance() }
    var pricingConfig by remember { mutableStateOf<PricingConfig?>(null) }
    
    LaunchedEffect(Unit) {
        repository.getPricingConfig().onSuccess { pricingConfig = it }
    }

    val name = vehicle["name"]?.toString() ?: "Vehicle"
    val model = vehicle["model"]?.toString() ?: ""
    val type = vehicle["vehicle_type"]?.toString() ?: "Car"
    val dailyRate = (vehicle["daily_rate"]?.toString()?.toDoubleOrNull() ?: 0.0)
    val description = vehicle["description"]?.toString() ?: "Explore the road with this comfortable and reliable vehicle. Perfect for business trips or family getaways."
    val features = vehicle["features"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val imageUrls = vehicle["image_urls"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    
    val seats = (vehicle["seats"]?.toString()?.toDoubleOrNull()?.toInt() ?: 5)
    val transmission = vehicle["transmission"]?.toString() ?: "Automatic"
    val fuelType = vehicle["fuel_type"]?.toString() ?: "Petrol"
    val ownerName = vehicle["owner_name"]?.toString() ?: "Verified Host"

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "₵${dailyRate.toInt()}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = BoltDark
                            )
                            Text(" / day", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        }
                        Text("Total shown at checkout", style = MaterialTheme.typography.bodySmall, color = BoltGreen, fontWeight = FontWeight.Bold)
                    }
                    val isAvailable = vehicle["status"]?.toString()?.uppercase() == "AVAILABLE"
                    
                    Button(
                        onClick = { if (isAvailable) onBookNow(vehicle) },
                        modifier = Modifier
                            .height(56.dp)
                            .width(180.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAvailable) BoltGreen else Color.Gray
                        ),
                        enabled = isAvailable
                    ) {
                        Text(
                            if (isAvailable) "Reserve Vehicle" else "Unavailable", 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.White)
        ) {
            // Premium Image Gallery with Gradient Overlay
            item {
                Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
                    if (imageUrls.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            items(imageUrls) { url ->
                                AsyncImage(
                                    model = url,
                                    contentDescription = name,
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .fillMaxHeight(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        
                        // Image Count Indicator
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                "1 / ${imageUrls.size}",
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Box(
                            Modifier.fillMaxSize().background(BoltLightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, null, Modifier.size(80.dp), Color.Gray)
                        }
                    }

                    // Floating Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopStart)
                            .clip(CircleShape)
                            .background(Color.White)
                            .size(40.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(20.dp))
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(24.dp)) {
                    // Header Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = BoltDark,
                                lineHeight = 32.sp
                            )
                            Text(
                                "$model • $type",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Gray
                            )
                        }
                        
                        // Rating Card
                        Surface(
                            color = Color(0xFFFFF9DB),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, null, tint = Color(0xFFF08C00), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("4.9", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE67700))
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    HorizontalDivider(color = BoltLightGray)
                    Spacer(Modifier.height(24.dp))

                    // Key Specs Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecChip(Icons.Default.People, "$seats Seats")
                        SpecChip(Icons.Default.Settings, transmission)
                        SpecChip(Icons.Default.LocalGasStation, fuelType)
                    }

                    Spacer(Modifier.height(32.dp))

                    // Host Info Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = BoltLightGray,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(shape = CircleShape, modifier = Modifier.size(48.dp), color = Color.White) {
                                Icon(Icons.Default.Person, null, modifier = Modifier.padding(10.dp), tint = BoltGreen)
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text("Hosted by $ownerName", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("Verified Host • 50+ Trips", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Description
                    Text(
                        "Vehicle Description",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = BoltDark
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.DarkGray,
                        lineHeight = 26.sp
                    )

                    Spacer(Modifier.height(32.dp))

                    // Features Flow
                    if (features.isNotEmpty()) {
                        Text(
                            "What this vehicle offers",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = BoltDark
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            features.forEach { feature ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Check, null, Modifier.size(20.dp), BoltGreen)
                                    Spacer(Modifier.width(12.dp))
                                    Text(feature.trim(), fontSize = 15.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Terms of Service
                    Text(
                        "Rental Terms",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = BoltDark
                    )
                    Spacer(Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BoltLightGray.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BoltLightGray)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, null, Modifier.size(20.dp), BoltGreen)
                                Spacer(Modifier.width(12.dp))
                                Text("Insurance included in every booking", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SupportAgent, null, Modifier.size(20.dp), BoltGreen)
                                Spacer(Modifier.width(12.dp))
                                Text("24/7 Roadside assistance", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, null, Modifier.size(20.dp), BoltGreen)
                                Spacer(Modifier.width(12.dp))
                                Text("Flat booking commission (no daily fees)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
fun SpecChip(icon: ImageVector, text: String) {
    Column(
        modifier = Modifier
            .width(90.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BoltLightGray)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = BoltDark)
        Spacer(Modifier.height(8.dp))
        Text(text, fontSize = 12.sp, color = BoltDark, fontWeight = FontWeight.Bold)
    }
}
