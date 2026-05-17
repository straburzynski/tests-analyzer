package com.github.straburzynski.testsanalyzer.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.model.TestResultType.SUITE
import com.github.straburzynski.testsanalyzer.model.TestResultType.TEST
import com.github.straburzynski.testsanalyzer.model.TestStatus.FAILED
import com.github.straburzynski.testsanalyzer.model.TestStatus.SKIPPED
import com.github.straburzynski.testsanalyzer.model.TestStatus.SUCCESS

class TestSummaryTest {

    private fun testResult(
        status: TestStatus,
        durationMs: Long = 100,
        startTime: Long = 0,
        endTime: Long = 0,
        type: TestResultType = TEST,
    ) = TestResult("Class", "test", "pkg", durationMs, status, startTime, endTime, type)

    @Test
    fun `from computes correct counts`() {
        val results = listOf(
            testResult(SUCCESS),
            testResult(SUCCESS),
            testResult(FAILED),
            testResult(SKIPPED),
        )

        val summary = TestSummary.from(results)

        assertEquals(4, summary.total)
        assertEquals(2, summary.passed)
        assertEquals(1, summary.failed)
        assertEquals(1, summary.skipped)
    }

    @Test
    fun `from excludes SUITE entries from counts`() {
        val results = listOf(
            testResult(SUCCESS),
            testResult(SUCCESS, type = SUITE),
        )

        val summary = TestSummary.from(results)

        assertEquals(1, summary.total)
    }

    @Test
    fun `from computes cumulative time`() {
        val results = listOf(
            testResult(SUCCESS, durationMs = 100),
            testResult(SUCCESS, durationMs = 200),
        )

        val summary = TestSummary.from(results)

        assertEquals(300, summary.cumulativeTimeMs)
    }

    @Test
    fun `from computes wall clock time from test start and end`() {
        val results = listOf(
            testResult(SUCCESS, durationMs = 100, startTime = 0, endTime = 100),
            testResult(SUCCESS, durationMs = 100, startTime = 50, endTime = 150),
        )

        val summary = TestSummary.from(results)

        assertEquals(150, summary.wallClockTimeMs)
    }

    @Test
    fun `hasParallelSavings is true when wall time is significantly less than cumulative`() {
        val summary = TestSummary(
            total = 10, passed = 10, failed = 0, skipped = 0,
            cumulativeTimeMs = 1000, wallClockTimeMs = 500,
        )

        assertTrue(summary.hasParallelSavings)
        assertEquals(500, summary.savedTimeMs)
    }

    @Test
    fun `hasParallelSavings is false when times are equal`() {
        val summary = TestSummary(
            total = 5, passed = 5, failed = 0, skipped = 0,
            cumulativeTimeMs = 1000, wallClockTimeMs = 1000,
        )

        assertFalse(summary.hasParallelSavings)
    }

    @Test
    fun `from handles empty list`() {
        val summary = TestSummary.from(emptyList())

        assertEquals(0, summary.total)
        assertEquals(0, summary.passed)
        assertEquals(0L, summary.cumulativeTimeMs)
    }
}
