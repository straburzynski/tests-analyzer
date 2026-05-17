package com.github.straburzynski.testsanalyzer.util

import com.github.straburzynski.testsanalyzer.model.TestResult

object TimestampUtils {

    fun normalizeTimestamps(results: List<TestResult>): List<TestResult> {
        val minStart = results.filter { it.startTime > 0 }.minOfOrNull { it.startTime } ?: return results
        return results.map {
            it.copy(
                startTime = it.startTime - minStart,
                endTime = it.endTime - minStart
            )
        }
    }
}
