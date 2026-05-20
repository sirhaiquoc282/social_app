# Code mẫu — Repository & ViewModel

## 1. ChatRepository

```kotlin
@Singleton
class ChatRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val currentUid get() = auth.currentUser!!.uid

    /** Tạo conversationId chuẩn từ 2 uid (idempotent) */
    fun conversationId(otherUid: String): String =
        listOf(currentUid, otherUid).sorted().joinToString("_")

    /** Gửi tin nhắn */
    suspend fun sendMessage(otherUid: String, text: String) {
        val convId = conversationId(otherUid)
        val msgRef = firestore
            .collection("conversations").document(convId)
            .collection("messages").document()

        val message = hashMapOf(
            "senderId" to currentUid,
            "text"     to text,
            "type"     to "text",
            "sentAt"   to FieldValue.serverTimestamp()
        )

        firestore.runBatch { batch ->
            batch.set(msgRef, message)
            batch.set(
                firestore.collection("conversations").document(convId),
                hashMapOf(
                    "participants"    to listOf(currentUid, otherUid),
                    "lastMessage"     to text,
                    "lastMessageAt"   to FieldValue.serverTimestamp(),
                    "lastSenderId"    to currentUid
                ),
                SetOptions.merge()
            )
        }.await()
    }

    /** Lắng nghe tin nhắn realtime — trả về Flow */
    fun observeMessages(otherUid: String): Flow<List<Message>> = callbackFlow {
        val convId = conversationId(otherUid)
        val listener = firestore
            .collection("conversations").document(convId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    /** Danh sách conversations của user hiện tại */
    fun observeConversations(): Flow<List<Conversation>> = callbackFlow {
        val listener = firestore
            .collection("conversations")
            .whereArrayContains("participants", currentUid)
            .orderBy("lastMessageAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                trySend(snapshot?.toObjects(Conversation::class.java) ?: emptyList())
            }
        awaitClose { listener.remove() }
    }
}
```

---

## 2. CallRepository

```kotlin
@Singleton
class CallRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val agoraManager: AgoraManager
) {
    private val currentUid get() = auth.currentUser!!.uid

    /** Caller: tạo call document và bắt đầu đổ chuông */
    suspend fun startCall(
        calleeId: String,
        calleeFcmToken: String,
        type: String           // "voice" | "video"
    ): String {
        val callId = UUID.randomUUID().toString()
        firestore.collection("calls").document(callId).set(
            hashMapOf(
                "callerId"    to currentUid,
                "calleeId"    to calleeId,
                "callerName"  to (auth.currentUser?.displayName ?: "Unknown"),
                "channelName" to callId,
                "type"        to type,
                "status"      to "ringing",
                "createdAt"   to FieldValue.serverTimestamp()
            )
        ).await()

        // Gửi FCM (gọi thẳng từ client — OK cho lab)
        sendCallNotification(calleeFcmToken, callId, type)
        return callId
    }

    /** Callee: chấp nhận cuộc gọi */
    suspend fun acceptCall(callId: String) {
        firestore.collection("calls").document(callId).update(
            mapOf(
                "status"     to "accepted",
                "answeredAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Từ chối */
    suspend fun declineCall(callId: String) {
        firestore.collection("calls").document(callId)
            .update("status", "declined").await()
    }

    /** Kết thúc — cả 2 bên đều có thể gọi */
    suspend fun endCall(callId: String) {
        agoraManager.leaveChannel()
        firestore.collection("calls").document(callId).update(
            mapOf(
                "status"  to "ended",
                "endedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /** Lắng nghe trạng thái call để phản ứng realtime */
    fun observeCall(callId: String): Flow<CallSignal?> = callbackFlow {
        val listener = firestore.collection("calls").document(callId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(CallSignal::class.java))
            }
        awaitClose { listener.remove() }
    }

    /** Lắng nghe cuộc gọi đến (cho callee đang online) */
    fun observeIncomingCall(): Flow<CallSignal?> = callbackFlow {
        val listener = firestore.collection("calls")
            .whereEqualTo("calleeId", currentUid)
            .whereEqualTo("status", "ringing")
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents?.firstOrNull()
                    ?.toObject(CallSignal::class.java))
            }
        awaitClose { listener.remove() }
    }

    // Gửi FCM trực tiếp từ client (chỉ dùng cho lab)
    private suspend fun sendCallNotification(token: String, callId: String, type: String) {
        // Implement HTTP call đến FCM Legacy API hoặc v1 API
        // Hoặc dùng Cloud Function trigger onWrite trên /calls/{callId}
    }
}
```

