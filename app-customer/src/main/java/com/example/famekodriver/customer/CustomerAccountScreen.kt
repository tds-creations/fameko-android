package com.example.famekodriver.customer

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.OrderRepository
import com.example.famekodriver.core.data.repository.RentalRepository
import com.example.famekodriver.core.data.repository.UserRepository
import com.example.famekodriver.customer.ui.theme.BoltDark
import java.util.Locale

@Composable
fun CustomerAccountScreen(
    sessionManager: SessionManager,
    onNavigate: (CustomerScreen) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val userRepository = remember { UserRepository() }
    val rentalRepository = remember { RentalRepository() }
    val orderRepository = remember { OrderRepository() }

    val customerId = sessionManager.getCustomerId() ?: sessionManager.getDriverId() ?: "1"

    var userName by remember { mutableStateOf(sessionManager.getDriverName() ?: "Joel Asare") }
    var userRating by remember { mutableStateOf("4.90") }
    var ridesCount by remember { mutableStateOf("64") }
    var profilePicUrl by remember { mutableStateOf<String?>(null) }
    var famekoPayBalance by remember { mutableStateOf("142.50") }
    var rewardsPoints by remember { mutableStateOf("350 Pts") }
    var activeRentalsText by remember { mutableStateOf("None active") }
    var promosText by remember { mutableStateOf("2 Available") }

    LaunchedEffect(Unit) {
        // 1. Fetch Profile
        userRepository.getCustomerProfile(customerId).onSuccess { profile ->
            if (profile.isNotEmpty()) {
                userName = profile["name"]?.toString() ?: profile["username"]?.toString() ?: userName
                userRating = profile["rating"]?.toString() ?: userRating
                ridesCount = profile["rides_count"]?.toString() ?: profile["total_rides"]?.toString() ?: ridesCount
                profilePicUrl = profile["profile_picture"]?.toString()
                profile["wallet_balance"]?.let { famekoPayBalance = String.format(Locale.US, "%.2f", it.toString().toDoubleOrNull() ?: 142.50) }
                profile["rewards_points"]?.let { rewardsPoints = "$it Pts" }
            }
        }

        // 2. Fetch Active Rentals Count
        rentalRepository.getCustomerRentals(customerId).onSuccess { rentals ->
            val activeCount = rentals.count { it["status"]?.toString()?.uppercase() in listOf("ACTIVE", "PENDING", "ONGOING") }
            activeRentalsText = if (activeCount > 0) "$activeCount active" else "None active"
        }

        // 3. Fetch Promotions Count
        orderRepository.getPromotions().onSuccess { promos ->
            val promoCount = promos.size
            promosText = if (promoCount > 0) "$promoCount Available" else "No promotions"
        }
    }

    val initials = userName.split(" ").let { if (it.size > 1) "${it[0].first()}${it[1].first()}" else it[0].take(2) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Header Section (Name & Avatar) at absolute top with zero padding
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color = BoltDark,
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "$userRating • $ridesCount rides",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BoltDark
                                )
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                    }

                    // Profile Picture Avatar with camera badge
                    Box(modifier = Modifier.size(72.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0A192F),
                            modifier = Modifier
                                .fillMaxSize()
                                .border(2.dp, Color(0xFF10B981), CircleShape)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(initials.uppercase(), color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Upload Photo",
                                    tint = BoltDark,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Action Cards (Fameko Pay & Rewards)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Fameko Pay Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigate(CustomerScreen.Payment) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF10B981),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.AccountBalanceWallet, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("FAMEKO PAY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(1.dp))
                                Text("GH₵ $famekoPayBalance", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }

                    // Rewards Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { Toast.makeText(context, "$rewardsPoints available", Toast.LENGTH_SHORT).show() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0A192F),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.CardGiftcard, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("REWARDS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(1.dp))
                                Text(rewardsPoints, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            }

            // Section Header 1: ACCOUNT MANAGEMENT
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "ACCOUNT MANAGEMENT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Account Management Card Container
            item {
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountManagementRow(
                            icon = Icons.Default.PersonOutline,
                            iconColor = Color(0xFF2563EB),
                            title = "Profile",
                            subtitle = "Personal details & contact verification",
                            badgeText = "Verified",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = { onNavigate(CustomerScreen.Profile) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.Payment,
                            iconColor = Color(0xFF059669),
                            title = "Payment",
                            subtitle = "MTN MoMo, Visa •••• 4821, Cash",
                            showGreenDot = true,
                            onClick = { onNavigate(CustomerScreen.Payment) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.SupportAgent,
                            iconColor = Color(0xFF7C3AED),
                            title = "Support",
                            subtitle = "Help center, trips inquiry & live chat",
                            onClick = { onNavigate(CustomerScreen.Support) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.Security,
                            iconColor = Color(0xFFDC2626),
                            title = "Safety",
                            subtitle = "Emergency contacts & 24/7 Fameko Shield",
                            badgeText = "Active",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = { onNavigate(CustomerScreen.Safety) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.LocationOn,
                            iconColor = Color(0xFFD97706),
                            title = "Manage places",
                            subtitle = "Home, Work & favorite pickup spots",
                            onClick = { onNavigate(CustomerScreen.ManagePlaces) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.Settings,
                            iconColor = Color(0xFF64748B),
                            title = "Settings",
                            subtitle = "Privacy, language, notifications & security",
                            onClick = { onNavigate(CustomerScreen.NotificationSettings) }
                        )
                    }
                }
            }

            // Section Header 2: FAMEKO SERVICES & PERKS
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "FAMEKO SERVICES & PERKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Services & Perks Card Container
            item {
                Surface(
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        AccountManagementRow(
                            icon = Icons.Default.DirectionsCar,
                            iconColor = Color(0xFF0284C7),
                            title = "Fameko Rentals",
                            subtitle = "Rent a vehicle for your convenience",
                            badgeText = "NEW",
                            badgeColor = Color(0xFFECFDF5),
                            badgeTextColor = Color(0xFF059669),
                            onClick = { onNavigate(CustomerScreen.FleetBrowse) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.History,
                            iconColor = Color(0xFF7C3AED),
                            title = "My Rentals",
                            subtitle = "View your active and past rentals",
                            badgeText = activeRentalsText,
                            badgeColor = Color(0xFFF1F5F9),
                            badgeTextColor = Color(0xFF64748B),
                            onClick = { onNavigate(CustomerScreen.Rentals) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.LocalOffer,
                            iconColor = Color(0xFFD97706),
                            title = "Promotions",
                            subtitle = "Promo codes, offers, and savings",
                            badgeText = promosText,
                            badgeColor = Color(0xFFFEF3C7),
                            badgeTextColor = Color(0xFFD97706),
                            onClick = { onNavigate(CustomerScreen.Promotions) }
                        )
                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                        AccountManagementRow(
                            icon = Icons.Default.Home,
                            iconColor = Color(0xFF9333EA),
                            title = "Family Profile",
                            subtitle = "Manage and pay for your family's rides",
                            onClick = { onNavigate(CustomerScreen.FamilyProfile) }
                        )
                    }
                }
            }

            // Log Out Button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                    ) {
                        Text("Log Out", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Version Footer
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Fameko Rider v4.26.1 (Build 2024.9)",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AccountManagementRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    badgeColor: Color = Color.LightGray,
    badgeTextColor: Color = Color.DarkGray,
    showGreenDot: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = iconColor.copy(alpha = 0.1f),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                badgeText?.let {
                    Surface(
                        color = badgeColor,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = it,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeTextColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (showGreenDot) {
                    Box(modifier = Modifier.size(6.dp).background(Color(0xFF10B981), CircleShape))
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = Color.Gray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
    }
}
