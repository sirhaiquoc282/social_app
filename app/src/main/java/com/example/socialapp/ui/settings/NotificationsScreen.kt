package com.example.socialapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.socialapp.ui.theme.*

@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val isPushOn by viewModel.isPushNotificationOn.collectAsState()
    val isSoundOn by viewModel.isSoundOn.collectAsState()
    val isVibrateOn by viewModel.isVibrateOn.collectAsState()

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
                    .padding(horizontal = 4.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                }
                Text(
                    "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCard)
                .padding(16.dp)
        ) {
            NotificationToggleItem(
                title = "Push Notifications",
                description = "Receive notifications for messages, calls, and other activities.",
                isChecked = isPushOn,
                onCheckedChange = { viewModel.setPushNotifications(it) }
            )
            
            HorizontalDivider(color = DarkDivider, modifier = Modifier.padding(vertical = 12.dp))
            
            NotificationToggleItem(
                title = "Sound",
                description = "Play sound for incoming notifications.",
                isChecked = isSoundOn,
                onCheckedChange = { viewModel.setSound(it) },
                enabled = isPushOn
            )
            
            HorizontalDivider(color = DarkDivider, modifier = Modifier.padding(vertical = 12.dp))
            
            NotificationToggleItem(
                title = "Vibrate",
                description = "Vibrate for incoming notifications.",
                isChecked = isVibrateOn,
                onCheckedChange = { viewModel.setVibrate(it) },
                enabled = isPushOn
            )
        }
    }
}

@Composable
private fun NotificationToggleItem(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (enabled) TextPrimary else TextSecondary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = description,
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
