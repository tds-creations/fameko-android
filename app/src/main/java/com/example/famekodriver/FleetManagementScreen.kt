package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetManagementScreen(
    viewModel: FleetManagementViewModel = viewModel(),
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddVehicle: () -> Unit,
    onEditVehicle: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadVehicles()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Fleet Management", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text(viewModel.companyName, fontSize = 14.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.Person, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddVehicle,
                containerColor = Color(0xFF004E89),
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Vehicle", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC))
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF004E89)
                )
            } else if (viewModel.vehicles.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.DirectionsCar,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = Color.LightGray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No vehicles in your fleet yet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add your first vehicle to start renting",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(viewModel.vehicles) { vehicle ->
                        FleetVehicleCard(
                            vehicle = vehicle,
                            onEdit = { onEditVehicle(vehicle) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FleetVehicleCard(
    vehicle: Map<String, Any>,
    onEdit: () -> Unit
) {
    val status = vehicle["status"]?.toString() ?: "AVAILABLE"
    val isAvailable = vehicle["is_available"] as? Boolean ?: true

    val statusColor = when {
        status == "AVAILABLE" && isAvailable -> Color(0xFF2B8A3E)
        status == "RENTED" -> Color(0xFF004E89)
        else -> Color(0xFFD9480F)
    }

    val statusLabel = when {
        status == "AVAILABLE" && isAvailable -> "AVAILABLE"
        status == "RENTED" -> "RENTED"
        else -> "MAINTENANCE"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Name & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = vehicle["name"]?.toString() ?: "Vehicle",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "${vehicle["model"] ?: ""} • ${vehicle["vehicle_number"] ?: ""}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Payments, null, modifier = Modifier.size(16.dp), tint = Color(0xFF004E89))
                Spacer(Modifier.width(4.dp))
                Text("₵${vehicle["daily_rate"]}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF004E89))
                Text(" / day", fontSize = 12.sp, color = Color.Gray)

                Spacer(Modifier.width(16.dp))

                Icon(Icons.Default.DirectionsCar, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                Spacer(Modifier.width(4.dp))
                Text(
                    "${vehicle["vehicle_type"]} • ${vehicle["transmission"] ?: "Auto"}",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }

            val description = vehicle["description"]?.toString()
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(description, fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp)
            }

            val features = vehicle["features"]?.toString()
            if (!features.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    features.split(",").take(3).forEach { feature ->
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                feature.trim(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }
                }
            }

            // Image Display Row (Uploaded Cloudinary photos or Type-based fallback photo)
            val rawImageUrls = vehicle["image_urls"]?.toString()
                ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            val displayImages = if (rawImageUrls.isNotEmpty()) rawImageUrls else {
                val vType = vehicle["vehicle_type"]?.toString() ?: "Sedan"
                listOf(
                    when {
                        vType.contains("SUV", ignoreCase = true) -> "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=800"
                        vType.contains("Luxury", ignoreCase = true) -> "https://images.unsplash.com/photo-1618843479313-40f8afb4b4d8?w=800"
                        vType.contains("Van", ignoreCase = true) -> "https://images.unsplash.com/photo-1570125909232-eb263c188f7e?w=800"
                        else -> "https://images.unsplash.com/photo-1621007947382-bb3c3994e3fb?w=800"
                    }
                )
            }

            Spacer(Modifier.height(14.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayImages) { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit Details", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
