package com.github.straburzynski.testsanalyzer.ui

import com.intellij.ui.components.JBScrollPane
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.SwingTestUtil.findAllButtons
import com.github.straburzynski.testsanalyzer.SwingTestUtil.findComponent
import com.github.straburzynski.testsanalyzer.TestDataFactory

class GanttChartPanelIntegrationTest {

    @Test
    fun `panel contains scroll pane with chart`() {
        val results = TestDataFactory.createStandardResults()
        val panel = GanttChartPanel(results)
        panel.setSize(1000, 600)
        panel.doLayout()

        val scrollPane = findComponent(panel, JBScrollPane::class.java)
        assertNotNull(scrollPane, "GanttChartPanel should contain a JBScrollPane")
    }

    @Test
    fun `panel has toolbar with zoom and sidebar buttons`() {
        val results = TestDataFactory.createStandardResults()
        val panel = GanttChartPanel(results)

        val buttons = findAllButtons(panel)
        val tooltips = buttons.map { it.toolTipText }.toSet()

        assertTrue(tooltips.contains("Zoom In"), "Should have Zoom In button")
        assertTrue(tooltips.contains("Zoom Out"), "Should have Zoom Out button")
        assertTrue(tooltips.contains("Reset"), "Should have Reset button")
        assertTrue(tooltips.contains("Toggle Sidebar"), "Should have Toggle Sidebar button")
    }

    @Test
    fun `panel has exactly 4 toolbar buttons`() {
        val results = TestDataFactory.createStandardResults()
        val panel = GanttChartPanel(results)

        val buttons = findAllButtons(panel)
        assertEquals(4, buttons.size, "Should have exactly 4 toolbar buttons (sidebar, zoom in, zoom out, reset)")
    }

    @Test
    fun `panel with empty results is rendered`() {
        assertDoesNotThrow {
            val panel = GanttChartPanel(TestDataFactory.createEmptyResults())
            panel.setSize(800, 400)
            panel.doLayout()
        }
    }

}
