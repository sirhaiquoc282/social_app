# SocialApp — Kotlin Android

Ứng dụng mạng xã hội Android với 3 tính năng cốt lõi:
- 💬 **Realtime Chat** (Firestore)
- 📞 **Voice Call** (Agora RTC)
- 📹 **Video Call** (Agora RTC)

**Màu sắc chủ đạo:** Navy Blue · Trắng · Xanh da trời nhạt

---

## Cấu trúc project

```
app/src/main/java/com/example/socialapp/
├── di/                          # Hilt Dependency Injection
│   └── AppModule.kt
├── data/
│   ├── model/                   # Data classes
│   │   ├── User.kt
│   │   ├── Message.kt
│   │   ├── Conversation.kt
│   │   └── CallSignal.kt
│   ├── repository/              # Business logic + Firestore
│   │   ├── AuthRepository.kt
│   │   ├── ChatRepository.kt
│   │   └── CallRepository.kt
│   └── remote/
│       └── AgoraManager.kt      # Agora RTC wrapper
├── ui/
│   ├── theme/                   # Navy Blue + Sky Blue theme
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Theme.kt
│   ├── navigation/
│   │   ├── Routes.kt
│   │   └── SocialAppNavGraph.kt
│   ├── splash/SplashScreen.kt
│   ├── auth/
│   │   ├── AuthViewModel.kt
│   │   ├── LoginScreen.kt
│   │   └── RegisterScreen.kt
│   ├── home/
│   │   ├── HomeViewModel.kt
│   │   └── HomeScreen.kt
│   ├── chat/
│   │   ├── ChatViewModel.kt
│   │   └── ChatScreen.kt
│   └── call/
│       ├── CallViewModel.kt
│       ├── IncomingCallScreen.kt
│       ├── IncomingCallActivity.kt
│       ├── PermissionRequest.kt
│       ├── voice/VoiceCallScreen.kt
│       └── video/VideoCallScreen.kt
├── service/
│   └── MyFirebaseMessagingService.kt
└── util/
    ├── Constants.kt
    ├── Extensions.kt
    └── PermissionHelper.kt
```

---

## Setup trước khi chạy

### 1. Firebase (bắt buộc)

1. Truy cập [console.firebase.google.com](https://console.firebase.google.com)
2. Tạo project → Thêm Android app với package `com.example.socialapp`
3. Download `google-services.json` → đặt vào `app/google-services.json`
4. Bật **Authentication** → Email/Password
5. Tạo **Firestore** database (test mode, region asia-southeast1)
6. Bật **Cloud Messaging** (FCM)

### 2. Agora (cho voice & video call)

1. Đăng ký tại [console.agora.io](https://console.agora.io)
2. Tạo project → chọn **Testing mode**
3. Sao chép **App ID** (32 ký tự hex)
4. Thêm vào `local.properties`:
   ```properties
   AGORA_APP_ID=your_32_char_app_id_here
   ```

### 3. Build

```bash
./gradlew assembleDebug
```

---

## Luồng hoạt động

### Chat
```
ChatScreen → ChatViewModel.sendMessage() → ChatRepository → Firestore
                                        ← observeMessages (realtime Flow)
```

### Voice Call
```
Caller: ChatScreen [📞] → VoiceCallScreen → CallViewModel.startCall()
                                          → Firestore /calls/{id} status="ringing"
                                          → FCM notification → Callee
Callee: IncomingCallScreen → acceptCall() → Firestore status="accepted"
                           → Agora join channel
Caller: nhận snapshot "accepted" → join channel → 2 bên kết nối
```

### Video Call
```
(giống Voice Call + AgoraEngine.enableVideo() + SurfaceView)
```

---

## Firestore Security Rules

Xem file `firestore.rules` — deploy lên Firebase:
```bash
firebase deploy --only firestore:rules
```

---

## Color Palette

| Tên | Hex | Dùng cho |
|-----|-----|---------|
| NavyBlueDark | `#0D1B4A` | Top bar, nền splash |
| NavyBlue | `#1B2A6B` | Primary, buttons, bubble gửi |
| LightSkyBlue | `#ADD8FF` | Accent, subtitle, call controls |
| SkyBlue | `#5BA4CF` | Secondary |
| White | `#FFFFFF` | Background card, bubble nhận |
| OffWhite | `#F0F4FF` | Background màn hình chính |

---

## Dependencies chính

- Jetpack Compose + Material3
- Hilt (DI)
- Firebase (Auth, Firestore, FCM, Storage)
- Agora RTC SDK 4.3.1
- Coil (image loading)
- Navigation Compose
- Kotlin Coroutines + Flow

