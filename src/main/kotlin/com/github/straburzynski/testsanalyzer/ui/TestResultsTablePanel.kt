package com.github.straburzynski.testsanalyzer.ui

import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestStatus
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.JCheckBox
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableRowSorter

class TestResultsTablePanel(allResults: List<TestResult>) : JBPanel<TestResultsTablePanel>(BorderLayout()) {

    private val results = allResults.filter { it.type == TestResultType.TEST }

    private val columnNames = arrayOf("Class Name", "Test Name", "Package", "Time (ms)", "Status")
    private val tableModel = TestResultTableModel()
    private val sorter = TableRowSorter(tableModel)

    private var showSuccess = true
    private var showFailed = true
    private var showSkipped = true
    private var textFilter = ""

    init {
        val topPanel = JPanel(BorderLayout(0, 4))

        val filterRow = JPanel(FlowLayout(FlowLayout.LEFT, 8, 4))
        filterRow.add(JBLabel("Filter:"))

        val searchField = SearchTextField(false)
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onTextChanged(searchField.text)
            override fun removeUpdate(e: DocumentEvent) = onTextChanged(searchField.text)
            override fun changedUpdate(e: DocumentEvent) = onTextChanged(searchField.text)
        })
        filterRow.add(searchField)

        val cbSuccess = JCheckBox("Success", true).apply {
            foreground = TestsAnalyzerColors.STATUS_SUCCESS
            addActionListener { showSuccess = isSelected; applyFilters() }
        }
        val cbFailed = JCheckBox("Failed", true).apply {
            foreground = TestsAnalyzerColors.STATUS_FAILED
            addActionListener { showFailed = isSelected; applyFilters() }
        }
        val cbSkipped = JCheckBox("Skipped", true).apply {
            foreground = TestsAnalyzerColors.STATUS_SKIPPED
            addActionListener { showSkipped = isSelected; applyFilters() }
        }

        filterRow.add(cbSuccess)
        filterRow.add(cbFailed)
        filterRow.add(cbSkipped)

        topPanel.add(filterRow, BorderLayout.SOUTH)
        add(topPanel, BorderLayout.NORTH)

        val table = JBTable(tableModel)
        sorter.setComparator(3, Comparator.comparingLong<Any> { (it as Number).toLong() })
        sorter.sortKeys = listOf(RowSorter.SortKey(3, SortOrder.DESCENDING))
        table.rowSorter = sorter
        table.columnModel.getColumn(4).cellRenderer = StatusCellRenderer()

        val scrollPane = JBScrollPane(table)
        add(scrollPane, BorderLayout.CENTER)
    }

    private fun onTextChanged(text: String) {
        textFilter = text.trim()
        applyFilters()
    }

    private fun applyFilters() {
        val filters = mutableListOf<RowFilter<TestResultTableModel, Int>>()

        filters.add(object : RowFilter<TestResultTableModel, Int>() {
            override fun include(entry: Entry<out TestResultTableModel, out Int>): Boolean {
                val status = entry.getStringValue(4)
                return when (status) {
                    "SUCCESS" -> showSuccess
                    "FAILED" -> showFailed
                    "SKIPPED" -> showSkipped
                    else -> true
                }
            }
        })

        if (textFilter.isNotEmpty()) {
            filters.add(object : RowFilter<TestResultTableModel, Int>() {
                override fun include(entry: Entry<out TestResultTableModel, out Int>): Boolean {
                    val query = textFilter.lowercase()
                    for (col in 0..2) {
                        if (entry.getStringValue(col).lowercase().contains(query)) return true
                    }
                    return false
                }
            })
        }

        sorter.rowFilter = RowFilter.andFilter(filters)
    }

    private inner class TestResultTableModel : AbstractTableModel() {
        override fun getRowCount(): Int = results.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]

        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            3 -> Long::class.javaObjectType
            else -> String::class.java
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val r = results[rowIndex]
            return when (columnIndex) {
                0 -> r.className
                1 -> r.testName
                2 -> r.packageName
                3 -> r.durationMs
                4 -> r.status.name
                else -> ""
            }
        }
    }

    private class StatusCellRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            if (!isSelected) {
                val status = TestStatus.entries.find { it.name == value?.toString() }
                foreground = if (status != null) TestsAnalyzerColors.statusJBColor(status) else table.foreground
            }
            return comp
        }
    }
}
