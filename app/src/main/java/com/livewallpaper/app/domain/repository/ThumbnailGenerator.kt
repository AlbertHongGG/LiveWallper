package com.livewallpaper.app.domain.repository

import java.io.File

/**
 * 影片縮圖生成器介面。
 * 負責從影片指定時間點擷取影格並壓縮儲存為輕量靜態圖檔。
 */
interface ThumbnailGenerator {
    suspend fun generateThumbnail(
        videoFile: File,
        destinationFile: File,
        frameTimeMs: Long = 0L,
        maxDimension: Int = 960
    ): Result<File>
}
