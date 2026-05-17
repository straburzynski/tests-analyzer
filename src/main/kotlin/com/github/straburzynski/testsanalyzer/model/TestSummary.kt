package com.github.straburzynski.testsanalyzer.model

data class TestSummary(
    val total: Int,
    val passed: Int,
    val failed: Int,
    val skipped: Int,
    val cumulativeTimeMs: Long,
    val wallClockTimeMs: Long,
) {
    val savedTimeMs: Long get() = (cumulativeTimeMs - wallClockTimeMs).coerceAtLeast(0)

    /** Whether parallel execution saved meaningful time (> 5% of cumulative). */
    val hasParallelSavings: Boolean get() = wallClockTimeMs < cumulativeTimeMs &&
        savedTimeMs > cumulativeTimeMs * 0.05

    companion object {
        fun from(allResults: List<TestResult>): TestSummary {
            val testResults = allResults.filter { it.type == TestResultType.TEST }
            val suiteResults = allResults.filter { it.type == TestResultType.SUITE }

            val passed = testResults.count { it.status == TestStatus.SUCCESS }
            val failed = testResults.count { it.status == TestStatus.FAILED }
            val skipped = testResults.count { it.status == TestStatus.SKIPPED }
            val cumulativeTime = testResults.sumOf { it.durationMs }

            val minStart = suiteResults.minOfOrNull { it.startTime }
                ?: testResults.minOfOrNull { it.startTime } ?: 0L
            val maxEnd = suiteResults.maxOfOrNull { it.endTime }
                ?: testResults.maxOfOrNull { it.endTime } ?: 0L
            val wallClockTime = if (maxEnd > 0L) maxEnd - minStart else cumulativeTime

            return TestSummary(
                total = testResults.size,
                passed = passed,
                failed = failed,
                skipped = skipped,
                cumulativeTimeMs = cumulativeTime,
                wallClockTimeMs = wallClockTime,
            )
        }
    }
}
