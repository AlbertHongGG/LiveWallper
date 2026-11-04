package com.livewallpaper.app.presentation.features.settings

import android.content.Intent
import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.livewallpaper.app.domain.repository.WallpaperPermissionManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init loads config, engine version, and permission snapshot states properly`() = runTest {
        val repository = FakeWallpaperRepository()
        val extractorManager = FakeExtractorEngineManager()
        val permissionManager = FakeWallpaperPermissionManager().apply { hasPermission = true }
        val systemManager = FakeWallpaperSystemManager().apply { hasSnapshotValue = true }

        val viewModel = SettingsViewModel(
            wallpaperRepository = repository,
            extractorEngineManager = extractorManager,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2026.10.26", state.ytDlpVersion)
        assertTrue(state.doubleTapToPause)
        assertTrue(state.hasBackupPermission)
        assertTrue(state.hasSnapshotBackup)
        assertFalse(state.isUpdatingYtDlp)
    }

    @Test
    fun `toggleDoubleTapToPause updates wallpaper repository`() = runTest {
        val repository = FakeWallpaperRepository()
        val extractorManager = FakeExtractorEngineManager()
        val permissionManager = FakeWallpaperPermissionManager()
        val systemManager = FakeWallpaperSystemManager()

        val viewModel = SettingsViewModel(
            wallpaperRepository = repository,
            extractorEngineManager = extractorManager,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager
        )

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.doubleTapToPause)

        viewModel.toggleDoubleTapToPause(false)
        advanceUntilIdle()

        assertFalse(repository.getCurrentConfig().doubleTapToPause)
        assertFalse(viewModel.uiState.value.doubleTapToPause)
    }

    @Test
    fun `updateYtDlpEngine calls extractor manager and updates version state`() = runTest {
        val repository = FakeWallpaperRepository()
        val extractorManager = FakeExtractorEngineManager()
        val permissionManager = FakeWallpaperPermissionManager()
        val systemManager = FakeWallpaperSystemManager()

        val viewModel = SettingsViewModel(
            wallpaperRepository = repository,
            extractorEngineManager = extractorManager,
            wallpaperPermissionManager = permissionManager,
            wallpaperSystemManager = systemManager
        )

        advanceUntilIdle()

        viewModel.updateYtDlpEngine()
        advanceUntilIdle()

        assertTrue(extractorManager.updateEngineCalled)
        assertEquals("2026.11.04", viewModel.uiState.value.ytDlpVersion)
        assertFalse(viewModel.uiState.value.isUpdatingYtDlp)
    }
}
