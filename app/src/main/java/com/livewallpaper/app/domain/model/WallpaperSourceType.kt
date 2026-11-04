package com.livewallpaper.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 桌布來源類型。
 */
@Serializable
enum class WallpaperSourceType {
    /** 來自本地相簿/檔案選取器 */
    LOCAL_GALLERY,

    /** 來自外部網址解析下載（如 Pinterest） */
    REMOTE_DOWNLOAD
}
