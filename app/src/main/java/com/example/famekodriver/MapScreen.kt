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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.utils.VoiceCallHandler
import com.example.famekodriver.ui.theme.*
import com.google.android.gms.location.LocationServices
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.*
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.Polygon
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

    LaunchedEffect(status) {
        if (status != "APPROVED" && viewModel.isOnline) {
            viewModel.toggleOnlineStatus(online = false)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { viewModel.driverLatLng = GeoPoint(it.latitude, it.longitude) }
            }
            
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 5000
            ).build()
            
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(res: com.google.android.gms.location.LocationResult) {
                        res.lastLocation?.let { viewModel.driverLatLng = GeoPoint(it.latitude, it.longitude) }
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
            val dest = if (delivery.status == DeliveryStatus.ASSIGNED) {
                GeoPoint(delivery.pickupLat ?: 0.0, delivery.pickupLng ?: 0.0)
            } else {
                GeoPoint(delivery.dropOffLat ?: 0.0, delivery.dropOffLng ?: 0.0)
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
                        onClick = {
                            @SuppressLint("MissingPermission")
                            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                                loc?.let {
                                    viewModel.driverLatLng = GeoPoint(it.latitude, it.longitude)
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
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                        controller.setZoom(12.0)
                        controller.setCenter(GeoPoint(5.6037, -0.1870))
                        if (hasLocationPermission) {
                            val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                            locationOverlay.enableMyLocation()
                            overlays.add(locationOverlay)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { mv ->
                    mv.overlays.removeAll { it is Polyline || (it is Marker && it.title != null) || it is Polygon }
                    viewModel.heatmapPoints.forEach { point ->
                        val circle = Polygon(mv)
                        circle.points = Polygon.pointsAsCircle(GeoPoint(point.latitude, point.longitude), 300.0)
                        circle.fillPaint.color = Color(1f, 0f, 0f, point.intensity.toFloat()).toArgb()
                        circle.outlinePaint.strokeWidth = 0f
                        mv.overlays.add(circle)
                    }
                    if (viewModel.navigationPath.isNotEmpty()) {
                        val line = Polyline(mv).apply {
                            setPoints(viewModel.navigationPath)
                            outlinePaint.color = "#004E89".toColorInt()
                            outlinePaint.strokeWidth = 12f
                        }
                        mv.overlays.add(line)
                        
                        val marker = Marker(mv).apply {
                            position = viewModel.navigationPath.last()
                            title = if (viewModel.currentDelivery?.status == DeliveryStatus.ASSIGNED) {
                                "Pickup"
                            } else {
                                "Destination"
                            }
                        }
                        mv.overlays.add(marker)
                    }
                    mv.invalidate()
                }
            )

            // Floating Menu Button
            FloatingActionButton(
                onClick = onNavigateToMenu,
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.TopStart),
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
                    onArrived = { viewModel.showPinDialog = true }
                )
            }

            viewModel.activeRequest?.let { request ->
                IncomingRequestSheet(
                    request = request,
                    timerProgress = viewModel.requestTimer / 30f,
                    onAccept = { viewModel.acceptDelivery() },
                    onIgnore = { viewModel.activeRequest = null },
                    isAccepting = viewModel.isAccepting
                )
            }

            if (viewModel.currentDelivery == null && viewModel.activeRequest == null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (status != "APPROVED") {
                        RegistrationNotice(
                            status = status,
                            onGoToProfile = onNavigateToProfile
                        )
                    }

                    if (viewModel.isDailyFeePaid && viewModel.dailyFeeRemainingSeconds > 0) {
                        DailyFeeCountdown(secondsRemaining = viewModel.dailyFeeRemainingSeconds)
                    }
                    
                    OnlineToggleButton(
                        isOnline = viewModel.isOnline,
                        isApproved = status == "APPROVED",
                        onClick = { viewModel.checkAndGoOnline(status) }
                    )
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
                    onDismiss = { viewModel.showTripSummary = false }
                )
            }
        }
    }
}

