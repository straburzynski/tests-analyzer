package com.github.straburzynski.testsanalyzer.ui

import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.github.straburzynski.testsanalyzer.export.HtmlExporter
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestSummary
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

class TestsAnalyzerDialog(
    private val project: Project,
    private val results: List<TestResult>
) : DialogWrapper(project, true) {

    private val summary = TestSummary.from(results)

    init {
        title = "Tests Analyzer — ${summary.total} tests"
        setOKButtonText("Close")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout(0, 4))

        val summaryPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        val summaryPrefix = "Total: ${summary.total} tests, ${summary.passed} passed, ${summary.failed} failed, ${summary.skipped} skipped, "
        summaryPanel.add(JBLabel(summaryPrefix))

        if (summary.hasParallelSavings) {
            summaryPanel.add(JBLabel("Wall time: ${formatTotalTime(summary.wallClockTimeMs)}").apply {
                toolTipText = "Elapsed real time from the first test start to the last test finish (includes parallel execution)"
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            })
            summaryPanel.add(JBLabel(", "))
            summaryPanel.add(JBLabel("Cumulative: ${formatTotalTime(summary.cumulativeTimeMs)}").apply {
                toolTipText = "Sum of all individual test durations (as if tests ran sequentially)"
                cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            })
            if (summary.savedTimeMs > 0) {
                summaryPanel.add(JBLabel(", Saved: ${formatTotalTime(summary.savedTimeMs)}").apply {
                    toolTipText = "Time saved by running tests in parallel (cumulative minus wall time)"
                    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                })
            }
        } else {
            summaryPanel.add(JBLabel("Total time: ${formatTotalTime(summary.cumulativeTimeMs)}"))
        }

        val summaryRow = JPanel(BorderLayout())
        summaryRow.add(summaryPanel, BorderLayout.CENTER)
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 4, 0))
        val exportButton = JButton("Export HTML")
        exportButton.addActionListener { exportResults() }
        buttonPanel.add(exportButton)
        summaryRow.add(buttonPanel, BorderLayout.EAST)

        panel.add(summaryRow, BorderLayout.NORTH)

        val tabbedPane = JBTabbedPane()
        tabbedPane.addTab("Results Table", TestResultsTablePanel(results))
        tabbedPane.addTab("Timeline Chart", GanttChartPanel(results))
        panel.add(tabbedPane, BorderLayout.CENTER)
        panel.preferredSize = Dimension(1200, 750)
        return panel
    }

    override fun createActions() = arrayOf(okAction)

    private fun exportResults() {
        val descriptor = FileSaverDescriptor("Export Test Results", "Save test results as HTML", "html")
        val wrapper = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project)
        val result = wrapper.save(null as VirtualFile?, "test-results.html") ?: return
        HtmlExporter.exportToHtml(results, result.file)
    }

    private fun formatTotalTime(ms: Long): String = when {
        ms >= 60_000 -> "${ms}ms (${"%.1f".format(ms / 60_000.0)} minutes)"
        ms >= 10_000 -> "${ms}ms (${"%.1f".format(ms / 1000.0)} seconds)"
        else -> "${ms}ms"
    }
}
