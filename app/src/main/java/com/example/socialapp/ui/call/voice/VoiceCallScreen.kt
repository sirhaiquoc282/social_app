package com.example.socialapp.ui.call.voice

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.ui.call.CallState
import com.example.socialapp.ui.call.CallViewModel
import com.example.socialapp.ui.call.RequestCallPermissions
import com.example.socialapp.ui.theme.*

@Composable
fun VoiceCallScreen(
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
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val callSignal by viewModel.currentCallSignal.collectAsState()
    var permissionGranted by remember { mutableStateOf(false) }

    RequestCallPermissions(
        callType = "voice",
        onGranted = { permissionGranted = true },
        onDenied = { onCallEnded() }
    ) { requestPermission ->
        LaunchedEffect(Unit) { requestPermission() }
    }

    // Xử lý khi call ended/declined
    LaunchedEffect(callState) {
        when (callState) {
            is CallState.Ended, is CallState.Declined -> onCallEnded()
            else -> {}
        }
    }

    // Khởi tạo: nếu là caller (callId == "new"), bắt đầu cuộc gọi khi đã có quyền
    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) return@LaunchedEffect

        if (!isCallee && callId == "new" && callState is CallState.Idle) {
            viewModel.startCall(
                context = context,
                calleeId = calleeId,
                callerName = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.displayName ?: "Unknown",
                callerAvatar = com.google.firebase.auth.FirebaseAuth.getInstance()
                    .currentUser?.photoUrl?.toString() ?: "",
                type = "voice"
            )
        } else if (isCallee && callState is CallState.Ringing) {
            // Đến từ IncomingCallActivity: setup signal trước khi accept
            viewModel.prepareCallAsCallee(
                callId = callId,
                callerId = calleeId,   // calleeId param holds callerId when isCallee
                callerName = calleeName,
                callerAvatar = calleeAvatar,
                type = "voice"
            )
            viewModel.acceptCall(context)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBgDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.5f))

            // Status text
            Text(
                text = when (callState) {
                    is CallState.Calling -> "Đang gọi..."
                    is CallState.Ringing -> "Đang đổ chuông..."
                    is CallState.Connected -> "Đang kết nối"
                    is CallState.Declined -> "Đã từ chối"
                    else -> "Cuộc gọi thoại"
                },
                color = LightSkyBlue,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(24.dp))

            // Avatar với pulse animation khi đang gọi
            PulsingAvatar(
                name = calleeName,
                avatarUrl = calleeAvatar,
                isPulsing = callState is CallState.Calling || callState is CallState.Ringing
            )

            Spacer(Modifier.height(20.dp))

            // Tên người dùng
            Text(
                text = calleeName,
                color = White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Call duration (khi connected)
            if (callState is CallState.Connected) {
                CallDurationTimer()
            }

            Spacer(Modifier.weight(1f))

            // Controls
            when {
                isCallee && callState is CallState.Ringing -> {
                    // Incoming: Từ chối & Nghe
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
                            icon = Icons.Default.Call,
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
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CallActionButton(
                            icon = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (isMicMuted) "Bật mic" else "Tắt mic",
                            color = if (isMicMuted) WarningAmber else MuteButtonColor,
                            onClick = { viewModel.toggleMic() }
                        )
                        CallActionButton(
                            icon = Icons.Default.CallEnd,
                            label = "Kết thúc",
                            color = EndCallRed,
                            size = 72.dp,
                            onClick = { viewModel.endCall() }
                        )
                        CallActionButton(
                            icon = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            label = if (isSpeakerOn) "Loa ngoài" else "Loa trong",
                            color = if (isSpeakerOn) SkyBlue else MuteButtonColor,
                            onClick = { viewModel.toggleSpeaker() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PulsingAvatar(name: String, avatarUrl: String, isPulsing: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPulsing) 1.12f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = if (isPulsing) 0.2f else 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        // Pulse ring
        if (isPulsing) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(LightSkyBlue.copy(alpha = alpha))
            )
        }
        // Avatar
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(NavyBlue),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl.isNotBlank()) {
                AsyncImage(model = avatarUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    text = name.firstOrNull()?.uppercase() ?: "?",
                    color = White,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun CallActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    size: Dp = 56.dp,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, modifier = Modifier.size(size)) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = LightSkyBlue, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun CallDurationTimer() {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            seconds++
        }
    }
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    val timeStr = if (h > 0) {
        "%02d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
    Text(timeStr, color = LightSkyBlue, style = MaterialTheme.typography.bodyMedium)
}

