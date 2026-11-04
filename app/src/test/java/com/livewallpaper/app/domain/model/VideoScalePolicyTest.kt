package com.livewallpaper.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 驗證純幾何縮放與手勢變換計算引擎 [VideoScalePolicy]。
 */
class VideoScalePolicyTest {

    @Test
    fun `calculateSurfaceTransform returns container size for invalid dimensions`() {
        val result1 = VideoScalePolicy.calculateSurfaceTransform(0, 1920, 1080, 1920, ScalingMode.FIT)
        assertEquals(0, result1.baseWidth)
        assertEquals(1920, result1.baseHeight)
        assertEquals(1.0f, result1.scale, 0.0001f)

        val result2 = VideoScalePolicy.calculateSurfaceTransform(1080, 0, 1080, 1920, ScalingMode.FILL)
        assertEquals(1080, result2.baseWidth)
        assertEquals(0, result2.baseHeight)
    }

    @Test
    fun `calculateSurfaceTransform with FIT mode for wider video has baseWidth equal to container`() {
        // 容器 1080x1920 (9:16), 影片 1920x1080 (16:9 橫片)
        val result = VideoScalePolicy.calculateSurfaceTransform(
            containerWidth = 1080,
            containerHeight = 1920,
            videoWidth = 1920,
            videoHeight = 1080,
            scalingMode = ScalingMode.FIT
        )

        assertEquals(1080, result.baseWidth)
        assertEquals(607, result.baseHeight)
        assertEquals(0, result.baseOffsetX)
        assertEquals((1920 - 607) / 2, result.baseOffsetY)
        assertEquals(1.0f, result.scale, 0.0001f)
    }

    @Test
    fun `calculateSurfaceTransform with FILL mode keeps base dimensions fixed during zoom`() {
        // 容器 1080x2400 (9:20), 影片 1080x1920 (9:16)
        val base = VideoScalePolicy.calculateSurfaceTransform(
            containerWidth = 1080,
            containerHeight = 2400,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FILL,
            transform = ViewportTransform.IDENTITY
        )

        // 滿版模式下 base 寬度等比放大超出: 2400 * (1080/1920) = 1350
        assertEquals(1350, base.baseWidth)
        assertEquals(2400, base.baseHeight)
        assertEquals(1.0f, base.scale, 0.0001f)
        assertEquals(0f, base.translationX, 0.0001f)

        // 手勢放大至 2.0x 且平移至最右邊界 panOffsetX = 1.0f
        val zoomed = VideoScalePolicy.calculateSurfaceTransform(
            containerWidth = 1080,
            containerHeight = 2400,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FILL,
            transform = ViewportTransform(zoomScale = 2.0f, panOffsetX = 1.0f, panOffsetY = 0f)
        )

        // 關鍵驗證：baseWidth 與 baseHeight 永遠維持不變（保證 0 次 View Relayout）！
        assertEquals(1350, zoomed.baseWidth)
        assertEquals(2400, zoomed.baseHeight)
        assertEquals(2.0f, zoomed.scale, 0.0001f)

        // 放大後 renderedW = 1350 * 2 = 2700, maxPanX = (2700 - 1080) / 2 = 810
        // panOffsetX = 1.0f -> translationX = +810.0f
        assertEquals(810f, zoomed.translationX, 0.0001f)
        assertEquals(0f, zoomed.translationY, 0.0001f)
    }

    @Test
    fun `calculateNdcTransform accurately maps SurfaceTransform to OpenGL NDC space`() {
        // 容器 1080x1920, 影片 1080x1920, zoom 2.0x, panOffsetX = 1.0f
        val ndc = VideoScalePolicy.calculateNdcTransform(
            containerWidth = 1080,
            containerHeight = 1920,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FILL,
            transform = ViewportTransform(zoomScale = 2.0f, panOffsetX = 1.0f, panOffsetY = 0.5f)
        )

        assertEquals(2.0f, ndc.scaleX, 0.0001f)
        assertEquals(2.0f, ndc.scaleY, 0.0001f)

        // maxPanX = (2160 - 1080) / 2 = 540, panOffsetX = 1.0 -> translationX = 540
        // translateX in NDC = (2 * 540) / 1080 = 1.0f
        assertEquals(1.0f, ndc.translateX, 0.0001f)

        // maxPanY = (3840 - 1920) / 2 = 960, panOffsetY = 0.5 -> translationY = 480
        // translateY in NDC = -(2 * 480) / 1920 = -0.5f (Y 軸反轉)
        assertEquals(-0.5f, ndc.translateY, 0.0001f)
    }

