package com.example.famekodriver.customer

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CustomerAgoraCallOverlay(
    callState: CustomerCallState,
    callManager: CustomerAgoraCallManager
) {
    if (callState is CustomerCallState.Idle) return

    val isMuted by callManager.isMicMuted.collectAsState()
    val isSpeakerOn by callManager.isSpeakerOn.collectAsState()

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
                    is CustomerCallState.Ringing -> {
                        Surface(color = Color(0xFF065F46), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Call, null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                Text("INCOMING DRIVER CALL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399))
                            }
                        }

                        Text(callState.callerName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                        Text("Order #${callState.orderId}", fontSize = 12.sp, color = Color.LightGray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { callManager.rejectCall(callState.channelName) },
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
                                onClick = { callManager.acceptIncomingCall(callState.channelName, callState.callerName, callState.orderId) },
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

                    is CustomerCallState.Dialing, is CustomerCallState.Active -> {
                        val isLive = callState is CustomerCallState.Active
                        val remoteName = if (isLive) (callState as CustomerCallState.Active).remoteName else (callState as CustomerCallState.Dialing).recipientName
                        val statusText = if (isLive) "CUSTOMER HD VOICE CONNECTED" else "DIALING DRIVER..."

                        Surface(color = if (isLive) Color(0xFF065F46) else Color(0xFF1E293B), shape = RoundedCornerShape(12.dp)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isLive) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                }
                                Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isLive) Color(0xFF34D399) else Color(0xFF38BDF8))
                            }
                        }

                        Text(remoteName, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute Button
                            IconButton(onClick = { callManager.toggleMicrophone() }) {
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
                            IconButton(onClick = { callManager.toggleSpeaker() }) {
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSpeakerOn) Color(0xFF2563EB) else Color(0xFF1E293B),
                                    modifier = Modifier.size(52.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff, contentDescription = "Speaker", tint = Color.White, modifier = Modifier.size(24.dp))
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

                    is CustomerCallState.Ended -> {
                        Text(callState.reason, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.LightGray)
                    }

                    else -> {}
                }
            }
        }
    }
}
