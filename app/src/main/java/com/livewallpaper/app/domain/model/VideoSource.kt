package com.livewallpaper.app.domain.model

/**
 * 影片來源抽象表示。
 * 讓 UI 與 UseCase 層無需直接依賴具體的 Android Intent 或下載實作。
 */
sealed interface VideoSource {
    /** 來自本地相簿/檔案選取器（包含 Content URI 或 File URI） */
    data class LocalUri(val uriString: String) : VideoSource

    /** 來自外部分享（如 Pinterest 分享字串，可能帶有文案與連結）或使用者手動貼上之字串 */
    data class RemoteShared(val rawInput: String) : VideoSource
}