    @Test
    fun `applyGestureDelta accumulates multi-frame pan without jitter`() {
        val containerW = 1080f
        val containerH = 2400f
        val videoW = 1080
        val videoH = 1920
        val mode = ScalingMode.FILL

        // 初始狀態 (zoom=1.0, pan=0.0)
        var state = ViewportTransform.IDENTITY

        // 第 1 幀：手指拖曳 +20px
        state = VideoScalePolicy.applyGestureDelta(
            current = state,
            containerWidth = containerW,
            containerHeight = containerH,
            videoWidth = videoW,
            videoHeight = videoH,
            scalingMode = mode,
            zoomChange = 1.0f,
            panChangeX = 20f,
            panChangeY = 0f
        )
        assertTrue("panOffsetX should be positive", state.panOffsetX > 0f)
        val frame1Offset = state.panOffsetX

        // 第 2 幀：手指持續拖曳 +20px
        state = VideoScalePolicy.applyGestureDelta(
            current = state,
            containerWidth = containerW,
            containerHeight = containerH,
            videoWidth = videoW,
            videoHeight = videoH,
            scalingMode = mode,
            zoomChange = 1.0f,
            panChangeX = 20f,
            panChangeY = 0f
        )
        // 關鍵驗證：第 2 幀位移必須累加，絕不跳回原點！
        assertTrue("panOffsetX must accumulate", state.panOffsetX > frame1Offset)
        assertEquals(frame1Offset * 2f, state.panOffsetX, 0.001f)
    }

    @Test
    fun `applyGestureDelta clamps pan within container boundaries`() {
        val containerW = 1080f
        val containerH = 1920f
        val videoW = 1080
        val videoH = 1920
        val mode = ScalingMode.FILL

        var state = ViewportTransform(zoomScale = 1.5f, panOffsetX = 0f, panOffsetY = 0f)

        // 巨幅拖曳 +9999px（大幅超出邊界）
        state = VideoScalePolicy.applyGestureDelta(
            current = state,
            containerWidth = containerW,
            containerHeight = containerH,
            videoWidth = videoW,
            videoHeight = videoH,
            scalingMode = mode,
            zoomChange = 1.0f,
            panChangeX = 9999f,
            panChangeY = 9999f
        )

        // 驗證邊界鎖定在 1.0f
        assertEquals(1.0f, state.panOffsetX, 0.0001f)
        assertEquals(1.0f, state.panOffsetY, 0.0001f)

        // 巨幅反向拖曳 -9999px
        state = VideoScalePolicy.applyGestureDelta(
            current = state,
            containerWidth = containerW,
            containerHeight = containerH,
            videoWidth = videoW,
            videoHeight = videoH,
            scalingMode = mode,
            zoomChange = 1.0f,
            panChangeX = -99999f,
            panChangeY = -99999f
        )

        // 驗證邊界鎖定在 -1.0f
        assertEquals(-1.0f, state.panOffsetX, 0.0001f)
        assertEquals(-1.0f, state.panOffsetY, 0.0001f)
    }

    @Test
    fun `applyGestureDelta smoothly zooms and clamps between 1x and 3x`() {
        var state = ViewportTransform.IDENTITY

        // 嘗試縮小至小於 1.0x -> 鎖定在 1.0x
        state = VideoScalePolicy.applyGestureDelta(
            current = state,
            containerWidth = 1080f,
            containerHeight = 1920f,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FILL,
            zoomChange = 0.5f,
            panChangeX = 0f,
            panChangeY = 0f
        )
        assertEquals(1.0f, state.zoomScale, 0.0001f)

        // 連續放大至大於 3.0x -> 鎖定在 3.0x
        state = VideoScalePolicy.applyGestureDelta(
            current = state,
            containerWidth = 1080f,
            containerHeight = 1920f,
            videoWidth = 1080,
            videoHeight = 1920,
            scalingMode = ScalingMode.FILL,
            zoomChange = 5.0f,
            panChangeX = 0f,
            panChangeY = 0f
        )
        assertEquals(3.0f, state.zoomScale, 0.0001f)
    }
}
