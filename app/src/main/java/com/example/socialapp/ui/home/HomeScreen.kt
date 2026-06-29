package com.example.socialapp.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val incomingNotification by viewModel.incomingNotification.collectAsState()
    val currentUid = viewModel.getCurrentUid()

    Box(modifier = Modifier.fillMaxSize()) {
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Search icon on the left
                    IconButton(
                        onClick = {},
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = White)
                    }

                    // Title with Notification Dot
                    Box(contentAlignment = Alignment.TopEnd) {
                        Text(
                            "Home",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = White,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        if (conversations.any { !it.isReadBy(currentUid) && it.lastSenderId != currentUid }) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MissedRed)
                                    .padding(2.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(White))
                            }
                        }
                    }

                    // Avatar on the right
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChatboxTealDark)
                            .align(Alignment.CenterEnd),
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
                    items(
                        items = conversations,
                        key = { it.id } // Quan trọng để UI biết item nào thay đổi
                    ) { conv ->
                        ConversationItem(
                            conversation = conv,
                            currentUid = currentUid,
                            onClick = { onNavigateToChat(conv.otherUserId, conv.otherUserName, conv.otherUserAvatar) }
                        )
                    }
                }
            }
        }

        // --- Notification Banner ---
        AnimatedVisibility(
            visible = incomingNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp) // Ngay dưới top bar
                .padding(horizontal = 16.dp)
        ) {
            incomingNotification?.let { conv ->
                Surface(
                    color = ChatboxTealAccent,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { 
                            viewModel.dismissNotification()
                            onNavigateToChat(conv.otherUserId, conv.otherUserName, conv.otherUserAvatar)
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ChatBubble, null, tint = White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tin nhắn mới từ ${conv.otherUserName}",
                                color = White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                conv.lastMessage,
                                color = White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        IconButton(onClick = { viewModel.dismissNotification() }) {
                            Icon(Icons.Default.Close, null, tint = White, modifier = Modifier.size(16.dp))
                        }
                    }
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
    val isUnread = !conversation.isReadBy(currentUid) && !isMyMessage

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(DarkCard)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar với dấu chấm unread
        Box(contentAlignment = Alignment.TopEnd) {
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
            // Dấu chấm xanh trên avatar khi có tin nhắn chưa đọc
            if (isUnread) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .offset(x = 2.dp, y = (-2).dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(ChatboxTealAccent)
                    )
                }
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
                    fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold,
                    color = if (isUnread) White else TextPrimary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    timeStr, 
                    color = if (isUnread) ChatboxTealAccent else TextSecondary, 
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isMyMessage) "You: ${conversation.lastMessage}" else conversation.lastMessage,
                    color = if (isUnread) White.copy(alpha = 0.9f) else TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal
                )
                if (isUnread) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(ChatboxTealAccent)
                            .padding(2.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(White))
                    }
                }
            }
        }
    }
    HorizontalDivider(color = DarkDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 82.dp))
}
