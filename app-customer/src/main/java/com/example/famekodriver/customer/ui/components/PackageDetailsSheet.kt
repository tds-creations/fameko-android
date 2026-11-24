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
            // --- STAGE 1: Intent Modal ("Send" vs "Receive") ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
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

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Send Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .clickable {
                                onIsSendingChange(true)
                                onNextStage()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("📦", fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Send a package", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }

                    // Receive Card
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(115.dp)
                            .clickable {
                                onIsSendingChange(false)
                                onNextStage()
                            },
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFF1F5F9),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("🎁", fontSize = 34.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Receive a package", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(72.dp))
            }
        }



        4 -> {
            // --- STAGE 4: Contact Details (Recipient & Sender Info) ---
            val isDetailsComplete = recipientName.trim().isNotEmpty() && recipientPhone.trim().isNotEmpty()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
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
                        Text(if (isSending) "Recipient Contact Details" else "Sender Contact Details", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                        Text("Enter contact details for package handoff", fontSize = 12.sp, color = Color(0xFF64748B))
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

                // Continue Button
                Button(
                    onClick = onNextStage,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DispatchGreen),
                    enabled = isDetailsComplete
                ) {
                    Text("Continue to Vehicle Selection", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        else -> {
            // --- STAGE 5: Complete Delivery Review & Confirmation Summary ---
            val selectedEst = rideEstimates.find { it.serviceId.equals(category, true) || it.name.equals(category, true) } ?: rideEstimates.firstOrNull()
            val finalFare = selectedEst?.fare ?: basePrice
            val origFare = selectedEst?.originalFare ?: basePrice
            val discAmount = selectedEst?.discountAmount ?: 0.0

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onPrevStage) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                        }
                        Text("Review Delivery Order", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                    }
                    Surface(
                        color = DispatchLightGreen,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (isSending) "📦 SEND" else "🎁 RECEIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DispatchGreen,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Route Summary Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = DispatchGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isSending) "Pickup Spot" else "Collection Spot", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(pickupAddress.ifEmpty { "Pickup Spot" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(if (isSending) "Delivery Spot" else "Destination", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Text(dropOffAddress.ifEmpty { "Dropoff Spot" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF0F172A), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Details Summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isSending) "Recipient Contact:" else "Sender Contact:", fontSize = 12.sp, color = Color.Gray)
                            Text("$recipientName • $recipientPhone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Vehicle Category:", fontSize = 12.sp, color = Color.Gray)
                            Text(category, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DispatchGreen)
                        }
                        if (isFragile || isOtpRequired) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Security Options:", fontSize = 12.sp, color = Color.Gray)
                                Text(
                                    listOfNotNull(
                                        if (isOtpRequired) "4-Digit OTP" else null,
                                        if (isFragile) "Fragile Item" else null
                                    ).joinToString(" • "),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Fare Summary
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = DispatchLightGreen,
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Total Estimated Fare", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            if (discAmount > 0.0) {
                                Text("Discount savings applied", fontSize = 10.sp, color = Color(0xFF047857))
                            }
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (discAmount > 0.0) {
                                Text(
                                    com.example.famekodriver.core.utils.CurrencyUtils.formatCedi(origFare),
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    style = androidx.compose.ui.text.TextStyle(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough)
                                )
                            }
                            Text(
                                com.example.famekodriver.core.utils.CurrencyUtils.formatCedi(finalFare),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = DispatchGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm & Place Order Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DispatchGreen),
                    enabled = !isPlacing
                ) {
                    if (isPlacing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Confirm & Place Order", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
