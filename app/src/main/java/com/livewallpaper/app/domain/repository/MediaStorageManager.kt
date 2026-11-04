package com.livewallpaper.app.domain.repository

import java.io.File

/**
 * 內部沙盒媒體檔案生命週期管理介面。
 * 負責預覽暫存、晉升正式桌布、與自動清理舊檔案。
 */
interface MediaStorageManager {
    /** 取得或建立預覽暫存檔案 (`staging_preview.mp4`) */
    fun getStagingVideoFile(): File

    /**
     * 從系統 Content URI 安全複製檔案至指定的沙盒目的檔案
     * @return 複製是否成功
     */
    suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File>

    /**
     * 將預覽完成的 staging 檔案原子性地晉升為正式桌布檔案 (`active_<timestamp>.mp4`)，
     * 並自動觸發清理舊桌布檔案以釋放手機空間。
     */
    suspend fun promoteStagingToActive(stagingFile: File): Result<File>

    /**
     * 清理舊的歷史桌布檔案，保留目前活躍的桌布檔案
     */
    suspend fun cleanupOldWallpapers(keepActiveFile: File)

    /**
     * 清理所有桌布與暫存檔案（重設桌布）
     */
    suspend fun clearActiveWallpapers()

    /**
     * 取得暫存快取目錄統計資訊 (總位元組大小, 檔案數量)
     */
    suspend fun getCacheStats(): Pair<Long, Int>

    /**
     * 清空下載與預覽暫存快取目錄
     */
    suspend fun clearCache()
}
