package com.example.socialapp.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val fcmToken: String = "",
    val status: String = "offline",
    val createdAt: Timestamp? = null
)

