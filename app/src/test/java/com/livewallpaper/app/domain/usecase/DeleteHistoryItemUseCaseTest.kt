package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.ArchivedMediaAsset
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeleteHistoryItemUseCaseTest {

    private class FakeWallpaperHistoryRepository : WallpaperHistoryRepository {
        var deletedId: String? = null

        override val historyItems: Flow<List<WallpaperHistoryItem>> = flowOf(emptyList())
        override suspend fun getHistoryItems(): List<WallpaperHistoryItem> = emptyList()
        override suspend fun recordHistory(item: WallpaperHistoryItem) {}
        override suspend fun deleteHistoryItem(id: String) {
            deletedId = id
        }
        override suspend fun clearAllHistory(keepVideoPath: String?) {}
    }

    private class FakeWallpaperRepository(var activePath: String = "") : WallpaperRepository {
        override val wallpaperConfig: Flow<WallpaperConfig> = flowOf(WallpaperConfig(videoFilePath = activePath))
        override suspend fun getCurrentConfig(): WallpaperConfig = WallpaperConfig(videoFilePath = activePath)
        override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
            val updated = transform(getCurrentConfig())
            activePath = updated.videoFilePath
        }
        override suspend fun saveConfig(config: WallpaperConfig) {
            activePath = config.videoFilePath
        }
    }

    private class FakeMediaStorageManager : MediaStorageManager {
        var clearStagingCount = 0

        override suspend fun prepareStagingFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long): Result<ArchivedMediaAsset> {
            val f = File("/tmp/media.mp4")
            return Result.success(ArchivedMediaAsset("id", f, f, VideoMetadata(f)))
        }
        override suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean = true
        override suspend fun clearAllArchivedMedia(excludeVideoPath: String?) {}
        override suspend fun clearStaging() { clearStagingCount++ }
        override suspend fun getStagingStats(): Pair<Long, Int> = Pair(0L, 0)
        override suspend fun getArchiveStats(): Pair<Long, Int> = Pair(0L, 0)
    }

    private class FakeWallpaperSystemManager : WallpaperSystemManager {
        var restoreCount = 0

        override fun isWallpaperServiceActive(): Boolean = false
        override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> = Result.success(true)
        override suspend fun restoreOriginalWallpaper(): Result<Unit> {
            restoreCount++
            return Result.success(Unit)
        }
        override fun hasSnapshot(): Boolean = false
    }

    @Test
    fun `delete inactive item deletes from history without resetting wallpaper`() = runTest {
        val historyRepo = FakeWallpaperHistoryRepository()
        val wallpaperRepo = FakeWallpaperRepository(activePath = "/data/active.mp4")
        val storageManager = FakeMediaStorageManager()
        val systemManager = FakeWallpaperSystemManager()
        val resetUseCase = ResetWallpaperUseCase(storageManager, wallpaperRepo, systemManager)
        val deleteUseCase = DeleteHistoryItemUseCase(historyRepo, wallpaperRepo, resetUseCase)

        val result = deleteUseCase(id = "item_1", videoFilePath = "/data/other.mp4")

        assertTrue(result.isSuccess)
        assertEquals("item_1", historyRepo.deletedId)
        assertEquals(0, systemManager.restoreCount)
        assertEquals("/data/active.mp4", wallpaperRepo.activePath)
    }

    @Test
    fun `delete active item deletes from history and resets wallpaper`() = runTest {
        val historyRepo = FakeWallpaperHistoryRepository()
        val wallpaperRepo = FakeWallpaperRepository(activePath = "/data/active.mp4")
        val storageManager = FakeMediaStorageManager()
        val systemManager = FakeWallpaperSystemManager()
        val resetUseCase = ResetWallpaperUseCase(storageManager, wallpaperRepo, systemManager)
        val deleteUseCase = DeleteHistoryItemUseCase(historyRepo, wallpaperRepo, resetUseCase)

        val result = deleteUseCase(id = "item_1", videoFilePath = "/data/active.mp4")

        assertTrue(result.isSuccess)
        assertEquals("item_1", historyRepo.deletedId)
        assertEquals(1, systemManager.restoreCount)
        assertEquals("", wallpaperRepo.activePath)
    }
}
