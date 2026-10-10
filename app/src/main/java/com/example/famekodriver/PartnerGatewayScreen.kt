package com.example.famekodriver

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun PartnerGatewayScreen(
    onSelectDriverPortal: () -> Unit,
    onSelectFleetConsole: () -> Unit,
    onRegisterNewPartner: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Pill & Brand Logo Title
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF2563EB), CircleShape))
                            Text("SINGLE APP • DUAL ECOSYSTEM", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF), letterSpacing = 0.5.sp)
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF059669),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Fameko ", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF0F172A))
                                Text("Mobility", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF059669))
                            }
                            Text("Greater Accra Partner Gateway", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                    }

                    Text(
                        "Select your role to access your dedicated operations portal, vehicle telemetry, and instant settlements.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Card 1: Driver Partner Gateway
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectDriverPortal() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    elevation = CardDefaults.cardElevation(2.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF059669), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.SportsScore, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Column {
                                    Text("Driver Partner", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF065F46))
                                    Text("Street & Transit Operations", fontSize = 11.sp, color = Color(0xFF047857))
                                }
                            }

                            Surface(color = Color(0xFFA7F3D0), shape = RoundedCornerShape(12.dp)) {
                                Text("Active Shifts", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46), modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        // Banner Image Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?w=800",
                                contentDescription = "Driver Partner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Payments, null, tint = Color(0xFF34D399), modifier = Modifier.size(12.dp))
                                    Text("Instant MoMo Daily Cashout Ready", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Text(
                            "For active drivers on the road. Track live shifts, instant Mobile Money earnings, with turn-by-turn navigation in English & Twi audio.",
                            fontSize = 12.sp,
                            color = Color(0xFF047857),
                            lineHeight = 16.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                    Text("Ghana Card Linked", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                                }
                            }

                            Surface(color = Color.White, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFA7F3D0))) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Bolt, null, tint = Color(0xFF2563EB), modifier = Modifier.size(12.dp))
                                    Text("Zero Fuel Downtime", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E40AF))
                                }
                            }
                        }

                        Button(
                            onClick = onSelectDriverPortal,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("Log In as Driver", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Card 2: Fleet Owner Enterprise Gateway
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFleetConsole() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFF2563EB), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Domain, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                    }
                                }
                                Column {
                                    Text("Fleet Owner", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                                    Text("Asset & Escrow Governance", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }

                            Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp)) {
                                Text("Enterprise", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                        }

                        // Banner Image Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(16.dp))
                        ) {
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1551836022-d5d88e9218df?w=800",
                                contentDescription = "Fleet Operations",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF34D399), CircleShape))
                                    Text("Multi-Vehicle Live Telemetry Active", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Text(
                            "For vehicle owners & fleet managers. Multi-car live telemetry, automated 70/30 split ledger, and proactive DVLA roadworthy compliance alerts.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            lineHeight = 16.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Wifi, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    Text("Live DVLA Tracking", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }

                            Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AccountBalance, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                                    Text("Bank & MoMo Escrow", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }

                        Button(
                            onClick = onSelectFleetConsole,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Text("Log In as Fleet Owner", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Dual Profile Banner
            item {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(shape = CircleShape, color = Color(0xFF2563EB).copy(alpha = 0.15f), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.SwapHoriz, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            }
                        }
                        Column {
                            Text("Dual Profile Support Enabled", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0F172A))
                            Text("Drivers who also own assets can toggle mode in app settings.", fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                }
            }

            // Registration Link
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("New to Fameko Partner Platform?", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)

                    Surface(
                        onClick = onRegisterNewPartner,
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFF2563EB)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddCircleOutline, null, tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Register New Vehicle or Driver License", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2563EB))
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Text("REGULATED BY DVLA GHANA • GRA WITHHOLDING TAX COMPLIANT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                    Text("Gateway Build 4.12.0 (Accra West Cluster)", fontSize = 9.sp, color = Color.LightGray)
                }
            }
        }
    }
}
