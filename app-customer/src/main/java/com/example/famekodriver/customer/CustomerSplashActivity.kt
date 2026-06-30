package com.example.famekodriver.customer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import com.example.famekodriver.core.data.SessionManager
import com.example.famekodriver.core.utils.ImageLinks
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class CustomerSplashActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_splash)

        sessionManager = SessionManager(this)
        val ivSplash = findViewById<ImageView>(R.id.ivSplash)
        
        ivSplash.load(ImageLinks.CUSTOMER_SPLASH_SCREEN) {
            listener(
                onSuccess = { _, _ ->
                    lifecycleScope.launch {
                        delay(2000) // Show for 2 seconds after loading
                        proceed()
                    }
                },
                onError = { _, _ ->
                    proceed() // Proceed anyway if image fails
                }
            )
        }
    }

    private fun proceed() {
        if (sessionManager.isLoggedIn()) {
            navigateToCustomerMap()
        } else {
            startActivity(Intent(this, CustomerLoginActivity::class.java))
            finish()
        }
    }

    private fun navigateToCustomerMap() {
        val intent = Intent().setClassName(this, "com.example.famekodriver.customer.CustomerMapActivity")
        startActivity(intent)
        finish()
    }
}
