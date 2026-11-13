package com.example.famekodriver.customer.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.famekodriver.core.domain.model.SavedPlace
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.ui.theme.BoltGreen
import com.example.famekodriver.customer.ui.theme.FamekoBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedPlacesSpeedDialScreen(
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit,
    onAddPlace: (String) -> Unit,
    onSelectPlace: (SavedPlace) -> Unit
) {
    val context = LocalContext.current
    val savedPlaces by viewModel.savedPlaces.collectAsState()

    // Default sample places if user list is empty for immediate speed-dial utility
    val displayPlaces = if (savedPlaces.isNotEmpty()) {
        savedPlaces
    } else {
        listOf(
            SavedPlace(
                id = "home_default",
                customerId = "0",
                label = "Home",
                address = "East Legon, Accra",
                latitude = 5.6358,
                longitude = -0.1601
            ),
            SavedPlace(
                id = "work_default",
                customerId = "0",
                label = "Work",
                address = "Airport City, Accra",
                latitude = 5.6037,
                longitude = -0.1870
            ),
            SavedPlace(
                id = "mall_default",
                customerId = "0",
                label = "Accra Mall",
                address = "Spintex Rd, Tetteh Quarshie Interchange",
                latitude = 5.6225,
                longitude = -0.1772
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Saved Places Speed Dial",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "1-Tap instant destination booking",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { onAddPlace("Favorite") }) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = FamekoBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add New", fontWeight = FontWeight.Bold, color = FamekoBlue, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Speed Dial Instruction Card
            Surface(
                color = Color(0xFFECFDF5),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BoltGreen.copy(alpha = 0.2f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.FlashOn, null, tint = Color(0xFF047857), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Speed Dial Destinations",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "Tap any saved spot below to instantly calculate route & book ride or package dispatch.",
                            fontSize = 11.sp,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }

            // List of Speed Dial Saved Places
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(displayPlaces, key = { it.id }) { place ->
                    SpeedDialPlaceCard(
                        place = place,
                        onSelect = {
                            viewModel.selectSavedPlace(place)
                            onSelectPlace(place)
                        },
                        onDelete = {
                            viewModel.deleteSavedPlace(place.id)
                            Toast.makeText(context, "${place.label} removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun SpeedDialPlaceCard(
    place: SavedPlace,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val (icon, iconBg, iconTint) = when (place.label.lowercase()) {
        "home" -> Triple(Icons.Default.Home, Color(0xFFECFDF5), Color(0xFF059669))
        "work" -> Triple(Icons.Default.Work, Color(0xFFEFF6FF), Color(0xFF2563EB))
        "gym" -> Triple(Icons.Default.FitnessCenter, Color(0xFFFAF5FF), Color(0xFF9333EA))
        "airport" -> Triple(Icons.Default.Flight, Color(0xFFFEF3C7), Color(0xFFD97706))
        else -> Triple(Icons.Default.Star, Color(0xFFFEF3C7), Color(0xFFD97706))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = iconBg,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = place.label,
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF0F172A)
                    )
                    Surface(
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "SPEED DIAL",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = place.address,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Delete Button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Action Button: Instant Go
            Surface(
                shape = CircleShape,
                color = FamekoBlue,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Directions,
                        contentDescription = "Book Ride",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
