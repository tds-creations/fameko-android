package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarningsScreen(
    onBack: () -> Unit,
    onNavigateToPayment: () -> Unit,
    viewModel: EarningsViewModel = viewModel()
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Earnings & Fees", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
        ) {
            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF004E89))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        EarningsOverviewCard(
                            todayEarnings = viewModel.stats.earningsToday,
                            completedTrips = viewModel.stats.completedToday
                        )
                    }

                    item {
                        DailyFeeStatusCard(
                            isPaid = viewModel.isDailyFeePaid,
                            amount = viewModel.dailyFeeAmount,
                            isPaying = viewModel.isPaying,
                            onPayClick = onNavigateToPayment,
                            onManualProofClick = onNavigateToPayment
                        )
                    }

                    item {
                        StatsGrid(
                            totalEarnings = viewModel.stats.totalEarnings,
                            totalTrips = viewModel.stats.totalDeliveries,
                            rating = viewModel.stats.rating,
                            completionRate = viewModel.stats.completionRate
                        )
                    }

                    item {
                        InfoCard(
                            title = "How it works",
                            text = "Pay your daily service fee to activate your account for the day. Once paid, you keep 100% of all ride fares collected directly from customers."
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsOverviewCard(todayEarnings: Double, completedTrips: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF004E89))
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Today's Earnings", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Text(
                "₵${String.format(Locale.getDefault(), "%.2f", todayEarnings)}",
                color = Color.White,
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color.White.copy(alpha = 0.2f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    "$completedTrips Trips Completed Today",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun DailyFeeStatusCard(
    isPaid: Boolean,
    amount: Double,
    isPaying: Boolean,
    onPayClick: () -> Unit,
    onManualProofClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) Color(0xFFE7F4E9) else Color(0xFFFFF4E5)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPaid) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isPaid) Color(0xFF28A745) else Color(0xFFFD7E14),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isPaid) "Daily Fee Paid" else "Daily Fee Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    text = if (isPaid) "You are active for today" else "Pay ₵${amount.toInt()} to receive requests",
                    fontSize = 13.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
            }
            if (!isPaid) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onPayClick,
                        enabled = !isPaying,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        if (isPaying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("PAY NOW", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    TextButton(
                        onClick = onManualProofClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Manual Proof", fontSize = 11.sp, color = Color(0xFF004E89), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StatsGrid(totalEarnings: Double, totalTrips: Int, rating: Double, completionRate: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem(Modifier.weight(1f), "Life Earnings", "₵${totalEarnings.toInt()}", Icons.Default.Payments, Color(0xFF28A745))
            StatItem(Modifier.weight(1f), "Total Trips", "$totalTrips", Icons.Default.TrendingUp, Color(0xFF004E89))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatItem(Modifier.weight(1f), "Avg Rating", String.format(Locale.getDefault(), "%.1f", rating), Icons.Default.Star, Color(0xFFFFC107))
            StatItem(Modifier.weight(1f), "Completion", "$completionRate%", Icons.Default.CheckCircle, Color(0xFF17A2B8))
        }
    }
}

@Composable
fun StatItem(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, color = Color.Gray, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
fun InfoCard(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE9ECEF).copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text, fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp)
            }
        }
    }
}
