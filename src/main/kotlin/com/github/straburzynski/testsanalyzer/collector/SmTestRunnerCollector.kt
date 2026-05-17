package com.github.straburzynski.testsanalyzer.collector

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsAdapter
import com.intellij.execution.testframework.sm.runner.SMTestProxy
import com.intellij.openapi.diagnostic.Logger
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestStatus
import com.github.straburzynski.testsanalyzer.util.ClassNameUtils
import com.github.straburzynski.testsanalyzer.util.TimestampUtils
import java.util.concurrent.ConcurrentHashMap

class SmTestRunnerCollector(
    private val onAllTestsFinished: ((List<TestResult>) -> Unit)? = null
) : SMTRunnerEventsAdapter() {

    private val log = Logger.getInstance(SmTestRunnerCollector::class.java)

    private val startTimestamps = ConcurrentHashMap<Int, Long>()
    private val endTimestamps = ConcurrentHashMap<Int, Long>()
    private val suiteStartTimestamps = ConcurrentHashMap<Int, Long>()
    private val suiteEndTimestamps = ConcurrentHashMap<Int, Long>()

    override fun onTestStarted(test: SMTestProxy) {
        startTimestamps[System.identityHashCode(test)] = System.currentTimeMillis()
    }

    override fun onTestFinished(test: SMTestProxy) {
        endTimestamps[System.identityHashCode(test)] = System.currentTimeMillis()
    }

    override fun onTestFailed(test: SMTestProxy) {
        endTimestamps[System.identityHashCode(test)] = System.currentTimeMillis()
    }

    override fun onTestIgnored(test: SMTestProxy) {
        val id = System.identityHashCode(test)
        val now = System.currentTimeMillis()
        startTimestamps.putIfAbsent(id, now)
        endTimestamps[id] = now
    }

    override fun onSuiteStarted(suite: SMTestProxy) {
        suiteStartTimestamps[System.identityHashCode(suite)] = System.currentTimeMillis()
    }

    override fun onSuiteFinished(suite: SMTestProxy) {
        suiteEndTimestamps[System.identityHashCode(suite)] = System.currentTimeMillis()
    }

    override fun onTestingFinished(testsRoot: SMTestProxy.SMRootTestProxy) {
        log.info("Test execution finished, collecting results from SMTestProxy tree")
        val results = collectResults(testsRoot)
        val normalized = TimestampUtils.normalizeTimestamps(results)
        onAllTestsFinished?.invoke(normalized)
    }

    private fun collectResults(root: SMTestProxy): List<TestResult> {
        val results = mutableListOf<TestResult>()
        collectFromProxy(root, results)
        return results
    }

    private fun collectFromProxy(proxy: SMTestProxy, results: MutableList<TestResult>) {
        if (proxy.isSuite) {
            for (child in proxy.children) {
                collectFromProxy(child, results)
            }
            if (proxy.children.isNotEmpty() && proxy !is SMTestProxy.SMRootTestProxy) {
                results.add(proxyToTestResult(proxy, TestResultType.SUITE))
            }
        } else {
            results.add(proxyToTestResult(proxy, TestResultType.TEST))
        }
    }

    private fun proxyToTestResult(proxy: SMTestProxy, type: TestResultType): TestResult {
        val id = System.identityHashCode(proxy)
        val durationMs = proxy.duration ?: 0L

        val startTimeMs: Long
        val endTimeMs: Long

        if (type == TestResultType.SUITE) {
            startTimeMs = suiteStartTimestamps[id] ?: 0L
            endTimeMs = suiteEndTimestamps[id] ?: (startTimeMs + durationMs)
        } else {
            startTimeMs = startTimestamps[id] ?: 0L
            endTimeMs = endTimestamps[id] ?: (startTimeMs + durationMs)
        }

        val status = when {
            proxy.isIgnored -> TestStatus.SKIPPED
            proxy.isPassed -> TestStatus.SUCCESS
            proxy.isDefect -> TestStatus.FAILED
            else -> TestStatus.SUCCESS
        }

        val (className, testName) = extractNames(proxy, type)
        val packageName = if (type == TestResultType.SUITE) "" else ClassNameUtils.extractPackage(className)
        val simpleClassName = if (type == TestResultType.SUITE) className else ClassNameUtils.extractSimpleClassName(className)

        return TestResult(
            className = simpleClassName,
            testName = testName,
            packageName = packageName,
            durationMs = durationMs,
            status = status,
            startTime = startTimeMs,
            endTime = endTimeMs,
            type = type
        )
    }

    private fun extractNames(proxy: SMTestProxy, type: TestResultType): Pair<String, String> {
        if (type == TestResultType.SUITE) {
            return Pair("suite", proxy.name ?: "unknown")
        }

        val locationUrl = proxy.locationUrl
        if (locationUrl != null) {
            val withoutProtocol = locationUrl.substringAfter("://")
            val parts = withoutProtocol.split("/", limit = 2)
            val fqClassName = parts.getOrElse(0) { "" }
            val methodName = parts.getOrElse(1) { proxy.name ?: "unknown" }
            if (fqClassName.isNotEmpty()) {
                return Pair(fqClassName, methodName)
            }
        }

        val parentName = proxy.parent?.name ?: ""
        return Pair(parentName, proxy.name ?: "unknown")
    }
}
