package com.livewallpaper.app.domain.repository

/**
 * 系統級動態桌布管理器介面 (Dependency Inversion Principle)。
 * 抽象化 Android WallpaperManager 互動，提供快照備份、智慧精準還原與狀態查詢。
 */
interface WallpaperSystemManager {
    /**
     * 檢查當前系統桌面是否正由本應用的 VideoWallpaperService 託管。
     */
    fun isWallpaperServiceActive(): Boolean

    /**
     * 若當前系統為自訂靜態桌布且尚未快照，擷取並持久化備份至內部沙盒。
     * @return 是否成功完成快照捕獲或已存在快照
     */
    suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean>

    /**
     * 智慧還原桌布：
     * 1. 優先使用先前備份的快照點陣圖 100% 精準還原使用者原本的照片；
     * 2. 若無快照（原為動態桌布或讀取受限），則優雅回退至系統原廠預設桌布。
     */
    suspend fun restoreOriginalWallpaper(): Result<Unit>

    /**
     * 是否已有備份快照存在。
     */
    fun hasSnapshot(): Boolean
}
