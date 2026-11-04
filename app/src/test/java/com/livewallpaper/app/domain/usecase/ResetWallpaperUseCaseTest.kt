package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.WallpaperConfig
import com.livewallpaper.app.domain.repository.MediaStorageManager
import com.livewallpaper.app.domain.repository.WallpaperRepository
import com.livewallpaper.app.domain.repository.WallpaperSystemManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ResetWallpaperUseCaseTest {

    private class FakeMediaStorageManager : MediaStorageManager {
        var clearCount = 0

        override suspend fun prepareStagingFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun promoteStagingToActive(stagingFile: File): Result<File> = Result.success(stagingFile)
        override suspend fun cleanupOldWallpapers(keepActiveFile: File) {}
        override suspend fun clearActiveWallpapers() {
            clearCount++
        }
    }

    private class FakeWallpaperRepository : WallpaperRepository {
        var current = WallpaperConfig(videoFilePath = "/data/video.mp4")

        override val wallpaperConfig: Flow<WallpaperConfig> = flowOf(current)
        override suspend fun getCurrentConfig(): WallpaperConfig = current
        override suspend fun updateConfig(transform: (WallpaperConfig) -> WallpaperConfig) {
            current = transform(current)
        }
        override suspend fun saveConfig(config: WallpaperConfig) {
            current = config
        }
    }

    private class FakeWallpaperSystemManager : WallpaperSystemManager {
        var restoreCount = 0
        var snapshotCaptured = false
        var hasSnapshotCached = true

        override fun isWallpaperServiceActive(): Boolean = false

        override suspend fun captureCurrentWallpaperSnapshot(): Result<Boolean> {
            snapshotCaptured = true
            hasSnapshotCached = true
            return Result.success(true)
        }

        override suspend fun restoreOriginalWallpaper(): Result<Unit> {
            restoreCount++
            hasSnapshotCached = false
            return Result.success(Unit)
        }

        override fun hasSnapshot(): Boolean = hasSnapshotCached
    }

    @Test
    fun `invoke should clear active files, reset videoFilePath and restore original wallpaper`() = runTest {
        val storageManager = FakeMediaStorageManager()
        val repository = FakeWallpaperRepository()
        val systemManager = FakeWallpaperSystemManager()
        val useCase = ResetWallpaperUseCase(storageManager, repository, systemManager)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, storageManager.clearCount)
        assertEquals("", repository.current.videoFilePath)
        assertEquals(1, systemManager.restoreCount)
        assertEquals(false, systemManager.hasSnapshot())
    }
}
