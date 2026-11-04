package com.livewallpaper.app.data.extractor.strategy

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * 平台特定下載參數自訂策略。
 * 針對不同影片來源（如 Pinterest、通用等）注入防爬蟲 Header、User-Agent 或最佳格式參數。
 */
interface PlatformRequestCustomizer {
    /** 判斷該策略是否適用此 URL */
    fun matches(url: String): Boolean

    /** 針對此 URL 套用特定參數至 YoutubeDLRequest */
    fun customize(request: YoutubeDLRequest, url: String)
}
