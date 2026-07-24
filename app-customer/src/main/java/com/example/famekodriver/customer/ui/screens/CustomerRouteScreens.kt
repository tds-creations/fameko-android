package com.example.famekodriver.customer.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.famekodriver.core.domain.model.LocationSuggestion
import com.example.famekodriver.customer.CustomerMapViewModel
import com.example.famekodriver.customer.ui.theme.*
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationServices

@Composable
fun SaveLocationSearchScreen(
    label: String,
    placeId: String? = null,
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var customLabel by remember { mutableStateOf(label) }
    val suggestions = viewModel.dropOffSuggestions
    val recentPlaces = viewModel.recentPlaces
    val isDefaultLabel = label == "Home" || label == "Work"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
            Text(
                text = if (placeId != null) "Edit $label" else "Set $label",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (!isDefaultLabel) {
            OutlinedTextField(
                value = customLabel,
                onValueChange = { customLabel = it },
                label = { Text("Name this place") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.updateDropOffLocation(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search location", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                { 
                    IconButton(onClick = { 
                        searchQuery = ""
                        viewModel.updateDropOffLocation("")
                    }) { 
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray) 
                    } 
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = FamekoBlue,
                unfocusedBorderColor = Color.LightGray,
                focusedContainerColor = BoltLightGray,
                unfocusedContainerColor = BoltLightGray
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            val listToUse = if (searchQuery.isEmpty()) recentPlaces else suggestions
            
            items(listToUse) { suggestion ->
                LocationSearchItem(
                    suggestion = suggestion,
                    onClick = {
                        if (placeId != null) {
                            viewModel.updateSavedPlace(placeId, customLabel, suggestion)
                        } else {
                            viewModel.savePlace(suggestion, customLabel)
                        }
                        onBack()
                    }
                )
                HorizontalDivider(color = BoltLightGray, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun LocationSearchItem(suggestion: LocationSuggestion, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (suggestion.type == "recent") Icons.Default.History else Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name ?: suggestion.displayName.split(",").first(),
                fontWeight = FontWeight.Medium,
                color = BoltDark
            )
            Text(
                text = suggestion.displayName,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RouteSelectionScreen(
    viewModel: CustomerMapViewModel,
    onBack: () -> Unit,
    onMapClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val focusManager = LocalFocusManager.current
    val pickupFocusRequester = remember { FocusRequester() }
    val dropOffFocusRequester = remember { FocusRequester() }
    
    var isPickupFocused by remember { mutableStateOf(true) }
    var isDropOffFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (viewModel.pickupLocation.isEmpty()) {
            pickupFocusRequester.requestFocus()
        } else if (viewModel.dropOffLocation.isEmpty()) {
            dropOffFocusRequester.requestFocus()
        } else {
            dropOffFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Route",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Search Box Container
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BoltLightGray)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Pickup Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (isPickupFocused) BoltGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .border(2.dp, Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = viewModel.pickupLocation,
                            onValueChange = { viewModel.updatePickupLocation(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(pickupFocusRequester)
                                .onFocusChanged { 
                                    if (it.isFocused) {
                                        isPickupFocused = true
                                        isDropOffFocused = false
                                        viewModel.focusedStopIndex = -1
                                    }
                                },
                            textStyle = TextStyle(fontSize = 16.sp, color = BoltDark, fontWeight = FontWeight.Medium),
                            decorationBox = { innerTextField ->
                                if (viewModel.pickupLocation.isEmpty()) {
                                    Text("Pickup location", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )

                        if (viewModel.pickupLocation.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updatePickupLocation("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        if (isPickupFocused) {
                            Surface(
                                modifier = Modifier.clickable { onMapClick() },
                                color = FamekoBlue,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Map", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Stops
                    viewModel.stops.forEachIndexed { index, stop ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Gray))
                            Spacer(modifier = Modifier.width(12.dp))
                            BasicTextField(
                                value = stop,
                                onValueChange = { newValue ->
                                    viewModel.updateStopLocation(index, newValue)
                                },
                                modifier = Modifier.weight(1f).onFocusChanged { 
                                    if (it.isFocused) {
                                        isPickupFocused = false
                                        isDropOffFocused = false
                                        viewModel.focusedStopIndex = index
                                    }
                                },
                                textStyle = TextStyle(fontSize = 14.sp, color = BoltDark),
                                decorationBox = { innerTextField ->
                                    if (stop.isEmpty()) Text("Stop ${index + 1}", color = Color.Gray, fontSize = 14.sp)
                                    innerTextField()
                                },
                                singleLine = true
                            )
                            IconButton(onClick = {
                                viewModel.removeStop(index)
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    if (viewModel.stops.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))

                    // Dropoff Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(
                                width = 1.5.dp,
                                color = if (isDropOffFocused) BoltGreen else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = BoltDark
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        BasicTextField(
                            value = viewModel.dropOffLocation,
                            onValueChange = { viewModel.updateDropOffLocation(it) },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(dropOffFocusRequester)
                                .onFocusChanged { 
                                    if (it.isFocused) {
                                        isPickupFocused = false
                                        isDropOffFocused = true
                                        viewModel.focusedStopIndex = -1
                                    }
                                },
                            textStyle = TextStyle(fontSize = 16.sp, color = BoltDark, fontWeight = FontWeight.Medium),
                            decorationBox = { innerTextField ->
                                if (viewModel.dropOffLocation.isEmpty()) {
                                    Text("Dropoff location", color = Color.Gray, fontSize = 16.sp)
                                }
                                innerTextField()
                            },
                            singleLine = true
                        )
                        
                        if (viewModel.dropOffLocation.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateDropOffLocation("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                        
                        if (isDropOffFocused) {
                            Surface(
                                modifier = Modifier.clickable { onMapClick() },
                                color = FamekoBlue,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Map", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.Default.Map, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Action Icons
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = { viewModel.addStop() }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Add, null, tint = BoltDark)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = {
                        val tempLoc = viewModel.pickupLocation
                        val tempLat = viewModel.pickupLat
                        val tempLng = viewModel.pickupLng
                        
                        viewModel.pickupLocation = viewModel.dropOffLocation
                        viewModel.pickupLat = viewModel.dropOffLat
                        viewModel.pickupLng = viewModel.dropOffLng
                        
                        viewModel.dropOffLocation = tempLoc
                        viewModel.dropOffLat = tempLat
                        viewModel.dropOffLng = tempLng
                    }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.SwapVert, null, tint = BoltDark)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Suggestions List
            val suggestions = when {
                isPickupFocused -> viewModel.pickupSuggestions
                isDropOffFocused -> viewModel.dropOffSuggestions
                viewModel.focusedStopIndex != -1 -> viewModel.stopSuggestions
                else -> emptyList()
            }
            
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                val currentText = when {
                    isPickupFocused -> viewModel.pickupLocation
                    isDropOffFocused -> viewModel.dropOffLocation
                    viewModel.focusedStopIndex != -1 -> viewModel.stops.getOrElse(viewModel.focusedStopIndex) { "" }
                    else -> ""
                }
                
                if (currentText.isEmpty()) {
                    item {
                        ListItem(
                            headlineContent = { Text("Use current location", fontWeight = FontWeight.Bold) },
                            leadingContent = { Icon(Icons.Default.MyLocation, null, tint = BoltGreen) },
                            modifier = Modifier.clickable { 
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                    viewModel.isLoading = true
                                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                        .addOnSuccessListener { location: android.location.Location? ->
                                            location?.let { 
                                                viewModel.useCurrentLocation(it, isPickupFocused, viewModel.focusedStopIndex)
                                            } ?: run { viewModel.isLoading = false }
                                        }.addOnFailureListener { viewModel.isLoading = false }
                                }
                            }
                        )
                    }
                }

                items(suggestions) { suggestion ->
                    LocationSuggestionItem(suggestion) {
                        when {
                            isPickupFocused -> {
                                viewModel.selectSuggestion(suggestion, true)
                                dropOffFocusRequester.requestFocus()
                            }
                            isDropOffFocused -> {
                                viewModel.selectSuggestion(suggestion, false)
                            }
                            viewModel.focusedStopIndex != -1 -> {
                                viewModel.selectStopSuggestion(viewModel.focusedStopIndex, suggestion)
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
fun LocationSuggestionItem(
    suggestion: LocationSuggestion,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BoltLightGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (suggestion.type == "recent") Icons.Default.History else Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Gray
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.name ?: suggestion.displayName.split(",").firstOrNull() ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = suggestion.displayName,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Random distance to match image
        val dist = remember { "${(1..15).random()}.${(0..9).random()} km" }
        Text(
            text = dist,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
