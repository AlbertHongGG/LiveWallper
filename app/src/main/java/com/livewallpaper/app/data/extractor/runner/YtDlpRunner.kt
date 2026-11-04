package com.livewallpaper.app.data.extractor.runner

import com.yausername.youtubedl_android.YoutubeDLRequest

/**
 * 抽象化之底層 yt-dlp 執行器介面。
 * 將 YoutubeDL 單例與進程控制隔離於領域與測試之外。
 */
interface YtDlpRunner {
    /**
     * 執行下載請求並回呼即時進度。
     * @param request 包含各項 CLI 參數之請求物件
     * @param processId 用於生命週期與取消控制之程序 ID
     * @param onProgress 即時進度回呼 (進度百分比, 預估剩餘秒數, 原始日誌行)
     */
    suspend fun execute(
        request: YoutubeDLRequest,
        processId: String,
        onProgress: (progress: Float, etaInSeconds: Long, line: String?) -> Unit
    )

    /**
     * 依據程序 ID 終止執行中之下載程序
     */
    fun destroyProcessById(processId: String)
}
