package com.livewallpaper.app.domain.repository

import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import kotlinx.coroutines.flow.Flow

/**
 * 歷史紀錄典藏儲存庫介面。
 */
interface WallpaperHistoryRepository {
    /** 依套用時間倒序排序的歷史紀錄流（自動過濾檔案不存在之無效項目） */
    val historyItems: Flow<List<WallpaperHistoryItem>>

    /** 取得當前所有有效的歷史紀錄清單 */
    suspend fun getHistoryItems(): List<WallpaperHistoryItem>

    /** 寫入或更新一筆歷史紀錄（同 ID 自動去重、更新配置與最後套用時間） */
    suspend fun recordHistory(item: WallpaperHistoryItem)

    /** 刪除指定 ID 的歷史紀錄並同步清理磁碟實體媒體 */
    suspend fun deleteHistoryItem(id: String)

    /** 清空所有歷史紀錄並同步清理典藏檔案（可選擇保留當前使用中的桌布路徑） */
    suspend fun clearAllHistory(keepVideoPath: String? = null)
}
