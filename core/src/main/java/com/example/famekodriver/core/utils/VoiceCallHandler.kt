package com.example.famekodriver.core.utils

import android.annotation.SuppressLint
import android.media.*
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicBoolean

class VoiceCallHandler(private val onAudioData: (ByteArray) -> Unit) {
    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private val isRecording = AtomicBoolean(false)
    private val isPlaying = AtomicBoolean(false)
    private var recordingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording.get()) return
        
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("VoiceCallHandler", "AudioRecord initialization failed")
                return
            }

            audioRecord?.startRecording()
            isRecording.set(true)

            recordingJob = scope.launch {
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        onAudioData(buffer.copyOf(read))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceCallHandler", "Error starting recording", e)
        }
    }

    fun stopRecording() {
        isRecording.set(false)
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
        } catch (e: Exception) {
            Log.e("VoiceCallHandler", "Error stopping recording", e)
        }
    }

    fun startPlayback() {
        if (isPlaying.get()) return

        try {
            val outBufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                audioFormat
            )

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(outBufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            isPlaying.set(true)
        } catch (e: Exception) {
            Log.e("VoiceCallHandler", "Error starting playback", e)
        }
    }

    fun receiveAudio(data: ByteArray) {
        if (isPlaying.get()) {
            audioTrack?.write(data, 0, data.size)
        }
    }

    fun stopPlayback() {
        isPlaying.set(false)
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e("VoiceCallHandler", "Error stopping playback", e)
        }
    }

    fun cleanup() {
        stopRecording()
        stopPlayback()
    }
}
