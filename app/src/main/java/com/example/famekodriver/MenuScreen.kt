package com.example.famekodriver

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.famekodriver.core.data.SessionManager

@Composable
fun MenuScreen(
    onBack: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToRentals: () -> Unit,
    onNavigateToRideHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFleet: () -> Unit = {},
    onNavigateToVehicleReg: () -> Unit = {},
    onNavigateToSupport: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var activeRole by remember { mutableStateOf(sessionManager.getUserRole()) }

    val isFleetOwner = activeRole == "OWNER"

    if (isFleetOwner) {
        FleetOwnerMenuScreen(
            onBack = onBack,
            onNavigateToFleetSettings = onNavigateToSettings,
            onNavigateToFleetInventory = onNavigateToFleet,
            onNavigateToDriverRoster = onNavigateToFleet,
            onNavigateToWallet = onNavigateToWallet,
            onSwitchToDriverMode = {
                sessionManager.setUserRole("DRIVER")
                activeRole = "DRIVER"
            }
        )
    } else {
        DriverMenuScreen(
            onBack = onBack,
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToWallet = onNavigateToWallet,
            onNavigateToRentals = onNavigateToRentals,
            onNavigateToRideHistory = onNavigateToRideHistory,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToSupport = onNavigateToSupport,
            onSwitchToFleetConsole = {
                sessionManager.setUserRole("OWNER")
                activeRole = "OWNER"
            }
        )
    }
}
