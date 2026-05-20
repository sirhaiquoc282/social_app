# Data Model — Firestore Schema

## Cấu trúc Collections

### `/users/{userId}`

Lưu thông tin người dùng.

```json
{
  "uid": "string",
  "displayName": "string",
  "email": "string",
  "avatarUrl": "string",
  "fcmToken": "string",        // cập nhật mỗi lần app mở
  "status": "online | offline",
  "createdAt": "timestamp"
}
```

---

### `/conversations/{conversationId}`

Mỗi conversation là 1 cặp 2 user (1-1 chat).

```json
{
  "participants": ["userId1", "userId2"],
  "lastMessage": "string",
  "lastMessageAt": "timestamp",
  "lastSenderId": "string"
}
```

> **Quy tắc tạo `conversationId`:** ghép 2 uid theo thứ tự alphabetical rồi nối bằng `_`  
> Ví dụ: `uid_ABC` + `uid_XYZ` → conversationId = `uid_ABC_uid_XYZ`  
> Điều này đảm bảo 2 user luôn tìm cùng 1 document, không cần query.

---

### `/conversations/{conversationId}/messages/{messageId}`

Subcollection lưu tin nhắn.

```json
{
  "senderId": "string",
  "text": "string",
  "imageUrl": "string | null",
  "type": "text | image",
  "sentAt": "timestamp",
  "readAt": "timestamp | null"
}
```

---

### `/calls/{callId}`

Dùng làm signaling channel cho Agora — thông báo có cuộc gọi đến.

```json
{
  "callerId": "string",
  "calleeId": "string",
  "callerName": "string",
  "callerAvatar": "string",
  "channelName": "string",      // = callId, dùng để join Agora channel
  "type": "voice | video",
  "status": "ringing | accepted | declined | ended | missed",
  "createdAt": "timestamp",
  "answeredAt": "timestamp | null",
  "endedAt": "timestamp | null"
}
```

> **Lưu ý signaling:** Firestore thay thế hoàn toàn WebSocket signaling server.  
> Caller tạo document → Callee lắng nghe snapshot → accept/decline → cả 2 join Agora channel.

---

## Firestore Security Rules (cơ bản cho lab)

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    // Users: chỉ đọc nếu đã login, chỉ sửa profile của chính mình
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }

    // Conversations: chỉ participants mới được đọc/ghi
    match /conversations/{conversationId} {
      allow read, write: if request.auth.uid in resource.data.participants
                         || request.auth.uid in request.resource.data.participants;

      match /messages/{messageId} {
        allow read, write: if request.auth != null;
        // Đơn giản cho lab — production cần check participants
      }
    }

    // Calls: caller hoặc callee mới được tương tác
    match /calls/{callId} {
      allow read: if request.auth.uid == resource.data.callerId
                  || request.auth.uid == resource.data.calleeId;
      allow create: if request.auth.uid == request.resource.data.callerId;
      allow update: if request.auth.uid == resource.data.calleeId
                    || request.auth.uid == resource.data.callerId;
    }
  }
}
```

---

## Indexes cần tạo (Firestore Composite Index)

| Collection | Fields | Order |
|---|---|---|
| `conversations` | `participants` (array-contains), `lastMessageAt` | DESC |
| `conversations/{id}/messages` | `sentAt` | ASC |
| `calls` | `calleeId`, `status`, `createdAt` | DESC |