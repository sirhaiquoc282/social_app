# Hạ tầng & Setup

## 1. Firebase Project Setup

### Bước 1 — Tạo project

1. Truy cập [console.firebase.google.com](https://console.firebase.google.com)
2. **Add project** → đặt tên (vd: `social-app-lab`)
3. Tắt Google Analytics (không cần cho lab)
4. **Add Android app**:
    - Package name: `com.example.socialapp`
    - Download `google-services.json` → đặt vào `/app/google-services.json`

### Bước 2 — Kích hoạt các services

#### Firebase Authentication
```
Console → Authentication → Sign-in method → Enable:
  ✅ Email/Password
  ✅ Google (cần SHA-1 fingerprint của keystore)
```

Lấy SHA-1 fingerprint:
```bash
./gradlew signingReport
# Lấy giá trị SHA1 từ debug keystore
```

#### Cloud Firestore
```
Console → Firestore Database → Create database
  → Start in test mode (cho lab, đổi rules sau)
  → Chọn region: asia-southeast1 (Singapore, gần Việt Nam)
```

#### Firebase Cloud Messaging
```
Console → Project Settings → Cloud Messaging
  → FCM đã tự động enabled, lấy Server Key để dùng nếu cần
```

#### Firebase Storage (tùy chọn — cho avatar/ảnh)
```
Console → Storage → Get started → Test mode
  → Region: asia-southeast1
```

---

## 2. Agora Setup

### Tạo tài khoản & project

1. Đăng ký tại [console.agora.io](https://console.agora.io)
2. **Create Project**:
    - Project name: `social-app-lab`
    - Authentication: **Testing mode** (không cần token — đơn giản cho lab)
3. Lấy **App ID** (dạng hex 32 ký tự)

> **Testing mode vs. Secure mode:**  
> Testing mode: không cần generate token, chỉ cần App ID. Dùng cho lab.  
> Secure mode: cần backend generate token trước mỗi cuộc gọi. Dùng cho production.

### Free tier Agora
- 10,000 phút voice/video miễn phí mỗi tháng
- Không cần thẻ tín dụng để dùng free tier

---

## 3. Android Project Configuration

### `build.gradle` (project level)
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
}
```

### `build.gradle` (app level)
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34

        // Đặt Agora App ID vào local.properties, không commit lên git
        buildConfigField("String", "AGORA_APP_ID", "\"${project.findProperty("AGORA_APP_ID")}\"")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Firebase BOM — quản lý version tập trung
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Agora RTC
    implementation("io.agora.rtc:full-sdk:4.3.1")

    // Jetpack Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ViewModel + Coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Coil (ảnh)
    implementation("io.coil-kt:coil-compose:2.6.0")
}
```

### `local.properties` (KHÔNG commit lên Git)
```properties
AGORA_APP_ID=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

### `AndroidManifest.xml` — Permissions
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH" />

<!-- FCM Service -->
<service
    android:name=".service.MyFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

---

## 4. FCM — Xử lý thông báo cuộc gọi

### `MyFirebaseMessagingService.kt`
```kotlin
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        // Lưu token mới lên Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users").document(uid)
            .update("fcmToken", token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        when (data["type"]) {
            "incoming_call" -> showIncomingCallNotification(data)
            "new_message"   -> showMessageNotification(data)
        }
    }

    private fun showIncomingCallNotification(data: Map<String, String>) {
        val callId    = data["callId"] ?: return
        val callerName = data["callerName"] ?: "Unknown"
        val callType  = data["callType"] ?: "voice"

        // Intent mở IncomingCallActivity khi nhấn notification
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            putExtra("callType", callType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        val notification = NotificationCompat.Builder(this, CALL_CHANNEL_ID)
            .setContentTitle("Cuộc gọi đến từ $callerName")
            .setContentText(if (callType == "video") "Gọi video" else "Gọi thoại")
            .setSmallIcon(R.drawable.ic_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE), true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .build()

        NotificationManagerCompat.from(this).notify(CALL_NOTIFICATION_ID, notification)
    }
}
```

---

## 5. Agora Integration — Code mẫu

### `AgoraManager.kt`
```kotlin
@Singleton
class AgoraManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var engine: RtcEngine? = null

    fun initEngine(eventHandler: IRtcEngineEventHandler) {
        val config = RtcEngineConfig().apply {
            mContext   = context
            mAppId     = BuildConfig.AGORA_APP_ID
            mEventHandler = eventHandler
        }
        engine = RtcEngine.create(config)
    }

    fun joinVoiceChannel(channelName: String, uid: Int = 0) {
        engine?.apply {
            enableAudio()
            disableVideo()
            joinChannel(null, channelName, uid, ChannelMediaOptions().apply {
                clientRoleType = CLIENT_ROLE_BROADCASTER
                channelProfile  = CHANNEL_PROFILE_COMMUNICATION
            })
        }
    }

    fun joinVideoChannel(channelName: String, localView: SurfaceView, uid: Int = 0) {
        engine?.apply {
            enableAudio()
            enableVideo()
            setupLocalVideo(VideoCanvas(localView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            startPreview()
            joinChannel(null, channelName, uid, ChannelMediaOptions().apply {
                clientRoleType = CLIENT_ROLE_BROADCASTER
                channelProfile  = CHANNEL_PROFILE_COMMUNICATION
            })
        }
    }

    fun setupRemoteVideo(remoteView: SurfaceView, remoteUid: Int) {
        engine?.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, remoteUid))
    }

    fun muteAudio(muted: Boolean)  = engine?.muteLocalAudioStream(muted)
    fun muteVideo(muted: Boolean)  = engine?.muteLocalVideoStream(muted)
    fun switchCamera()             = engine?.switchCamera()
    fun setSpeaker(on: Boolean)    = engine?.setEnableSpeakerphone(on)

    fun leaveChannel() {
        engine?.leaveChannel()
    }

    fun destroy() {
        engine?.let { RtcEngine.destroy() }
        engine = null
    }
}
```

---

## 6. Biến môi trường & Secrets checklist

| Thứ gì | Lưu ở đâu | Commit git? |
|---|---|---|
| `google-services.json` | `/app/` | ❌ Không (thêm vào `.gitignore`) |
| `AGORA_APP_ID` | `local.properties` | ❌ Không |
| Firebase config | trong `google-services.json` | ❌ Không |
| Firestore rules | `firestore.rules` | ✅ Có |

### `.gitignore` bổ sung
```
google-services.json
local.properties
*.jks
*.keystore
```