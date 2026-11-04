package com.livewallpaper.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PinterestUrlParserTest {

    private lateinit var parser: PinterestUrlParser

    @Before
    fun setUp() {
        parser = PinterestUrlParser()
    }

    @Test
    fun `extractCleanUrl should parse short pin-it url correctly`() {
        val input = "看看我在 Pinterest 發現的釘圖：https://pin.it/7w4yX8A"
        assertTrue(parser.canHandle(input))
        val url = parser.extractCleanUrl(input)
        assertEquals("https://pin.it/7w4yX8A", url)
    }

    @Test
    fun `extractCleanUrl should extract clean url from text containing emojis and preamble`() {
        val input = "Take a look! 📌 https://pin.it/4Yg9qhT9a"
        assertTrue(parser.canHandle(input))
        val url = parser.extractCleanUrl(input)
        assertEquals("https://pin.it/4Yg9qhT9a", url)
    }

    @Test
    fun `extractCleanUrl should trim punctuation at the end of url`() {
        val input = "超美動態桌布 (https://pin.it/abc123)！"
        assertTrue(parser.canHandle(input))
        val url = parser.extractCleanUrl(input)
        assertEquals("https://pin.it/abc123", url)
    }

    @Test
    fun `extractCleanUrl should parse standard web pin url`() {
        val input = "https://www.pinterest.com/pin/123456789012345678/"
        assertTrue(parser.canHandle(input))
        val url = parser.extractCleanUrl(input)
        assertEquals("https://www.pinterest.com/pin/123456789012345678/", url)
    }

    @Test
    fun `extractCleanUrl should parse international pin url`() {
        val input = "推薦你這個：https://tw.pinterest.com/pin/987654321098765432"
        assertTrue(parser.canHandle(input))
        val url = parser.extractCleanUrl(input)
        assertEquals("https://tw.pinterest.com/pin/987654321098765432", url)
    }

    @Test
    fun `extractCleanUrl should reject invalid non-pinterest url`() {
        val input = "https://www.google.com/search?q=wallpaper"
        assertFalse(parser.canHandle(input))
        assertNull(parser.extractCleanUrl(input))
    }

    @Test
    fun `extractCleanUrl should reject empty or blank strings`() {
        assertFalse(parser.canHandle(""))
        assertFalse(parser.canHandle("   "))
        assertNull(parser.extractCleanUrl(""))
    }
}
