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

// Brand Colors from Design
private val DispatchGreen = Color(0xFF006847)
private val DispatchLightGreen = Color(0xFFE8F5E9)
private val PillGreen = Color(0xFFD4F5E2)
private val TextGreen = Color(0xFF00875A)
private val DarkEstimateBg = Color(0xFF182232)
private val CardBorderColor = Color(0xFFE0E6ED)

data class PackageCategoryItem(
    val id: String,
    val title: String,
    val weight: String,
    val description: String,
    val icon: String // Emoji or visual representation
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

    val isDetailsComplete = remember(isSending, recipientName, recipientPhone, pickupAddress, dropOffAddress) {
        recipientName.trim().isNotEmpty() &&
        recipientPhone.trim().isNotEmpty() &&
        pickupAddress.trim().isNotEmpty() &&
        dropOffAddress.trim().isNotEmpty()
    }

    val displayFareValue = if (estimatedFare > 0.0) estimatedFare else 15.00

    val fareDisplayText = when {
        !isDetailsComplete -> "--"
        else -> "GH₵ ${String.format(java.util.Locale.US, "%.2f", displayFareValue)}"
    }

    val transitSubtext = when {
        !isDetailsComplete -> "Complete all details above to calculate fare"
        else -> "Est. Transit: ${estimatedTime.ifEmpty { "15-25 mins" }} (${estimatedDistance.ifEmpty { "5.2 km" }})"
    }

    val actionButtonText = when {
        !isDetailsComplete -> if (isSending) "Complete Details to Find Courier" else "Complete Details to Request Pickup"
        isSending -> "Find Nearby Courier • GH₵ ${String.format(java.util.Locale.US, "%.2f", displayFareValue)}"
        else -> "Request Package Pickup • GH₵ ${String.format(java.util.Locale.US, "%.2f", displayFareValue)}"
    }

    val isButtonEnabled = isDetailsComplete && !isPlacing

    val categories = listOf(
        PackageCategoryItem("Document", "Document", "≤ 1kg", "Passports, contracts,...", "✉️"),
        PackageCategoryItem("Small Parcel", "Small Parcel", "≤ 5kg", "Food, clothes, gadgets", "📦"),
        PackageCategoryItem("Medium Box", "Medium Box", "≤ 15kg", "Retail supplies, cartons", "📦"),
        PackageCategoryItem("Aboboyaa Cargo", "Aboboyaa Cargo", "> 15kg", "Motorized tricycle freight", "🛺")
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
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PillGreen,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(TextGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("FAMEKO LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGreen)
                    }
                }
                Text(
                    "Package Dispatch Setup",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1E293B)
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

        // --- 2. Real-Time Rush Banner ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = PillGreen
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(TextGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "ACCRA PARCEL RUSH • REAL-TIME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextGreen
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.FlashOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("~3 min dispatch", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 3. Headline ---
        Text(
            "Send or Receive Package",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            if (isSending) "Fast, end-to-end tracked courier dispatches across Greater Accra"
            else "Request a courier to pick up an item from sender and deliver to you",
            fontSize = 13.sp,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- 4. Sending / Receiving Toggle Segment ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // I am Sending Button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clickable { onIsSendingChange(true) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSending) Color.White else Color(0xFFF1F5F9),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSending) 2.dp else 1.dp,
                    color = if (isSending) DispatchGreen else CardBorderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isSending) DispatchLightGreen else Color(0xFFE2E8F0),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CardGiftcard,
                                contentDescription = null,
                                tint = if (isSending) DispatchGreen else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "I am Sending",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isSending) Color(0xFF0F172A) else Color(0xFF64748B)
                        )
                        Text(
                            "Handing item to courier",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // I am Receiving Button
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clickable { onIsSendingChange(false) },
                shape = RoundedCornerShape(16.dp),
                color = if (!isSending) Color.White else Color(0xFFF1F5F9),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (!isSending) 2.dp else 1.dp,
                    color = if (!isSending) DispatchGreen else CardBorderColor
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (!isSending) DispatchLightGreen else Color(0xFFE2E8F0),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CallReceived,
                                contentDescription = null,
                                tint = if (!isSending) DispatchGreen else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "I am Receiving",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (!isSending) Color(0xFF0F172A) else Color(0xFF64748B)
                        )
                        Text(
                            "Pickup from sender to me",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isSending) {
            // --- 5A. SENDING: Pickup Location (YOU) Card ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = DispatchGreen,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(top = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PICKUP LOCATION (YOU)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGreen
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onChangePickup() }
                            ) {
                                Text(
                                    "Change Pickup",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGreen
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TextGreen, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            if (pickupNamePhone.isNotEmpty()) pickupNamePhone else "Your Contact Info",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (pickupAddress.isNotEmpty()) pickupAddress else "Select Pickup Location",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 6A. SENDING: Recipient Information Card ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Recipient Information",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFE2E8F0)
                        ) {
                            Text(
                                "DESTINATION",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Recipient Full Name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = onRecipientNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. Abena Osei", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DispatchGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recipient Phone Number", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onChooseFromContacts() }
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = TextGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Choose from Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = onRecipientPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 0244000000", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DispatchGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Drop-off Address / Landmark", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = dropOffAddress,
                        onValueChange = onDropOffAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Enter landmark or drop-off location", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = onChangeDropOff) {
                                Icon(Icons.Default.Map, contentDescription = "Select on map", tint = TextGreen)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DispatchGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            val randomGps = "GA-${(100..999).random()}-${(1000..9999).random()}"
                            if (notes.isEmpty()) {
                                onNotesChange("GhanaPost GPS: $randomGps")
                            } else if (!notes.contains("GhanaPost GPS")) {
                                onNotesChange("$notes | GhanaPost GPS: $randomGps")
                            }
                        }
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TextGreen, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "GhanaPost GPS match: ${if (ghanaPostGps.isNotEmpty()) ghanaPostGps else "GA-342-9912"}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.clickable { onNotifyRecipientChange(!notifyRecipientSmsWhatsapp) }
                    ) {
                        Checkbox(
                            checked = notifyRecipientSmsWhatsapp,
                            onCheckedChange = onNotifyRecipientChange,
                            colors = CheckboxDefaults.colors(checkedColor = DispatchGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                "Notify Recipient via SMS & WhatsApp",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Auto-shares live dispatch rider tracking & contact without requiring app",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        } else {
            // --- 5B. RECEIVING: Sender Information Card (PICKUP SOURCE) ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = DispatchLightGreen,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = DispatchGreen, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Sender Information",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = PillGreen
                        ) {
                            Text(
                                "PICKUP SOURCE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Sender Full Name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = onRecipientNameChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Person handing item to courier (e.g. Kwame)", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DispatchGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Sender Phone Number", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onChooseFromContacts() }
                        ) {
                            Icon(Icons.Default.Contacts, contentDescription = null, tint = TextGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Choose from Contacts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGreen)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = recipientPhone,
                        onValueChange = onRecipientPhoneChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("e.g. 0244000000", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DispatchGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Pickup Address / Landmark", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = pickupAddress,
                        onValueChange = onPickupAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Where courier picks up the package", color = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray) },
                        trailingIcon = {
                            IconButton(onClick = onChangePickup) {
                                Icon(Icons.Default.Map, contentDescription = "Select on map", tint = TextGreen)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DispatchGreen,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.clickable { onNotifyRecipientChange(!notifyRecipientSmsWhatsapp) }
                    ) {
                        Checkbox(
                            checked = notifyRecipientSmsWhatsapp,
                            onCheckedChange = onNotifyRecipientChange,
                            colors = CheckboxDefaults.colors(checkedColor = DispatchGreen)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                "Notify Sender via SMS & WhatsApp",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                "Auto-shares pickup rider arrival & tracking link with sender without requiring app",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- 6B. RECEIVING: Drop-off Location (YOU) Card ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FamekoBlue,
                        modifier = Modifier
                            .size(32.dp)
                            .padding(top = 2.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "DESTINATION LOCATION (YOU)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FamekoBlue
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onChangeDropOff() }
                            ) {
                                Text(
                                    "Change Destination",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = FamekoBlue
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(Icons.Default.Edit, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            if (pickupNamePhone.isNotEmpty()) pickupNamePhone else "Your Contact Info",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (dropOffAddress.isNotEmpty()) dropOffAddress else "Select Drop-off Destination",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 7. Package Category Grid ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Package Category",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                "Tap to pick",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2x2 Grid of Category Cards
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
                            .height(96.dp)
                            .clickable {
                                onCategoryChange(cat.title)
                                onWeightSizeChange(cat.weight)
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) DispatchLightGreen else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) DispatchGreen else CardBorderColor
                        )
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = DispatchGreen,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    }
                                }
                            }

                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) DispatchGreen else Color(0xFFF1F5F9),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(cat.icon, fontSize = 16.sp)
                                        }
                                    }
                                    if (!isSelected) {
                                        Text(cat.weight, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                    } else {
                                        Text(cat.weight, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = DispatchGreen)
                                    }
                                }

                                Column {
                                    Text(
                                        cat.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        cat.description,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 8. Security & Instructions Card ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorderColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Security & Instructions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Secure OTP Handoff Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOtpRequiredChange(!isOtpRequired) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEBF5FF),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Secure OTP Handoff", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = PillGreen
                                ) {
                                    Text(
                                        "RECOMMENDED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextGreen,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                if (isSending) "Recipient shares 4-digit code before rider releases cargo"
                                else "Courier verifies 4-digit OTP with sender at pickup and with you at drop-off",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Switch(
                        checked = isOtpRequired,
                        onCheckedChange = onOtpRequiredChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = DispatchGreen
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(14.dp))

                // Fragile / Handle With Care Checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFragileChange(!isFragile) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFFEF2F2),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Fragile / Handle With Care", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Text(
                                "Glassware, fresh meals, delicate electronics",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Checkbox(
                        checked = isFragile,
                        onCheckedChange = onFragileChange,
                        colors = CheckboxDefaults.colors(checkedColor = DispatchGreen)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Special Instructions Field
                Text("Special Instructions for Courier", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF475569))
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. Call when outside the black gate, 2nd floor...", color = Color.Gray, fontSize = 12.sp) },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    leadingIcon = { Icon(Icons.Default.StickyNote2, contentDescription = null, tint = Color.Gray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DispatchGreen,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 9. Dark Active Route Estimate & Action Container ---
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF22C55E))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "ACTIVE ROUTE ESTIMATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF22C55E)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Okada Express",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            fareDisplayText,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            transitSubtext,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    // Payment Method Dropdown Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.clickable { showPaymentPicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(BoltYellow)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                selectedPaymentMethod,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Change payment method", tint = Color.White, modifier = Modifier.size(16.dp))
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
                                    "MTN MoMo (..6261)" to "📱 Mobile Money",
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

                Spacer(modifier = Modifier.height(16.dp))

                // Main Action Button
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DispatchGreen,
                        disabledContainerColor = Color(0xFF334155)
                    ),
                    enabled = isButtonEnabled
                ) {
                    if (isPlacing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🚀", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                actionButtonText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isButtonEnabled) Color.White else Color.LightGray
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = if (isButtonEnabled) Color.White else Color.LightGray, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- 10. Footer Protection Guarantees ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🛡 Covered up to GH₵1,000", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
            Spacer(modifier = Modifier.width(8.dp))
            Text("•", fontSize = 11.sp, color = Color(0xFF94A3B8))
            Spacer(modifier = Modifier.width(8.dp))
            Text("⚡ Direct Point-to-Point", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF64748B))
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
