package com.livewallpaper.app.domain.parser

/**
 * 集中式遠端網址解析與淨化協調器。
 * 整合所有註冊的 UrlParser，為分享 Intent、剪貼簿與手動輸入提供單一、統一的正規化網址擷取入口。
 */
class SharedUrlResolver(
    private val parsers: List<UrlParser>
) {
    /**
     * 嘗試從原始字串中擷取第一個合法的乾淨網址。
     * 若字串中不含任何受支援平台的網址，則回傳 null。
     */
    fun resolveCleanUrl(rawInput: String): String? {
        if (rawInput.isBlank()) return null
        for (parser in parsers) {
            if (parser.canHandle(rawInput)) {
                val clean = parser.extractCleanUrl(rawInput)
                if (!clean.isNullOrBlank()) {
                    return clean
                }
            }
        }
        return null
    }

    /**
     * 針對輸入框文字進行智慧淨化：
     * 若文字中包含有效網址（例如分享文字「Take a look! 📌 https://pin.it/...」），
     * 則直接提取並僅保留乾淨網址；
     * 若使用者正在手動輸入或尚未形成完整網址，則保留原文字避免中斷輸入。
     */
    fun sanitizeInput(input: String): String {
        val resolved = resolveCleanUrl(input)
        return resolved ?: input
    }
}
