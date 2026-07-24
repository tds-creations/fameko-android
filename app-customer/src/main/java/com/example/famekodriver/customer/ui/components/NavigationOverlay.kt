package com.example.famekodriver.customer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.domain.model.RouteInstruction
import com.example.famekodriver.core.utils.LocationUtils
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltGreen
import com.example.famekodriver.customer.ui.theme.FamekoBlue
import org.maplibre.android.geometry.LatLng
import java.text.SimpleDateFormat
import java.util.*

@Suppress("DEPRECATION")
@Composable
fun NavigationOverlay(
    instruction: RouteInstruction?,
    currentLatLng: LatLng?,
    distanceKm: Double,
    durationMin: Double,
    onExit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Instruction Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            shape = RoundedCornerShape(16.dp),
            color = BoltDark, // Use a dark high-contrast background for navigation
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Icon (Placeholder logic)
                val icon = when {
                    instruction?.text?.contains("left", true) == true -> Icons.AutoMirrored.Filled.ArrowBack
                    instruction?.text?.contains("right", true) == true -> Icons.AutoMirrored.Filled.ArrowForward
                    else -> Icons.Default.Navigation
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = instruction?.text ?: "Follow the path",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    val distanceToManeuver = if (instruction != null && currentLatLng != null) {
                        val instrLat = instruction.point?.getOrNull(1) ?: 0.0
                        val instrLng = instruction.point?.getOrNull(0) ?: 0.0
                        val dist = LocationUtils.calculateDistance(
                            currentLatLng.latitude,
                            currentLatLng.longitude,
                            instrLat,
                            instrLng
                        )
                        if (dist > 1000) String.format(Locale.US, "%.1f km", dist / 1000.0)
                        else "${dist.toInt()} m"
                    } else "..."

                    Text(
                        text = "Next turn in $distanceToManeuver",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Bottom Stats and Exit
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", distanceKm)} km",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = BoltDark
                    )
                    Text(
                        text = "REMAINING",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val arrivalTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(System.currentTimeMillis() + (durationMin * 60000).toLong()))
                    Text(
                        text = arrivalTime,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = FamekoBlue
                    )
                    Text(
                        text = "ARRIVAL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${durationMin.toInt()} min",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = BoltGreen
                    )
                    Text(
                        text = "ESTIMATED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Icon(Icons.Default.Close, null)
                Spacer(Modifier.width(12.dp))
                Text("Exit Navigation", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
