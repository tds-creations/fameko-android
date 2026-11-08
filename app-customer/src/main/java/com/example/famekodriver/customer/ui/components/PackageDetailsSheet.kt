package com.example.famekodriver.customer.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.ui.theme.*

// Brand Colors
private val DispatchGreen = Color(0xFF006847)
private val DispatchLightGreen = Color(0xFFE8F5E9)
private val TextGreen = Color(0xFF00875A)
private val DarkEstimateBg = Color(0xFF0F172A)
private val CardBorderColor = Color(0xFFE2E8F0)

data class PackageCategoryItem(
    val id: String,
    val title: String,
    val weight: String,
    val description: String,
    val icon: String
)

@Composable
fun PackageDetailsSheet(
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
    category: String = "Small Parcel",
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
    selectedPaymentMethod: String = "MTN MoMo",
    onPaymentMethodChange: (String) -> Unit = {},
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    isPlacing: Boolean = false
) {
    val scrollState = rememberScrollState()
    var showPaymentPicker by remember { mutableStateOf(false) }

    val isDetailsComplete = remember(recipientName, recipientPhone, pickupAddress, dropOffAddress) {
        recipientName.trim().isNotEmpty() &&
        recipientPhone.trim().isNotEmpty() &&
        pickupAddress.trim().isNotEmpty() &&
        dropOffAddress.trim().isNotEmpty()
    }

    val displayFareValue = if (estimatedFare > 0.0) estimatedFare else 15.00

    val actionButtonText = when {
        !isDetailsComplete -> "Complete Details to Continue"
        isSending -> "Find Courier • GH₵ ${String.format(java.util.Locale.US, "%.2f", displayFareValue)}"
        else -> "Request Pickup • GH₵ ${String.format(java.util.Locale.US, "%.2f", displayFareValue)}"
    }

    val isButtonEnabled = isDetailsComplete && !isPlacing

    val categories = listOf(
        PackageCategoryItem("Document", "Document", "≤ 1kg", "Passports, contracts", "✉️"),
        PackageCategoryItem("Small Parcel", "Small Parcel", "≤ 5kg", "Food, clothes, items", "📦"),
        PackageCategoryItem("Medium Box", "Medium Box", "≤ 15kg", "Supplies, cartons", "📦"),
        PackageCategoryItem("Aboboyaa Cargo", "Aboboyaa Cargo", "> 15kg", "Heavy tricycle cargo", "🛺")
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // --- 1. Top Bar ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isSending) "Send a Package" else "Receive a Package",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )
            }

            Surface(
                shape = CircleShape,
                color = DispatchGreen,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onProfileClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 2. Segmented Switcher (Send / Receive) ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFE2E8F0)
        ) {
            Row(
                modifier = Modifier
                    .padding(3.dp)
                    .fillMaxWidth()
            ) {
                // Send Tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { onIsSendingChange(true) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSending) Color.White else Color.Transparent,
                    shadowElevation = if (isSending) 2.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "I'm Sending",
                            fontSize = 13.sp,
                            fontWeight = if (isSending) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSending) DispatchGreen else Color(0xFF64748B)
                        )
                    }
                }

                // Receive Tab
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { onIsSendingChange(false) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (!isSending) Color.White else Color.Transparent,
                    shadowElevation = if (!isSending) 2.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "I'm Receiving",
                            fontSize = 13.sp,
                            fontWeight = if (!isSending) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isSending) DispatchGreen else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Unified Route Timeline Card ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Pickup Location Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangePickup() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(DispatchGreen)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (isSending) "Pickup (You)" else "Pickup (Sender)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = DispatchGreen
                            )
                            if (pickupNamePhone.isNotBlank()) {
                                Text("• $pickupNamePhone", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                            }
                        }
                        Text(
                            if (pickupAddress.isNotEmpty()) pickupAddress else "Select Pickup Location",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                    }
                    Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DispatchGreen)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // Dropoff Location Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onChangeDropOff() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                if (isSending) "Drop-off (Recipient)" else "Drop-off (You)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF4444)
                            )
                            if (ghanaPostGps.isNotBlank()) {
                                Text("• GPS: $ghanaPostGps", fontSize = 10.sp, color = TextGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            if (dropOffAddress.isNotEmpty()) dropOffAddress else "Select Drop-off Location",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1
                        )
                    }
                    Text("Change", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DispatchGreen)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // Contact Details Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (isSending) "Recipient Contact" else "Sender Contact",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onChooseFromContacts() }
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = null, tint = TextGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGreen)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

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
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Package Size Category ---
        Text(
            "Package Size",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val categoryPairs = categories.chunked(2)
        categoryPairs.forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { cat ->
                    val isSelected = category == cat.title
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(68.dp)
                            .clickable {
                                onCategoryChange(cat.title)
                                onWeightSizeChange(cat.weight)
                            },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) DispatchLightGreen else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) DispatchGreen else CardBorderColor
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(cat.icon, fontSize = 22.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    cat.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    cat.weight,
                                    fontSize = 11.sp,
                                    color = if (isSelected) DispatchGreen else Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 5. Delivery Options Card ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Secure OTP Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOtpRequiredChange(!isOtpRequired) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("4-Digit Secure OTP Handoff", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Rider verifies 4-digit code before releasing cargo", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isOtpRequired,
                        onCheckedChange = onOtpRequiredChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DispatchGreen)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                // Fragile Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFragileChange(!isFragile) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fragile Item", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Handle glass, electronics or delicate goods with care", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Checkbox(
                        checked = isFragile,
                        onCheckedChange = onFragileChange,
                        colors = CheckboxDefaults.colors(checkedColor = DispatchGreen)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                // SMS/WhatsApp Notification Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNotifyRecipientChange(!notifyRecipientSmsWhatsapp) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SMS / WhatsApp Tracking Link", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Auto-shares live courier tracking link with recipient", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = notifyRecipientSmsWhatsapp,
                        onCheckedChange = onNotifyRecipientChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = DispatchGreen)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 10.dp))

                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Special instructions for courier (Optional)...", color = Color.Gray, fontSize = 12.sp) },
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DispatchGreen,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 6. Bottom Price & Action Container ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = DarkEstimateBg
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "GH₵ ${String.format(java.util.Locale.US, "%.2f", displayFareValue)}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            "Est. Transit: ${estimatedTime.ifEmpty { "15-20 mins" }} ${if (estimatedDistance.isNotBlank()) "($estimatedDistance)" else ""}",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Payment Method Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { showPaymentPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                selectedPaymentMethod,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Change payment method", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DispatchGreen,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    enabled = isButtonEnabled
                ) {
                    if (isPlacing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                actionButtonText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isButtonEnabled) Color.White else Color.LightGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (isButtonEnabled) Color.White else Color.LightGray, modifier = Modifier.size(18.dp))
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
                            "MTN MoMo" to "📱 Mobile Money",
                            "Telecel Cash" to "📱 Mobile Money",
                            "AT Money" to "📱 Mobile Money",
                            "Credit / Debit Card" to "💳 Card",
                            "Cash on Delivery" to "💵 Cash"
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
                                border = androidx.compose.foundation.BorderStroke(
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

        Spacer(modifier = Modifier.height(20.dp))
    }
}
