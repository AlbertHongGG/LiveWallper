package com.livewallpaper.app.presentation.preview

import android.content.Intent
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.parser.PinterestUrlParser
import com.livewallpaper.app.domain.parser.SharedUrlResolver
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperPermissionManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import com.livewallpaper.app.domain.usecase.ApplyWallpaperUseCase
import com.livewallpaper.app.domain.usecase.ProcessVideoSourceUseCase
import com.livewallpaper.app.domain.usecase.ResetWallpaperUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
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
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeWallpaperPermissionManager : WallpaperPermissionManager {
        var hasPermission = false

        override fun hasBackupPermission(): Boolean = hasPermission
        override fun createRequestPermissionIntent(): Intent = Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION")
    }

    private class FakeWallpaperSystemManager : WallpaperSystemManager {
        var isServiceActiveReturn = false
        var snapshotCaptured = false
        var snapshotExists = false
        var restoreCount = 0

        override fun isWallpaperServiceActive(): Boolean = isServiceActiveReturn

        override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> {
            snapshotCaptured = true
            snapshotExists = true
            return Result.success(true)
        }

        override suspend fun restoreOriginalWallpaper(): Result<Unit> {
            restoreCount++
            snapshotExists = false
            return Result.success(Unit)
        }

        override fun hasSnapshot(): Boolean = snapshotExists
    }

    private class FakeWallpaperRepository : WallpaperRepository {
        var current = WallpaperConfig()

        override val wallpaperConfig: Flow<WallpaperConfig> = flowOf(current)
        override suspend fun getCurrentConfig(): WallpaperConfig = current
        override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
            current = transform(current)
        }
        override suspend fun saveConfig(config: WallpaperConfig) {
            current = config
        }
    }

    private class FakeMediaStorageManager : MediaStorageManager {
        override suspend fun prepareStagingFile(): File = File.createTempFile("staging", ".mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun promoteStagingToActive(stagingFile: File): Result<File> = Result.success(stagingFile)
        override suspend fun cleanupOldWallpapers(keepActiveFile: File) {}
        override suspend fun clearActiveWallpapers() {}
        override suspend fun getCacheStats(): Pair<Long, Int> = Pair(0L, 0)
        override suspend fun clearCache() {}
    }

    private class FakeMediaExtractor(
        var progressFlow: Flow<com.livewallpaper.app.domain.model.DownloadProgress> = flowOf()
    ) : com.livewallpaper.app.domain.repository.MediaExtractor {
        override fun extractAndDownload(url: String, targetFile: File): Flow<com.livewallpaper.app.domain.model.DownloadProgress> = progressFlow
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
    fun `applyWallpaper without permission and without snapshot should show permission prompt`() = runTest {
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = false }
        val systemManager = FakeWallpaperSystemManager().apply { snapshotExists = false }
        val repository = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()

        val resolver = SharedUrlResolver(listOf(PinterestUrlParser()))
        val applyUseCase = ApplyWallpaperUseCase(storageManager, repository, systemManager)
        val resetUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager)
        val processUseCase = ProcessVideoSourceUseCase(storageManager, FakeMediaExtractor(), resolver)

        val viewModel = MainViewModel(
            wallpaperRepository = repository,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            processVideoSourceUseCase = processUseCase,
            applyWallpaperUseCase = applyUseCase,
            resetWallpaperUseCase = resetUseCase,
            urlResolver = resolver
        )

        advanceUntilIdle()

        // 嘗試套用桌布
        val tempVideo = File.createTempFile("sample", ".mp4")
        viewModel.applyWallpaper(WallpaperConfig(videoFilePath = tempVideo.absolutePath)) {}

        assertTrue("Should prompt for permission when neither permission nor snapshot exists", viewModel.showPermissionPrompt.value)
    }

    @Test
    fun `checkAndCaptureSnapshot captures snapshot when permission is granted and no snapshot exists`() = runTest {
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = true }
        val systemManager = FakeWallpaperSystemManager().apply { snapshotExists = false }
        val repository = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()

        val resolver = SharedUrlResolver(listOf(PinterestUrlParser()))
        val applyUseCase = ApplyWallpaperUseCase(storageManager, repository, systemManager)
        val resetUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager)
        val processUseCase = ProcessVideoSourceUseCase(storageManager, FakeMediaExtractor(), resolver)

        val viewModel = MainViewModel(
            wallpaperRepository = repository,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            processVideoSourceUseCase = processUseCase,
            applyWallpaperUseCase = applyUseCase,
            resetWallpaperUseCase = resetUseCase,
            urlResolver = resolver
        )

        advanceUntilIdle()

        assertTrue(systemManager.snapshotCaptured)
        assertTrue(systemManager.hasSnapshot())
    }

    @Test
    fun `dismissPermissionPrompt with proceedWithoutBackup hides prompt`() = runTest {
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = false }
        val systemManager = FakeWallpaperSystemManager().apply { snapshotExists = false }
        val repository = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()

        val resolver = SharedUrlResolver(listOf(PinterestUrlParser()))
        val applyUseCase = ApplyWallpaperUseCase(storageManager, repository, systemManager)
        val resetUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager)
        val processUseCase = ProcessVideoSourceUseCase(storageManager, FakeMediaExtractor(), resolver)

        val viewModel = MainViewModel(
            wallpaperRepository = repository,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            processVideoSourceUseCase = processUseCase,
            applyWallpaperUseCase = applyUseCase,
            resetWallpaperUseCase = resetUseCase,
            urlResolver = resolver
        )

        val tempVideo = File.createTempFile("sample", ".mp4")
        viewModel.applyWallpaper(WallpaperConfig(videoFilePath = tempVideo.absolutePath)) {}
        assertTrue(viewModel.showPermissionPrompt.value)

        viewModel.dismissPermissionPrompt(proceedWithoutBackup = true)
        advanceUntilIdle()

        assertFalse(viewModel.showPermissionPrompt.value)
    }

    @Test
    fun `updateUrlInput with messy text containing pin-it url should sanitize to clean url`() = runTest {
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = true }
        val systemManager = FakeWallpaperSystemManager().apply { snapshotExists = true }
        val repository = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()
        val resolver = SharedUrlResolver(listOf(PinterestUrlParser()))

        val viewModel = MainViewModel(
            wallpaperRepository = repository,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            processVideoSourceUseCase = ProcessVideoSourceUseCase(storageManager, FakeMediaExtractor(), resolver),
            applyWallpaperUseCase = ApplyWallpaperUseCase(storageManager, repository, systemManager),
            resetWallpaperUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager),
            urlResolver = resolver
        )

        viewModel.updateUrlInput("Take a look! 📌 https://pin.it/4Yg9qhT9a")

        assertEquals("https://pin.it/4Yg9qhT9a", viewModel.urlInput.value)
    }

    @Test
    fun `handleVideoSource emits NavigateToEditor when download completes`() = runTest {
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = true }
        val systemManager = FakeWallpaperSystemManager().apply { snapshotExists = true }
        val repository = FakeWallpaperRepository()
        val storageManager = FakeMediaStorageManager()
        val resolver = SharedUrlResolver(listOf(PinterestUrlParser()))
        val sampleFile = File.createTempFile("editor_sample", ".mp4")
        val fakeExtractor = FakeMediaExtractor(
            progressFlow = flowOf(
                com.livewallpaper.app.domain.model.DownloadProgress.Preparing("準備中..."),
                com.livewallpaper.app.domain.model.DownloadProgress.Completed(sampleFile)
            )
        )

        val viewModel = MainViewModel(
            wallpaperRepository = repository,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager,
            processVideoSourceUseCase = ProcessVideoSourceUseCase(storageManager, fakeExtractor, resolver),
            applyWallpaperUseCase = ApplyWallpaperUseCase(storageManager, repository, systemManager),
            resetWallpaperUseCase = ResetWallpaperUseCase(storageManager, repository, systemManager),
            urlResolver = resolver
        )

        val effects = mutableListOf<MainUiEffect>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effect.collect { effects.add(it) }
        }

        viewModel.handleVideoSource(com.livewallpaper.app.domain.model.VideoSource.RemoteShared("https://pin.it/1XlyxF9YU"))
        advanceUntilIdle()

        assertTrue(effects.any { it is MainUiEffect.NavigateToEditor && it.filePath == sampleFile.absolutePath })
        job.cancel()
    }
}
