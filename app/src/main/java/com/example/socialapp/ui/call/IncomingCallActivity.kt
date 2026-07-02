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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.core.app.NotificationManagerCompat
import com.example.socialapp.service.MyFirebaseMessagingService
import com.example.socialapp.ui.call.CallState
import com.example.socialapp.ui.call.CallViewModel
import com.google.firebase.firestore.FirebaseFirestore
import androidx.hilt.navigation.compose.hiltViewModel

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

        // ═══ BẢO VỆ: Kiểm tra trạng thái cuộc gọi TRƯỚC KHI hiển thị UI ═══
        // Nếu FCM notification đến trễ (cuộc gọi đã kết thúc), đóng Activity ngay lập tức.
        if (callId.isNotEmpty()) {
            FirebaseFirestore.getInstance().collection("calls").document(callId)
                .get()
                .addOnSuccessListener { doc ->
                    val status = doc.getString("status")
                    if (status != "ringing") {
                        android.util.Log.d("IncomingCallActivity",
                            "Call $callId status=$status (not ringing), closing immediately")
                        NotificationManagerCompat.from(this)
                            .cancel(MyFirebaseMessagingService.CALL_NOTIFICATION_ID)
                        finish()
                    }
                }
        }

        setContent {
            SocialAppTheme {
                val viewModel: CallViewModel = hiltViewModel()
                val callState by viewModel.callState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.prepareCallAsCallee(
                        callId = callId,
                        callerId = callerId,
                        callerName = callerName,
                        callerAvatar = callerAvatar,
                        type = callType
                    )
                    viewModel.startObservingIncomingCalls()
                }

                LaunchedEffect(callState) {
                    if (callState is CallState.Ended || callState is CallState.Declined) {
                        NotificationManagerCompat.from(this@IncomingCallActivity).cancel(MyFirebaseMessagingService.CALL_NOTIFICATION_ID)
                        finish()
                    }
                }

                if (callType == "video") {
                    VideoCallScreen(
                        callId = callId,
                        calleeId = callerId,
                        calleeName = callerName,
                        calleeAvatar = callerAvatar,
                        isCallee = true,
                        onCallEnded = { finish() },
                        viewModel = viewModel
                    )
                } else {
                    VoiceCallScreen(
                        callId = callId,
                        calleeId = callerId,
                        calleeName = callerName,
                        calleeAvatar = callerAvatar,
                        isCallee = true,
                        onCallEnded = { finish() },
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

