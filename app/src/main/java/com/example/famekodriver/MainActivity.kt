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
}

class MainActivity : ComponentActivity() {
    private val repository = DriverRepository()
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
            val userRole = remember { sessionManager.getUserRole() }
            var currentStatus by rememberSaveable { mutableStateOf(sessionManager.getDriverStatus()) }
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
                    is Screen.Settings, is Screen.Earnings, is Screen.Rentals, is Screen.RideHistory, is Screen.VehicleRegistration, is Screen.Payment, is Screen.TermsAndConditions -> {
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
                }
            }

            when (val screen = currentScreen) {
                is Screen.DriverMap -> {
                    if (userRole == "OWNER") {
                        currentScreen = Screen.FleetManagement
                    } else {
                        MapScreen(
                            status = currentStatus,
                            onNavigateToMenu = {
                                currentScreen = Screen.Menu
                            },
                            onNavigateToProfile = {
                                val intent = Intent(this@MainActivity, DriverProfileActivity::class.java)
                                startActivity(intent)
                            },
                            onNavigateToChat = { convId, name ->
                                currentScreen = Screen.Chat(convId, name)
                            }
                        )
                    }
                }
                is Screen.Menu -> {
                    MenuScreen(
                        onBack = { 
                            currentScreen = if (userRole == "OWNER") Screen.FleetManagement else Screen.DriverMap 
                        },
                        onNavigateToProfile = {
                            val intent = Intent(this@MainActivity, DriverProfileActivity::class.java)
                            startActivity(intent)
                        },
                        onNavigateToWallet = {
                            currentScreen = Screen.Earnings
                        },
                        onNavigateToRentals = {
                            currentScreen = Screen.Rentals
                        },
                        onNavigateToRideHistory = {
                            currentScreen = Screen.RideHistory
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                        },
                        onNavigateToFleet = {
                            currentScreen = Screen.FleetManagement
                        },
                        onNavigateToVehicleReg = {
                            currentScreen = Screen.VehicleRegistration
                        },
                        onNavigateToSupport = {
                            currentScreen = Screen.SupportChat
                        }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(
                        onBack = { currentScreen = Screen.Menu },
                        onNavigateToNotificationSettings = {
                            currentScreen = Screen.NotificationSettings
                        },
                        onNavigateToTerms = {
                            currentScreen = Screen.TermsAndConditions
                        },
                        onLogout = {
                            sessionManager.logout()
                            val intent = Intent(this@MainActivity, DriverLoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()
                        }
                    )
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
                        onComplete = { currentScreen = Screen.DriverMap }
                    )
                }
                is Screen.NotificationSettings -> {
                    NotificationSettingsScreen(onBack = { currentScreen = Screen.Settings })
                }
                is Screen.Chat -> {
                    ChatScreen(
                        conversationId = screen.conversationId,
                        customerName = screen.customerName,
                        onBack = { currentScreen = Screen.DriverMap }
                    )
                }
                is Screen.SupportChat -> {
                    SupportChatScreen(
                        onBack = { currentScreen = Screen.Menu }
                    )
                }
                is Screen.TermsAndConditions -> {
                    TermsAndConditionsScreen(onBack = { currentScreen = Screen.Settings })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun startApprovalPolling(sessionManager: SessionManager) {
        val driverId = sessionManager.getDriverId() ?: return

        lifecycleScope.launch {
            while (true) {
                delay(10000) // Poll every 10 seconds
                repository.getDriverStatus(driverId).onSuccess { response ->
                    val oldStatus = sessionManager.getDriverStatus()
                    if (response.status != oldStatus) {
                        sessionManager.updateStatus(response.status)
                        
                        if (response.status == "APPROVED") {
                            Toast.makeText(this@MainActivity, "Account Approved!", Toast.LENGTH_LONG).show()
                        } else if (response.status == "SUSPENDED") {
                            Toast.makeText(this@MainActivity, "Account Suspended!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun updateFcmToken() {
        val driverId = sessionManager.getDriverId() ?: return
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                lifecycleScope.launch {
                    repository.updateFcmToken(driverId, token, "driver")
                }
            }
        }
    }
}
