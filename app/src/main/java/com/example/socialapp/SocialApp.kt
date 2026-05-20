package com.example.socialapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SocialApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)

            // Channel cho cuộc gọi đến
            NotificationChannel(
                CHANNEL_CALL,
                "Cuộc gọi đến",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cuộc gọi đến"
                enableVibration(true)
                nm.createNotificationChannel(this)
            }

            // Channel cho tin nhắn
            NotificationChannel(
                CHANNEL_MESSAGE,
                "Tin nhắn",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo tin nhắn mới"
                nm.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val CHANNEL_CALL = "channel_call"
        const val CHANNEL_MESSAGE = "channel_message"
    }
}

