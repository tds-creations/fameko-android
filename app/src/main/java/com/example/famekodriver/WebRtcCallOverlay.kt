package com.example.famekodriver

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.network.CallState
import com.example.famekodriver.core.network.WebRtcCallManager

@Composable
fun WebRtcCallOverlay(
    callState: CallState,
    callManager: WebRtcCallManager
) {
    if (callState is CallState.Idle) return

    var isMuted by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = callState !is CallState.Idle,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 16.dp, end = 16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                elevation = CardDefaults.cardElevation(8.dp),
                border = BorderStroke(1.5.dp, Color(0xFF2563EB))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (callState) {
                        is CallState.Incoming -> {
                            Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Call, null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                    Text("INCOMING VOICE CALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }

                            Text(callState.callerName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                            Text("Customer on Order #${callState.orderId}", fontSize = 12.sp, color = Color.LightGray)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Button(
                                    onClick = { callManager.rejectCall(callState.callId) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Decline", fontWeight = FontWeight.Bold)
                                    }
                                }

                                Button(
                                    onClick = { callManager.acceptCall(callState.callId) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                        Text("Accept", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        is CallState.Outgoing -> {
                            Surface(color = Color(0xFF1E293B), shape = RoundedCornerShape(12.dp)) {
                                Text("OUTGOING CALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                            }

                            Text(callState.targetName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconButton(
                                    onClick = { isMuted = callManager.toggleMute() }
                                ) {
                                    Surface(shape = CircleShape, color = if (isMuted) Color(0xFFDC2626) else Color(0xFF1E293B), modifier = Modifier.size(44.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { callManager.endCall() }
                                ) {
                                    Surface(shape = CircleShape, color = Color(0xFFDC2626), modifier = Modifier.size(44.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }

                        is CallState.Connected -> {
                            Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text("HD VOICE CONNECTED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }

                            Text(callState.targetName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)

                            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                                IconButton(
                                    onClick = { isMuted = callManager.toggleMute() }
                                ) {
                                    Surface(shape = CircleShape, color = if (isMuted) Color(0xFFDC2626) else Color(0xFF1E293B), modifier = Modifier.size(48.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { callManager.endCall() }
                                ) {
                                    Surface(shape = CircleShape, color = Color(0xFFDC2626), modifier = Modifier.size(48.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CallEnd, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        }
                                    }
                                }
                            }
                        }

                        is CallState.Ended -> {
                            Text(callState.reason, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.LightGray)
                        }

                        else -> {}
                    }
                }
            }
        }
    }
}
