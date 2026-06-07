package com.example.socialapp.ui.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object Routes {
    const val SPLASH        = "splash"
    const val ONBOARDING    = "onboarding"
    const val LOGIN         = "login"
    const val REGISTER      = "register"
    const val MAIN          = "main"   // bottom-nav host

    const val CHAT          = "chat/{otherUid}/{otherName}/{otherAvatar}"
    const val USER_PROFILE  = "user_profile/{uid}/{name}/{avatar}"
    const val VOICE_CALL    = "voice_call/{callId}/{calleeId}/{calleeName}/{calleeAvatar}/{isCallee}"
    const val VIDEO_CALL    = "video_call/{callId}/{calleeId}/{calleeName}/{calleeAvatar}/{isCallee}"

    private fun encode(s: String): String =
        URLEncoder.encode(s.ifBlank { "none" }, StandardCharsets.UTF_8.toString())

    fun chatRoute(otherUid: String, otherName: String, otherAvatar: String) =
        "chat/${encode(otherUid)}/${encode(otherName)}/${encode(otherAvatar)}"

    fun userProfileRoute(uid: String, name: String, avatar: String) =
        "user_profile/${encode(uid)}/${encode(name)}/${encode(avatar)}"

    fun voiceCallRoute(
        callId: String, calleeId: String, calleeName: String,
        calleeAvatar: String, isCallee: Boolean
    ) = "voice_call/${encode(callId)}/${encode(calleeId)}/${encode(calleeName)}/${encode(calleeAvatar)}/$isCallee"

    fun videoCallRoute(
        callId: String, calleeId: String, calleeName: String,
        calleeAvatar: String, isCallee: Boolean
    ) = "video_call/${encode(callId)}/${encode(calleeId)}/${encode(calleeName)}/${encode(calleeAvatar)}/$isCallee"
}
