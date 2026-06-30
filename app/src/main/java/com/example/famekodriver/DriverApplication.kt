package com.example.famekodriver

import android.app.Application
import org.osmdroid.config.Configuration
import androidx.preference.PreferenceManager
import org.maplibre.android.MapLibre
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.OkHttpClient
import com.example.famekodriver.core.network.NetworkClient

class DriverApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize MapLibre FIRST with required parameters for version 11.x
        MapLibre.getInstance(this, null, WellKnownTileServer.MapLibre)

        // 2. Configure HTTP Client with User-Agent and Automatic TomTom Key Injection
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val originalUrl = originalRequest.url

                // Automatically add API key to all TomTom requests if not present
                val newRequest = if (originalUrl.host.contains("tomtom.com") && originalUrl.queryParameter("key") == null) {
                    val newUrl = originalUrl.newBuilder()
                        .addQueryParameter("key", NetworkClient.TOMTOM_API_KEY)
                        .build()
                    originalRequest.newBuilder()
                        .url(newUrl)
                        .header("User-Agent", packageName)
                        .build()
                } else {
                    originalRequest.newBuilder()
                        .header("User-Agent", packageName)
                        .build()
                }
                
                chain.proceed(newRequest)
            }
            .build()
        
        HttpRequestUtil.setOkHttpClient(client)
        
        // Initialize osmdroid configuration
        Configuration.getInstance().load(
            this,
            PreferenceManager.getDefaultSharedPreferences(this)
        )
        Configuration.getInstance().userAgentValue = packageName
    }
}
