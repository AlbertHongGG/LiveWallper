package com.livewallpaper.app.domain.model

import java.io.File

/**
 * 遠端影片解析與下載進度狀態。
 */
sealed interface DownloadProgress {
    /** 正在準備或解析網址中（尚未能計算確切百分比） */
    data class Preparing(val stageMessage: String = "正在解析網址...") : DownloadProgress

    /** 下載中，帶有進度百分比 (0.0f ~ 100.0f) 與即時速度/預估時間 */
    data class Downloading(
        val progressPercent: Float,
        val speedText: String? = null,
        val etaText: String? = null
    ) : DownloadProgress

    /** 下載與轉碼完成，輸出最終影片檔案 */
    data class Completed(val outputFile: File) : DownloadProgress
}
