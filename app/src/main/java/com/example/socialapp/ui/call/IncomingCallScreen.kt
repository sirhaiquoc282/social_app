package com.example.socialapp.ui.call

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.socialapp.ui.call.voice.CallActionButton
import com.example.socialapp.ui.call.voice.PulsingAvatar
import com.example.socialapp.ui.theme.*

/**
 * Màn hình cuộc gọi đến — hiển thị khi app foreground hoặc từ IncomingCallActivity.
 */
@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatar: String,
    callType: String,        // "voice" | "video"
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ringScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CallBgDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp)
        ) {
            Spacer(Modifier.weight(0.5f))

            // Type badge
            Surface(
                color = NavyBlue,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = null,
                        tint = LightSkyBlue,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (callType == "video") "Cuộc gọi video đến" else "Cuộc gọi thoại đến",
                        color = LightSkyBlue,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            PulsingAvatar(name = callerName, avatarUrl = callerAvatar, isPulsing = true)

            Spacer(Modifier.height(20.dp))

            Text(
                text = callerName,
                color = White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Đang gọi cho bạn...",
                color = LightSkyBlue,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.weight(1f))

            // Accept / Decline buttons
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Từ chối
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(EndCallRed),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onDecline, modifier = Modifier.size(72.dp)) {
                            Icon(
                                Icons.Default.CallEnd,
                                contentDescription = "Từ chối",
                                tint = White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Từ chối", color = LightSkyBlue, style = MaterialTheme.typography.bodySmall)
                }

                // Nghe
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(AcceptCallGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onAccept, modifier = Modifier.size(72.dp)) {
                            Icon(
                                imageVector = if (callType == "video") Icons.Default.Videocam else Icons.Default.Call,
                                contentDescription = "Nghe",
                                tint = White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Nghe", color = LightSkyBlue, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