---

## 3. ChatViewModel

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun loadMessages(otherUid: String) {
        viewModelScope.launch {
            chatRepository.observeMessages(otherUid)
                .catch { _uiState.value = ChatUiState.Error(it.message) }
                .collect { _messages.value = it }
        }
    }

    fun sendMessage(otherUid: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            runCatching { chatRepository.sendMessage(otherUid, text) }
                .onFailure { _uiState.value = ChatUiState.Error(it.message) }
        }
    }
}

sealed class ChatUiState {
    object Idle : ChatUiState()
    data class Error(val message: String?) : ChatUiState()
}
```

---

## 4. CallViewModel

```kotlin
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val agoraManager: AgoraManager
) : ViewModel() {

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    /** Caller khởi tạo call */
    fun startCall(calleeId: String, fcmToken: String, type: String) {
        viewModelScope.launch {
            _callState.value = CallState.Calling
            val callId = callRepository.startCall(calleeId, fcmToken, type)
            observeCallStatus(callId, isCallee = false)
        }
    }

    /** Callee chấp nhận */
    fun acceptCall(callId: String, type: String) {
        viewModelScope.launch {
            callRepository.acceptCall(callId)
            joinAgoraChannel(callId, type)
            _callState.value = CallState.Connected
            observeCallStatus(callId, isCallee = true)
        }
    }

    private fun observeCallStatus(callId: String, isCallee: Boolean) {
        viewModelScope.launch {
            callRepository.observeCall(callId).collect { signal ->
                when (signal?.status) {
                    "accepted" -> {
                        if (!isCallee) {
                            joinAgoraChannel(callId, signal.type)
                            _callState.value = CallState.Connected
                        }
                    }
                    "declined" -> _callState.value = CallState.Declined
                    "ended"    -> {
                        agoraManager.leaveChannel()
                        _callState.value = CallState.Ended
                    }
                    "ringing"  -> _callState.value = CallState.Ringing
                }
            }
        }
    }

    private fun joinAgoraChannel(channelName: String, type: String) {
        val handler = object : IRtcEngineEventHandler() {
            override fun onUserJoined(uid: Int, elapsed: Int) {
                _callState.value = CallState.Connected
            }
            override fun onUserOffline(uid: Int, reason: Int) {
                _callState.value = CallState.Ended
            }
        }
        agoraManager.initEngine(handler)
        if (type == "video") agoraManager.joinVideoChannel(channelName, SurfaceView(/* context */))
        else agoraManager.joinVoiceChannel(channelName)
    }

    fun endCall(callId: String) {
        viewModelScope.launch { callRepository.endCall(callId) }
    }

    fun toggleMic(muted: Boolean)    = agoraManager.muteAudio(muted)
    fun toggleCamera(muted: Boolean) = agoraManager.muteVideo(muted)
    fun switchCamera()               = agoraManager.switchCamera()

    override fun onCleared() {
        super.onCleared()
        agoraManager.destroy()
    }
}

sealed class CallState {
    object Idle      : CallState()
    object Calling   : CallState()   // Caller đang đổ chuông
    object Ringing   : CallState()   // Callee nhìn thấy màn hình incoming
    object Connected : CallState()   // Đang trong cuộc gọi
    object Declined  : CallState()
    object Ended     : CallState()
}
```

---

## 5. Data Models

```kotlin
// User.kt
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val fcmToken: String = "",
    val status: String = "offline"
)

// Message.kt
data class Message(
    val senderId: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val type: String = "text",
    val sentAt: Timestamp? = null,
    val readAt: Timestamp? = null
)

// Conversation.kt
data class Conversation(
    val participants: List<String> = emptyList(),
    val lastMessage: String = "",
    val lastMessageAt: Timestamp? = null,
    val lastSenderId: String = ""
)

// CallSignal.kt
data class CallSignal(
    val callerId: String = "",
    val calleeId: String = "",
    val callerName: String = "",
    val callerAvatar: String = "",
    val channelName: String = "",
    val type: String = "voice",
    val status: String = "ringing",
    val createdAt: Timestamp? = null
)
```