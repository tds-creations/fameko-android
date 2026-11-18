package com.example.famekodriver.core.utils

import android.content.Context
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.famekodriver.core.domain.model.RouteInstruction
import java.util.*
import kotlin.math.*

class VoiceNavigationManager(context: Context) : TextToSpeech.OnInitListener {
    private val appContext: Context = context.applicationContext
    private var tts: TextToSpeech = TextToSpeech(appContext, this)
    private var isInitialized = false
    private var isEnabled = true

    private var lastEtaAnnouncementTime = 0L
    private val etaInterval = 120_000L // 2 minutes

    private var lastInstructionIndex = -1
    private var lastAnnouncedStage = 0 // 0 = none, 300 = 300m, 100 = 100m, 25 = now

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS || focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            tts.stop()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("VoiceNav", "Language not supported")
            } else {
                isInitialized = true
                setupUtteranceListener()
            }
        } else {
            Log.e("VoiceNav", "TTS Initialization failed")
        }
    }

    private fun setupUtteranceListener() {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                abandonAudioFocus()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                abandonAudioFocus()
            }
        })
    }

    fun setEnabled(enabled: Boolean) {
        this.isEnabled = enabled
        if (!enabled) {
            tts.stop()
            abandonAudioFocus()
        }
    }

    fun resetAnnouncementState() {
        lastInstructionIndex = -1
        lastAnnouncedStage = 0
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()

            val focusReq = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(focusChangeListener)
                .build()

            audioFocusRequest = focusReq
            return audioManager.requestAudioFocus(focusReq) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            return audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(focusChangeListener)
        }
    }

    fun speak(text: String) {
        if (!isEnabled || !isInitialized || text.isBlank()) return
        requestAudioFocus()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "NavUtterance_${System.currentTimeMillis()}")
    }

    fun announceTripStart(destination: String) {
        resetAnnouncementState()
        speak("Starting navigation to $destination")
    }

    fun announceArrivedAtPickup() {
        resetAnnouncementState()
        speak("You have arrived at the pickup location.")
    }

    fun announceArrival() {
        resetAnnouncementState()
        speak("You have arrived at your destination.")
    }

    fun announceStructuredInstruction(instruction: RouteInstruction?, distToManeuverMeters: Double, index: Int) {
        if (!isEnabled || !isInitialized || instruction == null) return

        val text = instruction.text?.takeIf { it.isNotBlank() } ?: return

        if (index != lastInstructionIndex) {
            lastInstructionIndex = index
            lastAnnouncedStage = 0
        }

        when {
            distToManeuverMeters in 250.0..350.0 && lastAnnouncedStage < 300 -> {
                speak("In 300 meters, $text")
                lastAnnouncedStage = 300
            }
            distToManeuverMeters in 80.0..130.0 && lastAnnouncedStage < 100 -> {
                speak("In 100 meters, $text")
                lastAnnouncedStage = 100
            }
            distToManeuverMeters < 25.0 && lastAnnouncedStage < 25 -> {
                speak(text)
                lastAnnouncedStage = 25
            }
        }
    }

    fun updateProgress(
        currentLat: Double,
        currentLng: Double,
        route: List<Pair<Double, Double>>,
        etaMin: Double,
        distanceKm: Double,
        currentInstruction: RouteInstruction? = null,
        distToManeuverMeters: Double = 0.0,
        instructionIndex: Int = 0
    ) {
        if (!isEnabled || !isInitialized) return

        val now = System.currentTimeMillis()

        // Periodic ETA announcement
        if (now - lastEtaAnnouncementTime > etaInterval && distanceKm > 0.5) {
            val etaText = if (etaMin < 1.0) "less than a minute" else "${etaMin.toInt()} minutes"
            speak("Remaining time to destination: $etaText. Distance: ${String.format(Locale.US, "%.1f", distanceKm)} kilometers.")
            lastEtaAnnouncementTime = now
        }

        // Structured Instruction Voice Guidance
        if (currentInstruction != null) {
            announceStructuredInstruction(currentInstruction, distToManeuverMeters, instructionIndex)
        } else if (route.isNotEmpty()) {
            // Fallback bearing calculation
            detectAndAnnounceTurn(currentLat, currentLng, route)
        }
    }

    private fun detectAndAnnounceTurn(currentLat: Double, currentLng: Double, route: List<Pair<Double, Double>>) {
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

        var turnIdx = -1
        for (i in closestIndex until minStore(closestIndex + 20, route.size - 2)) {
            val b1 = calculateBearing(route[i], route[i + 1])
            val b2 = calculateBearing(route[i + 1], route[i + 2])
            
            var diff = abs(b1 - b2)
            if (diff > 180) diff = 360 - diff

            if (diff > 35.0) {
                turnIdx = i + 1
                break
            }
        }

        if (turnIdx != -1 && turnIdx != lastInstructionIndex) {
            val distToTurn = calculateDistance(currentLat, currentLng, route[turnIdx].first, route[turnIdx].second)
            
            if (distToTurn in 250.0..350.0 && lastAnnouncedStage < 300) {
                val direction = getTurnDirection(route[turnIdx-1], route[turnIdx], route[turnIdx+1])
                speak("In 300 meters, $direction")
                lastInstructionIndex = turnIdx
                lastAnnouncedStage = 300
            } else if (distToTurn in 80.0..120.0 && lastAnnouncedStage < 100) {
                val direction = getTurnDirection(route[turnIdx-1], route[turnIdx], route[turnIdx+1])
                speak("In 100 meters, $direction")
                lastInstructionIndex = turnIdx
                lastAnnouncedStage = 100
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
        return results[1]
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
        abandonAudioFocus()
    }
}
