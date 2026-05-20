# Tổng quan hệ thống — Kotlin Social App

## Mục tiêu

Ứng dụng mạng xã hội Android (Kotlin) dành cho bài lab môn học, tập trung vào 3 tính năng cốt lõi:

| Tính năng | Mô tả |
|---|---|
| **Realtime Chat** | Nhắn tin 1-1 theo thời gian thực |
| **Voice Call** | Gọi thoại 1-1 qua internet |
| **Video Call** | Gọi video 1-1 qua internet |

---

## Nguyên tắc thiết kế

- **Đơn giản trước hết** — ưu tiên Firebase-native thay vì tự dựng backend
- **Serverless** — không cần máy chủ riêng, toàn bộ dùng managed services
- **Offline-first nhẹ** — Firestore hỗ trợ cache cục bộ sẵn
- **1 repo, 1 app** — monolithic Android project, không tách microservice

---

## Stack công nghệ

### Android Client

| Thành phần | Lựa chọn | Lý do |
|---|---|---|
| Ngôn ngữ | Kotlin | Yêu cầu đề bài |
| UI | Jetpack Compose | Hiện đại, ít boilerplate |
| Async | Kotlin Coroutines + Flow | Native Kotlin, tích hợp tốt Firestore |
| DI | Hilt | Tiêu chuẩn Google |
| Navigation | Navigation Compose | Single-activity pattern |
| Image loading | Coil | Nhẹ, hỗ trợ Compose |

### Backend (Firebase — không cần server riêng)

| Service | Vai trò |
|---|---|
| Firebase Auth | Đăng ký / đăng nhập (Email + Google) |
| Cloud Firestore | Lưu tin nhắn, thông tin user, signaling WebRTC |
| Firebase Cloud Messaging (FCM) | Push notification cuộc gọi đến |
| Firebase Storage | Ảnh đại diện, file đính kèm (tùy chọn) |

### Voice & Video Call

| Thành phần | Lựa chọn | Lý do |
|---|---|---|
| Media engine | **Agora RTC SDK** (gói free) | Đơn giản nhất cho lab: ~10 dòng để có call hoạt động, free tier 10,000 phút/tháng |
| Signaling | Firestore | Dùng lại Firebase đã có, không cần server signaling riêng |
| Fallback | WebRTC thuần (tùy chọn nâng cao) | Phức tạp hơn, chỉ dùng nếu yêu cầu không dùng SDK bên thứ 3 |

> **Lý do chọn Agora thay vì WebRTC thuần:**  
> WebRTC thuần yêu cầu bạn tự dựng STUN/TURN server (coturn) hoặc dùng dịch vụ trả phí. Agora cung cấp toàn bộ infrastructure này, SDK chỉ cần `appId + channelName + token` là có call hoạt động — phù hợp bài lab.

---

## Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────┐
│                   Android App (Kotlin)               │
│                                                      │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ Chat UI  │  │ Voice Call UI│  │ Video Call UI │  │
│  └────┬─────┘  └──────┬───────┘  └───────┬───────┘  │
│       │               │                  │           │
│  ┌────▼───────────────▼──────────────────▼───────┐  │
│  │          ViewModel Layer (MVVM)                │  │
│  └────┬───────────────┬───────────────────────────┘  │
│       │               │                              │
│  ┌────▼────┐    ┌──────▼──────┐                      │
│  │  Chat   │    │  Call Repo  │                      │
│  │  Repo   │    │  (Agora)    │                      │
│  └────┬────┘    └──────┬──────┘                      │
└───────┼────────────────┼────────────────────────────┘
        │                │
        ▼                ▼
┌───────────────┐  ┌─────────────────────────────────┐
│   Firebase    │  │         Agora Cloud               │
│  ─ Auth       │  │  (STUN/TURN + Media Relay tích   │
│  ─ Firestore  │  │   hợp sẵn, không cần cấu hình)  │
│  ─ FCM        │  └─────────────────────────────────┘
│  ─ Storage    │
└───────────────┘
```

---

## Cấu trúc package Android

```
com.example.socialapp/
├── di/                      # Hilt modules
├── data/
│   ├── model/               # User, Message, CallSignal (data class)
│   ├── repository/
│   │   ├── AuthRepository.kt
│   │   ├── ChatRepository.kt
│   │   └── CallRepository.kt
│   └── remote/
│       ├── FirestoreSource.kt
│       └── AgoraManager.kt
├── ui/
│   ├── auth/                # Login, Register screens
│   ├── home/                # Danh sách conversation
│   ├── chat/                # Màn hình chat
│   ├── call/
│   │   ├── voice/           # Voice call screen
│   │   └── video/           # Video call screen
│   └── profile/             # Hồ sơ người dùng
└── util/                    # Extensions, constants
```