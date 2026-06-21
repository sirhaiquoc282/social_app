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
                    // Bắt đầu observe trạng thái cuộc gọi này
                    // để phát hiện khi caller hủy cuộc gọi
                    observeCallStatus(signal.id)
                } else if (signal == null && _callState.value == CallState.Ringing && isCallee) {
                    // Caller đã hủy cuộc gọi (status không còn "ringing")
                    // → reset trạng thái về Idle
                    android.util.Log.d(TAG, "Incoming call cancelled by caller")
                    _callState.value = CallState.Ended
                    resetAfterCall()
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
                android.util.Log.d(TAG, "startCall() type=$type, calleeId=$calleeId")
                val (callId, channelName) = callRepository.startCall(calleeId, callerName, callerAvatar, type)
                android.util.Log.d(TAG, "startCall() got callId=$callId, channelName=$channelName")
                currentCallId = callId
                isCallee = false
                if (initAgoraEngine(context, channelName, type, isCallee = false)) {
                    if (type == "video") {
                        android.util.Log.d(TAG, "Setting readyToJoinVideo=$channelName")
                        _readyToJoinVideo.value = channelName
                    }
                }
                observeCallStatus(callId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "startCall() error: ${e.message}", e)
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
            val channelName = if (com.example.socialapp.BuildConfig.AGORA_TOKEN.isNotEmpty()) "test_channel" else callId
            _currentCallSignal.value = CallSignal(
                id = callId,
                callerId = callerId,
                calleeId = callRepository.getCurrentUid(),
                callerName = callerName,
                callerAvatar = callerAvatar,
                channelName = channelName,
                type = type,
                status = "ringing"
            )
            _callState.value = CallState.Ringing
        }
    }

    // ─── Callee: bắt máy ─────────────────────────────────────────────────────
    fun acceptCall(context: Context) {
        val signal = _currentCallSignal.value ?: return
        android.util.Log.d(TAG, "acceptCall() channelName=${signal.channelName}, type=${signal.type}")
        
        // Cập nhật state ngay lập tức để khi update Firestore ("accepted"),
        // observeIncomingCall nhận signal = null sẽ không bị nhầm là caller cancel (do state không còn là Ringing).
        _callState.value = CallState.Connected
        
        viewModelScope.launch {
            try {
                callRepository.acceptCall(currentCallId)
                if (initAgoraEngine(context, signal.channelName, signal.type, isCallee = true)) {
                    if (signal.type == "video") {
                        android.util.Log.d(TAG, "Setting readyToJoinVideo=${signal.channelName}")
                        _readyToJoinVideo.value = signal.channelName
                    }
                }
                observeCallStatus(currentCallId)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "acceptCall() error: ${e.message}", e)
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
        observeJob?.cancel()
        incomingCallJob?.cancel()
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

    fun setupLocalVideo(localView: SurfaceView) {
        agoraManager.setupLocalVideo(localView)
    }

    // ─── Internal ─────────────────────────────────────────────────────────────
    private fun initAgoraEngine(
        context: Context,
        channelName: String,
        type: String,
        isCallee: Boolean
    ): Boolean {
        android.util.Log.d(TAG, "initAgoraEngine() channelName=$channelName, type=$type, isCallee=$isCallee")
        val handler = object : IRtcEngineEventHandler() {
            // ... (giữ nguyên handler)
            override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                android.util.Log.d(TAG, "onJoinChannelSuccess channel=$channel, uid=$uid")
                if (!isCallee) _callState.value = CallState.Calling 
            }
            override fun onUserJoined(uid: Int, elapsed: Int) {
                android.util.Log.d(TAG, "onUserJoined uid=$uid")
                _remoteUid.value = uid
                _callState.value = CallState.Connected
                agoraManager.setSpeaker(true)
            }
            override fun onUserOffline(uid: Int, reason: Int) {
                android.util.Log.d(TAG, "onUserOffline uid=$uid, reason=$reason")
                _remoteUid.value = null
                _callState.value = CallState.Ended
            }
            override fun onError(err: Int) {
                android.util.Log.e(TAG, "Agora onError: $err")
                _callState.value = CallState.Error("Agora error: $err")
            }
        }

        val success = agoraManager.initEngine(handler)
        android.util.Log.d(TAG, "initEngine result=$success")
        
        if (!success) {
            _callState.value = CallState.Error("Lỗi khởi tạo Agora. Kiểm tra App ID!")
            return false
        }

        if (type == "video") {
            android.util.Log.d(TAG, "Video type: waiting for UI to call joinVideoWithView")
        } else {
            agoraManager.joinVoiceChannel(channelName)
            agoraManager.setSpeaker(true)
        }
        return true
    }

    fun joinVideoWithView(localView: SurfaceView, channelName: String) {
        android.util.Log.d(TAG, "joinVideoWithView() channelName=$channelName, engineInit=${agoraManager.isInitialized()}")
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

    companion object {
        private const val TAG = "CallViewModel"
    }
}

