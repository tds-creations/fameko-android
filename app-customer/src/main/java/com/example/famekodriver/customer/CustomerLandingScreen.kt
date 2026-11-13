package com.example.famekodriver.customer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.famekodriver.core.domain.model.LocationSuggestion
import com.example.famekodriver.core.domain.model.RideEstimateResponse
import com.example.famekodriver.core.domain.model.ServiceType
import com.example.famekodriver.core.utils.ImageLinks
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import com.example.famekodriver.customer.ui.theme.FamekoBlue
import com.example.famekodriver.customer.ui.theme.FamekoLightBlue
import java.util.Locale

@Composable
fun CustomerLandingScreen(
    activeRental: Map<String, Any>? = null,
    onViewRental: (Map<String, Any>) -> Unit = {},
    onServiceSelected: (ServiceType) -> Unit,
    onScheduleClick: () -> Unit = {},
    recentPlaces: List<LocationSuggestion> = emptyList(),
    rideEstimates: List<RideEstimateResponse> = emptyList(),
    onSearchClick: () -> Unit = {},
    onPlaceClick: (LocationSuggestion) -> Unit = {}
) {
    val isRentalActive = activeRental != null
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FamekoLightBlue)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        activeRental?.let { rental ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewRental(rental) },
                colors = CardDefaults.cardColors(containerColor = FamekoBlue.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, FamekoBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = FamekoBlue,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.DirectionsCar, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Active Rental", fontWeight = FontWeight.Bold, color = FamekoBlue, fontSize = 14.sp)
                        Text(rental["vehicle_name"]?.toString() ?: rental["name"]?.toString() ?: "Ongoing Trip", fontWeight = FontWeight.ExtraBold, color = BoltDark, fontSize = 16.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = FamekoBlue, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Quick Destination Shortcuts Grid (Home, Work, Recent, Saved)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShortcutGridItem(
                title = "Home",
                subtitle = "East Legon",
                icon = Icons.Default.Home,
                containerColor = Color(0xFFECFDF5),
                contentColor = Color(0xFF059669),
                onClick = onSearchClick
            )
            ShortcutGridItem(
                title = "Work",
                subtitle = "Airport City",
                icon = Icons.Default.Work,
                containerColor = Color(0xFFEFF6FF),
                contentColor = Color(0xFF2563EB),
                onClick = onSearchClick
            )
            ShortcutGridItem(
                title = "Recent",
                subtitle = "Accra Mall",
                icon = Icons.Default.History,
                containerColor = Color(0xFFF1F5F9),
                contentColor = Color(0xFF475569),
                onClick = onSearchClick
            )
            ShortcutGridItem(
                title = "Saved",
                subtitle = "3 spots",
                icon = Icons.Default.Star,
                containerColor = Color(0xFFFEF3C7),
                contentColor = Color(0xFFD97706),
                onClick = onSearchClick
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Popular Destination Quick Booking Card (Kotoka Airport)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSearchClick() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(1.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFF059669), modifier = Modifier.size(22.dp))
                        }
                    }

                    Column {
                        Text("Kotoka Int. Airport (ACC)", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF0F172A))
                        Spacer(Modifier.height(2.dp))
                        Text("Fast pickup • ~18 mins away • ✈️ Includes GH₵ 10 Toll", fontSize = 11.sp, color = Color(0xFF059669), fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    color = Color(0xFFECFDF5),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("GH₵ 42", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF059669), modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Service Grid - Row 1
        Row(modifier = Modifier.fillMaxWidth()) {
            ServiceGridItem(
                title = "Rides",
                description = if (isRentalActive) "Rental in progress" else "Let's get moving",
                imageUrl = ImageLinks.RIDE,
                modifier = Modifier.weight(1f),
                enabled = !isRentalActive,
                onClick = { onServiceSelected(ServiceType.RIDE_HAILING) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            ServiceGridItem(
                title = "Schedule",
                description = if (isRentalActive) "Rental in progress" else "Book ahead",
                imageUrl = ImageLinks.SCHEDULE,
                modifier = Modifier.weight(1f),
                enabled = !isRentalActive,
                onClick = { onScheduleClick() }
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Service Grid - Row 2
        Row(modifier = Modifier.fillMaxWidth()) {
            ServiceGridItem(
                title = "Delivery",
                description = if (isRentalActive) "Rental in progress" else "Send packages",
                imageUrl = ImageLinks.DELIVERY,
                modifier = Modifier.weight(1f),
                enabled = !isRentalActive,
                onClick = { onServiceSelected(ServiceType.PACKAGE_DELIVERY) }
            )
            Spacer(modifier = Modifier.width(12.dp))
            ServiceGridItem(
                title = "Rentals",
                description = "Hire a car",
                imageUrl = ImageLinks.RENTAL,
                modifier = Modifier.weight(1f),
                onClick = { onServiceSelected(ServiceType.RENTAL) }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Recent Places List
        recentPlaces.forEach { place ->
            RecentPlaceItem(
                suggestion = place,
                enabled = !isRentalActive,
                onClick = { onPlaceClick(place) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Special Offers & Services Carousel
        SpecialOffersSection(
            onServiceSelected = onServiceSelected,
            enabled = !isRentalActive
        )
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ShortcutGridItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            modifier = Modifier.size(60.dp),
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = contentColor, modifier = Modifier.size(26.dp))
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BoltDark)
        Text(subtitle, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FleetTierCard(title: String, eta: String, price: String, bgColor: Color, textColor: Color) {
    Surface(
        modifier = Modifier
            .width(140.dp)
            .height(90.dp),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                Text(eta, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsCar, null, tint = textColor, modifier = Modifier.size(24.dp))
                Text(price, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = textColor)
            }
        }
    }
}

@Composable
fun ServiceGridItem(
    title: String,
    description: String,
    imageUrl: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(132.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 10.dp)
                .alpha(if (enabled) 1f else 0.5f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
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
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Fit
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = BoltDark,
                    maxLines = 1
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = Color.Gray,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RecentPlaceItem(
    suggestion: LocationSuggestion,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.5f),
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
                fontSize = 15.sp,
                color = if (enabled) BoltDark else Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = suggestion.displayName,
                fontSize = 12.sp,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SpecialOffersSection(
    onServiceSelected: (ServiceType) -> Unit,
    onSeeAllClick: () -> Unit = {},
    enabled: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header: "Special Offers & Services" + "See all"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Special Offers & Services",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = Color(0xFF0F172A)
            )
            Text(
                text = "See all",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF059669),
                modifier = Modifier.clickable { onSeeAllClick() }
            )
        }

        // 1. Featured Big Hero Visual Banner (Okada Express)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
                .clickable(enabled = enabled) { onServiceSelected(ServiceType.PACKAGE_DELIVERY) },
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageLinks.HERO_OKADA_EXPRESS,
                    contentDescription = "Okada Express Delivery",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark Gradient Overlay for readable white text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.25f),
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Pill + Action Arrow
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = Color(0xFFDC2626), // Red badge
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(
                                text = "OKADA EXPRESS • FROM GH₵ 15",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = Color.White.copy(alpha = 0.25f),
                            shape = CircleShape,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Bottom Headlines
                    Column {
                        Text(
                            text = "TAP TO SEND YOUR PACKAGES",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = Color.White,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Fast doorstep motorbike dispatch across Accra. Real-time GPS tracking.",
                            fontSize = 11.sp,
                            color = Color(0xFFE2E8F0),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2. 2-Column Split Feature Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Aboboyaa Cargo Delivery
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(155.dp)
                    .clickable(enabled = enabled) { onServiceSelected(ServiceType.PACKAGE_DELIVERY) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageLinks.HERO_ABOBOYAA_CARGO,
                        contentDescription = "Aboboyaa Cargo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color(0xFFD97706), // Amber badge
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = "CARGO TRIKE",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "ABOBOYAA IN CARGO",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bulky goods & market freight delivered seamlessly.",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Card 2: Comfort Air-conditioned Rides
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(155.dp)
                    .clickable(enabled = enabled) { onServiceSelected(ServiceType.RIDE_HAILING) },
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(2.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageLinks.HERO_COMFORT_RIDE,
                        contentDescription = "Comfort Rides",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.85f)
                                    )
                                )
                            )
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            color = Color(0xFF10B981), // Emerald badge
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.Start)
                        ) {
                            Text(
                                text = "FROM GH₵ 18",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "COMFORT RIDES",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Beat the Accra heat in air-conditioned luxury.",
                                fontSize = 10.sp,
                                color = Color(0xFFCBD5E1),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
