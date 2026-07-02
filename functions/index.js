const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

// Khởi tạo Firebase Admin SDK
initializeApp();

const db = getFirestore();
const messaging = getMessaging();

/**
 * Cloud Function: Gửi FCM notification khi có cuộc gọi mới.
 *
 * Trigger: Khi document mới được tạo trong collection "calls".
 * Flow:
 *   1. Đọc calleeId từ document mới
 *   2. Lấy fcmToken của callee từ collection "users"
 *   3. Gửi FCM data message đến máy callee
 *
 * Dùng DATA message (không phải notification message) để đảm bảo
 * MyFirebaseMessagingService.onMessageReceived() luôn được gọi,
 * kể cả khi app ở foreground hay background.
 */
exports.onCallCreated = onDocumentCreated("calls/{callId}", async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    console.log("No data in snapshot");
    return null;
  }

  const callData = snapshot.data();
  const callId = event.params.callId;

  // Chỉ gửi notification khi status = "ringing"
  if (callData.status !== "ringing") {
    console.log(`Call ${callId} status is "${callData.status}", skipping notification`);
    return null;
  }

  const calleeId = callData.calleeId;
  if (!calleeId) {
    console.log("Missing calleeId");
    return null;
  }

  // Lấy FCM token của callee
  const calleeDoc = await db.collection("users").doc(calleeId).get();
  if (!calleeDoc.exists) {
    console.log(`Callee ${calleeId} not found in users collection`);
    return null;
  }

  const calleeData = calleeDoc.data();
  const fcmToken = calleeData.fcmToken;

  if (!fcmToken) {
    console.log(`Callee ${calleeId} has no FCM token`);
    return null;
  }

  // Gửi FCM DATA message (quan trọng: dùng "data" thay vì "notification"
  // để onMessageReceived() luôn được gọi)
  const message = {
    token: fcmToken,
    data: {
      type: "incoming_call",
      callId: callId,
      callerId: callData.callerId || "",
      callerName: callData.callerName || "Unknown",
      callerAvatar: callData.callerAvatar || "",
      callType: callData.type || "voice",
      channelName: callData.channelName || "",
    },
    android: {
      priority: "high",
      // TTL 30 giây — cuộc gọi sẽ hết hạn sau 30s
      ttl: 30000,
    },
  };

  try {
    const response = await messaging.send(message);
    console.log(`✅ FCM sent to ${calleeId} for call ${callId}:`, response);
    return response;
  } catch (error) {
    console.error(`❌ FCM send failed for call ${callId}:`, error);

    // Nếu token không hợp lệ → xóa token cũ
    if (
      error.code === "messaging/invalid-registration-token" ||
      error.code === "messaging/registration-token-not-registered"
    ) {
      console.log(`Removing invalid FCM token for user ${calleeId}`);
      await db.collection("users").doc(calleeId).update({ fcmToken: "" });
    }

    return null;
  }
});

/**
 * Cloud Function: Gửi yêu cầu huỷ Push Notification khi cuộc gọi kết thúc/bị huỷ.
 */
exports.onCallUpdated = onDocumentUpdated("calls/{callId}", async (event) => {
  const before = event.data.before.data();
  const after = event.data.after.data();
  const callId = event.params.callId;

  // Nếu chuyển từ "ringing" sang trạng thái khác (ended, declined, accepted, missed)
  // → gửi yêu cầu huỷ notification trên máy callee
  const terminalStatuses = ["ended", "declined", "accepted", "missed"];
  if (before.status === "ringing" && terminalStatuses.includes(after.status)) {
    const calleeId = after.calleeId;
    if (!calleeId) return null;

    const calleeDoc = await db.collection("users").doc(calleeId).get();
    if (!calleeDoc.exists) return null;

    const fcmToken = calleeDoc.data().fcmToken;
    if (!fcmToken) return null;

    const message = {
      token: fcmToken,
      data: {
        type: "cancel_call",
        callId: callId
      },
      android: {
        priority: "high"
      }
    };

    try {
      await messaging.send(message);
      console.log(`✅ FCM 'cancel_call' sent to ${calleeId} for call ${callId}`);
    } catch (error) {
      console.error(`❌ FCM 'cancel_call' send failed for call ${callId}:`, error);
    }
  }
});
