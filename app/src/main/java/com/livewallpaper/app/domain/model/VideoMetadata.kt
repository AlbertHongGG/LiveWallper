package com.livewallpaper.app.domain.model

import com.livewallpaper.app.domain.model.common.MediaFormatters
import java.io.File

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
        get() = MediaFormatters.formatDuration(durationMs)

    val formattedResolution: String
        get() = if (width > 0 && height > 0) "${width} × ${height}" else "未知解析度"

    val formattedFileSize: String
        get() = MediaFormatters.formatFileSize(fileSizeBytes)
}
