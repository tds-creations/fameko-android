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
import androidx.compose.ui.graphics.Color
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
fun RadarPulseAnimation() {
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
    
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(800.dp)) {
            drawCircle(
                color = BoltGreen,
                radius = radius.dp.toPx(),
                alpha = alpha
            )
        }
        
        // Inner core
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Surface(shape = CircleShape, color = BoltGreen, modifier = Modifier.size(16.dp)) {}
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
