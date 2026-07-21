package com.example.famekodriver.customer

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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.RentalRepository
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RentalsScreen(
    onBack: () -> Unit,
    onNavigateToDetails: (Map<String, Any>) -> Unit,
    onRebook: (Map<String, Any>) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RentalRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    
    var rentals by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    fun loadRentals() {
        isLoading = true
        scope.launch {
            val customerId = sessionManager.getCustomerId() ?: ""
            repository.getCustomerRentals(customerId).onSuccess {
                rentals = it
                isLoading = false
            }.onFailure {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadRentals() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Rentals", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8F9FA)
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoBlue)
            }
        } else if (rentals.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.History, null, Modifier.size(64.dp), Color.LightGray)
                Spacer(Modifier.height(16.dp))
                Text("No rental history yet", fontWeight = FontWeight.Bold, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)) {
                    Text("Browse Vehicles")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rentals) { rental ->
                    RentalHistoryItem(rental, onClick = { onNavigateToDetails(rental) })
                }
            }
        }
    }
}

@Composable
fun RentalHistoryItem(rental: Map<String, Any>, onClick: () -> Unit) {
    val status = rental["status"]?.toString() ?: "PENDING"
    val vehicleName = rental["vehicle_name"]?.toString() ?: "Vehicle"
    val date = rental["created_at"]?.toString()?.take(10) ?: ""
    val price = rental["total_price"]?.toString() ?: "0"
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = BoltLightGray, modifier = Modifier.size(60.dp)) {
                AsyncImage(
                    model = rental["vehicle_image"],
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(vehicleName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(date, fontSize = 13.sp, color = Color.Gray)
                Text("GH₵$price", fontWeight = FontWeight.Black, fontSize = 14.sp, color = FamekoBlue)
            }
            StatusBadge(status)
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status.uppercase()) {
        "ACTIVE", "IN_PROGRESS" -> BoltGreen
        "PENDING", "BOOKED" -> FamekoBlue
        "COMPLETED" -> Color.Gray
        else -> Color.Red
    }
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
