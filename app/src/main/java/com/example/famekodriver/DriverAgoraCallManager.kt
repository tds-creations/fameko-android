package com.example.famekodriver

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

sealed interface DriverCallState {
    object Idle : DriverCallState
    data class Ringing(val channelName: String, val callerName: String, val orderId: Int, val initiatorId: String) : DriverCallState
    data class Dialing(val channelName: String, val recipientName: String, val orderId: Int) : DriverCallState
    data class Active(val channelName: String, val remoteName: String, val orderId: Int) : DriverCallState
    data class Ended(val reason: String) : DriverCallState
}

class DriverAgoraCallManager(
    private val context: Context,
    private val repository: DriverRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val appId = "38144168c418426faf0ee6a02c9b46e4"

    private var rtcEngine: RtcEngine? = null

    private val _callState = MutableStateFlow<DriverCallState>(DriverCallState.Idle)
    val callState: StateFlow<DriverCallState> = _callState

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
                    Log.d("DriverAgoraCall", "Customer joined Agora channel: $uid")
                    val channel = activeChannelName ?: ""
                    val currentState = _callState.value
                    val remoteName = when (currentState) {
                        is DriverCallState.Dialing -> currentState.recipientName
                        is DriverCallState.Ringing -> currentState.callerName
                        is DriverCallState.Active -> currentState.remoteName
                        else -> "Customer"
                    }
                    _callState.value = DriverCallState.Active(channel, remoteName, 0)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d("DriverAgoraCall", "Customer left Agora channel: $uid")
                    endCallLocally("Customer Ended Call")
                }

                override fun onConnectionLost() {
                    Log.e("DriverAgoraCall", "Driver Connection Lost")
                    endCallLocally("Connection Lost")
                }

                override fun onError(err: Int) {
                    Log.e("DriverAgoraCall", "Driver Agora Error: $err")
                }
            })

            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_GAME_STREAMING
            )
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            rtcEngine?.enableAudio()
            rtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)

            Log.d("DriverAgoraCall", "Driver Agora RTC Engine Initialized.")
        } catch (e: Exception) {
            Log.e("DriverAgoraCall", "Failed to initialize Driver Agora Engine", e)
        }
    }

    private fun listenToSignalingEvents() {
        scope.launch {
            repository.events.collect { event ->
                when (event) {
                    is FamekoEvent.IncomingCall -> {
                        activeChannelName = event.callId
                        _callState.value = DriverCallState.Ringing(event.callId, event.callerName, 0, "")
                    }
                    is FamekoEvent.CallAccepted -> {
                        val channelName = event.callId
                        activeChannelName = channelName
                        rtcEngine?.joinChannel(null, channelName, null, 0)
                        _callState.value = DriverCallState.Active(channelName, "Customer", 0)
                    }
                    is FamekoEvent.CallRejected -> {
                        endCallLocally(event.reason ?: "Customer Rejected Call")
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
                                val callerName = data["caller_name"]?.toString() ?: "Customer"
                                val orderId = (data["order_id"] as? Number)?.toInt() ?: 0
                                val initiatorId = data["initiator_id"]?.toString() ?: ""
                                activeChannelName = callId
                                _callState.value = DriverCallState.Ringing(callId, callerName, orderId, initiatorId)
                            }
                            "CALL_ACCEPTED" -> {
                                val channelName = data["call_id"]?.toString() ?: ""
                                activeChannelName = channelName
                                rtcEngine?.joinChannel(null, channelName, null, 0)
                                _callState.value = DriverCallState.Active(channelName, "Customer", 0)
                            }
                            "CALL_REJECTED" -> {
                                endCallLocally("Customer Rejected Call")
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
        _callState.value = DriverCallState.Dialing(channelName, "Calling Customer...", orderId)

        rtcEngine?.joinChannel(null, channelName, null, 0)

        scope.launch {
            repository.initiateCall(targetId, callerName, orderId)
        }
    }

    fun acceptIncomingCall(callId: String, callerName: String, orderId: Int) {
        val channelName = if (callId.startsWith("fameko_order_")) callId else "fameko_order_$orderId"
        activeChannelName = channelName
        _callState.value = DriverCallState.Active(channelName, callerName, orderId)

        rtcEngine?.joinChannel(null, channelName, null, 0)

        scope.launch {
            repository.acceptCall(callId)
        }
    }

    fun rejectCall(callId: String) {
        scope.launch {
            repository.rejectCall(callId, "Rejected by Driver")
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
            _callState.value = DriverCallState.Ended(reason)
            scope.launch {
                delay(1500)
                _callState.value = DriverCallState.Idle
            }
        } catch (e: Exception) {
            Log.e("DriverAgoraCall", "Error leaving channel", e)
            _callState.value = DriverCallState.Idle
        }
    }

    fun cleanup() {
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
        } catch (e: Exception) {
            Log.e("DriverAgoraCall", "Error destroying Driver Agora Engine", e)
        }
    }
}
