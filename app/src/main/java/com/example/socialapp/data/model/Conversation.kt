package com.example.socialapp.data.model

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String = "",
    val isRead: Boolean = true, // Thêm trường này
    // Runtime only — populated from /users/
    val otherUserName: String = "",
    val otherUserAvatar: String = "",
    val otherUserId: String = ""
)

