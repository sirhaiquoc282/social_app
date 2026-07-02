package com.example.socialapp.ui.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.data.model.CallSignal
import com.example.socialapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CallsScreen(
    onNavigateToVoiceCall: (calleeId: String, calleeName: String, calleeAvatar: String) -> Unit,
    onNavigateToVideoCall: (calleeId: String, calleeName: String, calleeAvatar: String) -> Unit,
    viewModel: CallsViewModel = hiltViewModel()
) {
    val history by viewModel.callHistory.collectAsState()
    val uid = viewModel.currentUid

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Teal top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChatboxTeal)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, null, tint = White)
                }
                Text("Calls", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = White)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Call, null, tint = White)
                }
            }
        }

        if (history.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(DarkCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CallEnd, null,
                        modifier = Modifier.size(56.dp), tint = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Text("No call history", color = TextSecondary)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(DarkCard),
                contentPadding = PaddingValues(top = 12.dp)
            ) {
                item {
                    Text(
                        "Recent",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(history) { signal ->
                    CallHistoryItem(
                        signal = signal,
                        currentUid = uid,
                        onVoiceCall = {
                            val otherId = if (signal.callerId == uid) signal.calleeId else signal.callerId
                            val otherName = if (signal.callerId == uid) signal.calleeName.ifBlank { "Contact" } else signal.callerName
                            val otherAvatar = if (signal.callerId == uid) signal.calleeAvatar else signal.callerAvatar
                            onNavigateToVoiceCall(otherId, otherName, otherAvatar)
                        },
                        onVideoCall = {
                            val otherId = if (signal.callerId == uid) signal.calleeId else signal.callerId
                            val otherName = if (signal.callerId == uid) signal.calleeName.ifBlank { "Contact" } else signal.callerName
                            val otherAvatar = if (signal.callerId == uid) signal.calleeAvatar else signal.callerAvatar
                            onNavigateToVideoCall(otherId, otherName, otherAvatar)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CallHistoryItem(
    signal: CallSignal,
    currentUid: String,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    val isOutgoing = signal.callerId == currentUid
    val otherName = if (isOutgoing) signal.calleeName.ifBlank { "Contact" } else signal.callerName
    val otherAvatar = if (isOutgoing) signal.calleeAvatar else signal.callerAvatar
    val isMissedByMe = !isOutgoing && (signal.status == "missed" || signal.status == "declined")
    val isAnswered = signal.status == "accepted" || signal.status == "ended"

    val timeStr = signal.createdAt?.toDate()?.let {
        val now = Date()
        val diffMs = now.time - it.time
        when {
            diffMs < 86_400_000 -> "Today, ${SimpleDateFormat("HH:mm a", Locale.getDefault()).format(it)}"
            diffMs < 172_800_000 -> "Yesterday, ${SimpleDateFormat("HH:mm a", Locale.getDefault()).format(it)}"
            else -> SimpleDateFormat("dd/MM/yy, HH:mm a", Locale.getDefault()).format(it)
        }
    } ?: ""

    val statusColor = when {
        isMissedByMe -> MissedRed
        isAnswered && isOutgoing -> OnlineGreen
        !isOutgoing && isAnswered -> ChatboxTealAccent
        isOutgoing -> TextSecondary
        else -> ChatboxTealAccent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ChatboxTeal),
            contentAlignment = Alignment.Center
        ) {
            if (otherAvatar.isNotBlank()) {
                AsyncImage(model = otherAvatar, contentDescription = null,
                    modifier = Modifier.fillMaxSize())
            } else {
                Text(otherName.firstOrNull()?.uppercase() ?: "?",
                    color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(otherName, color = TextPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isOutgoing) Icons.Default.CallMade else Icons.Default.CallReceived,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(timeStr, color = statusColor, style = MaterialTheme.typography.bodySmall)
            }
        }

        // Quick call buttons
        IconButton(onClick = onVoiceCall) {
            Icon(Icons.Default.Call, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onVideoCall) {
            Icon(Icons.Default.Videocam, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
    }
    HorizontalDivider(color = DarkDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 82.dp))
}
