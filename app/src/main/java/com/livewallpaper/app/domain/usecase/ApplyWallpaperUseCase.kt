package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.WallpaperSourceType
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import java.io.File

/**
 * 套用動態桌布用例。
 * 1. 首次套用前自動快照備份使用者原有的靜態照片桌布。
 * 2. 以 SHA-256 內容定址方式封存影片並生成 WebP 縮圖。
 * 3. 儲存全域桌布配置。
 * 4. 寫入或更新歷史典藏紀錄（保留完整自訂配置）。
 */
class ApplyWallpaperUseCase(
    private val storageManager: MediaStorageManager,
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperSystemManager: WallpaperSystemManager,
    private val historyRepository: WallpaperHistoryRepository
) {
    suspend operator fun invoke(
        sourceFile: File,
        config: WallpaperConfig,
        sourceType: WallpaperSourceType = WallpaperSourceType.LOCAL_GALLERY,
        sourceUrl: String? = null
    ): Result<WallpaperConfig> {
        return runCatching {
            // 首次套用前自動快照備份使用者原有的靜態照片桌布
            wallpaperSystemManager.captureCurrentWallpaperSnapshot()

            val frameTime = if (config.enableTrim) config.trimStartMs else 0L
            val asset = storageManager.archiveMedia(sourceFile, frameTimeMs = frameTime).getOrThrow()

            val newConfig = config.copy(
                videoFilePath = asset.videoFile.absolutePath,
                updatedAtMillis = System.currentTimeMillis()
            )
            wallpaperRepository.saveConfig(newConfig)

            val historyItem = WallpaperHistoryItem(
                id = asset.id,
                videoFilePath = asset.videoFile.absolutePath,
                thumbnailFilePath = asset.thumbnailFile.absolutePath,
                sourceType = sourceType,
                sourceUrl = sourceUrl,
                width = asset.metadata.width,
                height = asset.metadata.height,
                durationMs = asset.metadata.durationMs,
                fileSizeBytes = asset.metadata.fileSizeBytes,
                lastAppliedConfig = newConfig,
                createdAtMillis = System.currentTimeMillis(),
                lastAppliedAtMillis = System.currentTimeMillis()
            )
            historyRepository.recordHistory(historyItem)

            newConfig
        }
    }
}
