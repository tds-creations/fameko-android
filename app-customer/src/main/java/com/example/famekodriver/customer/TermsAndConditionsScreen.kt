package com.example.famekodriver.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.customer.ui.theme.BoltDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms and Conditions", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Welcome to Fameko",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BoltDark
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            TermsSection(
                title = "1. Introduction",
                content = "Fameko is a platform that connects users with transportation and delivery services. By using our application, you agree to these terms and conditions in full."
            )
            
            TermsSection(
                title = "2. User Accounts",
                content = "You must be at least 18 years old to create an account. You are responsible for maintaining the confidentiality of your account details and for all activities under your account."
            )
            
            TermsSection(
                title = "3. Services",
                content = "Fameko provides ride-hailing, package delivery, and vehicle rental services. Prices for these services are estimated and may vary based on traffic, demand, and other factors."
            )
            
            TermsSection(
                title = "4. Payments",
                content = "Payments can be made via cash, mobile money, or card. For rentals, a commission may apply. You agree to pay all charges incurred in connection with your use of the services."
            )
            
            TermsSection(
                title = "5. Safety",
                content = "Your safety is our priority. Users are encouraged to use the safety toolkit within the app. Fameko is not liable for indirect or consequential damages arising from the use of our services."
            )
            
            TermsSection(
                title = "6. Privacy Policy",
                content = "We value your privacy. Please refer to our Privacy Policy for details on how we collect, use, and protect your personal information."
            )
            
            TermsSection(
                title = "7. Termination",
                content = "We reserve the right to suspend or terminate your account at our discretion if you violate these terms or engage in fraudulent activities."
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Last updated: October 2023",
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun TermsSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = BoltDark
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            fontSize = 14.sp,
            color = Color.DarkGray,
            lineHeight = 20.sp
        )
    }
}
