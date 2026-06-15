package com.example.famekodriver.customer

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.famekodriver.customer.ui.theme.*

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
                title = { Text("Notification Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "RIDE UPDATES",
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )

            CustomerNotificationToggle(
                title = "Trip Status",
                description = "Updates on driver arrival and trip progress",
                enabled = tripUpdates,
                onCheckedChange = {
                    tripUpdates = it
                    sessionManager.setTripUpdatesEnabled(it)
                }
            )

            CustomerNotificationToggle(
                title = "Messages",
                description = "New messages from your driver",
                enabled = messages,
                onCheckedChange = {
                    messages = it
                    sessionManager.setMessagesEnabled(it)
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BoltLightGray)
            
            Text(
                "OFFERS & ACCOUNT",
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )

            CustomerNotificationToggle(
                title = "Promotions",
                description = "Discounts, ride credits, and special offers",
                enabled = promotions,
                onCheckedChange = {
                    promotions = it
                    sessionManager.setPromotionsEnabled(it)
                }
            )

            CustomerNotificationToggle(
                title = "Account Security",
                description = "Important updates about your login and profile",
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
fun CustomerNotificationToggle(
    title: String,
    description: String,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BoltDark)
            Text(description, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(
            checked = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = BoltGreen,
                checkedTrackColor = BoltGreen.copy(alpha = 0.5f)
            )
        )
    }
}
