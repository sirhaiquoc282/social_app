package com.example.socialapp.ui.call.video

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.ui.call.CallState
import com.example.socialapp.ui.call.CallViewModel
import com.example.socialapp.ui.call.voice.CallActionButton
import com.example.socialapp.ui.call.voice.CallDurationTimer
import com.example.socialapp.ui.call.voice.PulsingAvatar
import com.example.socialapp.ui.theme.*

@Composable
fun VideoCallScreen(
    callId: String,
    calleeId: String,
    calleeName: String,
    calleeAvatar: String,
    isCallee: Boolean,
    onCallEnded: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val callState by viewModel.callState.collectAsState()
    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val isCamMuted by viewModel.isCamMuted.collectAsState()
    val remoteUid by viewModel.remoteUid.collectAsState()
    val readyToJoinVideo by viewModel.readyToJoinVideo.collectAsState()
    var permissionGranted by remember { mutableStateOf(false) }

    val localSurfaceView = remember { SurfaceView(context) }
    val remoteSurfaceView = remember { SurfaceView(context) }

    // Yêu cầu quyền Camera & Micro
    com.example.socialapp.ui.call.RequestCallPermissions(
        callType = "video",
        onGranted = { permissionGranted = true },
        onDenied = { onCallEnded() }
    ) { requestPermission ->
        LaunchedEffect(Unit) { requestPermission() }
    }

    LaunchedEffect(callState) {
        when (callState) {
            is CallState.Ended, is CallState.Declined -> onCallEnded()
            else -> {}
        }
    }

    // Caller: join video ngay khi callId thực được emit
    LaunchedEffect(readyToJoinVideo) {
        readyToJoinVideo?.let { channelName ->
            if (permissionGranted) {
                viewModel.joinVideoWithView(localSurfaceView, channelName)
            }
        }
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect

        if (!isCallee && callId == "new" && callState is CallState.Idle) {
            // Caller: khởi tạo video call
            viewModel.startCall(
                context = context,
                calleeId = calleeId,
                callerName = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.displayName ?: "Unknown",
                callerAvatar = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.photoUrl?.toString() ?: "",
                type = "video"
            )
        } else if (isCallee && callState is CallState.Ringing) {
            // Callee: Chuẩn bị signal
            viewModel.prepareCallAsCallee(
                callId = callId,
                callerId = calleeId,
                callerName = calleeName,
                callerAvatar = calleeAvatar,
                type = "video"
            )
            // Tự động accept nếu vào từ Activity
            viewModel.acceptCall(context)
            viewModel.joinVideoWithView(localSurfaceView, callId)
        }
    }

    // Setup remote video khi có remote uid
    LaunchedEffect(remoteUid) {
        remoteUid?.let { uid ->
            viewModel.setupRemoteVideo(remoteSurfaceView, uid)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBgDark)
    ) {
        // Remote video (full screen)
        if (callState is CallState.Connected && remoteUid != null) {
            AndroidView(
                factory = { remoteSurfaceView.apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }},
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Waiting state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulsingAvatar(
                        name = calleeName,
                        avatarUrl = calleeAvatar,
                        isPulsing = callState is CallState.Calling || callState is CallState.Ringing
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(calleeName, color = White, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = when (callState) {
                            is CallState.Calling -> "Đang gọi video..."
                            is CallState.Ringing -> "Đang đổ chuông..."
                            else -> "Gọi video"
                        },
                        color = LightSkyBlue,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Local video preview (góc nhỏ — Picture-in-Picture)
        if (!isCamMuted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 60.dp, end = 16.dp)
                    .size(width = 100.dp, height = 140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, LightSkyBlue, RoundedCornerShape(12.dp))
                    .background(NavyBlueDark)
            ) {
                AndroidView(
                    factory = { localSurfaceView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }},
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Status / Timer overlay (top)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            if (callState is CallState.Connected) {
                Surface(
                    color = NavyBlueDark.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Videocam, contentDescription = null,
                            tint = LightSkyBlue, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        CallDurationTimer()
                    }
                }
            }
        }

        // Controls overlay (bottom)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            when {
                isCallee && callState is CallState.Ringing -> {
                    // Incoming video call
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CallActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "Từ chối",
                            color = EndCallRed,
                            size = 72.dp,
                            onClick = { viewModel.declineCall() }
                        )
                        CallActionButton(
                            icon = Icons.Default.Videocam,
                            label = "Nghe",
                            color = AcceptCallGreen,
                            size = 72.dp,
                            onClick = { viewModel.acceptCall(context) }
                        )
                    }
                }
                else -> {
                    // In-call controls
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    ) {
                        // Tắt/Bật mic
                        CallActionButton(
                            icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMicMuted) "Bật mic" else "Tắt mic",
                            color = if (isMicMuted) WarningAmber else MuteButtonColor,
                            onClick = { viewModel.toggleMic() }
                        )
                        // Kết thúc
                        CallActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "Kết thúc",
                            color = EndCallRed,
                            size = 72.dp,
                            onClick = { viewModel.endCall() }
                        )
                        // Tắt/Bật camera
                        CallActionButton(
                            icon = if (isCamMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            label = if (isCamMuted) "Bật cam" else "Tắt cam",
                            color = if (isCamMuted) WarningAmber else MuteButtonColor,
                            onClick = { viewModel.toggleCamera() }
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Đổi camera
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CallActionButton(
                            icon = Icons.Default.Cameraswitch,
                            label = "Đổi cam",
                            color = MuteButtonColor,
                            onClick = { viewModel.switchCamera() }
                        )
                    }
                }
            }
        }
    }
}
