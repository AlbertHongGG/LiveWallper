package com.livewallpaper.app.data.extractor

import com.livewallpaper.app.data.extractor.runner.YtDlpRunner
import com.livewallpaper.app.data.extractor.strategy.CompositeRequestCustomizer
import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.ExtractorFailure
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class ResilientMediaExtractorTest {

    private class FakeYtDlpRunner(
        var executeBehavior: (request: YoutubeDLRequest, processId: String) -> Unit = { _, _ -> }
    ) : YtDlpRunner {
        var callCount = 0

        override suspend fun execute(
            request: YoutubeDLRequest,
            processId: String,
            onProgress: (progress: Float, etaInSeconds: Long, line: String?) -> Unit
        ) {
            callCount++
            executeBehavior(request, processId)
        }

        override fun destroyProcessById(processId: String) {}
    }

    private class FakeExtractorEngineManager : ExtractorEngineManager {
        var updateCallCount = 0
        var updateResult: Result<String> = Result.success("ok")

        override suspend fun ensureInitialized() {}
        override suspend fun getEngineVersion(): String = "test"
        override suspend fun updateEngine(): Result<String> {
            updateCallCount++
            return updateResult
        }
    }

    @Test
    fun `extractAndDownload should complete successfully on first attempt when runner succeeds`() = runTest {
        val targetFile = File.createTempFile("test_target", ".mp4")
        val runner = FakeYtDlpRunner { _, _ ->
            targetFile.writeText("sample video content")
        }
        val engineManager = FakeExtractorEngineManager()
        val customizer = CompositeRequestCustomizer(emptyList())
        val extractor = ResilientMediaExtractor(runner, engineManager, customizer)

        val events = extractor.extractAndDownload("https://pin.it/abc", targetFile).toList()

        assertEquals(1, runner.callCount)
        assertEquals(0, engineManager.updateCallCount)
        assertTrue(events.any { it is DownloadProgress.Completed })
    }

    @Test
    fun `extractAndDownload should self-heal and retry on 403 Forbidden`() = runTest {
        val targetFile = File.createTempFile("test_target", ".mp4")
        val runner = FakeYtDlpRunner()
        runner.executeBehavior = { _, _ ->
            if (runner.callCount == 1) {
                throw RuntimeException("HTTP Error 403: Forbidden")
            } else {
                targetFile.writeText("sample video content")
            }
        }
        val engineManager = FakeExtractorEngineManager()
        val customizer = CompositeRequestCustomizer(emptyList())
        val extractor = ResilientMediaExtractor(runner, engineManager, customizer)

        val events = extractor.extractAndDownload("https://pin.it/abc", targetFile).toList()

        assertEquals(2, runner.callCount)
        assertEquals(1, engineManager.updateCallCount)
        assertTrue(events.any { it is DownloadProgress.SelfHealing })
        assertTrue(events.any { it is DownloadProgress.Completed })
    }

    @Test
    fun `extractAndDownload should not self-heal on FormatUnavailable`() = runTest {
        val targetFile = File.createTempFile("test_target", ".mp4")
        val runner = FakeYtDlpRunner { _, _ ->
            throw RuntimeException("Requested format is not available")
        }
        val engineManager = FakeExtractorEngineManager()
        val customizer = CompositeRequestCustomizer(emptyList())
        val extractor = ResilientMediaExtractor(runner, engineManager, customizer)

        try {
            extractor.extractAndDownload("https://pin.it/abc", targetFile).toList()
            fail("Expected WallpaperError.DownloadFailed")
        } catch (e: WallpaperError.DownloadFailed) {
            assertEquals(ExtractorFailure.FormatUnavailable, e.failure)
            assertEquals(1, runner.callCount)
            assertEquals(0, engineManager.updateCallCount)
        }
    }
}
