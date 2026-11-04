package com.livewallpaper.app.service.engine.model

import com.livewallpaper.app.domain.model.ScalingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 驗證桌布渲染狀態值物件 [WallpaperRenderState] 的亮度乘數轉換與初始化防禦。
 */
class WallpaperRenderStateTest {

    @Test
    fun `default state has zero dimensions and full brightness`() {
        val state = WallpaperRenderState()
        assertEquals(0, state.surfaceWidth)
        assertEquals(0, state.surfaceHeight)
        assertEquals(0, state.videoWidth)
        assertEquals(0, state.videoHeight)
        assertEquals(ScalingMode.FILL, state.scalingMode)
        assertEquals(0f, state.dimAlpha, 0.0001f)
        assertEquals(1.0f, state.zoomScale, 0.0001f)
        assertEquals(0f, state.panOffsetX, 0.0001f)
        assertEquals(0f, state.panOffsetY, 0.0001f)
        assertEquals(1.0f, state.brightnessMultiplier, 0.0001f)
        assertFalse(state.isReadyToRender)
    }

    @Test
    fun `brightnessMultiplier calculates accurate inverse brightness for dimAlpha`() {
        val state0 = WallpaperRenderState(dimAlpha = 0.0f)
        assertEquals(1.0f, state0.brightnessMultiplier, 0.0001f)

        val state20 = WallpaperRenderState(dimAlpha = 0.2f)
        assertEquals(0.8f, state20.brightnessMultiplier, 0.0001f)

        val state40 = WallpaperRenderState(dimAlpha = 0.4f)
        assertEquals(0.6f, state40.brightnessMultiplier, 0.0001f)

        val state60 = WallpaperRenderState(dimAlpha = 0.6f)
        assertEquals(0.4f, state60.brightnessMultiplier, 0.0001f)
    }

    @Test
    fun `brightnessMultiplier clamps negative and excessive dimAlpha values`() {
        val stateNegative = WallpaperRenderState(dimAlpha = -0.5f)
        assertEquals(1.0f, stateNegative.brightnessMultiplier, 0.0001f)

        val stateExcessive = WallpaperRenderState(dimAlpha = 1.5f)
        assertEquals(0.0f, stateExcessive.brightnessMultiplier, 0.0001f)
    }

    @Test
    fun `isReadyToRender is true only when surface dimensions are positive`() {
        val stateZero = WallpaperRenderState(surfaceWidth = 0, surfaceHeight = 1920)
        assertFalse(stateZero.isReadyToRender)

        val stateNegative = WallpaperRenderState(surfaceWidth = 1080, surfaceHeight = -1)
        assertFalse(stateNegative.isReadyToRender)

        val stateValid = WallpaperRenderState(surfaceWidth = 1080, surfaceHeight = 1920)
        assertTrue(stateValid.isReadyToRender)
    }
}
