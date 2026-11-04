package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import java.io.File

class ApplyWallpaperUseCase(
    private val storageManager: MediaStorageManager,
    private val wallpaperRepository: WallpaperRepository
) {
    suspend operator fun invoke(
        stagingFile: File,
        config: WallpaperConfig
    ): Result<WallpaperConfig> {
        return runCatching {
            val activeFile = if (stagingFile.absolutePath.contains("files/wallpapers/")) {
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
