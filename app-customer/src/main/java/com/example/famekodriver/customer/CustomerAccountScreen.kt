package com.example.famekodriver.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.customer.ui.theme.BoltDark
import com.example.famekodriver.customer.ui.theme.BoltLightGray
import com.example.famekodriver.customer.ui.theme.BoltGreen

@Composable
fun CustomerAccountScreen(
    sessionManager: SessionManager,
    onNavigate: (CustomerScreen) -> Unit,
    onLogout: () -> Unit
) {
    val userName = sessionManager.getDriverName() ?: "User"
    val userRating = "4.90"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = BoltDark,
                    fontSize = 26.sp
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = BoltGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = userRating,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BoltDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Profile Picture Placeholder
            Surface(
                shape = CircleShape,
                color = BoltLightGray,
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "Upload Photo",
                        tint = BoltDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Menu Items
        AccountMenuItem(
            icon = Icons.Default.PersonOutline,
            title = "Profile",
            onClick = { onNavigate(CustomerScreen.Profile) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.Payment,
            title = "Payment",
            onClick = { onNavigate(CustomerScreen.Payment) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            title = "Support",
            onClick = { onNavigate(CustomerScreen.Support) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.Security,
            title = "Safety",
            onClick = { onNavigate(CustomerScreen.Safety) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.LocationOn,
            title = "Manage places",
            onClick = { onNavigate(CustomerScreen.ManagePlaces) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.Settings,
            title = "Settings",
            onClick = { onNavigate(CustomerScreen.NotificationSettings) }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(thickness = 8.dp, color = BoltLightGray)
        Spacer(modifier = Modifier.height(16.dp))

        // Secondary Menu Items (with subtitles)
        AccountMenuItem(
            icon = Icons.Default.DirectionsCar,
            title = "Fameko Rentals",
            subtitle = "Rent a vehicle for your convenience",
            onClick = { onNavigate(CustomerScreen.FleetBrowse) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.LocalOffer,
            title = "Promotions",
            subtitle = "Promo codes, offers, and savings",
            onClick = { onNavigate(CustomerScreen.Promotions) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.Home,
            title = "Family Profile",
            subtitle = "Manage and pay for your family's rides",
            onClick = { onNavigate(CustomerScreen.FamilyProfile) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), thickness = 0.5.dp, color = BoltLightGray)
        AccountMenuItem(
            icon = Icons.Default.BusinessCenter,
            title = "Work Profile",
            subtitle = "Expense your rides",
            onClick = { onNavigate(CustomerScreen.WorkProfile) }
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        // Log Out
        Text(
            text = "Log Out",
            color = Color.Red,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )

        Spacer(modifier = Modifier.height(100.dp)) // Nav bar space
    }
}

@Composable
fun AccountMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = BoltDark
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(14.dp)
        )
    }
}
