package com.livewallpaper.app.data.extractor

import com.livewallpaper.app.domain.model.ExtractorFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExtractorFailureClassifierTest {

    @Test
    fun `classify should map 403 Forbidden to EngineOutdated`() {
        val error = RuntimeException("ERROR: [Pinterest] 664421751319955179: Unable to download JSON metadata: HTTP Error 403: Forbidden")
        val failure = ExtractorFailureClassifier.classify(error)
        assertEquals(ExtractorFailure.EngineOutdated, failure)
    }

    @Test
    fun `classify should map 404 Not Found to EngineOutdated`() {
        val error = RuntimeException("HTTP Error 404: Not Found")
        val failure = ExtractorFailureClassifier.classify(error)
        assertEquals(ExtractorFailure.EngineOutdated, failure)
    }

    @Test
    fun `classify should map Requested format is not available to FormatUnavailable`() {
        val error = RuntimeException("ERROR: [Pinterest] 664421751319955179: Requested format is not available. Use --list-formats for a list of available formats")
        val failure = ExtractorFailureClassifier.classify(error)
        assertEquals(ExtractorFailure.FormatUnavailable, failure)
    }

    @Test
    fun `classify should map network errors to NetworkUnavailable`() {
        val error = RuntimeException("Failed to connect to www.pinterest.com: network is unreachable")
        val failure = ExtractorFailureClassifier.classify(error)
        assertEquals(ExtractorFailure.NetworkUnavailable, failure)
    }

    @Test
    fun `classify should map unsupported content to ContentUnsupported`() {
        val error = RuntimeException("Unsupported URL: this pin is private")
        val failure = ExtractorFailureClassifier.classify(error)
        assertEquals(ExtractorFailure.ContentUnsupported, failure)
    }

    @Test
    fun `classify should map linker and ffmpeg missing errors to ProcessingFailed`() {
        val linkerError = RuntimeException("CANNOT LINK EXECUTABLE libffmpeg.so: library libavdevice.so.61 not found")
        val failure1 = ExtractorFailureClassifier.classify(linkerError)
        assertEquals(ExtractorFailure.ProcessingFailed, failure1)

        val ffmpegError = RuntimeException("ffmpeg is not installed, unable to merge formats")
        val failure2 = ExtractorFailureClassifier.classify(ffmpegError)
        assertEquals(ExtractorFailure.ProcessingFailed, failure2)
    }

    @Test
    fun `classify should map engine initialization failures to EngineUnavailable`() {
        val initError = RuntimeException("FFmpeg initialization failed on device")
        val failure = ExtractorFailureClassifier.classify(initError)
        assertEquals(ExtractorFailure.EngineUnavailable, failure)
    }

    @Test
    fun `classify should map unknown errors to Unknown`() {
        val error = RuntimeException("Unexpected disk I/O error")
        val failure = ExtractorFailureClassifier.classify(error)
        assertTrue(failure is ExtractorFailure.Unknown)
    }
}
