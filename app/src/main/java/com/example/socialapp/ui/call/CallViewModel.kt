package com.example.socialapp.ui.call

import android.content.Context
import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialapp.data.model.CallSignal
import com.example.socialapp.data.remote.AgoraManager
import com.example.socialapp.data.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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

    // ═══ CỜ BẢO VỆ: Ngăn resetAfterCall() bị gọi nhiều lần cùng lúc ═══
    @Volatile
    private var isResetting = false

    init {
        // Tự động lắng nghe cuộc gọi đến khi app foreground
        startObservingIncomingCalls()
    }

    /** Bắt đầu lắng nghe cuộc gọi đến (foreground) */
    fun startObservingIncomingCalls() {
        incomingCallJob?.cancel()
        incomingCallJob = viewModelScope.launch {
            callRepository.observeIncomingCall().collect { signal ->
                android.util.Log.d(TAG, "observeIncomingCall → signal=${signal?.id}, state=${_callState.value}, isResetting=$isResetting")

                // Nếu đang trong quá trình reset, bỏ qua mọi event
                if (isResetting) return@collect

                if (signal != null && _callState.value == CallState.Idle) {
                    _currentCallSignal.value = signal
                    currentCallId = signal.id
                    isCallee = true
                    _callState.value = CallState.Ringing

                    // SHOW NOTIFICATION BẰNG TAY Ở ĐÂY (thay vì phụ thuộc Cloud Function)
                    com.example.socialapp.service.MyFirebaseMessagingService.showIncomingCallNotificationLocal(context, signal)

                    // Bắt đầu observe trạng thái cuộc gọi này
                    // để phát hiện khi caller hủy cuộc gọi
                    observeCallStatus(signal.id)

                } else if (signal == null && _callState.value == CallState.Ringing && isCallee) {
                    // Caller đã hủy cuộc gọi (status không còn "ringing")
                    android.util.Log.d(TAG, "Incoming call cancelled by caller (detected via observeIncomingCall)")
                    cleanupCall()
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
            cleanupCall()
        }
    }

    // ─── Kết thúc cuộc gọi (cả 2 bên) ───────────────────────────────────────
    fun endCall() {
        viewModelScope.launch {
            android.util.Log.d(TAG, "endCall() called, currentCallId=$currentCallId")

            // 1. Dừng lắng nghe Firestore NGAY LẬP TỨC
            observeJob?.cancel()
            observeJob = null

            // 2. Rời khỏi Agora channel TRƯỚC (dừng âm thanh ngay lập tức)
            agoraManager.leaveChannel()

            // 3. Cập nhật trạng thái Firestore
            try { callRepository.endCall(currentCallId) } catch (_: Exception) {}

            // 4. Dọn dẹp toàn bộ
            cleanupCall()
        }
    }

    /**
     * ═══ HÀM DỌN DẸP TRUNG TÂM ═══
     * Đây là hàm DUY NHẤT chịu trách nhiệm dọn dẹp sau mỗi cuộc gọi.
     * Có cờ bảo vệ [isResetting] để CHẮC CHẮN chỉ chạy 1 lần duy nhất.
     */
    private fun cleanupCall() {
        // ═══ GUARD: Nếu đang dọn dẹp rồi thì dừng ngay, không làm gì thêm ═══
        if (isResetting) {
            android.util.Log.d(TAG, "cleanupCall() SKIPPED - already resetting")
            return
        }
        isResetting = true
        android.util.Log.d(TAG, "cleanupCall() STARTED for callId=$currentCallId")

        // 1. Dừng TẤT CẢ listeners ngay lập tức
        observeJob?.cancel()
        observeJob = null
        incomingCallJob?.cancel()
        incomingCallJob = null

        // 2. Rời channel Agora (an toàn nếu gọi nhiều lần)
        agoraManager.leaveChannel()

        // 3. Xoá thông báo cuộc gọi trong System Tray NGAY LẬP TỨC
        com.example.socialapp.service.MyFirebaseMessagingService.cancelCallNotification(context)

        // 4. Cập nhật UI state → Dialog sẽ bị ẩn ngay trong Compose
        _callState.value = CallState.Ended
        _currentCallSignal.value = null

        // 5. Reset toàn bộ internal state
        currentCallId = ""
        isCallee = false
        _isMicMuted.value = false
        _isCamMuted.value = false
        _remoteUid.value = null
        _readyToJoinVideo.value = null
        _isSpeakerOn.value = true

        // 6. Sau 1 giây, chuyển về Idle và bắt đầu lắng nghe cuộc gọi mới
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000)
            _callState.value = CallState.Idle
            isResetting = false  // ═══ MỞ KHOÁ cho cuộc gọi kế tiếp ═══
            android.util.Log.d(TAG, "cleanupCall() COMPLETED - ready for next call")
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
                // Khi đối phương thoát khỏi channel → kết thúc cuộc gọi
                cleanupCall()
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

                // Bỏ qua nếu đang dọn dẹp
                if (isResetting) return@collect

                // CHỈ xử lý nếu callId khớp với cuộc gọi hiện tại
                if (signal.id != currentCallId && currentCallId.isNotEmpty()) {
                    android.util.Log.d(TAG, "Ignoring stale event for callId=${signal.id}, currentCallId=$currentCallId")
                    return@collect
                }

                android.util.Log.d(TAG, "observeCallStatus → callId=${signal.id}, status=${signal.status}")
                _currentCallSignal.value = signal

                when (signal.status) {
                    "accepted" -> {
                        if (!isCallee) _callState.value = CallState.Connected
                    }
                    "declined" -> {
                        android.util.Log.d(TAG, "Call declined by callee")
                        cleanupCall()
                    }
                    "ended" -> {
                        android.util.Log.d(TAG, "Call ended (detected via observeCallStatus)")
                        cleanupCall()
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
