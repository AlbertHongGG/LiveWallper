package com.livewallpaper.app.data.extractor

import android.content.Context
import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class YtDlpMediaExtractor(
    private val context: Context
) : MediaExtractor {

    private var isInitialized = false

    private suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        if (!isInitialized) {
            try {
                YoutubeDL.getInstance().init(context)
                isInitialized = true
            } catch (_: Exception) {
                isInitialized = true
            }
        }
    }

    override fun extractAndDownload(url: String, targetFile: File): Flow<DownloadProgress> = callbackFlow {
        val processId = UUID.randomUUID().toString()

        try {
            ensureInitialized()
            trySend(DownloadProgress.Preparing("正在解析 Pinterest 影片資訊..."))

            targetFile.parentFile?.mkdirs()
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val request = YoutubeDLRequest(url).apply {
                addOption("-f", "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best")
                addOption("--no-playlist")
                addOption("--no-check-certificates")
                addOption("-o", targetFile.absolutePath)
            }

            trySend(DownloadProgress.Downloading(progressPercent = 0f))

            YoutubeDL.getInstance().execute(request, processId) { progress, etaInSeconds, line ->
                val etaText = if (etaInSeconds > 0) "${etaInSeconds}s" else null
                trySend(
                    DownloadProgress.Downloading(
                        progressPercent = progress.coerceIn(0f, 100f),
                        speedText = extractSpeedFromLine(line),
                        etaText = etaText
                    )
                )
            }

            if (targetFile.exists() && targetFile.length() > 0L) {
                trySend(DownloadProgress.Completed(targetFile))
                close()
            } else {
                close(WallpaperError.UnsupportedContent("未找到有效的影片串流或下載檔案為空"))
            }
        } catch (e: Exception) {
            close(WallpaperError.DownloadFailed("解析或下載失敗: ${e.localizedMessage ?: e.message}", e))
        }

        awaitClose {
            try {
                YoutubeDL.getInstance().destroyProcessById(processId)
            } catch (_: Exception) {}
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getEngineVersion(): String = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            val version = YoutubeDL.getInstance().version(context)
            version ?: "已就緒"
        } catch (_: Exception) {
            "未初始化"
        }
    }

    override suspend fun updateExtractorEngine(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            ensureInitialized()
            val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
            val updatedVer = YoutubeDL.getInstance().version(context)
            "${status?.name ?: "已完成"} ($updatedVer)"
        }
    }

    override suspend fun getCacheStats(): Pair<Long, Int> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "staging")
            val files = cacheDir.listFiles() ?: emptyArray()
            val totalSize = files.sumOf { it.length() }
            Pair(totalSize, files.size)
        } catch (_: Exception) {
            Pair(0L, 0)
        }
    }

    override suspend fun clearCache(): Unit = withContext(Dispatchers.IO) {
        try {
            val cacheDir = File(context.cacheDir, "staging")
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        Unit
    }

    private fun extractSpeedFromLine(line: String?): String? {
        if (line == null) return null
        val match = Regex("""at\s+([0-9.]+[kKMGT]?i?B/s)""").find(line)
        return match?.groupValues?.getOrNull(1)
    }
}
