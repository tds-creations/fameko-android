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

    /**
     * Calculates the minimum distance from a point to a line segment.
     */
    fun distanceToSegment(pLat: Double, pLng: Double, aLat: Double, aLng: Double, bLat: Double, bLng: Double): Double {
        val results = FloatArray(1)
        
        // Vector AB
        val dLat = bLat - aLat
        val dLng = bLng - aLng
        
        if (dLat == 0.0 && dLng == 0.0) {
            Location.distanceBetween(pLat, pLng, aLat, aLng, results)
            return results[0].toDouble()
        }
        
        // Project point P onto line AB to find point C
        val t = ((pLat - aLat) * dLat + (pLng - aLng) * dLng) / (dLat * dLat + dLng * dLng)
        
        return when {
            t < 0.0 -> {
                Location.distanceBetween(pLat, pLng, aLat, aLng, results)
                results[0].toDouble()
            }
            t > 1.0 -> {
                Location.distanceBetween(pLat, pLng, bLat, bLng, results)
                results[0].toDouble()
            }
            else -> {
                val cLat = aLat + t * dLat
                val cLng = aLng + t * dLng
                Location.distanceBetween(pLat, pLng, cLat, cLng, results)
                results[0].toDouble()
            }
        }
    }

    /**
     * Projects a point P onto segment AB and returns projected LatLng, distance in meters, and segment fraction t.
     */
    fun projectPointOnSegment(pLat: Double, pLng: Double, aLat: Double, aLng: Double, bLat: Double, bLng: Double): ProjectionResult {
        val dLat = bLat - aLat
        val dLng = bLng - aLng
        
        if (dLat == 0.0 && dLng == 0.0) {
            val dist = calculateDistance(pLat, pLng, aLat, aLng)
            return ProjectionResult(aLat, aLng, dist, 0.0)
        }
        
        val t = (((pLat - aLat) * dLat + (pLng - aLng) * dLng) / (dLat * dLat + dLng * dLng)).coerceIn(0.0, 1.0)
        val cLat = aLat + t * dLat
        val cLng = aLng + t * dLng
        val dist = calculateDistance(pLat, pLng, cLat, cLng)
        return ProjectionResult(cLat, cLng, dist, t)
    }
}

data class ProjectionResult(
    val projectedLat: Double,
    val projectedLng: Double,
    val distanceMeters: Double,
    val fraction: Double
)
