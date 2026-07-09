package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.PricingConfig
import com.example.famekodriver.customer.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalBookingScreen(
    vehicle: Map<String, Any>,
    onBack: () -> Unit,
    onConfirm: (Int, Int, String, Double, String?, String?, String?, Boolean) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repository = remember { DriverRepository() }
    var pricingConfig by remember { mutableStateOf<PricingConfig?>(null) }
    
    LaunchedEffect(Unit) {
        repository.getPricingConfig().onSuccess { pricingConfig = it }
    }

    var selectedDays by remember { mutableIntStateOf(1) }
    var tripNotes by remember { mutableStateOf("") }
    var isScheduled by remember { mutableStateOf(false) }
    var isSelfDrive by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var stops by remember { mutableStateOf<List<String>>(emptyList()) }

    val datePickerState = rememberDatePickerState()
    val selectedDateText = remember(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it))
        } ?: "Select Date"
    }

    val dailyRate = vehicle["daily_rate"]?.toString()?.toDoubleOrNull() ?: 400.0
    val guestFeePercent = pricingConfig?.rentalCustomerServiceFeePercent ?: 7.5
    
    // Self-drive discount or chauffeur fee logic
    // Usually chauffeur adds cost, but here we can define it as:
    // Base rate is with driver, self-drive gets a small discount OR
    // Base rate is car only, chauffeur adds a daily fee.
    // Let's assume dailyRate is for the car, and chauffeur is extra.
    val chauffeurDailyFee = if (!isSelfDrive) 50.0 else 0.0
    
    val bookingServiceFee = (dailyRate + chauffeurDailyFee) * (guestFeePercent / 100.0)
    val subtotal = selectedDays * (dailyRate + chauffeurDailyFee)
    val grandTotal = subtotal + bookingServiceFee

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false }) { Text("OK") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Complete Your Booking", fontWeight = FontWeight.ExtraBold) },
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
                            if (isScheduled && datePickerState.selectedDateMillis == null) {
                                Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
                            } else {
                                val stopsStr = if (stops.isNotEmpty()) stops.filter { s -> s.isNotBlank() }.joinToString("|") else null
                                val vId = (vehicle["id"] as? Double)?.toInt() ?: (vehicle["id"] as? Int) ?: 1
                                onConfirm(selectedDays, vId, vehicle["vehicle_type"].toString(), grandTotal, if (isScheduled) selectedDateText else null, tripNotes, stopsStr, isSelfDrive)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
                    ) {
                        Text(if (isScheduled) "Schedule Rental" else "Confirm & Pay ₵${String.format(Locale.US, "%.2f", grandTotal)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                },
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
                            Text(vehicle["name"].toString(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("${vehicle["model"]} • ${vehicle["vehicle_type"]}", color = Color.Gray, fontSize = 13.sp)
                            Text("₵${dailyRate.toInt()} / day", fontWeight = FontWeight.Bold, color = FamekoBlue, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Rental Type (Self-Drive vs Chauffeur)
            item {
                Column {
                    Text("Rental Mode", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FamekoDark)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimingOption(
                            icon = Icons.Default.Person,
                            label = "With Driver",
                            isSelected = !isSelfDrive,
                            onClick = { isSelfDrive = false },
                            modifier = Modifier.weight(1f)
                        )
                        TimingOption(
                            icon = Icons.Default.DirectionsCar,
                            label = "Self-Drive",
                            isSelected = isSelfDrive,
                            onClick = { isSelfDrive = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    if (isSelfDrive) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            color = FamekoGold.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, null, Modifier.size(16.dp), FamekoGold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Self-drive requires a valid driver's license and security deposit.",
                                    fontSize = 11.sp,
                                    color = FamekoDark
                                )
                            }
                        }
                    }
                }
            }

            // Rental Duration
            item {
                Column {
                    Text("Rental Duration", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FamekoDark)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 2, 3, 5, 7, 14, 30).forEach { days ->
                            val isSelected = selectedDays == days
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clickable { selectedDays = days },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) FamekoBlue else Color.White,
                                border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)) else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (days >= 7) "${days/7}w" else "${days}d",
                                        color = if (isSelected) Color.White else FamekoDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Scheduling
            item {
                Column {
                    Text("Pickup Timing", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FamekoDark)
                    Spacer(Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimingOption(
                            icon = Icons.Default.FlashOn,
                            label = "Immediate",
                            isSelected = !isScheduled,
                            onClick = { isScheduled = false },
                            modifier = Modifier.weight(1f)
                        )
                        TimingOption(
                            icon = Icons.Default.CalendarMonth,
                            label = "Scheduled",
                            isSelected = isScheduled,
                            onClick = { isScheduled = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    AnimatedVisibility(visible = isScheduled) {
                        Surface(
                            modifier = Modifier.padding(top = 16.dp).fillMaxWidth().height(56.dp).clickable { showDatePicker = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                                Icon(Icons.Default.Event, null, tint = FamekoBlue)
                                Spacer(Modifier.width(12.dp))
                                Text(selectedDateText, fontWeight = FontWeight.Medium, color = FamekoDark)
                            }
                        }
                    }
                }
            }

            // Optional Stops
            item {
                Column {
                    Text("Route Stops (Optional)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FamekoDark)
                    Spacer(Modifier.height(16.dp))
                    stops.forEachIndexed { index, stop ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            OutlinedTextField(
                                value = stop,
                                onValueChange = { newValue -> stops = stops.toMutableList().apply { set(index, newValue) } },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("e.g. Osu, Airport City...") },
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = FamekoBlue)
                            )
                            IconButton(onClick = { stops = stops.toMutableList().apply { removeAt(index) } }) {
                                Icon(Icons.Default.Delete, null, tint = Color.Gray)
                            }
                        }
                    }
                    if (stops.size < 5) {
                        TextButton(onClick = { stops = stops + "" }) {
                            Icon(Icons.Default.Add, null, tint = FamekoBlue)
                            Spacer(Modifier.width(8.dp))
                            Text("Add a destination/stop", color = FamekoBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Trip Notes
            item {
                Column {
                    Text("Additional Instructions", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FamekoDark)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tripNotes,
                        onValueChange = { tripNotes = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Needs trunk space, Driver should speak Twi...") },
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.White, unfocusedContainerColor = Color.White, focusedIndicatorColor = FamekoBlue)
                    )
                }
            }

            // Total Summary
            item {
                pricingConfig?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = FamekoLightBlue.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Payment Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(16.dp))
                            SummaryRow("Rental Duration", "$selectedDays days")
                            if (!isSelfDrive) {
                                SummaryRow("Chauffeur Service", "₵${chauffeurDailyFee.toInt()} / day")
                            }
                            SummaryRow("Gross Total", "₵${String.format(Locale.US, "%.2f", grandTotal)}", isTotal = true)
                            
                            Spacer(Modifier.height(16.dp))
                            Surface(
                                color = FamekoBlue.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, null, Modifier.size(16.dp), FamekoBlue)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Total includes booking commission, insurance, and road assistance.",
                                        fontSize = 11.sp,
                                        color = FamekoDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun TimingOption(icon: ImageVector, label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.height(56.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) FamekoBlue.copy(alpha = 0.1f) else Color.White,
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, FamekoBlue) else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = if (isSelected) FamekoBlue else FamekoDark, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) FamekoBlue else FamekoDark)
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isTotal: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = if (isTotal) FamekoDark else Color.Gray, fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Medium)
        Text(value, color = if (isTotal) FamekoBlue else FamekoDark, fontWeight = if (isTotal) FontWeight.Black else FontWeight.Bold, fontSize = if (isTotal) 18.sp else 14.sp)
    }
}
