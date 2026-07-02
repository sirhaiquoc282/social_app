package com.example.socialapp.ui.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.socialapp.ui.auth.AuthViewModel
import com.example.socialapp.ui.auth.LoginScreen
import com.example.socialapp.ui.auth.RegisterScreen
import com.example.socialapp.ui.call.CallState
import com.example.socialapp.ui.call.CallViewModel
import com.example.socialapp.ui.call.video.VideoCallScreen
import com.example.socialapp.ui.call.voice.VoiceCallScreen
import com.example.socialapp.ui.chat.ChatScreen
import com.example.socialapp.ui.main.MainScreen
import com.example.socialapp.ui.onboarding.OnboardingScreen
import com.example.socialapp.ui.profile.UserProfileScreen
import com.example.socialapp.ui.settings.AccountDetailsScreen
import com.example.socialapp.ui.settings.HelpSupportScreen
import com.example.socialapp.ui.settings.NotificationsScreen
import com.example.socialapp.ui.splash.SplashScreen
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun SocialAppNavGraph() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    // CallViewModel được scope theo Activity → listener sống cùng Activity,
    // không bị hủy khi chuyển tab hoặc navigate giữa các màn hình
    val activity = LocalContext.current as ComponentActivity
    val callViewModel: CallViewModel = hiltViewModel(activity)

    fun decode(s: String?): String =
        URLDecoder.decode(s ?: "none", StandardCharsets.UTF_8.toString())
            .let { if (it == "none") "" else it }

    // ═══ INCOMING CALL OVERLAY (hiện trên MỌI màn hình) ═══
    val incomingCall by callViewModel.currentCallSignal.collectAsState()
    val callState by callViewModel.callState.collectAsState()
    val context = LocalContext.current

    val shouldShowDialog = incomingCall != null
            && incomingCall?.status == "ringing"
            && callState is CallState.Ringing

    LaunchedEffect(callState) {
        when (callState) {
            is CallState.Ended, is CallState.Declined, is CallState.Idle -> {
                com.example.socialapp.service.MyFirebaseMessagingService.cancelCallNotification(context)
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH
        ) {

        // ── Splash ────────────────────────────────────────────────────────────
        composable(Routes.SPLASH) {
            SplashScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        // ── Onboarding ────────────────────────────────────────────────────────
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onSignUpWithEmail = { navController.navigate(Routes.REGISTER) },
                onLogin = { navController.navigate(Routes.LOGIN) }
            )
        }

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }

        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Routes.MAIN) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        // ── Main (bottom nav) ─────────────────────────────────────────────────
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToChat = { uid, name, avatar ->
                    navController.navigate(Routes.chatRoute(uid, name, avatar))
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
                onNavigateToUserProfile = { uid, name, avatar ->
                    navController.navigate(Routes.userProfileRoute(uid, name, avatar))
                },
                onNavigateToAccountDetails = { navController.navigate(Routes.ACCOUNT_DETAILS) },
                onNavigateToNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onNavigateToHelpSupport = { navController.navigate(Routes.HELP_SUPPORT) },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.MAIN) { inclusive = true }
                    }
                },
                callViewModel = callViewModel
            )
        }

        // ── Settings Sub-Screens ──────────────────────────────────────────────
        composable(Routes.ACCOUNT_DETAILS) {
            AccountDetailsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.HELP_SUPPORT) {
            HelpSupportScreen(onBack = { navController.popBackStack() })
        }

        // ── User Profile ──────────────────────────────────────────────────────
        composable(
            route = Routes.USER_PROFILE,
            arguments = listOf(
                navArgument("uid") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType },
                navArgument("avatar") { type = NavType.StringType }
            )
        ) { backStack ->
            val uid = decode(backStack.arguments?.getString("uid"))
            val name = decode(backStack.arguments?.getString("name"))
            val avatar = decode(backStack.arguments?.getString("avatar"))
            UserProfileScreen(
                uid = uid,
                name = name,
                avatar = avatar,
                onBack = { navController.popBackStack() },
                onNavigateToChat = {
                    navController.navigate(Routes.chatRoute(uid, name, avatar)) {
                        popUpTo(Routes.USER_PROFILE) { inclusive = true }
                    }
                },
                onNavigateToVoiceCall = {
                    navController.navigate(Routes.voiceCallRoute("new", uid, name, avatar, false))
                },
                onNavigateToVideoCall = {
                    navController.navigate(Routes.videoCallRoute("new", uid, name, avatar, false))
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
            VoiceCallScreen(
                callId = decode(backStack.arguments?.getString("callId")),
                calleeId = decode(backStack.arguments?.getString("calleeId")),
                calleeName = decode(backStack.arguments?.getString("calleeName")),
                calleeAvatar = decode(backStack.arguments?.getString("calleeAvatar")),
                isCallee = backStack.arguments?.getBoolean("isCallee") ?: false,
                onCallEnded = { navController.popBackStack() },
                viewModel = callViewModel
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
            VideoCallScreen(
                callId = decode(backStack.arguments?.getString("callId")),
                calleeId = decode(backStack.arguments?.getString("calleeId")),
                calleeName = decode(backStack.arguments?.getString("calleeName")),
                calleeAvatar = decode(backStack.arguments?.getString("calleeAvatar")),
                isCallee = backStack.arguments?.getBoolean("isCallee") ?: false,
                onCallEnded = { navController.popBackStack() },
                viewModel = callViewModel
            )
        }
        } // end NavHost

        // ═══ INCOMING CALL DIALOG OVERLAY ═══
        // Hiển thị ĐÈ LÊN trên mọi màn hình (Chat, Profile, Settings, v.v.)
        if (shouldShowDialog) {
            val signal = incomingCall!!
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {}
            ) {
                com.example.socialapp.ui.call.IncomingCallScreen(
                    callerName = signal.callerName,
                    callerAvatar = signal.callerAvatar,
                    callType = signal.type,
                    onAccept = {
                        callViewModel.onIncomingCall(signal)
                        callViewModel.acceptCall(context)
                        if (signal.type == "video")
                            navController.navigate(
                                Routes.videoCallRoute(signal.id, signal.callerId, signal.callerName, signal.callerAvatar, true)
                            )
                        else
                            navController.navigate(
                                Routes.voiceCallRoute(signal.id, signal.callerId, signal.callerName, signal.callerAvatar, true)
                            )
                    },
                    onDecline = {
                        callViewModel.declineCall()
                    }
                )
            }
        }
    } // end Box
}

