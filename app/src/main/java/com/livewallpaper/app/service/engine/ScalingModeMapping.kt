package com.livewallpaper.app.service.engine

import androidx.media3.common.C
import com.livewallpaper.app.domain.model.ScalingMode

/**
 * 將領域層的 [ScalingMode] 映射至 ExoPlayer / MediaCodec 原生硬體視訊縮放常數。
 *
 * 集中定義於 Service 端，確保領域層不依賴 Media3，並以窮舉 when 確保類型安全。
 */
fun ScalingMode.toVideoScalingMode(): Int = when (this) {
    ScalingMode.FILL -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
    ScalingMode.FIT -> C.VIDEO_SCALING_MODE_SCALE_TO_FIT
}
