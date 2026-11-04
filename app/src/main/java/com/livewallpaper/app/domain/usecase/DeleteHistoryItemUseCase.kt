package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperRepository

/**
 * 刪除歷史紀錄項目用例。
 * 若刪除的項目正是當前作用中的桌布，一併智慧解除作用中動態桌布並還原原生桌布。
 */
class DeleteHistoryItemUseCase(
    private val historyRepository: WallpaperHistoryRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val resetWallpaperUseCase: ResetWallpaperUseCase
) {
    suspend operator fun invoke(id: String, videoFilePath: String): Result<Unit> = runCatching {
        val currentConfig = wallpaperRepository.getCurrentConfig()
        val isCurrentlyActive = currentConfig.videoFilePath == videoFilePath

        historyRepository.deleteHistoryItem(id)

        if (isCurrentlyActive) {
            resetWallpaperUseCase().getOrThrow()
        }
    }
}
