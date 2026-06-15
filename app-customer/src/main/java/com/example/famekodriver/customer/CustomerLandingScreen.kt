package com.example.famekodriver.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.domain.model.ServiceType
import com.example.famekodriver.core.utils.ImageLinks
import com.example.famekodriver.customer.ui.theme.FamekoBlue
import com.example.famekodriver.customer.ui.theme.BoltDark

@Composable
fun CustomerLandingScreen(
    onServiceSelected: (ServiceType) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome to Fameko",
            style = MaterialTheme.typography.headlineMedium,
            color = BoltDark
        )
        
        Text(
            text = "What do you need today?",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        ServiceCard(
            title = "Ride",
            description = "Get a ride in minutes",
            imageUrl = ImageLinks.RIDE,
            onClick = { onServiceSelected(ServiceType.RIDE_HAILING) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ServiceCard(
            title = "Delivery",
            description = "Send or receive packages",
            imageUrl = ImageLinks.DELIVERY,
            onClick = { onServiceSelected(ServiceType.PACKAGE_DELIVERY) }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ServiceCard(
            title = "Rental",
            description = "Book a car for hours or days",
            imageUrl = ImageLinks.RENTAL,
            onClick = { onServiceSelected(ServiceType.RENTAL) }
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun ServiceCard(
    title: String,
    description: String,
    imageUrl: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = BoltDark,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)),
                contentScale = ContentScale.Crop
            )
        }
    }
}
