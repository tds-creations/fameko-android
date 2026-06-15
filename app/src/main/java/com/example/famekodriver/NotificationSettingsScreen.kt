package com.example.famekodriver

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("Notification Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Activity Alerts",
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            NotificationToggle(
                title = "New Trip Requests",
                description = "Get notified when a new order is available nearby",
                enabled = tripUpdates,
                onCheckedChange = {
                    tripUpdates = it
                    sessionManager.setTripUpdatesEnabled(it)
                }
            )

            NotificationToggle(
                title = "Messages",
                description = "Receive alerts for new messages from customers",
                enabled = messages,
                onCheckedChange = {
                    messages = it
                    sessionManager.setMessagesEnabled(it)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Promotions & Updates",
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            NotificationToggle(
                title = "Promotions",
                description = "Updates on bonuses, surges, and special offers",
                enabled = promotions,
                onCheckedChange = {
                    promotions = it
                    sessionManager.setPromotionsEnabled(it)
                }
            )

            NotificationToggle(
                title = "Account Alerts",
                description = "Security alerts and account status updates",
                enabled = accountAlerts,
                onCheckedChange = {
                    accountAlerts = it
                    sessionManager.setAccountAlertsEnabled(it)
                }
            )
        }
    }
}

@Composable
fun NotificationToggle(
    title: String,
    description: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(description, fontSize = 12.sp, color = Color.Gray)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF34D186),
                checkedTrackColor = Color(0xFF34D186).copy(alpha = 0.5f)
            )
        )
    }
}
