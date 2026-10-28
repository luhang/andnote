package com.localnotes.app.ui

import com.localnotes.app.data.NoteEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 列表标题：优先用标题，没有就用正文首行 */
fun NoteEntity.displayTitle(): String {
    title.trim().takeIf { it.isNotEmpty() }?.let { return it }
    val firstLine = content.lineSequence()
        .map { it.trim() }
        .firstOrNull { it.isNotEmpty() }
    return firstLine?.take(40) ?: "无标题笔记"
}

/** 时间显示：刚刚 / N 分钟前 / 今天 HH:mm / M月d日 / yyyy年M月d日 */
fun formatNoteTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff in 0L..59_000L) return "刚刚"
    if (diff in 60_000L..3_599_999L) return "${diff / 60_000} 分钟前"

    val nowCal = Calendar.getInstance().apply { timeInMillis = now }
    val tsCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val sameDay = nowCal.get(Calendar.YEAR) == tsCal.get(Calendar.YEAR) &&
        nowCal.get(Calendar.DAY_OF_YEAR) == tsCal.get(Calendar.DAY_OF_YEAR)

    return when {
        sameDay -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        nowCal.get(Calendar.YEAR) == tsCal.get(Calendar.YEAR) ->
            SimpleDateFormat("M月d日 HH:mm", Locale.CHINA).format(Date(timestamp))
        else -> SimpleDateFormat("yyyy年M月d日", Locale.CHINA).format(Date(timestamp))
    }
}

fun formatClock(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
