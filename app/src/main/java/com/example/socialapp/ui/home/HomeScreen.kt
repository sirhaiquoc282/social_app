package com.example.socialapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.data.model.Conversation
import com.example.socialapp.data.model.User
import com.example.socialapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToChat: (otherUid: String, otherName: String, otherAvatar: String) -> Unit,
    onNavigateToVoiceCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String, isCallee: Boolean) -> Unit,
    onNavigateToVideoCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String, isCallee: Boolean) -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
    callViewModel: com.example.socialapp.ui.call.CallViewModel = hiltViewModel()
) {
    val conversations by viewModel.conversations.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val incomingCall by callViewModel.currentCallSignal.collectAsState()
    var showUsersDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showIncomingCallDialog by remember { mutableStateOf(false) }

    // Lắng nghe cuộc gọi đến khi app foreground
    LaunchedEffect(Unit) {
        // observeIncomingCall được trigger từ CallViewModel nếu cần
    }

    // Khi incomingCall thay đổi sang ringing
    LaunchedEffect(incomingCall) {
        if (incomingCall?.status == "ringing") {
            showIncomingCallDialog = true
        }
    }

    // Dialog cuộc gọi đến (foreground)
    if (showIncomingCallDialog && incomingCall != null) {
        val signal = incomingCall!!
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { /* không cho dismiss bằng cách bấm ngoài */ }
        ) {
            com.example.socialapp.ui.call.IncomingCallScreen(
                callerName = signal.callerName,
                callerAvatar = signal.callerAvatar,
                callType = signal.type,
                onAccept = {
                    showIncomingCallDialog = false
                    callViewModel.onIncomingCall(signal)
                    if (signal.type == "video") {
                        onNavigateToVideoCall(signal.id, signal.callerId, signal.callerName, signal.callerAvatar, true)
                    } else {
                        onNavigateToVoiceCall(signal.id, signal.callerId, signal.callerName, signal.callerAvatar, true)
                    }
                },
                onDecline = {
                    showIncomingCallDialog = false
                    callViewModel.declineCall()
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "SocialApp",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                        currentUser?.let {
                            Text(
                                it.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = LightSkyBlue
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyBlueDark),
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Đăng xuất", tint = LightSkyBlue)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { selectedTab = 1 },
                containerColor = SkyBlueDeep,
                contentColor = White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Tin nhắn mới")
            }
        },
        containerColor = OffWhite
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            // Tab: Tin nhắn / Người dùng
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = NavyBlue,
                contentColor = White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Tin nhắn") },
                    icon = { Icon(Icons.Default.Chat, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Người dùng") },
                    icon = { Icon(Icons.Default.People, contentDescription = null) }
                )
            }

            when (selectedTab) {
                0 -> ConversationList(
                    conversations = conversations,
                    currentUid = viewModel.getCurrentUid(),
                    onConversationClick = { conv ->
                        onNavigateToChat(conv.otherUserId, conv.otherUserName, conv.otherUserAvatar)
                    }
                )
                1 -> UsersList(
                    users = allUsers,
                    onUserClick = { user ->
                        onNavigateToChat(user.uid, user.displayName, user.avatarUrl)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<Conversation>,
    currentUid: String,
    onConversationClick: (Conversation) -> Unit
) {
    if (conversations.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.ChatBubbleOutline, contentDescription = null,
                    modifier = Modifier.size(64.dp), tint = MediumGray)
                Spacer(Modifier.height(12.dp))
                Text("Chưa có tin nhắn nào", color = MediumGray)
                Text("Nhấn ✏️ để bắt đầu trò chuyện", color = MediumGray,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(conversations) { conv ->
                ConversationItem(
                    conversation = conv,
                    currentUid = currentUid,
                    onClick = { onConversationClick(conv) }
                )
            }
        }
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
            .background(White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(NavyBlue),
            contentAlignment = Alignment.Center
        ) {
            if (conversation.otherUserAvatar.isNotBlank()) {
                AsyncImage(
                    model = conversation.otherUserAvatar,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = conversation.otherUserName.firstOrNull()?.uppercase() ?: "?",
                    color = White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = conversation.otherUserName,
                    fontWeight = FontWeight.SemiBold,
                    color = NavyBlueDark,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(text = timeStr, color = MediumGray, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (isMyMessage) "Bạn: ${conversation.lastMessage}" else conversation.lastMessage,
                color = DarkGray,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = LightGray, thickness = 0.5.dp)
}

@Composable
private fun UsersList(
    users: List<User>,
    onUserClick: (User) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(users) { user ->
            UserItem(user = user, onClick = { onUserClick(user) })
        }
    }
}

@Composable
private fun UserItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(NavyBlueLight),
            contentAlignment = Alignment.Center
        ) {
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(model = user.avatarUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    text = user.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = White, fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(user.displayName, fontWeight = FontWeight.SemiBold, color = NavyBlueDark)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (user.status == "online") OnlineGreen else MediumGray)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (user.status == "online") "Đang hoạt động" else "Ngoại tuyến",
                    color = MediumGray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
    HorizontalDivider(color = LightGray, thickness = 0.5.dp)
}

