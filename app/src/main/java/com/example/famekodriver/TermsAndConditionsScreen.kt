package com.example.famekodriver

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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = "Driver Terms of Service",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF004E89)
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            DriverTermsSection(
                title = "1. Relationship with Fameko",
                content = "As a driver, you are an independent contractor and not an employee of Fameko. You have the right to accept or decline service requests at your discretion."
            )
            
            DriverTermsSection(
                title = "2. Driver Requirements",
                content = "You must maintain a valid driver's license, vehicle insurance, and all necessary permits required by local laws. Your vehicle must meet Fameko's safety and quality standards."
            )
            
            DriverTermsSection(
                title = "3. Daily Service Fee",
                content = "To access the platform and receive ride requests, a daily service fee must be paid. This fee is non-refundable and grants you 24-hour access to our dispatch system."
            )
            
            DriverTermsSection(
                title = "4. Commission and Earnings",
                content = "For standard rides, you keep 100% of the fare after the daily service fee. For vehicle rentals facilitated through the platform, a commission of 15% applies to the total rental amount."
            )
            
            DriverTermsSection(
                title = "5. Professional Conduct",
                content = "Drivers are expected to maintain professional conduct, follow traffic laws, and provide safe transportation. Low ratings or safety violations may lead to account suspension."
            )
            
            DriverTermsSection(
                title = "6. Real-time Tracking",
                content = "By using the app, you consent to Fameko tracking your location in real-time to provide services to customers and for safety monitoring."
            )
            
            DriverTermsSection(
                title = "7. Limitation of Liability",
                content = "Fameko is not responsible for any disputes, damages, or losses resulting from your interaction with customers or the use of your vehicle."
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
fun DriverTermsSection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 20.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF004E89)
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
