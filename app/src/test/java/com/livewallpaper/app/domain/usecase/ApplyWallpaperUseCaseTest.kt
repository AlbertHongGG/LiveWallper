package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.ArchivedMediaAsset
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.WallpaperSourceType
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

class ApplyWallpaperUseCaseTest {

    private class FakeMediaStorageManager : MediaStorageManager {
        override suspend fun prepareStagingFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long): Result<ArchivedMediaAsset> {
            val video = File("/data/wallpapers/videos/video_abc123.mp4")
            val thumb = File("/data/wallpapers/thumbnails/thumb_abc123.webp")
            return Result.success(
                ArchivedMediaAsset(
                    id = "abc123hash",
                    videoFile = video,
                    thumbnailFile = thumb,
                    metadata = VideoMetadata(file = video, width = 1080, height = 1920, durationMs = 15000L)
                )
            )
        }
        override suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean = true
        override suspend fun clearAllArchivedMedia(excludeVideoPath: String?) {}
        override suspend fun clearStaging() {}
        override suspend fun getStagingStats(): Pair<Long, Int> = Pair(0L, 0)
        override suspend fun getArchiveStats(): Pair<Long, Int> = Pair(0L, 0)
    }

    private class FakeWallpaperRepository : WallpaperRepository {
        var saved: WallpaperConfig? = null

        override val wallpaperConfig: Flow<WallpaperConfig> = flowOf(WallpaperConfig())
        override suspend fun getCurrentConfig(): WallpaperConfig = saved ?: WallpaperConfig()
        override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
            saved = transform(saved ?: WallpaperConfig())
        }
        override suspend fun saveConfig(config: WallpaperConfig) {
            saved = config
        }
    }

    private class FakeWallpaperSystemManager : WallpaperSystemManager {
        var captureCount = 0

        override fun isWallpaperServiceActive(): Boolean = false
        override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> {
            captureCount++
            return Result.success(true)
        }
        override suspend fun restoreOriginalWallpaper(): Result<Unit> = Result.success(Unit)
        override fun hasSnapshot(): Boolean = captureCount > 0
    }

    private class FakeWallpaperHistoryRepository : WallpaperHistoryRepository {
        val records = mutableListOf<WallpaperHistoryItem>()

        override val historyItems: Flow<List<WallpaperHistoryItem>> = flowOf(records)
        override suspend fun getHistoryItems(): List<WallpaperHistoryItem> = records
        override suspend fun recordHistory(item: WallpaperHistoryItem) {
            records.add(0, item)
        }
        override suspend fun deleteHistoryItem(id: String) {
            records.removeAll { it.id == id }
        }
        override suspend fun clearAllHistory(keepVideoPath: String?) {
            records.clear()
        }
    }

    @Test
    fun `invoke should capture snapshot, archive media, update config and write history`() = runTest {
        val storageManager = FakeMediaStorageManager()
        val repository = FakeWallpaperRepository()
        val systemManager = FakeWallpaperSystemManager()
        val historyRepository = FakeWallpaperHistoryRepository()
        val useCase = ApplyWallpaperUseCase(storageManager, repository, systemManager, historyRepository)

        val expectedFile = File("/data/wallpapers/videos/video_abc123.mp4")
        val stagingFile = File("/data/staging/temp.mp4")
        val result = useCase(
            sourceFile = stagingFile,
            config = WallpaperConfig(),
            sourceType = WallpaperSourceType.LOCAL_GALLERY
        )

        assertTrue(result.isSuccess)
        assertEquals(1, systemManager.captureCount)
        assertEquals(expectedFile.absolutePath, result.getOrNull()?.videoFilePath)
        assertEquals(expectedFile.absolutePath, repository.saved?.videoFilePath)
        assertEquals(1, historyRepository.records.size)
        assertEquals("abc123hash", historyRepository.records.first().id)
        assertEquals(WallpaperSourceType.LOCAL_GALLERY, historyRepository.records.first().sourceType)
    }
}
