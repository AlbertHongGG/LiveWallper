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

class ApplyWallpaperUseCaseTest {

    private class FakeMediaStorageManager : MediaStorageManager {
        override fun getStagingVideoFile(): File = File("/tmp/staging.mp4")
        override suspend fun importFromContentUri(uriString: String, destinationFile: File): Result<File> = Result.success(destinationFile)
        override suspend fun promoteStagingToActive(stagingFile: File): Result<File> = Result.success(File("/data/wallpapers/active_123.mp4"))
        override suspend fun cleanupOldWallpapers(keepActiveFile: File) {}
        override suspend fun clearActiveWallpapers() {}
        override suspend fun getCacheStats(): Pair<Long, Int> = Pair(0L, 0)
        override suspend fun clearCache() {}
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

    @Test
    fun `invoke should capture original snapshot and promote staging to active`() = runTest {
        val storageManager = FakeMediaStorageManager()
        val repository = FakeWallpaperRepository()
        val systemManager = FakeWallpaperSystemManager()
        val useCase = ApplyWallpaperUseCase(storageManager, repository, systemManager)

        val expectedFile = File("/data/wallpapers/active_123.mp4")
        val stagingFile = File("/data/staging/temp.mp4")
        val result = useCase(stagingFile, WallpaperConfig())

        assertTrue(result.isSuccess)
        assertEquals(1, systemManager.captureCount)
        assertEquals(expectedFile.absolutePath, result.getOrNull()?.videoFilePath)
        assertEquals(expectedFile.absolutePath, repository.saved?.videoFilePath)
    }
}
