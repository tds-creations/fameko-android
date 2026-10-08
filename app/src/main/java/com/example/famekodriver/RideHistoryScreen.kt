package com.example.famekodriver

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.Delivery
import com.example.famekodriver.core.domain.model.DeliveryStatus
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { DriverRepository.getInstance() }
    val sessionManager = remember { SessionManager(context) }
    val driverId = sessionManager.getDriverId() ?: ""

    var history by remember { mutableStateOf<List<Delivery>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFilter by remember { mutableStateOf("All Trips") }

    LaunchedEffect(Unit) {
        repository.getDriverHistory(driverId).fold(
            onSuccess = { list ->
                history = list.sortedByDescending { it.id }
                isLoading = false
            },
            onFailure = {
                history = emptyList()
                isLoading = false
            }
        )
    }

    val filteredHistory = remember(history, selectedFilter) {
        when (selectedFilter) {
            "Completed" -> history.filter { it.status == DeliveryStatus.DELIVERED }
            "Cash" -> history
            else -> history
        }
    }

    val totalEarnings = remember(history) {
        history.filter { it.status == DeliveryStatus.DELIVERED }.sumOf { it.estimatedEarnings }
    }
    val avgFare = remember(history) {
        val delivered = history.filter { it.status == DeliveryStatus.DELIVERED }
        if (delivered.isNotEmpty()) totalEarnings / delivered.size else 0.0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Ride History", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Text("Completed trips & earnings summary", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF0F172A), modifier = Modifier.size(14.dp))
                            Text("Today, 24 Oct", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0F172A))
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Performance Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                                    Text("TODAY'S PERFORMANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        "Live Sync",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Trips", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(Modifier.height(2.dp))
                                    Text("${history.size}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(Modifier.height(2.dp))
                                    Text("100% completed", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }

                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.15f)))

                                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text("Net Earnings", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(Modifier.height(2.dp))
                                    Text("GH₵ ${String.format(Locale.US, "%.2f", totalEarnings)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF34D399))
                                    Spacer(Modifier.height(2.dp))
                                    Text("Payout ready", fontSize = 10.sp, color = Color.Gray)
                                }

                                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color.White.copy(alpha = 0.15f)))

                                Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                                    Text("Avg Fare", fontSize = 11.sp, color = Color.Gray)
                                    Spacer(Modifier.height(2.dp))
                                    Text("GH₵ ${String.format(Locale.US, "%.2f", avgFare)}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    Spacer(Modifier.height(2.dp))
                                    Text("per trip", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }

                // Filter Tabs Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filters = listOf("All Trips (${history.size})", "Completed (${history.count { it.status == DeliveryStatus.DELIVERED }})", "Cash", "MoMo")
                        filters.forEach { filterText ->
                            val baseName = filterText.substringBefore(" (")
                            val isSelected = selectedFilter.startsWith(baseName)
                            Surface(
                                onClick = { selectedFilter = baseName },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color.White,
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                            ) {
                                Text(
                                    text = filterText,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // Trip Items List
                if (filteredHistory.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            Text("No trips found for this filter", color = Color.Gray)
                        }
                    }
                } else {
                    items(filteredHistory) { delivery ->
                        RideHistoryItem(delivery)
                    }
                }
            }
        }
    }
}

@Composable
fun RideHistoryItem(delivery: Delivery) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Order ID & Earnings
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Order #${delivery.orderId}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Today, 20:14",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        color = Color(0xFFECFDF5),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            "Paid • Cash",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "GHS ${String.format(Locale.US, "%.2f", delivery.estimatedEarnings)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = Color(0xFF10B981)
                )
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Route Timeline
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        delivery.pickupLocation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).background(Color(0xFFDC2626), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        delivery.dropOffLocation,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Footer Row: Passenger & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val name = delivery.customerName ?: "Customer"
                val initials = name.split(" ").let { if (it.size > 1) "${it[0].first()}${it[1].first()}" else it[0].take(2) }
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFE2E8F0),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(initials.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("★ 4.9 • ${String.format(Locale.US, "%.1f", delivery.distanceKm)} km • 18 min", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                val isDelivered = delivery.status == DeliveryStatus.DELIVERED
                val statusColor = if (isDelivered) Color(0xFF059669) else Color.Red
                Surface(
                    color = if (isDelivered) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = delivery.status.name,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
            }
        }
    }
}
