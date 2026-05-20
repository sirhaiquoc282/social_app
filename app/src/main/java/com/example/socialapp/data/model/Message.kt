package com.example.socialapp.data.model

import com.google.firebase.Timestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val type: String = "text",   // "text" | "image"
    val sentAt: Timestamp? = null,
    val readAt: Timestamp? = null
)

