package com.example.famekodriver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.media.RingtoneManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.VoiceCallHandler
import com.example.famekodriver.ui.theme.*
import com.example.famekodriver.core.network.NetworkClient
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    status: String,
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: (Int, String) -> Unit,
    viewModel: DriverMapViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val voiceCallHandler = remember { VoiceCallHandler { data -> viewModel.sendAudioData(data) } }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasLocationPermission = it }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasAudioPermission = it }

    var mapLibreMap by remember { mutableStateOf<MapLibreMap?>(null) }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    LaunchedEffect(status) {
        if (status != "APPROVED" && viewModel.isOnline) {
            viewModel.toggleOnlineStatus(online = false)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { 
                    viewModel.driverLatLng = LatLng(it.latitude, it.longitude)
                    mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 15.0))
                }
            }
            
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000
            ).build()
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(res: com.google.android.gms.location.LocationResult) {
                        res.lastLocation?.let { 
                            viewModel.updateDriverLocation(it.latitude, it.longitude, it.bearing)
                        }
                    }
                },
                context.mainLooper
            )
        }
    }

    // Sound Management
    val ringtone = remember {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        RingtoneManager.getRingtone(context, uri)
    }

    LaunchedEffect(viewModel.activeRequest, viewModel.incomingCall) {
        if (viewModel.activeRequest != null || viewModel.incomingCall != null) {
            ringtone?.play()
        } else {
            ringtone?.stop()
        }
    }

    LaunchedEffect(viewModel.ongoingCall, hasAudioPermission) {
        if (viewModel.ongoingCall != null) {
            if (hasAudioPermission) {
                voiceCallHandler.startRecording()
                voiceCallHandler.startPlayback()
            } else {
                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        } else {
            voiceCallHandler.stopRecording()
            voiceCallHandler.stopPlayback()
        }
    }

    LaunchedEffect(viewModel.isOnline, viewModel.currentDelivery) {
        val intent = Intent(context, LocationService::class.java).apply {
            action = if (viewModel.isOnline || viewModel.currentDelivery != null) {
                LocationService.ACTION_START
            } else {
                LocationService.ACTION_STOP
            }
        }
        if (viewModel.isOnline || viewModel.currentDelivery != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } else {
            context.stopService(intent)
        }
    }

    LaunchedEffect(viewModel.currentDelivery, viewModel.driverLatLng) {
        val delivery = viewModel.currentDelivery
        val driverPos = viewModel.driverLatLng
        if (delivery != null && driverPos != null) {
            val dest = if (delivery.status == DeliveryStatus.ASSIGNED || delivery.status == DeliveryStatus.ARRIVED) {
                LatLng(delivery.pickupLat ?: 0.0, delivery.pickupLng ?: 0.0)
            } else {
                LatLng(delivery.dropOffLat ?: 0.0, delivery.dropOffLng ?: 0.0)
            }
            if (dest.latitude != 0.0) {
                viewModel.calculateRoute(driverPos, dest)
            }
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (viewModel.isOnline || viewModel.currentDelivery != null) {
                    FloatingActionButton(
                        onClick = { viewModel.showSOSDialog = true },
                        containerColor = Color.Red,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Warning, stringResource(R.string.sos))
                    }
                }
                if (hasLocationPermission && viewModel.activeRequest == null) {
                    FloatingActionButton(
                        onClick = { viewModel.toggleVoice(!viewModel.isVoiceEnabled) },
                        containerColor = if (viewModel.isVoiceEnabled) BoltGreen else Color.White,
                        contentColor = if (viewModel.isVoiceEnabled) Color.White else Color.Gray,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            if (viewModel.isVoiceEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            null
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            @SuppressLint("MissingPermission")
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                loc?.let {
                                    val pos = LatLng(it.latitude, it.longitude)
                                    viewModel.driverLatLng = pos
                                    mapLibreMap?.animateCamera(CameraUpdateFactory.newLatLng(pos))
                                }
                            }
                        },
                        containerColor = Color.White,
                        contentColor = Color(0xFFFF6B35)
                    ) {
                        Icon(Icons.Default.MyLocation, stringResource(R.string.my_location))
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        val styleUrl = "https://api.tomtom.com/style/2/custom/style/dG9tdG9tQEBAZFVDV2NzZ09mRGhEaU9MdDsVGbKlskhOMbwzZ3vdhit8?key=${NetworkClient.TOMTOM_API_KEY}"
                        getMapAsync { map ->
                            mapLibreMap = map
                            map.setStyle(styleUrl)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(5.6037, -0.1870), 12.0))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    val map = mapLibreMap ?: return@AndroidView
                    map.clear()
                    
                    // Driver Marker
                    viewModel.driverLatLng?.let { pos ->
                        val carBitmap = ContextCompat.getDrawable(context, R.drawable.ic_car_saloon)?.toBitmap()
                        val carIcon = carBitmap?.let { 
                            val matrix = android.graphics.Matrix()
                            matrix.postRotate(viewModel.driverBearing)
                            val scaled = Bitmap.createScaledBitmap(it, 40, 40, false)
                            val rotated = Bitmap.createBitmap(scaled, 0, 0, scaled.width, scaled.height, matrix, true)
                            IconFactory.getInstance(context).fromBitmap(rotated) 
                        }

                        map.addMarker(MarkerOptions()
                            .position(pos)
                            .apply { if (carIcon != null) icon(carIcon) }
                        )
                    }
                    
                    viewModel.heatmapPoints.forEach { point ->
                        val circlePoints = createCirclePoints(LatLng(point.latitude, point.longitude), 300.0)
                        map.addPolygon(PolygonOptions()
                            .addAll(circlePoints)
                            .fillColor(Color(1f, 0f, 0f, (point.intensity * 0.5).toFloat()).toArgb())
                            .strokeColor(Color.Transparent.toArgb())
                        )
                    }

                    if (viewModel.navigationPath.isNotEmpty()) {
                        map.addPolyline(PolylineOptions()
                            .addAll(viewModel.navigationPath)
                            .color("#004E89".toColorInt())
                            .width(5f)
                        )
                        
                        map.addMarker(MarkerOptions()
                            .position(viewModel.navigationPath.last())
                            .title(if (viewModel.currentDelivery?.status == DeliveryStatus.ASSIGNED) {
                                "Pickup"
                            } else {
                                "Destination"
                            })
                        )
                    }
                }
            )

            // Floating Menu Button
            FloatingActionButton(
                onClick = onNavigateToMenu,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart)
                    .statusBarsPadding(),
                containerColor = Color.White,
                contentColor = Color.DarkGray,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Menu, stringResource(R.string.menu))
            }

            // Overlays
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .statusBarsPadding()
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.currentSurge?.let { 
                    if (it.isActive) SurgeIndicator(multiplier = it.multiplier) 
                }
                val delivery = viewModel.currentDelivery
                if (viewModel.navigationPath.isNotEmpty() && delivery != null) {
                    NavigationHUD(delivery = delivery)
                }
            }

            // Bottom Sheets
            viewModel.currentDelivery?.let { delivery ->
                DeliveryControlSheet(
                    delivery = delivery,
                    onCall = { viewModel.initiateCall() },
                    onChat = { onNavigateToChat(delivery.orderId, delivery.customerName ?: "Customer") },
                    onStatusUpdate = { viewModel.updateDeliveryStatus(it) },
                    onCancel = { viewModel.cancelActiveTrip() },
                    onArrived = { viewModel.showPinDialog = true }
                )
            }

            viewModel.activeRequest?.let { request ->
                IncomingRequestSheet(
                    request = request,
                    timerProgress = viewModel.requestTimer / 30f,
                    onAccept = { viewModel.acceptDelivery() },
                    onCancel = { viewModel.rejectDelivery() },
                    isAccepting = viewModel.isAccepting
                )
            }

            if (viewModel.currentDelivery == null && viewModel.activeRequest == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Color.White, 
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    if (status != "APPROVED") {
                        RegistrationNotice(
                            status = status,
                            onGoToProfile = onNavigateToProfile
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Today's Earnings", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("GH₵${viewModel.finalFare.toInt()}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = BoltDark)
                        }
                        
                        if (viewModel.isDailyFeePaid && viewModel.dailyFeeRemainingSeconds > 0) {
                            DailyFeeCountdown(secondsRemaining = viewModel.dailyFeeRemainingSeconds)
                        }
                    }

                    OnlineToggleButton(
                        isOnline = viewModel.isOnline,
                        isApproved = status == "APPROVED",
                        onClick = { viewModel.checkAndGoOnline(status) }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Paystack WebView Overlay
            viewModel.payDailyFeeCheckoutUrl?.let { url ->
                Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
                    PaystackWebViewScreen(
                        url = url,
                        onBack = { viewModel.payDailyFeeCheckoutUrl = null },
                        onSuccess = {
                            viewModel.payDailyFeeCheckoutUrl = null
                            Toast.makeText(context, "Payment processing...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            // Dialogs
            if (viewModel.showBatteryOptimizationDialog) {
                BatteryOptimizationDialog(
                    onConfirm = { 
                        viewModel.toggleOnlineStatus(true)
                        viewModel.showBatteryOptimizationDialog = false 
                    },
                    onDismiss = { viewModel.showBatteryOptimizationDialog = false }
                )
            }
            if (viewModel.showPaymentDialog) {
                PaymentDialog(
                    amount = viewModel.dailyFeeAmount,
                    onPay = { viewModel.payDailyFee() },
                    onDismiss = { viewModel.showPaymentDialog = false },
                    isPaying = viewModel.isPaying
                )
            }
            if (viewModel.showPinDialog && viewModel.currentDelivery != null) {
                PinVerificationDialog(
                    pin = viewModel.enteredPin,
                    onPinChange = { viewModel.enteredPin = it },
                    onVerify = { viewModel.verifyPin() },
                    onDismiss = { viewModel.showPinDialog = false },
                    isVerifying = viewModel.isVerifyingPin
                )
            }
            if (viewModel.showSOSDialog) {
                SOSDialog(
                    sosType = viewModel.sosType,
                    onTypeSelect = { viewModel.sosType = it },
                    otherType = viewModel.otherSOSType,
                    onOtherChange = { viewModel.otherSOSType = it },
                    onSend = { _, _ -> 
                        viewModel.driverLatLng?.let { 
                            viewModel.triggerSOS(it.latitude, it.longitude) 
                        }
                    },
                    onDismiss = { viewModel.showSOSDialog = false },
                    isSending = viewModel.isSendingSOS
                )
            }
            
            viewModel.incomingCall?.let { call -> 
                IncomingCallDialog(
                    call = call,
                    onAccept = { viewModel.acceptCall(call.callId) },
                    onReject = { viewModel.rejectCall(call.callId) }
                )
            }
            viewModel.ongoingCall?.let { call -> 
                CallOverlay(
                    call = call,
                    onEnd = { viewModel.endCall(call.callId) }
                )
            }

            if (viewModel.showTripSummary) {
                TripSummaryDialog(
                    totalFare = viewModel.finalTotalFare,
                    earnings = viewModel.finalFare,
                    isRental = viewModel.isFinalTripRental,
                    onDismiss = { viewModel.showTripSummary = false }
                )
            }
        }
    }
}

private fun createCirclePoints(center: LatLng, radiusInMeters: Double): List<LatLng> {
    val points = mutableListOf<LatLng>()
    val distanceRadians = radiusInMeters / 6371000.0 // earth radius
    val centerLatRadians = Math.toRadians(center.latitude)
    val centerLonRadians = Math.toRadians(center.longitude)
    for (i in 0 until 360 step 10) {
        val bearingRadians = Math.toRadians(i.toDouble())
        val pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distanceRadians) +
                Math.cos(centerLatRadians) * Math.sin(distanceRadians) * Math.cos(bearingRadians))
        val pointLonRadians = centerLonRadians + Math.atan2(Math.sin(bearingRadians) * Math.sin(distanceRadians) * Math.cos(centerLatRadians),
                Math.cos(distanceRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians))
        points.add(LatLng(Math.toDegrees(pointLatRadians), Math.toDegrees(pointLonRadians)))
    }
    points.add(points[0])
    return points
}

@Composable
fun TripSummaryDialog(
    totalFare: Double,
    earnings: Double,
    isRental: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Trip Completed",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = BoltDark
                )
                Text(
                    "Great job! Here's your summary.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = BoltGreen,
                    modifier = Modifier.size(72.dp)
                )
                Spacer(Modifier.height(24.dp))
                
                Surface(
                    color = BoltLightGray,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (isRental) {
                            SummaryRow("Total Trip Fare", "GH₵${totalFare.toInt()}")
                            SummaryRow("Fameko Commission", "-GH₵${(totalFare - earnings).toInt()}")
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(if (isRental) "Your Net Earnings" else "Total Earnings", fontWeight = FontWeight.Bold, color = BoltDark)
                            Text("GH₵${earnings.toInt()}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BoltGreen)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FamekoPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("BACK TO MAP", fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BoltDark)
    }
}

@Composable
fun SurgeIndicator(multiplier: Double) {
    Surface(
        color = Color(0xFFFF6B35),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${multiplier}x Surge Active!",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun NavigationHUD(delivery: Delivery) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, BoltLightGray)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(FamekoLightBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (delivery.status == DeliveryStatus.ASSIGNED) Icons.Default.MyLocation else Icons.Default.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = FamekoPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (delivery.status == DeliveryStatus.ASSIGNED) {
                        "${delivery.pickupLocation.split(",").first()} → Pickup"
                    } else {
                        "Heading to → ${delivery.dropOffLocation.split(",").first()}"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = FamekoPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Surface(
                color = BoltGreen,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", delivery.distanceKm)} km",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun DeliveryControlSheet(
    delivery: Delivery,
    onCall: () -> Unit,
    onChat: () -> Unit,
    onStatusUpdate: (DeliveryStatus) -> Unit,
    onCancel: () -> Unit,
    onArrived: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(12.dp),
        border = BorderStroke(1.dp, BoltLightGray)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = BoltLightGray,
                    modifier = Modifier.size(64.dp)
                ) {
                    if (!delivery.customerProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = delivery.customerProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(16.dp), tint = Color.Gray)
                    }
                }
                
                Spacer(Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    val statusText = when (delivery.status) {
                        DeliveryStatus.ASSIGNED -> "PICKING UP"
                        DeliveryStatus.IN_TRANSIT -> "ON TRIP"
                        else -> delivery.status.name
                    }
                    Surface(
                        color = if (delivery.status == DeliveryStatus.ASSIGNED) BoltGreen.copy(alpha = 0.1f) else FamekoLightBlue,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = statusText,
                            fontWeight = FontWeight.Black,
                            color = if (delivery.status == DeliveryStatus.ASSIGNED) BoltGreen else FamekoPrimary,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = delivery.customerName ?: "Customer",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 24.sp,
                        color = BoltDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(48.dp).background(FamekoLightBlue, CircleShape)
                    ) {
                        Icon(Icons.Default.Call, null, tint = FamekoPrimary, modifier = Modifier.size(20.dp))
                    }
                    IconButton(
                        onClick = onChat,
                        modifier = Modifier.size(48.dp).background(FamekoLightBlue, CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, null, tint = FamekoPrimary, modifier = Modifier.size(20.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Surface(
                color = BoltLightGray,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = if (delivery.status == DeliveryStatus.ASSIGNED) Icons.Default.MyLocation else Icons.Default.Navigation
                    val tint = if (delivery.status == DeliveryStatus.ASSIGNED) BoltGreen else BoltOrange
                    
                    Box(
                        modifier = Modifier.size(36.dp).background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = if (delivery.status == DeliveryStatus.ASSIGNED) "PICKUP LOCATION" else "DESTINATION",
                            fontSize = 10.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (delivery.status == DeliveryStatus.ASSIGNED) delivery.pickupLocation else delivery.dropOffLocation,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BoltDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.LightGray)
                ) {
                    Text("Cancel Trip", fontWeight = FontWeight.Bold, color = BoltDark)
                }
                
                Button(
                    onClick = {
                        if (delivery.status == DeliveryStatus.ASSIGNED) onArrived()
                        else onStatusUpdate(DeliveryStatus.DELIVERED)
                    },
                    modifier = Modifier.weight(1.5f).height(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (delivery.status == DeliveryStatus.ASSIGNED) BoltGreen else FamekoPrimary)
                ) {
                    Text(
                        text = if (delivery.status == DeliveryStatus.ASSIGNED) "I'VE ARRIVED" else "COMPLETE TRIP",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ActionIcon(icon: ImageVector, color: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.background(Color(0xFFF0F0F0), CircleShape)
    ) {
        Icon(icon, null, tint = color)
    }
}

@Composable
fun IncomingRequestSheet(
    request: Delivery,
    timerProgress: Float,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    isAccepting: Boolean
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(16.dp),
        border = BorderStroke(1.dp, BoltLightGray)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(BoltLightGray, CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(timerProgress)
                        .fillMaxHeight()
                        .background(BoltOrange, CircleShape)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = "New Ride Request",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = BoltDark
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = BoltLightGray,
                    modifier = Modifier.size(48.dp)
                ) {
                    if (!request.customerProfilePic.isNullOrEmpty()) {
                        AsyncImage(
                            model = request.customerProfilePic,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.padding(12.dp), tint = Color.Gray)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = request.customerName ?: "Customer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BoltDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = FamekoLightBlue,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Schedule, null, modifier = Modifier.size(18.dp), tint = FamekoPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${request.pickupEtaMin?.toInt() ?: 5} min away",
                            fontWeight = FontWeight.Bold,
                            color = FamekoPrimary,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BoltLightGray, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LocationRow(icon = Icons.Default.MyLocation, color = BoltGreen, address = request.pickupLocation)
                Box(modifier = Modifier.padding(start = 7.dp).width(2.dp).height(12.dp).background(Color.LightGray))
                LocationRow(icon = Icons.Default.Navigation, color = BoltOrange, address = request.dropOffLocation)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .size(64.dp)
                        .background(Color(0xFFFFF1F0), RoundedCornerShape(16.dp))
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.Red)
                }
                
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BoltGreen),
                    enabled = !isAccepting
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = "Accept Ride",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LocationRow(icon: ImageVector, color: Color, address: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(address, fontSize = 14.sp, maxLines = 1)
    }
}

@Composable
fun DailyFeeCountdown(secondsRemaining: Long) {
    val hours = secondsRemaining / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val seconds = secondsRemaining % 60
    
    val timeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    
    val backgroundColor = when {
        secondsRemaining < 3600 -> Color.Red // Less than 1 hour
        secondsRemaining < 7200 -> Color(0xFFFFA500) // Less than 2 hours (Orange)
        else -> Color(0xFF28A745) // More than 2 hours (Green)
    }
    
    Surface(
        color = backgroundColor.copy(alpha = 0.9f),
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.Timer,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Active for: $timeText",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun OnlineToggleButton(isOnline: Boolean, isApproved: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = ButtonDefaults.buttonElevation(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOnline) Color.Red else if (isApproved) BoltGreen else Color.Gray
        )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isOnline) Icons.Default.PowerSettingsNew else Icons.Default.FlashOn,
                contentDescription = null,
                tint = Color.White
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = if (isOnline) "GO OFFLINE" else "GO ONLINE",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun BatteryOptimizationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_battery_opt_title)) },
        text = { Text(stringResource(R.string.dialog_battery_opt_text)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.btn_disable_optimization))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_do_it_later))
            }
        }
    )
}

