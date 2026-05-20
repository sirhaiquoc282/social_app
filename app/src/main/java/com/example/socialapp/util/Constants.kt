package com.example.socialapp.util

object Constants {
    const val COLLECTION_USERS = "users"
    const val COLLECTION_CONVERSATIONS = "conversations"
    const val COLLECTION_MESSAGES = "messages"
    const val COLLECTION_CALLS = "calls"

    const val CALL_TYPE_VOICE = "voice"
    const val CALL_TYPE_VIDEO = "video"

    const val CALL_STATUS_RINGING  = "ringing"
    const val CALL_STATUS_ACCEPTED = "accepted"
    const val CALL_STATUS_DECLINED = "declined"
    const val CALL_STATUS_ENDED    = "ended"
    const val CALL_STATUS_MISSED   = "missed"

    const val USER_STATUS_ONLINE  = "online"
    const val USER_STATUS_OFFLINE = "offline"

    const val CALL_TIMEOUT_MS = 30_000L  // 30 giây
}