@Composable
fun TripSummaryDialog(
    totalFare: Double,
    earnings: Double,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Trip Completed",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Default.CheckCircle,
                    null,
                    tint = Color(0xFF28A745),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                
                SummaryRow("Total Fare", "₵${totalFare.toInt()}")
                SummaryRow("Commission (20%)", "-₵${(totalFare - earnings).toInt()}")
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF0F0F0))
                
                Text("Your Earnings", fontSize = 14.sp, color = Color.Gray)
                Text(
                    "₵${earnings.toInt()}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF28A745)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Back to Map", fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp),
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
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF004E89),
        contentColor = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Navigation, null, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (delivery.status == DeliveryStatus.ASSIGNED) {
                        stringResource(R.string.heading_to_pickup)
                    } else {
                        stringResource(R.string.heading_to_destination)
                    },
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = if (delivery.status == DeliveryStatus.ASSIGNED) {
                        delivery.pickupLocation
                    } else {
                        delivery.dropOffLocation
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${String.format(Locale.getDefault(), "%.1f", delivery.distanceKm)} km",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Text(
                    text = stringResource(R.string.remaining),
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.7f)
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
    onArrived: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val statusText = when (delivery.status) {
                        DeliveryStatus.ASSIGNED -> stringResource(R.string.status_going_to_pickup)
                        DeliveryStatus.IN_TRANSIT -> stringResource(R.string.status_trip_in_progress)
                        else -> delivery.status.name
                    }
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B35),
                        fontSize = 12.sp
                    )
                    Text(
                        text = delivery.customerName ?: stringResource(R.string.customer),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
                Row {
                    ActionIcon(icon = Icons.Default.Call, color = Color(0xFF28A745), onClick = onCall)
                    Spacer(modifier = Modifier.width(8.dp))
                    ActionIcon(icon = Icons.AutoMirrored.Filled.Chat, color = Color(0xFF004E89), onClick = onChat)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF0F0F0))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val icon = if (delivery.status == DeliveryStatus.ASSIGNED) {
                    Icons.Default.MyLocation
                } else {
                    Icons.Default.Navigation
                }
                val tint = if (delivery.status == DeliveryStatus.ASSIGNED) {
                    Color(0xFF34D186)
                } else {
                    Color(0xFFDC3545)
                }
                Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (delivery.status == DeliveryStatus.ASSIGNED) {
                            stringResource(R.string.pickup)
                        } else {
                            stringResource(R.string.destination)
                        },
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = if (delivery.status == DeliveryStatus.ASSIGNED) {
                            delivery.pickupLocation
                        } else {
                            delivery.dropOffLocation
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (delivery.status == DeliveryStatus.ASSIGNED) {
                        onArrived()
                    } else {
                        onStatusUpdate(DeliveryStatus.DELIVERED)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF004E89))
            ) {
                Text(
                    text = if (delivery.status == DeliveryStatus.ASSIGNED) {
                        stringResource(R.string.btn_arrived_at_pickup)
                    } else {
                        stringResource(R.string.btn_complete_trip)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
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
    onIgnore: () -> Unit,
    isAccepting: Boolean
) {
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { timerProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(bottom = 12.dp),
                color = Color(0xFFFF6B35),
                trackColor = Color(0xFFF0F0F0)
            )
            Text(
                text = stringResource(R.string.new_delivery_request),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "₵${request.estimatedEarnings.toInt()}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = Color(0xFF28A745)
                    )
                    Text(
                        text = stringResource(R.string.estimated_earnings),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = request.customerName ?: stringResource(R.string.customer),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Customer",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Surface(color = Color(0xFFFFEAD1), shape = RoundedCornerShape(12.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFFE67E22)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.min_away, request.pickupEtaMin?.toInt() ?: 5),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE67E22)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            LocationRow(icon = Icons.Default.MyLocation, color = Color(0xFF34D186), address = request.pickupLocation)
            Spacer(modifier = Modifier.height(8.dp))
            LocationRow(icon = Icons.Default.Navigation, color = Color(0xFFDC3545), address = request.dropOffLocation)
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = stringResource(R.string.ignore), color = Color.Gray)
                }
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.5f)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28A745)),
                    enabled = !isAccepting
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(
                            text = stringResource(R.string.btn_accept),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
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
            .size(90.dp),
        shape = CircleShape,
        elevation = ButtonDefaults.buttonElevation(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isOnline) Color.Red else if (isApproved) Color(0xFF28A745) else Color.Gray
        )
    ) {
        Text(
            text = if (isOnline) stringResource(R.string.btn_off) else stringResource(R.string.btn_go),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = Color.White
        )
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
        title = { Text(stringResource(R.string.dialog_sos_title), color = Color.Red, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_sos_text))
                Spacer(Modifier.height(16.dp))
                var expanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (sosType.isEmpty()) stringResource(R.string.label_select_type) else sosType)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        listOf(
                            "Accident",
                            "Robbery/Threat",
                            "Vehicle Breakdown",
                            "Medical Emergency",
                            stringResource(R.string.label_other)
                        ).forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type) },
                                onClick = {
                                    onTypeSelect(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                if (sosType == stringResource(R.string.label_other)) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = otherType,
                        onValueChange = onOtherChange,
                        label = { Text(stringResource(R.string.label_specify_emergency)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(0.0, 0.0) },
                enabled = !isSending,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                if (isSending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Text(stringResource(R.string.btn_send_sos_now))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSending) {
                Text(stringResource(R.string.cancel))
            }
        }
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
