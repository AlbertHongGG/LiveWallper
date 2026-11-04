package com.livewallpaper.app.data.extractor.runner

import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 基於 youtubedl-android 的真實執行器實作。
 */
class YoutubeDlAndroidRunner : YtDlpRunner {

    override suspend fun execute(
        request: YoutubeDLRequest,
        processId: String,
        onProgress: (progress: Float, etaInSeconds: Long, line: String?) -> Unit
    ): Unit = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
            onProgress(progress, etaInSeconds, line)
        }
    }

    override fun destroyProcessById(processId: String) {
        try {
            YoutubeDL.getInstance().destroyProcessById(processId)
        } catch (_: Exception) {}
    }
}
