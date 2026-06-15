package com.example.famekodriver.customer

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.famekodriver.customer.ui.theme.*
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch
import java.util.*

// We use colors defined in CustomerMapActivity.kt (package-level visibility)
private val AppBlue = Color(0xFF004E89)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDetailsScreen(
    rental: Map<String, Any>,
    onBack: () -> Unit,
    onNavigateToMainMap: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val scope = rememberCoroutineScope()
    
    var currentRental by remember { mutableStateOf(rental) }
    var isEditing by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }

    // Editable fields
    var destinationLocation by remember { mutableStateOf(currentRental["destination_location"]?.toString() ?: "") }
    var stops by remember { mutableStateOf(currentRental["stops"]?.toString()?.split("|")?.filter { it.isNotBlank() } ?: emptyList()) }

    val status = currentRental["status"]?.toString()?.uppercase() ?: "PENDING"
    val isOngoing = status in listOf("PENDING", "BOOKED", "ASSIGNED", "ACTIVE", "IN_PROGRESS")
    val isSelfDrive = currentRental["is_self_drive"] == true || currentRental["is_self_drive"] == "true"
    val isUnlocked = currentRental["is_unlocked"] == true || currentRental["is_unlocked"] == "true"
    val bookingCode = currentRental["booking_code"]?.toString() ?: "----"
    val totalPrice = currentRental["total_price"]?.toString()?.toDoubleOrNull() ?: 0.0
    val dateStr = currentRental["created_at"]?.toString() ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rental Details", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isOngoing && !isEditing) {
                        TextButton(onClick = { isEditing = true }) {
                            Text("Edit", color = BoltGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (isEditing) {
                Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = Color.White) {
                    Row(modifier = Modifier.padding(20.dp).navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = { isEditing = false },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                isUpdating = true
                                scope.launch {
                                    val id = (currentRental["id"] as? Double)?.toInt() ?: (currentRental["id"] as? Int) ?: 0
                                    val stopsStr = if (stops.isNotEmpty()) stops.filter { it.isNotBlank() }.joinToString("|") else null
                                    
                                    val lat = currentRental["destination_lat"] as? Double ?: 0.0
                                    val lng = currentRental["destination_lng"] as? Double ?: 0.0

                                    repository.updateRentalDestination(id, destinationLocation, lat, lng, stopsStr)
                                        .onSuccess {
                                            Toast.makeText(context, "Rental updated successfully", Toast.LENGTH_SHORT).show()
                                            isEditing = false
                                            isUpdating = false
                                            currentRental = currentRental.toMutableMap().apply {
                                                put("destination_location", destinationLocation)
                                                put("stops", stopsStr ?: "")
                                            }
                                        }
                                        .onFailure {
                                            Toast.makeText(context, "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                            isUpdating = false
                                        }
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BoltGreen),
                            enabled = !isUpdating
                        ) {
                            if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            else Text("Save Changes", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (isOngoing) {
                Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = Color.White) {
                    Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                        Button(
                            onClick = {
                                if (status == "BOOKED") {
                                    scope.launch {
                                        val id = (currentRental["id"] as? Double)?.toInt() ?: (currentRental["id"] as? Int) ?: 0
                                        repository.updateRentalStatus(id, "ACTIVE")
                                    }
                                }

                                val lat = currentRental["destination_lat"] as? Double
                                val lng = currentRental["destination_lng"] as? Double
                                val pLat = currentRental["pickup_lat"] as? Double
                                val pLng = currentRental["pickup_lng"] as? Double
                                
                                // Target logic: 
                                // If not unlocked -> go to pickup location
                                // If unlocked -> go to destination
                                val targetLat = if (isUnlocked) lat else pLat
                                val targetLng = if (isUnlocked) lng else pLng
                                val targetAddress = if (isUnlocked) currentRental["destination_location"] else currentRental["pickup_location"]

                                if (targetLat != null && targetLng != null && targetLat != 0.0) {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=$targetLat,$targetLng")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                                    }
                                } else if (!targetAddress?.toString().isNullOrBlank() && targetAddress != "Not set") {
                                    val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(targetAddress.toString())}")
                                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                    mapIntent.setPackage("com.google.android.apps.maps")
                                    try {
                                        context.startActivity(mapIntent)
                                    } catch (e: Exception) {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                                    }
                                } else {
                                    onNavigateToMainMap()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSelfDrive) BoltGreen else AppBlue)
                        ) {
                            Icon(if (isSelfDrive) Icons.Default.Navigation else Icons.Default.Map, null)
                            Spacer(Modifier.width(12.dp))
                            Text(if (isSelfDrive) "Start Navigation" else "View on Google Maps", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusBadgeLarge(status)
                        Spacer(Modifier.height(8.dp))
                        Text("Rental Mode: ${if (isSelfDrive) "Self-Drive" else "With Driver"}", fontWeight = FontWeight.Bold, color = AppBlue, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Booking Code", fontSize = 12.sp, color = Color.Gray)
                        Text(bookingCode, fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = BoltDark)
                        Text(dateStr, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }

            // Vehicle Info
            item {
                Text("Vehicle Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = BoltLightGray, modifier = Modifier.size(64.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsCar, null, tint = AppBlue, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(currentRental["vehicle_name"].toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("₵${String.format(Locale.US, "%.2f", totalPrice)} Total", color = BoltGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Trip Details (Editable)
            item {
                Text("Trip Route", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        RoutePoint(
                            icon = Icons.Default.RadioButtonChecked,
                            color = BoltGreen,
                            title = if (isSelfDrive) "Pick up vehicle from" else "Pickup Location",
                            address = currentRental["pickup_location"].toString()
                        )
                        
                        VerticalLineLong()

                        if (isEditing) {
                            Column {
                                stops.forEachIndexed { index, stop ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Flag, null, Modifier.size(16.dp), Color(0xFFF08C00))
                                        Spacer(Modifier.width(12.dp))
                                        OutlinedTextField(
                                            value = stop,
                                            onValueChange = { newValue -> stops = stops.toMutableList().apply { set(index, newValue) } },
                                            modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                            placeholder = { Text("Stop address") },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        IconButton(onClick = { stops = stops.toMutableList().apply { removeAt(index) } }) {
                                            Icon(Icons.Default.Close, null, tint = Color.Gray)
                                        }
                                    }
                                    VerticalLineLong()
                                }
                                if (stops.size < 5) {
                                    TextButton(onClick = { stops = stops + "" }) {
                                        Icon(Icons.Default.Add, null, Modifier.size(16.dp), BoltGreen)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Add Stop", color = BoltGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                    VerticalLineLong()
                                }
                                
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), Color.Red)
                                    Spacer(Modifier.width(12.dp))
                                    OutlinedTextField(
                                        value = destinationLocation,
                                        onValueChange = { destinationLocation = it },
                                        modifier = Modifier.weight(1f),
                                        placeholder = { Text("Final destination") },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        } else {
                            stops.forEach { stop ->
                                RoutePoint(
                                    icon = Icons.Default.Flag,
                                    color = Color(0xFFF08C00),
                                    title = "Stop",
                                    address = stop
                                )
                                VerticalLineLong()
                            }
                            
                            RoutePoint(
                                icon = Icons.Default.LocationOn,
                                color = Color.Red,
                                title = "Destination",
                                address = if (destinationLocation.isNotBlank()) destinationLocation else "Not set"
                            )
                        }
                    }
                }
            }

            // Self-Drive Instructions
            if (isSelfDrive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9DB)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Self-Drive Checklist", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE67700))
                            Spacer(Modifier.height(8.dp))
                            SelfDriveInstructionRow("1. Bring your physical Driver's License")
                            SelfDriveInstructionRow("2. Inspect vehicle and take 4 photos (all sides)")
                            SelfDriveInstructionRow("3. Check fuel level and mileage before starting")
                            SelfDriveInstructionRow("4. The key is in the secure lockbox")
                        }
                    }
                }
            }

            // Instructions
            item {
                Text("Instructions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    val notes = currentRental["trip_notes"]?.toString()
                    Text(
                        text = if (notes.isNullOrBlank()) "No additional instructions provided." else notes,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 14.sp,
                        color = if (notes.isNullOrBlank()) Color.Gray else BoltDark
                    )
                }
            }

            // Danger Zone
            if (isOngoing && !isEditing) {
                item {
                    Spacer(Modifier.height(16.dp))
                    
                    if (status == "ACTIVE" || status == "IN_PROGRESS") {
                        Button(
                            onClick = {
                                scope.launch {
                                    val id = (currentRental["id"] as? Double)?.toInt() ?: (currentRental["id"] as? Int) ?: 0
                                    repository.endRental(id).onSuccess {
                                        Toast.makeText(context, "Rental ended successfully", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }.onFailure {
                                        Toast.makeText(context, "Failed to end rental: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("End Rental Now", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val id = (currentRental["id"] as? Double)?.toInt() ?: (currentRental["id"] as? Int) ?: 0
                                    repository.cancelRental(id).onSuccess {
                                        Toast.makeText(context, "Rental cancelled", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }.onFailure {
                                        Toast.makeText(context, "Failed to cancel: ${it.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel Booking")
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun SelfDriveInstructionRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(16.dp), Color(0xFFF08C00))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = Color(0xFFE67700))
    }
}

@Composable
fun StatusBadgeLarge(status: String) {
    val color = when (status.uppercase()) {
        "PENDING" -> Color(0xFFF08C00)
        "BOOKED" -> BoltGreen
        "ASSIGNED", "ACTIVE", "IN_PROGRESS", "COMPLETED" -> BoltGreen
        else -> Color(0xFFC92A2A)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            color = color,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp
        )
    }
}

@Composable
fun RoutePoint(icon: ImageVector, color: Color, title: String, address: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(16.dp).padding(top = 2.dp), color)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(address, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = BoltDark)
        }
    }
}

@Composable
fun VerticalLineLong() {
    Box(
        modifier = Modifier
            .padding(start = 7.dp, top = 4.dp, bottom = 4.dp)
            .height(24.dp)
            .width(2.dp)
            .background(BoltLightGray)
    )
}
