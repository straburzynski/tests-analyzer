package com.github.straburzynski.testsanalyzer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestStatus
import com.github.straburzynski.testsanalyzer.model.TestStatus.SUCCESS

class TimestampUtilsTest {

    @Test
    fun `normalizeTimestamps shifts all timestamps so minimum starts at zero`() {
        val results = listOf(
            TestResult("A", "test1", "pkg", 100, SUCCESS, startTime = 1000, endTime = 1100),
            TestResult("B", "test2", "pkg", 200, SUCCESS, startTime = 1200, endTime = 1400),
        )

        val normalized = TimestampUtils.normalizeTimestamps(results)

        assertEquals(0L, normalized[0].startTime)
        assertEquals(100L, normalized[0].endTime)
        assertEquals(200L, normalized[1].startTime)
        assertEquals(400L, normalized[1].endTime)
    }

    @Test
    fun `normalizeTimestamps returns original list when all startTimes are zero`() {
        val results = listOf(
            TestResult("A", "test1", "pkg", 100, SUCCESS, startTime = 0, endTime = 0),
        )

        val normalized = TimestampUtils.normalizeTimestamps(results)

        assertEquals(results, normalized)
    }

    @Test
    fun `normalizeTimestamps handles empty list`() {
        val normalized = TimestampUtils.normalizeTimestamps(emptyList())
        assertEquals(emptyList<TestResult>(), normalized)
    }

    @Test
    fun `normalizeTimestamps ignores zero startTimes when finding minimum`() {
        val results = listOf(
            TestResult("A", "test1", "pkg", 0, TestStatus.SKIPPED, startTime = 0, endTime = 0),
            TestResult("B", "test2", "pkg", 100, SUCCESS, startTime = 500, endTime = 600),
        )

        val normalized = TimestampUtils.normalizeTimestamps(results)

        assertEquals(-500L, normalized[0].startTime)
        assertEquals(0L, normalized[1].startTime)
        assertEquals(100L, normalized[1].endTime)
    }
}
