package com.livewallpaper.app.domain.parser

import java.net.URI

/**
 * Pinterest 專用網址擷取與正規化解析器。
 *
 * 支援格式範例：
 * - 短網址：https://pin.it/7x3Yz9A
 * - 標準貼文：https://www.pinterest.com/pin/123456789012345678/
 * - 國際網址：https://tw.pinterest.com/pin/123456789012345678/
 * - 混合分享文字：「來看看這篇釘圖：https://pin.it/3kLmNoP (來自 Pinterest)」
 */
class PinterestUrlParser : UrlParser {

    private val urlExtractionRegex = Regex(
        """https?://[a-zA-Z0-9\-._~:/?#\[\]@!$&'()*+,;=%]+""",
        RegexOption.IGNORE_CASE
    )

    override fun canHandle(rawInput: String): Boolean {
        if (rawInput.isBlank()) return false
        val extracted = extractCleanUrl(rawInput)
        return !extracted.isNullOrBlank()
    }

    override fun extractCleanUrl(rawInput: String): String? {
        if (rawInput.isBlank()) return null

        // 尋找文字內所有 URL 片段
        val matches = urlExtractionRegex.findAll(rawInput)
        for (match in matches) {
            val potentialUrl = cleanUrlArtifacts(match.value)
            if (isPinterestUrl(potentialUrl)) {
                return potentialUrl
            }
        }
        return null
    }

    private fun isPinterestUrl(url: String): Boolean {
        return try {
            val uri = URI(url)
            val host = uri.host?.lowercase() ?: return false
            val path = uri.path ?: ""

            when {
                // 短網址：pin.it/<id>
                host == "pin.it" || host.endsWith(".pin.it") -> {
                    path.length > 1
                }
                // 主網域：pinterest.com, www.pinterest.com, tw.pinterest.com 等
                host.contains("pinterest.") -> {
                    path.contains("/pin/") || path.contains("/sent/")
                }
                else -> false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 去除 URL 尾部可能伴隨的中文或英文標點符號（如括號、句號、引號）
     */
    private fun cleanUrlArtifacts(url: String): String {
        var clean = url.trim()
        val trailingPunctuation = listOf(')', ']', '}', '>', '。', '，', '、', '！', '!', '？', '?', '\"', '\'')
        while (clean.isNotEmpty() && trailingPunctuation.contains(clean.last())) {
            clean = clean.dropLast(1)
        }
        return clean
    }
}
