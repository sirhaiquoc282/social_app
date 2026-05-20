# Agent Deployment Checklist

Danh sách bước tuần tự để agent (hoặc developer) triển khai hệ thống từ đầu đến khi chạy được.

---

## Phase 1 — Firebase Setup (15 phút)

- [ ] 1.1 Tạo Firebase project tại console.firebase.google.com
- [ ] 1.2 Thêm Android app với package `com.example.socialapp`
- [ ] 1.3 Download `google-services.json` → đặt vào `app/`
- [ ] 1.4 Bật **Authentication** → Email/Password + Google
- [ ] 1.5 Chạy `./gradlew signingReport` → lấy SHA-1 → thêm vào Firebase project settings
- [ ] 1.6 Tạo **Firestore** database ở region `asia-southeast1`, mode `test`
- [ ] 1.7 Tạo **Storage** bucket (nếu cần ảnh đại diện)
- [ ] 1.8 Vào **Cloud Messaging** → xác nhận FCM đã enabled

---

## Phase 2 — Agora Setup (5 phút)

- [ ] 2.1 Đăng ký / đăng nhập tại console.agora.io
- [ ] 2.2 Tạo project mới, chọn **Testing mode** (không cần token)
- [ ] 2.3 Sao chép **App ID** (32 ký tự hex)
- [ ] 2.4 Thêm vào `local.properties`:
  ```
  AGORA_APP_ID=<app_id_của_bạn>
  ```

---

## Phase 3 — Android Project Init (10 phút)

- [ ] 3.1 Tạo Android project mới (Empty Compose Activity)
    - Package: `com.example.socialapp`
    - Min SDK: 24
    - Language: Kotlin
- [ ] 3.2 Copy `google-services.json` vào `app/`
- [ ] 3.3 Cập nhật `build.gradle` (project + app) theo file `04-infrastructure-setup.md`
- [ ] 3.4 Thêm permissions vào `AndroidManifest.xml`
- [ ] 3.5 Sync Gradle → đảm bảo build thành công

---

## Phase 4 — Firestore Schema Init (5 phút)

- [ ] 4.1 Vào Firestore Console → tạo collection `users` (document rỗng, tự tạo khi user đăng ký)
- [ ] 4.2 Tạo collection `conversations` (document rỗng)
- [ ] 4.3 Tạo collection `calls` (document rỗng)
- [ ] 4.4 Deploy Firestore Rules theo nội dung trong `02-data-model.md`
- [ ] 4.5 Tạo Composite Indexes (Firestore sẽ tự nhắc khi query fail lần đầu, hoặc tạo thủ công):
    - `conversations`: `participants` (array) + `lastMessageAt` DESC
    - `calls`: `calleeId` ASC + `status` ASC + `createdAt` DESC

---

## Phase 5 — Code Implementation Order

Thứ tự implement để tránh dependency lỗi:

```
1. Data models (User, Message, Conversation, CallSignal)
2. Hilt DI modules (FirebaseModule, AgoraModule)
3. AuthRepository + AuthViewModel + Login/Register Screen
4. ChatRepository + ChatViewModel + ChatScreen
5. AgoraManager
6. CallRepository + CallViewModel
7. VoiceCallScreen + VideoCallScreen
8. IncomingCallScreen + FCM Service
9. HomeScreen (danh sách conversations)
10. ProfileScreen
```

---

## Phase 6 — Test Checklist

### Auth
- [ ] Đăng ký tài khoản mới → user document tạo trong Firestore
- [ ] Đăng nhập email/password thành công
- [ ] Đăng nhập Google thành công
- [ ] Đăng xuất → về màn Login

### Chat
- [ ] Gửi tin từ device A → hiển thị ngay trên device B (< 1s)
- [ ] Tin nhắn hiển thị đúng bubble trái/phải
- [ ] Conversation list cập nhật last message
- [ ] Notification nhận tin khi app background

### Voice Call
- [ ] Device A gọi B → B nhận notification
- [ ] B nhấn Nghe → cả 2 nghe được nhau
- [ ] Nút mute hoạt động
- [ ] Nút kết thúc → cả 2 màn hình đóng

### Video Call
- [ ] Camera preview hiển thị trước khi kết nối
- [ ] Sau kết nối: local cam góc nhỏ, remote cam toàn màn hình
- [ ] Nút tắt camera, tắt mic hoạt động
- [ ] Nút đổi camera (front/back) hoạt động

---

## Biến môi trường cần truyền cho agent

```yaml
# Các giá trị agent cần được cung cấp trước khi bắt đầu
firebase:
  project_id: "social-app-lab"
  package_name: "com.example.socialapp"
  # google-services.json sẽ download từ Firebase Console

agora:
  app_id: "<AGORA_APP_ID>"   # từ Agora Console, Testing mode

android:
  min_sdk: 24
  target_sdk: 34
  kotlin_version: "1.9.23"
```

---

## Lỗi thường gặp & cách sửa

| Lỗi | Nguyên nhân | Cách sửa |
|---|---|---|
| `google-services.json not found` | File chưa đặt đúng chỗ | Đặt vào `app/` (cùng cấp `build.gradle` của app module) |
| `SHA-1 not registered` | Google Sign-In fail | Thêm SHA-1 vào Firebase project settings |
| Firestore permission denied | Rules chưa đúng | Kiểm tra rules, tạm thời dùng `allow read, write: if true` để test |
| Agora `ERR_JOIN_CHANNEL_REJECTED` | App ID sai | Kiểm tra `AGORA_APP_ID` trong `local.properties` |
| FCM không nhận được | FCM token chưa lưu | Đảm bảo `onNewToken` lưu token lên Firestore sau login |
| Video đen (không hiện hình) | Thiếu CAMERA permission | Runtime request permission trước khi join channel |
| Composite index error | Index chưa tạo | Click link trong logcat để tạo index tự động |