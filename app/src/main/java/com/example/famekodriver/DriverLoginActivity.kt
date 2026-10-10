package com.example.famekodriver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch

class DriverLoginActivity : ComponentActivity() {
    private lateinit var sessionManager: SessionManager
    private val repository = DriverRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        setContent {
            var authStep by remember { mutableStateOf(0) } // 0 = Gateway, 1 = Driver Login, 2 = Fleet Owner Login

            when (authStep) {
                0 -> {
                    PartnerGatewayScreen(
                        onSelectDriverPortal = { authStep = 1 },
                        onSelectFleetConsole = { authStep = 2 },
                        onRegisterNewPartner = {
                            val intent = Intent(this@DriverLoginActivity, DriverSignupActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
                1 -> {
                    DriverSignInScreen(
                        onBack = { authStep = 0 },
                        onLogin = { phone, password -> performLogin(phone, password, "DRIVER") },
                        onSwitchToFleetLogin = { authStep = 2 },
                        onContactSupport = {
                            Toast.makeText(this@DriverLoginActivity, "Opening Accra Hub WhatsApp Support...", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
                2 -> {
                    FleetOwnerSignInScreen(
                        onBack = { authStep = 0 },
                        onLogin = { phone, password -> performLogin(phone, password, "OWNER") },
                        onSwitchToDriverLogin = { authStep = 1 },
                        onRegisterFleet = {
                            val intent = Intent(this@DriverLoginActivity, DriverSignupActivity::class.java)
                            startActivity(intent)
                        }
                    )
                }
            }
        }
    }

    private fun normalizePhone(phone: String): String {
        var cleaned = phone.trim().replace(" ", "").replace("-", "")
        if (cleaned.startsWith("0")) cleaned = cleaned.substring(1)
        if (cleaned.startsWith("+233")) return cleaned
        if (cleaned.startsWith("233")) return "+$cleaned"
        if (cleaned.startsWith("+")) return cleaned
        return "+233$cleaned"
    }

    private fun performLogin(phone: String, password: String, selectedRole: String) {
        val fullPhone = normalizePhone(phone)
        val roleTitle = if (selectedRole == "OWNER") "Fleet Owner" else "Driver"
        Toast.makeText(this, "Logging in as $roleTitle...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            repository.login(fullPhone, password, selectedRole)
                .onSuccess { driver ->
                    if (driver != null) {
                        sessionManager.saveSession(
                            driverId = driver.id.toString(),
                            driverName = driver.fullName,
                            status = driver.status,
                            phone = driver.phone,
                            role = driver.userRole,
                            company = driver.companyName,
                            vehicleType = driver.vehicleType,
                            profilePicture = driver.profilePicture
                        )
                        Toast.makeText(this@DriverLoginActivity, "Welcome ${driver.fullName}!", Toast.LENGTH_SHORT).show()

                        val intent = Intent(this@DriverLoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@DriverLoginActivity, "Account not found", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { error ->
                    Toast.makeText(this@DriverLoginActivity, error.message ?: "Login failed", Toast.LENGTH_LONG).show()
                }
        }
    }
}
