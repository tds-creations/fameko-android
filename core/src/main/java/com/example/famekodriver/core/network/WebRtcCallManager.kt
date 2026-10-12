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
    private var isInitiator = false
    private var isMuted = false

    private val pendingIceCandidates = mutableListOf<IceCandidate>()

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
                .setAudioRecordErrorCallback(object : JavaAudioDeviceModule.AudioRecordErrorCallback {
                    override fun onWebRtcAudioRecordInitError(errorMessage: String?) { Log.e("WebRtcCallManager", "AudioRecord Init Error: $errorMessage") }
                    override fun onWebRtcAudioRecordStartError(errorType: JavaAudioDeviceModule.AudioRecordStartErrorCode?, errorMessage: String?) { Log.e("WebRtcCallManager", "AudioRecord Start Error: $errorMessage") }
                    override fun onWebRtcAudioRecordError(errorMessage: String?) { Log.e("WebRtcCallManager", "AudioRecord Error: $errorMessage") }
                })
                .setAudioTrackErrorCallback(object : JavaAudioDeviceModule.AudioTrackErrorCallback {
                    override fun onWebRtcAudioTrackInitError(errorMessage: String?) { Log.e("WebRtcCallManager", "AudioTrack Init Error: $errorMessage") }
                    override fun onWebRtcAudioTrackStartError(errorType: JavaAudioDeviceModule.AudioTrackStartErrorCode?, errorMessage: String?) { Log.e("WebRtcCallManager", "AudioTrack Start Error: $errorMessage") }
                    override fun onWebRtcAudioTrackError(errorMessage: String?) { Log.e("WebRtcCallManager", "AudioTrack Error: $errorMessage") }
                })
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
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL)
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, maxVol, 0)
            Log.d("WebRtcCallManager", "AudioManager configured for Voice Communication (Max Vol $maxVol).")
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
                        isInitiator = false
                        _callState.value = CallState.Incoming(event.callId, event.callerName, 0, "")
                    }
                    is FamekoEvent.CallAccepted -> {
                        activeCallId = event.callId
                        if (isInitiator) {
                            configureAudioManagerForCall()
                            createAndSendOffer(event.callId)
                        }
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
                                isInitiator = false
                                _callState.value = CallState.Incoming(callId, callerName, orderId, initiatorId)
                            }
                            "CALL_ACCEPTED" -> {
                                val callId = data["call_id"]?.toString() ?: ""
                                activeCallId = callId
                                if (isInitiator) {
                                    configureAudioManagerForCall()
                                    createAndSendOffer(callId)
                                }
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
                                val sdpMid = data["sdpMid"]?.toString() ?: "0"
                                val sdpMLineIndex = (data["sdpMLineIndex"] as? Number)?.toInt() ?: 0
                                handleRemoteIceCandidate(candStr, sdpMid, sdpMLineIndex)
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
        isInitiator = true
        configureAudioManagerForCall()
        _callState.value = CallState.Outgoing(callId, "Calling...")
        
        scope.launch {
            repository.initiateCall(targetId, callerName, orderId)
        }
    }

    fun acceptCall(callId: String) {
        activeCallId = callId
        isInitiator = false
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
        if (peerConnection != null) return

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
                Log.d("WebRtcCallManager", "Generated Local ICE Candidate: mid=${candidate.sdpMid}, index=${candidate.sdpMLineIndex}")
                scope.launch {
                    repository.sendIceCandidate(callId, candidate.sdp, candidate.sdpMid ?: "0", candidate.sdpMLineIndex)
                }
            }

            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>?) {}
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d("WebRtcCallManager", "Signaling State Changed -> $state")
            }
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d("WebRtcCallManager", "ICE Connection State Changed -> $state")
                when (state) {
                    PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> {
                        _callState.value = CallState.Connected(activeCallId ?: "", "HD Voice Live")
                        Log.d("WebRtcCallManager", "WebRTC Audio P2P / TURN Stream Connected Successfully!")
                        startStatsMonitoring()
                    }
                    PeerConnection.IceConnectionState.DISCONNECTED, PeerConnection.IceConnectionState.FAILED -> {
                        Log.e("WebRtcCallManager", "WebRTC ICE Connection Failed or Disconnected ($state)")
                        endCallLocally("Connection Lost")
                    }
                    else -> {}
                }
            }
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d("WebRtcCallManager", "ICE Connection Receiving Change -> $receiving")
            }
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d("WebRtcCallManager", "ICE Gathering State Changed -> $state")
            }
            override fun onAddStream(stream: MediaStream?) {
                stream?.audioTracks?.forEach {
                    it.setEnabled(true)
                    it.setVolume(1.0)
                    Log.d("WebRtcCallManager", "Remote Stream Audio Track enabled: ${it.id()}")
                }
            }
            override fun onRemoveStream(stream: MediaStream?) {}
            override fun onDataChannel(channel: DataChannel?) {}
            override fun onRenegotiationNeeded() {}
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                val track = receiver?.track() as? AudioTrack
                if (track != null) {
                    track.setEnabled(true)
                    track.setVolume(1.0)
                    Log.d("WebRtcCallManager", "Remote Audio Track enabled & volume set to 1.0: ${track.id()}")
                }
            }
        })

        // Force bidirectional Audio Transceiver
        peerConnection?.addTransceiver(
            MediaStreamTrack.MediaType.MEDIA_TYPE_AUDIO,
            RtpTransceiver.RtpTransceiverInit(RtpTransceiver.RtpTransceiverDirection.SEND_RECV)
        )

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
                drainPendingIceCandidates()
                val constraints = MediaConstraints().apply {
                    mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                }
                peerConnection?.createAnswer(object : SimpleSdpObserver() {
                    override fun onCreateSuccess(sdp: SessionDescription) {
                        peerConnection?.setLocalDescription(SimpleSdpObserver(), sdp)
                        scope.launch {
                            repository.sendWebrtcAnswer(callId, sdp.description)
                        }
                    }
                }, constraints)
            }
        }, offerSdp)
    }

    private fun handleRemoteAnswer(sdpAnswer: String) {
        val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, sdpAnswer)
        peerConnection?.setRemoteDescription(object : SimpleSdpObserver() {
            override fun onSetSuccess() {
                drainPendingIceCandidates()
            }
        }, answerSdp)
    }

    private fun handleRemoteIceCandidate(candStr: String, sdpMid: String, sdpMLineIndex: Int) {
        if (candStr.isNotBlank()) {
            val candidate = IceCandidate(sdpMid, sdpMLineIndex, candStr)
            if (peerConnection?.remoteDescription != null) {
                val added = peerConnection?.addIceCandidate(candidate) ?: false
                Log.d("WebRtcCallManager", "Added Remote ICE Candidate (mid=$sdpMid, index=$sdpMLineIndex): success=$added")
            } else {
                pendingIceCandidates.add(candidate)
                Log.d("WebRtcCallManager", "Buffered Remote ICE Candidate (total buffered: ${pendingIceCandidates.size})")
            }
        }
    }

    private fun drainPendingIceCandidates() {
        pendingIceCandidates.forEach { candidate ->
            val added = peerConnection?.addIceCandidate(candidate) ?: false
            Log.d("WebRtcCallManager", "Drained Buffered ICE Candidate: success=$added")
        }
        pendingIceCandidates.clear()
    }

    private fun startStatsMonitoring() {
        scope.launch {
            while (_callState.value is CallState.Connected && peerConnection != null) {
                peerConnection?.getStats { report ->
                    for (stat in report.statsMap.values) {
                        if (stat.type == "outbound-rtp" && stat.members["mediaType"] == "audio") {
                            Log.d("WebRtcCallManager", "Audio Outbound RTP Stats: bytesSent=${stat.members["bytesSent"]}, packetsSent=${stat.members["packetsSent"]}")
                        }
                        if (stat.type == "inbound-rtp" && stat.members["mediaType"] == "audio") {
                            Log.d("WebRtcCallManager", "Audio Inbound RTP Stats: bytesReceived=${stat.members["bytesReceived"]}, packetsReceived=${stat.members["packetsReceived"]}")
                        }
                    }
                }
                delay(3000)
            }
        }
    }

    var isSpeakerphoneOn = true
        private set

    fun toggleSpeakerphone(): Boolean {
        isSpeakerphoneOn = !isSpeakerphoneOn
        audioManager.isSpeakerphoneOn = isSpeakerphoneOn
        Log.d("WebRtcCallManager", "Speakerphone toggled -> $isSpeakerphoneOn")
        return isSpeakerphoneOn
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
            localAudioTrack?.dispose()
            localAudioTrack = null

            localAudioSource?.dispose()
            localAudioSource = null

            peerConnection?.close()
            peerConnection?.dispose()
            peerConnection = null

            pendingIceCandidates.clear()
            activeCallId = null
            isInitiator = false

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
