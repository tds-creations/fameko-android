package com.example.famekodriver.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// We use colors defined in CustomerMapActivity.kt (package-level visibility)
private val AppBlue = FamekoBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsScreen(
    onBack: () -> Unit,
    onNavigateToDetails: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var rentals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }

    fun fetchRentals() {
        isLoading = true
        val customerId = sessionManager.getDriverId() ?: ""
        scope.launch {
            repository.getCustomerRentals(customerId).onSuccess { list ->
                rentals = list
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchRentals() }

    val ongoingRentals = rentals.filter { 
        val status = it["status"]?.toString()?.uppercase() ?: "PENDING"
        status in listOf("PENDING", "ASSIGNED", "ACTIVE", "IN_PROGRESS")
    }
    
    val pastRentals = rentals.filter { 
        val status = it["status"]?.toString()?.uppercase() ?: "COMPLETED"
        status in listOf("COMPLETED", "CANCELLED", "REJECTED")
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(Color.White)) {
                TopAppBar(
                    title = { Text("Rentals", fontWeight = FontWeight.ExtraBold, color = BoltDark) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BoltDark)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.White,
                    contentColor = AppBlue,
                    indicator = { tabPositions ->
                        if (selectedTab < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = AppBlue
                            )
                        }
                    },
                    divider = { HorizontalDivider(color = BoltLightGray) }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Active", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("History", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        val currentList = if (selectedTab == 0) ongoingRentals else pastRentals

        if (isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppBlue)
            }
        } else if (currentList.isEmpty()) {
            EmptyRentalState(padding, selectedTab == 0)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp) // Tight for grouping
            ) {
                // Group by Month for past rentals
                if (selectedTab == 1) {
                    val grouped = pastRentals.groupBy { 
                        val dateStr = it["created_at"]?.toString() ?: ""
                        if (dateStr.length >= 7) dateStr.substring(0, 7) else "Other"
                    }
                    
                    grouped.forEach { (month, monthRentals) ->
                        item {
                            MonthHeader(month)
                        }
                        items(monthRentals) { rental ->
                            RentalHistoryCard(
                                rental = rental,
                                onClick = { onNavigateToDetails(rental) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = BoltLightGray, thickness = 0.5.dp)
                        }
                    }
                } else {
                    items(ongoingRentals) { rental ->
                        RentalHistoryCard(
                            rental = rental,
                            onClick = { onNavigateToDetails(rental) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(monthKey: String) {
    val displayMonth = try {
        if (monthKey == "Other") "Earlier"
        else {
            val date = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthKey)
            SimpleDateFormat("MMMM yyyy", Locale.US).format(date!!)
        }
    } catch (e: Exception) { monthKey }

    Text(
        text = displayMonth.uppercase(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Gray,
        letterSpacing = 1.sp
    )
}

@Composable
fun EmptyRentalState(padding: PaddingValues, isOngoing: Boolean) {
    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = BoltLightGray,
                modifier = Modifier.size(100.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isOngoing) Icons.Default.DirectionsCar else Icons.Default.History,
                        null,
                        Modifier.size(48.dp),
                        Color.LightGray
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                if (isOngoing) "No active rentals" else "No rental history",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = BoltDark
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isOngoing) "Your active bookings will appear here." else "Bookings you've completed will show up here.",
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun RentalHistoryCard(
    rental: Map<String, Any>,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val status = rental["status"]?.toString() ?: "PENDING"
    val paymentStatus = rental["payment_status"]?.toString() ?: "PENDING"
    val bookingCode = rental["booking_code"]?.toString() ?: "----"
    val totalPrice = rental["total_price"]?.toString()?.toDoubleOrNull() ?: 0.0
    val checkoutUrl = rental["checkout_url"]?.toString()
    val dateStr = rental["created_at"]?.toString() ?: ""
    val vehicleName = rental["vehicle_name"]?.toString() ?: "Vehicle Rental"

    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Vehicle Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = BoltLightGray,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.DirectionsCar,
                            null,
                            Modifier.size(24.dp),
                            AppBlue
                        )
                    }
                }
                
                Spacer(Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        vehicleName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = BoltDark
                    )
                    Text(
                        if (dateStr.length >= 16) dateStr.substring(0, 16) else dateStr,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₵${String.format(Locale.US, "%.2f", totalPrice)}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = BoltDark
                    )
                    StatusBadge(status)
                }
            }

            Spacer(Modifier.height(12.dp))
            
            // Locations
            LocationInfo(
                icon = Icons.Default.RadioButtonChecked,
                iconColor = BoltGreen,
                text = rental["pickup_location"]?.toString() ?: "Pickup"
            )
            
            val dest = rental["destination_location"]?.toString()
            if (!dest.isNullOrBlank()) {
                VerticalLine()
                LocationInfo(
                    icon = Icons.Default.LocationOn,
                    iconColor = Color.Red,
                    text = dest
                )
            } else {
                val stops = rental["stops"]?.toString()
                if (!stops.isNullOrBlank()) {
                    VerticalLine()
                    LocationInfo(
                        icon = Icons.Default.Flag,
                        iconColor = Color(0xFFF08C00), // Orange
                        text = "Stops: $stops"
                    )
                }
            }

            if ((paymentStatus == "PENDING" && checkoutUrl != null && status != "CANCELLED" && status != "REJECTED") || status == "COMPLETED") {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (paymentStatus == "PENDING" && checkoutUrl != null && status != "CANCELLED" && status != "REJECTED") {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(checkoutUrl))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AppBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Pay Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (status == "COMPLETED") {
                        OutlinedButton(
                            onClick = { /* Rebook logic */ },
                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Rebook", fontSize = 12.sp, color = AppBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status.uppercase()) {
        "PENDING" -> Color(0xFFF08C00)
        "ASSIGNED", "ACTIVE", "IN_PROGRESS", "COMPLETED" -> BoltGreen
        else -> Color(0xFFC92A2A)
    }

    Text(
        text = status,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun LocationInfo(icon: ImageVector, iconColor: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(10.dp), iconColor)
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            fontSize = 13.sp,
            color = BoltDark.copy(alpha = 0.8f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VerticalLine() {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .height(8.dp)
            .width(1.dp)
            .background(Color.LightGray)
    )
}
