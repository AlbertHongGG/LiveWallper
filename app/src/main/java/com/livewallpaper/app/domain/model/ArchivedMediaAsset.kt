package com.livewallpaper.app.domain.model

import java.io.File

/**
 * 媒體封存至正式沙盒庫後的資產表示。
 */
data class ArchivedMediaAsset(
    val id: String,
    val videoFile: File,
    val thumbnailFile: File,
    val metadata: VideoMetadata
)
