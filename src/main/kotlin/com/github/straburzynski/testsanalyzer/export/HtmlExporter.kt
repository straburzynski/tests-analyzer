package com.github.straburzynski.testsanalyzer.export

import com.github.straburzynski.testsanalyzer.model.TestGroup
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestSummary
import com.github.straburzynski.testsanalyzer.model.toChartGroups
import com.github.straburzynski.testsanalyzer.ui.TestsAnalyzerColors
import com.github.straburzynski.testsanalyzer.util.ChartUtils
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object HtmlExporter {

    private const val ROW_HEIGHT = 20
    private const val HEADER_HEIGHT = 22
    private const val ROW_GAP = 2
    private const val TOP_MARGIN = 24

    private fun loadResource(path: String): String =
        HtmlExporter::class.java.getResourceAsStream(path)?.bufferedReader()?.readText()
            ?: error("Resource not found: $path")

    private fun loadCss(): String = loadResource("/export/style.css")

    private fun loadJs(): String = loadResource("/export/script.js")

    fun exportToHtml(allResults: List<TestResult>, file: File) {
        val results = allResults.filter { it.type == TestResultType.TEST }
        val summary = TestSummary.from(allResults)
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

        val maxEnd = if (results.isNotEmpty()) results.maxOf { it.startTime + it.durationMs } else 0L
        val labelWidth = 160
        val chartWidth = 800
        val svgWidth = labelWidth + chartWidth
        val groups = results.toChartGroups()
        val svgHeight = TOP_MARGIN + groups.sumOf { HEADER_HEIGHT + ROW_GAP + it.tests.size * (ROW_HEIGHT + ROW_GAP) } + 20
        val scale = if (maxEnd > 0) chartWidth.toDouble() / maxEnd else 1.0

        val timeSummary = if (summary.hasParallelSavings) {
            """<span class="time">Wall time: ${summary.wallClockTimeMs}ms, Cumulative: ${summary.cumulativeTimeMs}ms</span>"""
        } else {
            """<span class="time">Total time: ${summary.cumulativeTimeMs}ms</span>"""
        }

        val html = buildString {
            appendLine("""
                |<!DOCTYPE html>
                |<html lang="en"><head><meta charset="UTF-8">
                |<meta name="viewport" content="width=device-width, initial-scale=1.0">
                |<title>Test Results - $timestamp</title>
                |<style>
                |${loadCss()}
                |</style></head><body>
                |<div class="header-row"><h1>Tests Analyzer Results</h1><button class="theme-toggle" onclick="toggleTheme()">Toggle Light/Dark</button></div>
                |<p class="timestamp">Generated: $timestamp</p>
                |<div class="summary">
                |<span class="total">Total: ${summary.total}</span>
                |<span class="pass">Passed: ${summary.passed}</span>
                |<span class="fail">Failed: ${summary.failed}</span>
                |<span class="skip">Skipped: ${summary.skipped}</span>
                |$timeSummary
                |</div>
                |<div class="tab-bar">
                |<button class="tab-btn active" onclick="switchTab('results')">Results Table</button>
                |<button class="tab-btn" onclick="switchTab('chart')">Timeline Chart</button>
                |</div>
            """.trimMargin())

            appendResultsTab(results)
            appendChartTab(groups, maxEnd, scale, labelWidth, chartWidth, svgWidth, svgHeight)

            appendLine("""
                |<div class="chart-tooltip" id="chartTooltip"></div>
                |<script>
                |${loadJs()}
                |</script>
                |</body></html>
            """.trimMargin())
        }

        file.writeText(html)
    }

    private fun StringBuilder.appendResultsTab(results: List<TestResult>) {
        appendLine("""
            |<div id="tab-results" class="tab-content active">
            |<div class="filter-bar">
            |<label>Search: <input type="text" id="searchInput" placeholder="Filter by class, test, package..." oninput="applyFilters()"></label>
            |<label class="cb-success"><input type="checkbox" id="cbSuccess" checked onchange="applyFilters()"> Success</label>
            |<label class="cb-failed"><input type="checkbox" id="cbFailed" checked onchange="applyFilters()"> Failed</label>
            |<label class="cb-skipped"><input type="checkbox" id="cbSkipped" checked onchange="applyFilters()"> Skipped</label>
            |</div>
            |<div class="table-container">
            |<table id="resultsTable"><thead><tr>
            |<th onclick="sortTable(0)">Class &#x25B4;&#x25BE;</th>
            |<th onclick="sortTable(1)">Test &#x25B4;&#x25BE;</th>
            |<th onclick="sortTable(2)">Package &#x25B4;&#x25BE;</th>
            |<th onclick="sortTable(3)">Time (ms) &#x25B4;&#x25BE;</th>
            |<th onclick="sortTable(4)">Status &#x25B4;&#x25BE;</th>
            |</tr></thead><tbody>
        """.trimMargin())

        for (r in results) {
            val statusClass = r.status.name.lowercase()
            appendLine("""
                |<tr data-status="${r.status.name}">
                |<td>${ChartUtils.escapeHtml(r.className)}</td>
                |<td>${ChartUtils.escapeHtml(r.testName)}</td>
                |<td>${ChartUtils.escapeHtml(r.packageName)}</td>
                |<td class="num">${r.durationMs}</td>
                |<td><span class="badge $statusClass">${r.status}</span></td>
                |</tr>
            """.trimMargin())
        }

        appendLine("""
            |</tbody></table>
            |</div>
            |</div>
        """.trimMargin())
    }

    private fun StringBuilder.appendChartTab(
        groups: List<TestGroup>,
        maxEnd: Long,
        scale: Double,
        labelWidth: Int,
        chartWidth: Int,
        svgWidth: Int,
        svgHeight: Int,
    ) {
        appendLine("""
            |<div id="tab-chart" class="tab-content">
            |<div class="chart-header-row">
            |<div class="chart-toolbar-left"><button class="toolbar-btn" onclick="toggleChartSidebar()">Toggle Sidebar</button></div>
            |<h2>Timeline</h2>
            |<div class="chart-toolbar-right">
            |<button class="toolbar-btn" onclick="chartZoomIn()">+</button>
            |<button class="toolbar-btn" onclick="chartZoomOut()">-</button>
            |<button class="toolbar-btn" onclick="chartZoomReset()">Reset</button>
            |</div>
            |</div>
            |<div class="chart-container">
            |<svg id="ganttSvg" viewBox="0 0 $svgWidth $svgHeight" preserveAspectRatio="xMinYMin meet" xmlns="http://www.w3.org/2000/svg" data-label-width="$labelWidth" data-chart-width="$chartWidth" data-base-width="$svgWidth" data-base-height="$svgHeight" data-max-end="$maxEnd" data-header-height="$HEADER_HEIGHT" data-row-height="$ROW_HEIGHT" data-row-gap="$ROW_GAP" data-top-margin="$TOP_MARGIN">
            |<defs><clipPath id="labelClip"><rect x="0" y="0" width="${labelWidth - 8}" height="$svgHeight"/></clipPath></defs>
            |<line x1="$labelWidth" y1="$TOP_MARGIN" x2="$svgWidth" y2="$TOP_MARGIN" class="axis-line chart-axis" stroke="currentColor" stroke-width="1"/>
        """.trimMargin())

        // Tick marks
        val interval = ChartUtils.chooseInterval(maxEnd)
        var tick = 0L
        while (tick <= maxEnd) {
            val x = labelWidth + (tick * scale).toInt()
            appendLine("""
                |<line x1="$x" y1="${TOP_MARGIN - 5}" x2="$x" y2="$TOP_MARGIN" stroke="currentColor" class="chart-tick" data-tick-ms="$tick"/>
                |<text x="${x + 2}" y="${TOP_MARGIN - 6}" font-size="13" fill="currentColor" class="axis-text chart-tick-text" data-tick-ms="$tick">${ChartUtils.formatMs(tick)}</text>
            """.trimMargin())
            tick += interval
        }

        // Bars grouped by class
        var clipIndex = 0
        var currentY = TOP_MARGIN
        var groupIndex = 0
        for (group in groups) {
            val groupId = "group-$groupIndex"
            val groupLabel = ChartUtils.escapeHtml(group.className)
            val groupTooltip = ChartUtils.escapeHtml("${group.packageName}.${group.className}")
            appendLine("""
                |<g class="gantt-group-header" data-group="$groupId" style="cursor:pointer;">
                |<rect x="0" y="$currentY" width="$svgWidth" height="$HEADER_HEIGHT" class="group-header-bg"/>
                |<line x1="0" y1="${currentY + HEADER_HEIGHT}" x2="$svgWidth" y2="${currentY + HEADER_HEIGHT}" class="group-header-line"/>
                |<foreignObject x="4" y="$currentY" width="${labelWidth - 12}" height="$HEADER_HEIGHT" class="group-label-sidebar">
                |<div xmlns="http://www.w3.org/1999/xhtml" class="svg-label group-label chart-label" data-tooltip="$groupTooltip"><span class="collapse-arrow" data-group="$groupId">&#x25BC;</span> $groupLabel</div>
                |</foreignObject>
                |<foreignObject x="${labelWidth + 4}" y="$currentY" width="$chartWidth" height="$HEADER_HEIGHT" class="group-label-chart" style="display:none;">
                |<div xmlns="http://www.w3.org/1999/xhtml" class="svg-label group-label chart-label" data-tooltip="$groupTooltip"><span class="collapse-arrow" data-group="$groupId">&#x25BC;</span> $groupLabel</div>
                |</foreignObject>
                |</g>
            """.trimMargin())
            currentY += HEADER_HEIGHT + ROW_GAP

            appendLine("""<g class="gantt-group-tests" data-group="$groupId">""")
            for (r in group.tests) {
                val barX = labelWidth + (r.startTime * scale).toInt()
                val barW = (r.durationMs * scale).toInt().coerceAtLeast(1)
                val color = TestsAnalyzerColors.statusHex(r.status)
                val tooltipText = ChartUtils.escapeHtml("${r.className}.${r.testName} — ${r.durationMs}ms — ${r.status}")
                val barLabel = ChartUtils.escapeHtml("${r.durationMs}ms")
                val textY = currentY + ROW_HEIGHT / 2 + 5

                appendLine("""
                    |<foreignObject x="14" y="$currentY" width="${labelWidth - 22}" height="$ROW_HEIGHT">
                    |<div xmlns="http://www.w3.org/1999/xhtml" class="svg-label chart-label" data-tooltip="$tooltipText">${ChartUtils.escapeHtml(r.testName)}</div>
                    |</foreignObject>
                    |<rect class="chart-bar" x="$barX" y="${currentY + 2}" width="$barW" height="${ROW_HEIGHT - 4}" rx="3" fill="$color" data-tooltip="$tooltipText" data-start-ms="${r.startTime}" data-duration-ms="${r.durationMs}"/>
                    |<text x="${barX + 3}" y="$textY" font-size="13" class="bar-text-outside" pointer-events="none" data-start-ms="${r.startTime}">$barLabel</text>
                    |<clipPath id="clip-$clipIndex"><rect x="$barX" y="${currentY + 2}" width="$barW" height="${ROW_HEIGHT - 4}" data-start-ms="${r.startTime}" data-duration-ms="${r.durationMs}"/></clipPath>
                    |<text x="${barX + 3}" y="$textY" font-size="13" class="bar-text-inside" clip-path="url(#clip-$clipIndex)" pointer-events="none" data-start-ms="${r.startTime}">$barLabel</text>
                """.trimMargin())

                clipIndex++
                currentY += ROW_HEIGHT + ROW_GAP
            }
            appendLine("</g>")
            groupIndex++
        }

        appendLine("""
            |</svg></div>
            |</div>
        """.trimMargin())
    }
}
