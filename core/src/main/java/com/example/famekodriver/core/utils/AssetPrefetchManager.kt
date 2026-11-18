package com.example.famekodriver.core.utils

import android.content.Context
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * High-performance controlled background asset pre-fetcher.
 * Silently caches critical UI artwork asynchronously without blocking app startup or network traffic.
 */
object AssetPrefetchManager {

    /**
     * Pre-fetches primary customer landing screen posters and hero banners.
     */
    fun prefetchCustomerAssets(context: Context) {
        val criticalAssets = listOf(
            ImageLinks.RIDE,
            ImageLinks.SCHEDULE,
            ImageLinks.DELIVERY,
            ImageLinks.RENTAL,
            ImageLinks.HERO_OKADA_EXPRESS,
            ImageLinks.HERO_ABOBOYAA_CARGO,
            ImageLinks.HERO_COMFORT_RIDE
        )

        enqueueBackgroundPrefetch(context, criticalAssets)
    }

    /**
     * Pre-fetches primary driver app posters and splash graphics.
     */
    fun prefetchDriverAssets(context: Context) {
        val criticalAssets = listOf(
            ImageLinks.DRIVER_SPLASH_SCREEN,
            ImageLinks.IC_OKADA,
            ImageLinks.IC_PRAGYA,
            ImageLinks.IC_ABOBOYAA,
            ImageLinks.IC_CAR_SALOON
        )

        enqueueBackgroundPrefetch(context, criticalAssets)
    }

    private fun enqueueBackgroundPrefetch(context: Context, urls: List<String>) {
        val loader = context.imageLoader

        CoroutineScope(Dispatchers.IO).launch {
            urls.forEach { url ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .listener(
                            onSuccess = { _, _ -> },
                            onError = { _, _ -> }
                        )
                        .build()
                    loader.enqueue(request)
                } catch (_: Exception) {
                    // Silent fail-safe for background pre-fetching
                }
            }
        }
    }
}
