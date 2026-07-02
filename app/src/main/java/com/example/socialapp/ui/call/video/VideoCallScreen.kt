package com.example.socialapp.ui.call.video

import android.view.SurfaceView
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.ui.call.CallState
import com.example.socialapp.ui.call.CallViewModel
import com.example.socialapp.ui.call.RequestCallPermissions
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
    var permissionGranted by remember { 
        mutableStateOf(com.example.socialapp.util.PermissionHelper.hasVideoPermissions(context)) 
    }

    val localSurfaceView = remember { viewModel.createRendererView(context) }
    val remoteSurfaceView = remember { viewModel.createRendererView(context) }

    // Xin quyền Camera/Mic
    com.example.socialapp.ui.call.RequestCallPermissions(
        callType = "video",
        onGranted = { permissionGranted = true },
        onDenied = { onCallEnded() }
    ) { requestPermission ->
        LaunchedEffect(Unit) { requestPermission() }
    }

    // Kết thúc cuộc gọi khi trạng thái thay đổi
    LaunchedEffect(callState) {
        when (callState) {
            is CallState.Ended, is CallState.Declined -> onCallEnded()
            else -> {}
        }
    }

    // Bắt đầu cuộc gọi SAU KHI đã có quyền
    LaunchedEffect(permissionGranted, callState) {
        if (!permissionGranted) return@LaunchedEffect
        if (!isCallee && callId == "new" && callState is CallState.Idle) {
            // Caller: tạo cuộc gọi → initEngine → set readyToJoinVideo
            viewModel.startCall(
                context = context,
                calleeId = calleeId,
                callerName = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.displayName ?: "Unknown",
                callerAvatar = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.photoUrl?.toString() ?: "",
                calleeName = calleeName,
                calleeAvatar = calleeAvatar,
                type = "video"
            )
        }
    }

    // CHỈ join video channel SAU KHI engine đã init xong VÀ có channelName
    LaunchedEffect(readyToJoinVideo, permissionGranted) {
        val channelName = readyToJoinVideo ?: return@LaunchedEffect
        if (permissionGranted && channelName.isNotEmpty()) {
            android.util.Log.d("VideoCallScreen", "Joining video channel: $channelName")
            viewModel.joinVideoWithView(localSurfaceView, channelName)
        }
    }

    LaunchedEffect(remoteUid) {
        remoteUid?.let { uid -> viewModel.setupRemoteVideo(remoteSurfaceView, uid) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Remote video full screen
        if (callState is CallState.Connected && remoteUid != null) {
            AndroidView(
                factory = {
                    remoteSurfaceView.apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PulsingAvatar(
                        name = calleeName, avatarUrl = calleeAvatar,
                        isPulsing = callState is CallState.Calling || callState is CallState.Ringing
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(calleeName, color = White, style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        when (callState) {
                            is CallState.Calling -> "Calling video..."
                            is CallState.Ringing -> "Ringing..."
                            else -> "Video call"
                        },
                        color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // Back arrow
        IconButton(
            onClick = { viewModel.endCall() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White)
        }

        // Local PiP (top right)
        if (!isCamMuted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 8.dp, end = 12.dp)
                    .size(width = 96.dp, height = 130.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, ChatboxTealAccent, RoundedCornerShape(12.dp))
                    .background(Color.DarkGray)
            ) {
                AndroidView(
                    factory = { 
                        localSurfaceView.apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Timer overlay (top left)
        if (callState is CallState.Connected) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 56.dp, top = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Videocam, null, tint = ChatboxTealAccent,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    CallDurationTimer()
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            when {
                isCallee && callState is CallState.Ringing -> {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CallActionButton(Icons.Default.CallEnd, "Decline", EndCallRed, 72.dp,
                            onClick = { viewModel.declineCall() })
                        CallActionButton(Icons.Default.Videocam, "Answer", AcceptCallGreen, 72.dp,
                            onClick = { viewModel.acceptCall(context) })
                    }
                }
                else -> {
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        CallActionButton(
                            if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            "Mute",
                            Color(0xFF3A3A3A),
                            onClick = { viewModel.toggleMic() }
                        )
                        CallActionButton(
                            Icons.Default.VolumeUp, "Speaker",
                            Color(0xFF3A3A3A),
                            onClick = { viewModel.toggleSpeaker() }
                        )
                        CallActionButton(
                            if (isCamMuted) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            "Camera",
                            Color(0xFF3A3A3A),
                            onClick = { viewModel.toggleCamera() }
                        )
                        CallActionButton(
                            Icons.Default.ChatBubbleOutline, "Chat",
                            ChatboxTeal,
                            onClick = {}
                        )
                        CallActionButton(
                            Icons.Default.Close, "End",
                            EndCallRed,
                            onClick = { viewModel.endCall() }
                        )
                    }
                }
            }
        }
    }
}
