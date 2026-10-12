package com.example.famekodriver.core.network

import android.content.Context
import android.util.Log
import com.example.famekodriver.core.data.repository.DriverRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule

sealed class CallState {
    object Idle : CallState()
    data class Outgoing(val callId: String, val targetName: String) : CallState()
    data class Incoming(val callId: String, val callerName: String, val orderId: Int, val initiatorId: String) : CallState()
    data class Connected(val callId: String, val targetName: String) : CallState()
    data class Ended(val reason: String) : CallState()
}

class WebRtcCallManager(
    private val context: Context,
    private val repository: DriverRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState

    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localAudioSource: AudioSource? = null
    private var localAudioTrack: AudioTrack? = null

    private var activeCallId: String? = null
    private var isMuted = false

    init {
        initWebRtcFactory()
        listenToSignalingEvents()
    }

    private fun initWebRtcFactory() {
        try {
            val options = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(options)

            val audioDeviceModule = JavaAudioDeviceModule.builder(context)
                .setUseHardwareAcousticEchoCanceler(true)
                .setUseHardwareNoiseSuppressor(true)
                .createAudioDeviceModule()

            peerConnectionFactory = PeerConnectionFactory.builder()
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory()

            Log.d("WebRtcCallManager", "WebRTC PeerConnectionFactory initialized successfully.")
        } catch (e: Exception) {
            Log.e("WebRtcCallManager", "Failed to initialize WebRTC Factory", e)
        }
    }

    private fun listenToSignalingEvents() {
        scope.launch {
            repository.events.collect { event ->
                when (event) {
                    is com.example.famekodriver.core.domain.model.FamekoEvent.Unknown -> {
                        val type = event.type
                        val data = event.data
                        when (type) {
                            "CALL_INCOMING" -> {
                                val callId = data["call_id"]?.toString() ?: ""
                                val callerName = data["caller_name"]?.toString() ?: "Incoming Call"
                                val orderId = (data["order_id"] as? Number)?.toInt() ?: 0
                                val initiatorId = data["initiator_id"]?.toString() ?: ""
                                activeCallId = callId
                                _callState.value = CallState.Incoming(callId, callerName, orderId, initiatorId)
                            }
                            "CALL_ACCEPTED" -> {
                                val callId = data["call_id"]?.toString() ?: ""
                                createAndSendOffer(callId)
                            }
                            "CALL_REJECTED" -> {
                                endCallLocally("Call Rejected")
                            }
                            "CALL_ENDED" -> {
                                endCallLocally("Call Ended")
                            }
                            "WEBRTC_OFFER" -> {
                                val callId = data["call_id"]?.toString() ?: ""
                                val sdpStr = data["offer"]?.toString() ?: data["sdp"]?.toString() ?: ""
                                handleRemoteOffer(callId, sdpStr)
                            }
                            "WEBRTC_ANSWER" -> {
                                val sdpStr = data["answer"]?.toString() ?: data["sdp"]?.toString() ?: ""
                                handleRemoteAnswer(sdpStr)
                            }
                            "WEBRTC_ICE_CANDIDATE" -> {
                                val candStr = data["candidate"]?.toString() ?: data["sdp"]?.toString() ?: ""
                                handleRemoteIceCandidate(candStr)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun initiateCall(targetId: String, callerName: String, orderId: Int) {
        val callId = java.util.UUID.randomUUID().toString()
        activeCallId = callId
        _callState.value = CallState.Outgoing(callId, "Calling...")
        
        scope.launch {
            repository.initiateCall(targetId, callerName, orderId)
        }
    }

    fun acceptCall(callId: String) {
        activeCallId = callId
        _callState.value = CallState.Connected(callId, "Connecting Audio...")
        scope.launch {
            repository.acceptCall(callId)
        }
    }

    fun rejectCall(callId: String, reason: String = "Busy") {
        scope.launch {
            repository.rejectCall(callId, reason)
            endCallLocally("Call Rejected")
        }
    }

    fun endCall() {
        val callId = activeCallId
        if (callId != null) {
            scope.launch {
                repository.endCall(callId)
                endCallLocally("Call Ended")
            }
        } else {
            endCallLocally("Call Ended")
        }
    }

    private fun createPeerConnection(callId: String) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: IceCandidate) {
                scope.launch {
                    repository.sendIceCandidate(callId, candidate.sdp)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {}
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d("WebRtcCallManager", "ICE Connection State: $state")
                if (state == PeerConnection.IceConnectionState.DISCONNECTED || state == PeerConnection.IceConnectionState.FAILED) {
                    endCallLocally("Connection Lost")
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {}
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {}
            override fun onAddStream(stream: MediaStream?) {}
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {}
        })

        // Add local Audio Track
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }

        localAudioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", localAudioSource)
        localAudioTrack?.setEnabled(true)

        peerConnection?.addTrack(localAudioTrack, listOf("ARDAMS"))
    }

    private fun createAndSendOffer(callId: String) {
        createPeerConnection(callId)
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        }

        peerConnection?.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                scope.launch {
                    repository.sendWebrtcOffer(callId, sdp.description)
                }
            }
        }, constraints)
    }

    private fun handleRemoteOffer(callId: String, sdpOffer: String) {
        createPeerConnection(callId)
        val offerSdp = SessionDescription(SessionDescription.Type.OFFER, sdpOffer)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                }
                peerConnection?.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                        scope.launch {
                            repository.sendWebrtcAnswer(callId, sdp.description)
                        }
                        _callState.value = CallState.Connected(callId, "HD Voice Connected")
                    }
                }, constraints)
            }
        }, offerSdp)
    }

    private fun handleRemoteAnswer(sdpAnswer: String) {
        val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection?.setRemoteDescription(SimpleSdpObserver(), answerSdp)
        val cid = activeCallId ?: ""
        _callState.value = CallState.Connected(cid, "HD Voice Connected")
    }

    private fun handleRemoteIceCandidate(candStr: String) {
        val candidate = IceCandidate("audio", 0, candStr)
        peerConnection?.addIceCandidate(candidate)
    }

    fun toggleMute(): Boolean {
        isMuted = !isMuted
        localAudioTrack?.setEnabled(!isMuted)
        return isMuted
    }

    fun endCallLocally(reason: String) {
        try {
            localAudioTrack?.setEnabled(false)
            localAudioSource?.dispose()
            peerConnection?.close()
            peerConnection = null
            activeCallId = null
            _callState.value = CallState.Ended(reason)
            scope.launch {
                delay(1500)
                _callState.value = CallState.Idle
            }
        } catch (e: Exception) {
            Log.e("WebRtcCallManager", "Error closing WebRTC connection", e)
            _callState.value = CallState.Idle
        }
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(reason: String?) { Log.e("WebRtcCallManager", "SDP Create Failure: $reason") }
        override fun onSetFailure(reason: String?) { Log.e("WebRtcCallManager", "SDP Set Failure: $reason") }
    }
}
