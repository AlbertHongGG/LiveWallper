package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager

/**
 * 清空並智慧還原桌布用例。
 * 原子性依序執行：
 * 1. 清理內部沙盒檔案 (staging / active)
 * 2. 清空 DataStore 中的桌布路徑
 * 3. 呼叫系統桌布管理器智慧還原使用者原有照片（無照片時優雅退回原廠預設）並解除 LiveWallpaperService 綁定
 */
class ResetWallpaperUseCase(
    private val storageManager: MediaStorageManager,
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperSystemManager: WallpaperSystemManager
) {
    suspend operator fun invoke(): Result<Unit> = runCatching {
        // 1. 清理儲存空間中殘存的活躍桌布檔案與暫存檔案
        storageManager.clearActiveWallpapers()

        // 2. 更新配置狀態為無影片
        wallpaperRepository.updateConfig { current ->
            current.copy(videoFilePath = "")
        }

        // 3. 智慧還原使用者原本的靜態照片（若無備份則優雅降級退回原廠桌面）
        wallpaperSystemManager.restoreOriginalWallpaper().getOrThrow()
    }
}
