package com.example.socialapp.ui.call

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.socialapp.ui.call.voice.VoiceCallScreen
import com.example.socialapp.ui.call.video.VideoCallScreen
import com.example.socialapp.ui.theme.SocialAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity hiển thị màn hình cuộc gọi đến từ FCM notification.
 * Được start khi user nhấn notification khi app ở background.
 */
@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val callId = intent.getStringExtra("callId") ?: ""
        val callerName = intent.getStringExtra("callerName") ?: "Unknown"
        val callerAvatar = intent.getStringExtra("callerAvatar") ?: ""
        val callType = intent.getStringExtra("callType") ?: "voice"
        val callerId = intent.getStringExtra("callerId") ?: ""

        setContent {
            SocialAppTheme {
                if (callType == "video") {
                    VideoCallScreen(
                        callId = callId,
                        calleeId = callerId,
                        calleeName = callerName,
                        calleeAvatar = callerAvatar,
                        isCallee = true,
                        onCallEnded = { finish() }
                    )
                } else {
                    VoiceCallScreen(
                        callId = callId,
                        calleeId = callerId,
                        calleeName = callerName,
                        calleeAvatar = callerAvatar,
                        isCallee = true,
                        onCallEnded = { finish() }
                    )
                }
            }
        }
    }
}

