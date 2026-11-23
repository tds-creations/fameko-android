package com.example.famekodriver

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.scale
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.famekodriver.core.domain.model.*
import com.example.famekodriver.core.network.NetworkClient
import com.example.famekodriver.core.ui.LocationDisclosureDialog
import com.example.famekodriver.core.utils.ImageLinks
import com.example.famekodriver.core.utils.LocationUtils
import com.example.famekodriver.core.utils.MapCacheManager
import com.example.famekodriver.core.utils.VoiceCallHandler
import com.example.famekodriver.ui.theme.*
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*
import kotlin.time.Duration.Companion.seconds

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    status: String,
    vehicleTypeFromSession: String = "",
    onNavigateToMenu: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToChat: (Int, String) -> Unit,
    onNavigateToEarnings: () -> Unit = {},
    viewModel: DriverMapViewModel = viewModel()
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val voiceCallHandler = remember { VoiceCallHandler { data -> viewModel.sendAudioData(data) } }

    LaunchedEffect(Unit) {
        viewModel.fetchDriverStatus()
    }

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
    var isCameraAnimating by remember { mutableStateOf(false) }
    var pendingCameraPosition by remember { mutableStateOf<org.maplibre.android.camera.CameraPosition?>(null) }
    var isPaddingApplied by remember { mutableStateOf(false) }
    var showLocationDisclosure by remember { mutableStateOf(!hasLocationPermission) }

    if (showLocationDisclosure) {
        LocationDisclosureDialog(
            onConfirm = {
                showLocationDisclosure = false
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onDismiss = {
                showLocationDisclosure = false
            }
        )
    }

    var vehicleType by remember(vehicleTypeFromSession) { mutableStateOf(vehicleTypeFromSession.lowercase()) }

    var vehicleBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(vehicleType) {
        val type = vehicleType
        val isOkada = type.contains("okada") || type.contains("motorcycle") || type.contains("rider") || type.contains("bike") || type.contains("motorbike") || type.contains("motor") || type.contains("delivery")
        val isTruck = type.contains("truck") || type.contains("cargo") || type.contains("hauling")

        val markerSize = if (isTruck) 56 else if (isOkada) 48 else 52

        val localDrawable = if (isOkada) ContextCompat.getDrawable(context, R.drawable.ic_okada) else null
        if (localDrawable != null) {
            vehicleBitmap = localDrawable.toBitmap().scale(markerSize, markerSize)
        } else {
            val iconUrl = when {
                isTruck -> ImageLinks.IC_TRUCK
                isOkada -> ImageLinks.IC_OKADA
                type.contains("pragya") -> ImageLinks.IC_PRAGYA
                type.contains("bicycle") -> ImageLinks.IC_BICYCLE
                type.contains("aboboyaa") -> ImageLinks.IC_ABOBOYAA
                else -> ImageLinks.IC_CAR_SALOON
            }

            val loader = context.imageLoader
            val request = ImageRequest.Builder(context)
                .data(iconUrl)
                .build()
            
            try {
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                if (result != null) {
                    vehicleBitmap = result.toBitmap().scale(markerSize, markerSize)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        // Permission request is now handled by the disclosure dialog
    }

    LaunchedEffect(status) {
        if (status != "APPROVED" && viewModel.isOnline) {
            viewModel.toggleOnlineStatus(online = false)
        }
    }

    LaunchedEffect(hasLocationPermission, viewModel.isOnline, mapLibreMap, viewModel.isFullscreenMap) {
        if (!hasLocationPermission || mapLibreMap == null) return@LaunchedEffect

        if (viewModel.isFullscreenMap) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setMinUpdateIntervalMillis(500)
                .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return
                    if (location.latitude == 0.0 || location.longitude == 0.0) return

                    // Update driver location (raw + visual smoothing + road snapping)
                    val lastPos = viewModel.driverLatLng
                    val lastBearing = viewModel.driverBearing
                    val distanceMoved = if (lastPos != null) LocationUtils.calculateDistance(location.latitude, location.longitude, lastPos.latitude, lastPos.longitude) else 10.0
                    val effectiveBearing = when {
                        location.hasBearing() && location.speed > 0.5 -> location.bearing
                        lastPos != null && distanceMoved > 1.0 -> {
                            val results = FloatArray(2)
                            android.location.Location.distanceBetween(lastPos.latitude, lastPos.longitude, location.latitude, location.longitude, results)
                            val calc = results[1]
                            if (calc != 0f) calc else lastBearing
                        }
                        else -> lastBearing
                    }

                    viewModel.updateDriverLocation(location.latitude, location.longitude, effectiveBearing, context)

                    // Live Camera Following Controller (Latest-Target-Wins Animation)
                    mapLibreMap?.let { map ->
                        if (viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.FOLLOWING) {
                            if (!isPaddingApplied) {
                                @Suppress("DEPRECATION")
                                map.setPadding(0, 0, 0, (context.resources.displayMetrics.heightPixels * 0.30).toInt())
                                isPaddingApplied = true
                            }

                            val targetPos = viewModel.visualDriverLatLng ?: viewModel.rawDriverLatLng ?: LatLng(location.latitude, location.longitude)
                            val targetBearing = viewModel.visualDriverBearing.toDouble()
                            val speedKmh = maxOf(0.0, location.speed.toDouble() * 3.6)
                            val targetZoom = (18.0 - (speedKmh / 100.0)).coerceIn(15.0, 18.0)

                            val nextCamPos = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(targetPos)
                                .zoom(targetZoom)
                                .bearing(targetBearing)
                                .tilt(45.0)
                                .build()

                            pendingCameraPosition = nextCamPos

                            fun animateNextCameraPosition() {
                                val targetToAnimate = pendingCameraPosition ?: return
                                pendingCameraPosition = null
                                if (viewModel.navCameraMode != DriverMapViewModel.NavigationCameraMode.FOLLOWING) return

                                isCameraAnimating = true
                                map.animateCamera(
                                    CameraUpdateFactory.newCameraPosition(targetToAnimate),
                                    350,
                                    object : MapLibreMap.CancelableCallback {
                                        override fun onFinish() {
                                            isCameraAnimating = false
                                            if (viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.FOLLOWING && pendingCameraPosition != null) {
                                                animateNextCameraPosition()
                                            }
                                        }

                                        override fun onCancel() {
                                            isCameraAnimating = false
                                            if (viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.FOLLOWING && pendingCameraPosition != null) {
                                                animateNextCameraPosition()
                                            }
                                        }
                                    }
                                )
                            }

                            if (!isCameraAnimating) {
                                animateNextCameraPosition()
                            }
                        }
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
                awaitCancellation()
            } finally {
                fusedLocationClient.removeLocationUpdates(locationCallback)
                // Reset padding when exiting navigation
                mapLibreMap?.setPadding(0, 0, 0, 0)
            }
        } else {
            // Normal Mode or Background Updates
            @SuppressLint("MissingPermission")
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let { 
                    val center = LatLng(it.latitude, it.longitude)
                    viewModel.driverLatLng = center
                    mapLibreMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15.0))
                    
                    // Performance: Pre-fetch current city tiles for offline speed
                    MapCacheManager.prefetchArea(context, center, "DriverShiftArea")
                }
            }
            
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
            
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(res: LocationResult) {
                    res.lastLocation?.let { 
                        viewModel.updateDriverLocation(it.latitude, it.longitude, it.bearing)
                    }
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
                awaitCancellation()
            } finally {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            }
        }
    }

    // Deterministic Initial Camera Positioning (Prevents default city center / sea misalignment)
    LaunchedEffect(mapLibreMap, viewModel.isFullscreenMap, viewModel.driverLatLng != null) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val driverPos = viewModel.visualDriverLatLng ?: viewModel.driverLatLng ?: return@LaunchedEffect
        if (driverPos.latitude == 0.0 || driverPos.longitude == 0.0) return@LaunchedEffect

        if (viewModel.isFullscreenMap && viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.FOLLOWING) {
            if (!isPaddingApplied) {
                @Suppress("DEPRECATION")
                map.setPadding(0, 0, 0, (context.resources.displayMetrics.heightPixels * 0.30).toInt())
                isPaddingApplied = true
            }

            val targetCamPos = org.maplibre.android.camera.CameraPosition.Builder()
                .target(driverPos)
                .zoom(17.0)
                .bearing(viewModel.visualDriverBearing.toDouble())
                .tilt(45.0)
                .build()

            map.moveCamera(CameraUpdateFactory.newCameraPosition(targetCamPos))
        }
    }

    DisposableEffect(mapLibreMap, viewModel.isFullscreenMap) {
        val map = mapLibreMap
        if (map != null && viewModel.isFullscreenMap) {
            val moveStartedListener = MapLibreMap.OnCameraMoveStartedListener { reason ->
                if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE &&
                    viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.FOLLOWING) {
                    viewModel.navCameraMode = DriverMapViewModel.NavigationCameraMode.SUSPENDED
                    isPaddingApplied = false
                    @Suppress("DEPRECATION")
                    map.setPadding(0, 0, 0, 0)
                }
            }
            map.addOnCameraMoveStartedListener(moveStartedListener)
            onDispose {
                map.removeOnCameraMoveStartedListener(moveStartedListener)
            }
        } else {
            onDispose { }
        }
    }

    // Sound Management
    LaunchedEffect(viewModel.isOnline, viewModel.currentDelivery) {
        val intent = Intent(context, LocationService::class.java).apply {
            action = if (viewModel.isOnline || viewModel.currentDelivery != null) {
                LocationService.ACTION_START
            } else {
                LocationService.ACTION_STOP
            }
        }
        if (viewModel.isOnline || viewModel.currentDelivery != null) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.stopService(intent)
        }
    }

    LaunchedEffect(viewModel.currentDelivery?.status, viewModel.activeRequest?.status) {
        val delivery = viewModel.currentDelivery ?: viewModel.activeRequest
        if (delivery != null && delivery.status != DeliveryStatus.CANCELLED && delivery.status != DeliveryStatus.DELIVERED) {
            when (delivery.status) {
                DeliveryStatus.ASSIGNED -> {
                    // Driver heading to pickup location
                    val driverLat = viewModel.driverLatLng?.latitude ?: delivery.pickupLat ?: 0.0
                    val driverLng = viewModel.driverLatLng?.longitude ?: delivery.pickupLng ?: 0.0
                    val pLat = delivery.pickupLat ?: 0.0
                    val pLng = delivery.pickupLng ?: 0.0
                    if (driverLat != 0.0 && pLat != 0.0) {
                        viewModel.calculateRoute(LatLng(driverLat, driverLng), LatLng(pLat, pLng))
                    }
                }
                DeliveryStatus.ARRIVED -> {
                    // Driver arrived at pickup, waiting for PIN entry!
                    // Clear path so actual trip route is drawn only AFTER PIN verification
                    viewModel.clearRouteAndNavigation()
                }
                DeliveryStatus.IN_TRANSIT -> {
                    // PIN entered & verified! Draw actual trip route to destination
                    val pLat = delivery.pickupLat ?: 0.0
                    val pLng = delivery.pickupLng ?: 0.0
                    val dLat = delivery.dropOffLat ?: 0.0
                    val dLng = delivery.dropOffLng ?: 0.0
                    if (pLat != 0.0 && dLat != 0.0) {
                        viewModel.calculateRoute(LatLng(pLat, pLng), LatLng(dLat, dLng))
                    }
                }
                else -> {
                    viewModel.clearRouteAndNavigation()
                }
            }
        } else {
            viewModel.clearRouteAndNavigation()
        }
    }

    LaunchedEffect(viewModel.driverLatLng, viewModel.isOnline) {
        if (viewModel.isOnline && viewModel.driverLatLng != null && viewModel.activeRequest == null && viewModel.currentDelivery == null) {
            viewModel.refreshDeliveries()
        }
    }

    val activePolylineRef = remember { object { var value: org.maplibre.android.annotations.Polyline? = null } }
    val activeRouteMarkers = remember { mutableListOf<org.maplibre.android.annotations.Marker>() }

    val pickupIcon = remember(context) { createMarkerIcon(context, BoltGreen) }
    val destinationIcon = remember(context) { createMarkerIcon(context, BoltOrange) }

    // Physical Back Button Logic
    androidx.activity.compose.BackHandler(enabled = viewModel.isFullscreenMap) {
        viewModel.isFullscreenMap = false
    }

    LaunchedEffect(mapLibreMap, viewModel.driverLatLng, viewModel.driverBearing, vehicleBitmap) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val pos = viewModel.driverLatLng ?: return@LaunchedEffect
        val bitmap = vehicleBitmap ?: return@LaunchedEffect
        
        map.getStyle { style ->
            if (style.getImage("driver-icon") == null) {
                style.addImage("driver-icon", bitmap)
            }
            
            val source = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("driver-source")
            if (source != null) {
                val feature = org.maplibre.geojson.Feature.fromGeometry(
                    org.maplibre.geojson.Point.fromLngLat(pos.longitude, pos.latitude)
                )
                feature.addNumberProperty("bearing", viewModel.driverBearing)
                source.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeature(feature))
            }
        }
    }

    val activeHeatmapPolygons = remember { mutableListOf<org.maplibre.android.annotations.Polygon>() }

    LaunchedEffect(mapLibreMap, viewModel.heatmapPoints) {
        val map = mapLibreMap ?: return@LaunchedEffect
        
        // Offload point calculation to background thread to avoid UI jank
        val polygons = withContext(kotlinx.coroutines.Dispatchers.Default) {
            viewModel.heatmapPoints.map { point ->
                val circlePoints = createCirclePoints(LatLng(point.latitude, point.longitude))
                PolygonOptions()
                    .addAll(circlePoints)
                    .fillColor(Color(1f, 0f, 0f, (point.intensity * 0.5).toFloat()).toArgb())
                    .strokeColor(Color.Transparent.toArgb())
            }
        }

        @Suppress("DEPRECATION")
        activeHeatmapPolygons.forEach { map.removePolygon(it) }
        activeHeatmapPolygons.clear()

        polygons.forEach { options ->
            @Suppress("DEPRECATION")
            val polygon = map.addPolygon(options)
            activeHeatmapPolygons.add(polygon)
        }
    }

    LaunchedEffect(mapLibreMap, viewModel.remainingNavigationPath, viewModel.currentDelivery?.status, viewModel.activeRequest, pickupIcon, destinationIcon) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val path = viewModel.remainingNavigationPath.ifEmpty { viewModel.navigationPath }
        
        map.getStyle { style ->
            val source = style.getSourceAs<org.maplibre.android.style.sources.GeoJsonSource>("route-source")
            if (source != null) {
                if (path.isNotEmpty()) {
                    val feature = org.maplibre.geojson.Feature.fromGeometry(
                        org.maplibre.geojson.LineString.fromLngLats(
                            path.map { org.maplibre.geojson.Point.fromLngLat(it.longitude, it.latitude) }
                        )
                    )
                    source.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeature(feature))
                } else {
                    source.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeatures(emptyList()))
                }
            }
        }

        @Suppress("DEPRECATION")
        activeRouteMarkers.forEach { map.removeMarker(it) }
        activeRouteMarkers.clear()

        if (path.isNotEmpty()) {
            @Suppress("DEPRECATION")
            val currentStatus = viewModel.currentDelivery?.status ?: viewModel.activeRequest?.status
            val isPickup = currentStatus == DeliveryStatus.ASSIGNED || currentStatus == DeliveryStatus.PENDING || currentStatus == DeliveryStatus.ARRIVED
            
            @Suppress("DEPRECATION")
            val destMarker = map.addMarker(MarkerOptions()
                .position(path.last())
                .icon(if (isPickup) pickupIcon else destinationIcon)
                .title(if (isPickup) "Pickup" else "Destination")
            )
            activeRouteMarkers.add(destMarker)

            // Fit bounds if not in fullscreen nav
            if (!viewModel.isFullscreenMap) {
                try {
                    val bounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                        .include(viewModel.driverLatLng ?: path.first())
                        .include(path.last())
                        .build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } catch (_: Exception) {}
            }
        }
    }
    Scaffold(
        floatingActionButton = {
            Column(
                modifier = Modifier.padding(bottom = if (viewModel.activeRequest != null || viewModel.currentDelivery != null) 310.dp else 140.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                if (viewModel.isOnline || viewModel.currentDelivery != null) {
                    FloatingActionButton(
                        onClick = { viewModel.showSOSDialog = true },
                        containerColor = Color.Red,
                        contentColor = Color.White,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Warning, stringResource(R.string.sos), modifier = Modifier.size(20.dp))
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
                            if (viewModel.isVoiceEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    FloatingActionButton(
                        onClick = {
                            @SuppressLint("MissingPermission")
                            val currentPos = viewModel.driverLatLng
                            if (currentPos != null) {
                                mapLibreMap?.animateCamera(
                                    org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                        org.maplibre.android.camera.CameraPosition.Builder()
                                            .target(currentPos)
                                            .zoom(16.0)
                                            .build()
                                    ),
                                    1000
                                )
                            } else {
                                fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener { loc ->
                                    loc?.let {
                                        val pos = LatLng(it.latitude, it.longitude)
                                        viewModel.driverLatLng = pos
                                        mapLibreMap?.animateCamera(
                                            org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                                                org.maplibre.android.camera.CameraPosition.Builder()
                                                    .target(pos)
                                                    .zoom(16.0)
                                                    .build()
                                            ),
                                            1000
                                        )
                                    } ?: Toast.makeText(context, "Acquiring GPS location...", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        containerColor = Color.White,
                        contentColor = Color(0xFFFF6B35),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, stringResource(R.string.my_location), modifier = Modifier.size(20.dp))
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
                            map.setStyle(styleUrl) { style ->
                                // Initialize Route Line Source and Layer (GPU-Accelerated, Zero-Flicker)
                                if (style.getSource("route-source") == null) {
                                    val routeSource = org.maplibre.android.style.sources.GeoJsonSource("route-source")
                                    style.addSource(routeSource)
                                    val routeLayer = org.maplibre.android.style.layers.LineLayer("route-layer", "route-source").apply {
                                        withProperties(
                                            org.maplibre.android.style.layers.PropertyFactory.lineColor(android.graphics.Color.parseColor("#004E89")),
                                            org.maplibre.android.style.layers.PropertyFactory.lineWidth(6f),
                                            org.maplibre.android.style.layers.PropertyFactory.lineCap(org.maplibre.android.style.layers.Property.LINE_CAP_ROUND),
                                            org.maplibre.android.style.layers.PropertyFactory.lineJoin(org.maplibre.android.style.layers.Property.LINE_JOIN_ROUND)
                                        )
                                    }
                                    style.addLayer(routeLayer)
                                }

                                // Initialize Driver Marker Data Source and Layer
                                if (style.getSource("driver-source") == null) {
                                    style.addSource(org.maplibre.android.style.sources.GeoJsonSource("driver-source"))
                                    val layer = org.maplibre.android.style.layers.SymbolLayer("driver-layer", "driver-source")
                                    layer.withProperties(
                                        org.maplibre.android.style.layers.PropertyFactory.iconImage("driver-icon"),
                                        org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap(true),
                                        org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement(true),
                                        org.maplibre.android.style.layers.PropertyFactory.iconRotate(org.maplibre.android.style.expressions.Expression.get("bearing"))
                                    )
                                    style.addLayer(layer)
                                }
                            }
                            val initialTarget = viewModel.visualDriverLatLng ?: viewModel.driverLatLng ?: LatLng(5.6037, -0.1870)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(initialTarget, 15.0))
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { _ ->
                    // This block ensures the map stays in sync with Compose state
                    // and prevents markers from "disappearing" due to incorrect recycling.
                }
            )

            // Left Menu Floating FAB Button
            Surface(
                onClick = onNavigateToMenu,
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp)
                    .size(48.dp)
                    .align(Alignment.TopStart)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = stringResource(R.string.menu),
                        tint = Color(0xFF0F172A),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }



            // Surge / High Demand Floating Banner
            viewModel.currentSurge?.let { surge ->
                if (surge.isActive && viewModel.currentDelivery == null && viewModel.activeRequest == null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 80.dp, start = 16.dp, end = 16.dp)
                            .statusBarsPadding()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF0F172A),
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFF97316), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FlashOn, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("⚡ ${surge.multiplier}x Surge in ${surge.region ?: "Accra"}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                }
                                Text(surge.reason ?: "High ride demand in area", fontSize = 11.sp, color = Color.LightGray)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }

            if (viewModel.isOfflineMode) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFDC2626),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WifiOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Offline Mode — Navigating with cached route",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (viewModel.isFullscreenMap) {
                DriverNavigationOverlay(
                    instruction = viewModel.currentInstruction,
                    nextInstruction = viewModel.nextInstruction,
                    driverLatLng = viewModel.driverLatLng,
                    distanceKm = viewModel.distanceKm,
                    durationMin = viewModel.durationMin,
                    onOverviewClick = {
                        val map = mapLibreMap ?: return@DriverNavigationOverlay
                        if (viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.OVERVIEW) {
                            viewModel.navCameraMode = DriverMapViewModel.NavigationCameraMode.FOLLOWING
                            isPaddingApplied = false
                        } else {
                            viewModel.navCameraMode = DriverMapViewModel.NavigationCameraMode.OVERVIEW
                            isPaddingApplied = false
                            @Suppress("DEPRECATION")
                            map.setPadding(0, 0, 0, 0)
                            val path = viewModel.remainingNavigationPath.ifEmpty { viewModel.navigationPath }
                            if (path.isNotEmpty()) {
                                try {
                                    val bounds = org.maplibre.android.geometry.LatLngBounds.Builder()
                                        .include(viewModel.driverLatLng ?: path.first())
                                        .include(path.last())
                                        .build()
                                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                                } catch (_: Exception) {}
                            }
                        }
                    },
                    onExit = { viewModel.isFullscreenMap = false }
                )
            }

            if (viewModel.isFullscreenMap && viewModel.navCameraMode == DriverMapViewModel.NavigationCameraMode.SUSPENDED) {
                FloatingActionButton(
                    onClick = {
                        val map = mapLibreMap ?: return@FloatingActionButton
                        viewModel.navCameraMode = DriverMapViewModel.NavigationCameraMode.FOLLOWING
                        isPaddingApplied = false
                        @Suppress("DEPRECATION")
                        map.setPadding(0, 0, 0, (context.resources.displayMetrics.heightPixels * 0.30).toInt())
                        val targetPos = viewModel.visualDriverLatLng ?: viewModel.driverLatLng
                        if (targetPos != null) {
                            val cameraPosition = org.maplibre.android.camera.CameraPosition.Builder()
                                .target(targetPos)
                                .zoom(17.0)
                                .bearing(viewModel.visualDriverBearing.toDouble())
                                .tilt(45.0)
                                .build()
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 600)
                        }
                    },
                    containerColor = Color(0xFF0F172A),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .padding(bottom = 210.dp, end = 16.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Recenter", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Text("Recenter", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Color.White)
                    }
                }
            }

            // Bottom Sheets
            viewModel.currentDelivery?.let { delivery ->
                DeliveryControlSheet(
                    delivery = delivery,
                    durationMin = viewModel.durationMin,
                    distanceKm = viewModel.distanceKm,
                    isFullscreenMap = viewModel.isFullscreenMap,
                    onCall = { viewModel.initiateCall(context) },
                    onChat = { onNavigateToChat(delivery.orderId, delivery.customerName ?: "Customer") },
                    onStatusUpdate = { viewModel.updateDeliveryStatus(it) },
                    onCancel = { viewModel.cancelActiveTrip() },
                    onArrived = { viewModel.updateDeliveryStatus(DeliveryStatus.ARRIVED) },
                    onNavigateClick = { viewModel.isFullscreenMap = !viewModel.isFullscreenMap }
                )
            }

            AnimatedVisibility(
                visible = viewModel.activeRequest != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                viewModel.activeRequest?.let { request ->
                    IncomingRequestSheet(
                        request = request,
                        timerProgress = viewModel.requestTimer / 30f,
                        onAccept = { viewModel.acceptDelivery() },
                        onCancel = { viewModel.rejectDelivery() },
                        isAccepting = viewModel.isAccepting
                    )
                }
            }

            if (viewModel.currentDelivery == null && viewModel.activeRequest == null) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (status != "APPROVED") {
                            RegistrationNotice(
                                status = status,
                                onGoToProfile = onNavigateToProfile
                            )
                        }

                        // Row 1: Today's Earnings & Active Countdown
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.clickable { onNavigateToEarnings() }) {
                                Text("TODAY'S EARNINGS", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Spacer(Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("GH₵ ${String.format(Locale.US, "%.2f", viewModel.driverStats.earningsToday)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF0F172A))
                                    Spacer(Modifier.width(4.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                                }
                            }
                            
                            Column(horizontalAlignment = Alignment.End) {
                                if (viewModel.isDailyFeePaid && viewModel.dailyFeeRemainingSeconds > 0) {
                                    DailyFeeCountdown(secondsRemaining = viewModel.dailyFeeRemainingSeconds)
                                    Spacer(Modifier.height(4.dp))
                                    Text("Daily fee valid until 11:59 PM", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        // Row 2: 3 Driver Stats Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DriverStatItem("Acceptance", "${viewModel.driverStats.completionRate}%", Modifier.weight(1f))
                            DriverStatItem("Driver Rating", "${String.format(Locale.US, "%.2f", if (viewModel.driverStats.rating > 0) viewModel.driverStats.rating else 5.0)} ★", Modifier.weight(1f))
                            DriverStatItem("Trips Completed", "${viewModel.driverStats.completedToday}", Modifier.weight(1f))
                        }

                        // Row 3: Go Online / Go Offline Button
                        Button(
                            onClick = { viewModel.checkAndGoOnline(status) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (viewModel.isOnline) Color(0xFFDC2626) else Color(0xFF10B981)
                            ),
                            elevation = ButtonDefaults.buttonElevation(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.PowerSettingsNew, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (viewModel.isOnline) "GO OFFLINE" else "GO ONLINE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        Text(
                            text = if (viewModel.isOnline) "You are currently ONLINE. Ready to receive requests." 
                                   else "You are currently OFFLINE. Tap above to start receiving ride requests in Accra.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
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

private fun createMarkerIcon(context: android.content.Context, color: Color): org.maplibre.android.annotations.Icon {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    
    // Draw pin body
    paint.color = color.toArgb()
    val path = android.graphics.Path()
    path.moveTo(size / 2f, size.toFloat())
    path.lineTo(size * 0.2f, size * 0.4f)
    path.arcTo(size * 0.2f, 0f, size * 0.8f, size * 0.8f, 180f, 180f, false)
    path.lineTo(size / 2f, size.toFloat())
    path.close()
    
    // Simpler path for pin
    val pinPath = android.graphics.Path()
    pinPath.moveTo(size / 2f, size.toFloat())
    pinPath.lineTo(size * 0.15f, size * 0.4f)
    val rectF = android.graphics.RectF(size * 0.15f, 0f, size * 0.85f, size * 0.7f)
    pinPath.arcTo(rectF, 150f, 240f, false)
    pinPath.close()
    
    canvas.drawPath(pinPath, paint)

    // Draw inner circle
    paint.color = android.graphics.Color.WHITE
    canvas.drawCircle(size / 2f, size * 0.35f, size / 7f, paint)

    return IconFactory.getInstance(context).fromBitmap(bitmap)
}

private fun createCirclePoints(center: LatLng, radiusInMeters: Double = 300.0): List<LatLng> {
    val points = mutableListOf<LatLng>()
    val distanceRadians = radiusInMeters / 6371000.0 // earth radius
    val centerLatRadians = Math.toRadians(center.latitude)
    val centerLonRadians = Math.toRadians(center.longitude)
    for (i in 0 until 360 step 10) {
        val bearingRadians = Math.toRadians(i.toDouble())
        val pointLatRadians = asin(sin(centerLatRadians) * cos(distanceRadians) +
                cos(centerLatRadians) * sin(distanceRadians) * cos(bearingRadians))
        val pointLonRadians = centerLonRadians + atan2(sin(bearingRadians) * sin(distanceRadians) * cos(centerLatRadians),
                cos(distanceRadians) - sin(centerLatRadians) * sin(pointLatRadians))
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
    durationMin: Double = 0.0,
    distanceKm: Double = 0.0,
    isFullscreenMap: Boolean,
    onCall: () -> Unit,
    onChat: () -> Unit,
    onStatusUpdate: (DeliveryStatus) -> Unit,
    onCancel: () -> Unit,
    onArrived: () -> Unit,
    onNavigateClick: () -> Unit
) {
    if (isFullscreenMap) return // Don't show sheet in navigation mode

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(10.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Top Row: Profile Avatar + Rating Badge, Customer Info, Contact Action Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customer Avatar & Rating Badge Overlay
                Box {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEBF2FA),
                        modifier = Modifier.size(52.dp)
                    ) {
                        if (!delivery.customerProfilePic.isNullOrEmpty()) {
                            AsyncImage(
                                model = delivery.customerProfilePic,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            val initials = delivery.currentContactName
                                .split(" ")
                                .mapNotNull { it.firstOrNull()?.toString() }
                                .take(2)
                                .joinToString("")
                                .ifEmpty { "JL" }
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = initials,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }

                    // Gold Rating Badge
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFFB800),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text("★", fontSize = 9.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            Text("4.9", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                Spacer(Modifier.width(14.dp))

                // Customer Name & Trip Status Pill
                Column(modifier = Modifier.weight(1f)) {
                    val isPickup = delivery.status == DeliveryStatus.ASSIGNED || delivery.status == DeliveryStatus.PENDING
                    val statusPillText = when (delivery.status) {
                        DeliveryStatus.ASSIGNED -> "PICKUP"
                        DeliveryStatus.ARRIVED -> "ARRIVED"
                        DeliveryStatus.IN_TRANSIT -> "DROP-OFF"
                        else -> delivery.status.name
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFFEBF3FF),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = statusPillText,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF2563EB),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "• Cash Trip",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Gray
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = delivery.currentContactName,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Contact Actions: Call & Chat Icons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFEBF2FA),
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onCall() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Box {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEBF2FA),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { onChat() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Chat",
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Unread notification dot indicator
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(Color(0xFF2563EB), CircleShape)
                                .border(1.5.dp, Color.White, CircleShape)
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Embedded Location Card Box (Destination / Pickup + ETA Pill)
            val isPickup = delivery.status == DeliveryStatus.ASSIGNED || delivery.status == DeliveryStatus.PENDING
            val fullLocationStr = if (isPickup) delivery.pickupLocation else delivery.dropOffLocation
            val locationTitle = if (fullLocationStr.contains(",")) fullLocationStr.split(",").first() else fullLocationStr
            val locationSubtitle = if (fullLocationStr.contains(",")) fullLocationStr.substringAfter(",").trim() else "Accra, Ghana"

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isPickup) Color(0xFFECFDF5) else Color(0xFFFFEBEB),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = if (isPickup) Color(0xFF059669) else Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isPickup) "PICKUP LOCATION" else "DESTINATION",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        // Green ETA & Distance Pill
                        val formattedDuration = if (durationMin > 0) "${durationMin.toInt()} min" else "4 min"
                        val formattedDistance = if (distanceKm > 0) "${String.format(Locale.US, "%.1f", distanceKm)} km" else "1.2 km"

                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Text(
                                text = "$formattedDuration • $formattedDistance",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color(0xFF059669),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = locationTitle,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (locationSubtitle.isNotEmpty()) {
                        Text(
                            text = locationSubtitle,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Trip Type & Est. Fare Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (delivery.serviceType == ServiceType.PACKAGE_DELIVERY) "📦" else "🚘",
                        fontSize = 16.sp
                    )
                    Text(
                        text = if (delivery.serviceType == ServiceType.PACKAGE_DELIVERY) "Package Delivery" else "Ride Hailing Trip",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Est. Fare: ", fontSize = 13.sp, color = Color.Gray)
                    val fareVal = delivery.totalFare ?: delivery.estimatedEarnings
                    Text(
                        text = "GH₵ ${String.format(Locale.US, "%.2f", fareVal)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action Bar: Cancel (X), Navigate (Dark Navy), Complete / Arrived (Blue)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Trip Button (X icon)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFCA5A5).copy(alpha = 0.5f)),
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onCancel() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel Trip",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Navigate Button (Dark Navy)
                Button(
                    onClick = onNavigateClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NearMe,
                            contentDescription = "Navigate",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Navigate",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }

                // Primary Action Button (ARRIVED / COMPLETE)
                Button(
                    onClick = {
                        if (isPickup) onArrived()
                        else onStatusUpdate(DeliveryStatus.DELIVERED)
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPickup) Color(0xFF10B981) else Color(0xFF2563EB)
                    ),
                    elevation = ButtonDefaults.buttonElevation(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isPickup) "ARRIVED" else "COMPLETE",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DriverNavigationOverlay(
    instruction: RouteInstruction?,
    nextInstruction: RouteInstruction? = null,
    driverLatLng: LatLng?,
    distanceKm: Double,
    durationMin: Double,
    onOverviewClick: () -> Unit = {},
    onExit: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Top Instruction Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .statusBarsPadding(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val mType = instruction?.maneuverType ?: ManeuverType.STRAIGHT
                    val icon = when (mType) {
                        ManeuverType.TURN_LEFT, ManeuverType.SLIGHT_LEFT, ManeuverType.SHARP_LEFT, ManeuverType.RAMP_LEFT, ManeuverType.KEEP_LEFT -> Icons.AutoMirrored.Filled.ArrowBack
                        ManeuverType.TURN_RIGHT, ManeuverType.SLIGHT_RIGHT, ManeuverType.SHARP_RIGHT, ManeuverType.RAMP_RIGHT, ManeuverType.KEEP_RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
                        ManeuverType.UTURN -> Icons.AutoMirrored.Filled.Undo
                        ManeuverType.ROUNDABOUT -> Icons.Default.Autorenew
                        ManeuverType.ARRIVE -> Icons.Default.Place
                        else -> Icons.Default.Navigation
                    }

                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(Color(0xFF1E293B), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        val distanceToManeuver = if (instruction != null && driverLatLng != null) {
                            val instrLat = instruction.point?.getOrNull(1) ?: 0.0
                            val instrLng = instruction.point?.getOrNull(0) ?: 0.0
                            val dist = LocationUtils.calculateDistance(
                                driverLatLng.latitude,
                                driverLatLng.longitude,
                                instrLat,
                                instrLng
                            )
                            if (dist > 1000) String.format(Locale.US, "In %.1f km", dist / 1000.0)
                            else "In ${dist.toInt()} m"
                        } else "Next turn"

                        Text(
                            text = distanceToManeuver,
                            color = Color(0xFF10B981),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = instruction?.text ?: "Follow the path",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                val nextText = nextInstruction?.text
                if (!nextText.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "THEN: ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                        Text(
                            text = nextText,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Bottom Stats
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .padding(20.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", distanceKm)} km",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = BoltDark
                    )
                    Text("REMAINING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val arrivalTime = SimpleDateFormat("HH:mm", Locale.US).format(Date(System.currentTimeMillis() + (durationMin * 60000).toLong()))
                    Text(
                        text = arrivalTime,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = FamekoPrimary
                    )
                    Text("ARRIVAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${durationMin.toInt()} min",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = BoltGreen
                    )
                    Text("ESTIMATED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onOverviewClick,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, Color(0xFF0F172A))
                ) {
                    Icon(Icons.Default.Map, null, tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("OVERVIEW", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF0F172A))
                }

                Button(
                    onClick = onExit,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("EXIT NAV", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun TopMapStatusBar(
    isOnline: Boolean,
    city: String,
    earningsToday: Double,
    isSoundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onNavigateToMenu: () -> Unit,
    onNavigateToEarnings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Pill: Online/Offline Status & City + Menu trigger
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp,
            modifier = Modifier.clickable { onNavigateToMenu() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(if (isOnline) Color(0xFF10B981) else Color(0xFFEF4444), CircleShape)
                )
                Text(
                    text = "${if (isOnline) "ONLINE" else "OFFLINE"} • ${city.uppercase()}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = Color.White
                )
            }
        }

        // Center Button: Sound Notification Control
        Surface(
            onClick = onToggleSound,
            shape = CircleShape,
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSoundEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = "Sound Control",
                    tint = if (isSoundEnabled) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Right Pill: Today's Earnings
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            shadowElevation = 6.dp,
            modifier = Modifier.clickable { onNavigateToEarnings() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "TODAY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "GH₵ ${String.format(Locale.US, "%.2f", earningsToday)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = Color(0xFF10B981)
                )
            }
        }
    }
}

@Composable
fun IncomingRequestSheet(
    request: Delivery,
    timerProgress: Float,
    onAccept: () -> Unit,
    onCancel: () -> Unit,
    isAccepting: Boolean,
    modifier: Modifier = Modifier
) {
    val remainingSec = (timerProgress * 30).toInt().coerceIn(0, 30)
    val isDelivery = request.serviceType == ServiceType.PACKAGE_DELIVERY

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(16.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Drag Handle Indicator
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(6.dp))

            // SECTION 1: FARE AND COUNTDOWN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "GH₵ ${String.format(Locale.getDefault(), "%.2f", request.estimatedEarnings)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "⚡ 1.5x Surge",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Color(0xFFECFDF5),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Payments,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Cash Payment",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                        Text(
                            text = if (isDelivery) "Package Delivery" else "Standard Ride",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Circular Countdown Indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(46.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { timerProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = Color(0xFFF97316),
                        trackColor = Color(0xFFF1F5F9),
                        strokeWidth = 3.5.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$remainingSec",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "SEC",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // SECTION 2: PASSENGER / SENDER INFORMATION
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDelivery) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        val initials = request.customerName?.split(" ")?.let {
                            if (it.size > 1) "${it[0].first()}${it[1].first()}" else it[0].take(2)
                        } ?: "JL"
                        Text(
                            text = initials.uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isDelivery) "${request.customerName ?: "Sender"} (Sender)" else (request.customerName ?: "Joseph Lamptey"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    if (isDelivery) {
                        Text(
                            text = "Recipient: ${request.recipientName?.takeIf { it.isNotBlank() } ?: "Standard Package"}",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                            Text(
                                text = "4.9",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD97706)
                            )
                            Text(
                                text = "• 128 trips",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.1f", request.distanceKm)} km",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "~${(request.distanceKm * 2.5).toInt().coerceAtLeast(1)} mins trip",
                        fontSize = 10.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // SECTION 3: PICKUP SPOT AND DESTINATION (VERTICAL ROUTE TIMELINE)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Pickup Spot Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "PICKUP SPOT (${request.pickupEtaMin?.toInt() ?: 2} MINS AWAY)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = request.pickupLocation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "450m",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Vertical Connector Line
                Box(
                    modifier = Modifier
                        .padding(start = 3.dp)
                        .width(2.dp)
                        .height(10.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Drop-off Destination Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFF97316), RoundedCornerShape(2.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isDelivery) "DELIVERY DESTINATION" else "DROP-OFF DESTINATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF97316),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = request.dropOffLocation,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    val areaLabel = request.dropOffLocation.split(",").let {
                        if (it.size > 1) it[it.size - 2].trim() else ""
                    }
                    if (areaLabel.isNotBlank()) {
                        Text(
                            text = areaLabel,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // SECTION 4: ACTION BUTTONS (DECLINE & ACCEPT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // DECLINE BUTTON
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF334155)
                    ),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(15.dp), tint = Color(0xFF64748B))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Decline",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }
                }

                // ACCEPT BUTTON
                Button(
                    onClick = onAccept,
                    modifier = Modifier
                        .weight(1.8f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    enabled = !isAccepting,
                    elevation = ButtonDefaults.buttonElevation(2.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    if (isAccepting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = if (isDelivery) "ACCEPT DELIVERY" else "ACCEPT RIDE",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = Color.White,
                                maxLines = 1
                            )
                            Spacer(Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(12.dp)
                                    .background(Color.White.copy(alpha = 0.4f))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "GH₵ ${String.format(Locale.getDefault(), "%.2f", request.estimatedEarnings)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Bottom subtle bar indicator
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(3.dp)
                    .background(Color(0xFFE2E8F0), RoundedCornerShape(2.dp))
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun DailyFeeCountdown(secondsRemaining: Long) {
    val hours = secondsRemaining / 3600
    val minutes = (secondsRemaining % 3600) / 60
    val seconds = secondsRemaining % 60
    
    val timeText = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    
    Surface(
        color = Color(0xFFECFDF5),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.Schedule,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Active for: $timeText",
                color = Color(0xFF059669),
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DriverStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
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
                            text = sosType.ifEmpty { "Select Emergency Type" },
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
fun RegistrationNotice(status: String, onGoToProfile: () -> Unit) {
    val context = LocalContext.current
    val isSuspended = status == "SUSPENDED"
    val containerColor = if (isSuspended) Color(0xFFF8D7DA) else Color(0xFFFFF3CD)
    val contentColor = if (isSuspended) Color(0xFF721C24) else Color(0xFF856404)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, contentColor)
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
                            val intent = Intent(Intent.ACTION_DIAL, "tel:0541234567".toUri())
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
