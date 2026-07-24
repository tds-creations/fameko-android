package com.example.famekodriver.customer.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.ImageLinks
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.ui.theme.*
import java.util.Locale

@Composable
fun SearchBox(
    pickupLocation: String, 
    dropOffLocation: String, 
    stops: List<String> = emptyList(),
    onPickupChange: (String) -> Unit, 
    onDropOffChange: (String) -> Unit, 
    onStopChange: (Int, String) -> Unit = { _, _ -> },
    onAddStop: () -> Unit = {},
    onRemoveStop: (Int) -> Unit = {},
    onPickupFocus: (Boolean) -> Unit = {}, 
    onDropOffFocus: (Boolean) -> Unit = {}, 
    onStopFocus: (Int, Boolean) -> Unit = { _, _ -> },
    onSearch: () -> Unit = {}, 
    isLoading: Boolean = false, 
    pickupFocusRequester: FocusRequester = remember { FocusRequester() }, 
    dropOffFocusRequester: FocusRequester = remember { FocusRequester() },
    isRentalMode: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, BoltLightGray)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Pickup
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(BoltGreen))
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = pickupLocation, 
                    onValueChange = onPickupChange, 
                    modifier = Modifier.weight(1f).focusRequester(pickupFocusRequester).onFocusChanged { onPickupFocus(it.isFocused) }, 
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BoltDark),
                    decorationBox = { innerTextField -> 
                        if (pickupLocation.isEmpty()) Text(if (isRentalMode) "Pickup for rental" else "Pickup location", color = Color.Gray, fontSize = 16.sp)
                        innerTextField() 
                    }
                )
                if (pickupLocation.isNotEmpty()) {
                    IconButton(onClick = { onPickupChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
            
            // Stops
            stops.forEachIndexed { index, stop ->
                Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                    Box(modifier = Modifier.width(10.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color.Gray))
                    Spacer(modifier = Modifier.width(16.dp))
                    BasicTextField(
                        value = stop,
                        onValueChange = { onStopChange(index, it) },
                        modifier = Modifier.weight(1f).onFocusChanged { onStopFocus(index, it.isFocused) },
                        textStyle = TextStyle(fontSize = 16.sp, color = BoltDark),
                        decorationBox = { innerTextField -> 
                            if (stop.isEmpty()) Text("Stop ${index + 1}", color = Color.Gray, fontSize = 16.sp)
                            innerTextField() 
                        }
                    )
                    IconButton(onClick = { onRemoveStop(index) }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp), tint = Color.Gray) }
                }
            }

            // Connection line
            Row(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                Box(modifier = Modifier.width(10.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color.LightGray))
                }
            }

            // Destination
            Row(verticalAlignment = Alignment.CenterVertically) { 
                Box(modifier = Modifier.size(10.dp).background(BoltOrange, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(16.dp))
                BasicTextField(
                    value = dropOffLocation, 
                    onValueChange = onDropOffChange, 
                    modifier = Modifier.weight(1f).focusRequester(dropOffFocusRequester).onFocusChanged { onDropOffFocus(it.isFocused) }, 
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BoltDark),
                    decorationBox = { innerTextField -> 
                        if (dropOffLocation.isEmpty()) Text("Where to?", color = Color.Gray, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        innerTextField() 
                    }
                )
                
                if (dropOffLocation.isNotEmpty()) {
                    IconButton(onClick = { onDropOffChange("") }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }

                if (isRentalMode) {
                    IconButton(onClick = onAddStop) { Icon(Icons.Default.Add, null, modifier = Modifier.size(24.dp), tint = FamekoBlue) }
                }

                if (pickupLocation.isNotEmpty() && (dropOffLocation.isNotEmpty() || (isRentalMode && stops.isNotEmpty()))) {
                    IconButton(onClick = onSearch, modifier = Modifier.size(36.dp).background(FamekoBlue, CircleShape)) { 
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White) 
                        else Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    } 
                } 
            }
        }
    }
}

