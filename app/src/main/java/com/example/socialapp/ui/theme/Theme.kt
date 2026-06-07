package com.example.socialapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val AppColorScheme = darkColorScheme(
    primary                = ChatboxTeal,
    onPrimary              = White,
    primaryContainer       = ChatboxTealDark,
    onPrimaryContainer     = White,
    secondary              = ChatboxTealAccent,
    onSecondary            = White,
    secondaryContainer     = DarkCard,
    onSecondaryContainer   = TextPrimary,
    background             = DarkBg,
    onBackground           = TextPrimary,
    surface                = DarkCard,
    onSurface              = TextPrimary,
    surfaceVariant         = DarkSurface,
    onSurfaceVariant       = TextSecondary,
    outline                = DarkDivider,
    error                  = ErrorRed,
    onError                = White
)

@Composable
fun SocialAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = ChatboxTeal.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography  = Typography,
        content     = content
    )
}
