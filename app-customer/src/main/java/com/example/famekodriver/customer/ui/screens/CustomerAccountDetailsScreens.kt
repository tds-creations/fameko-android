package com.example.famekodriver.customer.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import com.example.famekodriver.customer.ui.theme.FamekoBlue
import com.example.famekodriver.core.domain.model.SavedPlace

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            content = content
        )
    }
}

@Composable
fun CustomerProfileScreen(profile: Map<String, Any>?, onBack: () -> Unit) {
    var name by remember(profile) { mutableStateOf(profile?.get("name")?.toString() ?: "") }
    var email by remember(profile) { mutableStateOf(profile?.get("email")?.toString() ?: "") }
    var phone by remember(profile) { mutableStateOf(profile?.get("phone")?.toString() ?: "") }
    var region by remember(profile) { mutableStateOf(profile?.get("region")?.toString() ?: "") }

    AccountDetailScreen(title = "Profile", onBack = onBack) {
        if (profile == null) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = FamekoBlue)
            }
        } else {
            Spacer(modifier = Modifier.height(20.dp))
            Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Surface(
                    shape = CircleShape,
                    color = BoltLightGray,
                    modifier = Modifier.size(100.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(20.dp), tint = Color.Gray)
                }
                Surface(
                    shape = CircleShape,
                    color = FamekoBlue,
                    modifier = Modifier.size(32.dp).align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.padding(6.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            ProfileField(label = "Full Name", value = name)
            ProfileField(label = "Phone Number", value = phone)
            ProfileField(label = "Email", value = email)
            ProfileField(label = "Region", value = region)
            
            Spacer(modifier = Modifier.height(40.dp))
            Button(
                onClick = { /* TODO: Save */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = label, fontSize = 14.sp, color = Color.Gray)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = BoltDark, modifier = Modifier.padding(top = 4.dp))
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), thickness = 0.5.dp, color = BoltLightGray)
    }
}

@Composable
fun CustomerPaymentScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Payment", onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Payment Methods", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
        Spacer(modifier = Modifier.height(16.dp))
        
        PaymentMethodItem(icon = Icons.Default.Money, title = "Cash", isSelected = true)
        PaymentMethodItem(icon = Icons.Default.Wallet, title = "Fameko Balance", subtitle = "₵0.00")
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Add Payment Method", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
        Spacer(modifier = Modifier.height(16.dp))
        
        AddPaymentItem(icon = Icons.Default.CreditCard, title = "Credit/Debit Card")
        AddPaymentItem(icon = Icons.Default.Smartphone, title = "Mobile Money")
    }
}

@Composable
fun PaymentMethodItem(icon: ImageVector, title: String, subtitle: String? = null, isSelected: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium, color = BoltDark)
            if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        if (isSelected) {
            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF2ECC71))
        }
    }
}

@Composable
fun AddPaymentItem(icon: ImageVector, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), color = BoltDark)
        Icon(Icons.Default.Add, contentDescription = null, tint = FamekoBlue)
    }
}

@Composable
fun CustomerSafetyScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Safety", onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        SafetyFeatureItem(
            icon = Icons.Default.Shield,
            title = "Safety Toolkit",
            description = "Quickly share your ride details or contact emergency services."
        )
        SafetyFeatureItem(
            icon = Icons.Default.Group,
            title = "Trusted Contacts",
            description = "Share your trip status with friends and family automatically."
        )
        SafetyFeatureItem(
            icon = Icons.Default.Lock,
            title = "Ride Check",
            description = "We\u0027ll check in if a trip doesn\u0027t go as planned."
        )
    }
}

