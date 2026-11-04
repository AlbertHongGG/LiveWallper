package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.VideoSource
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.parser.UrlParser
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.livewallpaper.app.domain.repository.MediaStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 處理影片來源並落入預覽暫存區 (Staging) 的 UseCase。
 * 統一抽象處理「本地媒體複製」與「遠端 Pinterest 解析下載」。
 */
class ProcessVideoSourceUseCase(
    private val storageManager: MediaStorageManager,
    private val mediaExtractor: MediaExtractor,
    private val urlParsers: List<UrlParser>
) {
    operator fun invoke(source: VideoSource): Flow<DownloadProgress> = flow {
        val stagingFile = storageManager.getStagingVideoFile()

        when (source) {
            is VideoSource.LocalUri -> {
                emit(DownloadProgress.Preparing("正在匯入本地影片..."))
                val result = storageManager.importFromContentUri(source.uriString, stagingFile)
                result.onSuccess { file ->
                    emit(DownloadProgress.Completed(file))
                }.onFailure { throwable ->
                    throw throwable as? WallpaperError
                        ?: WallpaperError.StorageError("無法匯入本地檔案", throwable)
                }
            }

            is VideoSource.RemoteShared -> {
                emit(DownloadProgress.Preparing("正在辨識與正規化網址..."))
                val cleanUrl = resolveCleanUrl(source.rawInput)
                    ?: throw WallpaperError.InvalidUrl(source.rawInput)

                // 委託 MediaExtractor 串流下載至 staging 檔案
                mediaExtractor.extractAndDownload(cleanUrl, stagingFile).collect { progress ->
                    emit(progress)
                }
            }
        }
    }

    private fun resolveCleanUrl(rawInput: String): String? {
        for (parser in urlParsers) {
            if (parser.canHandle(rawInput)) {
                val clean = parser.extractCleanUrl(rawInput)
                if (!clean.isNullOrBlank()) return clean
            }
        }
        return null
    }
}
