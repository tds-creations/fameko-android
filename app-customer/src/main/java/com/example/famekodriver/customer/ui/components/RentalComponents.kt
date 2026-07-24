package com.example.famekodriver.customer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.ui.theme.*

@Composable
fun ActiveRentalSheetContent(
    rental: Map<String, Any>,
    viewModel: CustomerMapViewModel,
    onDetailsClick: () -> Unit,
    onStartNavigation: () -> Unit
) {
    val isSelfDrive = rental["is_self_drive"] == true || rental["is_self_drive"] == "true"
    val bookingCode = rental["booking_code"]?.toString() ?: "----"
    
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (isSelfDrive) "Self-Drive Active" else "Active Rental", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = FamekoBlue)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDetailsClick) {
                Text("Details", fontWeight = FontWeight.Bold, color = FamekoBlue)
                Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(12.dp).padding(start = 4.dp), FamekoBlue)
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BoltLightGray),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(if (isSelfDrive) "VEHICLE ACCESS CODE" else "HANDSHAKE CODE", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(bookingCode, fontSize = 28.sp, fontWeight = FontWeight.Black, color = BoltDark, letterSpacing = 2.sp)
                Text(if (isSelfDrive) "Use this to unlock the vehicle or keybox" else "Give this to your driver to start the trip", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (viewModel.polylinePoints.isNotEmpty() && !viewModel.isFullscreenMap) {
                OutlinedButton(
                    onClick = { viewModel.clearDestination() },
                    modifier = Modifier.weight(0.8f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Icon(Icons.Default.Stop, null, tint = Color.Red)
                    Spacer(Modifier.width(8.dp))
                    Text("Clear", fontWeight = FontWeight.Bold)
                }
            }
            
            Button(
                onClick = {
                    if (viewModel.isFullscreenMap) {
                        viewModel.isFullscreenMap = false
                    } else {
                        onStartNavigation()
                    }
                },
                modifier = Modifier.weight(1.2f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isFullscreenMap) Color.Red else FamekoBlue
                )
            ) {
                val isNoRoute = viewModel.dropOffLocation.isEmpty()
                val icon = if (viewModel.isFullscreenMap) Icons.Default.Cancel else if (isNoRoute) Icons.Default.Search else Icons.Default.Navigation
                val label = if (viewModel.isFullscreenMap) "Stop Navigation" else if (isNoRoute) "Set Destination" else "Navigate"
                
                Icon(icon, null)
                Spacer(Modifier.width(12.dp))
                Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
