package com.example.famekodriver

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetManagementScreen(
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAddVehicle: () -> Unit = {},
    onEditVehicle: (Map<String, Any>) -> Unit = {},
    viewModel: FleetManagementViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadVehicles()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Fleet Management", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(viewModel.companyName, fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateToMenu) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.DirectionsCar, contentDescription = "Profile")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddVehicle,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Vehicle") },
                containerColor = Color(0xFF004E89),
                contentColor = Color.White
            )
        }
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF004E89))
                }
            }

            viewModel.vehicles.isEmpty() -> {
                EmptyFleetView(padding)
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFF8F9FA)),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(viewModel.vehicles) { vehicle ->
                        VehicleCard(
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
fun EmptyFleetView(padding: PaddingValues) {
    Box(
        Modifier.fillMaxSize().padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.DirectionsCar, null, Modifier.size(64.dp), Color.LightGray)
            Spacer(Modifier.height(16.dp))
            Text("No vehicles in your fleet yet", color = Color.Gray)
            Text("Add your first vehicle to start renting", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun VehicleCard(
    vehicle: Map<String, Any>,
    onEdit: () -> Unit
) {
    val statusLabel = resolveStatusLabel(vehicle)
    val statusColor = resolveStatusColor(vehicle)

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(vehicle["name"].toString(), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        "${vehicle["model"]} • ${vehicle["vehicle_number"]}",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }

                Surface(
                    color = statusColor,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
                Text("₵${vehicle["daily_rate"]}", fontWeight = FontWeight.Bold, color = Color(0xFF004E89))
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
                Spacer(Modifier.height(12.dp))
                Text(description, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
            }

            val features = vehicle["features"]?.toString()
            if (!features.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    features.split(",").take(4).forEach { feature ->
                        Surface(
                            color = Color.LightGray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                feature.trim(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            val imageUrls = vehicle["image_urls"]?.toString()
                ?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
            if (imageUrls.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(imageUrls) { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(width = 120.dp, height = 80.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onEdit,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF004E89))
                ) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Details")
                }
            }
        }
    }
}

private fun resolveVehicleStatus(vehicle: Map<String, Any>): String =
    vehicle["status"]?.toString()?.uppercase()?.trim() ?: "UNKNOWN"

private fun resolveStatusLabel(vehicle: Map<String, Any>): String =
    when (resolveVehicleStatus(vehicle)) {
        "AVAILABLE"              -> "AVAILABLE"
        "BOOKED"                 -> "BOOKED"
        "OCCUPIED", "IN_USE"     -> "ON TRIP"
        "MAINTENANCE"            -> "MAINTENANCE"
        else                     -> resolveVehicleStatus(vehicle)
    }

private fun resolveStatusColor(vehicle: Map<String, Any>): Color =
    when (resolveVehicleStatus(vehicle)) {
        "AVAILABLE"              -> Color(0xFF28A745)
        "BOOKED"                 -> Color(0xFFF08C00)
        "OCCUPIED", "IN_USE"     -> Color(0xFF004E89)
        "MAINTENANCE"            -> Color(0xFF868E96)
        else                     -> Color.Red
    }
