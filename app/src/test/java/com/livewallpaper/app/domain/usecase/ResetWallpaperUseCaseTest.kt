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

        override fun getStagingVideoFile(): File = File("/tmp/staging.mp4")
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
        var resetCount = 0
        var isActive = true

        override suspend fun resetToSystemDefault(): Result<Unit> {
            resetCount++
            isActive = false
            return Result.success(Unit)
        }

        override fun isWallpaperServiceActive(): Boolean = isActive
    }

    @Test
    fun `invoke should clear active files, reset videoFilePath and reset system wallpaper to default`() = runTest {
        val storageManager = FakeMediaStorageManager()
        val repository = FakeWallpaperRepository()
        val systemManager = FakeWallpaperSystemManager()
        val useCase = ResetWallpaperUseCase(storageManager, repository, systemManager)

        val result = useCase()

        assertTrue(result.isSuccess)
        assertEquals(1, storageManager.clearCount)
        assertEquals("", repository.current.videoFilePath)
        assertEquals(1, systemManager.resetCount)
        assertEquals(false, systemManager.isWallpaperServiceActive())
    }
}
