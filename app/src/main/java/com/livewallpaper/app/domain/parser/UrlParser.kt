package com.livewallpaper.app.domain.parser

/**
 * 網址解析策略介面 (Strategy Pattern)。
 * 允許未來無縫新增 TikTok、Instagram、YouTube 等來源，符合開閉原則 (OCP)。
 */
interface UrlParser {
    /**
     * 判斷此解析器是否能處理傳入的原始輸入字串。
     */
    fun canHandle(rawInput: String): Boolean

    /**
     * 從原始文字（可能夾帶文案、標題、雜訊）中擷取乾淨合法的目標網址。
     * 若擷取失敗或不合法則回傳 null。
     */
    fun extractCleanUrl(rawInput: String): String?
}
