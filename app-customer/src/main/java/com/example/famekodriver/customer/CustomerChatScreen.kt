package com.example.famekodriver.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.FamekoEvent
import com.example.famekodriver.core.domain.model.Message
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerChatScreen(
    orderId: Int,
    driverName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repository = remember { DriverRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var newMessageText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val customerId = sessionManager.getDriverId()?.toIntOrNull() ?: 0

    LaunchedEffect(Unit) {
        repository.getChatHistory(orderId).onSuccess {
            messages = it
            isLoading = false
            if (it.isNotEmpty()) {
                delay(100)
                listState.scrollToItem(it.size - 1)
            }
        }.onFailure {
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        repository.events.collect { event ->
            if (event is FamekoEvent.NewMessage && event.message.conversationId == orderId) {
                messages = messages + event.message
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = BoltLightGray, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.Person, null, modifier = Modifier.padding(8.dp), tint = BoltDark)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(driverName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BoltDark)
                            Text("Active Trip", fontSize = 11.sp, color = BoltGreen, fontWeight = FontWeight.Medium)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = BoltDark)
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newMessageText,
                        onValueChange = { newMessageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message...", fontSize = 15.sp) },
                        shape = RoundedCornerShape(28.dp),
                        maxLines = 4,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = BoltLightGray,
                            unfocusedContainerColor = BoltLightGray,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = BoltGreen
                        )
                    )
                    if (newMessageText.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if (newMessageText.isNotBlank()) {
                                    val text = newMessageText.trim()
                                    newMessageText = ""
                                    val msg = Message(
                                        id = 0,
                                        conversationId = orderId,
                                        senderType = "customer",
                                        senderId = customerId,
                                        body = text,
                                        createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date()),
                                        read = false
                                    )
                                    scope.launch {
                                        repository.sendMessage(msg).onSuccess {
                                            messages = messages + it
                                        }
                                    }
                                }
                            },
                            containerColor = BoltGreen,
                            contentColor = Color.White,
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = BoltGreen)
            } else if (messages.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                    Spacer(Modifier.height(16.dp))
                    Text("No messages yet", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Say hello to your driver!", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        },
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    var lastDate = ""
                    items(messages) { msg ->
                        val isMe = msg.senderType == "customer"
                        
                        // Parse timestamp
                        val (dateStr, timeStr) = try {
                            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
                            val date = format.parse(msg.createdAt)
                            if (date != null) {
                                val d = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)
                                val t = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                                d to t
                            } else "" to ""
                        } catch (e: Exception) { "" to "" }

                        // Date Separator
                        if (dateStr != lastDate && dateStr.isNotEmpty()) {
                            lastDate = dateStr
                            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                                Surface(color = BoltLightGray.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp)) {
                                    Text(
                                        text = if (dateStr == SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())) "Today" else dateStr,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        CustomerMessageBubble(msg, isMe = isMe, time = timeStr)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerMessageBubble(message: Message, isMe: Boolean, time: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (isMe) BoltGreen else BoltLightGray,
            contentColor = if (isMe) Color.White else BoltDark,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isMe) 18.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 18.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                Text(
                    text = message.body,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                )
                if (time.isNotEmpty()) {
                    Text(
                        text = time,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.End),
                        color = if (isMe) Color.White.copy(alpha = 0.7f) else Color.Gray
                    )
                }
            }
        }
    }
}

