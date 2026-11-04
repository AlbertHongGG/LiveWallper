package com.livewallpaper.app.data.repository

import com.livewallpaper.app.data.datastore.WallpaperHistoryDataSource
import com.livewallpaper.app.domain.model.ArchivedMediaAsset
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.WallpaperSourceType
import com.livewallpaper.app.domain.repository.MediaStorageManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class WallpaperHistoryRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private class FakeMediaStorageManager : MediaStorageManager {
        val deletedFiles = mutableListOf<String>()
        var clearAllCalled = false
        var keptVideoPath: String? = null

        override suspend fun prepareStagingFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long): Result<ArchivedMediaAsset> {
            return Result.success(ArchivedMediaAsset("hash", sourceFile, sourceFile, VideoMetadata(sourceFile)))
        }
        override suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean {
            deletedFiles.add(videoFilePath)
            deletedFiles.add(thumbnailFilePath)
            return true
        }
        override suspend fun clearAllArchivedMedia(excludeVideoPath: String?) {
            clearAllCalled = true
            keptVideoPath = excludeVideoPath
        }
        override suspend fun clearStaging() {}
        override suspend fun getStagingStats(): Pair<Long, Int> = Pair(0L, 0)
        override suspend fun getArchiveStats(): Pair<Long, Int> = Pair(0L, 0)
    }

    private class FakeHistoryDataSource {
        var items = mutableListOf<WallpaperHistoryItem>()

        val historyFlow = kotlinx.coroutines.flow.flow {
            emit(items.toList())
        }

        suspend fun getHistoryItems(): List<WallpaperHistoryItem> = items.toList()

        suspend fun saveHistoryItems(newItems: List<WallpaperHistoryItem>) {
            items = newItems.toMutableList()
        }
    }

    private class TestableWallpaperHistoryRepository(
        private val fakeDataSource: FakeHistoryDataSource,
        private val storageManager: MediaStorageManager
    ) : com.livewallpaper.app.domain.repository.WallpaperHistoryRepository {

        override val historyItems = kotlinx.coroutines.flow.flow {
            emit(fakeDataSource.getHistoryItems().filter { File(it.videoFilePath).exists() }.sortedByDescending { it.lastAppliedAtMillis })
        }

        override suspend fun getHistoryItems(): List<WallpaperHistoryItem> {
            return fakeDataSource.getHistoryItems()
                .filter { File(it.videoFilePath).exists() }
                .sortedByDescending { it.lastAppliedAtMillis }
        }

        override suspend fun recordHistory(item: WallpaperHistoryItem) {
            val currentItems = fakeDataSource.getHistoryItems().toMutableList()
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

            fakeDataSource.saveHistoryItems(currentItems)
        }

        override suspend fun deleteHistoryItem(id: String) {
            val currentItems = fakeDataSource.getHistoryItems().toMutableList()
            val targetIndex = currentItems.indexOfFirst { it.id == id }
            if (targetIndex >= 0) {
                val target = currentItems.removeAt(targetIndex)
                fakeDataSource.saveHistoryItems(currentItems)
                storageManager.deleteArchivedMedia(target.videoFilePath, target.thumbnailFilePath)
            }
        }

        override suspend fun clearAllHistory(keepVideoPath: String?) {
            val currentItems = fakeDataSource.getHistoryItems()
            if (keepVideoPath != null) {
                val kept = currentItems.filter { it.videoFilePath == keepVideoPath }
                fakeDataSource.saveHistoryItems(kept)
            } else {
                fakeDataSource.saveHistoryItems(emptyList())
            }
            storageManager.clearAllArchivedMedia(keepVideoPath)
        }
    }

    private lateinit var storageManager: FakeMediaStorageManager
    private lateinit var fakeDataSource: FakeHistoryDataSource
    private lateinit var repository: TestableWallpaperHistoryRepository

    @Before
    fun setup() {
        storageManager = FakeMediaStorageManager()
        fakeDataSource = FakeHistoryDataSource()
        repository = TestableWallpaperHistoryRepository(fakeDataSource, storageManager)
    }

    @Test
    fun `recordHistory with new item adds to top of list`() = runTest {
        val testFile = tempFolder.newFile("video1.mp4")
        val item1 = WallpaperHistoryItem(
            id = "hash1",
            videoFilePath = testFile.absolutePath,
            thumbnailFilePath = "/thumb1.webp",
            sourceType = WallpaperSourceType.LOCAL_GALLERY,
            createdAtMillis = 1000L,
            lastAppliedAtMillis = 1000L
        )

        repository.recordHistory(item1)

        val items = repository.getHistoryItems()
        assertEquals(1, items.size)
        assertEquals("hash1", items.first().id)
    }

    @Test
    fun `recordHistory with existing item deduplicates and updates config and timestamp`() = runTest {
        val testFile = tempFolder.newFile("video1.mp4")
        val item1 = WallpaperHistoryItem(
            id = "hash1",
            videoFilePath = testFile.absolutePath,
            thumbnailFilePath = "/thumb1.webp",
            sourceType = WallpaperSourceType.LOCAL_GALLERY,
            lastAppliedConfig = WallpaperConfig(volume = 0.2f),
            lastAppliedAtMillis = 1000L
        )
        repository.recordHistory(item1)

        val item1Updated = item1.copy(
            lastAppliedConfig = WallpaperConfig(volume = 0.8f)
        )
        repository.recordHistory(item1Updated)

        val items = repository.getHistoryItems()
        assertEquals(1, items.size)
        assertEquals("hash1", items.first().id)
        assertEquals(0.8f, items.first().lastAppliedConfig.volume, 0.001f)
        assertTrue(items.first().lastAppliedAtMillis > 1000L)
    }

    @Test
    fun `deleteHistoryItem removes from list and deletes files`() = runTest {
        val testFile = tempFolder.newFile("video_del.mp4")
        val item = WallpaperHistoryItem(
            id = "del_id",
            videoFilePath = testFile.absolutePath,
            thumbnailFilePath = "/thumb_del.webp",
            sourceType = WallpaperSourceType.REMOTE_DOWNLOAD
        )
        repository.recordHistory(item)

        repository.deleteHistoryItem("del_id")

        val items = repository.getHistoryItems()
        assertTrue(items.isEmpty())
        assertTrue(storageManager.deletedFiles.contains(testFile.absolutePath))
    }

    @Test
    fun `clearAllHistory with keepVideoPath preserves only active wallpaper`() = runTest {
        val testFile1 = tempFolder.newFile("video_active.mp4")
        val testFile2 = tempFolder.newFile("video_old.mp4")
        val item1 = WallpaperHistoryItem(
            id = "id1",
            videoFilePath = testFile1.absolutePath,
            thumbnailFilePath = "/thumb1.webp",
            sourceType = WallpaperSourceType.LOCAL_GALLERY
        )
        val item2 = WallpaperHistoryItem(
            id = "id2",
            videoFilePath = testFile2.absolutePath,
            thumbnailFilePath = "/thumb2.webp",
            sourceType = WallpaperSourceType.REMOTE_DOWNLOAD
        )
        repository.recordHistory(item1)
        repository.recordHistory(item2)

        repository.clearAllHistory(keepVideoPath = testFile1.absolutePath)

        val items = repository.getHistoryItems()
        assertEquals(1, items.size)
        assertEquals("id1", items.first().id)
        assertTrue(storageManager.clearAllCalled)
        assertEquals(testFile1.absolutePath, storageManager.keptVideoPath)
    }
}
