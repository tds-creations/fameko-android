package com.example.famekodriver.customer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.domain.model.FamekoEvent
import com.example.famekodriver.customer.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.time.Duration.Companion.seconds

@Composable
fun TripSummaryDialog(
    fare: Double,
    onRate: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trip Completed", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CheckCircle, null, tint = BoltGreen, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Total Fare", fontSize = 16.sp, color = Color.Gray)
                Text("₵${fare.toInt()}", fontSize = 36.sp, fontWeight = FontWeight.Black, color = BoltDark)
                Spacer(Modifier.height(8.dp))
                Text("Payment completed via cash/wallet", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onRate,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Rate Driver", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}


@Composable
fun RatingDialog(
    driverName: String,
    driverProfilePic: String? = null,
    onRate: (Float, String) -> Unit,
    onDismiss: () -> Unit
) {
    var rating by remember { mutableFloatStateOf(0f) }
    var comment by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = BoltLightGray, modifier = Modifier.size(80.dp)) {
                    if (!driverProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = driverProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Rate your trip", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                Text("How was your ride with $driverName?", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in 1..5) {
                        val isSelected = rating >= i
                        IconButton(onClick = { rating = i.toFloat() }) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (isSelected) BoltYellow else Color.Gray,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    placeholder = { Text("Write a comment (optional)", fontSize = 14.sp) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onRate(rating, comment) },
                enabled = rating > 0,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
            ) {
                Text("Submit Rating", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Maybe Later", color = Color.Gray)
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White
    )
}


@Composable
fun RadarPulseAnimation(
    modifier: Modifier = Modifier,
    centerOffset: Offset? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val radius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )
    
    val boxModifier = if (centerOffset != null) {
        modifier.graphicsLayer {
            translationX = centerOffset.x - (size.width / 2f)
            translationY = centerOffset.y - (size.height / 2f)
        }
    } else {
        modifier.fillMaxSize()
    }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(800.dp)) {
            drawCircle(
                color = BoltGreen,
                radius = radius.dp.toPx(),
                alpha = alpha
            )
        }
    }
}

@Composable
fun MapCenterPin(
    isMoving: Boolean,
    targetMode: com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget = com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP,
    modifier: Modifier = Modifier
) {
    val pinYOffset by animateDpAsState(
        targetValue = if (isMoving) (-16).dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pinBounce"
    )

    val shadowScale by animateFloatAsState(
        targetValue = if (isMoving) 0.6f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "shadowScale"
    )

    val pinColor = when (targetMode) {
        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP -> BoltGreen
        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.DROPOFF -> BoltOrange
        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.STOP -> FamekoBlue
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Ground Shadow (fixed at map center)
        Box(
            modifier = Modifier
                .offset(y = 22.dp)
                .size(width = (24 * shadowScale).dp, height = (8 * shadowScale).dp)
                .background(Color.Black.copy(alpha = 0.25f * shadowScale), CircleShape)
        )

        // Floating Drop Pin (lifts on map drag, spring-bounces down on camera idle)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = pinYOffset - 18.dp)
        ) {
            // Top Pin Head
            Surface(
                shape = CircleShape,
                color = pinColor,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (targetMode == com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP) Icons.Default.MyLocation else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            // Pin Stem
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .background(pinColor)
            )
        }
    }
}

