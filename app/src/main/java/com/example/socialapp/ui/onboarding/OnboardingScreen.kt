package com.example.socialapp.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.socialapp.ui.theme.*

@Composable
fun OnboardingScreen(
    onSignUpWithEmail: () -> Unit,
    onLogin: () -> Unit
) {
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
            // Logo
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    text = "C",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Chatbox",
                    style = MaterialTheme.typography.titleMedium,
                    color = White,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(36.dp))

            // Headline
            Text(
                text = "Connect\nfriends\neasily &\nquickly",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = White,
                lineHeight = 54.sp
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Our chat app is the perfect way to stay\nconnected with friends and family.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 22.sp
            )

            Spacer(Modifier.weight(1f))



            // Sign up button
            Button(
                onClick = onSignUpWithEmail,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White,
                    contentColor = DarkBg
                )
            ) {
                Text(
                    "Sign up with mail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(20.dp))

            // Login link
            TextButton(
                onClick = onLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary)) { append("Existing account? ") }
                        withStyle(SpanStyle(color = White, fontWeight = FontWeight.Bold)) { append("Log in") }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}


