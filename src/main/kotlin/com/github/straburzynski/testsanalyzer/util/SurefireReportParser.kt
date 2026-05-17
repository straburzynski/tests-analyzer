package com.github.straburzynski.testsanalyzer.util

import com.intellij.openapi.diagnostic.Logger
import org.w3c.dom.Element
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestStatus
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Parses Maven Surefire/Failsafe XML test reports into [TestResult] entries.
 *
 * Standard report directory: target/surefire-reports/ (or target/failsafe-reports/).
 * Each XML file follows the JUnit XML format:
 * ```xml
 * <testsuite name="com.example.MyTest" tests="3" failures="1" errors="0" skipped="1" time="1.234">
 *   <testcase name="testMethod" classname="com.example.MyTest" time="0.123"/>
 *   <testcase name="failingTest" classname="com.example.MyTest" time="0.456">
 *     <failure message="expected true">stacktrace</failure>
 *   </testcase>
 *   <testcase name="skippedTest" classname="com.example.MyTest" time="0.0">
 *     <skipped/>
 *   </testcase>
 * </testsuite>
 * ```
 */
object SurefireReportParser {

    private val log = Logger.getInstance(SurefireReportParser::class.java)

    fun parse(moduleDir: File): List<TestResult> {
        val reportDirs = listOf(
            File(moduleDir, "target/surefire-reports"),
            File(moduleDir, "target/failsafe-reports")
        )
        val xmlFiles = reportDirs
            .filter { it.isDirectory }
            .flatMap { dir -> dir.listFiles { f -> f.extension == "xml" }?.toList() ?: emptyList() }

        if (xmlFiles.isEmpty()) {
            log.warn("No Surefire/Failsafe XML reports found in ${moduleDir.absolutePath}")
            return emptyList()
        }

        val results = mutableListOf<TestResult>()
        for (xmlFile in xmlFiles) {
            try {
                results.addAll(parseXmlFile(xmlFile))
            } catch (e: Exception) {
                log.warn("Failed to parse Surefire report: ${xmlFile.absolutePath}", e)
            }
        }

        log.info("Parsed ${results.size} test results from ${xmlFiles.size} Surefire XML files")
        return TimestampUtils.normalizeTimestamps(results)
    }

    private fun parseXmlFile(file: File): List<TestResult> {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.parse(file)
        val testsuites = doc.getElementsByTagName("testsuite")
        val results = mutableListOf<TestResult>()

        for (i in 0 until testsuites.length) {
            val suite = testsuites.item(i) as Element
            results.addAll(parseSuite(suite))
        }

        // If the root element is a testsuite itself (common case)
        if (testsuites.length == 0 && doc.documentElement.tagName == "testsuite") {
            results.addAll(parseSuite(doc.documentElement))
        }

        return results
    }

    private fun parseSuite(suite: Element): List<TestResult> {
        val results = mutableListOf<TestResult>()
        val testcases = suite.getElementsByTagName("testcase")
        // Use suite-level timestamp as a base if available
        val suiteTimestamp = suite.getAttribute("timestamp")
        val baseTimeMs = parseTimestamp(suiteTimestamp)

        var accumulatedTimeMs = 0L

        for (i in 0 until testcases.length) {
            val tc = testcases.item(i) as Element
            // Skip testcases nested inside inner testsuites to avoid double counting
            if (tc.parentNode != suite) continue

            val className = tc.getAttribute("classname") ?: ""
            val testName = tc.getAttribute("name") ?: "unknown"
            val timeSeconds = tc.getAttribute("time").toDoubleOrNull() ?: 0.0
            val durationMs = (timeSeconds * 1000).toLong()

            val status = when {
                tc.getElementsByTagName("failure").length > 0 -> TestStatus.FAILED
                tc.getElementsByTagName("error").length > 0 -> TestStatus.FAILED
                tc.getElementsByTagName("skipped").length > 0 -> TestStatus.SKIPPED
                else -> TestStatus.SUCCESS
            }

            // Approximate start/end times — Surefire XML doesn't have per-test timestamps
            val startTime = baseTimeMs + accumulatedTimeMs
            val endTime = startTime + durationMs
            accumulatedTimeMs += durationMs

            val packageName = ClassNameUtils.extractPackage(className)
            val simpleClassName = ClassNameUtils.extractSimpleClassName(className)

            results.add(
                TestResult(
                    className = simpleClassName,
                    testName = testName,
                    packageName = packageName,
                    durationMs = durationMs,
                    status = status,
                    startTime = startTime,
                    endTime = endTime,
                    type = TestResultType.TEST
                )
            )
        }

        // Add suite entry if there are test results
        if (results.isNotEmpty()) {
            val suiteName = suite.getAttribute("name") ?: "unknown"
            val suiteStart = results.minOf { it.startTime }
            val suiteEnd = results.maxOf { it.endTime }
            results.add(
                TestResult(
                    className = "suite",
                    testName = suiteName,
                    packageName = "",
                    durationMs = suiteEnd - suiteStart,
                    status = if (results.any { it.status == TestStatus.FAILED }) TestStatus.FAILED else TestStatus.SUCCESS,
                    startTime = suiteStart,
                    endTime = suiteEnd,
                    type = TestResultType.SUITE
                )
            )
        }

        return results
    }

    private fun parseTimestamp(timestamp: String?): Long {
        if (timestamp.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(timestamp.replace(" ", "T") + "Z").toEpochMilli()
        } catch (_: Exception) {
            try {
                java.time.LocalDateTime.parse(timestamp.replace(" ", "T"))
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                System.currentTimeMillis()
            }
        }
    }
}
