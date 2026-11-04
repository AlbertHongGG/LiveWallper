package com.livewallpaper.app.domain.usecase

import com.livewallpaper.app.domain.model.DownloadProgress
import com.livewallpaper.app.domain.model.MediaTaskProgress
import com.livewallpaper.app.domain.model.VideoSource
import com.livewallpaper.app.domain.model.WallpaperError
import com.livewallpaper.app.domain.model.WallpaperSourceType
import com.livewallpaper.app.domain.parser.SharedUrlResolver
import com.livewallpaper.app.domain.repository.MediaExtractor
import com.livewallpaper.app.domain.repository.MediaStorageManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * 處理影片來源並落入預覽暫存區 (Staging) 的 UseCase。
 * 統一抽象處理「本地媒體複製」與「遠端 Pinterest 解析下載」，輸出語意清晰且富含來源元數據之 [MediaTaskProgress]。
 */
class ProcessVideoSourceUseCase(
    private val storageManager: MediaStorageManager,
    private val mediaExtractor: MediaExtractor,
    private val urlResolver: SharedUrlResolver
) {
    operator fun invoke(source: VideoSource): Flow<MediaTaskProgress> = flow {
        val stagingFile = storageManager.prepareStagingFile()

        when (source) {
            is VideoSource.LocalUri -> {
                emit(MediaTaskProgress.LocalImport.Preparing("正在匯入本地影片..."))
                val result = storageManager.importFromContentUri(source.uriString, stagingFile)
                result.onSuccess { file ->
                    emit(MediaTaskProgress.LocalImport.Analyzing("正在驗證影片格式..."))
                    emit(
                        MediaTaskProgress.Completed(
                            outputFile = file,
                            sourceType = WallpaperSourceType.LOCAL_GALLERY,
                            resolvedUrl = null
                        )
                    )
                }.onFailure { throwable ->
                    throw throwable as? WallpaperError
                        ?: WallpaperError.StorageError("無法匯入本地檔案", throwable)
                }
            }

            is VideoSource.RemoteShared -> {
                emit(MediaTaskProgress.RemoteDownload.Preparing("正在辨識與正規化網址..."))
                val cleanUrl = urlResolver.resolveCleanUrl(source.rawInput)
                    ?: throw WallpaperError.InvalidUrl(source.rawInput)

                // 委託 MediaExtractor 串流下載至 staging 檔案
                mediaExtractor.extractAndDownload(cleanUrl, stagingFile).collect { downloadProgress ->
                    when (downloadProgress) {
                        is DownloadProgress.Preparing -> {
                            emit(MediaTaskProgress.RemoteDownload.Preparing(downloadProgress.stageMessage))
                        }
                        is DownloadProgress.SelfHealing -> {
                            emit(MediaTaskProgress.RemoteDownload.SelfHealing(downloadProgress.message))
                        }
                        is DownloadProgress.Downloading -> {
                            emit(
                                MediaTaskProgress.RemoteDownload.Downloading(
                                    progressPercent = downloadProgress.progressPercent,
                                    speedText = downloadProgress.speedText,
                                    etaText = downloadProgress.etaText
                                )
                            )
                        }
                        is DownloadProgress.Completed -> {
                            emit(
                                MediaTaskProgress.Completed(
                                    outputFile = downloadProgress.outputFile,
                                    sourceType = WallpaperSourceType.REMOTE_DOWNLOAD,
                                    resolvedUrl = cleanUrl
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