@Composable
fun SafetyFeatureItem(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 16.dp)) {
        Icon(icon, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BoltDark)
            Text(description, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerManagePlacesScreen(
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit,
    onAddPlace: (String) -> Unit,
    onEditPlace: (Int, String) -> Unit
) {
    val uiState by viewModel.savedPlacesUiState.collectAsState()
    val isRefreshing = uiState is CustomerMapViewModel.SavedPlacesUiState.Loading
    
    val savedPlacesLocal by viewModel.savedPlaces.collectAsState()
    
    var showDeleteDialog by remember { mutableStateOf<SavedPlace?>(null) }

    AccountDetailScreen(title = "Manage places", onBack = onBack) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.fetchSavedPlaces() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (savedPlacesLocal.isEmpty() && uiState is CustomerMapViewModel.SavedPlacesUiState.Loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FamekoBlue)
                    }
                } else if (savedPlacesLocal.isEmpty() && uiState is CustomerMapViewModel.SavedPlacesUiState.Error) {
                    Box(modifier = Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) {
                        Text("Error: ${(uiState as CustomerMapViewModel.SavedPlacesUiState.Error).message}", color = Color.Red)
                    }
                } else {
                    val savedPlaces = savedPlacesLocal
                    val homePlace = savedPlaces.find { it.label.equals("Home", ignoreCase = true) }
                    val workPlace = savedPlaces.find { it.label.equals("Work", ignoreCase = true) }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        "Quick access",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Home Item
                    SavedPlaceActionItem(
                        icon = Icons.Default.Home,
                        title = "Home",
                        subtitle = homePlace?.address ?: "Add home address",
                        isSet = homePlace != null,
                        onDelete = { homePlace?.let { showDeleteDialog = it } },
                        onClick = { 
                            if (homePlace != null) onEditPlace(homePlace.id.toInt(), "Home")
                            else onAddPlace("Home") 
                        }
                    )

                    HorizontalDivider(color = BoltLightGray, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))

                    // Work Item
                    SavedPlaceActionItem(
                        icon = Icons.Default.Work,
                        title = "Work",
                        subtitle = workPlace?.address ?: "Add work address",
                        isSet = workPlace != null,
                        onDelete = { workPlace?.let { showDeleteDialog = it } },
                        onClick = { 
                            if (workPlace != null) onEditPlace(workPlace.id.toInt(), "Work")
                            else onAddPlace("Work") 
                        }
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Other places",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
                        )
                        TextButton(onClick = { onAddPlace("Favorite") }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add new", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // List of other saved places
                    val otherPlaces = savedPlaces.filter { 
                        !it.label.equals("Home", ignoreCase = true) && !it.label.equals("Work", ignoreCase = true) 
                    }

                    if (otherPlaces.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No other places saved", color = Color.LightGray, fontSize = 14.sp)
                        }
                    } else {
                        otherPlaces.forEach { place ->
                            SavedPlaceActionItem(
                                icon = Icons.Default.Place,
                                title = place.label,
                                subtitle = place.address,
                                isSet = true,
                                onDelete = { showDeleteDialog = place },
                                onClick = { onEditPlace(place.id.toInt(), place.label) }
                            )
                            HorizontalDivider(color = BoltLightGray, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }

    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Place") },
            text = { Text("Are you sure you want to remove '${showDeleteDialog?.label}' from your managed places?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog?.let { viewModel.deleteSavedPlace(it.id) }
                    showDeleteDialog = null
                }) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}

@Composable
fun SavedPlaceActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSet: Boolean,
    onDelete: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = BoltLightGray,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isSet) FamekoBlue else Color.Gray, 
                modifier = Modifier.padding(10.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title, 
                style = MaterialTheme.typography.bodyLarge, 
                fontWeight = FontWeight.Bold,
                color = BoltDark
            )
            Text(
                subtitle, 
                style = MaterialTheme.typography.bodyMedium, 
                color = if (!isSet) FamekoBlue else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        if (isSet && onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.LightGray, modifier = Modifier.size(20.dp))
            }
        }
        
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight, 
            contentDescription = null, 
            tint = Color.LightGray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun CustomerFamilyProfileScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Family Profile", onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(16.dp)).background(BoltLightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("Rides for everyone", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BoltDark)
        Text(
            "Pay for your family's rides and get notified when they arrive.",
            fontSize = 15.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
        ) {
            Text("Set Up Family Profile", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CustomerWorkProfileScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Work Profile", onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(40.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text("Separate work and personal rides", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BoltDark)
        }
        Spacer(modifier = Modifier.height(24.dp))
        WorkBenefitItem(Icons.Default.Receipt, "Easy expensing", "Get receipts sent directly to your work email.")
        WorkBenefitItem(Icons.Default.Assessment, "Monthly reports", "Track your business travel monthly.")
        
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = { /* TODO */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = FamekoBlue)
        ) {
            Text("Join or Create Work Profile", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun WorkBenefitItem(icon: ImageVector, title: String, description: String) {
    Row(modifier = Modifier.padding(vertical = 12.dp)) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, color = BoltDark)
            Text(description, fontSize = 13.sp, color = Color.Gray)
        }
    }
}
