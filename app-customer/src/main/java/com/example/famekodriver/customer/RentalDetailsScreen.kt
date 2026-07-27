package com.example.famekodriver.customer

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
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
    val isOngoing = status in listOf("PENDING", "BOOKED", "ACTIVE", "IN_PROGRESS", "ASSIGNED")
    val isSelfDrive = rental["is_self_drive"] == true || rental["is_self_drive"] == "true"
    val bookingCode = rental["booking_code"]?.toString() ?: "----"
    val vehicleName = rental["vehicle_name"]?.toString() ?: "Rental Vehicle"
    val vehicleModel = rental["vehicle_model"]?.toString() ?: ""
    val totalPrice = rental["total_price"]?.toString() ?: "0.00"
    val duration = rental["duration_hours"]?.toString() ?: "24"
    val notes = rental["trip_notes"]?.toString() ?: ""
    
    val driverName = rental["driver_name"]?.toString()
    val driverPhone = rental["driver_phone"]?.toString()
    val driverPic = rental["driver_profile_pic"]?.toString()
    val driverPlate = rental["driver_plate"]?.toString()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rental Details", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif) },
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
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 16.dp,
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier
                            .padding(20.dp)
                            .navigationBarsPadding(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, "tel:0541234567".toUri())
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(0.4f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Icon(Icons.Default.SupportAgent, null, tint = BoltDark)
                        }

                        Button(
                            onClick = { onStartNavigation(rental) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
                        ) {
                            Icon(Icons.Default.Map, null)
                            Spacer(Modifier.width(12.dp))
                            Text("Map View", fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Image & Status
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(BoltLightGray)
                ) {
                    AsyncImage(
                        model = rental["vehicle_image"],
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    
                    Surface(
                        modifier = Modifier
                            .padding(16.dp)
                            .align(Alignment.TopEnd),
                        color = Color.White.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isOngoing) BoltGreen else Color.Gray))
                            Spacer(Modifier.width(8.dp))
                            Text(status, fontWeight = FontWeight.Black, fontSize = 11.sp, color = BoltDark, fontFamily = FontFamily.SansSerif)
                        }
                    }
                }
            }

            // Code Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isSelfDrive) "VEHICLE ACCESS CODE" else "HANDSHAKE CODE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            fontFamily = FontFamily.SansSerif,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = bookingCode,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 4.sp,
                            color = BoltDark,
                            fontFamily = FontFamily.SansSerif
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (isSelfDrive) "Use this to unlock the vehicle or keybox." else "Provide this code to your driver when you meet.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            fontFamily = FontFamily.SansSerif
                        )
                    }
                }
            }

            // Driver / Owner Card (If not self-drive)
            if (!isSelfDrive && !driverName.isNullOrEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = BoltLightGray, modifier = Modifier.size(56.dp)) {
                                if (!driverPic.isNullOrEmpty()) {
                                    AsyncImage(model = driverPic, contentDescription = null, contentScale = ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.Person, null, Modifier.padding(12.dp), Color.Gray)
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Your Driver", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(driverName, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = BoltDark)
                                Text(driverPlate ?: "VERIFIED PARTNER", fontSize = 13.sp, color = FamekoBlue, fontWeight = FontWeight.Bold)
                            }
                            
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, "tel:$driverPhone".toUri())
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.background(FamekoBlue.copy(alpha = 0.1f), CircleShape)
                            ) {
                                Icon(Icons.Default.Call, null, tint = FamekoBlue)
                            }
                        }
                    }
                }
            }

            // Trip Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Trip Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BoltDark, fontFamily = FontFamily.SansSerif)
                        
                        DetailRowFixed(Icons.Default.DirectionsCar, "Vehicle", "$vehicleName ($vehicleModel)")
                        DetailRowFixed(Icons.Default.Timer, "Duration", "$duration Hours")
                        DetailRowFixed(Icons.Default.Payments, "Total Price", "GH₵$totalPrice")
                        
                        HorizontalDivider(color = BoltLightGray, thickness = 0.5.dp)
                        
                        DetailRowFixed(Icons.Default.MyLocation, "Pickup", rental["pickup_location"]?.toString() ?: "Point A")
                        DetailRowFixed(Icons.Default.LocationOn, "Destination", rental["destination_location"]?.toString() ?: "Waiting for you to set...")
                        
                        if (notes.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Surface(color = BoltLightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                                Text(
                                    text = "Notes: $notes",
                                    modifier = Modifier.padding(12.dp),
                                    fontSize = 13.sp,
                                    color = Color.DarkGray,
                                    fontFamily = FontFamily.SansSerif
                                )
                            }
                        }
                    }
                }
            }

            if (isOngoing && status == "PENDING") {
                item {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val id = (rental["id"] as? Number)?.toInt() ?: 0
                                repository.cancelRental(id).onSuccess {
                                    Toast.makeText(context, "Booking Cancelled", Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel Booking", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun DetailRowFixed(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), Color.Gray)
        Spacer(Modifier.width(12.dp))
        Text("$label:", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(90.dp), fontFamily = FontFamily.SansSerif)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BoltDark, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = FontFamily.SansSerif)
    }
}
