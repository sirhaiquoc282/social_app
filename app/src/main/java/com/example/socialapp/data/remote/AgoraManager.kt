package com.example.socialapp.data.remote

import android.content.Context
import android.util.Log
import android.view.SurfaceView
import com.example.socialapp.BuildConfig
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgoraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var engine: RtcEngine? = null

    // Dùng Object Lock để đảm bảo destroy() chạy xong trước khi initEngine() tạo engine mới
    private val engineLock = Object()

    /** Khởi tạo Agora engine với event handler */
    fun initEngine(eventHandler: IRtcEngineEventHandler): Boolean {
        val appId = BuildConfig.AGORA_APP_ID.trim()
        Log.d(TAG, "initEngine() - AppID: ${appId.take(4)}...${appId.takeLast(4)} (Length: ${appId.length})")

        if (appId.isEmpty()) {
            Log.e(TAG, "AppID is EMPTY!")
            return false
        }

        return try {
            // Ép buộc nạp thư viện native của Agora trước (giúp sửa lỗi trên một số máy ảo)
            try {
                System.loadLibrary("agora-rtc-sdk")
                Log.d(TAG, "Native library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load native library: ${e.message}")
            }

            // QUAN TRỌNG: Dùng synchronized để chờ destroy() cũ chạy xong (nếu có)
            synchronized(engineLock) {
                // Xóa engine cũ nếu có (đồng bộ, không chạy ngầm)
                engine?.let {
                    Log.d(TAG, "Destroying old engine before creating new one...")
                    it.setupLocalVideo(VideoCanvas(null))
                    it.stopPreview()
                    it.disableVideo()
                    it.disableAudio()
                    it.leaveChannel()
                    RtcEngine.destroy()
                    engine = null
                    // Chờ Agora native layer dọn dẹp xong
                    Thread.sleep(200)
                }

                // Tạo engine mới
                engine = RtcEngine.create(context, appId, eventHandler)
            }

            if (engine == null) {
                Log.e(TAG, "RtcEngine.create() still returns NULL!")
                false
            } else {
                Log.d(TAG, "Engine created successfully!")
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during init: ${e.message}", e)
            false
        }
    }

    /** Tham gia channel thoại */
    fun joinVoiceChannel(channelName: String, uid: Int = 0) {
        if (engine == null) return
        engine?.apply {
            enableAudio()
            disableVideo()
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                autoSubscribeAudio = true
                autoSubscribeVideo = false
            }
            val token = if (BuildConfig.AGORA_TOKEN.isNotEmpty()) BuildConfig.AGORA_TOKEN else null
            joinChannel(token, channelName, uid, options)
        }
    }

    /** Tham gia channel video */
    fun joinVideoChannel(channelName: String, localView: SurfaceView, uid: Int = 0) {
        if (engine == null) return
        engine?.apply {
            enableAudio()
            enableVideo()
            setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            startPreview()

            if (channelName.isNotEmpty()) {
                val options = ChannelMediaOptions().apply {
                    clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                    channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                    publishMicrophoneTrack = true
                    publishCameraTrack = true
                    autoSubscribeAudio = true
                    autoSubscribeVideo = true
                }
                val token = if (BuildConfig.AGORA_TOKEN.isNotEmpty()) BuildConfig.AGORA_TOKEN else null
                joinChannel(token, channelName, uid, options)
            }
        }
    }

    /** Setup video cho remote user */
    fun setupRemoteVideo(remoteView: SurfaceView, remoteUid: Int) {
        engine?.setupRemoteVideo(
            VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid)
        )
    }

    /** Setup video cho local user */
    fun setupLocalVideo(localView: SurfaceView) {
        engine?.setupLocalVideo(
            VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )
    }

    fun muteAudio(muted: Boolean) { engine?.muteLocalAudioStream(muted) }
    fun muteVideo(muted: Boolean) { engine?.muteLocalVideoStream(muted) }
    fun switchCamera() { engine?.switchCamera() }
    fun setSpeaker(on: Boolean) { engine?.setEnableSpeakerphone(on) }

    /**
     * Rời khỏi channel hiện tại nhưng GIỮ engine lại (để tái sử dụng nhanh cho cuộc gọi kế tiếp).
     * Tắt hết Camera, Mic, Preview để trả lại tài nguyên phần cứng cho hệ điều hành.
     */
    fun leaveChannel() {
        engine?.apply {
            setupLocalVideo(VideoCanvas(null))  // Gỡ tham chiếu SurfaceView
            leaveChannel()
            stopPreview()
            disableVideo()
            disableAudio()
        }
        Log.d(TAG, "leaveChannel() completed - engine kept alive for reuse")
    }

    /**
     * Phá hủy hoàn toàn engine. Chỉ dùng khi ViewModel bị hủy (onCleared).
     * ĐỒNG BỘ (synchronous) - chạy trên luồng hiện tại để đảm bảo dọn dẹp xong 100%.
     */
    fun destroy() {
        synchronized(engineLock) {
            engine?.let {
                Log.d(TAG, "destroy() - releasing all resources...")
                it.setupLocalVideo(VideoCanvas(null))
                it.stopPreview()
                it.disableVideo()
                it.disableAudio()
                it.leaveChannel()
                RtcEngine.destroy()
                Log.d(TAG, "destroy() - RtcEngine destroyed successfully")
            }
            engine = null
        }
    }

    fun isInitialized(): Boolean = engine != null

    companion object {
        private const val TAG = "AgoraManager"
    }
}
