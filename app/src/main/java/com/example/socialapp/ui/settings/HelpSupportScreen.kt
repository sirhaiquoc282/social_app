package com.example.socialapp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.socialapp.ui.theme.*

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
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
                    "Help & Support",
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
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "Frequently Asked Questions (FAQ)",
                color = ChatboxTealAccent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            FaqItem("How do I change my display name?", "Go to Settings > Account Details to update your display name.")
            FaqItem("How can I reset my password?", "Go to Settings > Account Details and click on 'Send Reset Password Email'.")
            FaqItem("Why are my calls failing?", "Ensure you have granted Camera and Microphone permissions and have a stable internet connection.")
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = DarkDivider)
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Contact Us",
                color = ChatboxTealAccent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "If you need further assistance, please contact our support team at:",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "support@chatbox.com",
                color = White,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = DarkDivider)
            Spacer(Modifier.height(24.dp))
            
            Text(
                "Privacy Policy",
                color = ChatboxTealAccent,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your privacy is important to us. We do not share your personal information or chat data with third parties. All calls are peer-to-peer and messages are securely stored.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(question, color = TextPrimary, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Text(answer, color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
    }
}
