package com.example.socialapp.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.socialapp.ui.theme.*

@Composable
fun AccountDetailsScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = hiltViewModel()
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val message by viewModel.message.collectAsState()
    
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    // Load initial user data on first render
    LaunchedEffect(Unit) {
        viewModel.loadUser()
    }

    var editMode by remember { mutableStateOf(false) }
    var displayName by remember(currentUser) { mutableStateOf(currentUser?.displayName ?: "") }

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
                    "Account Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = White,
                    modifier = Modifier.weight(1f)
                )
                if (editMode) {
                    TextButton(onClick = { 
                        viewModel.updateDisplayName(displayName)
                        editMode = false 
                    }) {
                        Text("Save", color = White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { editMode = true }) {
                        Text("Edit", color = White)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkCard)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(ChatboxTeal),
                contentAlignment = Alignment.Center
            ) {
                if (currentUser?.avatarUrl?.isNotBlank() == true) {
                    AsyncImage(model = currentUser!!.avatarUrl, contentDescription = null,
                        modifier = Modifier.fillMaxSize())
                } else {
                    Text(
                        currentUser?.displayName?.firstOrNull()?.uppercase() ?: "?",
                        color = White, fontWeight = FontWeight.Bold, fontSize = 36.sp
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            // Name Field
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text("Display Name") },
                enabled = editMode,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ChatboxTealAccent,
                    unfocusedBorderColor = DarkDivider,
                    disabledBorderColor = DarkDivider,
                    disabledTextColor = TextPrimary,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(16.dp))
            
            // Email Field
            OutlinedTextField(
                value = currentUser?.email ?: "",
                onValueChange = {},
                label = { Text("Email") },
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    disabledBorderColor = DarkDivider,
                    disabledTextColor = TextSecondary,
                    disabledLabelColor = TextSecondary
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.resetPassword() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ChatboxTeal),
                shape = RoundedCornerShape(25.dp)
            ) {
                Text("Send Reset Password Email", color = White, fontWeight = FontWeight.SemiBold)
            }
            
            Spacer(Modifier.height(8.dp))
            Text(
                "A password reset link will be sent to your email address.",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
