package com.example.famekodriver.customer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.UserRepository
import com.example.famekodriver.core.domain.model.FamekoEvent
import com.example.famekodriver.customer.ui.theme.BoltGreen
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import com.example.famekodriver.customer.ui.theme.FamekoBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { UserRepository() }
    val sessionManager = remember { SessionManager(context) }
    var tickets by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    
    LaunchedEffect(Unit) {
        val customerId = sessionManager.getDriverId() ?: "1"
        repository.getSupportTickets(customerId).onSuccess { tickets = it }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Support", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(Modifier.padding(16.dp)) {
                Text("How can we help?", fontWeight = FontWeight.Bold, fontSize = 24.sp)
                Spacer(Modifier.height(16.dp))
                val categories = listOf(
                    "Trip issues" to Icons.Default.DirectionsCar,
                    "Payment & Pricing" to Icons.Default.CreditCard,
                    "Account & Profile" to Icons.Default.Person,
                    "General questions" to Icons.AutoMirrored.Filled.Help
                )
                categories.forEach { (title, icon) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { }
                            .padding(vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = BoltGreen)
                        Spacer(Modifier.width(16.dp))
                        Text(title, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
                    }
                    HorizontalDivider(color = BoltLightGray)
                }
            }
            Spacer(Modifier.height(24.dp))
            Box(Modifier.fillMaxWidth().background(BoltLightGray).padding(16.dp)) {
                Text("Recent Tickets", fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            LazyColumn(Modifier.fillMaxSize()) {
                items(tickets) { ticket ->
                    ListItem(
                        headlineContent = { Text(ticket["subject"].toString(), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text(ticket["date"].toString(), fontSize = 12.sp) },
                        trailingContent = {
                            val color = if (ticket["status"] == "OPEN") Color.Blue else Color.Gray
                            Text(ticket["status"].toString(), color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        },
                        modifier = Modifier.clickable { }
                    )
                    HorizontalDivider(color = BoltLightGray, modifier = Modifier.padding(horizontal = 16.dp))
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BoltGreen)
                    ) {
                        Text("Chat with Support", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotificationsScreen(
    notifications: List<FamekoEvent.NotificationReceived>,
    onDelete: (Int) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No notifications yet", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(notifications, key = { it.id }) { notification ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { },
                                onLongClick = { onDelete(notification.id) }
                            ),
                        colors = CardDefaults.cardColors(containerColor = BoltLightGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(
                                        when(notification.type) {
                                            "ERROR", "REGIONAL" -> Color.Red
                                            "SUCCESS" -> BoltGreen
                                            else -> FamekoBlue
                                        }
                                    )
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(notification.message, color = Color.DarkGray, fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(notification.createdAt, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.align(Alignment.End))
                        }
                    }
                }
            }
        }
    }
}
