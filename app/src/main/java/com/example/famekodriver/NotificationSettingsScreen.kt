package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var tripUpdates by remember { mutableStateOf(sessionManager.getTripUpdatesEnabled()) }
    var messages by remember { mutableStateOf(sessionManager.getMessagesEnabled()) }
    var promotions by remember { mutableStateOf(sessionManager.getPromotionsEnabled()) }
    var accountAlerts by remember { mutableStateOf(sessionManager.getAccountAlertsEnabled()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Notification Settings", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                        Text("Manage alerts, surge updates & sounds", fontSize = 12.sp, color = Color.Gray)
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Activity Alerts
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "ACTIVITY ALERTS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            NotificationToggleItem(
                                icon = Icons.Default.NotificationsActive,
                                iconColor = Color(0xFF2563EB),
                                title = "New Trip Requests",
                                description = "Get notified when a new order is available nearby",
                                enabled = tripUpdates,
                                onCheckedChange = {
                                    tripUpdates = it
                                    sessionManager.setTripUpdatesEnabled(it)
                                    Toast.makeText(context, if (it) "Trip Requests Enabled" else "Trip Requests Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            NotificationToggleItem(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                iconColor = Color(0xFF059669),
                                title = "Customer Messages",
                                description = "Receive alerts for incoming messages from riders",
                                enabled = messages,
                                onCheckedChange = {
                                    messages = it
                                    sessionManager.setMessagesEnabled(it)
                                    Toast.makeText(context, if (it) "Messages Enabled" else "Messages Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Section 2: Promotions & Updates
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "PROMOTIONS & UPDATES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            NotificationToggleItem(
                                icon = Icons.Default.LocalOffer,
                                iconColor = Color(0xFFD97706),
                                title = "Promotions & Surges",
                                description = "Updates on bonuses, surges, and special offers",
                                enabled = promotions,
                                onCheckedChange = {
                                    promotions = it
                                    sessionManager.setPromotionsEnabled(it)
                                    Toast.makeText(context, if (it) "Promotions Enabled" else "Promotions Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            NotificationToggleItem(
                                icon = Icons.Default.Security,
                                iconColor = Color(0xFF7C3AED),
                                title = "Account & Security Alerts",
                                description = "Security alerts and daily access fee status",
                                enabled = accountAlerts,
                                onCheckedChange = {
                                    accountAlerts = it
                                    sessionManager.setAccountAlertsEnabled(it)
                                    Toast.makeText(context, if (it) "Account Alerts Enabled" else "Account Alerts Disabled", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationToggleItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981)
            )
        )
    }
}
