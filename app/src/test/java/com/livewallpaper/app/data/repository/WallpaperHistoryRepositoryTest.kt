package com.livewallpaper.app.data.repository

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

        suspend fun updateHistoryItems(transform: (List<WallpaperHistoryItem>) -> List<WallpaperHistoryItem>) {
            items = transform(items.toList()).toMutableList()
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
            fakeDataSource.updateHistoryItems { currentItems ->
                val list = currentItems.toMutableList()
                val existingIndex = list.indexOfFirst { it.id == item.id }

                if (existingIndex >= 0) {
                    val existing = list[existingIndex]
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
            fakeDataSource.updateHistoryItems { currentItems ->
                val list = currentItems.toMutableList()
                val targetIndex = list.indexOfFirst { it.id == id }
                if (targetIndex >= 0) {
                    targetToDelete = list.removeAt(targetIndex)
                }
                list
            }
            targetToDelete?.let { target ->
                storageManager.deleteArchivedMedia(target.videoFilePath, target.thumbnailFilePath)
            }
        }

        override suspend fun clearAllHistory(keepVideoPath: String?) {
            fakeDataSource.updateHistoryItems { currentItems ->
                if (keepVideoPath != null) {
                    currentItems.filter { it.videoFilePath == keepVideoPath }
                } else {
                    emptyList()
                }
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
    fun `recordHistory preserves original REMOTE_DOWNLOAD and sourceUrl when re-applying as LOCAL_GALLERY`() = runTest {
        val testFile = tempFolder.newFile("video_downloaded.mp4")
        val remoteItem = WallpaperHistoryItem(
            id = "pin_hash",
            videoFilePath = testFile.absolutePath,
            thumbnailFilePath = "/thumb_pin.webp",
            sourceType = WallpaperSourceType.REMOTE_DOWNLOAD,
            sourceUrl = "https://pinterest.com/pin/12345"
        )
        repository.recordHistory(remoteItem)

        // 使用者在主畫面點擊預覽舞台編輯並重新套用，傳入預設的 LOCAL_GALLERY 且 sourceUrl 為 null
        val reAppliedItem = WallpaperHistoryItem(
            id = "pin_hash",
            videoFilePath = testFile.absolutePath,
            thumbnailFilePath = "/thumb_pin.webp",
            sourceType = WallpaperSourceType.LOCAL_GALLERY,
            sourceUrl = null,
            lastAppliedConfig = WallpaperConfig(volume = 0.9f)
        )
        repository.recordHistory(reAppliedItem)

        val items = repository.getHistoryItems()
        assertEquals(1, items.size)
        val resultItem = items.first()
        assertEquals(WallpaperSourceType.REMOTE_DOWNLOAD, resultItem.sourceType)
        assertEquals("https://pinterest.com/pin/12345", resultItem.sourceUrl)
        assertEquals(0.9f, resultItem.lastAppliedConfig.volume, 0.001f)
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
