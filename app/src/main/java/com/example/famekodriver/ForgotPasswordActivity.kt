package com.example.famekodriver

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.famekodriver.core.data.repository.DriverRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {
    private val repository = DriverRepository()
    private var isOtpSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val llResetFields = findViewById<LinearLayout>(R.id.llResetFields)
        val etOtp = findViewById<TextInputEditText>(R.id.etOtp)
        val etNewPassword = findViewById<TextInputEditText>(R.id.etNewPassword)
        val btnAction = findViewById<MaterialButton>(R.id.btnAction)

        toolbar.setNavigationOnClickListener { finish() }

        btnAction.setOnClickListener {
            val email = etEmail.text?.toString()?.trim() ?: ""
            
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOtpSent) {
                // Flow 1: Request OTP
                btnAction.isEnabled = false
                lifecycleScope.launch {
                    repository.forgotPassword(email).onSuccess { resp ->
                        if (resp.success) {
                            isOtpSent = true
                            tilEmail.isEnabled = false
                            llResetFields.visibility = View.VISIBLE
                            btnAction.text = "Update Password"
                            btnAction.isEnabled = true
                            Toast.makeText(this@ForgotPasswordActivity, "Check your email for the code", Toast.LENGTH_LONG).show()
                        } else {
                            btnAction.isEnabled = true
                            Toast.makeText(this@ForgotPasswordActivity, resp.message ?: "Error", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure {
                        btnAction.isEnabled = true
                        Toast.makeText(this@ForgotPasswordActivity, "Failed to send code", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Flow 2: Verify OTP and Reset
                val otp = etOtp.text?.toString()?.trim() ?: ""
                val newPass = etNewPassword.text?.toString()?.trim() ?: ""

                if (otp.length < 6 || newPass.isEmpty()) {
                    Toast.makeText(this, "Enter the 6-digit code and new password", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                btnAction.isEnabled = false
                lifecycleScope.launch {
                    repository.resetPassword(email, otp, newPass).onSuccess { resp ->
                        if (resp.success) {
                            Toast.makeText(this@ForgotPasswordActivity, "Password reset successful! Please login.", Toast.LENGTH_LONG).show()
                            finish()
                        } else {
                            btnAction.isEnabled = true
                            Toast.makeText(this@ForgotPasswordActivity, resp.message ?: "Invalid code", Toast.LENGTH_SHORT).show()
                        }
                    }.onFailure {
                        btnAction.isEnabled = true
                        Toast.makeText(this@ForgotPasswordActivity, "Reset failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
