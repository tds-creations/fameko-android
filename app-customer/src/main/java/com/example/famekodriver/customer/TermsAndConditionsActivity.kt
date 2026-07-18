package com.example.famekodriver.customer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class TermsAndConditionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TermsAndConditionsScreen(onBack = { finish() })
        }
    }
}
