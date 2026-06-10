package com.example.socialapp.ui.call

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.socialapp.util.PermissionHelper

/**
 * Composable helper yêu cầu quyền trước khi thực hiện cuộc gọi.
 * Gọi [onGranted] khi tất cả quyền đã được chấp nhận.
 * Gọi [onDenied] nếu bị từ chối.
 */
@Composable
fun RequestCallPermissions(
    callType: String,  // "voice" | "video"
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    content: @Composable (requestPermission: () -> Unit) -> Unit
) {
    val context = LocalContext.current
    val permissions = if (callType == "video") {
        PermissionHelper.VIDEO_PERMISSIONS
    } else {
        PermissionHelper.AUDIO_PERMISSIONS
    }

    // Kiểm tra xem quyền đã được cấp chưa
    val alreadyGranted = if (callType == "video") {
        PermissionHelper.hasVideoPermissions(context)
    } else {
        PermissionHelper.hasAudioPermissions(context)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) onGranted() else onDenied()
    }

    // Nếu quyền đã được cấp rồi, gọi onGranted ngay lập tức
    LaunchedEffect(alreadyGranted) {
        if (alreadyGranted) {
            onGranted()
        }
    }

    content {
        if (alreadyGranted) {
            onGranted()
        } else {
            launcher.launch(permissions)
        }
    }
}

