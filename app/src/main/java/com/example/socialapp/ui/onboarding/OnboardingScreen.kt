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

            // Social login row (UI only)
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                SocialButton(label = "f", bgColor = Color(0xFF1877F2))
                Spacer(Modifier.width(20.dp))
                SocialButton(label = "G", bgColor = Color(0xFFFFFFFF), textColor = Color(0xFF444444))
                Spacer(Modifier.width(20.dp))
                SocialButton(label = "", bgColor = Color(0xFF000000), isApple = true)
            }

            Spacer(Modifier.height(24.dp))

            // OR divider
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkDivider)
                Text(
                    "  OR  ",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = DarkDivider)
            }

            Spacer(Modifier.height(20.dp))

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = TextSecondary)) { append("Existing account? ") }
                        withStyle(SpanStyle(color = White, fontWeight = FontWeight.Bold)) { append("Log in") }
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            // Wrap in clickable
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        Modifier.padding(bottom = 4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                TextButton(onClick = onLogin) {
                    Text(
                        "Already have an account? Log in",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SocialButton(
    label: String,
    bgColor: Color,
    textColor: Color = White,
    isApple: Boolean = false
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(1.dp, DarkDivider, CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        if (isApple) {
            Text("", color = textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        } else {
            Text(label, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