@Composable
fun MapPickerOverlay(
    viewModel: com.example.famekodriver.customer.CustomerMapViewModel,
    isCameraMoving: Boolean,
    onRecenter: () -> Unit,
    onBack: () -> Unit
) {
    val target = viewModel.mapPickerTarget
    val titleText = when (target) {
        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP -> "Pick Pickup Location"
        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.DROPOFF -> "Pick Destination"
        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.STOP -> "Pick Stop Location"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fixed Center Pin with spring bounce animation
        MapCenterPin(
            isMoving = isCameraMoving,
            targetMode = target
        )

        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FloatingActionButton(
                onClick = onBack,
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A),
                shape = CircleShape,
                modifier = Modifier.size(44.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
            }

            Spacer(Modifier.width(12.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.weight(1f).height(44.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                if (target == com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP) BoltGreen else BoltOrange,
                                CircleShape
                            )
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Floating Action Buttons (Recenter / Current Location)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 220.dp)
        ) {
            FloatingActionButton(
                onClick = onRecenter,
                containerColor = Color.White,
                contentColor = Color(0xFF0F172A),
                shape = CircleShape,
                modifier = Modifier.size(48.dp),
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", modifier = Modifier.size(20.dp))
            }
        }

        // Bottom Confirmation Sheet
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 24.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                // Drag handle indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(36.dp)
                        .height(4.dp)
                        .background(Color.LightGray.copy(alpha = 0.6f), CircleShape)
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = when (target) {
                        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP -> "CONFIRM PICKUP LOCATION"
                        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.DROPOFF -> "CONFIRM DESTINATION"
                        com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.STOP -> "CONFIRM STOP LOCATION"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = FamekoBlue,
                    letterSpacing = 1.sp
                )

                Spacer(Modifier.height(6.dp))

                val address = viewModel.mapPickerAddress
                val displayTitle = if (address.contains(",")) address.split(",").first() else address.ifBlank { "Setting location..." }
                val displaySubtitle = if (address.contains(",")) address.substringAfter(",").trim() else ""

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = if (target == com.example.famekodriver.customer.CustomerMapViewModel.MapPickerTarget.PICKUP) BoltGreen else BoltOrange,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = BoltDark,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        if (displaySubtitle.isNotEmpty()) {
                            Text(
                                text = displaySubtitle,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (viewModel.isReverseGeocodingMapPicker) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = FamekoBlue,
                        trackColor = FamekoBlue.copy(alpha = 0.15f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = { viewModel.confirmMapPickerLocation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Confirm Location",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TimedOutSheetContent(onRetry: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Icon(Icons.Default.Error, null, tint = BoltOrange, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("No Driver Found", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Sorry, all drivers are currently busy in your area. Please try again in a few minutes.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
            ) {
                Text("Try Again", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ScheduledRideSheetContent(onCancel: () -> Unit, onClose: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Icon(Icons.Default.CheckCircle, null, tint = BoltGreen, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text("Ride Scheduled!", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        Text("Your driver will be assigned closer to your pickup time.", color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Cancel Ride", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onClose,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BoltDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CallOverlay(call: FamekoEvent.IncomingCall, onEnd: () -> Unit) {
    var callDuration by remember { mutableIntStateOf(0) }
    val isConnecting = call.callId == "pending"
    LaunchedEffect(isConnecting) { if (!isConnecting) { while (true) { delay(1.seconds); callDuration++ } } }
    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.95f)).padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(64.dp))
            Surface(shape = CircleShape, color = BoltGreen.copy(alpha = 0.2f), modifier = Modifier.size(140.dp)) { 
                Box(contentAlignment = Alignment.Center) { 
                    if (!isConnecting) { 
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val scale by infiniteTransition.animateFloat(initialValue = 1f, targetValue = 1.2f, animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "")
                        Surface(shape = CircleShape, color = BoltGreen.copy(alpha = 0.1f), modifier = Modifier.size(140.dp * scale)) {} 
                    }
                    Icon(Icons.Default.Person, null, tint = Color.White, modifier = Modifier.size(80.dp)) 
                } 
            }
            Spacer(modifier = Modifier.height(32.dp))
            Text(call.callerName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(if (isConnecting) "Connecting..." else { val mins = callDuration / 60; val secs = callDuration % 60; "In-app Call • %02d:%02d".format(Locale.US, mins, secs) }, color = if (isConnecting) Color.Gray else BoltGreen, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 48.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) { 
                Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                    IconButton(onClick = { }, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.Default.MicOff, null, tint = Color.White) }
                    Text("Mute", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) 
                }
                FloatingActionButton(onClick = onEnd, containerColor = Color(0xFFDC3545), contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(80.dp)) { Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(36.dp)) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) { 
                    IconButton(onClick = { }, modifier = Modifier.size(56.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White) }
                    Text("Speaker", color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) 
                } 
            }
        }
    }
}

@Composable
fun QuickShortcutItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = CircleShape,
            color = BoltLightGray,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.padding(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
    }
}
