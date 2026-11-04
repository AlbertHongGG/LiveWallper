package com.livewallpaper.app.data.repository

import com.livewallpaper.app.data.datastore.WallpaperHistoryDataSource
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    override suspend fun getHistoryItems(): List<WallpaperHistoryItem> {
        return historyDataSource.getHistoryItems()
            .filter { File(it.videoFilePath).exists() }
            .sortedByDescending { it.lastAppliedAtMillis }
    }

    override suspend fun recordHistory(item: WallpaperHistoryItem) {
        val currentItems = historyDataSource.getHistoryItems().toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.id == item.id }

        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            val updated = existing.copy(
                thumbnailFilePath = if (item.thumbnailFilePath.isNotBlank()) item.thumbnailFilePath else existing.thumbnailFilePath,
                lastAppliedConfig = item.lastAppliedConfig,
                lastAppliedAtMillis = System.currentTimeMillis()
            )
            currentItems.removeAt(existingIndex)
            currentItems.add(0, updated)
        } else {
            currentItems.add(0, item)
        }

        historyDataSource.saveHistoryItems(currentItems)
    }

    override suspend fun deleteHistoryItem(id: String) {
        val currentItems = historyDataSource.getHistoryItems().toMutableList()
        val targetIndex = currentItems.indexOfFirst { it.id == id }
        if (targetIndex >= 0) {
            val target = currentItems.removeAt(targetIndex)
            historyDataSource.saveHistoryItems(currentItems)
            storageManager.deleteArchivedMedia(target.videoFilePath, target.thumbnailFilePath)
        }
    }

    override suspend fun clearAllHistory(keepVideoPath: String?) {
        val currentItems = historyDataSource.getHistoryItems()
        if (keepVideoPath != null) {
            val kept = currentItems.filter { it.videoFilePath == keepVideoPath }
            historyDataSource.saveHistoryItems(kept)
        } else {
            historyDataSource.saveHistoryItems(emptyList())
        }
        storageManager.clearAllArchivedMedia(keepVideoPath)
    }
}
