package com.example.socialapp.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.socialapp.ui.calls.CallsScreen
import com.example.socialapp.ui.contacts.ContactsScreen
import com.example.socialapp.ui.home.HomeScreen
import com.example.socialapp.ui.settings.SettingsScreen
import com.example.socialapp.ui.theme.*

private data class BottomNavItem(val label: String, val icon: ImageVector, val route: String)

private val navItems = listOf(
    BottomNavItem("Message",  Icons.Default.ChatBubbleOutline, "messages"),
    BottomNavItem("Calls",    Icons.Default.Call,              "calls"),
    BottomNavItem("Contacts", Icons.Default.Person,            "contacts"),
    BottomNavItem("Settings", Icons.Default.Settings,          "settings")
)

@Composable
fun MainScreen(
    onNavigateToChat: (uid: String, name: String, avatar: String) -> Unit,
    onNavigateToVoiceCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String, isCallee: Boolean) -> Unit,
    onNavigateToVideoCall: (callId: String, calleeId: String, calleeName: String, calleeAvatar: String, isCallee: Boolean) -> Unit,
    onNavigateToUserProfile: (uid: String, name: String, avatar: String) -> Unit,
    onLogout: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = DarkBg,
        bottomBar = {
            NavigationBar(
                containerColor = DarkCard,
                tonalElevation = 0.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedIndex == index,
                        onClick = { selectedIndex = index },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = ChatboxTealAccent,
                            selectedTextColor = ChatboxTealAccent,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = DarkSurface
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedIndex) {
                0 -> HomeScreen(
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToVoiceCall = onNavigateToVoiceCall,
                    onNavigateToVideoCall = onNavigateToVideoCall
                )
                1 -> CallsScreen(
                    onNavigateToVoiceCall = { calleeId, calleeName, calleeAvatar ->
                        onNavigateToVoiceCall("new", calleeId, calleeName, calleeAvatar, false)
                    },
                    onNavigateToVideoCall = { calleeId, calleeName, calleeAvatar ->
                        onNavigateToVideoCall("new", calleeId, calleeName, calleeAvatar, false)
                    }
                )
                2 -> ContactsScreen(
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToUserProfile = onNavigateToUserProfile
                )
                3 -> SettingsScreen(onLogout = onLogout)
            }
        }
    }
}
