package com.livewallpaper.app.presentation.features.settings

import android.content.Intent
import com.livewallpaper.app.domain.model.ArchivedMediaAsset
import com.livewallpaper.app.domain.model.VideoMetadata
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.model.WallpaperHistoryItem
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperHistoryRepository
import com.livewallpaper.app.domain.repository.WallpaperPermissionManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.domain.usecase.ClearWallpaperHistoryUseCase
import com.livewallpaper.app.domain.usecase.ResetWallpaperUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
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
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeWallpaperRepository : WallpaperRepository {
        private val _config = MutableStateFlow(WallpaperConfig(doubleTapToPause = true))
        override val wallpaperConfig: Flow<WallpaperConfig> = _config.asStateFlow()

        override suspend fun getCurrentConfig(): WallpaperConfig = _config.value

        override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
            _config.value = transform(_config.value)
        }

        override suspend fun saveConfig(config: WallpaperConfig) {
            _config.value = config
        }
    }

    private class FakeExtractorEngineManager : ExtractorEngineManager {
        var engineVersion = "2026.10.26"
        var updateEngineCalled = false
        var updateResult: Result<String> = Result.success("2026.11.04")

        override suspend fun ensureReady() {}

        override suspend fun updateEngine(): Result<String> {
            updateEngineCalled = true
            return updateResult.onSuccess { engineVersion = it }
        }

        override suspend fun getEngineVersion(): String = engineVersion
    }

    private class FakeWallpaperPermissionManager : WallpaperPermissionManager {
        var hasPermission = true

        override fun hasBackupPermission(): Boolean = hasPermission
        override fun createRequestPermissionIntent(): Intent = Intent()
    }

    private class FakeWallpaperSystemManager : WallpaperSystemManager {
        var hasSnapshotValue = true

        override fun isWallpaperServiceActive(): Boolean = true
        override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> = Result.success(true)
        override suspend fun restoreOriginalWallpaper(): Result<Unit> = Result.success(Unit)
        override fun hasSnapshot(): Boolean = hasSnapshotValue
    }

    private class FakeMediaStorageManager : MediaStorageManager {
        var stagingCleared = false
        var archiveCleared = false

        override suspend fun prepareStagingFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun archiveMedia(sourceFile: File, frameTimeMs: Long): Result<ArchivedMediaAsset> {
            val f = File("/tmp/media.mp4")
            return Result.success(ArchivedMediaAsset("id", f, f, VideoMetadata(f)))
        }
        override suspend fun deleteArchivedMedia(videoFilePath: String, thumbnailFilePath: String): Boolean = true
        override suspend fun clearAllArchivedMedia(excludeVideoPath: String?) {
            archiveCleared = true
        }
        override suspend fun clearStaging() {
            stagingCleared = true
        }
        override suspend fun getStagingStats(): Pair<Long, Int> = Pair(1024L * 1024L * 5, 2)
        override suspend fun getArchiveStats(): Pair<Long, Int> = Pair(1024L * 1024L * 25, 4)
    }

    private class FakeWallpaperHistoryRepository : WallpaperHistoryRepository {
        var clearCalled = false

        override val historyItems: Flow<List<WallpaperHistoryItem>> = flowOf(emptyList())
        override suspend fun getHistoryItems(): List<WallpaperHistoryItem> = emptyList()
        override suspend fun recordHistory(item: WallpaperHistoryItem) {}
        override suspend fun deleteHistoryItem(id: String) {}
        override suspend fun clearAllHistory(keepVideoPath: String?) {
            clearCalled = true
        }
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
    fun `init loads config, engine version, stats, and permission snapshot states properly`() = runTest {
        val repository = FakeWallpaperRepository()
        val extractorManager = FakeExtractorEngineManager()
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = true }
        val systemManager = FakeWallpaperSystemManager().apply { hasSnapshotValue = true }
        val storageManager = FakeMediaStorageManager()
        val historyRepository = FakeWallpaperHistoryRepository()
        val resetUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager)
        val clearHistoryUseCase = ClearWallpaperHistoryUseCase(historyRepository, repository, resetUseCase)

        val viewModel = SettingsViewModel(
            wallpaperRepository = repository,
            extractorEngineManager = extractorManager,
            mediaStorageManager = storageManager,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            clearWallpaperHistoryUseCase = clearHistoryUseCase
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026.10.26", state.ytDlpVersion)
        assertTrue(state.doubleTapToPause)
        assertTrue(state.hasBackupPermission)
        assertTrue(state.hasSnapshotBackup)
        assertEquals("5.0 MB", state.stagingSizeFormatted)
        assertEquals(2, state.stagingFilesCount)
        assertEquals("25.0 MB", state.archiveSizeFormatted)
        assertEquals(4, state.archiveFilesCount)
        assertFalse(state.isUpdatingYtDlp)
    }

    @Test
    fun `toggleDoubleTapToPause updates wallpaper repository`() = runTest {
        val repository = FakeWallpaperRepository()
        val extractorManager = FakeExtractorEngineManager()
        val permissionManager = FakeWallpaperPermissionManager()
        val systemManager = FakeWallpaperSystemManager()
        val storageManager = FakeMediaStorageManager()
        val historyRepository = FakeWallpaperHistoryRepository()
        val resetUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager)
        val clearHistoryUseCase = ClearWallpaperHistoryUseCase(historyRepository, repository, resetUseCase)

        val viewModel = SettingsViewModel(
            wallpaperRepository = repository,
            extractorEngineManager = extractorManager,
            mediaStorageManager = storageManager,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            clearWallpaperHistoryUseCase = clearHistoryUseCase
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.doubleTapToPause)

        viewModel.toggleDoubleTapToPause(false)
        advanceUntilIdle()

        assertFalse(repository.getCurrentConfig().doubleTapToPause)
        assertFalse(viewModel.uiState.value.doubleTapToPause)
    }

    @Test
    fun `clearStagingCache and clearArchive invoke respective storage and use case cleanups`() = runTest {
        val repository = FakeWallpaperRepository()
        val extractorManager = FakeExtractorEngineManager()
        val permissionManager = FakeWallpaperPermissionManager()
        val systemManager = FakeWallpaperSystemManager()
        val storageManager = FakeMediaStorageManager()
        val historyRepository = FakeWallpaperHistoryRepository()
        val resetUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager)
        val clearHistoryUseCase = ClearWallpaperHistoryUseCase(historyRepository, repository, resetUseCase)

        val viewModel = SettingsViewModel(
            wallpaperRepository = repository,
            extractorEngineManager = extractorManager,
            mediaStorageManager = storageManager,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            clearWallpaperHistoryUseCase = clearHistoryUseCase
        )

        advanceUntilIdle()

        viewModel.clearStagingCache()
        advanceUntilIdle()
        assertTrue(storageManager.stagingCleared)

        viewModel.clearArchive(preserveActive = true)
        advanceUntilIdle()
        assertTrue(historyRepository.clearCalled)
    }
}
