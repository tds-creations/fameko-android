package com.example.famekodriver

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.utils.ImageLinks
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class DriverSignupActivity : AppCompatActivity() {
    private val repository = DriverRepository()
    private var selectedVehicleType: String = "Car"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_signup)

        setupCarousel()

        // Load vehicle icons from URLs
        findViewById<ImageView>(R.id.ivCar).load(ImageLinks.IC_CAR_SALOON)
        findViewById<ImageView>(R.id.ivPragya).load(ImageLinks.IC_PRAGYA)
        findViewById<ImageView>(R.id.ivBicycle).load(ImageLinks.IC_BICYCLE)
        findViewById<ImageView>(R.id.ivAboboyaa).load(ImageLinks.IC_ABOBOYAA)
        findViewById<ImageView>(R.id.ivOkada).load(ImageLinks.IC_OKADA)
        findViewById<ImageView>(R.id.ivTruck).load(ImageLinks.IC_TRUCK)

        setupRegistrationForm()
        setupVehicleTypeSelection()
        checkGoogleExtras()
    }

    private fun checkGoogleExtras() {
        val googleName = intent.getStringExtra("google_name")
        val googleEmail = intent.getStringExtra("google_email")

        if (googleEmail != null) {
            val etName = findViewById<TextInputEditText>(R.id.etName)
            val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
            val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
            val etConfirmPassword = findViewById<TextInputEditText>(R.id.etConfirmPassword)
            val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
            val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
            val tilConfirmPassword = findViewById<TextInputLayout>(R.id.tilConfirmPassword)

            etName.setText(googleName)
            etEmail.setText(googleEmail)
            
            // Lock fields
            etEmail.isEnabled = false
            tilEmail.helperText = "Verified via Google"
            
            // Hide password fields as they aren't needed for Google login
            tilPassword.visibility = View.GONE
            tilConfirmPassword.visibility = View.GONE
            etPassword.setText("GOOGLE_AUTH")
            etConfirmPassword.setText("GOOGLE_AUTH")
            
            Toast.makeText(this, "Signed in as $googleEmail. Please complete the remaining details.", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupVehicleTypeSelection() {
        val items = mapOf(
            R.id.itemCar to "Car",
            R.id.itemPragya to "Pragya",
            R.id.itemBicycle to "Bicycle",
            R.id.itemAboboyaa to "Aboboyaa",
            R.id.itemOkada to "Okada",
            R.id.itemTruck to "Truck"
        )

        items.keys.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                updateVehicleSelection(id, items[id] ?: "Car")
            }
        }

        // Initial selection
        updateVehicleSelection(R.id.itemCar, "Car")
    }

    private fun updateVehicleSelection(selectedId: Int, type: String) {
        selectedVehicleType = type
        val itemIds = listOf(R.id.itemCar, R.id.itemPragya, R.id.itemBicycle, R.id.itemAboboyaa, R.id.itemOkada, R.id.itemTruck)
        itemIds.forEach { id ->
            findViewById<View>(id).isSelected = (id == selectedId)
        }
    }

    private fun setupRegistrationForm() {
        val actRegion = findViewById<AutoCompleteTextView>(R.id.actRegion)
        val regions = arrayOf(
            "Ahafo", "Ashanti", "Bono", "Bono East", "Central", "Eastern",
            "Greater Accra", "Northern", "North East", "Oti", "Savannah",
            "Upper East", "Upper West", "Volta", "Western", "Western North"
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, regions)
        actRegion.setAdapter(adapter)

        setupRoleToggle()

        findViewById<MaterialButton>(R.id.btnSubmit).setOnClickListener {
            if (validateForm()) {
                submitRegistration()
            }
        }

        val tvLogin = findViewById<TextView>(R.id.tvLogin)
        val loginText = "Already a driver? Login here"
        val spannableLogin = android.text.SpannableString(loginText)
        val brandBlue = "#0047AB".toColorInt()
        
        val loginStart = loginText.indexOf("Login here")
        if (loginStart != -1) {
            spannableLogin.setSpan(
                android.text.style.ForegroundColorSpan(brandBlue),
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
        tvLogin.setOnClickListener { finish() }
    }

    private fun setupRoleToggle() {
        val toggleRole = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleRole)
        val tilCompanyName = findViewById<View>(R.id.tilCompanyName)
        val tilRegNum = findViewById<TextInputLayout>(R.id.tilRegistrationNumber)
        val tilLicenseNum = findViewById<TextInputLayout>(R.id.tilDriversLicense)
        
        toggleRole.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.btnRoleDriver -> {
                        tilCompanyName.visibility = View.GONE
                        tilRegNum.visibility = View.GONE
                        tilLicenseNum.visibility = View.VISIBLE
                        tilLicenseNum.hint = "Driver's License Number"
                        tilLicenseNum.helperText = "Enter your valid DVLA license number"
                    }
                    R.id.btnRoleOwner -> {
                        tilCompanyName.visibility = View.VISIBLE
                        tilRegNum.visibility = View.VISIBLE
                        tilRegNum.hint = "Business Registration Number"
                        tilRegNum.helperText = "Enter your RGD business number"
                        tilLicenseNum.visibility = View.GONE
                    }
                    R.id.btnRoleBoth -> {
                        tilCompanyName.visibility = View.VISIBLE
                        tilRegNum.visibility = View.VISIBLE
                        tilRegNum.hint = "Business Registration Number"
                        tilRegNum.helperText = "Enter your RGD business number"
                        tilLicenseNum.visibility = View.VISIBLE
                        tilLicenseNum.hint = "Driver's License Number"
                        tilLicenseNum.helperText = "Enter your valid DVLA license number"
                    }
                }
            }
        }
        
        // Initial state
        tilRegNum.visibility = View.GONE
    }

    private fun validateForm(): Boolean {
        val name = findViewById<TextInputEditText>(R.id.etName).text?.toString() ?: ""
        val email = findViewById<TextInputEditText>(R.id.etEmail).text?.toString() ?: ""
        val phone = findViewById<TextInputEditText>(R.id.etPhone).text?.toString() ?: ""
        val license = findViewById<TextInputEditText>(R.id.etLicenseNumber).text?.toString() ?: ""
        val region = findViewById<AutoCompleteTextView>(R.id.actRegion).text?.toString() ?: ""
        val e1 = findViewById<TextInputEditText>(R.id.etEmergency1).text?.toString() ?: ""
        val e2 = findViewById<TextInputEditText>(R.id.etEmergency2).text?.toString() ?: ""
        val pass = findViewById<TextInputEditText>(R.id.etPassword).text?.toString() ?: ""
        val confirmPass = findViewById<TextInputEditText>(R.id.etConfirmPassword).text?.toString() ?: ""
        
        val roleId = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleRole).checkedButtonId
        val regNum = findViewById<TextInputEditText>(R.id.etRegistrationNumber).text?.toString() ?: ""
        val companyName = findViewById<TextInputEditText>(R.id.etCompanyName).text?.toString() ?: ""

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || region.isEmpty() || e1.isEmpty() || e2.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (roleId == R.id.btnRoleDriver || roleId == R.id.btnRoleBoth) {
            if (license.isEmpty()) {
                Toast.makeText(this, "Please enter vehicle registration number", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        if (roleId == R.id.btnRoleOwner || roleId == R.id.btnRoleBoth) {
            if (companyName.isEmpty() || regNum.isEmpty()) {
                Toast.makeText(this, "Please enter company name and registration number", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun submitRegistration() {
        val name = findViewById<TextInputEditText>(R.id.etName).text?.toString() ?: ""
        val email = findViewById<TextInputEditText>(R.id.etEmail).text?.toString() ?: ""
        val phone = findViewById<TextInputEditText>(R.id.etPhone).text?.toString() ?: ""
        val password = findViewById<TextInputEditText>(R.id.etPassword).text?.toString() ?: ""
        val licenseNum = findViewById<TextInputEditText>(R.id.etLicenseNumber).text?.toString() ?: ""
        val region = findViewById<AutoCompleteTextView>(R.id.actRegion).text?.toString() ?: ""
        val emergency1 = findViewById<TextInputEditText>(R.id.etEmergency1).text?.toString() ?: ""
        val emergency2 = findViewById<TextInputEditText>(R.id.etEmergency2).text?.toString() ?: ""
        val companyName = findViewById<TextInputEditText>(R.id.etCompanyName).text?.toString() ?: ""
        val regNum = findViewById<TextInputEditText>(R.id.etRegistrationNumber).text?.toString() ?: ""
        val roleId = findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleRole).checkedButtonId
        
        val userRole = when (roleId) {
            R.id.btnRoleOwner -> "OWNER"
            R.id.btnRoleBoth -> "BOTH"
            else -> "DRIVER"
        }
        
        findViewById<MaterialButton>(R.id.btnSubmit).isEnabled = false
        Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

        val googleUid = intent.getStringExtra("google_uid")
        val normalizedPhone = normalizePhone(phone)

        lifecycleScope.launch {
            repository.driverRegister(
                name = name,
                email = email,
                phone = normalizedPhone,
                password = password,
                licenseNumber = licenseNum, // Assign driver's license number
                region = region,
                vehicleType = selectedVehicleType,
                serviceType = "BOTH",    
                vehicleNumber = licenseNum, // Assign it here too
                emergencyContact1 = emergency1,
                emergencyContact2 = emergency2,
                docs = emptyMap(),
                userRole = userRole,
                companyName = companyName,
                registrationNumber = if (userRole == "DRIVER") "DRV-" + (System.currentTimeMillis() % 1000000) else regNum,
                firebaseUid = googleUid
            ).onSuccess {
                Toast.makeText(this@DriverSignupActivity, "Registration successful! You can now log in.", Toast.LENGTH_LONG).show()
                finish()
            }.onFailure {
                findViewById<MaterialButton>(R.id.btnSubmit).isEnabled = true
                Toast.makeText(this@DriverSignupActivity, "Error: ${it.message}", Toast.LENGTH_LONG).show()
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