package com.example.socialapp.ui.call

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
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
    val permissions = if (callType == "video") {
        PermissionHelper.VIDEO_PERMISSIONS
    } else {
        PermissionHelper.AUDIO_PERMISSIONS
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.all { it.value }) onGranted() else onDenied()
    }

    content { launcher.launch(permissions) }
}

