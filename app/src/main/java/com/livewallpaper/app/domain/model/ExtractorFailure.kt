package com.livewallpaper.app.domain.model

/**
 * 結構化之解析與下載失敗原因類別。
 * 將第三方工具之底層例外歸納為領域層可辨識之語意狀態。
 */
sealed interface ExtractorFailure {
    /** 核心引擎或擷取器版本過舊，可透過線上熱更新進行自癒 */
    data object EngineOutdated : ExtractorFailure

    /** 核心二進制或 FFmpeg 初始化不可用 */
    data object EngineUnavailable : ExtractorFailure

    /** 平台未提供相容之影片或音訊格式串流 */
    data object FormatUnavailable : ExtractorFailure

    /** 視訊與音訊下載成功但轉碼、合併或後製處理失敗 */
    data object ProcessingFailed : ExtractorFailure

    /** 網路逾時、連線阻斷或 DNS 解析異常 */
    data object NetworkUnavailable : ExtractorFailure

    /** 貼文內容不支援（如純圖片貼文）或為私密內容 */
    data object ContentUnsupported : ExtractorFailure

    /** 未知或未歸類之錯誤 */
    data class Unknown(val detail: String) : ExtractorFailure
}
