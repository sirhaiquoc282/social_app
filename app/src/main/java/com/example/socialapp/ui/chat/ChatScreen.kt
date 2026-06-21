package com.example.socialapp.ui.chat

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.socialapp.data.model.Message
import com.example.socialapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    otherUid: String,
    otherName: String,
    otherAvatar: String,
    onBack: () -> Unit,
    onNavigateToVoiceCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String) -> Unit,
    onNavigateToVideoCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val newNotification by viewModel.newNotification.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val currentUid = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }

    LaunchedEffect(otherUid) { viewModel.loadMessages(otherUid) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBg)
        ) {
            // Top bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ChatboxTeal)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White)
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(ChatboxTealDark),
                        contentAlignment = Alignment.Center
                    ) {
                        if (otherAvatar.isNotBlank()) {
                            AsyncImage(model = otherAvatar, contentDescription = null,
                                modifier = Modifier.fillMaxSize())
                        } else {
                            Text(otherName.firstOrNull()?.uppercase() ?: "?",
                                color = White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(otherName, color = White, fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge)
                        Text("Active now", color = White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall)
                    }

                    IconButton(onClick = {
                        onNavigateToVoiceCall("new", otherUid, otherName, otherAvatar)
                    }) {
                        Icon(Icons.Default.Call, null, tint = White)
                    }
                    IconButton(onClick = {
                        onNavigateToVideoCall("new", otherUid, otherName, otherAvatar)
                    }) {
                        Icon(Icons.Default.Videocam, null, tint = White)
                    }
                }
            }

            // Message list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkBg)
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (messages.isNotEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Surface(
                                color = DarkSurface,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    "Today",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
                items(messages) { message ->
                    MessageBubble(message = message, isMine = message.senderId == currentUid,
                        otherName = otherName, otherAvatar = otherAvatar)
                }
            }

            // Input bar
            ChatInputBar(
                text = inputText,
                onTextChange = { viewModel.setInputText(it) },
                onSend = { viewModel.sendMessage() }
            )
        }

        // --- Notification Banner for other messages ---
        AnimatedVisibility(
            visible = newNotification != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 60.dp) // Below the top bar
                .padding(horizontal = 16.dp)
        ) {
            newNotification?.let { conv ->
                Surface(
                    color = ChatboxTealAccent,
                    shape = RoundedCornerShape(12.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissNotification() }
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
private fun MessageBubble(message: Message, isMine: Boolean, otherName: String, otherAvatar: String) {
    val timeStr = message.sentAt?.toDate()?.let {
        SimpleDateFormat("hh:mm a", Locale.getDefault()).format(it)
    } ?: ""

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(ChatboxTeal),
                contentAlignment = Alignment.Center
            ) {
                if (otherAvatar.isNotBlank()) {
                    AsyncImage(model = otherAvatar, contentDescription = null,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Text(otherName.firstOrNull()?.uppercase() ?: "?",
                        color = White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 260.dp)
        ) {
            if (!isMine) {
                Text(otherName, color = TextSecondary, style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            }
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isMine) 18.dp else 4.dp,
                            bottomEnd = if (isMine) 4.dp else 18.dp
                        )
                    )
                    .background(if (isMine) BubbleSent else BubbleReceived)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    color = White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = DarkCard,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = {}) {
                Icon(Icons.Default.AttachFile, null, tint = TextSecondary)
            }

            TextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Write your message", color = TextHint) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    cursorColor = ChatboxTealAccent
                ),
                maxLines = 4
            )

            Spacer(Modifier.width(6.dp))

            IconButton(onClick = {}) {
                Icon(Icons.Default.CameraAlt, null, tint = TextSecondary)
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Mic, null, tint = TextSecondary)
            }

            if (text.isNotBlank()) {
                FloatingActionButton(
                    onClick = onSend,
                    modifier = Modifier.size(42.dp),
                    containerColor = ChatboxTeal,
                    contentColor = White,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
