package com.livewallpaper.app.domain.model

import java.io.File
import java.util.Locale

data class VideoMetadata(
    val file: File,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L
) {
    val isLandscape: Boolean
        get() = width > height

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).toInt()
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }

    val formattedResolution: String
        get() = if (width > 0 && height > 0) "${width} × ${height}" else "未知解析度"

    val formattedFileSize: String
        get() {
            val bytes = fileSizeBytes
            return when {
                bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
                bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024f)
                else -> "$bytes B"
            }
        }
}
