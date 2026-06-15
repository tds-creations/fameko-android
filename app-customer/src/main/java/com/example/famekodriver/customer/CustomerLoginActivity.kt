package com.example.famekodriver.customer

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

class CustomerLoginActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val repository = DriverRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        android.util.Log.d("Fameko", "CustomerLoginActivity onCreate")
        sessionManager = SessionManager(this)

        // Check if already logged in as customer
        if (sessionManager.isLoggedIn()) {
            navigateToCustomerMap()
            return
        }

        setContentView(R.layout.activity_customer_login)

        findViewById<ImageView>(R.id.ivBackground).load(ImageLinks.MILLIE_LOGIN)
        findViewById<ImageView>(R.id.ivLogo).load(ImageLinks.IC_FAMEKO_LOGO)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<MaterialButton>(R.id.btnLogin)
        val tvBackToHome = findViewById<TextView>(R.id.tvBackToHome)

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
                repository.customerLogin(email, password)
                    .onSuccess { (customerId, customerName) ->
                        sessionManager.saveSession(customerId, customerName)
                        navigateToCustomerMap()
                    }
                    .onFailure { error ->
                        btnLogin.isEnabled = true
                        Toast.makeText(this@CustomerLoginActivity, "Login failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        val tvRegister = findViewById<TextView>(R.id.tvRegister)
        val registerText = "New to Fameko? Create account"
        val spannableRegister = android.text.SpannableString(registerText)
        val blueColor = android.graphics.Color.parseColor("#004E89")
        
        // Find index of "Create account"
        val startIndex = registerText.indexOf("Create account")
        if (startIndex != -1) {
            spannableRegister.setSpan(
                android.text.style.ForegroundColorSpan(blueColor),
                startIndex,
                registerText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableRegister.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                registerText.length,
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

    private fun navigateToCustomerMap() {
        val intent = Intent().setClassName(this, "com.example.famekodriver.customer.CustomerMapActivity")
        startActivity(intent)
        finish()
    }
}
