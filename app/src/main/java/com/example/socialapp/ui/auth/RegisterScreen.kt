package com.example.socialapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.socialapp.ui.theme.*

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onRegisterSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 28.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            IconButton(onClick = onNavigateToLogin) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
            }

            Spacer(Modifier.height(24.dp))

            // Title
            Text(
                buildAnnotatedString {
                    append("Sign up with ")
                    withStyle(SpanStyle(color = ChatboxTealAccent, fontWeight = FontWeight.Bold)) {
                        append("Email")
                    }
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Get chatting with friends and family today by\nsigning up for our chat app!",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(36.dp))

            // Fields
            Text("Your name", color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            AuthTextField(value = name, onValueChange = { name = it }, placeholder = "Enter your name")

            Spacer(Modifier.height(20.dp))

            Text("Your email", color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            AuthTextField(value = email, onValueChange = { email = it },
                keyboardType = KeyboardType.Email, placeholder = "Enter your email")

            Spacer(Modifier.height(20.dp))

            Text("Password", color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            AuthTextField(value = password, onValueChange = { password = it },
                keyboardType = KeyboardType.Password, isPassword = true, placeholder = "Create password")

            Spacer(Modifier.height(20.dp))

            Text("Confirm Password", color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            AuthTextField(value = confirmPassword, onValueChange = { confirmPassword = it },
                keyboardType = KeyboardType.Password, isPassword = true, placeholder = "Confirm your password")

            Spacer(Modifier.weight(1f))

            if (uiState is AuthUiState.Error) {
                Text(
                    (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = {
                    if (password != confirmPassword) return@Button
                    viewModel.registerWithEmail(email, password, name)
                },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurface,
                    contentColor = White
                )
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ChatboxTealAccent, strokeWidth = 2.dp)
                } else {
                    Text("Create an account", fontWeight = FontWeight.SemiBold, color = White)
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = onNavigateToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary)) { append("Already have an account? ") }
                        withStyle(SpanStyle(color = ChatboxTealAccent, fontWeight = FontWeight.Bold)) { append("Log in") }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
