package com.example.famekodriver

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.utils.ImageLinks
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DriverLoginActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val repository = DriverRepository()

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account: GoogleSignInAccount? = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                loginWithGoogle(idToken)
            } else {
                Toast.makeText(this, "Google Sign-In failed: No ID Token", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.w("Fameko", "Google Sign-In failed: ${e.statusCode}")
            Toast.makeText(this, "Google Sign-In failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        setContentView(R.layout.activity_driver_login)

        setupCarousel()

        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnGoogleLogin = findViewById<MaterialButton>(R.id.btnGoogleLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val otpContainer = findViewById<View>(R.id.otpContainer)
        val etOtp = findViewById<EditText>(R.id.etOtp)

        var isOtpSent = false
        
        val registerText = "Don't have an account? Register here"
        val spannableRegister = android.text.SpannableString(registerText)
        val brandBlue = "#0047AB".toColorInt()
        
        val regStart = registerText.indexOf("Register here")
        if (regStart != -1) {
            spannableRegister.setSpan(
                android.text.style.ForegroundColorSpan(brandBlue),
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

        btnGoogleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("989048143840-chmqrl6lr2s0kdtep3gbbp0t5kse2gf6.apps.googleusercontent.com")
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        btnLogin.setOnClickListener {
            val phone = etPhone.text?.toString() ?: ""

            if (phone.isEmpty()) {
                Toast.makeText(this, "Please enter your phone number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Normalize phone number (ensure it has +233 prefix if not present)
            // Automatically strip leading zero if user enters 10 digits
            var cleanedPhone = phone.trim()
            if (cleanedPhone.length == 10 && cleanedPhone.startsWith("0")) {
                cleanedPhone = cleanedPhone.substring(1)
            }
            val fullPhone = if (cleanedPhone.startsWith("+")) cleanedPhone else "+233$cleanedPhone"

            if (!isOtpSent) {
                // Step 1: Request OTP
                btnLogin.isEnabled = false
                Toast.makeText(this, getString(R.string.msg_sending_otp), Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    repository.requestDriverOtp(fullPhone)
                        .onSuccess { message ->
                            isOtpSent = true
                            btnLogin.isEnabled = true
                            btnLogin.text = getString(R.string.btn_verify_otp)
                            otpContainer.visibility = View.VISIBLE
                            Toast.makeText(this@DriverLoginActivity, message, Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { error ->
                            btnLogin.isEnabled = true
                            Toast.makeText(this@DriverLoginActivity, "Failed to send OTP: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                // Step 2: Verify OTP
                val otp = etOtp.text?.toString() ?: ""
                if (otp.length < 6) {
                    Toast.makeText(this, "Please enter the 6-digit OTP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnLogin.isEnabled = false
                Toast.makeText(this, getString(R.string.msg_verifying_otp), Toast.LENGTH_SHORT).show()

                lifecycleScope.launch {
                    repository.verifyDriverOtp(fullPhone, otp)
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
                                Toast.makeText(this@DriverLoginActivity, "Driver not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .onFailure { error ->
                            btnLogin.isEnabled = true
                            Toast.makeText(this@DriverLoginActivity, "Verification failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, DriverSignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginWithGoogle(idToken: String) {
        lifecycleScope.launch {
            repository.driverGoogleLogin(idToken)
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
                    }
                }
                .onFailure { error ->
                    Toast.makeText(this@DriverLoginActivity, "Google login failed: ${error.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun setupCarousel() {
        val ivLogo = findViewById<ImageView>(R.id.ivLogo)
        val images = listOf(
            ImageLinks.DRIVER_LOGIN_CAROUSEL_1,
            ImageLinks.DRIVER_LOGIN_CAROUSEL_2,
            ImageLinks.DRIVER_LOGIN_CAROUSEL_3
        )

        var currentIndex = 0
        lifecycleScope.launch {
            while (true) {
                ivLogo.load(images[currentIndex]) {
                    crossfade(true)
                    crossfade(2000)
                }
                currentIndex = (currentIndex + 1) % images.size
                delay(10000.milliseconds)
            }
        }
    }
}
