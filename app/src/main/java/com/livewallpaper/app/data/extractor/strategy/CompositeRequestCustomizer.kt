package com.livewallpaper.app.data.extractor.strategy

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * 複合型平台請求自訂器。
 * 依序對匹配該 URL 之所有平台策略執行參數自訂。
 */
class CompositeRequestCustomizer(
    private val customizers: List<PlatformRequestCustomizer>
) {
    fun customize(request: YoutubeDLRequest, url: String) {
        for (customizer in customizers) {
            if (customizer.matches(url)) {
                customizer.customize(request, url)
            }
        }
    }
}
