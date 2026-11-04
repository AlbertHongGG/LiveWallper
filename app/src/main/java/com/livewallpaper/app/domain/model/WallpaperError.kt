package com.livewallpaper.app.domain.model

/**
 * 系統領域錯誤定義。
 */
sealed class WallpaperError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /** 網址格式無效或無法從字串中擷取支援的 URL */
    data class InvalidUrl(val rawInput: String) : WallpaperError("無效的分享連結: $rawInput")

    /** 該貼文並非影片（例如純圖片 Pin）或未包含可播放之串流 */
    data class UnsupportedContent(val details: String) : WallpaperError("無法取得影片內容: $details")

    /** yt-dlp 下載或網路連線失敗，攜帶結構化之失敗類別 */
    data class DownloadFailed(
        val reason: String,
        val failure: ExtractorFailure = ExtractorFailure.Unknown(reason),
        override val cause: Throwable? = null
    ) : WallpaperError("影片下載失敗: $reason", cause)

    /** 本地存取、複製或空間不足異常 */
    data class StorageError(val reason: String, override val cause: Throwable? = null) :
        WallpaperError("儲存空間或檔案處理失敗: $reason", cause)

    /** 播放器解碼或初始化異常 */
    data class PlaybackError(val reason: String, override val cause: Throwable? = null) :
        WallpaperError("影片解碼播放失敗: $reason", cause)
}
