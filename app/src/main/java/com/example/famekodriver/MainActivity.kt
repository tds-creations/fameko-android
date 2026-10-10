package com.example.famekodriver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen {
    object DriverMap : Screen()
    object Menu : Screen()
    object Earnings : Screen()
    object Rentals : Screen()
    object RideHistory : Screen()
    object Settings : Screen()
    object NotificationSettings : Screen()
    object FleetManagement : Screen()
    object AddRentalVehicle : Screen()
    data class EditRentalVehicle(val vehicle: Map<String, Any>) : Screen()
    object VehicleRegistration : Screen()
    object Payment : Screen()
    data class Chat(val conversationId: Int, val customerName: String) : Screen()
    object SupportChat : Screen()
    object TermsAndConditions : Screen()
    object PrivacyPolicy : Screen()
}

class MainActivity : ComponentActivity() {
    private val repository = DriverRepository.getInstance()
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            val intent = Intent(this, DriverLoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
        
        startApprovalPolling(sessionManager)
        updateFcmToken()

        setContent {
            var userRole by remember { mutableStateOf(sessionManager.getUserRole()) }
            var currentStatus by rememberSaveable { mutableStateOf(sessionManager.getDriverStatus()) }
            var currentVehicleType by rememberSaveable { mutableStateOf(sessionManager.getVehicleType() ?: "") }
            
            var isTermsAccepted by remember { 
                mutableStateOf(sessionManager.getAcceptedTermsVersion() == TermsConstants.CURRENT_DRIVER_TERMS_VERSION) 
            }

            if (!isTermsAccepted) {
                TermsAndConditionsScreen(
                    onBack = { finish() },
                    onAccept = {
                        sessionManager.setAcceptedTermsVersion(TermsConstants.CURRENT_DRIVER_TERMS_VERSION)
                        isTermsAccepted = true
                    }
                )
                return@setContent
            }

            var currentScreen by remember { 
                mutableStateOf<Screen>(if (userRole == "OWNER") Screen.FleetManagement else Screen.DriverMap) 
            }
            
            val context = LocalContext.current
            var lastBackPressTime by remember { mutableLongStateOf(0L) }

            BackHandler {
                when (currentScreen) {
                    is Screen.Menu, is Screen.Chat, is Screen.SupportChat -> {
                        currentScreen = if (userRole == "OWNER") Screen.FleetManagement else Screen.DriverMap
                    }
                    is Screen.FleetManagement -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000) {
                            finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(context, "Double tap back to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                    is Screen.Settings, is Screen.Earnings, is Screen.Rentals, is Screen.RideHistory, is Screen.VehicleRegistration, is Screen.Payment, is Screen.TermsAndConditions, is Screen.PrivacyPolicy -> {
                        currentScreen = Screen.Menu
                    }
                    is Screen.AddRentalVehicle, is Screen.EditRentalVehicle -> {
                        currentScreen = Screen.FleetManagement
                    }
                    is Screen.NotificationSettings -> {
                        currentScreen = Screen.Settings
                    }
                    is Screen.DriverMap -> {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackPressTime < 2000) {
                            finish()
                        } else {
                            lastBackPressTime = currentTime
                            Toast.makeText(context, "Double tap back to exit", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            LaunchedEffect(Unit) {
                while(true) {
                    delay(5000)
                    currentStatus = sessionManager.getDriverStatus()
                    currentVehicleType = sessionManager.getVehicleType() ?: ""
                }
            }

            when (val screen = currentScreen) {
                is Screen.DriverMap -> {
                    if (userRole == "OWNER") {
                        currentScreen = Screen.FleetManagement
                    } else {
                        MapScreen(
                            status = currentStatus,
                            vehicleTypeFromSession = currentVehicleType,
                            onNavigateToMenu = {
                                currentScreen = Screen.Menu
                            },
                            onNavigateToProfile = {
                                val intent = Intent(this@MainActivity, DriverProfileActivity::class.java)
                                startActivity(intent)
                            },
                            onNavigateToChat = { convId, name ->
                                currentScreen = Screen.Chat(convId, name)
                            },
                            onNavigateToEarnings = {
                                currentScreen = Screen.Earnings
                            }
                        )
                    }
                }
                is Screen.Menu -> {
                    if (userRole == "OWNER") {
                        FleetOwnerMenuScreen(
                            onBack = { currentScreen = Screen.FleetManagement },
                            onNavigateToFleetSettings = { currentScreen = Screen.Settings },
                            onNavigateToFleetInventory = { currentScreen = Screen.FleetManagement },
                            onNavigateToDriverRoster = { currentScreen = Screen.FleetManagement },
                            onNavigateToWallet = { currentScreen = Screen.Earnings },
                            onSwitchToDriverMode = {
                                sessionManager.setUserRole("DRIVER")
                                userRole = "DRIVER"
                                currentScreen = Screen.DriverMap
                            }
                        )
                    } else {
                        DriverMenuScreen(
                            onBack = { currentScreen = Screen.DriverMap },
                            onNavigateToProfile = {
                                val intent = Intent(this@MainActivity, DriverProfileActivity::class.java)
                                startActivity(intent)
                            },
                            onNavigateToWallet = { currentScreen = Screen.Earnings },
                            onNavigateToRentals = { currentScreen = Screen.Rentals },
                            onNavigateToRideHistory = { currentScreen = Screen.RideHistory },
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                            onNavigateToSupport = { currentScreen = Screen.SupportChat },
                            onSwitchToFleetConsole = {
                                sessionManager.setUserRole("OWNER")
                                userRole = "OWNER"
                                currentScreen = Screen.FleetManagement
                            }
                        )
                    }
                }
                is Screen.Settings -> {
                    if (userRole == "OWNER") {
                        FleetOwnerSettingsScreen(
                            onBack = { currentScreen = Screen.Menu },
                            onLogout = {
                                sessionManager.logout()
                                val intent = Intent(this@MainActivity, DriverLoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            },
                            onSwitchToDriverMode = {
                                sessionManager.setUserRole("DRIVER")
                                userRole = "DRIVER"
                                currentScreen = Screen.DriverMap
                            }
                        )
                    } else {
                        DriverSettingsScreen(
                            onBack = { currentScreen = Screen.Menu },
                            onLogout = {
                                sessionManager.logout()
                                val intent = Intent(this@MainActivity, DriverLoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                                finish()
                            },
                            onSwitchToFleetConsole = {
                                sessionManager.setUserRole("OWNER")
                                userRole = "OWNER"
                                currentScreen = Screen.FleetManagement
                            },
                            onNavigateToTerms = { currentScreen = Screen.TermsAndConditions },
                            onNavigateToPrivacy = { currentScreen = Screen.PrivacyPolicy }
                        )
                    }
                }
                is Screen.Earnings -> {
                    EarningsScreen(
                        onBack = { currentScreen = Screen.Menu },
                        onNavigateToPayment = { currentScreen = Screen.Payment }
                    )
                }
                is Screen.Payment -> {
                    PaymentScreen(onBack = { currentScreen = Screen.Earnings })
                }
                is Screen.Rentals -> {
                    RentalsScreen(onBack = { currentScreen = Screen.Menu })
                }
                is Screen.RideHistory -> {
                    RideHistoryScreen(onBack = { currentScreen = Screen.Menu })
                }
                is Screen.FleetManagement -> {
                    FleetManagementScreen(
                        onNavigateToMenu = { currentScreen = Screen.Menu },
                        onNavigateToProfile = {
                            val intent = Intent(this@MainActivity, DriverProfileActivity::class.java)
                            startActivity(intent)
                        },
                        onNavigateToAddVehicle = {
                            currentScreen = Screen.AddRentalVehicle
                        },
                        onEditVehicle = { vehicle ->
                            currentScreen = Screen.EditRentalVehicle(vehicle)
                        }
                    )
                }
                is Screen.AddRentalVehicle -> {
                    AddRentalVehicleScreen(
                        onBack = { currentScreen = Screen.FleetManagement },
                        onComplete = { currentScreen = Screen.FleetManagement }
                    )
                }
                is Screen.EditRentalVehicle -> {
                    AddRentalVehicleScreen(
                        vehicle = screen.vehicle,
                        onBack = { currentScreen = Screen.FleetManagement },
                        onComplete = { currentScreen = Screen.FleetManagement }
                    )
                }
                is Screen.VehicleRegistration -> {
                    VehicleRegistrationScreen(
                        onBack = { currentScreen = Screen.Menu },
                        onComplete = { currentScreen = Screen.Menu }
                    )
                }
                is Screen.Chat -> {
                    ChatScreen(
                        conversationId = screen.conversationId,
                        customerName = screen.customerName,
                        onBack = { currentScreen = if (userRole == "OWNER") Screen.FleetManagement else Screen.DriverMap }
                    )
                }
                is Screen.SupportChat -> {
                    SupportChatScreen(
                        onBack = { currentScreen = Screen.Menu }
                    )
                }
                is Screen.NotificationSettings -> {
                    NotificationSettingsScreen(
                        onBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.TermsAndConditions -> {
                    TermsAndConditionsScreen(
                        onBack = { currentScreen = Screen.Settings }
                    )
                }
                is Screen.PrivacyPolicy -> {
                    PrivacyPolicyScreen(
                        onBack = { currentScreen = Screen.Settings }
                    )
                }
            }
        }
    }

    private fun startApprovalPolling(sessionManager: SessionManager) {
        lifecycleScope.launch {
            while (true) {
                val driverId = sessionManager.getDriverId()
                if (!driverId.isNullOrEmpty()) {
                    repository.getDriverStatus(driverId).onSuccess { response ->
                        if (response.status != sessionManager.getDriverStatus()) {
                            sessionManager.updateStatus(response.status)
                        }
                    }
                }
                delay(30000)
            }
        }
    }

    private fun updateFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val driverId = sessionManager.getDriverId()
                val role = sessionManager.getUserRole()
                if (!token.isNullOrEmpty() && !driverId.isNullOrEmpty()) {
                    lifecycleScope.launch {
                        repository.updateFcmToken(driverId, token, if (role == "OWNER") "FLEET_OWNER" else "DRIVER")
                    }
                }
            }
        }
    }
}
