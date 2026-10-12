package com.example.famekodriver.core.network

import android.content.Context
import android.media.AudioManager
import android.util.Log
import com.example.famekodriver.core.data.repository.DriverRepository
import com.example.famekodriver.core.domain.model.FamekoEvent
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

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

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

    private fun configureAudioManagerForCall() {
        try {
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
            audioManager.isSpeakerphoneOn = true
            Log.d("WebRtcCallManager", "AudioManager configured for Voice Communication on Speakerphone.")
        } catch (e: Exception) {
            Log.e("WebRtcCallManager", "Failed to configure AudioManager", e)
        }
    }

    private fun restoreAudioManager() {
        try {
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.isSpeakerphoneOn = false
            Log.d("WebRtcCallManager", "AudioManager restored to Normal mode.")
        } catch (e: Exception) {
            Log.e("WebRtcCallManager", "Failed to restore AudioManager", e)
        }
    }

    private fun listenToSignalingEvents() {
        scope.launch {
            repository.events.collect { event ->
                when (event) {
                    is FamekoEvent.IncomingCall -> {
                        activeCallId = event.callId
                        _callState.value = CallState.Incoming(event.callId, event.callerName, 0, "")
                    }
                    is FamekoEvent.CallAccepted -> {
                        activeCallId = event.callId
                        configureAudioManagerForCall()
                        createAndSendOffer(event.callId)
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
                                activeCallId = callId
                                _callState.value = CallState.Incoming(callId, callerName, orderId, initiatorId)
                            }
                            "CALL_ACCEPTED" -> {
                                val callId = data["call_id"]?.toString() ?: ""
                                activeCallId = callId
                                configureAudioManagerForCall()
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
        configureAudioManagerForCall()
        _callState.value = CallState.Outgoing(callId, "Calling...")
        
        scope.launch {
            repository.initiateCall(targetId, callerName, orderId)
        }
    }

    fun acceptCall(callId: String) {
        activeCallId = callId
        configureAudioManagerForCall()
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
            // Free Google STUN Servers
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),

            // ExpressTURN TURN Servers (Symmetric CGNAT & Carrier Relay Traversal)
            PeerConnection.IceServer.builder("turn:free.expressturn.com:3478")
                .setUsername("000000002107168884")
                .setPassword("0cYnict4u/vHVZaiV6vLm/pvFCU=")
                .createIceServer(),

            PeerConnection.IceServer.builder("turn:free.expressturn.com:3478?transport=udp")
                .setUsername("000000002107168884")
                .setPassword("0cYnict4u/vHVZaiV6vLm/pvFCU=")
                .createIceServer(),

            PeerConnection.IceServer.builder("turn:free.expressturn.com:3478?transport=tcp")
                .setUsername("000000002107168884")
                .setPassword("0cYnict4u/vHVZaiV6vLm/pvFCU=")
                .createIceServer(),

            PeerConnection.IceServer.builder("turns:free.expressturn.com:5349")
                .setUsername("000000002107168884")
                .setPassword("0cYnict4u/vHVZaiV6vLm/pvFCU=")
                .createIceServer()
        )

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
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
            override fun onAddStream(stream: MediaStream?) {
                stream?.audioTracks?.forEach {
                    it.setEnabled(true)
                    Log.d("WebRtcCallManager", "Remote Stream Audio Track enabled: ${it.id()}")
                }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track() as? AudioTrack
                track?.setEnabled(true)
                Log.d("WebRtcCallManager", "Remote Audio Track added & enabled: ${track?.id()}")
            }
        })

        // Add local Audio Track
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
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
            restoreAudioManager()
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
            restoreAudioManager()
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
