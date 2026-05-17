package com.github.straburzynski.testsanalyzer.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.ui.components.JBTabbedPane
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.BaseIntegrationTest
import com.github.straburzynski.testsanalyzer.SwingTestUtil.findButton
import com.github.straburzynski.testsanalyzer.SwingTestUtil.findComponent
import com.github.straburzynski.testsanalyzer.TestDataFactory
import com.github.straburzynski.testsanalyzer.model.TestResult
import javax.swing.JComponent

class TestsAnalyzerDialogIntegrationTest : BaseIntegrationTest() {

    private fun createDialogOnEdt(results: List<TestResult>): TestsAnalyzerDialog {
        var dialog: TestsAnalyzerDialog? = null
        ApplicationManager.getApplication().invokeAndWait {
            dialog = TestsAnalyzerDialog(project, results)
        }
        return dialog!!
    }

    private fun createCenterPanelOnEdt(dialog: TestsAnalyzerDialog): JComponent {
        var panel: JComponent? = null
        ApplicationManager.getApplication().invokeAndWait {
            val method = dialog.javaClass.getDeclaredMethod("createCenterPanel")
            method.isAccessible = true
            panel = method.invoke(dialog) as JComponent
        }
        return panel!!
    }

    private fun closeDialogOnEdt(dialog: TestsAnalyzerDialog) {
        ApplicationManager.getApplication().invokeAndWait { dialog.close(0) }
    }

    @Test
    fun `dialog title contains total test count`() {
        val dialog = createDialogOnEdt(TestDataFactory.createStandardResults())
        try {
            assertTrue(
                dialog.title.contains("${TestDataFactory.EXPECTED_TOTAL}"),
                "Dialog title should contain total test count, got: ${dialog.title}",
            )
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    @Test
    fun `dialog center panel contains summary with correct counts`() {
        val dialog = createDialogOnEdt(TestDataFactory.createStandardResults())
        try {
            val centerPanel = createCenterPanelOnEdt(dialog)
            val summaryText = collectAllText(centerPanel)
            assertTrue(
                summaryText.contains("${TestDataFactory.EXPECTED_TOTAL} tests"),
                "Summary should contain total count, got: $summaryText",
            )
            assertTrue(
                summaryText.contains("${TestDataFactory.EXPECTED_PASSED} passed"),
                "Summary should contain passed count, got: $summaryText",
            )
            assertTrue(
                summaryText.contains("${TestDataFactory.EXPECTED_FAILED} failed"),
                "Summary should contain failed count, got: $summaryText",
            )
            assertTrue(
                summaryText.contains("${TestDataFactory.EXPECTED_SKIPPED} skipped"),
                "Summary should contain skipped count, got: $summaryText",
            )
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    @Test
    fun `dialog has exactly two tabs - Results Table and Timeline Chart`() {
        val dialog = createDialogOnEdt(TestDataFactory.createStandardResults())
        try {
            val centerPanel = createCenterPanelOnEdt(dialog)
            val tabbedPane = findComponent(centerPanel, JBTabbedPane::class.java)

            assertNotNull(tabbedPane, "Dialog should contain a JBTabbedPane")
            org.junit.jupiter.api.Assertions.assertEquals(2, tabbedPane!!.tabCount, "Should have exactly 2 tabs")
            org.junit.jupiter.api.Assertions.assertEquals(
                "Results Table",
                tabbedPane.getTitleAt(0),
                "First tab should be Results Table",
            )
            org.junit.jupiter.api.Assertions.assertEquals(
                "Timeline Chart",
                tabbedPane.getTitleAt(1),
                "Second tab should be Timeline Chart",
            )
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    @Test
    fun `dialog contains Export HTML button`() {
        val dialog = createDialogOnEdt(TestDataFactory.createStandardResults())
        try {
            val centerPanel = createCenterPanelOnEdt(dialog)
            val exportButton = findButton(centerPanel, "Export HTML")
            assertNotNull(exportButton, "Dialog should contain an Export HTML button")
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    @Test
    fun `dialog Results Table tab contains TestResultsTablePanel`() {
        val dialog = createDialogOnEdt(TestDataFactory.createStandardResults())
        try {
            val centerPanel = createCenterPanelOnEdt(dialog)
            val tabbedPane = findComponent(centerPanel, JBTabbedPane::class.java)!!
            val tableTab = tabbedPane.getComponentAt(0)
            assertTrue(
                tableTab is TestResultsTablePanel,
                "First tab should contain TestResultsTablePanel, got: ${tableTab?.javaClass?.name}",
            )
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    @Test
    fun `dialog Timeline Chart tab contains GanttChartPanel`() {
        val dialog = createDialogOnEdt(TestDataFactory.createStandardResults())
        try {
            val centerPanel = createCenterPanelOnEdt(dialog)
            val tabbedPane = findComponent(centerPanel, JBTabbedPane::class.java)!!
            val chartTab = tabbedPane.getComponentAt(1)
            assertTrue(
                chartTab is GanttChartPanel,
                "Second tab should contain GanttChartPanel, got: ${chartTab?.javaClass?.name}",
            )
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    @Test
    fun `dialog with empty results shows zero in title`() {
        val dialog = createDialogOnEdt(TestDataFactory.createEmptyResults())
        try {
            assertTrue(dialog.title.contains("0"), "Empty results should show 0 in title")
        } finally {
            closeDialogOnEdt(dialog)
        }
    }

    private fun collectAllText(component: java.awt.Component): String {
        val sb = StringBuilder()
        collectTextRecursive(component, sb)
        return sb.toString()
    }

    private fun collectTextRecursive(component: java.awt.Component, sb: StringBuilder) {
        when (component) {
            is javax.swing.JLabel -> sb.append(component.text).append(" ")
            is javax.swing.AbstractButton -> sb.append(component.text).append(" ")
        }
        if (component is java.awt.Container) {
            for (child in component.components) {
                collectTextRecursive(child, sb)
            }
        }
    }
}
