package com.livewallpaper.app.data.extractor

import android.content.Context
import com.livewallpaper.app.domain.model.ExtractorFailure
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * 基於 youtubedl-android 與 FFmpeg 的底層執行引擎生命週期管理器。
 * 採用 Mutex 保證多協程存取時的線程安全，確保 Python, yt-dlp 與 FFmpeg 均初始化就緒。
 */
class YtDlpExtractorEngineManager(
    private val context: Context
) : ExtractorEngineManager {

    private val mutex = Mutex()
    private var isReady = false

    override suspend fun ensureReady(): Unit = withContext(Dispatchers.IO) {
        if (isReady) return@withContext
        mutex.withLock {
            if (!isReady) {
                try {
                    YoutubeDL.getInstance().init(context)
                } catch (e: Exception) {
                    throw WallpaperError.DownloadFailed(
                        reason = "YoutubeDL 核心初始化失敗: ${e.localizedMessage ?: e.message}",
                        failure = ExtractorFailure.EngineUnavailable,
                        cause = e
                    )
                }

                try {
                    FFmpeg.getInstance().init(context)
                } catch (e: Exception) {
                    throw WallpaperError.DownloadFailed(
                        reason = "FFmpeg 轉碼組件初始化失敗: ${e.localizedMessage ?: e.message}",
                        failure = ExtractorFailure.EngineUnavailable,
                        cause = e
                    )
                }

                isReady = true
            }
        }
    }

    override suspend fun getEngineVersion(): String = withContext(Dispatchers.IO) {
        try {
            ensureReady()
            YoutubeDL.getInstance().version(context) ?: "已就緒"
        } catch (_: Exception) {
            "未初始化"
        }
    }

    override suspend fun updateEngine(): Result<String> = withContext(Dispatchers.IO) {
        mutex.withLock {
            runCatching {
                ensureReady()
                val status = YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel._STABLE)
                val updatedVer = YoutubeDL.getInstance().version(context)
                "${status?.name ?: "已完成"} ($updatedVer)"
            }
        }
    }
}
