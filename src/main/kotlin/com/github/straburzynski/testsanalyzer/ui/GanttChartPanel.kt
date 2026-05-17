package com.github.straburzynski.testsanalyzer.ui

import com.intellij.icons.AllIcons
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.github.straburzynski.testsanalyzer.model.TestGroup
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.toChartGroups
import com.github.straburzynski.testsanalyzer.util.ChartUtils
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.Window
import java.awt.event.HierarchyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JEditorPane
import javax.swing.JPanel
import javax.swing.JWindow
import javax.swing.ScrollPaneConstants
import javax.swing.Scrollable
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

class GanttChartPanel(private val results: List<TestResult>) : JBPanel<GanttChartPanel>(BorderLayout()) {

    private val chartPanel = ChartDrawingPanel()

    private val scrollPane: JBScrollPane = JBScrollPane(chartPanel).apply {
        horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS
        verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS
        verticalScrollBar.unitIncrement = 16
        horizontalScrollBar.unitIncrement = 16
    }

    init {
        val sidebar = JPanel(BorderLayout()).apply {
            isOpaque = true
            border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
            val leftButtons = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
                isOpaque = false
                add(createToolButton(AllIcons.Actions.PreviewDetails, "Toggle Sidebar") { chartPanel.toggleSidebar() })
            }
            val rightButtons = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
                isOpaque = false
                add(createToolButton(AllIcons.General.Add, "Zoom In") { chartPanel.zoomIn() })
                add(createToolButton(AllIcons.General.Remove, "Zoom Out") { chartPanel.zoomOut() })
                add(createToolButton(AllIcons.General.Reset, "Reset") { chartPanel.resetZoom() })
            }
            add(leftButtons, BorderLayout.WEST)
            add(rightButtons, BorderLayout.EAST)
        }

        add(sidebar, BorderLayout.NORTH)
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun createToolButton(icon: Icon, tooltip: String, action: () -> Unit): JButton {
        return JButton(tooltip, icon).apply {
            toolTipText = tooltip
            isFocusable = false
            margin = JBUI.insets(2, 6)
            addActionListener { action() }
        }
    }

    private sealed class ChartRow {
        data class GroupHeader(val group: TestGroup) : ChartRow()
        data class TestBar(val result: TestResult) : ChartRow()
    }

    private inner class ChartDrawingPanel : JPanel(), Scrollable {
        private val fullLeftMargin = 200
        private val collapsedLeftMargin = 10
        private var sidebarVisible = true
        private val leftMargin: Int get() = if (sidebarVisible) fullLeftMargin else collapsedLeftMargin
        private val topMargin = 30
        private val rowHeight = 26
        private val headerHeight = 28
        private val rowGap = 2
        private var zoomFactor = 1.0
        private val groups: List<TestGroup> = results.toChartGroups()
        private val testResults: List<TestResult> = groups.flatMap { it.tests }
        private val collapsedGroups = mutableSetOf<String>()
        private var chartRows: List<ChartRow> = buildChartRows()

        private var rowYPositions: IntArray = computeRowYPositions()

        var isDark: Boolean = !JBColor.isBright()
            private set

        private val tooltipFgHex: String get() = if (isDark) "white" else "black"

        private var tooltipPopup: JWindow? = null
        private val tooltipPane = JEditorPane().apply {
            contentType = "text/html"
            isEditable = false
            isOpaque = false
            border = BorderFactory.createEmptyBorder(4, 8, 4, 8)
            putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
            font = Font("SansSerif", Font.PLAIN, 12)
        }

        private fun getOrCreateTooltipPopup(): JWindow {
            tooltipPopup?.let { return it }
            val owner = SwingUtilities.getWindowAncestor(this)
            val popup = JWindow(owner).apply {
                contentPane.background = TestsAnalyzerColors.TOOLTIP_BG
                contentPane.add(tooltipPane)
                type = Window.Type.POPUP
                focusableWindowState = false
            }
            tooltipPane.foreground = TestsAnalyzerColors.TOOLTIP_FG
            tooltipPopup = popup
            return popup
        }

        private fun buildChartRows(): List<ChartRow> {
            val rows = mutableListOf<ChartRow>()
            for (group in groups) {
                rows.add(ChartRow.GroupHeader(group))
                if (group.className !in collapsedGroups) {
                    for (test in group.tests) {
                        rows.add(ChartRow.TestBar(test))
                    }
                }
            }
            return rows
        }

        private fun computeRowYPositions(): IntArray {
            val positions = IntArray(chartRows.size)
            var y = topMargin
            for (i in chartRows.indices) {
                positions[i] = y
                y += when (chartRows[i]) {
                    is ChartRow.GroupHeader -> headerHeight + rowGap
                    is ChartRow.TestBar -> rowHeight + rowGap
                }
            }
            return positions
        }

        private fun rebuildRows() {
            chartRows = buildChartRows()
            rowYPositions = computeRowYPositions()
            revalidate()
            repaint()
        }

        private fun toggleGroup(className: String) {
            if (className in collapsedGroups) {
                collapsedGroups.remove(className)
            } else {
                collapsedGroups.add(className)
            }
            rebuildRows()
        }

        init {
            isOpaque = true
            background = TestsAnalyzerColors.CHART_BG
            foreground = TestsAnalyzerColors.CHART_FG
            ToolTipManager.sharedInstance().unregisterComponent(this)

            addMouseMotionListener(
                object : MouseMotionAdapter() {
                    override fun mouseMoved(e: MouseEvent) {
                        var overHeader = false
                        for ((index, row) in chartRows.withIndex()) {
                            val ry = rowYPositions[index]
                            val rh = rowHeight(index)
                            if (e.y >= ry && e.y < ry + rh && row is ChartRow.GroupHeader) {
                                overHeader = true
                                break
                            }
                        }
                        cursor =
                            if (overHeader) Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) else Cursor.getDefaultCursor()

                        val tip = getTooltipAt(e.x, e.y)
                        if (tip != null) {
                            val popup = getOrCreateTooltipPopup()
                            tooltipPane.text =
                                "<html><body style='color:$tooltipFgHex;white-space:nowrap;'>$tip</body></html>"
                            tooltipPane.size = Dimension(10000, 10000)
                            val pref = tooltipPane.preferredSize
                            val maxWidth = 500
                            if (pref.width > maxWidth) {
                                tooltipPane.text =
                                    "<html><body style='color:$tooltipFgHex;width:${maxWidth - 16}px;'>$tip</body></html>"
                                tooltipPane.size = Dimension(maxWidth, 10000)
                            }
                            popup.pack()
                            val screenPoint = e.locationOnScreen
                            popup.setLocation(screenPoint.x + 12, screenPoint.y - popup.height - 4)
                            popup.isVisible = true
                        } else {
                            tooltipPopup?.isVisible = false
                        }
                    }
                },
            )

            addMouseListener(
                object : MouseAdapter() {
                    override fun mouseExited(e: MouseEvent) {
                        tooltipPopup?.isVisible = false
                    }

                    override fun mouseClicked(e: MouseEvent) {
                        for ((index, row) in chartRows.withIndex()) {
                            val ry = rowYPositions[index]
                            val rh = rowHeight(index)
                            if (e.y >= ry && e.y < ry + rh && row is ChartRow.GroupHeader) {
                                toggleGroup(row.group.className)
                                break
                            }
                        }
                    }
                },
            )

            addHierarchyListener { e ->
                if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && !isShowing) {
                    tooltipPopup?.isVisible = false
                    tooltipPopup?.dispose()
                    tooltipPopup = null
                }
            }

            addMouseWheelListener { e ->
                val isZoomModifier = e.isControlDown || e.isMetaDown
                if (isZoomModifier) {
                    e.consume()
                    val factor = if (e.wheelRotation < 0) 1.2 else 1.0 / 1.2
                    zoomFactor = (zoomFactor * factor).coerceIn(0.1, 100.0)
                    revalidate()
                    repaint()
                } else {
                    scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(this, e, scrollPane))
                }
            }
        }

        private fun basePpmFor(availableWidth: Int): Double {
            if (testResults.isEmpty()) return 0.5
            val maxEnd = testResults.maxOf { it.startTime + it.durationMs }
            if (maxEnd <= 0) return 0.5
            val chartArea = availableWidth - leftMargin
            return if (chartArea > 0) chartArea.toDouble() / maxEnd else 0.5
        }

        fun zoomIn() {
            zoomFactor = (zoomFactor * 1.5).coerceIn(0.1, 100.0)
            revalidate(); repaint()
        }

        fun zoomOut() {
            zoomFactor = (zoomFactor / 1.5).coerceIn(0.1, 100.0)
            revalidate(); repaint()
        }

        fun resetZoom() {
            zoomFactor = 1.0
            revalidate(); repaint()
        }

        fun toggleSidebar() {
            sidebarVisible = !sidebarVisible
            revalidate(); repaint()
        }

        override fun getPreferredScrollableViewportSize(): Dimension = preferredSize
        override fun getScrollableUnitIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
            if (orientation == SwingConstants.VERTICAL) rowHeight + rowGap else 40

        override fun getScrollableBlockIncrement(visibleRect: Rectangle, orientation: Int, direction: Int) =
            if (orientation == SwingConstants.VERTICAL) visibleRect.height else visibleRect.width

        override fun getScrollableTracksViewportWidth(): Boolean = zoomFactor <= 1.0
        override fun getScrollableTracksViewportHeight() = false

        private fun rowHeight(index: Int): Int = when (chartRows[index]) {
            is ChartRow.GroupHeader -> headerHeight
            is ChartRow.TestBar -> rowHeight
        }

        private fun totalHeight(): Int {
            if (chartRows.isEmpty()) return topMargin + 20
            val lastIndex = chartRows.lastIndex
            return rowYPositions[lastIndex] + when (chartRows[lastIndex]) {
                is ChartRow.GroupHeader -> headerHeight + rowGap
                is ChartRow.TestBar -> rowHeight + rowGap
            } + 20
        }

        private fun getTooltipAt(x: Int, y: Int): String? {
            if (chartRows.isEmpty()) return null
            for ((index, row) in chartRows.withIndex()) {
                val ry = rowYPositions[index]
                val rh = rowHeight(index)
                if (y >= ry && y < ry + rh) {
                    return when (row) {
                        is ChartRow.GroupHeader -> {
                            if (x < leftMargin) ChartUtils.escapeHtml("${row.group.packageName}.${row.group.className}") else null
                        }

                        is ChartRow.TestBar -> {
                            val r = row.result
                            if (x < leftMargin) {
                                ChartUtils.escapeHtml(r.testName)
                            } else {
                                val effectivePpm = basePpmFor(width) * zoomFactor
                                val barX = leftMargin + (r.startTime * effectivePpm).toInt()
                                val barW = (r.durationMs * effectivePpm).toInt().coerceAtLeast(1)
                                if (x in barX..(barX + barW)) {
                                    "${ChartUtils.escapeHtml(r.testName)}<br>${r.durationMs}ms — ${r.status}"
                                } else null
                            }
                        }
                    }
                }
            }
            return null
        }

        override fun getPreferredSize(): Dimension {
            if (testResults.isEmpty()) return Dimension(400, 200)
            if (zoomFactor <= 1.0) {
                return Dimension(1, totalHeight())
            }
            val viewportWidth = scrollPane.viewport.width
            val availableWidth = if (viewportWidth > 0) viewportWidth else 1000
            val ppm = basePpmFor(availableWidth) * zoomFactor
            val maxEnd = testResults.maxOf { it.startTime + it.durationMs }
            val width = leftMargin + (maxEnd * ppm).toInt()
            return Dimension(width, totalHeight())
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2 = g as Graphics2D
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            if (testResults.isEmpty()) {
                g2.color = TestsAnalyzerColors.CHART_FG
                g2.drawString("No test results to display.", 20, 40)
                return
            }

            val effectivePpm = basePpmFor(width) * zoomFactor
            val maxEnd = testResults.maxOf { it.startTime + it.durationMs }

            drawXAxis(g2, maxEnd, effectivePpm)

            val fm = g2.fontMetrics
            for ((index, row) in chartRows.withIndex()) {
                val y = rowYPositions[index]
                when (row) {
                    is ChartRow.GroupHeader -> {
                        g2.color = TestsAnalyzerColors.CHART_GROUP_BG
                        g2.fillRect(0, y, width, headerHeight)
                        g2.color = TestsAnalyzerColors.CHART_GROUP_BORDER
                        g2.drawLine(0, y + headerHeight, width, y + headerHeight)

                        val collapsed = row.group.className in collapsedGroups
                        val arrow = if (collapsed) "\u25B6" else "\u25BC"
                        g2.color = TestsAnalyzerColors.CHART_FG
                        g2.font = g2.font.deriveFont(Font.BOLD, 11f)

                        val label = "$arrow ${row.group.className}"
                        if (sidebarVisible) {
                            val truncated = truncateText(label, leftMargin - 10, g2.fontMetrics)
                            g2.drawString(truncated, 4, y + (headerHeight + g2.fontMetrics.ascent) / 2 - 1)
                        } else {
                            g2.drawString(label, leftMargin + 4, y + (headerHeight + g2.fontMetrics.ascent) / 2 - 1)
                        }
                    }

                    is ChartRow.TestBar -> {
                        val result = row.result

                        if (sidebarVisible) {
                            g2.color = TestsAnalyzerColors.CHART_FG
                            g2.font = g2.font.deriveFont(Font.PLAIN, 11f)
                            val label = result.testName
                            val truncated = truncateText(label, leftMargin - 20, g2.fontMetrics)
                            g2.drawString(truncated, 14, y + rowHeight / 2 + fm.ascent / 2 - 2)
                        }

                        val barX = leftMargin + (result.startTime * effectivePpm).toInt()
                        val barW = (result.durationMs * effectivePpm).toInt().coerceAtLeast(1)
                        g2.color = TestsAnalyzerColors.statusColor(result.status)
                        g2.fillRoundRect(barX, y + 2, barW, rowHeight - 4, 4, 4)

                        val barLabel = "${result.durationMs}ms"
                        val barLabelWidth = fm.stringWidth(barLabel)
                        g2.color = if (barLabelWidth < barW - 8) TestsAnalyzerColors.WHITE else TestsAnalyzerColors.CHART_FG
                        val labelX = if (barLabelWidth < barW - 8) barX + 4 else barX + barW + 4
                        g2.drawString(barLabel, labelX, y + rowHeight / 2 + fm.ascent / 2 - 2)
                    }
                }
            }
        }

        private fun drawXAxis(g2: Graphics2D, maxEndMs: Long, ppm: Double) {
            g2.color = TestsAnalyzerColors.CHART_FG
            g2.font = g2.font.deriveFont(Font.PLAIN, 10f)

            val interval = ChartUtils.chooseInterval(maxEndMs)
            var tick = 0L
            while (tick <= maxEndMs) {
                val x = leftMargin + (tick * ppm).toInt()
                g2.drawLine(x, topMargin - 5, x, topMargin)
                val label = ChartUtils.formatMs(tick)
                g2.drawString(label, x + 2, topMargin - 8)
                tick += interval
            }

            val totalWidth = leftMargin + (maxEndMs * ppm).toInt()
            g2.drawLine(leftMargin, topMargin, totalWidth, topMargin)
        }

        private fun truncateText(text: String, maxWidth: Int, fm: FontMetrics): String {
            if (fm.stringWidth(text) <= maxWidth) return text
            var t = text
            while (t.isNotEmpty() && fm.stringWidth("$t...") > maxWidth) {
                t = t.dropLast(1)
            }
            return "$t..."
        }
    }
}
