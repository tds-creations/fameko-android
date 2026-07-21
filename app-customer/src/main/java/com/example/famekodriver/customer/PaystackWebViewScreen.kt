package com.example.famekodriver.customer

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import com.example.famekodriver.customer.ui.theme.*

/**
 * A professional in-app payment screen using Paystack Checkout.
 * This is the industry standard for handling multi-method payments (Card, MoMo, Bank)
 * while keeping the user inside the application.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaystackWebViewScreen(
    url: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Payment", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        @SuppressLint("SetJavaScriptEnabled")
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportMultipleWindows(true)
                        
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val currentUrl = request?.url.toString()
                                
                                // Industry Standard: Detect success markers in URL
                                if (currentUrl.contains("checkout.paystack.com/success") ||
                                    currentUrl.contains("/success") ||
                                    currentUrl.contains("transaction/confirm")) {
                                    onSuccess()
                                    return true
                                }
                                
                                // Handle potential MoMo deep links (tel:, intent:, etc)
                                if (currentUrl.startsWith("tel:") || currentUrl.startsWith("intent:")) {
                                    return true 
                                }

                                return false
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = BoltGreen
                )
            }
        }
    }
}
