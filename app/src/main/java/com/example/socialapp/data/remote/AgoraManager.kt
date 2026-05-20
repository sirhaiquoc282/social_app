package com.example.socialapp.data.remote

import android.content.Context
import android.view.SurfaceView
import com.example.socialapp.BuildConfig
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AgoraManager @Inject constructor(
    private val context: Context
) {

    private var engine: RtcEngine? = null

    /** Khởi tạo Agora engine với event handler */
    fun initEngine(eventHandler: IRtcEngineEventHandler): Boolean {
        return try {
            val config = RtcEngineConfig().apply {
                mContext = context
                mAppId = BuildConfig.AGORA_APP_ID
                mEventHandler = eventHandler
            }
            engine = RtcEngine.create(config)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** Tham gia channel thoại */
    fun joinVoiceChannel(channelName: String, uid: Int = 0) {
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
            joinChannel(null, channelName, uid, options)
        }
    }

    /** Tham gia channel video */
    fun joinVideoChannel(channelName: String, localView: SurfaceView, uid: Int = 0) {
        engine?.apply {
            enableAudio()
            enableVideo()
            setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            startPreview()
            val options = ChannelMediaOptions().apply {
                clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
                channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
                publishMicrophoneTrack = true
                publishCameraTrack = true
                autoSubscribeAudio = true
                autoSubscribeVideo = true
            }
            joinChannel(null, channelName, uid, options)
        }
    }

    /** Setup video cho remote user */
    fun setupRemoteVideo(remoteView: SurfaceView, remoteUid: Int) {
        engine?.setupRemoteVideo(
            VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid)
        )
    }

    fun muteAudio(muted: Boolean) { engine?.muteLocalAudioStream(muted) }

    fun muteVideo(muted: Boolean) { engine?.muteLocalVideoStream(muted) }

    fun switchCamera() { engine?.switchCamera() }

    fun setSpeaker(on: Boolean) { engine?.setEnableSpeakerphone(on) }

    fun leaveChannel() { engine?.leaveChannel() }

    fun destroy() {
        engine?.let {
            it.leaveChannel()
            RtcEngine.destroy()
        }
        engine = null
    }

    fun isInitialized(): Boolean = engine != null
}

