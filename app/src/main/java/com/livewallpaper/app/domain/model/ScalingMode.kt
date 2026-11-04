package com.livewallpaper.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 影片在螢幕上的縮放縮放模式。
 */
@Serializable
enum class ScalingMode(val label: String) {
    /** 完整顯示（保持原始比例完整呈現，保留黑邊） */
    FIT("完整顯示"),
    /** 滿版裁切（等比放大填滿螢幕，可自由搭配水平視角位移） */
    FILL("滿版裁切")
}
