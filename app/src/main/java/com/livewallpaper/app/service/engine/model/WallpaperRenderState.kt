package com.livewallpaper.app.service.engine.model

import com.livewallpaper.app.domain.model.ScalingMode

/**
 * 桌布 OpenGL 渲染引擎的不可變狀態值物件 (Immutable Render State)。
 *
 * 專為多執行緒渲染安全設計，封裝 Surface 尺寸、視訊幾何尺寸、縮放模式與壓暗參數。
 */
data class WallpaperRenderState(
    val surfaceWidth: Int = 0,
    val surfaceHeight: Int = 0,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val scalingMode: ScalingMode = ScalingMode.FILL,
    val dimAlpha: Float = 0f
) {
    /**
     * 經由 dimAlpha 轉換後的著色器亮度乘數。
     *
     * 數學意義：$C_{out} = C_{video} \times (1 - \alpha) + C_{black} \times \alpha = C_{video} \times (1 - \alpha)$
     * 保證完全等於在影片上方覆蓋 (0, 0, 0, alpha) 遮罩層。
     */
    val brightnessMultiplier: Float
        get() = (1f - dimAlpha.coerceIn(0f, 1f))

    /**
     * 是否具備執行 GL 繪製之最小尺寸條件。
     */
    val isReadyToRender: Boolean
        get() = surfaceWidth > 0 && surfaceHeight > 0
}
