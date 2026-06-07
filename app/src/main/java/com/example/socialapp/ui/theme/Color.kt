package com.example.socialapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── Primary – Teal (header, appbar, status row) ─────────────────────────────
val ChatboxTeal        = Color(0xFF2D7B6B)   // chủ đạo: header / top bar
val ChatboxTealDark    = Color(0xFF1E5C4F)   // tối hơn
val ChatboxTealAccent  = Color(0xFF3CB89A)   // nhạt hơn / icon active

// ── Dark backgrounds ──────────────────────────────────────────────────────────
val DarkBg             = Color(0xFF0E0E13)   // nền toàn màn hình
val DarkCard           = Color(0xFF1A1A22)   // card / list item
val DarkSurface        = Color(0xFF25252F)   // surface nổi (input bar, modal)
val DarkDivider        = Color(0xFF2A2A35)   // đường kẻ phân cách

// ── Text ──────────────────────────────────────────────────────────────────────
val TextPrimary        = Color(0xFFFFFFFF)
val TextSecondary      = Color(0xFF9BA3B5)
val TextHint           = Color(0xFF6B7280)

// ── Chat bubbles ──────────────────────────────────────────────────────────────
val BubbleSent         = Color(0xFF2D7B6B)   // tin nhắn gửi – teal
val BubbleReceived     = Color(0xFF1E1E2A)   // tin nhắn nhận – dark

// ── Status ────────────────────────────────────────────────────────────────────
val OnlineGreen        = Color(0xFF4CAF50)
val MissedRed          = Color(0xFFE53935)
val OutgoingTeal       = Color(0xFF26A69A)

// ── Call screen ───────────────────────────────────────────────────────────────
val CallBgDark         = Color(0xFF0A1628)
val EndCallRed         = Color(0xFFE53935)
val AcceptCallGreen    = Color(0xFF43A047)
val MuteButtonColor    = Color(0xFF37474F)

// ── Kept for backward compat (call screens still reference these) ─────────────
val White              = Color(0xFFFFFFFF)
val LightSkyBlue       = ChatboxTealAccent
val NavyBlue           = Color(0xFF1B2A6B)
val NavyBlueDark       = Color(0xFF0D1B4A)
val NavyBlueLight      = Color(0xFF2E3F8F)
val OffWhite           = Color(0xFFF0F4FF)
val LightGray          = Color(0xFFE8EDF5)
val MediumGray         = Color(0xFF9BA4B5)
val DarkGray           = Color(0xFF3D4563)
val SkyBlue            = Color(0xFF5BA4CF)
val SkyBlueDeep        = Color(0xFF2196F3)
val WarningAmber       = Color(0xFFFF8F00)
val ErrorRed           = Color(0xFFE53935)
