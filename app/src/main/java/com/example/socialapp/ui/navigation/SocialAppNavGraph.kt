package com.example.socialapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.socialapp.ui.auth.AuthViewModel
import com.example.socialapp.ui.auth.LoginScreen
import com.example.socialapp.ui.auth.RegisterScreen
import com.example.socialapp.ui.call.IncomingCallActivity
import com.example.socialapp.ui.call.voice.VoiceCallScreen
import com.example.socialapp.ui.call.video.VideoCallScreen
import com.example.socialapp.ui.chat.ChatScreen
import com.example.socialapp.ui.home.HomeScreen
import com.example.socialapp.ui.splash.SplashScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun SocialAppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    fun decode(s: String?): String =
        URLDecoder.decode(s ?: "none", StandardCharsets.UTF_8.toString())
            .let { if (it == "none") "" else it }

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToChat = { otherUid, otherName, otherAvatar ->
                    navController.navigate(Routes.chatRoute(otherUid, otherName, otherAvatar))
                },
                onNavigateToVoiceCall = { callId, calleeId, calleeName, calleeAvatar, isCallee ->
                    navController.navigate(
                        Routes.voiceCallRoute(callId, calleeId, calleeName, calleeAvatar, isCallee)
                    )
                },
                onNavigateToVideoCall = { callId, calleeId, calleeName, calleeAvatar, isCallee ->
                    navController.navigate(
                        Routes.videoCallRoute(callId, calleeId, calleeName, calleeAvatar, isCallee)
                    )
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                }
            )
        }

        // ── Chat ──────────────────────────────────────────────────────────────
        composable(
            route = Routes.CHAT,
            arguments = listOf(
                navArgument("otherUid") { type = NavType.StringType },
                navArgument("otherName") { type = NavType.StringType },
                navArgument("otherAvatar") { type = NavType.StringType }
            )
        ) { backStack ->
            val otherUid = decode(backStack.arguments?.getString("otherUid"))
            val otherName = decode(backStack.arguments?.getString("otherName"))
            val otherAvatar = decode(backStack.arguments?.getString("otherAvatar"))
            ChatScreen(
                otherUid = otherUid,
                otherName = otherName,
                otherAvatar = otherAvatar,
                onBack = { navController.popBackStack() },
                onNavigateToVoiceCall = { callId, calleeId, calleeName, calleeAvatar ->
                    navController.navigate(
                        Routes.voiceCallRoute(callId, calleeId, calleeName, calleeAvatar, false)
                    )
                },
                onNavigateToVideoCall = { callId, calleeId, calleeName, calleeAvatar ->
                    navController.navigate(
                        Routes.videoCallRoute(callId, calleeId, calleeName, calleeAvatar, false)
                    )
                }
            )
        }

        // ── Voice Call ────────────────────────────────────────────────────────
        composable(
            route = Routes.VOICE_CALL,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("calleeId") { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("calleeAvatar") { type = NavType.StringType },
                navArgument("isCallee") { type = NavType.BoolType }
            )
        ) { backStack ->
            val callId = decode(backStack.arguments?.getString("callId"))
            val calleeId = decode(backStack.arguments?.getString("calleeId"))
            val calleeName = decode(backStack.arguments?.getString("calleeName"))
            val calleeAvatar = decode(backStack.arguments?.getString("calleeAvatar"))
            val isCallee = backStack.arguments?.getBoolean("isCallee") ?: false
            VoiceCallScreen(
                callId = callId,
                calleeId = calleeId,
                calleeName = calleeName,
                calleeAvatar = calleeAvatar,
                isCallee = isCallee,
                onCallEnded = { navController.popBackStack() }
            )
        }

        // ── Video Call ────────────────────────────────────────────────────────
        composable(
            route = Routes.VIDEO_CALL,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("calleeId") { type = NavType.StringType },
                navArgument("calleeName") { type = NavType.StringType },
                navArgument("calleeAvatar") { type = NavType.StringType },
                navArgument("isCallee") { type = NavType.BoolType }
            )
        ) { backStack ->
            val callId = decode(backStack.arguments?.getString("callId"))
            val calleeId = decode(backStack.arguments?.getString("calleeId"))
            val calleeName = decode(backStack.arguments?.getString("calleeName"))
            val calleeAvatar = decode(backStack.arguments?.getString("calleeAvatar"))
            val isCallee = backStack.arguments?.getBoolean("isCallee") ?: false
            VideoCallScreen(
                callId = callId,
                calleeId = calleeId,
                calleeName = calleeName,
                calleeAvatar = calleeAvatar,
                isCallee = isCallee,
                onCallEnded = { navController.popBackStack() }
            )
        }
    }
}

