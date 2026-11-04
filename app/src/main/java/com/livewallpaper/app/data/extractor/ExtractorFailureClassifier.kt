package com.livewallpaper.app.data.extractor

import com.livewallpaper.app.domain.model.ExtractorFailure

/**
 * 下載錯誤特徵分類器。
 * 將第三方底層例外精確映射為結構化的領域失敗類別。
 */
object ExtractorFailureClassifier {

    fun classify(throwable: Throwable): ExtractorFailure {
        val message = (throwable.message ?: throwable.localizedMessage ?: "").lowercase()

        return when {
            // 引擎環境或 FFmpeg 初始化異常
            message.contains("ffmpeg initialization failed") ||
            message.contains("failed to initialize youtubedl") ||
            message.contains("engine unavailable") -> ExtractorFailure.EngineUnavailable

            // 轉碼、合併或二進制連結異常
            message.contains("cannot link executable") ||
            message.contains("libavdevice") ||
            message.contains("libffmpeg") ||
            message.contains("ffmpeg is not installed") ||
            message.contains("postprocessing") ||
            message.contains("merging failed") -> ExtractorFailure.ProcessingFailed

            // 核心過舊 / API 封鎖 / 標頭拒絕 / JSON metadata 失敗
            message.contains("403") ||
            message.contains("forbidden") ||
            message.contains("unable to download json metadata") ||
            message.contains("signature extraction failed") ||
            message.contains("extractorerror") ||
            message.contains("404") -> ExtractorFailure.EngineOutdated

            // 格式無法滿足
            message.contains("requested format is not available") ||
            message.contains("format not available") -> ExtractorFailure.FormatUnavailable

            // 網路連線 / 逾時 / DNS
            message.contains("network is unreachable") ||
            message.contains("connection refused") ||
            message.contains("timed out") ||
            message.contains("unable to resolve host") ||
            message.contains("failed to connect") -> ExtractorFailure.NetworkUnavailable

            // 不支援內容 / 私人內容 / 非影片
            message.contains("this pin is private") ||
            message.contains("unsupported url") ||
            message.contains("no video formats found") -> ExtractorFailure.ContentUnsupported

            else -> ExtractorFailure.Unknown(throwable.message ?: "未知解析異常")
        }
    }
}
