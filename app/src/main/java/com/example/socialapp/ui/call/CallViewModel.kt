package com.example.socialapp.ui.call

import android.content.Context
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.CallSignal
import com.example.socialapp.data.remote.AgoraManager
import com.example.socialapp.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.rtc2.IRtcEngineEventHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CallState {
    object Idle      : CallState()
    object Calling   : CallState()   // Caller đang đổ chuông (chờ callee bắt máy)
    object Ringing   : CallState()   // Callee đang thấy màn hình incoming call
    object Connected : CallState()   // Đang trong cuộc gọi
    object Declined  : CallState()
    object Ended     : CallState()
    data class Error(val msg: String) : CallState()
}

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val agoraManager: AgoraManager
) : ViewModel() {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private val _currentCallSignal = MutableStateFlow<CallSignal?>(null)
    val currentCallSignal: StateFlow<CallSignal?> = _currentCallSignal.asStateFlow()

    private val _isMicMuted = MutableStateFlow(false)
    val isMicMuted: StateFlow<Boolean> = _isMicMuted.asStateFlow()

    private val _isCamMuted = MutableStateFlow(false)
    val isCamMuted: StateFlow<Boolean> = _isCamMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(true)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()

    private val _remoteUid = MutableStateFlow<Int?>(null)
    val remoteUid: StateFlow<Int?> = _remoteUid.asStateFlow()

    // Phát ra channelName khi sẵn sàng join video (dành cho Caller)
    private val _readyToJoinVideo = MutableStateFlow<String?>(null)
    val readyToJoinVideo: StateFlow<String?> = _readyToJoinVideo.asStateFlow()

    private var observeJob: Job? = null
    private var incomingCallJob: Job? = null
    private var currentCallId: String = ""
    private var isCallee = false

    init {
        // Tự động lắng nghe cuộc gọi đến khi app foreground
        startObservingIncomingCalls()
    }

    /** Bắt đầu lắng nghe cuộc gọi đến (foreground) */
    fun startObservingIncomingCalls() {
        incomingCallJob?.cancel()
        incomingCallJob = viewModelScope.launch {
            callRepository.observeIncomingCall().collect { signal ->
                if (signal != null && _callState.value == CallState.Idle) {
                    _currentCallSignal.value = signal
                    currentCallId = signal.id
                    isCallee = true
                    _callState.value = CallState.Ringing
                }
            }
        }
    }

    // ─── Caller: khởi tạo cuộc gọi ──────────────────────────────────────────
    fun startCall(
        context: Context,
        calleeId: String,
        callerName: String,
        callerAvatar: String,
        type: String   // "voice" | "video"
    ) {
        viewModelScope.launch {
            _callState.value = CallState.Calling
            try {
                val callId = callRepository.startCall(calleeId, callerName, callerAvatar, type)
                currentCallId = callId
                isCallee = false
                initAgoraEngine(context, callId, type, isCallee = false)
                if (type == "video") {
                    // Thông báo cho UI biết channelName để join video với SurfaceView
                    _readyToJoinVideo.value = callId
                }
                observeCallStatus(callId)
            } catch (e: Exception) {
                _callState.value = CallState.Error(e.message ?: "Không thể tạo cuộc gọi")
            }
        }
    }

    // ─── Callee: nhận cuộc gọi đến (từ FCM hoặc Firestore) ──────────────────
    fun onIncomingCall(callSignal: CallSignal) {
        _currentCallSignal.value = callSignal
        currentCallId = callSignal.id
        isCallee = true
        _callState.value = CallState.Ringing
        incomingCallJob?.cancel() // stop observing, we already have the signal
    }

    /**
     * Được gọi khi đến từ IncomingCallActivity.
     * Setup signal từ các tham số truyền vào qua Intent.
     */
    fun prepareCallAsCallee(
        callId: String,
        callerId: String,
        callerName: String,
        callerAvatar: String,
        type: String
    ) {
        if (currentCallId != callId) {
            currentCallId = callId
            isCallee = true
            _currentCallSignal.value = CallSignal(
                id = callId,
                callerId = callerId,
                calleeId = callRepository.getCurrentUid(),
                callerName = callerName,
                callerAvatar = callerAvatar,
                channelName = callId,
                type = type,
                status = "ringing"
            )
            _callState.value = CallState.Ringing
        }
    }

    // ─── Callee: bắt máy ─────────────────────────────────────────────────────
    fun acceptCall(context: Context) {
        val signal = _currentCallSignal.value ?: return
        viewModelScope.launch {
            try {
                callRepository.acceptCall(currentCallId)
                initAgoraEngine(context, signal.channelName, signal.type, isCallee = true)
                // Với video, báo UI biết channelName để join với SurfaceView
                if (signal.type == "video") {
                    _readyToJoinVideo.value = signal.channelName
                }
                _callState.value = CallState.Connected
                observeCallStatus(currentCallId)
            } catch (e: Exception) {
                _callState.value = CallState.Error(e.message ?: "Lỗi kết nối")
            }
        }
    }

    // ─── Callee: từ chối ─────────────────────────────────────────────────────
    fun declineCall() {
        viewModelScope.launch {
            try { callRepository.declineCall(currentCallId) } catch (_: Exception) {}
            _callState.value = CallState.Ended
            resetAfterCall()
        }
    }

    // ─── Kết thúc cuộc gọi (cả 2 bên) ───────────────────────────────────────
    fun endCall() {
        viewModelScope.launch {
            observeJob?.cancel()
            try { callRepository.endCall(currentCallId) } catch (_: Exception) {}
            _callState.value = CallState.Ended
            resetAfterCall()
        }
    }

    private fun resetAfterCall() {
        currentCallId = ""
        isCallee = false
        _currentCallSignal.value = null
        _isMicMuted.value = false
        _isCamMuted.value = false
        _remoteUid.value = null
        _readyToJoinVideo.value = null
        viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            _callState.value = CallState.Idle
            startObservingIncomingCalls()
        }
    }

    // ─── Controls ─────────────────────────────────────────────────────────────
    fun toggleMic() {
        val muted = !_isMicMuted.value
        _isMicMuted.value = muted
        agoraManager.muteAudio(muted)
    }

    fun toggleCamera() {
        val muted = !_isCamMuted.value
        _isCamMuted.value = muted
        agoraManager.muteVideo(muted)
    }

    fun switchCamera() = agoraManager.switchCamera()

    fun toggleSpeaker() {
        val on = !_isSpeakerOn.value
        _isSpeakerOn.value = on
        agoraManager.setSpeaker(on)
    }

    fun setupRemoteVideo(remoteView: SurfaceView, uid: Int) {
        agoraManager.setupRemoteVideo(remoteView, uid)
    }

    // ─── Internal ─────────────────────────────────────────────────────────────
    private fun initAgoraEngine(
        context: Context,
        channelName: String,
        type: String,
        isCallee: Boolean
    ) {
        val handler = object : IRtcEngineEventHandler() {
            override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                if (!isCallee) _callState.value = CallState.Calling // Caller chờ callee
            }
            override fun onUserJoined(uid: Int, elapsed: Int) {
                _remoteUid.value = uid
                _callState.value = CallState.Connected
                agoraManager.setSpeaker(true)
            }
            override fun onUserOffline(uid: Int, reason: Int) {
                _remoteUid.value = null
                _callState.value = CallState.Ended
            }
            override fun onError(err: Int) {
                _callState.value = CallState.Error("Agora error: $err")
            }
        }

        agoraManager.initEngine(handler)

        if (type == "video") {
            // Video: joinVideoChannel yêu cầu local SurfaceView — gọi từ UI
            // (điều phối thông qua _callState == Connected)
        } else {
            agoraManager.joinVoiceChannel(channelName)
            agoraManager.setSpeaker(true)
        }
    }

    fun joinVideoWithView(localView: SurfaceView, channelName: String) {
        agoraManager.joinVideoChannel(channelName, localView)
    }

    private fun observeCallStatus(callId: String) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            callRepository.observeCall(callId).collect { signal ->
                signal ?: return@collect
                _currentCallSignal.value = signal
                when (signal.status) {
                    "accepted" -> {
                        if (!isCallee) _callState.value = CallState.Connected
                    }
                    "declined" -> {
                        agoraManager.leaveChannel()
                        _callState.value = CallState.Declined
                    }
                    "ended" -> {
                        agoraManager.leaveChannel()
                        _callState.value = CallState.Ended
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        observeJob?.cancel()
        incomingCallJob?.cancel()
        agoraManager.destroy()
    }
}

