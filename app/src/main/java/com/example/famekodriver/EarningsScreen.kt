package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.clip
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
                title = { 
                    Column {
                        Text("Earnings & Fees", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("Driver Online", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                        }
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
                    IconButton(onClick = { viewModel.refresh(); Toast.makeText(context, "Earnings synced", Toast.LENGTH_SHORT).show() }) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF0F172A))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Earnings Hero Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                        elevation = CardDefaults.cardElevation(8.dp)
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
                                    Text("TODAY'S EARNINGS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                                    Surface(
                                        color = Color(0xFF065F46),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            "Live",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color.White.copy(alpha = 0.1f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("Today, 24 Oct", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Icon(Icons.Default.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("GH₵ ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                Text(
                                    String.format(Locale.US, "%.2f", viewModel.stats.earningsToday),
                                    color = Color.White,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Net driver fare earnings (0% commission deducted)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )

                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)
                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        Text("${viewModel.stats.completedToday} Trip Completed", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }

                                Button(
                                    onClick = { Toast.makeText(context, "Opening payout summary...", Toast.LENGTH_SHORT).show() },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text("Payout Details", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.White, modifier = Modifier.size(10.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Daily Fee Status Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = if (viewModel.isDailyFeePaid) "Daily Fee Paid" else "Daily Fee Required",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = if (viewModel.isDailyFeePaid) "You are fully activated to take passenger trips today." else "Pay ₵${viewModel.dailyFeeAmount.toInt()} to receive requests",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFF065F46),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        "ACTIVE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Schedule, null, tint = Color(0xFF059669), modifier = Modifier.size(14.dp))
                                Text("Valid until 23:59 tonight", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }

                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFA7F3D0), thickness = 1.dp)
                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { Toast.makeText(context, "Opening payment receipt...", Toast.LENGTH_SHORT).show() },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("View Payment Receipt ↗", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }

                                TextButton(
                                    onClick = onNavigateToPayment,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Auto-Renew Setup", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                }
                            }
                        }
                    }
                }

                // Stats 2x2 Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernStatCard(
                                title = "LIFETIME",
                                label = "Life Earnings",
                                value = "₵${String.format(Locale.US, "%.2f", viewModel.stats.totalEarnings)}",
                                subtitle = "↑ +₵${String.format(Locale.US, "%.2f", viewModel.stats.earningsToday)} today",
                                subtitleColor = Color(0xFF059669),
                                icon = Icons.Default.AccountBalanceWallet,
                                iconColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                title = "VOLUME",
                                label = "Total Trips",
                                value = "${viewModel.stats.totalDeliveries}",
                                unit = "Rides",
                                subtitle = "100% accepted",
                                subtitleColor = Color.Gray,
                                icon = Icons.Default.TrendingUp,
                                iconColor = Color(0xFF2563EB),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernStatCard(
                                title = "FEEDBACK",
                                label = "Avg Rating",
                                value = String.format(Locale.US, "%.1f", viewModel.stats.rating),
                                unit = "★",
                                subtitle = "Needs attention",
                                subtitleColor = Color(0xFFD97706),
                                icon = Icons.Default.Star,
                                iconColor = Color(0xFFFFC107),
                                modifier = Modifier.weight(1f)
                            )
                            ModernStatCard(
                                title = "RELIABILITY",
                                label = "Completion",
                                value = "${viewModel.stats.completionRate}%",
                                subtitle = "Consistent",
                                subtitleColor = Color(0xFF059669),
                                icon = Icons.Default.CheckCircle,
                                iconColor = Color(0xFF059669),
                                modifier = Modifier.weight(1f),
                                showProgressBar = true
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernStatCard(
    title: String,
    label: String,
    value: String,
    unit: String? = null,
    subtitle: String,
    subtitleColor: Color,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    showProgressBar: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF0F172A))
                unit?.let { Text(it, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 2.dp)) }
            }

            Spacer(Modifier.height(6.dp))
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subtitleColor)

            if (showProgressBar) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { 0.46f },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = Color(0xFF10B981),
                    trackColor = Color(0xFFF1F5F9)
                )
            }
        }
    }
}
