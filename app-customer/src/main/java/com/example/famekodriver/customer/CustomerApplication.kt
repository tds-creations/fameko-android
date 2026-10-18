package com.example.famekodriver.customer

import android.app.Application
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil
import okhttp3.OkHttpClient
import com.example.famekodriver.core.network.NetworkClient
import com.example.famekodriver.core.utils.MapCacheManager

import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

class CustomerApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .build()
    }
    override fun onCreate() {
        super.onCreate()

        // 1. Initialize MapLibre
        MapLibre.getInstance(this)
        MapCacheManager.initCache(this)

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
    }
}
