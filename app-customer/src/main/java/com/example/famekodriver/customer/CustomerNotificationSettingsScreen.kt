package com.example.famekodriver.customer

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
fun CustomerNotificationSettingsScreen(onBack: () -> Unit) {
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
                        Spacer(Modifier.height(2.dp))
                        Text("Choose how and when Fameko notifies you about your trips, promos, and account activity.", fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: RIDE UPDATES
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RIDE UPDATES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Recommended", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            CustomerNotificationToggleRow(
                                icon = Icons.Default.Navigation,
                                iconColor = Color(0xFF2563EB),
                                title = "Trip Status",
                                description = "Updates on driver arrival and real-time trip progress",
                                checked = tripUpdates,
                                onCheckedChange = {
                                    tripUpdates = it
                                    sessionManager.setTripUpdatesEnabled(it)
                                    Toast.makeText(context, if (it) "Trip Status Enabled" else "Trip Status Muted", Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            CustomerNotificationToggleRow(
                                icon = Icons.AutoMirrored.Filled.Chat,
                                iconColor = Color(0xFF2563EB),
                                title = "Messages",
                                description = "New messages and in-app updates from your driver",
                                checked = messages,
                                onCheckedChange = {
                                    messages = it
                                    sessionManager.setMessagesEnabled(it)
                                    Toast.makeText(context, if (it) "Messages Enabled" else "Messages Muted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Section 2: OFFERS & ACCOUNT
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("OFFERS & ACCOUNT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            CustomerNotificationToggleRow(
                                icon = Icons.Default.LocalOffer,
                                iconColor = Color(0xFFD97706),
                                title = "Promotions",
                                description = "Discounts, ride credits, and special limited offers",
                                checked = promotions,
                                onCheckedChange = {
                                    promotions = it
                                    sessionManager.setPromotionsEnabled(it)
                                    Toast.makeText(context, if (it) "Promotions Enabled" else "Promotions Muted", Toast.LENGTH_SHORT).show()
                                }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            CustomerNotificationToggleRow(
                                icon = Icons.Default.VerifiedUser,
                                iconColor = Color(0xFF10B981),
                                title = "Account Security",
                                description = "Important updates about your login, payments, and profile verification",
                                badgeText = "Required",
                                checked = accountAlerts,
                                onCheckedChange = {
                                    accountAlerts = it
                                    sessionManager.setAccountAlertsEnabled(it)
                                    Toast.makeText(context, if (it) "Security Alerts Enabled" else "Security Alerts Muted", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // Section 3: DELIVERY CHANNELS
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DELIVERY CHANNELS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 4.dp))

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column {
                            CustomerChannelRow(
                                icon = Icons.Default.Notifications,
                                iconColor = Color(0xFF64748B),
                                title = "Push Notifications",
                                subtitle = "Instant banners & dynamic alerts",
                                badgeText = "Active",
                                badgeColor = Color(0xFFECFDF5),
                                badgeTextColor = Color(0xFF059669),
                                onClick = { Toast.makeText(context, "Push Notifications Active", Toast.LENGTH_SHORT).show() }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            CustomerChannelRow(
                                icon = Icons.Default.Smartphone,
                                iconColor = Color(0xFF64748B),
                                title = "SMS Updates",
                                subtitle = "Critical trip alerts via mobile number",
                                badgeText = "Active",
                                badgeColor = Color(0xFFECFDF5),
                                badgeTextColor = Color(0xFF059669),
                                onClick = { Toast.makeText(context, "SMS Updates Active", Toast.LENGTH_SHORT).show() }
                            )
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                            CustomerChannelRow(
                                icon = Icons.Default.Mail,
                                iconColor = Color(0xFF64748B),
                                title = "Email Invoices",
                                subtitle = "Trip receipts and monthly summary",
                                actionText = "Manage",
                                actionTextColor = Color(0xFF2563EB),
                                onClick = { Toast.makeText(context, "Email Invoice Settings", Toast.LENGTH_SHORT).show() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerNotificationToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    description: String,
    badgeText: String? = null,
    checked: Boolean,
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
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                badgeText?.let {
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(it, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(description, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF10B981)
            )
        )
    }
}

@Composable
fun CustomerChannelRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = Color.LightGray,
    badgeTextColor: Color = Color.DarkGray,
    actionText: String? = null,
    actionTextColor: Color = Color(0xFF2563EB),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        badgeText?.let {
            Surface(
                color = badgeColor,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(it, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = badgeTextColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
            }
        }
        actionText?.let {
            Text(it, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = actionTextColor)
        }
    }
}
