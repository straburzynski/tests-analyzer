package com.github.straburzynski.testsanalyzer.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.model.TestStatus.FAILED
import com.github.straburzynski.testsanalyzer.model.TestStatus.SUCCESS

class TestResultTest {

    @Test
    fun `toChartGroups groups by class name`() {
        val results = listOf(
            TestResult("ClassA", "test1", "pkg", 100, SUCCESS, startTime = 0, endTime = 100),
            TestResult("ClassA", "test2", "pkg", 200, SUCCESS, startTime = 100, endTime = 300),
            TestResult("ClassB", "test3", "pkg", 150, FAILED, startTime = 0, endTime = 150),
        )

        val groups = results.toChartGroups()

        assertEquals(2, groups.size)
        assertEquals("ClassA", groups[0].className)
        assertEquals(2, groups[0].tests.size)
        assertEquals("ClassB", groups[1].className)
        assertEquals(1, groups[1].tests.size)
    }

    @Test
    fun `toChartGroups excludes SUITE entries`() {
        val results = listOf(
            TestResult("ClassA", "test1", "pkg", 100, SUCCESS, type = TestResultType.TEST),
            TestResult("suite", "ClassA", "", 100, SUCCESS, type = TestResultType.SUITE),
        )

        val groups = results.toChartGroups()

        assertEquals(1, groups.size)
        assertEquals(1, groups[0].tests.size)
    }

    @Test
    fun `toChartGroups sorts tests by startTime within group`() {
        val results = listOf(
            TestResult("ClassA", "test2", "pkg", 100, SUCCESS, startTime = 200, endTime = 300),
            TestResult("ClassA", "test1", "pkg", 100, SUCCESS, startTime = 0, endTime = 100),
        )

        val groups = results.toChartGroups()

        assertEquals("test1", groups[0].tests[0].testName)
        assertEquals("test2", groups[0].tests[1].testName)
    }

    @Test
    fun `toChartGroups returns empty list for empty input`() {
        assertTrue(emptyList<TestResult>().toChartGroups().isEmpty())
    }

    @Test
    fun `TestGroup groupStartMs returns minimum startTime`() {
        val group = TestGroup(
            "ClassA", "pkg", listOf(
                TestResult("ClassA", "test1", "pkg", 100, SUCCESS, startTime = 200, endTime = 300),
                TestResult("ClassA", "test2", "pkg", 100, SUCCESS, startTime = 50, endTime = 150),
            )
        )

        assertEquals(50, group.groupStartMs)
    }
}
