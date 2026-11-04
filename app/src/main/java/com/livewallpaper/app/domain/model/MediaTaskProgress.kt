package com.livewallpaper.app.domain.model

import java.io.File

/**
 * 媒體處理與載入任務狀態。
 * 明確區分「遠端下載任務」與「本地相簿匯入任務」，消滅語意混亂與錯誤的使用者提示。
 */
sealed interface MediaTaskProgress {

    /** 本地影片匯入階段 */
    sealed interface LocalImport : MediaTaskProgress {
        val message: String

        /** 正在自 ContentProvider 複製或串流寫入沙盒 */
        data class Preparing(override val message: String = "正在匯入本地影片...") : LocalImport

        /** 正在驗證視訊格式與解碼相容性 */
        data class Analyzing(override val message: String = "正在驗證影片格式...") : LocalImport
    }

    /** 遠端網路解析與下載階段 */
    sealed interface RemoteDownload : MediaTaskProgress {
        /** 正在辨識、重定向或正規化網址 */
        data class Preparing(val stageMessage: String = "正在辨識與解析網址...") : RemoteDownload

        /** 偵測到解析引擎需要升級，正在無縫自動修復 */
        data class SelfHealing(val message: String = "正在更新解析組件...") : RemoteDownload

        /** 正在自網路下載串流，攜帶進度百分比與即時速率 */
        data class Downloading(
            val progressPercent: Float,
            val speedText: String? = null,
            val etaText: String? = null
        ) : RemoteDownload
    }

    /** 處理完成，輸出預覽/暫存之本地檔案 */
    data class Completed(val outputFile: File) : MediaTaskProgress
}
