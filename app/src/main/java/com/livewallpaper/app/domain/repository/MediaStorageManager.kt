package com.livewallpaper.app.domain.repository

import com.livewallpaper.app.domain.model.ArchivedMediaAsset
import java.io.File

/**
 * 媒體儲存與生命週期管理器。
 * 負責預覽暫存區 (Staging) 管理、本地檔案複製、內容定址封存 (CAS) 與典藏庫生命週期維護。
 */
interface MediaStorageManager {
    /** 準備全新的預覽暫存檔案 (`staging_preview.mp4`)，確保暫存目錄乾淨可用 */
    suspend fun prepareStagingFile(): File

    /** 從系統 Content URI 安全複製檔案至指定的沙盒目的檔案 */
    suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File>

    /**
     * 將來源檔案（通常為預覽暫存檔）以 SHA-256 內容定址封存至正式媒體庫，
     * 並自動產生 WebP 縮圖與提取元數據。
     * 封存完成後自動清理預覽暫存區。
     */
    suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long = 0L): Result<ArchivedMediaAsset>

    /** 刪除已封存的實體影片與對應縮圖 */
    suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean

    /** 清理所有已封存的媒體檔案（可選擇保留指定的使用中桌布檔案） */
    suspend fun clearAllArchivedMedia(excludeVideoPath: String? = null)

    /** 清理預覽暫存區 */
    suspend fun clearStaging()

    /** 取得預覽暫存區容量統計 (位元組大小, 檔案數量) */
    suspend fun getStagingStats(): Pair<Long, Int>

    /** 取得已封存典藏庫容量統計 (位元組大小, 檔案數量) */
    suspend fun getArchiveStats(): Pair<Long, Int>
}
