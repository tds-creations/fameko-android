package com.example.famekodriver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.launch

class DriverSignupActivity : ComponentActivity() {
    private val repository = DriverRepository.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val initialRole = intent.getStringExtra("role") ?: "DRIVER"

        setContent {
            var signupStep by remember { mutableStateOf(if (initialRole == "OWNER") 1 else 0) } // 0 = Driver, 1 = Fleet Owner

            when (signupStep) {
                0 -> {
                    DriverSignupScreen(
                        onBack = { finish() },
                        onSwitchToFleetSignup = { signupStep = 1 },
                        onLogin = {
                            val intent = Intent(this@DriverSignupActivity, DriverLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onSubmit = { formData -> submitRegistration(formData) }
                    )
                }
                1 -> {
                    FleetOwnerSignupScreen(
                        onBack = { finish() },
                        onSwitchToDriverSignup = { signupStep = 0 },
                        onLoginConsole = {
                            val intent = Intent(this@DriverSignupActivity, DriverLoginActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onSubmit = { formData -> submitRegistration(formData) }
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

    private fun submitRegistration(formData: Map<String, String>) {
        val phone = normalizePhone(formData["phone"] ?: "")
        val role = formData["user_role"] ?: "DRIVER"
        val name = formData["full_name"] ?: ""
        val email = formData["email"] ?: ""
        val password = formData["password"] ?: ""
        val licenseNo = (formData["license_number"] ?: "").ifEmpty { "DL-2024-" + (1000..9999).random() }
        val companyName = formData["company_name"]
        val regNumber = formData["registration_number"]

        Toast.makeText(this, "Submitting ${if (role == "OWNER") "Fleet Owner" else "Driver"} registration...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch {
            repository.driverRegister(
                name = name,
                email = email,
                phone = phone,
                password = password,
                licenseNumber = licenseNo,
                region = "Greater Accra",
                vehicleType = if (role == "OWNER") "Fleet" else "Car",
                serviceType = "BOTH",
                vehicleNumber = "GT-" + (1000..9999).random() + "-24",
                emergencyContact1 = "",
                emergencyContact2 = "",
                userRole = role,
                companyName = companyName,
                registrationNumber = regNumber
            ).onSuccess {
                Toast.makeText(this@DriverSignupActivity, "Registration Successful! Please log in.", Toast.LENGTH_LONG).show()
                val intent = Intent(this@DriverSignupActivity, DriverLoginActivity::class.java)
                startActivity(intent)
                finish()
            }.onFailure { error ->
                Toast.makeText(this@DriverSignupActivity, "Registration failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
