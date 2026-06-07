package com.example.socialapp.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socialapp.ui.theme.*

@Composable
fun UserProfileScreen(
    uid: String,
    name: String,
    avatar: String,
    email: String = "",
    onBack: () -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToVoiceCall: () -> Unit,
    onNavigateToVideoCall: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(DarkBg)) {

        // Teal header section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ChatboxTeal)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Back button
                Row(modifier = Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp)) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = White)
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(ChatboxTealDark),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatar.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = avatar, contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(name.firstOrNull()?.uppercase() ?: "?",
                            color = White, fontWeight = FontWeight.Bold, fontSize = 36.sp)
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(name, color = White, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "@${name.lowercase().replace(" ", "")}",
                    color = White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(20.dp))

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    ProfileAction(Icons.Default.ChatBubbleOutline, "Chat", onClick = onNavigateToChat)
                    ProfileAction(Icons.Default.Videocam, "Video", onClick = onNavigateToVideoCall)
                    ProfileAction(Icons.Default.Call, "Call", onClick = onNavigateToVoiceCall)
                    ProfileAction(Icons.Default.MoreHoriz, "More", onClick = {})
                }
            }
        }

        // Details card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            ProfileDetail("Display Name", name)
            Spacer(Modifier.height(16.dp))
            ProfileDetail("Email Address", email.ifBlank { "—" })
            Spacer(Modifier.height(16.dp))
            ProfileDetail("Status", if (true) "Online" else "Offline")

            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Media Shared", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = {}) {
                    Text("View All", color = ChatboxTealAccent, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Media grid placeholder
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Image, null, tint = TextSecondary,
                            modifier = Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(White.copy(alpha = 0.15f))
        ) {
            Icon(icon, null, tint = White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ProfileDetail(label: String, value: String) {
    Column {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.bodyLarge)
    }
}
