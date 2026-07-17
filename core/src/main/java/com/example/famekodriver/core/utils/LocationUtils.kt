package com.example.famekodriver.core.utils

import android.location.Location

object LocationUtils {
    /**
     * Calculates the distance in meters between two coordinates.
     */
    fun calculateDistance(startLat: Double, startLng: Double, endLat: Double, endLng: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            startLat, startLng,
            endLat, endLng,
            results
        )
        return results[0].toDouble()
    }
}
