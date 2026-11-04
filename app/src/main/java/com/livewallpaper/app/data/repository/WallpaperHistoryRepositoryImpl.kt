package com.livewallpaper.app.data.repository

import com.livewallpaper.app.data.datastore.WallpaperHistoryDataSource
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.WallpaperSourceType
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class WallpaperHistoryRepositoryImpl(
    private val historyDataSource: WallpaperHistoryDataSource,
    private val storageManager: MediaStorageManager
) : WallpaperHistoryRepository {

    override val historyItems: Flow<List<WallpaperHistoryItem>> = historyDataSource.historyFlow
        .map { items ->
            // 自癒對帳：過濾掉實體檔案已被刪除的孤兒項目，並按最近套用時間倒序排序
            items.filter { File(it.videoFilePath).exists() }
                .sortedByDescending { it.lastAppliedAtMillis }
        }
        .flowOn(Dispatchers.IO)

    override suspend fun getHistoryItems(): List<WallpaperHistoryItem> = withContext(Dispatchers.IO) {
        historyDataSource.getHistoryItems()
            .filter { File(it.videoFilePath).exists() }
            .sortedByDescending { it.lastAppliedAtMillis }
    }

    override suspend fun recordHistory(item: WallpaperHistoryItem) {
        historyDataSource.updateHistoryItems { currentItems ->
            val list = currentItems.toMutableList()
            val existingIndex = list.indexOfFirst { it.id == item.id }

            if (existingIndex >= 0) {
                val existing = list[existingIndex]
                // 智慧保留既有的來源屬性：
                // 若既有項目為 REMOTE_DOWNLOAD 或已有 sourceUrl，而在主畫面或相簿微調再次套用時，絕不覆寫為 LOCAL_GALLERY / null
                val resolvedSourceType = if (existing.sourceType == WallpaperSourceType.REMOTE_DOWNLOAD) {
                    WallpaperSourceType.REMOTE_DOWNLOAD
                } else {
                    item.sourceType
                }
                val resolvedSourceUrl = existing.sourceUrl ?: item.sourceUrl

                val updated = existing.copy(
                    thumbnailFilePath = if (item.thumbnailFilePath.isNotBlank()) item.thumbnailFilePath else existing.thumbnailFilePath,
                    sourceType = resolvedSourceType,
                    sourceUrl = resolvedSourceUrl,
                    lastAppliedConfig = item.lastAppliedConfig,
                    lastAppliedAtMillis = System.currentTimeMillis()
                )
                list.removeAt(existingIndex)
                list.add(0, updated)
            } else {
                list.add(0, item)
            }
            list
        }
    }

    override suspend fun deleteHistoryItem(id: String) {
        var targetToDelete: WallpaperHistoryItem? = null
        historyDataSource.updateHistoryItems { currentItems ->
            val list = currentItems.toMutableList()
            val index = list.indexOfFirst { it.id == id }
            if (index >= 0) {
                targetToDelete = list.removeAt(index)
            }
            list
        }
        targetToDelete?.let { target ->
            storageManager.deleteArchivedMedia(target.videoFilePath, target.thumbnailFilePath)
        }
    }

    override suspend fun clearAllHistory(keepVideoPath: String?) {
        historyDataSource.updateHistoryItems { currentItems ->
            if (keepVideoPath != null) {
                currentItems.filter { it.videoFilePath == keepVideoPath }
            } else {
                emptyList()
            }
        }
        storageManager.clearAllArchivedMedia(keepVideoPath)
    }
}
