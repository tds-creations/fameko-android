package com.example.famekodriver.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.domain.model.LocationSuggestion
import com.example.famekodriver.core.domain.model.ServiceType
import com.example.famekodriver.core.utils.ImageLinks
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltLightGray

@Composable
fun CustomerLandingScreen(
    onServiceSelected: (ServiceType) -> Unit,
    onScheduleClick: () -> Unit = {},
    recentPlaces: List<LocationSuggestion> = emptyList(),
    onSearchClick: () -> Unit = {},
    onPlaceClick: (LocationSuggestion) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Let's go places.",
            style = MaterialTheme.typography.headlineMedium,
            color = BoltDark,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))

        // Service Grid - Row 1
        Row(modifier = Modifier.fillMaxWidth()) {
            ServiceGridItem(
                title = "Rides",
                description = "Let's get moving",
                imageUrl = ImageLinks.RIDE,
                modifier = Modifier.weight(1f),
                onClick = { onServiceSelected(ServiceType.RIDE_HAILING) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            ServiceGridItem(
                title = "Schedule",
                description = "Book ahead",
                icon = Icons.Default.CalendarMonth,
                modifier = Modifier.weight(1f),
                onClick = { onScheduleClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Service Grid - Row 2
        Row(modifier = Modifier.fillMaxWidth()) {
            ServiceGridItem(
                title = "Rentals",
                description = "Hire a car",
                imageUrl = ImageLinks.RENTAL,
                modifier = Modifier.weight(1f),
                onClick = { onServiceSelected(ServiceType.RENTAL) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            ServiceGridItem(
                title = "Corporate",
                description = "Long term hire",
                icon = Icons.Default.BusinessCenter,
                modifier = Modifier.weight(1f),
                onClick = { onServiceSelected(ServiceType.RENTAL) }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Search Bar with "Later" button
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { onSearchClick() },
            shape = RoundedCornerShape(12.dp),
            color = BoltLightGray
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Icon(Icons.Default.Search, null, tint = BoltDark, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Where to?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BoltDark,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                
                Surface(
                    onClick = onScheduleClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier.height(36.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(Icons.Default.Schedule, null, tint = BoltDark, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Later", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Places List
        recentPlaces.forEach { place ->
            RecentPlaceItem(
                suggestion = place,
                onClick = { onPlaceClick(place) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Spacer(modifier = Modifier.height(100.dp)) // Extra space for bottom nav
    }
}

@Composable
fun ServiceGridItem(
    title: String,
    description: String,
    imageUrl: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = BoltLightGray
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    modifier = Modifier.size(64.dp),
                    tint = BoltDark
                )
            } else if (imageUrl != null) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BoltDark
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RecentPlaceItem(
    suggestion: LocationSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = BoltLightGray,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.History, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = suggestion.name ?: suggestion.displayName.split(",")[0],
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BoltDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = suggestion.displayName,
                fontSize = 13.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
