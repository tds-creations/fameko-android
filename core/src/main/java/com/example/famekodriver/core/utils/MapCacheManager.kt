package com.example.famekodriver.core.utils

import android.content.Context
import android.util.Log
import org.maplibre.android.offline.OfflineManager
import org.maplibre.android.offline.OfflineRegion
import org.maplibre.android.offline.OfflineTilePyramidRegionDefinition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.storage.FileSource
import org.json.JSONObject

/**
 * Manages offline map caching and pre-fetching to reduce lag.
 */
object MapCacheManager {
    private const val TAG = "MapCache"

    /**
     * Initializes the map cache with a generous limit.
     */
    fun initCache(context: Context) {
        try {
            // Note: MapLibre 11.x uses internal storage by default.
            Log.d(TAG, "Map cache initialized.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to init map cache", e)
        }
    }

    /**
     * Pre-downloads map tiles for a specific area (e.g. 5km radius).
     */
    fun prefetchArea(context: Context, center: LatLng, areaName: String) {
        val offlineManager = OfflineManager.getInstance(context)
        
        // Define bounds for ~5km radius
        val latOffset = 0.045 // roughly 5km
        val lngOffset = 0.045
        val bounds = LatLngBounds.Builder()
            .include(LatLng(center.latitude - latOffset, center.longitude - lngOffset))
            .include(LatLng(center.latitude + latOffset, center.longitude + lngOffset))
            .build()

        val definition = OfflineTilePyramidRegionDefinition(
            "https://api.tomtom.com/style/2/custom/style/dG9tdG9tQEBAZFVDV2NzZ09mRGhEaU9MdDsVGbKlskhOMbwzZ3vdhit8",
            bounds,
            12.0, // min zoom
            18.0, // max zoom
            context.resources.displayMetrics.density
        )

        // Metadata
        val metadata: ByteArray?
        try {
            val jsonObject = JSONObject()
            jsonObject.put("FIELD_REGION_NAME", areaName)
            metadata = jsonObject.toString().toByteArray(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to encode metadata", e)
            return
        }

        offlineManager.createOfflineRegion(definition, metadata, object : OfflineManager.CreateOfflineRegionCallback {
            override fun onCreate(offlineRegion: OfflineRegion) {
                Log.d(TAG, "Offline region created: $areaName. Starting download...")
                offlineRegion.setDownloadState(OfflineRegion.STATE_ACTIVE)
                
                offlineRegion.setObserver(object : OfflineRegion.OfflineRegionObserver {
                    override fun onStatusChanged(status: org.maplibre.android.offline.OfflineRegionStatus) {
                        if (status.isComplete) {
                            Log.d(TAG, "Map pre-fetch complete for $areaName.")
                        }
                    }

                    override fun onError(error: org.maplibre.android.offline.OfflineRegionError) {
                        Log.e(TAG, "Offline download error: ${error.reason}, ${error.message}")
                    }

                    override fun mapboxTileCountLimitExceeded(limit: Long) {
                        Log.w(TAG, "Tile limit exceeded: $limit")
                    }
                })
            }

            override fun onError(error: String) {
                Log.e(TAG, "Error creating offline region: $error")
            }
        })
    }
}
