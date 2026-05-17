package com.github.straburzynski.testsanalyzer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ChartUtilsTest {

    @Test
    fun `chooseInterval returns 50 for small max`() {
        assertEquals(50, ChartUtils.chooseInterval(300))
    }

    @Test
    fun `chooseInterval returns 100 for medium max`() {
        assertEquals(100, ChartUtils.chooseInterval(800))
    }

    @Test
    fun `chooseInterval returns 1000 for large max`() {
        assertEquals(1000, ChartUtils.chooseInterval(8000))
    }

    @Test
    fun `chooseInterval returns 5000 for very large max`() {
        assertEquals(5000, ChartUtils.chooseInterval(40000))
    }

    @Test
    fun `chooseInterval handles zero`() {
        assertEquals(50, ChartUtils.chooseInterval(0))
    }

    @Test
    fun `formatMs formats milliseconds`() {
        assertEquals("500ms", ChartUtils.formatMs(500))
    }

    @Test
    fun `formatMs formats exact seconds`() {
        assertEquals("2s", ChartUtils.formatMs(2000))
    }

    @Test
    fun `formatMs formats fractional seconds`() {
        assertEquals("1.5s", ChartUtils.formatMs(1500))
    }

    @Test
    fun `formatMs formats zero`() {
        assertEquals("0ms", ChartUtils.formatMs(0))
    }

    @Test
    fun `escapeHtml escapes ampersand`() {
        assertEquals("a&amp;b", ChartUtils.escapeHtml("a&b"))
    }

    @Test
    fun `escapeHtml escapes angle brackets`() {
        assertEquals("&lt;div&gt;", ChartUtils.escapeHtml("<div>"))
    }

    @Test
    fun `escapeHtml escapes quotes`() {
        assertEquals("&quot;hello&quot;", ChartUtils.escapeHtml("\"hello\""))
    }

    @Test
    fun `escapeHtml returns plain text unchanged`() {
        assertEquals("hello world", ChartUtils.escapeHtml("hello world"))
    }
}
