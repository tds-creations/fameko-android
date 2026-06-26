package com.example.famekodriver.customer

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.utils.ImageLinks
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class CustomerSignupActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private val repository = DriverRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_signup)
        sessionManager = SessionManager(this)

        setupCarousel()

        // Fields from layout
        val etName = findViewById<TextInputEditText>(R.id.etName)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPhone = findViewById<TextInputEditText>(R.id.etPhone)
        val actRegion = findViewById<AutoCompleteTextView>(R.id.actRegion)
        val etAddress = findViewById<TextInputEditText>(R.id.etAddress)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val cbTerms = findViewById<CheckBox>(R.id.cbTerms)
        val btnSignup = findViewById<MaterialButton>(R.id.btnSignup)
        val tvLogin = findViewById<TextView>(R.id.tvLogin)

        // Setup Region Dropdown
        val regions = arrayOf(
            "Ahafo", "Ashanti", "Bono", "Bono East", "Central", "Eastern",
            "Greater Accra", "Northern", "North East", "Oti", "Savannah",
            "Upper East", "Upper West", "Volta", "Western", "Western North"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, regions)
        actRegion.setAdapter(adapter)

        // Final Submission
        btnSignup.setOnClickListener {
            val name = etName.text?.toString() ?: ""
            val email = etEmail.text?.toString() ?: ""
            val phone = etPhone.text?.toString() ?: ""
            val region = actRegion.text?.toString() ?: ""
            val address = etAddress.text?.toString() ?: ""
            val password = etPassword.text?.toString() ?: ""
            val confirmPassword = etConfirmPassword.text?.toString() ?: ""

            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || region.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!cbTerms.isChecked) {
                Toast.makeText(this, "Please agree to terms and conditions", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSignup.isEnabled = false
            lifecycleScope.launch {
                repository.customerRegister(name, email, phone, address, password, region = region)
                    .onSuccess {
                        Toast.makeText(this@CustomerSignupActivity, "Account created successfully", Toast.LENGTH_LONG).show()
                        
                        // Automatic login
                        repository.customerLogin(email, password).onSuccess { (id, customerName) ->
                            sessionManager.saveSession(id, customerName)
                            val intent = Intent().setClassName(this@CustomerSignupActivity, "com.example.famekodriver.customer.CustomerMapActivity")
                            startActivity(intent)
                            finishAffinity()
                        }.onFailure { error ->
                            btnSignup.isEnabled = true
                            Toast.makeText(this@CustomerSignupActivity, "Auto-login failed: ${error.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    .onFailure { error ->
                        btnSignup.isEnabled = true
                        Toast.makeText(this@CustomerSignupActivity, "Signup failed: ${error.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }

        val loginText = "Already have an account? Sign In"
        val spannableLogin = android.text.SpannableString(loginText)
        val blueColor = android.graphics.Color.parseColor("#0061A4")
        
        val loginStart = loginText.indexOf("Sign In")
        if (loginStart != -1) {
            spannableLogin.setSpan(
                android.text.style.ForegroundColorSpan(blueColor),
                loginStart,
                loginText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableLogin.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                loginStart,
                loginText.length,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        tvLogin.text = spannableLogin
        tvLogin.setOnClickListener {
            finish()
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
                kotlinx.coroutines.delay(2500)
            }
        }
    }
}
