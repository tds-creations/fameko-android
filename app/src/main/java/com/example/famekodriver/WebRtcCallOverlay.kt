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
    var isSpeakerOn by remember { mutableStateOf(callManager.isSpeakerphoneOn) }

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
                elevation = CardDefaults.cardElevation(12.dp),
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

                        is CallState.Outgoing, is CallState.Connected -> {
                            val titleText = if (callState is CallState.Connected) "HD VOICE CONNECTED" else "OUTGOING CALL"
                            val targetName = if (callState is CallState.Connected) callState.targetName else (callState as CallState.Outgoing).targetName

                            Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text(titleText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                                }
                            }

                            Text(targetName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Mute Button
                                IconButton(onClick = { isMuted = callManager.toggleMute() }) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isMuted) Color(0xFFDC2626) else Color(0xFF1E293B),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(if (isMuted) Icons.Default.MicOff else Icons.Default.Mic, contentDescription = "Mute", tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }

                                // Speakerphone Button
                                IconButton(onClick = { isSpeakerOn = callManager.toggleSpeakerphone() }) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSpeakerOn) Color(0xFF2563EB) else Color(0xFF1E293B),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = "Speaker", tint = Color.White, modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }

                                // End Call Button
                                IconButton(onClick = { callManager.endCall() }) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFDC2626),
                                        modifier = Modifier.size(52.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White, modifier = Modifier.size(24.dp))
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
