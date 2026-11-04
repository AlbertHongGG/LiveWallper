package com.livewallpaper.app.domain.model

import kotlinx.serialization.Serializable

/**
 * 動態桌布全域設定模型（Single Source of Truth）。
 */
@Serializable
data class WallpaperConfig(
    val videoFilePath: String = "",
    val scalingMode: ScalingMode = ScalingMode.FILL,
    val horizontalOffset: Float = 0f,          // -1.0f (最左) ~ 0f (置中) ~ +1.0f (最右)
    val dimAlpha: Float = 0f,                  // 0.0f ~ 0.6f 桌面壓暗遮罩
    val isMuted: Boolean = true,
    val volume: Float = 0.5f,                  // 0.0f ~ 1.0f 音量
    val playbackSpeed: Float = 1.0f,           // 0.5f ~ 2.0f 播放速度
    val enableTrim: Boolean = false,           // 是否啟用片段循環剪輯
    val trimStartMs: Long = 0L,                // 片段開始毫秒
    val trimEndMs: Long = 0L,                  // 片段結束毫秒 (0 代表至結尾)
    val doubleTapToPause: Boolean = true,      // 雙擊桌面暫停/恢復
    val powerSaveDelaySeconds: Int = 3,        // 離開桌面後釋放硬體解碼器之延遲秒數
    val updatedAtMillis: Long = 0L
) {
    val hasVideo: Boolean
        get() = videoFilePath.isNotBlank()
}
