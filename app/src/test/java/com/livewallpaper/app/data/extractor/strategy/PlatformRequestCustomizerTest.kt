package com.livewallpaper.app.data.extractor.strategy

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformRequestCustomizerTest {

    private val pinterestCustomizer = PinterestRequestCustomizer()

    @Test
    fun `PinterestRequestCustomizer should match pinterest domains and pin it`() {
        assertTrue(pinterestCustomizer.matches("https://www.pinterest.com/pin/12345/"))
        assertTrue(pinterestCustomizer.matches("https://tw.pinterest.com/pin/67890/"))
        assertTrue(pinterestCustomizer.matches("https://pin.it/abcXYZ"))
        assertTrue(pinterestCustomizer.matches("http://pinterest.com/pin/999/"))
    }

    @Test
    fun `PinterestRequestCustomizer should not match other domains`() {
        assertFalse(pinterestCustomizer.matches("https://www.youtube.com/watch?v=123"))
        assertFalse(pinterestCustomizer.matches("https://www.tiktok.com/@user/video/123"))
        assertFalse(pinterestCustomizer.matches("https://example.com/video.mp4"))
        assertFalse(pinterestCustomizer.matches("invalid-url"))
    }
}
