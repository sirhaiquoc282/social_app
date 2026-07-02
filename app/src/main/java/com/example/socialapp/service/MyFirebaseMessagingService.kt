package com.example.socialapp.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.socialapp.R
import com.example.socialapp.SocialApp
import com.example.socialapp.ui.call.IncomingCallActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

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
            "incoming_call" -> checkAndShowIncomingCall(data)
            "cancel_call"   -> cancelCallNotification(this)
            "new_message"   -> showMessageNotification(data)
        }
    }

    private fun checkAndShowIncomingCall(data: Map<String, String>) {
        val callId = data["callId"] ?: return
        
        // Tránh tình trạng notification tới trễ (sau khi cuộc gọi đã kết thúc hoặc bắt máy)
        FirebaseFirestore.getInstance().collection("calls").document(callId)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists() && doc.getString("status") == "ringing") {
                    showIncomingCallNotification(data)
                } else {
                    android.util.Log.d("FCMService", "Ignoring stale incoming call notification for callId=$callId")
                }
            }
            .addOnFailureListener {
                // Nếu lỗi mạng, cứ show tạm, vào Activity nó sẽ tự kiểm tra lại
                showIncomingCallNotification(data)
            }
    }

    private fun showIncomingCallNotification(data: Map<String, String>) {
        val callId      = data["callId"] ?: return
        val callerName  = data["callerName"] ?: "Unknown"
        val callerAvatar = data["callerAvatar"] ?: ""
        val callerId    = data["callerId"] ?: ""
        val callType    = data["callType"] ?: "voice"

        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("callId", callId)
            putExtra("callerName", callerName)
            putExtra("callerAvatar", callerAvatar)
            putExtra("callerId", callerId)
            putExtra("callType", callType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, SocialApp.CHANNEL_CALL)
            .setContentTitle("Cuộc gọi đến từ $callerName")
            .setContentText(if (callType == "video") "📹 Gọi video" else "📞 Gọi thoại")
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(CALL_NOTIFICATION_ID, notification)
    }

    private fun showMessageNotification(data: Map<String, String>) {
        val senderName = data["senderName"] ?: "Tin nhắn mới"
        val body       = data["body"] ?: ""

        val notification = NotificationCompat.Builder(this, SocialApp.CHANNEL_MESSAGE)
            .setContentTitle(senderName)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(MESSAGE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val CALL_NOTIFICATION_ID = 1001
        const val MESSAGE_NOTIFICATION_ID = 1002

        fun cancelCallNotification(context: android.content.Context) {
            val nm = context.getSystemService(NotificationManager::class.java)
            nm.cancel(CALL_NOTIFICATION_ID)
        }

        fun showIncomingCallNotificationLocal(context: android.content.Context, signal: com.example.socialapp.data.model.CallSignal) {
            val intent = Intent(context, IncomingCallActivity::class.java).apply {
                putExtra("callId", signal.id)
                putExtra("callerName", signal.callerName)
                putExtra("callerAvatar", signal.callerAvatar)
                putExtra("callerId", signal.callerId)
                putExtra("callType", signal.type)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // QUAN TRỌNG: KHÔNG dùng setFullScreenIntent ở đây!
            // Vì MainScreen Dialog đã hiển thị popup cuộc gọi khi app foreground.
            // Nếu dùng fullScreenIntent, IncomingCallActivity sẽ tự mở ra
            // → IncomingCallActivity hiện VideoCallScreen → khởi động Camera
            // → Xung đột với Dialog đang hiển thị → Lỗi EGLImage liên tục!
            val notification = NotificationCompat.Builder(context, SocialApp.CHANNEL_CALL)
                .setContentTitle("Cuộc gọi đến từ ${signal.callerName}")
                .setContentText(if (signal.type == "video") "\uD83D\uDCF9 Gọi video" else "\uD83D\uDCDE Gọi thoại")
                .setSmallIcon(android.R.drawable.ic_menu_call)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            val nm = context.getSystemService(NotificationManager::class.java)
            nm.notify(CALL_NOTIFICATION_ID, notification)
        }
    }
}

