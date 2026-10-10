package com.example.famekodriver.customer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.repository.OrderRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromotionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repository = remember { OrderRepository() }
    val focusManager = LocalFocusManager.current
    var promoCodeInput by remember { mutableStateOf("") }
    var promos by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        repository.getPromotions().onSuccess {
            promos = it
            isLoading = false
        }.onFailure {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Header Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onBack) {
                                Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Promotions", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF0F172A))
                                Text("Ghana Special Offers", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        IconButton(onClick = { Toast.makeText(context, "Promotion Rules & Terms", Toast.LENGTH_SHORT).show() }) {
                            Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(40.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = "Help", tint = Color(0xFF0F172A), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }

                // Promo Voucher Input Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("HAVE A PROMO VOUCHER?", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 1.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = promoCodeInput,
                                    onValueChange = { promoCodeInput = it },
                                    placeholder = { Text("E.G. ACCRA50, RIDEFREE", fontSize = 12.sp, color = Color.Gray) },
                                    leadingIcon = { Icon(Icons.Default.ConfirmationNumber, null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFFF8FAFC),
                                        unfocusedContainerColor = Color(0xFFF8FAFC),
                                        focusedBorderColor = Color(0xFF10B981),
                                        unfocusedBorderColor = Color(0xFFE2E8F0)
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (promoCodeInput.isNotBlank()) {
                                            Toast.makeText(context, "Promo code $promoCodeInput applied!", Toast.LENGTH_SHORT).show()
                                            promoCodeInput = ""
                                            focusManager.clearFocus()
                                        } else {
                                            Toast.makeText(context, "Please enter a valid promo code", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.height(52.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Apply", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }

                // Section: ACTIVE DEALS (2)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                            Text("ACTIVE DEALS (${if (promos.isNotEmpty()) promos.size else 2})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A), letterSpacing = 1.sp)
                        }
                        Text("Auto-applied", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                    }
                }

                // Promo Item 1
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFECFDF5),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.Percent, null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("50% OFF Next 3 Rides", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                                        Text("Up to GH₵ 15 discount on Accra trips", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("ACTIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Schedule, null, tint = Color(0xFFD97706), modifier = Modifier.size(14.dp))
                                    Text("Expires in 3 days", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                                }

                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.Check, null, tint = Color(0xFF059669), modifier = Modifier.size(12.dp))
                                        Text("Applied to checkout", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                                    }
                                }
                            }
                        }
                    }
                }

                // Promo Item 2
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFEF3C7),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Default.AccountBalanceWallet, null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Column {
                                        Text("MoMo Cashback Special", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                                        Text("Get GH₵ 5 back when paying with MTN MoMo", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }

                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("READY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }

                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                    Text("Valid till 31 Oct", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                }

                                Surface(
                                    onClick = { Toast.makeText(context, "Code MOMO5 copied!", Toast.LENGTH_SHORT).show() },
                                    color = Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Code: MOMO5", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Icon(Icons.Default.ContentCopy, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Referral Hero Card: Invite Friends to Fameko
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A192F)),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("🎁 Give GH₵ 20, Get GH₵ 20", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF34D399), modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.1f),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF34D399), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            Text("Invite Friends to Fameko", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color.White)
                            Text("Share your personal invite code and earn free rides as soon as they complete their first trip.", fontSize = 12.sp, color = Color.LightGray, lineHeight = 16.sp)

                            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                                ) {
                                    Text("CODE:  FAMEKO - JOEL", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp))
                                }

                                Button(
                                    onClick = {
                                        val sendIntent: android.content.Intent = android.content.Intent().apply {
                                            action = android.content.Intent.ACTION_SEND
                                            putExtra(android.content.Intent.EXTRA_TEXT, "Use my Fameko code FAMEKO-JOEL for GH₵ 20 off your first ride! Download app now.")
                                            type = "text/plain"
                                        }
                                        val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                        context.startActivity(shareIntent)
                                    },
                                    modifier = Modifier.height(48.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Text("Share", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Links
                item {
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.clickable { Toast.makeText(context, "Opening Expired & Used Vouchers...", Toast.LENGTH_SHORT).show() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.History, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                            Text("View Expired & Used Vouchers", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF2563EB))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Promotion Terms", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { Toast.makeText(context, "Promotion Terms", Toast.LENGTH_SHORT).show() })
                            Text("•", fontSize = 11.sp, color = Color.Gray)
                            Text("Contact Rider Support", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { Toast.makeText(context, "Rider Support", Toast.LENGTH_SHORT).show() })
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
