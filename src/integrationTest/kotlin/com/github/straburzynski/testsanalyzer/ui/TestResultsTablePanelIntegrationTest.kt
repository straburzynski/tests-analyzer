package com.github.straburzynski.testsanalyzer.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.TestDataFactory
import com.github.straburzynski.testsanalyzer.model.TestResultType
import javax.swing.JCheckBox

class TestResultsTablePanelIntegrationTest {

    @Test
    fun `table shows correct number of rows for standard results`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)

        assertNotNull(table, "Panel should contain a JBTable")
        assertEquals(
            TestDataFactory.EXPECTED_TOTAL, table!!.model.rowCount,
            "Table model should have ${TestDataFactory.EXPECTED_TOTAL} rows (excluding SUITE)",
        )
    }

    @Test
    fun `table excludes SUITE entries from rows`() {
        val results = TestDataFactory.createStandardResults()
        val suiteCount = results.count { it.type == TestResultType.SUITE }
        assertTrue(suiteCount > 0, "Test data should include SUITE entries")

        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!
        val testOnly = results.filter { it.type == TestResultType.TEST }
        assertEquals(
            testOnly.size, table.model.rowCount,
            "Table should only show TEST entries, not SUITE",
        )
    }

    @Test
    fun `table has correct column names`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        val expectedColumns = listOf("Class Name", "Test Name", "Package", "Time (ms)", "Status")
        assertEquals(expectedColumns.size, table.model.columnCount, "Should have 5 columns")
        for (i in expectedColumns.indices) {
            assertEquals(
                expectedColumns[i], table.model.getColumnName(i),
                "Column $i should be named '${expectedColumns[i]}'",
            )
        }
    }

    @Test
    fun `table data contains all test names`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        val testOnly = results.filter { it.type == TestResultType.TEST }
        val tableTestNames = (0 until table.model.rowCount).map { table.model.getValueAt(it, 1) as String }.toSet()

        for (result in testOnly) {
            assertTrue(
                tableTestNames.contains(result.testName),
                "Table should contain test: ${result.testName}",
            )
        }
    }

    @Test
    fun `table status column contains correct status values`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        val statuses = (0 until table.model.rowCount).map { table.model.getValueAt(it, 4) as String }
        val successCount = statuses.count { it == "SUCCESS" }
        val failedCount = statuses.count { it == "FAILED" }
        val skippedCount = statuses.count { it == "SKIPPED" }

        assertEquals(TestDataFactory.EXPECTED_PASSED, successCount, "Should have correct SUCCESS count")
        assertEquals(TestDataFactory.EXPECTED_FAILED, failedCount, "Should have correct FAILED count")
        assertEquals(TestDataFactory.EXPECTED_SKIPPED, skippedCount, "Should have correct SKIPPED count")
    }

    @Test
    fun `unchecking SUCCESS filter hides success rows`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        // Finds the SUCCESS checkbox and uncheck it
        val successCheckbox = findCheckboxByText(panel, "Success")
        assertNotNull(successCheckbox, "Should have a Success checkbox")
        successCheckbox!!.isSelected = false
        successCheckbox.actionListeners.forEach { it.actionPerformed(null) }

        // After filtering, visible row count should exclude SUCCESS
        val visibleRows = table.rowCount
        val expectedVisible = TestDataFactory.EXPECTED_FAILED + TestDataFactory.EXPECTED_SKIPPED
        assertEquals(
            expectedVisible, visibleRows,
            "After unchecking SUCCESS, should show only FAILED + SKIPPED rows",
        )
    }

    @Test
    fun `unchecking all filters shows zero rows`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        // Uncheck all status checkboxes
        for (label in listOf("Success", "Failed", "Skipped")) {
            val cb = findCheckboxByText(panel, label)!!
            cb.isSelected = false
            cb.actionListeners.forEach { it.actionPerformed(null) }
        }

        assertEquals(0, table.rowCount, "Unchecking all filters should show zero rows")
    }

    @Test
    fun `table duration column contains correct time values`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        val testOnly = results.filter { it.type == TestResultType.TEST }
        val tableDurations = (0 until table.model.rowCount).map { table.model.getValueAt(it, 3) as Long }.toSet()

        for (result in testOnly) {
            assertTrue(
                tableDurations.contains(result.durationMs),
                "Table should contain duration ${result.durationMs}ms",
            )
        }
    }

    @Test
    fun `table with empty results has zero rows`() {
        val panel = TestResultsTablePanel(TestDataFactory.createEmptyResults())
        val table = findTable(panel)!!
        assertEquals(0, table.model.rowCount, "Empty results should produce zero rows")
    }

    @Test
    fun `table is sorted by duration descending by default`() {
        val results = TestDataFactory.createStandardResults()
        val panel = TestResultsTablePanel(results)
        val table = findTable(panel)!!

        if (table.rowCount > 1) {
            val sortKeys = table.rowSorter?.sortKeys
            assertNotNull(sortKeys, "Table should have sort keys")
            assertTrue(sortKeys!!.isNotEmpty(), "Table should have active sort")
            assertEquals(3, sortKeys[0].column, "Should be sorted by Time (ms) column (index 3)")
            assertEquals(javax.swing.SortOrder.DESCENDING, sortKeys[0].sortOrder, "Should be sorted descending")
        }
    }

    private fun findTable(component: java.awt.Component): JBTable? {
        if (component is JBTable) return component
        if (component is JBScrollPane) {
            val viewport = component.viewport
            if (viewport?.view is JBTable) return viewport.view as JBTable
        }
        if (component is java.awt.Container) {
            for (child in component.components) {
                val found = findTable(child)
                if (found != null) return found
            }
        }
        return null
    }

    private fun findCheckboxByText(component: java.awt.Component, text: String): JCheckBox? {
        if (component is JCheckBox && component.text == text) return component
        if (component is java.awt.Container) {
            for (child in component.components) {
                val found = findCheckboxByText(child, text)
                if (found != null) return found
            }
        }
        return null
    }
}
