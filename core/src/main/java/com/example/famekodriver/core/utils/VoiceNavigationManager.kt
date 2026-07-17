package com.example.famekodriver.core.utils

import android.content.Context
import android.location.Location
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.*
import kotlin.math.*

class VoiceNavigationManager(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isInitialized = false
    private var lastEtaAnnouncementTime = 0L
    private val etaInterval = 120_000L // 2 minutes

    private var nextTurnIndex = -1
    private var lastAnnouncedTurnIndex = -1
    private var isEnabled = true

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceNav", "Language not supported")
            } else {
                isInitialized = true
            }
        } else {
            Log.e("VoiceNav", "TTS Initialization failed")
        }
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        if (!enabled) tts.stop()
    }

    fun speak(text: String) {
        if (!isEnabled || !isInitialized) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun announceTripStart(destination: String) {
        speak("Starting navigation to $destination")
    }

    fun announceArrival() {
        speak("You have arrived at your destination.")
    }

    fun updateProgress(
        currentLat: Double,
        currentLng: Double,
        route: List<Pair<Double, Double>>,
        etaMin: Double,
        distanceKm: Double
    ) {
        if (!isEnabled || !isInitialized || route.isEmpty()) return

        val now = System.currentTimeMillis()

        // 1. Periodic ETA announcement
        if (now - lastEtaAnnouncementTime > etaInterval && distanceKm > 0.5) {
            val etaText = if (etaMin < 1.0) "less than a minute" else "${etaMin.toInt()} minutes"
            speak("Remaining time to destination: $etaText. Distance: ${String.format("%.1f", distanceKm)} kilometers.")
            lastEtaAnnouncementTime = now
        }

        // 2. Turn Detection (Basic)
        detectAndAnnounceTurn(currentLat, currentLng, route)
    }

    private fun detectAndAnnounceTurn(currentLat: Double, currentLng: Double, route: List<Pair<Double, Double>>) {
        // Find the closest point on the route
        var minDistance = Double.MAX_VALUE
        var closestIndex = -1

        for (i in route.indices) {
            val dist = calculateDistance(currentLat, currentLng, route[i].first, route[i].second)
            if (dist < minDistance) {
                minDistance = dist
                closestIndex = i
            }
        }

        if (closestIndex == -1) return

        // Look ahead for a significant bearing change (> 30 degrees)
        // This is a simplified turn detection logic
        var turnIdx = -1
        for (i in closestIndex until minStore(closestIndex + 20, route.size - 2)) {
            val b1 = calculateBearing(route[i], route[i + 1])
            val b2 = calculateBearing(route[i + 1], route[i + 2])
            
            var diff = abs(b1 - b2)
            if (diff > 180) diff = 360 - diff

            if (diff > 35.0) { // Found a turn
                turnIdx = i + 1
                break
            }
        }

        if (turnIdx != -1 && turnIdx != lastAnnouncedTurnIndex) {
            val distToTurn = calculateDistance(currentLat, currentLng, route[turnIdx].first, route[turnIdx].second)
            
            // Announce at 300m and 100m
            if (distToTurn in 250.0..350.0) {
                val direction = getTurnDirection(route[turnIdx-1], route[turnIdx], route[turnIdx+1])
                speak("In 300 meters, $direction")
                lastAnnouncedTurnIndex = turnIdx
            } else if (distToTurn in 80.0..120.0) {
                val direction = getTurnDirection(route[turnIdx-1], route[turnIdx], route[turnIdx+1])
                speak("In 100 meters, $direction")
                lastAnnouncedTurnIndex = turnIdx
            }
        }
    }

    private fun minStore(a: Int, b: Int) = if (a < b) a else b

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0].toDouble()
    }

    private fun calculateBearing(p1: Pair<Double, Double>, p2: Pair<Double, Double>): Float {
        val results = FloatArray(2)
        Location.distanceBetween(p1.first, p1.second, p2.first, p2.second, results)
        return results[1] // Initial bearing
    }

    private fun getTurnDirection(p1: Pair<Double, Double>, p2: Pair<Double, Double>, p3: Pair<Double, Double>): String {
        val b1 = calculateBearing(p1, p2)
        val b2 = calculateBearing(p2, p3)
        
        var diff = b2 - b1
        while (diff > 180) diff -= 360
        while (diff < -180) diff += 360

        return if (diff > 0) "turn right" else "turn left"
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