@Composable
fun PaymentDialog(
    amount: Double,
    onPay: () -> Unit,
    onDismiss: () -> Unit,
    isPaying: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_daily_fee_title)) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_daily_fee_text, amount.toInt()))
                Spacer(Modifier.height(8.dp))
                Text("You can pay via Mobile Money or Card.", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(
                onClick = onPay,
                enabled = !isPaying,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
            ) {
                if (isPaying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.btn_pay_go_live))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isPaying) {
                Text(stringResource(R.string.later))
            }
        }
    )
}

@Composable
fun PinVerificationDialog(
    pin: String,
    onPinChange: (String) -> Unit,
    onVerify: () -> Unit,
    onDismiss: () -> Unit,
    isVerifying: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dialog_pin_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_pin_text))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = { Text(stringResource(R.string.label_4_digit_pin)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onVerify,
                enabled = pin.length == 4 && !isVerifying,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745))
            ) {
                if (isVerifying) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.btn_verify_start))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isVerifying) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun SOSDialog(
    sosType: String,
    onTypeSelect: (String) -> Unit,
    otherType: String,
    onOtherChange: (String) -> Unit,
    onSend: (Double, Double) -> Unit,
    onDismiss: () -> Unit,
    isSending: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Warning, null, tint = Color.Red, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text("Emergency SOS", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = BoltDark)
            }
        },
        text = {
            Column {
                Text("Please select the type of emergency. Security and dispatch will be notified immediately.", textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { expanded = true }, 
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color.LightGray)
                    ) {
                        Text(
                            text = if (sosType.isEmpty()) "Select Emergency Type" else sosType,
                            color = if (sosType.isEmpty()) Color.Gray else BoltDark,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = expanded, 
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                    ) {
                        listOf("Accident", "Robbery/Threat", "Vehicle Breakdown", "Medical Emergency", "Other").forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type, fontWeight = FontWeight.Medium) },
                                onClick = {
                                    onTypeSelect(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                if (sosType == "Other") {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = otherType,
                        onValueChange = onOtherChange,
                        placeholder = { Text("Specify emergency details...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(0.0, 0.0) },
                enabled = !isSending,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("SEND SOS NOW", fontWeight = FontWeight.Black)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth(), enabled = !isSending) {
                Text("Cancel", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun IncomingCallDialog(call: FamekoEvent.IncomingCall, onAccept: () -> Unit, onReject: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text("Incoming Call", fontWeight = FontWeight.ExtraBold, color = FamekoPrimary) },
        text = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Surface(shape = CircleShape, color = BoltLightGray, modifier = Modifier.size(80.dp)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.padding(20.dp), tint = BoltDark)
                }
                Spacer(Modifier.height(16.dp))
                Text("Customer ${call.callerName} is calling you regarding the active trip.", textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept, 
                colors = ButtonDefaults.buttonColors(containerColor = FamekoSecondary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Call, null)
                Spacer(Modifier.width(8.dp))
                Text("Accept")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Reject")
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White
    )
}

@Composable
fun CallOverlay(call: FamekoEvent.IncomingCall, onEnd: () -> Unit) {
    var callDuration by remember { mutableIntStateOf(0) }
    val isConnecting = call.callId == "pending"
    
    LaunchedEffect(isConnecting) {
        if (!isConnecting) {
            while (true) {
                delay(1000L)
                callDuration++
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(64.dp))
            
            val scale = if (!isConnecting) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                ).value
            } else {
                1f
            }
            
            Surface(
                shape = CircleShape,
                color = FamekoSecondary.copy(alpha = 0.2f),
                modifier = Modifier.size(140.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (!isConnecting) {
                        Surface(
                            shape = CircleShape, 
                            color = FamekoSecondary.copy(alpha = 0.1f), 
                            modifier = Modifier.size(140.dp * scale)
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(80.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = call.callerName,
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (isConnecting) "Connecting..." else {
                    val mins = callDuration / 60
                    val secs = callDuration % 60
                    "In-app Call • %02d:%02d".format(Locale.US, mins, secs)
                },
                color = if (isConnecting) Color.Gray else FamekoSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.Default.MicOff, null, tint = Color.White)
                    }
                    Text(
                        text = "Mute",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                
                FloatingActionButton(
                    onClick = onEnd,
                    containerColor = Color(0xFFDC3545),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(80.dp)
                ) {
                    Icon(Icons.Default.CallEnd, null, modifier = Modifier.size(36.dp))
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.1f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, null, tint = Color.White)
                    }
                    Text(
                        text = "Speaker",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationNotice(status: String, onGoToProfile: () -> Unit) {
    val context = LocalContext.current
    val isSuspended = status == "SUSPENDED"
    val containerColor = if (isSuspended) Color(0xFFF8D7DA) else Color(0xFFFFF3CD)
    val contentColor = if (isSuspended) Color(0xFF721C24) else Color(0xFF856404)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, contentColor)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            val title = when (status) {
                "PENDING_DOCS" -> stringResource(R.string.notice_docs_required)
                "SUSPENDED" -> stringResource(R.string.notice_suspended)
                "REJECTED" -> stringResource(R.string.notice_rejected)
                else -> stringResource(R.string.notice_pending_approval)
            }
            Text(text = title, fontWeight = FontWeight.Bold, color = contentColor)
            
            Spacer(modifier = Modifier.height(4.dp))
            
            val message = when (status) {
                "PENDING_DOCS" -> stringResource(R.string.msg_upload_docs)
                "SUSPENDED" -> stringResource(R.string.msg_contact_support)
                "REJECTED" -> stringResource(R.string.msg_app_rejected)
                else -> stringResource(R.string.msg_under_review)
            }
            Text(text = message, fontSize = 12.sp, color = contentColor)
            
            if (status == "PENDING_DOCS" || status == "REJECTED" || status == "SUSPENDED") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (status == "REJECTED" || status == "SUSPENDED") {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:0541234567"))
                            context.startActivity(intent)
                        } else {
                            onGoToProfile()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = contentColor)
                ) {
                    Text(
                        text = if (status == "REJECTED" || status == "SUSPENDED") {
                            stringResource(R.string.btn_contact_support)
                        } else {
                            stringResource(R.string.btn_go_to_profile)
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
