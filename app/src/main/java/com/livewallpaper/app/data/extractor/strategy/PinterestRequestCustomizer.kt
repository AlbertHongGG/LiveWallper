package com.livewallpaper.app.data.extractor.strategy

import com.yausername.youtubedl_android.YoutubeDLRequest
import java.net.URI

/**
 * Pinterest 專屬請求策略。
 * 注入 Pinterest 內部 API 驗證 Header 與標準瀏覽器 User-Agent。
 */
class PinterestRequestCustomizer : PlatformRequestCustomizer {

    companion object {
        const val HEADER_PWS_HANDLER = "X-Pinterest-PWS-Handler: www/[username].js"
        const val USER_AGENT_CHROME =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    override fun matches(url: String): Boolean {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return false
            host.contains("pinterest.") || host == "pin.it" || host.endsWith(".pin.it")
        } catch (_: Exception) {
            false
        }
    }

    override fun customize(request: YoutubeDLRequest, url: String) {
        request.apply {
            addOption("--add-header", HEADER_PWS_HANDLER)
            addOption("--user-agent", USER_AGENT_CHROME)
        }
    }
}
