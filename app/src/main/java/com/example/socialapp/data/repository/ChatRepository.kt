package com.example.socialapp.data.repository

import com.example.socialapp.data.model.Conversation
import com.example.socialapp.data.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUid get() = auth.currentUser?.uid ?: ""

    /**
     * Tạo conversationId chuẩn từ 2 uid (idempotent).
     * Sắp xếp alphabetical để 2 user luôn ra cùng 1 ID.
     */
    fun conversationId(otherUid: String): String =
        listOf(currentUid, otherUid).sorted().joinToString("_")

    /** Gửi tin nhắn văn bản */
    suspend fun sendMessage(otherUid: String, text: String) {
        val convId = conversationId(otherUid)
        val msgRef = firestore
            .collection("conversations").document(convId)
            .collection("messages").document()

        val message = hashMapOf(
            "senderId" to currentUid,
            "text" to text,
            "type" to "text",
            "sentAt" to FieldValue.serverTimestamp()
        )

        firestore.runBatch { batch ->
            batch.set(msgRef, message)
            batch.set(
                firestore.collection("conversations").document(convId),
                hashMapOf(
                    "participants" to listOf(currentUid, otherUid),
                    "lastMessage" to text,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "lastSenderId" to currentUid,
                    "isRead" to false // Đánh dấu chưa đọc
                ),
                SetOptions.merge()
            )
        }.await()
    }

    /** Lắng nghe tin nhắn realtime — trả về Flow<List<Message>> */
    fun observeMessages(otherUid: String): Flow<List<Message>> = callbackFlow {
        val convId = conversationId(otherUid)
        val listener = firestore
            .collection("conversations").document(convId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                
                // Mỗi khi có tin nhắn mới trong khi đang mở màn hình chat, đánh dấu đã đọc
                if (messages.isNotEmpty() && messages.last().senderId != currentUid) {
                    firestore.collection("conversations").document(convId).update("isRead", true)
                }
                
                trySend(messages)
            }
        
        // Đánh dấu đã đọc ngay khi vừa mở chat
        markConversationRead(convId)
        
        awaitClose { listener.remove() }
    }

    /** Đánh dấu cuộc hội thoại đã đọc */
    suspend fun markConversationRead(convId: String) {
        try {
            firestore.collection("conversations").document(convId)
                .update("isRead", true).await()
        } catch (_: Exception) {}
    }

    /** Lắng nghe danh sách conversations của user hiện tại */
    fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore
            .collection("conversations")
            .whereArrayContains("participants", currentUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val conversations = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Conversation::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(conversations)
            }
        awaitClose { listener.remove() }
    }

    /** Đánh dấu tin nhắn đã đọc */
    suspend fun markAsRead(otherUid: String, messageId: String) {
        val convId = conversationId(otherUid)
        try {
            firestore.collection("conversations").document(convId)
                .collection("messages").document(messageId)
                .update("readAt", FieldValue.serverTimestamp()).await()
            
            markConversationRead(convId)
        } catch (_: Exception) {}
    }
}

