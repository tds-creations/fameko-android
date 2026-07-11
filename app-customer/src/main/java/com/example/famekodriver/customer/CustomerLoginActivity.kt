package com.example.famekodriver.customer

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

class CustomerLoginActivity : AppCompatActivity() {
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
        Log.d("Fameko", "CustomerLoginActivity onCreate")
        sessionManager = SessionManager(this)

        setContentView(R.layout.activity_customer_login)

        setupCarousel()

        val etPhone = findViewById<EditText>(R.id.etPhone)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val btnGoogleLogin = findViewById<MaterialButton>(R.id.btnGoogleLogin)
        val tvBackToHome = findViewById<TextView>(R.id.tvBackToHome)
        val otpContainer = findViewById<View>(R.id.otpContainer)
        val etOtp = findViewById<EditText>(R.id.etOtp)

        var isOtpSent = false

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
                    repository.requestCustomerOtp(fullPhone)
                        .onSuccess { message ->
                            isOtpSent = true
                            btnLogin.isEnabled = true
                            btnLogin.text = getString(R.string.btn_verify_otp)
                            otpContainer.visibility = View.VISIBLE
                            Toast.makeText(this@CustomerLoginActivity, message, Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { error ->
                            btnLogin.isEnabled = true
                            Toast.makeText(this@CustomerLoginActivity, "Failed to send OTP: ${error.message}", Toast.LENGTH_LONG).show()
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
                    repository.verifyCustomerOtp(fullPhone, otp)
                        .onSuccess { (customerId, customerName) ->
                            sessionManager.saveSession(customerId, customerName)
                            navigateToCustomerMap()
                        }
                        .onFailure { error ->
                            btnLogin.isEnabled = true
                            Toast.makeText(this@CustomerLoginActivity, "Verification failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                }
            }
        }

        btnGoogleLogin.setOnClickListener {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("989048143840-chmqrl6lr2s0kdtep3gbbp0t5kse2gf6.apps.googleusercontent.com")
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(this, gso)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }

        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val registerText = getString(R.string.new_to_fameko)
        val spannableRegister = android.text.SpannableString(registerText)
        val brandBlue = "#0047AB".toColorInt()
        
        // Find index of "Create account"
        val highlightedPart = "Create account"
        val startIndex = registerText.indexOf(highlightedPart)
        if (startIndex != -1) {
            spannableRegister.setSpan(
                android.text.style.ForegroundColorSpan(brandBlue),
                startIndex,
                startIndex + highlightedPart.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableRegister.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                startIndex + highlightedPart.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvRegister.text = spannableRegister
        tvRegister.setOnClickListener {
            val intent = Intent(this, CustomerSignupActivity::class.java)
            startActivity(intent)
        }

        tvBackToHome.setOnClickListener {
            finish()
        }
    }

    private fun loginWithGoogle(idToken: String, displayName: String?, email: String?, uid: String?) {
        lifecycleScope.launch {
            repository.customerGoogleLogin(idToken)
                .onSuccess { (customerId, customerName) ->
                    sessionManager.saveSession(customerId, customerName)
                    navigateToCustomerMap()
                }
                .onFailure { error ->
                    if (error.message == "USER_NOT_FOUND") {
                        Toast.makeText(this@CustomerLoginActivity, "Account not found. Please sign up.", Toast.LENGTH_LONG).show()
                        val intent = Intent(this@CustomerLoginActivity, CustomerSignupActivity::class.java).apply {
                            putExtra("google_name", displayName)
                            putExtra("google_email", email)
                            putExtra("google_uid", uid)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@CustomerLoginActivity, "Google login failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
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
                    crossfade(2000)
                }
                currentIndex = (currentIndex + 1) % images.size
                delay(10000.milliseconds)
            }
        }
    }

    private fun navigateToCustomerMap() {
        val intent = Intent().setClassName(this, "com.example.famekodriver.customer.CustomerMapActivity")
        startActivity(intent)
        finish()
    }
}
