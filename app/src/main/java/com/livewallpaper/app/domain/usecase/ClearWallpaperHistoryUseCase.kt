package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperRepository

/**
 * 清空所有歷史紀錄用例。
 * 可選擇是否保留當前使用中的桌布（預設保留以維持桌面運作）。
 */
class ClearWallpaperHistoryUseCase(
    private val historyRepository: WallpaperHistoryRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val resetWallpaperUseCase: ResetWallpaperUseCase
) {
    suspend operator fun invoke(preserveActiveWallpaper: Boolean = true): Result<Unit> = runCatching {
        val currentConfig = wallpaperRepository.getCurrentConfig()
        val activePath = if (preserveActiveWallpaper && currentConfig.hasVideo) currentConfig.videoFilePath else null

        historyRepository.clearAllHistory(keepVideoPath = activePath)

        if (!preserveActiveWallpaper && currentConfig.hasVideo) {
            resetWallpaperUseCase().getOrThrow()
        }
    }
}
