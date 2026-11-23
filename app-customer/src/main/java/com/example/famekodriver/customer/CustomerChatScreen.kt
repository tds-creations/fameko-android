package com.example.famekodriver.customer

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.data.repository.OrderRepository
import com.example.famekodriver.core.domain.model.FamekoEvent
import com.example.famekodriver.core.domain.model.Message
import com.example.famekodriver.core.domain.model.OrderStatusResponse
import com.example.famekodriver.core.utils.PhoneNumberUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val FamekoGreen = Color(0xFF00A859)
private val LightMintBg = Color(0xFFEDF7F2)
private val DarkText = Color(0xFF1F2937)
private val SubtitleGray = Color(0xFF64748B)
private val ChatBubbleIncoming = Color(0xFFF3F4F6)
private val AmberNoticeBg = Color(0xFFFEF9C3)
private val AmberNoticeBorder = Color(0xFFFDE68A)
private val AmberNoticeText = Color(0xFF78350F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerChatScreen(
    orderId: Int,
    driverName: String,
    orderStatus: OrderStatusResponse? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val repository = remember { DriverRepository.getInstance() }
    val orderRepository = remember { OrderRepository() }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var activeOrderStatus by remember { mutableStateOf(orderStatus) }
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var newMessageText by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var isDriverTyping by remember { mutableStateOf(false) }
    var showSafetyModal by remember { mutableStateOf(false) }
    var typingJob by remember { mutableStateOf<Job?>(null) }

    val customerId = sessionManager.getDriverId()?.toIntOrNull() ?: 0

    // Extract dynamic trip info
    val currentDriverName = activeOrderStatus?.driverName ?: driverName.ifBlank { "Driver" }
    val driverPhone = activeOrderStatus?.driverPhone ?: ""
    val driverProfilePic = activeOrderStatus?.driverProfilePic
    val orderRef = activeOrderStatus?.orderId ?: orderId

    val locationText = remember(activeOrderStatus) {
        val status = activeOrderStatus
        if (status != null) {
            val isPickup = status.status.uppercase() == "ASSIGNED" || status.status.uppercase() == "ARRIVED" || status.status.uppercase() == "PENDING"
            val label = if (isPickup) "Pickup: " else "Dropoff: "
            val loc = if (isPickup) (status.pickupLocation ?: "Pickup Location") else (status.dropOffLocation ?: "Destination")
            label + loc
        } else {
            "Pickup: Active Trip Location"
        }
    }

    val etaMinutes = remember(activeOrderStatus) {
        "4 min"
    }

    // Driver initials fallback
    val driverInitials = remember(currentDriverName) {
        val parts = currentDriverName.trim().split(" ").filter { it.isNotBlank() }
        if (parts.size >= 2) {
            "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
        } else if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
            parts[0].take(2).uppercase()
        } else {
            "D"
        }
    }

    // Load initial chat history & order details if not provided
    LaunchedEffect(orderId) {
        if (activeOrderStatus == null) {
            scope.launch {
                orderRepository.getOrderStatus(orderId).onSuccess { resp ->
                    activeOrderStatus = resp
                }
            }
        }

        repository.getChatHistory(orderId).onSuccess { list ->
            messages = list
            isLoading = false
            if (list.isNotEmpty()) {
                delay(100)
                listState.scrollToItem(list.size - 1)
            }
        }.onFailure {
            isLoading = false
        }
    }

    // Listen to real-time events over WebSocket
    LaunchedEffect(orderId) {
        repository.events.collect { event ->
            when (event) {
                is FamekoEvent.NewMessage -> {
                    if (event.message.conversationId == orderId) {
                        val exists = messages.any { it.id == event.message.id && it.id > 0 } ||
                                messages.any { it.body == event.message.body && it.senderType == event.message.senderType && it.id < 0 }
                        if (!exists) {
                            messages = messages.filterNot { it.id < 0 && it.body == event.message.body } + event.message
                        } else {
                            messages = messages.map { existing ->
                                if (existing.id < 0 && existing.body == event.message.body) event.message else existing
                            }
                        }
                        isDriverTyping = false
                    }
                }
                is FamekoEvent.UserTyping -> {
                    if (event.conversationId == orderId) {
                        isDriverTyping = event.isTyping
                    }
                }
                else -> {}
            }
        }
    }

    // Auto-scroll when messages update
    LaunchedEffect(messages.size, isDriverTyping) {
        if (messages.isNotEmpty() || isDriverTyping) {
            val targetIndex = (messages.size + (if (isDriverTyping) 1 else 0)) - 1
            if (targetIndex >= 0) {
                listState.animateScrollToItem(targetIndex)
            }
        }
    }

    // Helper to send message (or retry)
    fun sendMessageImpl(textToSend: String, retryMessageId: Int? = null) {
        val trimmed = textToSend.trim()
        if (trimmed.isBlank()) return

        val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val tempId = retryMessageId ?: -System.currentTimeMillis().toInt()
        val pendingMsg = Message(
            id = tempId,
            conversationId = orderId,
            senderType = "customer",
            senderId = customerId,
            body = trimmed,
            createdAt = nowIso,
            read = false,
            status = "SENDING"
        )

        if (retryMessageId != null) {
            messages = messages.map { if (it.id == retryMessageId) pendingMsg else it }
        } else {
            messages = messages + pendingMsg
            newMessageText = ""
        }

        scope.launch {
            repository.sendMessage(pendingMsg).onSuccess { serverMsg ->
                val finalMsg = serverMsg.copy(status = "SENT")
                messages = messages.map { if (it.id == tempId) finalMsg else it }
            }.onFailure {
                val failedMsg = pendingMsg.copy(status = "FAILED")
                messages = messages.map { if (it.id == tempId) failedMsg else it }
            }
        }
    }

    // Handle typing events debounce
    fun onInputTextChange(text: String) {
        newMessageText = text
        typingJob?.cancel()
        typingJob = scope.launch {
            repository.sendTypingEvent(orderId, text.isNotBlank())
            if (text.isNotBlank()) {
                delay(2500)
                repository.sendTypingEvent(orderId, false)
            }
        }
    }

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = DarkText
                        )
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Driver Profile Avatar
                        Box(modifier = Modifier.size(42.dp)) {
                            if (!driverProfilePic.isNullOrBlank()) {
                                AsyncImage(
                                    model = driverProfilePic,
                                    contentDescription = currentDriverName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, FamekoGreen.copy(alpha = 0.3f), CircleShape)
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = driverInitials,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = FamekoGreen
                                        )
                                    }
                                }
                            }
                            // Active status dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF10B981), CircleShape)
                                    .border(1.5.dp, Color.White, CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentDriverName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "● ",
                                    fontSize = 10.sp,
                                    color = FamekoGreen,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "ACTIVE TRIP • DRIVER",
                                    fontSize = 11.sp,
                                    color = FamekoGreen,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // Phone Call Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE8F5E9),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    PhoneNumberUtils.openDialer(context, driverPhone, currentDriverName)
                                }
                            ) {
                                Icon(
                                    Icons.Default.Call,
                                    contentDescription = "Call Driver",
                                    tint = FamekoGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Safety/Security Button
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.size(38.dp)
                        ) {
                            IconButton(
                                onClick = { showSafetyModal = true }
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = "Safety Guidelines",
                                    tint = Color(0xFF374151),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Quick Replies Strip
                CustomerQuickRepliesBar(
                    onSelectReply = { replyText ->
                        sendMessageImpl(replyText)
                    }
                )

                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                // Message Input Composer
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Attachment Plus Icon
                        IconButton(
                            onClick = {
                                Toast.makeText(context, "Quick photos & location sharing", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.AddCircleOutline,
                                contentDescription = "Attachments",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Input Field
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                TextField(
                                    value = newMessageText,
                                    onValueChange = { onInputTextChange(it) },
                                    modifier = Modifier.weight(1f),
                                    placeholder = {
                                        Text("Type a message...", fontSize = 15.sp, color = Color(0xFF94A3B8))
                                    },
                                    maxLines = 4,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                    keyboardActions = KeyboardActions(
                                        onSend = {
                                            if (newMessageText.isNotBlank()) {
                                                sendMessageImpl(newMessageText)
                                            }
                                        }
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = FamekoGreen
                                    )
                                )

                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Voice Message",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Green Circular Send FAB Button
                        val isSendEnabled = newMessageText.isNotBlank()
                        FloatingActionButton(
                            onClick = {
                                if (isSendEnabled) {
                                    sendMessageImpl(newMessageText)
                                }
                            },
                            containerColor = if (isSendEnabled) FamekoGreen else Color(0xFFE2E8F0),
                            contentColor = Color.White,
                            modifier = Modifier.size(46.dp),
                            shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(20.dp),
                                tint = if (isSendEnabled) Color.White else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Color.White)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            // TRIP INFORMATION STRIP
            CustomerTripInformationStrip(
                orderRef = orderRef,
                locationText = locationText,
                etaText = etaMinutes
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading && messages.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = FamekoGreen
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        // 1. Subtle Privacy Notice
                        item {
                            CustomerPrivacyNoticeCard()
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        var lastDateStr = ""

                        itemsIndexed(
                            items = messages,
                            key = { index, msg -> if (msg.id != 0) msg.id else "msg_${index}_${msg.createdAt}" }
                        ) { index, msg ->
                            val isMe = msg.senderType == "customer"

                            // Extract Date & Time
                            val (dateHeader, timeFormatted) = parseMessageTimestamp(msg.createdAt)

                            // Date Separator Pill
                            if (dateHeader.isNotEmpty() && dateHeader != lastDateStr) {
                                lastDateStr = dateHeader
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Surface(
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = dateHeader,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }
                            }

                            // Grouping check: Is same sender as previous message within short timeframe
                            val showAvatar = if (!isMe) {
                                val prevMsg = messages.getOrNull(index - 1)
                                prevMsg == null || prevMsg.senderType != "driver"
                            } else false

                            CustomerMessageBubbleItem(
                                message = msg,
                                isMe = isMe,
                                timeStr = timeFormatted,
                                showAvatar = showAvatar,
                                initials = driverInitials,
                                profilePic = driverProfilePic,
                                onRetry = { sendMessageImpl(msg.body, msg.id) }
                            )
                        }

                        // 2. Typing Indicator
                        if (isDriverTyping) {
                            item {
                                CustomerTypingIndicatorItem(driverName = currentDriverName)
                            }
                        }
                    }
                }
            }
        }
    }

    // Safety & Privacy Modal Sheet
    if (showSafetyModal) {
        CustomerSafetyPrivacyDialog(
            context = context,
            onDismiss = { showSafetyModal = false }
        )
    }
}

// ---------------------------------------------------------------------------
// TRIP INFORMATION STRIP
// ---------------------------------------------------------------------------
@Composable
private fun CustomerTripInformationStrip(
    orderRef: Int,
    locationText: String,
    etaText: String
) {
    Surface(
        color = LightMintBg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Order Tag Badge
            Surface(
                color = FamekoGreen,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "#$orderRef",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Location Info
            Text(
                text = locationText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = DarkText,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.width(8.dp))

            // ETA Capsule
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(0.5.dp, Color(0xFFCBD5E1))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.AccessTime,
                        contentDescription = "ETA",
                        tint = DarkText,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = etaText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkText
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// PRIVACY NOTICE CARD
// ---------------------------------------------------------------------------
@Composable
private fun CustomerPrivacyNoticeCard() {
    Surface(
        color = AmberNoticeBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AmberNoticeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = "Privacy Shield",
                tint = AmberNoticeText,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Phone numbers are masked to protect both driver & customer privacy.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = AmberNoticeText,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ---------------------------------------------------------------------------
// MESSAGE BUBBLE ITEM
// ---------------------------------------------------------------------------
@Composable
private fun CustomerMessageBubbleItem(
    message: Message,
    isMe: Boolean,
    timeStr: String,
    showAvatar: Boolean,
    initials: String,
    profilePic: String?,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Sender Avatar for incoming
            if (!isMe) {
                if (showAvatar) {
                    Box(modifier = Modifier.size(28.dp)) {
                        if (!profilePic.isNullOrBlank()) {
                            AsyncImage(
                                model = profilePic,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = initials,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(28.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Bubble container
            Surface(
                color = if (isMe) FamekoGreen else ChatBubbleIncoming,
                contentColor = if (isMe) Color.White else DarkText,
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMe) 18.dp else 4.dp,
                    bottomEnd = if (isMe) 4.dp else 18.dp
                ),
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                Text(
                    text = message.body,
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                )
            }
        }

        // Timestamp & Delivery status below bubble
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = if (!isMe) 36.dp else 0.dp,
                top = 2.dp,
                end = if (isMe) 4.dp else 0.dp
            )
        ) {
            if (timeStr.isNotEmpty()) {
                Text(
                    text = timeStr,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            // Message Status Indicators for Customer's sent messages
            if (isMe) {
                Spacer(modifier = Modifier.width(4.dp))
                when (message.status) {
                    "SENDING" -> {
                        Icon(
                            Icons.Outlined.AccessTime,
                            contentDescription = "Sending",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    "FAILED" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onRetry() }
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = "Failed",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "Failed. Tap to retry",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                    "READ", "DELIVERED" -> {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = "Delivered",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    else -> {
                        // SENT
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Sent",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// TYPING INDICATOR
// ---------------------------------------------------------------------------
@Composable
private fun CustomerTypingIndicatorItem(driverName: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing_dots")
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "dot1"
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 200), RepeatMode.Reverse), label = "dot2"
    )
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(600, delayMillis = 400), RepeatMode.Reverse), label = "dot3"
    )

    val firstName = driverName.split(" ").firstOrNull() ?: "Driver"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp, horizontal = 36.dp)
    ) {
        Surface(
            color = ChatBubbleIncoming,
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("●", fontSize = 10.sp, color = Color(0xFF64748B).copy(alpha = alpha1))
                Spacer(modifier = Modifier.width(3.dp))
                Text("●", fontSize = 10.sp, color = Color(0xFF64748B).copy(alpha = alpha2))
                Spacer(modifier = Modifier.width(3.dp))
                Text("●", fontSize = 10.sp, color = Color(0xFF64748B).copy(alpha = alpha3))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "$firstName is typing",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// QUICK REPLIES BAR
// ---------------------------------------------------------------------------
@Composable
private fun CustomerQuickRepliesBar(onSelectReply: (String) -> Unit) {
    val quickReplies = listOf(
        "⚡ Where are you?",
        "I'm waiting outside",
        "Standing near the entrance",
        "Please call when you arrive",
        "Thank you!"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        quickReplies.forEachIndexed { index, reply ->
            val isHighlighted = index == 0
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isHighlighted) Color(0xFFE6F4ED) else Color(0xFFF8FAFC),
                border = BorderStroke(
                    1.dp,
                    if (isHighlighted) Color(0xFF10B981) else Color(0xFFE2E8F0)
                ),
                modifier = Modifier.clickable { onSelectReply(reply) }
            ) {
                Text(
                    text = reply,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 13.sp,
                    fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
                    color = if (isHighlighted) Color(0xFF047857) else Color(0xFF334155)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// SAFETY & PRIVACY DIALOG MODAL
// ---------------------------------------------------------------------------
@Composable
private fun CustomerSafetyPrivacyDialog(
    context: Context,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = FamekoGreen)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Fameko Safety & Privacy", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "🔒 Number Masking Active",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkText
                )
                Text(
                    "Your personal phone number is hidden during active trips to protect your privacy.",
                    fontSize = 13.sp,
                    color = SubtitleGray
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Text(
                    "🚨 Emergency Support",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DarkText
                )
                Text(
                    "If you feel unsafe or need immediate assistance, contact Fameko 24/7 Shield or Ghana 112 Dispatch.",
                    fontSize = 13.sp,
                    color = SubtitleGray
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    PhoneNumberUtils.openDialer(context, "112", "Ghana Emergency")
                    onDismiss()
                }
            ) {
                Text("Call 112 Emergency", color = Color(0xFFDC2626), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = DarkText)
            }
        }
    )
}

// ---------------------------------------------------------------------------
// TIMESTAMP PARSER HELPER
// ---------------------------------------------------------------------------
private fun parseMessageTimestamp(createdAt: String): Pair<String, String> {
    if (createdAt.isBlank()) return "" to ""
    return try {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = isoFormat.parse(createdAt) ?: return "" to ""

        val todayStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date())
        val msgDateStr = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(date)

        val dateDisplay = if (todayStr == msgDateStr) "TODAY" else msgDateStr
        val timeDisplay = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)

        dateDisplay to timeDisplay
    } catch (_: Exception) {
        "" to ""
    }
}
