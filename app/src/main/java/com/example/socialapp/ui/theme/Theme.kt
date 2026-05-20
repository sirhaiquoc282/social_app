package com.example.socialapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary          = NavyBlue,
    onPrimary        = White,
    primaryContainer = NavyBlueLight,
    onPrimaryContainer = LightSkyBlue,
    secondary        = SkyBlue,
    onSecondary      = White,
    secondaryContainer = LightSkyBlue,
    onSecondaryContainer = NavyBlueDark,
    background       = OffWhite,
    onBackground     = NavyBlueDark,
    surface          = White,
    onSurface        = NavyBlueDark,
    surfaceVariant   = LightGray,
    onSurfaceVariant = DarkGray,
    outline          = MediumGray,
    error            = ErrorRed,
    onError          = White
)

private val DarkColorScheme = darkColorScheme(
    primary          = LightSkyBlue,
    onPrimary        = NavyBlueDark,
    primaryContainer = NavyBlue,
    onPrimaryContainer = LightSkyBlue,
    secondary        = SkyBlue,
    onSecondary      = NavyBlueDark,
    secondaryContainer = NavyBlue,
    onSecondaryContainer = LightSkyBlue,
    background       = CallBgDark,
    onBackground     = White,
    surface          = NavyBlueDark,
    onSurface        = White,
    surfaceVariant   = NavyBlue,
    onSurfaceVariant = LightSkyBlue,
    outline          = DarkGray,
    error            = ErrorRed,
    onError          = White
)

@Composable
fun SocialAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyBlueDark.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}

