package com.livewallpaper.app.domain.repository

/**
 * 系統級動態桌布管理器介面 (Dependency Inversion Principle)。
 * 抽象化 Android WallpaperManager 互動，隔離平台依賴，便於單元測試與架構維護。
 */
interface WallpaperSystemManager {
    /**
     * 重設並還原回系統出廠預設桌布，乾淨解除 LiveWallpaperService 的綁定。
     */
    suspend fun resetToSystemDefault(): Result<Unit>

    /**
     * 檢查當前系統桌面是否正由本應用的 VideoWallpaperService 託管。
     */
    fun isWallpaperServiceActive(): Boolean
}
