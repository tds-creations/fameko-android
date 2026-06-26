package com.example.famekodriver

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.utils.ImageLinks
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class DriverLoginActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val repository = DriverRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        // Check if already logged in
        if (sessionManager.isLoggedIn()) {
            Toast.makeText(this, "Welcome back, ${sessionManager.getDriverName()}", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        setContentView(R.layout.activity_driver_login)

        setupCarousel()

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val registerText = "Don't have an account? Register here"
        val spannableRegister = android.text.SpannableString(registerText)
        val orangeColor = android.graphics.Color.parseColor("#FF6B35")
        
        val regStart = registerText.indexOf("Register here")
        if (regStart != -1) {
            spannableRegister.setSpan(
                android.text.style.ForegroundColorSpan(orangeColor),
                regStart,
                registerText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableRegister.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                regStart,
                registerText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvRegister.text = spannableRegister

        btnLogin.setOnClickListener {
            val email = etEmail.text?.toString() ?: ""
            val password = etPassword.text?.toString() ?: ""

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                repository.login(email, password)
                    .onSuccess { driver ->
                        if (driver != null) {
                            sessionManager.saveSession(
                                driverId = driver.id.toString(),
                                driverName = driver.fullName,
                                status = driver.status,
                                phone = driver.phone,
                                role = driver.userRole,
                                company = driver.companyName,
                                vehicleType = driver.vehicleType
                            )
                            Toast.makeText(this@DriverLoginActivity, "Login Successful!", Toast.LENGTH_SHORT).show()
                            
                            val intent = Intent(this@DriverLoginActivity, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            btnLogin.isEnabled = true
                            Toast.makeText(this@DriverLoginActivity, "Invalid email or password", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .onFailure { error ->
                        btnLogin.isEnabled = true
                        val message = error.message ?: "Login failed"
                        Toast.makeText(this@DriverLoginActivity, message, Toast.LENGTH_LONG).show()
                    }
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, DriverSignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupCarousel() {
        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val images = listOf(
            ImageLinks.CUSTOMER_LOGIN_CAROUSEL_1,
            ImageLinks.CUSTOMER_LOGIN_CAROUSEL_2,
            ImageLinks.CUSTOMER_LOGIN_CAROUSEL_3
        )

        var currentIndex = 0
        lifecycleScope.launch {
            while (true) {
                ivLogo.load(images[currentIndex]) {
                    crossfade(true)
                    crossfade(500)
                }
                currentIndex = (currentIndex + 1) % images.size
                kotlinx.coroutines.delay(1500)
            }
        }
    }
}
