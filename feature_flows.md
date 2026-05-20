# Luồng tính năng chi tiết

## 1. Realtime Chat

### Luồng gửi tin nhắn

```
User A nhập text → nhấn Send
        │
        ▼
ChatViewModel.sendMessage()
        │
        ▼
ChatRepository.sendMessage()
        │  ┌─────────────────────────────────────────┐
        ├─►│ 1. Thêm document vào                    │
        │  │    /conversations/{id}/messages          │
        │  │    { senderId, text, sentAt: now() }     │
        │  └─────────────────────────────────────────┘
        │  ┌─────────────────────────────────────────┐
        └─►│ 2. Update /conversations/{id}            │
           │    { lastMessage, lastMessageAt,         │
           │      lastSenderId }                      │
           └─────────────────────────────────────────┘
```

### Luồng nhận tin nhắn (realtime)

```
App khởi động / mở màn chat
        │
        ▼
ChatRepository.observeMessages(conversationId)
        │
        ▼
Firestore.collection("messages")
         .orderBy("sentAt", ASC)
         .addSnapshotListener { snapshot, _ ->
             emit(snapshot.toObjects<Message>())
         }
        │
        ▼  (Flow<List<Message>>)
ChatViewModel.messages (StateFlow)
        │
        ▼
ChatScreen composable tự re-render khi có tin mới
```

### Luồng push notification (app ở background)

```
User B gửi tin → Firestore cập nhật
        │
        ▼
Firebase Cloud Function (trigger onWrite)  ← hoặc gọi FCM từ client
        │
        ▼
FCM gửi notification đến device User A
  payload: { title: "Tên người gửi", body: "Nội dung tin" }
        │
        ▼
Nếu app foreground  → hiển thị in-app snackbar
Nếu app background  → system notification tray
```

> **Lưu ý cho lab:** Có thể bỏ qua Cloud Function bằng cách gọi FCM HTTP API trực tiếp từ client khi gửi tin (đơn giản hơn nhưng không an toàn cho production).

---

## 2. Voice Call

### Tổng quan luồng

Firestore đóng vai trò **signaling server** — thông báo có cuộc gọi đến và đồng bộ trạng thái. Agora xử lý toàn bộ media (audio stream).

### Luồng Caller (người gọi)

```
User A nhấn "Gọi thoại" trên màn hình User B
        │
        ▼
CallViewModel.startVoiceCall(calleeId)
        │
        ├─► 1. Tạo callId = UUID.randomUUID()
        │
        ├─► 2. Tạo document /calls/{callId}
        │       { callerId: A, calleeId: B,
        │         channelName: callId,
        │         type: "voice", status: "ringing" }
        │
        ├─► 3. Gửi FCM notification đến B
        │       { type: "incoming_call", callId, callerName,
        │         callType: "voice" }
        │
        ├─► 4. Mở VoiceCallScreen (trạng thái: "Đang đổ chuông...")
        │
        └─► 5. Lắng nghe /calls/{callId}
                 status == "accepted" → join Agora channel
                 status == "declined" → đóng màn hình, thông báo
                 (timeout 30s không trả lời → set status = "missed")
```

### Luồng Callee (người nhận)

```
Device B nhận FCM notification
        │
        ├─ App foreground → mở IncomingCallScreen overlay
        └─ App background → Full-screen notification với nút Nghe / Từ chối
                │
                ▼
        User B nhấn "Nghe"
                │
                ▼
        CallRepository.acceptCall(callId)
                │
                ├─► 1. Update /calls/{callId} { status: "accepted", answeredAt: now() }
                │
                └─► 2. Join Agora channel với channelName = callId
                        → cả 2 bên đều đang lắng nghe Firestore
                        → cả 2 bên join cùng channel → media kết nối qua Agora
```

### Luồng kết thúc cuộc gọi

```
Một trong 2 bên nhấn "Kết thúc"
        │
        ▼
CallRepository.endCall(callId)
        │
        ├─► 1. AgoraEngine.leaveChannel()
        │
        └─► 2. Update /calls/{callId}
                { status: "ended", endedAt: now() }
                → bên còn lại nhận snapshot → leaveChannel() → đóng màn hình
```

---

## 3. Video Call

Luồng **hoàn toàn giống Voice Call**, chỉ khác ở:

| Điểm khác | Voice | Video |
|---|---|---|
| `type` trong Firestore | `"voice"` | `"video"` |
| Agora setup | `enableAudio()` only | `enableVideo()` + `enableAudio()` |
| UI | Avatar + controls | `AgoraVideoView` (local + remote) |
| Permission | `RECORD_AUDIO` | `RECORD_AUDIO` + `CAMERA` |

### Luồng khởi tạo video (bổ sung so với voice)

```
Sau khi join Agora channel thành công
        │
        ├─► AgoraEngine.setupLocalVideo(localView)
        │       → hiển thị camera của mình (góc nhỏ)
        │
        └─► Callback onUserJoined(remoteUid)
                → AgoraEngine.setupRemoteVideo(remoteView, remoteUid)
                → hiển thị camera đối phương (toàn màn hình)
```

### Camera/mic controls

```
Nút tắt mic  → AgoraEngine.muteLocalAudioStream(true/false)
Nút tắt cam  → AgoraEngine.muteLocalVideoStream(true/false)
Nút đổi cam  → AgoraEngine.switchCamera()
Nút loa ngoài → AudioManager.setSpeakerphoneOn(true/false)
```

---

## 4. Authentication Flow

```
App khởi động
        │
        ▼
SplashScreen: kiểm tra FirebaseAuth.currentUser
        │
        ├─ null  → LoginScreen
        └─ not null → HomeScreen (danh sách conversations)

LoginScreen
        │
        ├─ Email/Password → Auth.signInWithEmailAndPassword()
        └─ Google Sign-In → Auth.signInWithCredential(GoogleAuthProvider)
                │
                ▼
        Đăng nhập thành công
                │
                ├─► Upsert /users/{uid} (tạo nếu chưa có)
                ├─► Lấy FCM token → lưu vào /users/{uid}.fcmToken
                └─► Navigate → HomeScreen
```