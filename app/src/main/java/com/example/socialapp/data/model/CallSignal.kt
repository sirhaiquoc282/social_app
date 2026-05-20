package com.example.socialapp.data.model

import com.google.firebase.Timestamp

data class CallSignal(
    val id: String = "",
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val channelName: String = "",
    val type: String = "voice",     // "voice" | "video"
    val status: String = "ringing", // "ringing" | "accepted" | "declined" | "ended" | "missed"
    val createdAt: Timestamp? = null,
    val answeredAt: Timestamp? = null,
    val endedAt: Timestamp? = null
)

