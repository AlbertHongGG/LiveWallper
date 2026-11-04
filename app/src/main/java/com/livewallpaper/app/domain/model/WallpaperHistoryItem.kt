package com.livewallpaper.app.domain.model

import com.livewallpaper.app.domain.model.common.MediaFormatters
import kotlinx.serialization.Serializable

/**
 * 歷史紀錄典藏項目。
 * 紀錄曾被正式套用過的桌布資產、元數據與使用者套用時的專屬配置。
 */
@Serializable
data class WallpaperHistoryItem(
    val id: String,
    val videoFilePath: String,
    val thumbnailFilePath: String,
    val sourceType: WallpaperSourceType,
    val sourceUrl: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val lastAppliedConfig: WallpaperConfig = WallpaperConfig(),
    val createdAtMillis: Long = System.currentTimeMillis(),
    val lastAppliedAtMillis: Long = System.currentTimeMillis()
) {
    val formattedDuration: String
        get() = MediaFormatters.formatDuration(durationMs)

    val formattedResolution: String
        get() = if (width > 0 && height > 0) "${width} × ${height}" else "未知解析度"

    val formattedFileSize: String
        get() = MediaFormatters.formatFileSize(fileSizeBytes)
}
