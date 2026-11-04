package com.livewallpaper.app.presentation.features.history

import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.model.WallpaperSourceType
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.usecase.ClearWallpaperHistoryUseCase
import com.livewallpaper.app.domain.usecase.DeleteHistoryItemUseCase
import com.livewallpaper.app.domain.usecase.ResetWallpaperUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeWallpaperHistoryRepository : WallpaperHistoryRepository {
        val historyFlow = MutableStateFlow<List<WallpaperHistoryItem>>(emptyList())
        var deletedId: String? = null
        var clearCalled = false

        override val historyItems: Flow<List<WallpaperHistoryItem>> = historyFlow.asStateFlow()
        override suspend fun getHistoryItems(): List<WallpaperHistoryItem> = historyFlow.value
        override suspend fun recordHistory(item: WallpaperHistoryItem) {}
        override suspend fun deleteHistoryItem(id: String) {
            deletedId = id
            historyFlow.value = historyFlow.value.filter { it.id != id }
        }
        override suspend fun clearAllHistory(keepVideoPath: String?) {
            clearCalled = true
            historyFlow.value = if (keepVideoPath != null) {
                historyFlow.value.filter { it.videoFilePath == keepVideoPath }
            } else {
                emptyList()
            }
        }
    }

    private class FakeWallpaperRepository : WallpaperRepository {
        val configFlow = MutableStateFlow(WallpaperConfig(videoFilePath = "/data/active.mp4"))
        override val wallpaperConfig: Flow<WallpaperConfig> = configFlow.asStateFlow()
        override suspend fun getCurrentConfig(): WallpaperConfig = configFlow.value
        override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
            configFlow.value = transform(configFlow.value)
        }
        override suspend fun saveConfig(config: WallpaperConfig) {
            configFlow.value = config
        }
    }

    private class FakeMediaStorageManager : com.livewallpaper.app.domain.repository.MediaStorageManager {
        override suspend fun prepareStagingFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long): Result<com.livewallpaper.app.domain.model.ArchivedMediaAsset> {
            val f = File("/tmp/f.mp4")
            return Result.success(com.livewallpaper.app.domain.model.ArchivedMediaAsset("id", f, f, com.livewallpaper.app.domain.model.VideoMetadata(f)))
        }
        override suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean = true
        override suspend fun clearAllArchivedMedia(excludeVideoPath: String?) {}
        override suspend fun clearStaging() {}
        override suspend fun getStagingStats(): Pair<Long, Int> = Pair(0L, 0)
        override suspend fun getArchiveStats(): Pair<Long, Int> = Pair(0L, 0)
    }

    private class FakeWallpaperSystemManager : com.livewallpaper.app.domain.repository.WallpaperSystemManager {
        override fun isWallpaperServiceActive(): Boolean = false
        override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> = Result.success(true)
        override suspend fun restoreOriginalWallpaper(): Result<Unit> = Result.success(Unit)
        override fun hasSnapshot(): Boolean = false
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState correctly identifies active wallpaper and computes total size`() = runTest {
        val historyRepo = FakeWallpaperHistoryRepository()
        val wallpaperRepo = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()
        val systemManager = FakeWallpaperSystemManager()
        val resetUseCase = ResetWallpaperUseCase(storageManager, wallpaperRepo, systemManager)
        val deleteUseCase = DeleteHistoryItemUseCase(historyRepo, wallpaperRepo, resetUseCase)
        val clearUseCase = ClearWallpaperHistoryUseCase(historyRepo, wallpaperRepo, resetUseCase)

        val item1 = WallpaperHistoryItem(
            id = "id1",
            videoFilePath = "/data/active.mp4",
            thumbnailFilePath = "/thumb1.webp",
            sourceType = WallpaperSourceType.LOCAL_GALLERY,
            fileSizeBytes = 10 * 1024 * 1024L // 10 MB
        )
        val item2 = WallpaperHistoryItem(
            id = "id2",
            videoFilePath = "/data/other.mp4",
            thumbnailFilePath = "/thumb2.webp",
            sourceType = WallpaperSourceType.REMOTE_DOWNLOAD,
            fileSizeBytes = 5 * 1024 * 1024L // 5 MB
        )
        historyRepo.historyFlow.value = listOf(item1, item2)

        val viewModel = HistoryViewModel(historyRepo, wallpaperRepo, deleteUseCase, clearUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.totalCount)
        assertEquals("15.0 MB", state.totalSizeFormatted)
        assertEquals(2, state.items.size)
        assertTrue(state.items[0].isCurrentlyActive)
        assertFalse(state.items[1].isCurrentlyActive)
    }

    @Test
    fun `deleteItem calls deleteHistoryItemUseCase and removes item`() = runTest {
        val historyRepo = FakeWallpaperHistoryRepository()
        val wallpaperRepo = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()
        val systemManager = FakeWallpaperSystemManager()
        val resetUseCase = ResetWallpaperUseCase(storageManager, wallpaperRepo, systemManager)
        val deleteUseCase = DeleteHistoryItemUseCase(historyRepo, wallpaperRepo, resetUseCase)
        val clearUseCase = ClearWallpaperHistoryUseCase(historyRepo, wallpaperRepo, resetUseCase)

        val item = WallpaperHistoryItem(
            id = "item_del",
            videoFilePath = "/data/del.mp4",
            thumbnailFilePath = "/thumb.webp",
            sourceType = WallpaperSourceType.LOCAL_GALLERY
        )
        historyRepo.historyFlow.value = listOf(item)

        val viewModel = HistoryViewModel(historyRepo, wallpaperRepo, deleteUseCase, clearUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect {}
        }
        advanceUntilIdle()

        viewModel.deleteItem(item)
        advanceUntilIdle()

        assertEquals("item_del", historyRepo.deletedId)
        assertEquals(0, viewModel.uiState.value.totalCount)
    }
}
