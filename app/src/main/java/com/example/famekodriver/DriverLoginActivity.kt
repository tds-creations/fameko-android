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
                loginWithGoogle(idToken, account.displayName, account.email, account.id)
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
        val etPassword = findViewById<EditText>(R.id.etPassword)

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
            val password = etPassword.text?.toString() ?: ""

            if (phone.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter phone and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Normalize phone number (ensure it has +233 prefix if not present)
            val fullPhone = normalizePhone(phone)

            btnLogin.isEnabled = false
            Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

            lifecycleScope.launch {
                repository.login(fullPhone, password)
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
                        Toast.makeText(this@DriverLoginActivity, "Login failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        tvRegister.setOnClickListener {
            val intent = Intent(this, DriverSignupActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginWithGoogle(idToken: String, displayName: String?, email: String?, uid: String?) {
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
                    if (error.message == "USER_NOT_FOUND") {
                        Toast.makeText(this@DriverLoginActivity, "Account not found. Please complete your registration.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@DriverLoginActivity, DriverSignupActivity::class.java).apply {
                            putExtra("google_name", displayName)
                            putExtra("google_email", email)
                            putExtra("google_uid", uid)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@DriverLoginActivity, "Google login failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun normalizePhone(phone: String): String {
        var cleaned = phone.trim().replace(" ", "").replace("-", "")
        if (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }
        return if (cleaned.startsWith("+")) cleaned else "+233$cleaned"
    }

    private fun setupCarousel() {
        val viewPager = findViewById<androidx.viewpager2.widget.ViewPager2>(R.id.viewPagerCarousel)
        val images = listOf(
            ImageLinks.DRIVER_LOGIN_CAROUSEL_1,
            ImageLinks.DRIVER_LOGIN_CAROUSEL_2,
            ImageLinks.DRIVER_LOGIN_CAROUSEL_3
        )

        viewPager.adapter = com.example.famekodriver.core.utils.CarouselAdapter(images)

        lifecycleScope.launch {
            while (true) {
                delay(5000)
                val nextItem = (viewPager.currentItem + 1) % images.size
                viewPager.setCurrentItem(nextItem, true)
            }
        }
    }
}