@Composable
fun ServiceSelectionSheet(
    estimates: List<RideEstimateResponse>,
    activeServiceMode: ServiceType,
    discountRate: Int,
    peakMultiplier: Double,
    selectedType: String,
    onTypeSelected: (String, Double) -> Unit,
    onConfirm: () -> Unit,
    onScheduleClick: () -> Unit,
    isPlacing: Boolean,
    isLoading: Boolean = false,
    onUnavailableClick: (String) -> Unit = {}
) {
    val filteredEstimates = estimates.filter { estimate ->
        when (activeServiceMode) {
            ServiceType.RIDE_HAILING -> estimate.serviceType.equals("RIDE_HAILING", ignoreCase = true)
            ServiceType.PACKAGE_DELIVERY -> estimate.serviceType.equals("PACKAGE_DELIVERY", ignoreCase = true)
            else -> true
        }
    }
    
    LaunchedEffect(estimates.size, filteredEstimates.size) {
        if (estimates.isNotEmpty() && filteredEstimates.isEmpty()) {
            android.util.Log.w("PricingDiag", "All ${estimates.size} estimates filtered out! Mode: $activeServiceMode")
            estimates.forEach { 
                android.util.Log.d("PricingDiag", " - ${it.name}: type=${it.serviceType}")
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        if (discountRate > 0) {
            Surface(
                color = FamekoBlue,
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth().offset(y = (-8).dp)
            ) {
                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("$discountRate% promo applied", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.White, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                }
            }
        }
        
        if (peakMultiplier > 1.0) {
            Surface(
                color = BoltOrange.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = BoltOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Demand is high. Prices are slightly higher.", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BoltOrange)
                }
            }
        }
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 400.dp, min = if (filteredEstimates.isEmpty()) 100.dp else 0.dp)
        ) {
            if (filteredEstimates.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        if (isLoading) {
                            CircularProgressIndicator(color = FamekoBlue)
                        } else {
                            Text("No services available in this region", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(filteredEstimates) { estimate ->
                    ServiceItem(
                        estimate = estimate,
                        isSelected = selectedType == estimate.serviceId,
                        onSelect = { onTypeSelected(estimate.serviceId, estimate.fare) },
                        discountRate = discountRate,
                        onUnavailableClick = onUnavailableClick
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Money, null, tint = BoltGreen, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cash", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BoltDark)
            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
                enabled = !isPlacing && filteredEstimates.isNotEmpty()
            ) {
                if (isPlacing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Select $selectedType", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }

            Box {
                FloatingActionButton(
                    onClick = onScheduleClick,
                    containerColor = FamekoBlue,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(Icons.Default.CalendarMonth, null, modifier = Modifier.size(28.dp))
                }
                
                // Tooltip mock
                Surface(
                    color = Color.Black,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.align(Alignment.TopCenter).offset(y = (-45).dp)
                ) {
                    Text("Schedule a ride", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun ServiceItem(
    estimate: RideEstimateResponse,
    isSelected: Boolean,
    onSelect: () -> Unit,
    discountRate: Int,
    onUnavailableClick: (String) -> Unit = {}
) {
    val isAvailable = estimate.isAvailableInRegion
    val status = estimate.availabilityStatus
    val backgroundColor = if (isSelected) Color.Transparent else Color.Transparent
    
    val finalFare = (estimate.fare * (100 - discountRate) / 100).toInt()
    val originalFare = estimate.fare.toInt()

    Surface(
        onClick = {
            if (isAvailable) {
                onSelect()
            } else {
                onUnavailableClick(estimate.name)
            }
        },
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        border = if (isSelected) BorderStroke(2.dp, FamekoBlue) else null,
        modifier = Modifier.fillMaxWidth(),
        enabled = true
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp).alpha(if (isAvailable && status != "BUSY") 1f else 0.8f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                val iconUrl = estimate.icon.lowercase().let { lowIcon ->
                    when {
                        lowIcon.contains("okada") || lowIcon.contains("motorcycle") || lowIcon.contains("rider") || lowIcon.contains("bike") || lowIcon.contains("motorbike") || lowIcon.contains("motor") -> ImageLinks.IC_OKADA
                        lowIcon.contains("pragya") -> ImageLinks.IC_PRAGYA
                        lowIcon.contains("aboboyaa") -> ImageLinks.IC_ABOBOYAA
                        lowIcon.contains("truck") -> ImageLinks.IC_TRUCK
                        lowIcon.contains("bicycle") -> ImageLinks.IC_BICYCLE
                        else -> ImageLinks.IC_CAR_SALOON
                    }
                }
                AsyncImage(
                    model = iconUrl,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(estimate.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
                    if (estimate.name == "Comfort") {
                         Icon(Icons.Default.KeyboardDoubleArrowUp, null, tint = Color.Gray, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${estimate.pickupEtaMin} min", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(" 4", fontSize = 13.sp, color = Color.Gray)
                }
                
                if (estimate.pickupEtaMin <= 5) {
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        color = FamekoBlue,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("FASTER", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text("GH₵$finalFare", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BoltDark)
                if (discountRate > 0) {
                    Text(
                        text = "GH₵$originalFare",
                        style = TextStyle(
                            color = Color.Gray,
                            fontSize = 14.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SearchingSheetContent(viewModel: CustomerMapViewModel, onCancel: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) { 
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = BoltGreen)
        Spacer(Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Finding your driver...", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            if (viewModel.retryAttempt > 0) {
                Spacer(Modifier.width(8.dp))
                Badge(containerColor = BoltGreen, contentColor = Color.White) {
                    Text("Retry ${viewModel.retryAttempt}/${viewModel.maxRetryAttempts}", modifier = Modifier.padding(horizontal = 4.dp))
                }
            }
        }
        
        Text(viewModel.searchMessage, color = Color.Gray, fontSize = 14.sp)
        if (viewModel.retryAttempt > 0) {
            Text("Search radius: ${String.format("%.1f", viewModel.searchRadiusKm)} km", color = BoltGreen, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.height(32.dp))
        OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(12.dp)) { 
            Text("Cancel Request", color = Color.Red, fontWeight = FontWeight.Bold) 
        } 
    }
}

@Composable
fun DriverInfoSheetContent(
    data: OrderStatusResponse, 
    orderId: Int, 
    onNavigateToChat: (Int, String) -> Unit, 
    onCancel: () -> Unit, 
    onInitiateCall: () -> Unit,
    onShareTrip: () -> Unit
) {
    val pin = data.verificationPin
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(
                    shape = CircleShape,
                    color = BoltLightGray,
                    modifier = Modifier.size(68.dp),
                    border = BorderStroke(2.dp, Color.White)
                ) {
                    if (!data.driverProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = data.driverProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(16.dp), tint = Color.Gray)
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(24.dp).offset(x = 4.dp, y = 4.dp)
                ) {
                    Icon(Icons.Default.Star, null, tint = BoltYellow, modifier = Modifier.padding(4.dp))
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(data.driverName ?: "Your Driver", style = MaterialTheme.typography.titleLarge, color = BoltDark)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${data.driverRating ?: 4.9}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BoltDark)
                    Text(" • ", color = Color.Gray)
                    Text(data.driverVehicle ?: "Fameko Economy", fontSize = 14.sp, color = Color.Gray)
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onInitiateCall,
                    modifier = Modifier.size(44.dp).background(FamekoGold.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Call, null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { onNavigateToChat(orderId, data.driverName ?: "Driver") },
                    modifier = Modifier.size(44.dp).background(FamekoGold.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        
        Surface(
            color = BoltLightGray,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.DirectionsCar, null, tint = FamekoBlue, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = data.driverVehicleModel ?: "Vehicle Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BoltDark
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = data.driverVehicleNumber ?: "PLATE NUMBER",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        val phone = data.driverPhone
                        if (!phone.isNullOrEmpty()) {
                            Text(" • ", color = Color.Gray)
                            Text(
                                text = phone,
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                if (data.status == "ASSIGNED" && pin != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("PIN", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(pin, fontSize = 20.sp, fontWeight = FontWeight.Black, color = FamekoBlue, letterSpacing = 2.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray)
            ) {
                Text("Cancel", color = BoltDark, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onShareTrip,
                modifier = Modifier.weight(1f).height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoltDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Share Trip", fontWeight = FontWeight.Bold)
            }
        }
    }
}
