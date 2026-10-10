package com.example.famekodriver

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.famekodriver.core.data.SessionManager

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    var activeRole by remember { mutableStateOf(sessionManager.getUserRole()) }

    val isFleetOwner = activeRole == "OWNER"

    if (isFleetOwner) {
        FleetOwnerSettingsScreen(
            onBack = onBack,
            onLogout = onLogout,
            onSwitchToDriverMode = {
                sessionManager.setUserRole("DRIVER")
                activeRole = "DRIVER"
            }
        )
    } else {
        DriverSettingsScreen(
            onBack = onBack,
            onLogout = onLogout,
            onSwitchToFleetConsole = {
                sessionManager.setUserRole("OWNER")
                activeRole = "OWNER"
            },
            onNavigateToTerms = onNavigateToTerms,
            onNavigateToPrivacy = onNavigateToPrivacy
        )
    }
}
