package com.example.famekodriver.customer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.OrderRepository
import com.example.famekodriver.customer.ui.theme.BoltGreen
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHistoryScreen(
    title: String,
    emptyMessage: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { OrderRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    var trips by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    fun fetchTrips() {
        isLoading = true
        val customerId = sessionManager.getDriverId() ?: "1"
        scope.launch {
            repository.getCustomerTrips(customerId)
                .onSuccess {
                    trips = it
                    isLoading = false
                }
                .onFailure {
                    isLoading = false
                }
        }
    }
    
    LaunchedEffect(Unit) { fetchTrips() }

    Scaffold(
        topBar = { 
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) }, 
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    if (trips.isNotEmpty()) {
                        IconButton(onClick = {
                            val customerId = sessionManager.getDriverId() ?: "1"
                            scope.launch {
                                repository.clearAllTrips(customerId).onSuccess {
                                    Toast.makeText(context, "History cleared", Toast.LENGTH_SHORT).show()
                                    fetchTrips()
                                }
                            }
                        }) {
                            Icon(Icons.Default.DeleteSweep, "Clear All", tint = Color.Red)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            ) 
        }, 
        containerColor = Color.White
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BoltGreen)
            }
        } else if (trips.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text(emptyMessage, color = Color.Gray)
                }
            }
        } else { 
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { 
                items(trips) { trip -> 
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BoltLightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) { 
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { 
                                Text(trip["date"]?.toString()?.split(" ")?.firstOrNull() ?: "N/A", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("GHS ${trip["amount"].toString().toDoubleOrNull()?.toInt() ?: 0}", fontWeight = FontWeight.ExtraBold, color = BoltGreen)
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = {
                                        val tripId = (trip["id"] as? Double)?.toInt() ?: (trip["id"] as? Int) ?: 0
                                        scope.launch {
                                            repository.deleteTrip(tripId).onSuccess {
                                                Toast.makeText(context, "Entry deleted", Toast.LENGTH_SHORT).show()
                                                fetchTrips()
                                            }
                                        }
                                    }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BoltGreen))
                                Spacer(Modifier.width(12.dp))
                                Text(trip["pickup"].toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).background(Color.Red))
                                Spacer(Modifier.width(12.dp))
                                Text(trip["dropoff"].toString(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.5f))
                            Row(verticalAlignment = Alignment.CenterVertically) { 
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text("Driver: ${trip["driver"] ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)
                                Spacer(Modifier.weight(1f))
                                Surface(
                                    color = BoltGreen.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(trip["status"].toString(), modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = BoltGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } 
                            } 
                        } 
                    } 
                } 
            } 
        }
    }
}
