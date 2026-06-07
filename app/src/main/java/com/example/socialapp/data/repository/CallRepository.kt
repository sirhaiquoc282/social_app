package com.example.socialapp.data.repository

import com.example.socialapp.data.model.CallSignal
import com.example.socialapp.data.remote.AgoraManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val agoraManager: AgoraManager
) {
    // Đổi tên thành uid để tránh clash JVM với getCurrentUid()
    private val uid get() = auth.currentUser?.uid ?: ""

    fun getCurrentUid(): String = auth.currentUser?.uid ?: ""

    /**
     * Caller: tạo call document và bắt đầu đổ chuông.
     * Returns callId.
     */
    suspend fun startCall(
        calleeId: String,
        callerName: String,
        callerAvatar: String,
        type: String   // "voice" | "video"
    ): String {
        val callId = UUID.randomUUID().toString()
        firestore.collection("calls").document(callId).set(
            hashMapOf(
                "callerId" to uid,
                "calleeId" to calleeId,
                "callerName" to callerName,
                "callerAvatar" to callerAvatar,
                "channelName" to callId,
                "type" to type,
                "status" to "ringing",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        return callId
    }

    /** Callee: chấp nhận cuộc gọi */
    suspend fun acceptCall(callId: String) {
        firestore.collection("calls").document(callId).update(
            mapOf(
                "status" to "accepted",
                "answeredAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Callee: từ chối cuộc gọi */
    suspend fun declineCall(callId: String) {
        firestore.collection("calls").document(callId)
            .update("status", "declined").await()
    }

    /** Kết thúc cuộc gọi — cả 2 bên đều có thể gọi */
    suspend fun endCall(callId: String) {
        agoraManager.leaveChannel()
        try {
            firestore.collection("calls").document(callId).update(
                mapOf(
                    "status" to "ended",
                    "endedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {}
    }

    /** Đánh dấu cuộc gọi bị nhỡ */
    suspend fun markMissed(callId: String) {
        try {
            firestore.collection("calls").document(callId)
                .update("status", "missed").await()
        } catch (_: Exception) {}
    }

    /** Lắng nghe trạng thái 1 cuộc gọi cụ thể */
    fun observeCall(callId: String): Flow<CallSignal?> = callbackFlow {
        val listener = firestore.collection("calls").document(callId)
            .addSnapshotListener { snapshot, _ ->
                val signal = snapshot?.toObject(CallSignal::class.java)?.copy(id = snapshot.id)
                trySend(signal)
            }
        awaitClose { listener.remove() }
    }

    /** Lịch sử cuộc gọi của user hiện tại (cả gọi đi lẫn gọi đến), sắp xếp mới nhất lên trước. */
    fun observeCallHistory(): Flow<List<CallSignal>> = callbackFlow {
        val results = mutableMapOf<String, CallSignal>()

        fun emit() = trySend(results.values.sortedByDescending { it.createdAt }.toList())

        val asCaller = firestore.collection("calls")
            .whereEqualTo("callerId", uid)
            .addSnapshotListener { snap, _ ->
                snap?.documents?.forEach { doc ->
                    doc.toObject(CallSignal::class.java)?.copy(id = doc.id)
                        ?.let { results[doc.id] = it }
                }
                emit()
            }

        val asCallee = firestore.collection("calls")
            .whereEqualTo("calleeId", uid)
            .addSnapshotListener { snap, _ ->
                snap?.documents?.forEach { doc ->
                    doc.toObject(CallSignal::class.java)?.copy(id = doc.id)
                        ?.let { results[doc.id] = it }
                }
                emit()
            }

        awaitClose { asCaller.remove(); asCallee.remove() }
    }

    /**
     * Lắng nghe cuộc gọi đến cho callee đang online.
     * Dùng khi app foreground — FCM xử lý khi background.
     */
    fun observeIncomingCall(): Flow<CallSignal?> = callbackFlow {
        val listener = firestore.collection("calls")
            .whereEqualTo("calleeId", uid)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, _ ->
                val signal = snapshot?.documents?.firstOrNull()?.let { doc ->
                    doc.toObject(CallSignal::class.java)?.copy(id = doc.id)
                }
                trySend(signal)
            }
        awaitClose { listener.remove() }
    }
}

