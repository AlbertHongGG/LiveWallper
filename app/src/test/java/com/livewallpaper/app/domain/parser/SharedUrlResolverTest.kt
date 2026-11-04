package com.livewallpaper.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SharedUrlResolverTest {

    private lateinit var resolver: SharedUrlResolver

    @Before
    fun setUp() {
        resolver = SharedUrlResolver(listOf(PinterestUrlParser()))
    }

    @Test
    fun `resolveCleanUrl should extract url from messy share text with emojis`() {
        val input = "Take a look! 📌 https://pin.it/4Yg9qhT9a"
        val resolved = resolver.resolveCleanUrl(input)
        assertEquals("https://pin.it/4Yg9qhT9a", resolved)
    }

    @Test
    fun `resolveCleanUrl should return null for non matching text`() {
        val input = "Hello world, check this out: https://google.com"
        assertNull(resolver.resolveCleanUrl(input))
    }

    @Test
    fun `sanitizeInput should replace messy text with clean url when valid url present`() {
        val input = "Take a look! 📌 https://pin.it/4Yg9qhT9a"
        val sanitized = resolver.sanitizeInput(input)
        assertEquals("https://pin.it/4Yg9qhT9a", sanitized)
    }

    @Test
    fun `sanitizeInput should retain raw text when user is still typing and no valid url matches`() {
        val input = "http://pin"
        val sanitized = resolver.sanitizeInput(input)
        assertEquals("http://pin", sanitized)
    }

    @Test
    fun `sanitizeInput should keep already clean pin url untouched`() {
        val input = "https://www.pinterest.com/pin/664421751319955179/"
        val sanitized = resolver.sanitizeInput(input)
        assertEquals("https://www.pinterest.com/pin/664421751319955179/", sanitized)
    }
}
