package com.example.socialapp.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

fun Timestamp?.toTimeString(): String {
    this ?: return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(this.toDate())
}

fun Timestamp?.toDateString(): String {
    this ?: return ""
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { time = this@toDateString!!.toDate() }
    return when {
        now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR) ->
            "Hôm nay"
        now.get(Calendar.DAY_OF_YEAR) - cal.get(Calendar.DAY_OF_YEAR) == 1 ->
            "Hôm qua"
        else ->
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(this.toDate())
    }
}

/** Tạo conversationId chuẩn từ 2 uid */
fun conversationIdFrom(uid1: String, uid2: String): String =
    listOf(uid1, uid2).sorted().joinToString("_")

