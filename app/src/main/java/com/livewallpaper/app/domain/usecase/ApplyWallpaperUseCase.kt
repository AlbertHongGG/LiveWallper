package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import java.io.File

/**
 * 套用動態桌布用例。
 * 在首次啟用動態桌布前自動擷取使用者當前的靜態桌布快照備份，以利後續精準無縫還原。
 */
class ApplyWallpaperUseCase(
    private val storageManager: MediaStorageManager,
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperSystemManager: WallpaperSystemManager
) {
    suspend operator fun invoke(
        stagingFile: File,
        config: WallpaperConfig
    ): Result<WallpaperConfig> {
        return runCatching {
            // 首次套用前自動快照備份使用者原有的靜態照片桌布
            wallpaperSystemManager.captureCurrentWallpaperSnapshot()

            val activeFile = if (stagingFile.parentFile?.name == "wallpapers") {
                // 如果已經在正式目錄（例如重新微調當前桌布）
                stagingFile
            } else {
                storageManager.promoteStagingToActive(stagingFile).getOrThrow()
            }

            val newConfig = config.copy(
                videoFilePath = activeFile.absolutePath,
                updatedAtMillis = System.currentTimeMillis()
            )
            wallpaperRepository.saveConfig(newConfig)
            newConfig
        }
    }
}
