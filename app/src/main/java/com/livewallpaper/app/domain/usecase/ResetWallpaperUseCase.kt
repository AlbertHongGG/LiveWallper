package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperRepository

/**
 * 清空並重設動態桌布用例。
 * 清理儲存空間中殘存的活躍桌布檔案與暫存檔案，並將 DataStore 桌布路徑重置。
 */
class ResetWallpaperUseCase(
    private val storageManager: MediaStorageManager,
    private val wallpaperRepository: WallpaperRepository
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        storageManager.clearActiveWallpapers()
        wallpaperRepository.updateConfig { current ->
            current.copy(videoFilePath = "")
        }
    }
}
