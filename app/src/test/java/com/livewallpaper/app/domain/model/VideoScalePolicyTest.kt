package com.livewallpaper.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 驗證純幾何縮放策略計算器 [VideoScalePolicy] 與 NDC 座標縮放倍率。
 */
class VideoScalePolicyTest {

    @Test
    fun `calculateNdcScale returns 1 for invalid or zero dimensions`() {
        val result1 = VideoScalePolicy.calculateNdcScale(0, 1920, 1080, 1920, ScalingMode.FIT)
        assertEquals(1.0f, result1.scaleX, 0.0001f)
        assertEquals(1.0f, result1.scaleY, 0.0001f)

        val result2 = VideoScalePolicy.calculateNdcScale(1080, 0, 1080, 1920, ScalingMode.FILL)
        assertEquals(1.0f, result2.scaleX, 0.0001f)
        assertEquals(1.0f, result2.scaleY, 0.0001f)

        val result3 = VideoScalePolicy.calculateNdcScale(1080, 1920, 0, 1920, ScalingMode.FILL)
        assertEquals(1.0f, result3.scaleX, 0.0001f)
        assertEquals(1.0f, result3.scaleY, 0.0001f)
    }

    @Test
    fun `calculateNdcScale with FIT mode for wider video keeps scaleX 1 and scaleY less than 1`() {
        // 容器 1080x1920 (9:16), 影片 1920x1080 (16:9 橫片)
        val result = VideoScalePolicy.calculateNdcScale(
            containerWidth = 1080,
            containerHeight = 1920,
            videoWidth = 1920,
            videoHeight = 1080,
            scalingMode = ScalingMode.FIT
        )

        assertEquals(1.0f, result.scaleX, 0.0001f)
        assertTrue(result.scaleY < 1.0f)
        // 預期高度 = 1080 / (1920/1080) = 1080 * 1080 / 1920 = 607.5 -> 607
        assertEquals(607f / 1920f, result.scaleY, 0.001f)
    }

    @Test
    fun `calculateNdcScale with FIT mode for taller video keeps scaleY 1 and scaleX less than 1`() {
        // 容器 1080x1080 (1:1), 影片 1080x1920 (9:16 直片)
        val result = VideoScalePolicy.calculateNdcScale(
            containerWidth = 1080,
            containerHeight = 1080,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FIT
        )

        assertEquals(1.0f, result.scaleY, 0.0001f)
        assertTrue(result.scaleX < 1.0f)
        // 預期寬度 = 1080 * (1080/1920) = 607.5 -> 607
        assertEquals(607f / 1080f, result.scaleX, 0.001f)
    }

    @Test
    fun `calculateNdcScale with FILL mode for wider video keeps scaleY 1 and scaleX greater than 1`() {
        // 容器 1080x2400 (9:20), 影片 1080x1920 (9:16) -> 影片比容器寬
        val result = VideoScalePolicy.calculateNdcScale(
            containerWidth = 1080,
            containerHeight = 2400,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FILL
        )

        assertEquals(1.0f, result.scaleY, 0.0001f)
        assertTrue("scaleX should be > 1.0 for cropping left and right", result.scaleX > 1.0f)
        // 預期寬度 = 2400 * (1080/1920) = 1350, scaleX = 1350 / 1080 = 1.25
        assertEquals(1.25f, result.scaleX, 0.0001f)
    }

    @Test
    fun `calculateNdcScale with FILL mode for taller video keeps scaleX 1 and scaleY greater than 1`() {
        // 容器 1080x1920 (9:16), 影片 1080x2400 (9:20) -> 影片比容器窄（更長）
        val result = VideoScalePolicy.calculateNdcScale(
            containerWidth = 1080,
            containerHeight = 1920,
            videoWidth = 1080,
            videoHeight = 2400,
            scalingMode = ScalingMode.FILL
        )

        assertEquals(1.0f, result.scaleX, 0.0001f)
        assertTrue("scaleY should be > 1.0 for cropping top and bottom", result.scaleY > 1.0f)
        // 預期高度 = 1080 / (1080/2400) = 2400, scaleY = 2400 / 1920 = 1.25
        assertEquals(1.25f, result.scaleY, 0.0001f)
    }
}
