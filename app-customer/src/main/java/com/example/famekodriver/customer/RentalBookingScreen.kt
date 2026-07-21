package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.famekodriver.customer.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalBookingScreen(
    vehicle: Map<String, Any>,
    onBack: () -> Unit,
    onConfirm: (Int, Int, String, Double, String?, String?, String?, Boolean, String) -> Unit
) {
    val context = LocalContext.current
    var selectedDays by remember { mutableIntStateOf(1) }
    var tripNotes by remember { mutableStateOf("") }
    var isSelfDrive by remember { mutableStateOf(false) }
    var paymentMethod by remember { mutableStateOf("ELECTRONIC") }
    
    val dailyRate = vehicle["daily_rate"]?.toString()?.toDoubleOrNull() ?: 0.0
    val subtotal = selectedDays * dailyRate
    val serviceFee = subtotal * 0.05
    val total = subtotal + serviceFee

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Booking", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = Color.White
            ) {
                Column(modifier = Modifier.padding(20.dp).navigationBarsPadding()) {
                    Button(
                        onClick = {
                            val vId = (vehicle["id"] as? Number)?.toInt() ?: 1
                            onConfirm(selectedDays, vId, vehicle["vehicle_type"].toString(), total, null, tripNotes, null, isSelfDrive, paymentMethod)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
                    ) {
                        Text("Confirm GH₵${total.toInt()}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA)),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        val imageUrl = vehicle["image_urls"]?.toString()?.split(",")?.firstOrNull()
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(vehicle["name"]?.toString() ?: "Vehicle", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("${vehicle["model"] ?: ""} • ${vehicle["vehicle_type"] ?: ""}", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Rental Type
            item {
                Column {
                    Text("Rental Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BookingOption(
                            icon = Icons.Default.Person,
                            label = "With Driver",
                            isSelected = !isSelfDrive,
                            onClick = { isSelfDrive = false },
                            modifier = Modifier.weight(1f)
                        )
                        BookingOption(
                            icon = Icons.Default.DirectionsCar,
                            label = "Self-Drive",
                            isSelected = isSelfDrive,
                            onClick = { isSelfDrive = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Duration
            item {
                Column {
                    Text("Duration (Days)", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5, 7).forEach { days ->
                            val isSelected = selectedDays == days
                            Surface(
                                modifier = Modifier.weight(1f).height(44.dp).clickable { selectedDays = days },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) FamekoBlue else Color.White,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(days.toString(), color = if (isSelected) Color.White else BoltDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // Payment Method
            item {
                Column {
                    Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        BookingOption(
                            icon = Icons.Default.Payments,
                            label = "Cash",
                            isSelected = paymentMethod == "CASH",
                            onClick = { paymentMethod = "CASH" },
                            modifier = Modifier.weight(1f)
                        )
                        BookingOption(
                            icon = Icons.Default.AccountBalanceWallet,
                            label = "Electronic",
                            isSelected = paymentMethod == "ELECTRONIC",
                            onClick = { paymentMethod = "ELECTRONIC" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Trip Notes
            item {
                Column {
                    Text("Special Instructions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tripNotes,
                        onValueChange = { tripNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Needs trunk space...") },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White)
                    )
                }
            }

            // Price Breakdown
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = FamekoBlue.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        PriceRow("Daily Rate", "GH₵${dailyRate.toInt()}")
                        PriceRow("Duration", "$selectedDays days")
                        PriceRow("Service Fee (5%)", "GH₵${serviceFee.toInt()}")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.3f))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text("GH₵${total.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = FamekoBlue)
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun BookingOption(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(56.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) FamekoBlue.copy(alpha = 0.1f) else Color.White,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, FamekoBlue) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (isSelected) FamekoBlue else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) FamekoBlue else BoltDark)
        }
    }
}

@Composable
fun PriceRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}
