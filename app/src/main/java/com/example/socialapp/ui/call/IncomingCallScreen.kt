package com.example.socialapp.ui.call

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.socialapp.ui.call.voice.PulsingAvatar
import com.example.socialapp.ui.theme.*

@Composable
fun IncomingCallScreen(
    callerName: String,
    callerAvatar: String,
    callType: String,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        // Blurred avatar background
        if (callerAvatar.isNotBlank()) {
            AsyncImage(
                model = callerAvatar,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.25f
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.6f))

            // Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(ChatboxTeal),
                contentAlignment = Alignment.Center
            ) {
                if (callerAvatar.isNotBlank()) {
                    AsyncImage(model = callerAvatar, contentDescription = null,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Text(callerName.firstOrNull()?.uppercase() ?: "?",
                        color = White, fontWeight = FontWeight.Bold, fontSize = 40.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(callerName, color = White, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                if (callType == "video") "Incoming video call" else "Incoming call",
                color = White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.weight(1f))

            // Reminder / Message row
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Alarm, null, tint = White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Remind me", color = White, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null, tint = White, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(4.dp))
                    Text("Message", color = White, style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Slide to answer pill
            SlideToAnswerButton(
                onAccept = onAccept,
                onDecline = onDecline
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SlideToAnswerButton(onAccept: () -> Unit, onDecline: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50.dp),
        color = Color(0xFF4A4A4A),
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Accept button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AcceptCallGreen),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onAccept, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Call, null, tint = White, modifier = Modifier.size(24.dp))
                }
            }

            Text("slide to answer", color = White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodyMedium)

            // Decline button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(EndCallRed),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onDecline, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.CallEnd, null, tint = White, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
