package com.example.famekodriver.customer

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

sealed interface CustomerCallState {
    object Idle : CustomerCallState
    data class Ringing(val channelName: String, val callerName: String, val orderId: Int, val initiatorId: String) : CustomerCallState
    data class Dialing(val channelName: String, val recipientName: String, val orderId: Int) : CustomerCallState
    data class Active(val channelName: String, val remoteName: String, val orderId: Int) : CustomerCallState
    data class Ended(val reason: String) : CustomerCallState
}

class CustomerAgoraCallManager(
    private val context: Context,
    private val repository: DriverRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val appId = "38144168c418426faf0ee6a02c9b46e4"

    private var rtcEngine: RtcEngine? = null

    private val _callState = MutableStateFlow<CustomerCallState>(CustomerCallState.Idle)
    val callState: StateFlow<CustomerCallState> = _callState

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn

    private var activeChannelName: String? = null
    private var activeOrderId: Int = 0
    private var activeRecipientName: String = "Driver"

    init {
        initializeAgoraEngine()
        listenToSignalingEvents()
    }

    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(context, appId, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
                    Log.d("CustomerAgoraCall", "Customer successfully joined channel $channel as uid=$uid")
                    rtcEngine?.muteLocalAudioStream(false)
                    rtcEngine?.muteAllRemoteAudioStreams(false)
                }

                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Log.d("CustomerAgoraCall", "Driver joined Agora channel: $uid")
                    rtcEngine?.muteRemoteAudioStream(uid, false)
                    val channel = activeChannelName ?: ""
                    _callState.value = CustomerCallState.Active(channel, activeRecipientName, activeOrderId)
                }

                override fun onUserOffline(uid: Int, reason: Int) {
                    Log.d("CustomerAgoraCall", "Driver left Agora channel: $uid")
                    endCallLocally("Driver Ended Call")
                }

                override fun onAudioPublishStateChanged(channel: String?, oldState: Int, newState: Int, elapseSinceLastState: Int) {
                    Log.d("CustomerAgoraCall", "Customer Audio Publish State: $oldState -> $newState")
                }

                override fun onAudioSubscribeStateChanged(channel: String?, uid: Int, oldState: Int, newState: Int, elapseSinceLastState: Int) {
                    Log.d("CustomerAgoraCall", "Customer Audio Subscribe State for $uid: $oldState -> $newState")
                }

                override fun onRemoteAudioStateChanged(uid: Int, state: Int, reason: Int, elapsed: Int) {
                    Log.d("CustomerAgoraCall", "Customer Remote Audio State for $uid: state=$state, reason=$reason")
                }

                override fun onAudioVolumeIndication(speakers: Array<out IRtcEngineEventHandler.AudioVolumeInfo>?, totalVolume: Int) {
                    if (totalVolume > 0) {
                        Log.d("CustomerAgoraCall", "Customer Audio Volume Detected: $totalVolume")
                    }
                }

                override fun onConnectionLost() {
                    Log.e("CustomerAgoraCall", "Customer Connection Lost")
                    endCallLocally("Connection Lost")
                }

                override fun onError(err: Int) {
                    Log.e("CustomerAgoraCall", "Customer Agora Error: $err")
                }
            })

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            audioManager.mode = android.media.AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            val maxVol = audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(android.media.AudioManager.STREAM_VOICE_CALL, maxVol, 0)

            rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
            rtcEngine?.setAudioProfile(
                Constants.AUDIO_PROFILE_SPEECH_STANDARD,
                Constants.AUDIO_SCENARIO_DEFAULT
            )
            rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            rtcEngine?.enableAudio()
            rtcEngine?.enableLocalAudio(true)
            rtcEngine?.muteLocalAudioStream(false)
            rtcEngine?.muteAllRemoteAudioStreams(false)
            rtcEngine?.adjustRecordingSignalVolume(400)
            rtcEngine?.adjustPlaybackSignalVolume(400)
            rtcEngine?.setDefaultAudioRoutetoSpeakerphone(true)
            rtcEngine?.setEnableSpeakerphone(true)
            rtcEngine?.enableAudioVolumeIndication(200, 3, true)

            Log.d("CustomerAgoraCall", "Customer Agora RTC Engine Initialized.")
        } catch (e: Exception) {
            Log.e("CustomerAgoraCall", "Failed to initialize Customer Agora Engine", e)
        }
    }

    private fun joinChannelWithBackendToken(channelName: String) {
        scope.launch {
            val token = repository.fetchAgoraToken(channelName)
            Log.d("CustomerAgoraCall", "Fetched secure Agora token for channel $channelName from backend API.")
            rtcEngine?.joinChannel(token.ifBlank { null }, channelName, null, 0)
        }
    }

    private fun listenToSignalingEvents() {
        scope.launch {
            repository.events.collect { event ->
                when (event) {
                    is FamekoEvent.IncomingCall -> {
                        activeChannelName = event.callId
                        val parsedOrderId = if (event.orderId > 0) event.orderId else event.callId.removePrefix("fameko_order_").toIntOrNull() ?: activeOrderId
                        if (parsedOrderId > 0) activeOrderId = parsedOrderId
                        activeRecipientName = event.callerName.ifBlank { "Driver" }
                        _callState.value = CustomerCallState.Ringing(event.callId, activeRecipientName, activeOrderId, "")
                    }
                    is FamekoEvent.CallAccepted -> {
                        val channelName = event.callId
                        activeChannelName = channelName
                        val parsedOrderId = channelName.removePrefix("fameko_order_").toIntOrNull() ?: activeOrderId
                        if (parsedOrderId > 0) activeOrderId = parsedOrderId
                        joinChannelWithBackendToken(channelName)
                        _callState.value = CustomerCallState.Active(channelName, activeRecipientName, activeOrderId)
                    }
                    is FamekoEvent.CallRejected -> {
                        endCallLocally(event.reason ?: "Driver Rejected Call")
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
                                val callerName = data["caller_name"]?.toString() ?: "Driver"
                                val rawOrderId = (data["order_id"] as? Number)?.toInt() ?: callId.removePrefix("fameko_order_").toIntOrNull() ?: activeOrderId
                                if (rawOrderId > 0) activeOrderId = rawOrderId
                                activeRecipientName = callerName.ifBlank { "Driver" }
                                val initiatorId = data["initiator_id"]?.toString() ?: ""
                                activeChannelName = callId
                                _callState.value = CustomerCallState.Ringing(callId, activeRecipientName, activeOrderId, initiatorId)
                            }
                            "CALL_ACCEPTED" -> {
                                val channelName = data["call_id"]?.toString() ?: ""
                                activeChannelName = channelName
                                val parsedOrderId = channelName.removePrefix("fameko_order_").toIntOrNull() ?: activeOrderId
                                if (parsedOrderId > 0) activeOrderId = parsedOrderId
                                joinChannelWithBackendToken(channelName)
                                _callState.value = CustomerCallState.Active(channelName, activeRecipientName, activeOrderId)
                            }
                            "CALL_REJECTED" -> {
                                endCallLocally("Driver Rejected Call")
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

    fun startOutgoingCall(targetId: String, callerName: String, recipientName: String, orderId: Int) {
        val channelName = "fameko_order_$orderId"
        activeChannelName = channelName
        activeOrderId = orderId
        activeRecipientName = recipientName.ifBlank { "Driver" }
        _callState.value = CustomerCallState.Dialing(channelName, activeRecipientName, orderId)

        joinChannelWithBackendToken(channelName)

        scope.launch {
            repository.initiateCall(targetId, callerName, orderId)
        }
    }

    fun acceptIncomingCall(callId: String, callerName: String, orderId: Int) {
        val channelName = if (callId.startsWith("fameko_order_")) callId else "fameko_order_$orderId"
        activeChannelName = channelName
        val parsedOrderId = if (orderId > 0) orderId else channelName.removePrefix("fameko_order_").toIntOrNull() ?: activeOrderId
        if (parsedOrderId > 0) activeOrderId = parsedOrderId
        if (callerName.isNotBlank()) activeRecipientName = callerName
        _callState.value = CustomerCallState.Active(channelName, activeRecipientName, activeOrderId)

        joinChannelWithBackendToken(channelName)

        scope.launch {
            repository.acceptCall(callId)
        }
    }

    fun rejectCall(callId: String) {
        scope.launch {
            repository.rejectCall(callId, "Rejected by Customer")
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
            _callState.value = CustomerCallState.Ended(reason)
            scope.launch {
                delay(1500)
                _callState.value = CustomerCallState.Idle
            }
        } catch (e: Exception) {
            Log.e("CustomerAgoraCall", "Error leaving channel", e)
            _callState.value = CustomerCallState.Idle
        }
    }

    fun cleanup() {
        try {
            rtcEngine?.leaveChannel()
            RtcEngine.destroy()
            rtcEngine = null
        } catch (e: Exception) {
            Log.e("CustomerAgoraCall", "Error destroying Customer Agora Engine", e)
        }
    }
}
