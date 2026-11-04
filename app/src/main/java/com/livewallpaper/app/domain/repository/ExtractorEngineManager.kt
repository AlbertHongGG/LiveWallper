package com.livewallpaper.app.domain.repository

/**
 * 媒體解析底層核心引擎生命週期管理器。
 * 負責二進制執行檔 (Python, yt-dlp, FFmpeg) 初始化就緒狀態、版本檢驗與線上熱修復更新。
 */
interface ExtractorEngineManager {
    /** 確保底層二進制環境（含 Python, yt-dlp 及 FFmpeg）已就緒 */
    suspend fun ensureReady()

    /** 取得目前引擎版本資訊 */
    suspend fun getEngineVersion(): String

    /** 執行線上更新核心引擎二進制 */
    suspend fun updateEngine(): Result<String>
}
