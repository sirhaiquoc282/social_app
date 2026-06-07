package com.example.socialapp.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.socialapp.ui.theme.*

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onLoginSuccess()
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

            // Back arrow placeholder (full-screen login no back needed)
            Spacer(Modifier.height(32.dp))

            // Title
            Text(
                buildAnnotatedString {
                    append("Log in to ")
                    withStyle(SpanStyle(
                        color = ChatboxTealAccent,
                        fontWeight = FontWeight.Bold
                    )) { append("Chatbox") }
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Welcome back! Sign in using your social\naccount or email to continue us",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Start,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(32.dp))

            // Social buttons row (UI only)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                SocialCircle("f", Color(0xFF1877F2))
                Spacer(Modifier.width(20.dp))
                SocialCircle("G", White, Color(0xFF444444))
                Spacer(Modifier.width(20.dp))
                SocialCircle("", Color(0xFF111111))
            }

            Spacer(Modifier.height(28.dp))

            // OR divider
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkDivider)
                Text("  OR  ", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkDivider)
            }

            Spacer(Modifier.height(28.dp))

            // Email field
            Text("Your email", color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            AuthTextField(
                value = email,
                onValueChange = { email = it },
                keyboardType = KeyboardType.Email,
                placeholder = "Enter your email"
            )

            Spacer(Modifier.height(24.dp))

            // Password field
            Text("Password", color = ChatboxTealAccent, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            AuthTextField(
                value = password,
                onValueChange = { password = it },
                keyboardType = KeyboardType.Password,
                isPassword = true,
                placeholder = "Enter your password"
            )

            Spacer(Modifier.weight(1f))

            // Error
            if (uiState is AuthUiState.Error) {
                Text(
                    (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Login button
            Button(
                onClick = { viewModel.loginWithEmail(email, password) },
                enabled = uiState !is AuthUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurface,
                    contentColor = TextSecondary
                )
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ChatboxTealAccent, strokeWidth = 2.dp)
                } else {
                    Text("Log in", fontWeight = FontWeight.SemiBold, color = White)
                }
            }

            Spacer(Modifier.height(16.dp))

            TextButton(
                onClick = { /* TODO: forgot password */ },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Forgot password?", color = ChatboxTealAccent)
            }

            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onNavigateToRegister,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary)) { append("Don't have an account? ") }
                        withStyle(SpanStyle(color = ChatboxTealAccent, fontWeight = FontWeight.Bold)) { append("Sign up") }
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
internal fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    placeholder: String = ""
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = TextHint) },
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = White,
            unfocusedTextColor = White,
            focusedIndicatorColor = DarkDivider,
            unfocusedIndicatorColor = DarkDivider,
            cursorColor = ChatboxTealAccent
        )
    )
}

@Composable
private fun SocialCircle(label: String, bg: Color, textColor: Color = White) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .border(1.dp, DarkDivider, CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
