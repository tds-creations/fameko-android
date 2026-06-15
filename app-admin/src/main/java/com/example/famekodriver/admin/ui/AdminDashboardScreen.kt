package com.example.famekodriver.admin.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.famekodriver.admin.AdminViewModel
import com.example.famekodriver.admin.ui.theme.FamekoPrimary
import com.example.famekodriver.core.domain.model.AdminPlatformStats

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel = viewModel(),
    onNavigateToCustomers: () -> Unit = {},
    onNavigateToFleet: () -> Unit = {}
) {
    var stats by remember { mutableStateOf<AdminPlatformStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.getPlatformStats().onSuccess {
            stats = it
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Text(
            "Command Center",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A202C)
        )
        Text(
            "Platform Real-time Performance",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoPrimary)
            }
        } else {
            stats?.let { s ->
                val statItems = listOf(
                    StatItem("Total Drivers", s.totalDrivers.toString(), Icons.Default.People, Color(0xFF3182CE)),
                    StatItem("Online Now", s.onlineDrivers.toString(), Icons.Default.Sensors, Color(0xFF38A169)),
                    StatItem("Live Orders", s.liveDeliveries.toString(), Icons.Default.LocalShipping, Color(0xFF805AD5)),
                    StatItem("Pending Apps", s.pendingDrivers.toString(), Icons.Default.HourglassEmpty, Color(0xFFDD6B20)),
                    StatItem("Revenue (₵)", "%.2f".format(s.totalRevenue), Icons.Default.Payments, Color(0xFF38A169)),
                    StatItem("Active SOS", s.activeSOS.toString(), Icons.Default.Warning, Color.Red)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(statItems) { item ->
                        StatCard(item)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Quick Management", fontWeight = FontWeight.Bold, color = Color(0xFF2D333A))
        Spacer(Modifier.height(8.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickActionCard("Customers", Icons.Default.Group, Color(0xFF3182CE), Modifier.weight(1f)) {
                onNavigateToCustomers()
            }
            QuickActionCard("Fleet", Icons.Default.DirectionsCar, Color(0xFF38A169), Modifier.weight(1f)) {
                onNavigateToFleet()
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("System Health", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(Color(0xFF38A169), RoundedCornerShape(5.dp)))
                    Spacer(Modifier.width(8.dp))
                    Text("Backend Services: Online", fontSize = 14.sp)
                }
            }
        }
    }
}

data class StatItem(val label: String, val value: String, val icon: ImageVector, val color: Color)

@Composable
fun QuickActionCard(label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun StatCard(item: StatItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(item.color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, null, tint = item.color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(item.label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(item.value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D333A))
        }
    }
}
