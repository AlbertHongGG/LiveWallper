package com.livewallpaper.app.domain.model.common

import java.util.Locale

/**
 * 集中管理媒體時長與檔案大小之純領域格式化工具。
 * 遵循 DRY 原則，杜絕格式化邏輯散落於各模型與 ViewModel。
 */
object MediaFormatters {

    fun formatDuration(durationMs: Long): String {
        val totalSeconds = (durationMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
            else -> "$bytes B"
        }
    }
}
