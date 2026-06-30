package com.example.famekodriver.customer.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import com.example.famekodriver.customer.ui.theme.FamekoBlue

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
    val name = profile?.get("name")?.toString() ?: "User"
    val email = profile?.get("email")?.toString() ?: "No email"
    val phone = profile?.get("phone")?.toString() ?: "No phone"
    val region = profile?.get("region")?.toString() ?: "N/A"

    AccountDetailScreen(title = "Profile", onBack = onBack) {
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

@Composable
fun CustomerSavedPlacesScreen(onBack: () -> Unit) {
    AccountDetailScreen(title = "Saved Places", onBack = onBack) {
        Spacer(modifier = Modifier.height(24.dp))
        SavedPlaceItem(icon = Icons.Default.Home, title = "Home", address = "Add home address")
        SavedPlaceItem(icon = Icons.Default.Work, title = "Work", address = "Add work address")
        
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = { /* TODO */ }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add new place", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SavedPlaceItem(icon: ImageVector, title: String, address: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Medium, color = BoltDark)
            Text(address, fontSize = 14.sp, color = FamekoBlue)
        }
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
