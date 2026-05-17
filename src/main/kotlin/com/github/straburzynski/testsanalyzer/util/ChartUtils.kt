package com.github.straburzynski.testsanalyzer.util

object ChartUtils {

    fun chooseInterval(maxMs: Long): Long {
        val raw = maxMs / 10
        return when {
            raw <= 50 -> 50
            raw <= 100 -> 100
            raw <= 250 -> 250
            raw <= 500 -> 500
            raw <= 1000 -> 1000
            raw <= 2500 -> 2500
            raw <= 5000 -> 5000
            else -> ((raw / 5000) + 1) * 5000
        }
    }

    fun formatMs(ms: Long): String = when {
        ms >= 1000 && ms % 1000 == 0L -> "${ms / 1000}s"
        ms >= 1000 -> "%.1fs".format(ms / 1000.0)
        else -> "${ms}ms"
    }

    fun escapeHtml(text: String): String = text
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
}
