package com.example.famekodriver.customer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.ui.theme.*

// Brand Colors
private val DispatchGreen = Color(0xFF006847)
private val DispatchLightGreen = Color(0xFFE8F5E9)
private val TextGreen = Color(0xFF00875A)
private val CardBorderColor = Color(0xFFE2E8F0)

@Composable
fun PackageDetailsSheet(
    stage: Int = 1,
    onNextStage: () -> Unit = {},
    onPrevStage: () -> Unit = {},
    isSending: Boolean = true,
    onIsSendingChange: (Boolean) -> Unit = {},
    pickupNamePhone: String = "",
    pickupAddress: String = "",
    onPickupAddressChange: (String) -> Unit = {},
    onChangePickup: () -> Unit = {},
    recipientName: String = "",
    onRecipientNameChange: (String) -> Unit = {},
    recipientPhone: String = "",
    onRecipientPhoneChange: (String) -> Unit = {},
    onChooseFromContacts: () -> Unit = {},
    dropOffAddress: String = "",
    onDropOffAddressChange: (String) -> Unit = {},
    onChangeDropOff: () -> Unit = {},
    ghanaPostGps: String = "",
    notifyRecipientSmsWhatsapp: Boolean = true,
    onNotifyRecipientChange: (Boolean) -> Unit = {},
    category: String = "Send Motorbike",
    weightSize: String = "Small",
    onCategoryChange: (String) -> Unit = {},
    onWeightSizeChange: (String) -> Unit = {},
    isOtpRequired: Boolean = true,
    onOtpRequiredChange: (Boolean) -> Unit = {},
    isFragile: Boolean = false,
    onFragileChange: (Boolean) -> Unit = {},
    notes: String = "",
    onNotesChange: (String) -> Unit = {},
    estimatedFare: Double = 0.0,
    estimatedTime: String = "",
    estimatedDistance: String = "",
    discountRate: Int = 0,
    rideEstimates: List<com.example.famekodriver.core.domain.model.RideEstimateResponse> = emptyList(),
    selectedPaymentMethod: String = "Cash",
    onPaymentMethodChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    isPlacing: Boolean = false
) {
    var showPaymentPicker by remember { mutableStateOf(false) }

    val basePrice = if (estimatedFare > 0.0) estimatedFare else 19.0

    when (stage) {
        1 -> {
            // --- STAGE 1: Intent Modal ("Send" vs "Receive" - Screenshot 1) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fameko Send", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    IconButton(onClick = onBack) {
                        Surface(shape = CircleShape, color = Color(0xFFF1F5F9), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Do you want to send or receive items?", fontSize = 14.sp, color = Color(0xFF64748B))

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Send Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clickable {
                                onIsSendingChange(true)
                                onNextStage()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📦", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Send", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }

                    // Receive Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clickable {
                                onIsSendingChange(false)
                                onNextStage()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🎁", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Receive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        3 -> {
            // --- STAGE 3: Vehicle Category & Pricing Sheet (Screenshot 3) ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                // Top Promo Banner
                if (discountRate > 0) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F172A)
                    ) {
                        Box(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "⚡ $discountRate% promo applied",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Vehicle Options List
                val motorbikeFare = rideEstimates.find { it.serviceId.contains("Okada", true) }?.fare ?: basePrice
                val carFare = rideEstimates.find { it.serviceId.contains("Saloon", true) || it.serviceId.contains("Economy", true) || it.serviceId.contains("Car", true) }?.fare ?: (basePrice * 1.5)
                val cargoFare = rideEstimates.find { it.serviceId.contains("Aboboyaa", true) || it.serviceId.contains("Truck", true) }?.fare ?: (basePrice * 2.3)

                val options = listOf(
                    Triple("Send Motorbike", "For items that fit in a backpack", motorbikeFare),
                    Triple("Car Delivery", "For larger boxes & delicate packages", carFare),
                    Triple("Aboboyaa Cargo", "Heavy freight & commercial goods (>15kg)", cargoFare)
                )

                options.forEach { (title, desc, price) ->
                    val isSelected = category.contains(title, true) || (category.contains("Motorbike", true) && title.contains("Motorbike", true))
                    val finalDisplayPrice = if (discountRate > 0) price * (100 - discountRate) / 100 else price
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onCategoryChange(title)
                                onWeightSizeChange(if (title.contains("Motorbike")) "≤ 5kg" else if (title.contains("Car")) "≤ 15kg" else "> 15kg")
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) DispatchLightGreen else Color.White,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) DispatchGreen else CardBorderColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (title.contains("Motorbike")) "🛵" else if (title.contains("Car")) "🚗" else "🛺",
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(color = Color(0xFFECFDF5), shape = RoundedCornerShape(6.dp)) {
                                        Text(
                                            text = "${if (title.contains("Motorbike")) "8" else "12"} min",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = DispatchGreen,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Text("GH₵ ${String.format(java.util.Locale.US, "%.0f", finalDisplayPrice)}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFF0F172A))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Selector Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPaymentPicker = true }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = DispatchGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedPaymentMethod, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Continue Button
                Button(
                    onClick = onNextStage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DispatchGreen)
                ) {
                    Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        else -> {
            // --- STAGE 4: Recipient Details & Confirm Order Sheet (Screenshot 4) ---
            val isDetailsComplete = recipientName.trim().isNotEmpty() && recipientPhone.trim().isNotEmpty()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onPrevStage) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text("Confirm pickup spot", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        Text("Drag map or edit address to set pickup", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Vehicle & Fare Summary Pill
                val finalSummaryPrice = if (discountRate > 0) basePrice * (100 - discountRate) / 100 else basePrice
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(category, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                        Text("GH₵ ${String.format(java.util.Locale.US, "%.0f", finalSummaryPrice)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = DispatchGreen)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Contact Details Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isSending) "Recipient Contact" else "Sender Contact", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF475569))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onChooseFromContacts() }
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = TextGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGreen)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = recipientName,
                    onValueChange = onRecipientNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(if (isSending) "Recipient Full Name" else "Sender Full Name", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DispatchGreen,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = recipientPhone,
                    onValueChange = onRecipientPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Phone Number (e.g. 0244000000)", color = Color.Gray) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DispatchGreen,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Options (OTP & Fragile)
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onOtpRequiredChange(!isOtpRequired) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("4-Digit Secure OTP Handoff", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Switch(checked = isOtpRequired, onCheckedChange = onOtpRequiredChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DispatchGreen))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onFragileChange(!isFragile) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fragile Item", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    Checkbox(checked = isFragile, onCheckedChange = onFragileChange, colors = CheckboxDefaults.colors(checkedColor = DispatchGreen))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Order Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DispatchGreen),
                    enabled = isDetailsComplete && !isPlacing
                ) {
                    if (isPlacing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Confirm order", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (showPaymentPicker) {
        AlertDialog(
            onDismissRequest = { showPaymentPicker = false },
            title = { Text("Select Payment Method", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "Cash" to "💵 Cash Payment",
                        "MTN MoMo" to "📱 Mobile Money",
                        "Telecel Cash" to "📱 Mobile Money",
                        "AT Money" to "📱 Mobile Money",
                        "Credit / Debit Card" to "💳 Card"
                    ).forEach { (method, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPaymentMethodChange(method)
                                    showPaymentPicker = false
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedPaymentMethod == method) DispatchLightGreen else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (selectedPaymentMethod == method) DispatchGreen else Color(0xFFE2E8F0)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(method, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                                Text(desc, fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPaymentPicker = false }) {
                    Text("Close", color = DispatchGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
