package com.github.straburzynski.testsanalyzer.ui

import com.github.straburzynski.testsanalyzer.model.TestStatus
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import java.awt.Color

object TestsAnalyzerColors {

    // Status colors (theme-aware)
    val SUCCESS = JBColor(Color(0x59, 0xA8, 0x69), Color(0x59, 0xA8, 0x69))
    val FAILED = JBColor(Color(0xDB, 0x58, 0x60), Color(0xDB, 0x58, 0x60))
    val SKIPPED = JBColor(Gray._160, Gray._160)

    // JBColor wrappers for Swing components that need theme-aware status colors
    val STATUS_SUCCESS = JBColor(Color(0x38, 0x5F, 0x44), Color(0x59, 0xA8, 0x69))
    val STATUS_FAILED = JBColor(Color(0xCE, 0x3F, 0x4B), Color(0xDB, 0x58, 0x60))
    val STATUS_SKIPPED = JBColor(Gray._160, Gray._160)

    // Hex strings for HTML export
    const val SUCCESS_HEX: String = "#59A869"
    const val FAILED_HEX: String = "#DB5860"
    const val SKIPPED_HEX: String = "#A0A0A0"

    // Chart colors (light, dark)
    val CHART_GROUP_BG = JBColor(Gray._232, Color(0x3C, 0x3F, 0x41))
    val CHART_GROUP_BORDER = JBColor(Gray._204, Gray._85)
    val CHART_BG = JBColor(Color.WHITE, Color(0x1E, 0x1F, 0x22))
    val CHART_FG = JBColor(Gray._51, Gray._186)
    val WHITE = JBColor(Color.WHITE, Color.WHITE)

    // Tooltip colors (light, dark)
    val TOOLTIP_BG = JBColor(Gray._240, Gray._50)
    val TOOLTIP_FG = JBColor(Color.BLACK, Color.WHITE)

    fun statusHex(status: TestStatus): String = when (status) {
        TestStatus.SUCCESS -> SUCCESS_HEX
        TestStatus.FAILED -> FAILED_HEX
        TestStatus.SKIPPED -> SKIPPED_HEX
    }

    fun statusColor(status: TestStatus) = when (status) {
        TestStatus.SUCCESS -> SUCCESS
        TestStatus.FAILED -> FAILED
        TestStatus.SKIPPED -> SKIPPED
    }

    fun statusJBColor(status: TestStatus) = when (status) {
        TestStatus.SUCCESS -> STATUS_SUCCESS
        TestStatus.FAILED -> STATUS_FAILED
        TestStatus.SKIPPED -> STATUS_SKIPPED
    }
}
