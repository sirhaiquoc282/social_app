package com.example.socialapp.ui.contacts

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.data.model.User
import com.example.socialapp.ui.home.HomeViewModel
import com.example.socialapp.ui.theme.*

@Composable
fun ContactsScreen(
    onNavigateToChat: (uid: String, name: String, avatar: String) -> Unit,
    onNavigateToUserProfile: (uid: String, name: String, avatar: String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val users by viewModel.allUsers.collectAsState()

    // Group by first letter
    val grouped = users
        .sortedBy { it.displayName }
        .groupBy { it.displayName.firstOrNull()?.uppercaseChar() ?: '#' }

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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, null, tint = White)
                }
                Text("Contacts", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = White)
                IconButton(onClick = {}) {
                    Icon(Icons.Default.PersonAdd, null, tint = White)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCard),
            contentPadding = PaddingValues(top = 8.dp)
        ) {
            item {
                Text(
                    "My Contact",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            grouped.forEach { (letter, group) ->
                item {
                    Text(
                        letter.toString(),
                        color = ChatboxTealAccent,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                items(group) { user ->
                    ContactItem(
                        user = user,
                        onClick = { onNavigateToUserProfile(user.uid, user.displayName, user.avatarUrl) },
                        onChat = { onNavigateToChat(user.uid, user.displayName, user.avatarUrl) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    user: User,
    onClick: () -> Unit,
    onChat: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
            if (user.avatarUrl.isNotBlank()) {
                AsyncImage(model = user.avatarUrl, contentDescription = null,
                    modifier = Modifier.fillMaxSize())
            } else {
                Text(user.displayName.firstOrNull()?.uppercase() ?: "?",
                    color = White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.displayName, color = TextPrimary, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(3.dp))
            Text(
                user.email.ifBlank { if (user.status == "online") "Active now" else "Offline" },
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1
            )
        }
    }
    HorizontalDivider(color = DarkDivider, thickness = 0.5.dp, modifier = Modifier.padding(start = 82.dp))
}
