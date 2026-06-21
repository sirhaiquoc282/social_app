package com.example.socialapp.data.model

import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String = "",
    val isRead: Boolean = true, // Deprecated: giữ lại để backward compat
    val readBy: Map<String, Boolean> = emptyMap(), // Per-user read tracking: {uid: true/false}
    // Runtime only — populated from /users/
    val otherUserName: String = "",
    val otherUserAvatar: String = "",
    val otherUserId: String = ""
) {
    /**
     * Kiểm tra xem cuộc trò chuyện đã được đọc bởi user cụ thể chưa.
     * Nếu readBy map rỗng (dữ liệu cũ), fall back sang isRead field.
     */
    fun isReadBy(uid: String): Boolean {
        if (readBy.isEmpty()) return isRead
        return readBy[uid] ?: true // Mặc định là đã đọc nếu chưa có trong map
    }
}
