package com.github.straburzynski.testsanalyzer.export

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import com.github.straburzynski.testsanalyzer.TestDataFactory
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import java.io.File

class HtmlExporterIntegrationTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var standardResults: List<TestResult>
    private lateinit var standardHtml: String

    @BeforeEach
    fun setUp() {
        standardResults = TestDataFactory.createStandardResults()
        val file = File(tempDir, "output.html")
        HtmlExporter.exportToHtml(standardResults, file)
        standardHtml = file.readText()
    }

    private fun exportAndRead(results: List<TestResult>, fileName: String): String {
        val file = File(tempDir, fileName)
        HtmlExporter.exportToHtml(results, file)
        return file.readText()
    }

    @Test
    fun `exported HTML contains correct number of table rows`() {
        val rowCount = Regex("""<tr data-status="""").findAll(standardHtml).count()
        assertEquals(
            TestDataFactory.EXPECTED_TOTAL, rowCount,
            "HTML should contain exactly ${TestDataFactory.EXPECTED_TOTAL} result rows",
        )
    }

    @Test
    fun `exported HTML contains correct summary counts`() {
        assertTrue(standardHtml.contains("Total: ${TestDataFactory.EXPECTED_TOTAL}"), "Should show total count")
        assertTrue(standardHtml.contains("Passed: ${TestDataFactory.EXPECTED_PASSED}"), "Should show passed count")
        assertTrue(standardHtml.contains("Failed: ${TestDataFactory.EXPECTED_FAILED}"), "Should show failed count")
        assertTrue(standardHtml.contains("Skipped: ${TestDataFactory.EXPECTED_SKIPPED}"), "Should show skipped count")
    }

    @Test
    fun `exported HTML contains theme toggle button`() {
        assertTrue(standardHtml.contains("Toggle Light/Dark"), "Should contain theme toggle button")
        assertTrue(standardHtml.contains("toggleTheme()"), "Should contain toggleTheme JavaScript function call")
    }

    @Test
    fun `exported HTML contains SVG Gantt chart with correct group count`() {
        assertTrue(standardHtml.contains("<svg"), "Should contain SVG element for Gantt chart")

        for ((className, _) in TestDataFactory.EXPECTED_TESTS_PER_GROUP) {
            assertTrue(
                standardHtml.contains(className),
                "Gantt chart should contain group for $className",
            )
        }
    }

    @Test
    fun `exported HTML contains correct number of chart bars per group`() {
        val testOnly = standardResults.filter { it.type == TestResultType.TEST }

        val barCount = Regex("""<rect[^>]*rx="3"""").findAll(standardHtml).count()
        assertEquals(
            testOnly.size, barCount,
            "SVG should contain exactly ${testOnly.size} test bars (rounded rects)",
        )
    }

    @Test
    fun `exported HTML contains tab buttons for Results Table and Timeline Chart`() {
        assertTrue(standardHtml.contains("Results Table"), "Should contain Results Table tab")
        assertTrue(standardHtml.contains("Timeline Chart"), "Should contain Timeline Chart tab")
        assertTrue(standardHtml.contains("switchTab"), "Should contain tab switching JavaScript")
    }

    @Test
    fun `exported HTML handles empty results gracefully`() {
        val html = exportAndRead(TestDataFactory.createEmptyResults(), "empty.html")
        assertTrue(html.contains("<!DOCTYPE html>"), "Should produce valid HTML")
        assertTrue(html.contains("Total: 0"), "Should show zero total")
        assertTrue(html.contains("Passed: 0"), "Should show zero passed")

        val rowCount = Regex("""<tr data-status="""").findAll(html).count()
        assertEquals(0, rowCount, "Empty results should produce no table rows")
    }

    @Test
    fun `exported HTML escapes special characters in test names`() {
        val html = exportAndRead(TestDataFactory.createResultsWithSpecialChars(), "special.html")
        assertFalse(html.contains("<script>alert"), "Script tags should be escaped in output")
        assertTrue(
            html.contains("&lt;") || html.contains("&amp;"),
            "Special characters should be HTML-escaped",
        )
    }

    @Test
    fun `exported HTML is a valid complete document`() {
        assertTrue(standardHtml.startsWith("<!DOCTYPE html>"), "Should start with DOCTYPE")
        assertTrue(standardHtml.contains("<html"), "Should contain html tag")
        assertTrue(standardHtml.contains("</html>"), "Should contain closing html tag")
        assertTrue(standardHtml.contains("<head>") || standardHtml.contains("<head "), "Should contain head section")
        assertTrue(standardHtml.contains("<body>") || standardHtml.contains("<body "), "Should contain body section")
        assertTrue(standardHtml.contains("</body>"), "Should contain closing body tag")
    }

    @Test
    fun `exported HTML contains all test names from results`() {
        val testOnly = standardResults.filter { it.type == TestResultType.TEST }
        for (result in testOnly) {
            assertTrue(
                standardHtml.contains(result.testName),
                "HTML should contain test name: ${result.testName}",
            )
        }
    }
}
