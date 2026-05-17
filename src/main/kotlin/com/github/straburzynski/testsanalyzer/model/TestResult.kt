package com.github.straburzynski.testsanalyzer.model

enum class TestStatus { SUCCESS, FAILED, SKIPPED }

enum class TestResultType { TEST, SUITE }

data class TestResult(
    val className: String,
    val testName: String,
    val packageName: String,
    val durationMs: Long,
    val status: TestStatus,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val type: TestResultType = TestResultType.TEST
)

data class TestGroup(
    val className: String,
    val packageName: String,
    val tests: List<TestResult>
) {
    val groupStartMs: Long get() = tests.minOfOrNull { it.startTime } ?: 0L
}

fun List<TestResult>.toChartGroups(): List<TestGroup> {
    return filter { it.type == TestResultType.TEST }
        .groupBy { "${it.packageName}.${it.className}" }
        .map { (_, tests) ->
            val first = tests.first()
            TestGroup(
                className = first.className,
                packageName = first.packageName,
                tests = tests.sortedBy { it.startTime }
            )
        }
        .sortedBy { it.groupStartMs }
}
