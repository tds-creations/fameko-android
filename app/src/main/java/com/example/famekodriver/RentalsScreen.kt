package com.example.famekodriver

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DriverRepository.getInstance() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var rentals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    var showVerifyDialog by remember { mutableStateOf<Int?>(null) } // rentalId
    var verificationCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var isEndingTrip by remember { mutableStateOf<Int?>(null) } // rentalId

    fun fetchRentals() {
        isLoading = true
        val driverId = sessionManager.getDriverId() ?: ""
        scope.launch {
            repository.getDriverRentals(driverId).onSuccess { list ->
                rentals = list
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchRentals()
    }

    LaunchedEffect(Unit) {
        repository.events.collect { event ->
            if (event is com.example.famekodriver.core.domain.model.FamekoEvent.RentalDestinationUpdated) {
                fetchRentals()
            }
        }
    }

    if (showVerifyDialog != null) {
        AlertDialog(
            onDismissRequest = { if (!isVerifying) showVerifyDialog = null },
            title = { Text("Verify Rental Start") },
            text = {
                Column {
                    Text("Enter the Booking Code provided by the customer to unlock the trip details.")
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = verificationCode,
                        onValueChange = { verificationCode = it.uppercase() },
                        label = { Text("Booking Code (FAM-XXXX)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isVerifying = true
                        scope.launch {
                            repository.verifyRentalCode(showVerifyDialog!!, verificationCode).onSuccess { success ->
                                if (success) {
                                    Toast.makeText(context, "Handshake Successful! Trip Unlocked.", Toast.LENGTH_LONG).show()
                                    showVerifyDialog = null
                                    verificationCode = ""
                                    fetchRentals()
                                } else {
                                    Toast.makeText(context, "Invalid Code. Please try again.", Toast.LENGTH_SHORT).show()
                                }
                                isVerifying = false
                            }.onFailure {
                                Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                isVerifying = false
                            }
                        }
                    },
                    enabled = verificationCode.length >= 4 && !isVerifying
                ) {
                    if (isVerifying) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                    else Text("Verify & Start")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVerifyDialog = null }, enabled = !isVerifying) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rental Jobs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF004E89))
            }
        } else if (rentals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Key, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No assigned rentals yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(rentals) { rental ->
                    val isUnlocked = rental["is_unlocked"] == true
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF0F2F5),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    val pic = rental["customer_profile_pic"]?.toString()
                                    if (!pic.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = pic,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = Color.Gray)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = rental["customer_name"].toString(),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = rental["customer_phone"].toString(),
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Surface(
                                    color = if (isUnlocked) FamekoLightBlue else Color(0xFFFFF9DB),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (isUnlocked) "UNLOCKED" else "LOCKED",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) FamekoBlue else Color(0xFFF08C00)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            
                            DetailRow(Icons.Default.DirectionsCar, "Vehicle", rental["vehicle_name"].toString())
                            DetailRow(Icons.Default.LocationOn, "Pickup", rental["pickup_location"].toString())

                            if (isUnlocked) {
                                val dest = rental["destination_location"]?.toString()
                                val stops = rental["stops"]?.toString() ?: ""
                                
                                Column {
                                    if (!dest.isNullOrBlank()) {
                                        DetailRow(Icons.Default.Flag, "Destination", dest)
                                    } else {
                                        DetailRow(Icons.Default.Schedule, "Status", "Waiting for customer to set destination")
                                    }
                                    
                                    if (stops.isNotEmpty()) {
                                        stops.split("|").forEachIndexed { index, stop ->
                                            DetailRow(Icons.Default.StopCircle, "Stop ${index + 1}", stop)
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (!isUnlocked) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            val lat = rental["pickup_lat"]
                                            val lng = rental["pickup_lng"]
                                            if (lat != null && lng != null) {
                                                val gmmIntentUri = Uri.parse("google.navigation:q=$lat,$lng")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                try {
                                                    context.startActivity(mapIntent)
                                                } catch (e: java.lang.Exception) {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C757D)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Navigation, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Navigate to Pickup", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showVerifyDialog = (rental["id"] as? Double)?.toInt() ?: 0 },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.LockOpen, null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Enter Code", fontSize = 12.sp)
                                    }
                                }
                            } else {
                                val destLat = rental["destination_lat"] as? Double
                                val destLng = rental["destination_lng"] as? Double
                                val hasDest = destLat != null && destLng != null && destLat != 0.0
                                val rentalId = (rental["id"] as? Double)?.toInt() ?: 0

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = {
                                            if (hasDest) {
                                                val gmmIntentUri = Uri.parse("google.navigation:q=$destLat,$destLng")
                                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                                mapIntent.setPackage("com.google.android.apps.maps")
                                                try {
                                                    context.startActivity(mapIntent)
                                                } catch (e: java.lang.Exception) {
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, gmmIntentUri))
                                                }
                                            } else {
                                                Toast.makeText(context, "Customer hasn't set a destination yet", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (hasDest) FamekoPrimary else Color.LightGray
                                        ),
                                        enabled = hasDest,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.Directions, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (hasDest) "Navigate to Destination" else "Awaiting Destination")
                                    }

                                    Button(
                                        onClick = {
                                            isEndingTrip = rentalId
                                            scope.launch {
                                                repository.endRental(rentalId).onSuccess { success ->
                                                    if (success) {
                                                        Toast.makeText(context, "Trip Completed Successfully", Toast.LENGTH_LONG).show()
                                                        fetchRentals()
                                                    } else {
                                                        Toast.makeText(context, "Failed to end trip", Toast.LENGTH_SHORT).show()
                                                    }
                                                    isEndingTrip = null
                                                }.onFailure {
                                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                                    isEndingTrip = null
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC3545)),
                                        shape = RoundedCornerShape(8.dp),
                                        enabled = isEndingTrip == null
                                    ) {
                                        if (isEndingTrip == rentalId) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.DoneAll, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("End Trip")
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
}

@Composable
fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "$label: ", fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}
