# 🚀 Hướng dẫn chạy dự án trên Android Studio

## Yêu cầu hệ thống
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 17 (có sẵn trong Android Studio)
- Android SDK API 34
- Thiết bị hoặc Emulator API 24 trở lên

---

## BƯỚC 1 — Mở project trong Android Studio

1. Mở **Android Studio**
2. Chọn **Open** (hoặc **File → Open**)
3. Dẫn đến thư mục: `E:\Personal\Projects\social_app`
4. Click **OK**
5. Chờ Android Studio index project (có thể mất vài phút)

---

## BƯỚC 2 — Cấu hình Android SDK

Nếu gặp lỗi **"SDK not found"**:

1. Vào **File → Project Structure** (Ctrl+Alt+Shift+S)
2. Chọn **SDK Location**
3. Kiểm tra đường dẫn Android SDK (thường là `C:\Users\<TênBạn>\AppData\Local\Android\Sdk`)
4. Mở file `local.properties` ở thư mục gốc dự án
5. Sửa dòng `sdk.dir=` thành đường dẫn thực tế, ví dụ:
   ```
   sdk.dir=C\:\\Users\\NguyenVanA\\AppData\\Local\\Android\\Sdk
   ```
   > **Lưu ý:** Dùng `\\` thay vì `\` trong file properties

---

## BƯỚC 3 — Firebase Setup (Bắt buộc để app hoạt động)

### 3a. Tạo Firebase Project

1. Truy cập: https://console.firebase.google.com
2. Click **Add project** → Đặt tên (vd: `social-app-demo`)
3. Tắt Google Analytics → Click **Create project**

### 3b. Thêm Android App vào Firebase

1. Trong Firebase Console → Click biểu tượng Android **< />**
2. **Android package name**: `com.example.socialapp`
3. **App nickname**: SocialApp (tùy chọn)
4. Click **Register app**
5. **Download `google-services.json`**
6. **Thay thế** file `app\google-services.json` trong dự án bằng file vừa download
7. Click **Next** → **Next** → **Continue to console**

### 3c. Bật Authentication

1. Firebase Console → **Authentication** → **Get started**
2. Tab **Sign-in method** → Enable **Email/Password** → Save
3. (Tùy chọn) Enable **Google** sign-in

### 3d. Tạo Firestore Database

1. Firebase Console → **Firestore Database** → **Create database**
2. Chọn **Start in test mode**
3. Region: **asia-southeast1** (Singapore — gần VN nhất)
4. Click **Enable**

### 3e. Deploy Firestore Rules

Sau khi tạo Firestore, vào **Rules** tab và paste nội dung từ file `firestore.rules`:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth.uid == userId;
    }
    match /conversations/{conversationId} {
      allow read, write: if request.auth.uid in resource.data.participants
                         || request.auth.uid in request.resource.data.participants;
      match /messages/{messageId} {
        allow read, write: if request.auth != null;
      }
    }
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

Click **Publish**.

---

## BƯỚC 4 — Agora Setup (Cho Voice/Video Call)

1. Truy cập: https://console.agora.io
2. Đăng ký tài khoản (miễn phí)
3. Click **Create Project**:
   - Project name: `social-app`
   - Authentication: **Testing mode** ← Quan trọng! (không cần generate token)
4. Sao chép **App ID** (chuỗi 32 ký tự hex)
5. Mở file `local.properties` → sửa:
   ```properties
   AGORA_APP_ID=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
   ```
   (Thay bằng App ID thực)

> **Free tier:** 10,000 phút/tháng, không cần thẻ credit card

---

## BƯỚC 5 — Sync Gradle

Sau khi thay thế `google-services.json` và cập nhật `local.properties`:

1. Android Studio sẽ tự hỏi **"Gradle files have changed"** → Click **Sync Now**
2. Hoặc vào **File → Sync Project with Gradle Files**
3. Chờ sync hoàn tất (có thể tải dependencies mất 3-10 phút lần đầu)

---

## BƯỚC 6 — Chạy App

### Trên Emulator:
1. **Tools → Device Manager** → **Create Device**
2. Chọn Pixel 6 (hoặc bất kỳ)
3. System Image: **API 34** (Tiramisu)
4. Click **Run** (▶) hoặc Shift+F10

### Trên thiết bị thật:
1. Bật **Developer Options** trên điện thoại
   (Settings → About Phone → tap **Build Number** 7 lần)
2. Bật **USB Debugging**
3. Cắm cáp USB → Allow debugging
4. Click **Run** (▶)

---

## 🔧 Xử lý lỗi thường gặp

### ❌ `google-services.json is missing`
→ Download từ Firebase Console và đặt vào thư mục `app/`

### ❌ `SDK location not found`
→ Sửa `sdk.dir` trong `local.properties`

### ❌ `Gradle sync failed` / `Could not resolve...`
→ Kiểm tra kết nối internet → File → Sync Project with Gradle Files

### ❌ `AGORA_APP_ID is empty` (call không hoạt động)
→ Thêm `AGORA_APP_ID=...` vào `local.properties`

### ❌ `Permission denied` khi gọi
→ Cho phép quyền Microphone/Camera khi app hỏi

### ❌ Firestore permission denied
→ Vào Firebase Console → Firestore → Rules → tạm thời đổi thành:
```
allow read, write: if true;
```
(chỉ dùng cho test, không dùng production)

### ❌ Build failed: `Duplicate class`
→ Vào **File → Invalidate Caches → Invalidate and Restart**

### ❌ `minSdk` error
→ Đảm bảo emulator/thiết bị chạy Android 7.0 (API 24) trở lên

---

## 🧪 Test luồng Chat (2 thiết bị/emulator)

1. **Device A**: Đăng ký tài khoản → `userA@test.com`
2. **Device B**: Đăng ký tài khoản → `userB@test.com`
3. **Device A**: Tab "Người dùng" → Click vào userB → Gửi tin nhắn
4. **Device B**: Tab "Tin nhắn" → Thấy tin nhắn realtime

## 🧪 Test Voice Call

1. **Device A**: Mở chat với userB → Nhấn icon 📞
2. **Device B**: Sẽ thấy màn hình "Cuộc gọi đến" → Nhấn "Nghe"
3. Cả 2 có thể nói chuyện qua Agora

## 🧪 Test Video Call

1. **Device A**: Mở chat → Nhấn icon 📹
2. **Device B**: Nhấn "Nghe" → Camera bật
3. **Device A**: Camera preview ở góc nhỏ, remote ở full màn hình

---

## 📁 Files quan trọng cần biết

```
social_app/
├── local.properties          ← Thêm sdk.dir và AGORA_APP_ID
├── app/
│   ├── google-services.json  ← Download từ Firebase (THAY THẾ FILE HIỆN TẠI)
│   ├── build.gradle.kts      ← Dependencies
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── java/com/example/socialapp/
│           ├── ui/theme/Color.kt     ← Màu sắc Navy/White/SkyBlue
│           ├── ui/chat/ChatScreen.kt ← Màn hình chat
│           ├── ui/call/voice/        ← Voice call
│           └── ui/call/video/        ← Video call
└── firestore.rules           ← Deploy lên Firebase Console
```

