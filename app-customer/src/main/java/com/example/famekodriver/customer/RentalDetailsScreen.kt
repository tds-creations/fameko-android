package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.repository.RentalRepository
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalDetailsScreen(
    rental: Map<String, Any>,
    onBack: () -> Unit,
    onNavigateToMainMap: () -> Unit,
    onStartNavigation: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RentalRepository() }
    val scope = rememberCoroutineScope()
    
    val status = rental["status"]?.toString()?.uppercase() ?: "PENDING"
    val isOngoing = status in listOf("PENDING", "BOOKED", "ACTIVE", "IN_PROGRESS")
    val isSelfDrive = rental["is_self_drive"] == true || rental["is_self_drive"] == "true"
    val bookingCode = rental["booking_code"]?.toString() ?: "----"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Booking Details", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (isOngoing) {
                Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 16.dp, color = Color.White) {
                    Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                        Button(
                            onClick = { onStartNavigation(rental) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Navigate on Map", fontWeight = FontWeight.Bold)
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
            // Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        StatusBadge(status)
                        Spacer(Modifier.height(8.dp))
                        Text(if (isSelfDrive) "Self-Drive Rental" else "With Driver Rental", fontWeight = FontWeight.Bold, color = FamekoBlue)
                        
                        Spacer(Modifier.height(16.dp))
                        Text("Booking Code", fontSize = 12.sp, color = Color.Gray)
                        Text(bookingCode, fontSize = 28.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = BoltDark)
                    }
                }
            }

            // Vehicle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(12.dp), color = BoltLightGray, modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = rental["vehicle_image"],
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(rental["vehicle_name"]?.toString() ?: rental["name"]?.toString() ?: "Vehicle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(rental["vehicle_model"]?.toString() ?: rental["model"]?.toString() ?: "", color = Color.Gray, fontSize = 14.sp)
                            Text("GH₵${rental["total_price"] ?: rental["price"] ?: "0"}", color = FamekoBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Route Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailPoint(Icons.Default.MyLocation, BoltGreen, "Pickup", rental["pickup_location"]?.toString() ?: "Current Location")
                        Spacer(Modifier.height(16.dp))
                        DetailPoint(Icons.Default.LocationOn, Color.Red, "Destination", rental["destination_location"]?.toString() ?: "Not set")
                    }
                }
            }

            if (isOngoing) {
                item {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val id = (rental["id"] as? Number)?.toInt() ?: 0
                                repository.cancelRental(id).onSuccess {
                                    Toast.makeText(context, "Cancelled", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel Booking")
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPoint(icon: ImageVector, color: Color, title: String, address: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.size(20.dp).padding(top = 2.dp), color)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(address, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = BoltDark)
        }
    }
}
