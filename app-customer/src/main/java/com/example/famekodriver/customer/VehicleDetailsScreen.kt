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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.customer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailsScreen(
    vehicle: Map<String, Any>,
    onBack: () -> Unit,
    onBookNow: (Map<String, Any>) -> Unit,
) {
    val name = vehicle["name"]?.toString() ?: "Vehicle"
    val model = vehicle["model"]?.toString() ?: ""
    val type = vehicle["vehicle_type"]?.toString() ?: "Car"
    val dailyRate = vehicle["daily_rate"]?.toString() ?: "0"
    val description = vehicle["description"]?.toString() ?: "No description provided."
    val features = vehicle["features"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    val imageUrls = vehicle["image_urls"]?.toString()?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

    Scaffold(
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(20.dp).navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("₵$dailyRate", fontWeight = FontWeight.Black, fontSize = 24.sp, color = BoltDark)
                        Text("per day", fontSize = 12.sp, color = Color.Gray)
                    }
                    Button(
                        onClick = { onBookNow(vehicle) },
                        modifier = Modifier.height(56.dp).width(160.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
                    ) {
                        Text("Book Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.White)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    if (imageUrls.isNotEmpty()) {
                        AsyncImage(
                            model = imageUrls.first(),
                            contentDescription = name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(Modifier.fillMaxSize().background(BoltLightGray), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsCar, null, Modifier.size(64.dp), Color.Gray)
                        }
                    }
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.padding(16.dp).align(Alignment.TopStart).background(Color.White, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(name, fontWeight = FontWeight.Black, fontSize = 28.sp, color = BoltDark)
                    Text("$model • $type", color = Color.Gray, fontSize = 16.sp)
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        DetailSpecItem(Icons.Default.People, "Seats", vehicle["seats"]?.toString() ?: "5")
                        DetailSpecItem(Icons.Default.Settings, "Trans", vehicle["transmission"]?.toString() ?: "Auto")
                        DetailSpecItem(Icons.Default.LocalGasStation, "Fuel", vehicle["fuel_type"]?.toString() ?: "Gas")
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Text("Description", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
                    Spacer(Modifier.height(8.dp))
                    Text(description, color = Color.DarkGray, fontSize = 15.sp, lineHeight = 22.sp)
                    
                    if (features.isNotEmpty()) {
                        Spacer(Modifier.height(32.dp))
                        Text("Key Features", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
                        Spacer(Modifier.height(16.dp))
                        features.forEach { feature ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
                                Icon(Icons.Default.CheckCircle, null, Modifier.size(20.dp), FamekoBlue)
                                Spacer(Modifier.width(12.dp))
                                Text(feature.trim(), fontSize = 15.sp, color = Color.DarkGray)
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(32.dp))
                    
                    Text("Rental Terms", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
                    Spacer(Modifier.height(12.dp))
                    RentalTermItem("Full insurance coverage included.")
                    RentalTermItem("24/7 Roadside assistance.")
                    RentalTermItem("Fair fuel policy (Full-to-Full).")
                    
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun DetailSpecItem(icon: ImageVector, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = BoltLightGray, modifier = Modifier.size(48.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BoltDark)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun RentalTermItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Icon(Icons.Default.Info, null, Modifier.size(16.dp), Color.Gray)
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = Color.Gray)
    }
}
