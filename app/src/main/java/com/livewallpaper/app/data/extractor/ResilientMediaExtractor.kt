package com.livewallpaper.app.data.extractor

import com.livewallpaper.app.data.extractor.policy.DownloadFormatPolicy
import com.livewallpaper.app.data.extractor.runner.YtDlpRunner
import com.livewallpaper.app.data.extractor.strategy.CompositeRequestCustomizer
import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.ExtractorFailure
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.repository.ExtractorEngineManager
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.util.UUID

/**
 * 具備自癒彈性與結構化異常處置的媒體下載器。
 * 協調格式偏好策略、平台特定自訂標頭、執行器與引擎熱升級自癒重試。
 */
class ResilientMediaExtractor(
    private val runner: YtDlpRunner,
    private val engineManager: ExtractorEngineManager,
    private val requestCustomizer: CompositeRequestCustomizer,
    private val formatPolicy: DownloadFormatPolicy = DownloadFormatPolicy()
) : MediaExtractor {

    override fun extractAndDownload(url: String, targetFile: File): Flow<DownloadProgress> = callbackFlow {
        var currentProcessId: String? = null

        try {
            engineManager.ensureInitialized()
            trySend(DownloadProgress.Preparing("正在解析影片來源資訊..."))

            targetFile.parentFile?.mkdirs()

            var attempts = 0
            val maxAttempts = 2
            var isSuccess = false

            while (attempts < maxAttempts && !isSuccess) {
                attempts++
                if (targetFile.exists()) {
                    targetFile.delete()
                }

                val processId = UUID.randomUUID().toString()
                currentProcessId = processId

                val request = YoutubeDLRequest(url).apply {
                    addOption("-o", targetFile.absolutePath)
                }
                // 1. 套用全域格式排序偏好與轉碼輸出格式
                formatPolicy.applyFormatOptions(request)
                // 2. 套用特定平台（如 Pinterest）自訂 Header 與 User-Agent
                requestCustomizer.customize(request, url)

                trySend(DownloadProgress.Downloading(progressPercent = 0f))

                try {
                    runner.execute(request, processId) { progress, etaInSeconds, line ->
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
                        isSuccess = true
                        trySend(DownloadProgress.Completed(targetFile))
                        close()
                        return@callbackFlow
                    } else {
                        throw WallpaperError.UnsupportedContent("未找到有效的影片串流或下載檔案為空")
                    }
                } catch (e: Exception) {
                    if (e is WallpaperError) {
                        throw e
                    }

                    val failure = ExtractorFailureClassifier.classify(e)
                    val canSelfHeal = attempts < maxAttempts && failure is ExtractorFailure.EngineOutdated

                    if (canSelfHeal) {
                        trySend(DownloadProgress.SelfHealing("偵測到解析組件需要升級，正在自動修復..."))
                        val updateResult = engineManager.updateEngine()
                        if (updateResult.isSuccess) {
                            // 自癒升級成功，進行第二次下載嘗試
                            continue
                        }
                    }

                    // 無法自癒或重試後依舊失敗，拋出攜帶結構化 failure 的領域錯誤
                    close(WallpaperError.DownloadFailed(
                        reason = e.localizedMessage ?: e.message ?: "下載失敗",
                        failure = failure,
                        cause = e
                    ))
                    return@callbackFlow
                }
            }
        } catch (e: Exception) {
            close(if (e is WallpaperError) e else WallpaperError.DownloadFailed(
                reason = e.localizedMessage ?: "下載處理異常",
                cause = e
            ))
        }

        awaitClose {
            currentProcessId?.let { pid ->
                runner.destroyProcessById(pid)
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun extractSpeedFromLine(line: String?): String? {
        if (line == null) return null
        val match = Regex("""at\s+([0-9.]+[kKMGT]?i?B/s)""").find(line)
        return match?.groupValues?.getOrNull(1)
    }
}
