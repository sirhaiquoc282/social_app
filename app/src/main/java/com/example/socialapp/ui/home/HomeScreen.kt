package com.example.socialapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.data.model.Conversation
import com.example.socialapp.data.model.User
import com.example.socialapp.ui.call.CallViewModel
import com.example.socialapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToChat: (uid: String, name: String, avatar: String) -> Unit,
    onNavigateToVoiceCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String, isCallee: Boolean) -> Unit,
    onNavigateToVideoCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String, isCallee: Boolean) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val incomingCall by callViewModel.currentCallSignal.collectAsState()
    var showIncomingCallDialog by remember { mutableStateOf(false) }

    LaunchedEffect(incomingCall) {
        if (incomingCall?.status == "ringing") showIncomingCallDialog = true
    }

    if (showIncomingCallDialog && incomingCall != null) {
        val signal = incomingCall!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {}
        ) {
            com.example.socialapp.ui.call.IncomingCallScreen(
                callerName = signal.callerName,
                callerAvatar = signal.callerAvatar,
                callType = signal.type,
                onAccept = {
                    showIncomingCallDialog = false
                    callViewModel.onIncomingCall(signal)
                    if (signal.type == "video")
                        onNavigateToVideoCall(signal.id, signal.callerId, signal.callerName, signal.callerAvatar, true)
                    else
                        onNavigateToVoiceCall(signal.id, signal.callerId, signal.callerName, signal.callerAvatar, true)
                },
                onDecline = {
                    showIncomingCallDialog = false
                    callViewModel.declineCall()
                }
            )
        }
    }

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
                    Icon(Icons.Default.Search, contentDescription = "Search", tint = White)
                }
                Text(
                    "Home",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                // Current user avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChatboxTealDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentUser?.avatarUrl?.isNotBlank() == true) {
                        AsyncImage(
                            model = currentUser!!.avatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            currentUser?.displayName?.firstOrNull()?.uppercase() ?: "?",
                            color = White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Status / story row
        if (allUsers.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatboxTeal)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                // My status
                item {
                    StatusAvatar(
                        name = "My status",
                        avatarUrl = currentUser?.avatarUrl ?: "",
                        isMe = true,
                        onClick = {}
                    )
                }
                items(allUsers.take(8)) { user ->
                    StatusAvatar(
                        name = user.displayName.split(" ").first(),
                        avatarUrl = user.avatarUrl,
                        isOnline = user.status == "online",
                        onClick = { onNavigateToChat(user.uid, user.displayName, user.avatarUrl) }
                    )
                }
            }
        }

        // Conversation list
        if (conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(DarkCard),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ChatBubbleOutline, null,
                        modifier = Modifier.size(56.dp), tint = TextSecondary)
                    Spacer(Modifier.height(12.dp))
                    Text("No messages yet", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    Text("Tap Contacts to start a chat", color = TextHint, style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkCard),
                contentPadding = PaddingValues(top = 8.dp)
            ) {
                items(conversations) { conv ->
                    ConversationItem(
                        conversation = conv,
                        currentUid = viewModel.getCurrentUid(),
                        onClick = { onNavigateToChat(conv.otherUserId, conv.otherUserName, conv.otherUserAvatar) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusAvatar(
    name: String,
    avatarUrl: String,
    isMe: Boolean = false,
    isOnline: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(ChatboxTealDark),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl.isNotBlank()) {
                    AsyncImage(model = avatarUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Text(name.firstOrNull()?.uppercase() ?: "?",
                        color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                if (isMe) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ChatboxTeal.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (isOnline && !isMe) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(OnlineGreen))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, color = White, fontSize = 11.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    currentUid: String,
    onClick: () -> Unit
) {
    val timeStr = conversation.lastMessageAt?.toDate()?.let {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
    } ?: ""
    val isMyMessage = conversation.lastSenderId == currentUid

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(DarkCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ChatboxTeal),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.otherUserAvatar.isNotBlank()) {
                AsyncImage(model = conversation.otherUserAvatar, contentDescription = null,
                    modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    conversation.otherUserName.firstOrNull()?.uppercase() ?: "?",
                    color = White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    conversation.otherUserName,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(timeStr, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(3.dp))
            Text(
                if (isMyMessage) "You: ${conversation.lastMessage}" else conversation.lastMessage,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = DarkDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 82.dp))
}
