package com.example.famekodriver.core.network

import android.content.Context
import android.util.Log
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.FamekoEvent
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed interface AgoraCallState {
    object Idle : AgoraCallState
    data class Ringing(val channelName: String, val callerName: String, val orderId: Int, val initiatorId: String) : AgoraCallState
    data class Dialing(val channelName: String, val recipientName: String, val orderId: Int) : AgoraCallState
    data class Active(val channelName: String, val remoteName: String, val orderId: Int) : AgoraCallState
    data class Ended(val reason: String) : AgoraCallState
}

open class AgoraCallManager(
    private val context: Context,
    private val repository: DriverRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val appId = "38144168c418426faf0ee6a02c9b46e4"

    private var rtcEngine: RtcEngine? = null

    private val _callState = MutableStateFlow<AgoraCallState>(AgoraCallState.Idle)
    val callState: StateFlow<AgoraCallState> = _callState

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private var activeChannelName: String? = null

    init {
        initializeAgoraEngine()
        listenToSignalingEvents()
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(context, appId, object : IRtcEngineEventHandler() {
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d("AgoraCall", "Remote user joined channel: $uid")
                    val channel = activeChannelName ?: ""
                    val currentState = _callState.value
                    val remoteName = when (currentState) {
                        is AgoraCallState.Dialing -> currentState.recipientName
                        is AgoraCallState.Ringing -> currentState.callerName
                        is AgoraCallState.Active -> currentState.remoteName
                        else -> "Connected"
                    }
                    _callState.value = AgoraCallState.Active(channel, remoteName, 0)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d("AgoraCall", "Remote user went offline: $uid, reason: $reason")
                    endCallLocally("Call Ended")
                }

                override fun onConnectionLost() {
                    Log.e("AgoraCall", "Agora Connection Lost")
                    endCallLocally("Connection Lost")
                }

                override fun onError(err: Int) {
                    Log.e("AgoraCall", "Agora Error Code: $err")
                }
            })

            // Configure for Voice Communication
            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_GAME_STREAMING
            )
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            rtcEngine?.enableAudio()
            rtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            Log.d("AgoraCall", "Agora RTC Engine initialized successfully.")
        } catch (e: Exception) {
            Log.e("AgoraCall", "Failed to initialize Agora RTC Engine", e)
        }
    }

    private fun listenToSignalingEvents() {
        scope.launch {
            repository.events.collect { event ->
                when (event) {
                    is FamekoEvent.IncomingCall -> {
                        activeChannelName = event.callId
                        _callState.value = AgoraCallState.Ringing(event.callId, event.callerName, 0, "")
                    }
                    is FamekoEvent.CallAccepted -> {
                        val channelName = event.callId
                        activeChannelName = channelName
                        rtcEngine?.joinChannel(null, channelName, null, 0)
                        _callState.value = AgoraCallState.Active(channelName, "Connected", 0)
                    }
                    is FamekoEvent.CallRejected -> {
                        endCallLocally(event.reason ?: "Call Rejected")
                    }
                    is FamekoEvent.CallEnded -> {
                        endCallLocally("Call Ended")
                    }
                    is FamekoEvent.Unknown -> {
                        val type = event.type
                        val data = event.data
                        when (type) {
                            "CALL_INCOMING" -> {
                                val callId = data["call_id"]?.toString() ?: ""
                                val callerName = data["caller_name"]?.toString() ?: "Incoming Call"
                                val orderId = (data["order_id"] as? Number)?.toInt() ?: 0
                                val initiatorId = data["initiator_id"]?.toString() ?: ""
                                activeChannelName = callId
                                _callState.value = AgoraCallState.Ringing(callId, callerName, orderId, initiatorId)
                            }
                            "CALL_ACCEPTED" -> {
                                val channelName = data["call_id"]?.toString() ?: ""
                                activeChannelName = channelName
                                rtcEngine?.joinChannel(null, channelName, null, 0)
                                _callState.value = AgoraCallState.Active(channelName, "Connected", 0)
                            }
                            "CALL_REJECTED" -> {
                                endCallLocally("Call Rejected")
                            }
                            "CALL_ENDED" -> {
                                endCallLocally("Call Ended")
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun startOutgoingCall(targetId: String, callerName: String, orderId: Int) {
        val channelName = "fameko_order_$orderId"
        activeChannelName = channelName
        _callState.value = AgoraCallState.Dialing(channelName, "Calling...", orderId)

        rtcEngine?.joinChannel(null, channelName, null, 0)

        scope.launch {
            repository.initiateCall(targetId, callerName, orderId)
        }
    }

    fun acceptIncomingCall(callId: String, callerName: String, orderId: Int) {
        val channelName = if (callId.startsWith("fameko_order_")) callId else "fameko_order_$orderId"
        activeChannelName = channelName
        _callState.value = AgoraCallState.Active(channelName, callerName, orderId)

        rtcEngine?.joinChannel(null, channelName, null, 0)

        scope.launch {
            repository.acceptCall(callId)
        }
    }

    fun rejectCall(callId: String) {
        scope.launch {
            repository.rejectCall(callId, "Rejected")
            endCallLocally("Call Rejected")
        }
    }

    fun endCall() {
        val channel = activeChannelName ?: ""
        scope.launch {
            if (channel.isNotBlank()) {
                repository.endCall(channel)
            }
            endCallLocally("Call Ended")
        }
    }

    fun toggleMicrophone(): Boolean {
        val newMuteState = !_isMicMuted.value
        _isMicMuted.value = newMuteState
        rtcEngine?.muteLocalAudioStream(newMuteState)
        return newMuteState
    }

    fun toggleSpeaker(): Boolean {
        val newSpeakerState = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeakerState
        rtcEngine?.setDefaultAudioRoutetoSpeakerphone(newSpeakerState)
        rtcEngine?.setEnableSpeakerphone(newSpeakerState)
        return newSpeakerState
    }

    fun endCallLocally(reason: String) {
        try {
            rtcEngine?.leaveChannel()
            activeChannelName = null
            _isMicMuted.value = false
            _callState.value = AgoraCallState.Ended(reason)
            scope.launch {
                delay(1500)
                _callState.value = AgoraCallState.Idle
            }
        } catch (e: Exception) {
            Log.e("AgoraCall", "Error leaving channel", e)
            _callState.value = AgoraCallState.Idle
        }
    }

    fun cleanup() {
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
        } catch (e: Exception) {
            Log.e("AgoraCall", "Error destroying Agora Engine", e)
        }
    }
}
