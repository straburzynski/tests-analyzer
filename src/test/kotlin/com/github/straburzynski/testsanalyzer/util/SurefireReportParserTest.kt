package com.github.straburzynski.testsanalyzer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestStatus
import java.io.File

class SurefireReportParserTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `parse returns empty list when no report directories exist`() {
        val results = SurefireReportParser.parse(tempDir)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `parse returns empty list when report directory is empty`() {
        File(tempDir, "target/surefire-reports").mkdirs()
        val results = SurefireReportParser.parse(tempDir)
        assertTrue(results.isEmpty())
    }

    @Test
    fun `parse handles standard surefire XML report`() {
        val reportDir = File(tempDir, "target/surefire-reports").apply { mkdirs() }
        File(reportDir, "TEST-com.example.MyTest.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="com.example.MyTest" tests="3" failures="1" errors="0" skipped="1" time="1.5">
              <testcase name="shouldPass" classname="com.example.MyTest" time="0.5"/>
              <testcase name="shouldFail" classname="com.example.MyTest" time="0.8">
                <failure message="expected true">java.lang.AssertionError</failure>
              </testcase>
              <testcase name="shouldSkip" classname="com.example.MyTest" time="0.0">
                <skipped/>
              </testcase>
            </testsuite>
            """.trimIndent()
        )

        val results = SurefireReportParser.parse(tempDir)

        val tests = results.filter { it.type == TestResultType.TEST }
        val suites = results.filter { it.type == TestResultType.SUITE }

        assertEquals(3, tests.size)
        assertEquals(1, suites.size)

        val passed = tests.first { it.testName == "shouldPass" }
        assertEquals(TestStatus.SUCCESS, passed.status)
        assertEquals("MyTest", passed.className)
        assertEquals("com.example", passed.packageName)
        assertEquals(500L, passed.durationMs)

        val failed = tests.first { it.testName == "shouldFail" }
        assertEquals(TestStatus.FAILED, failed.status)
        assertEquals(800L, failed.durationMs)

        val skipped = tests.first { it.testName == "shouldSkip" }
        assertEquals(TestStatus.SKIPPED, skipped.status)
    }

    @Test
    fun `parse handles error testcases as FAILED`() {
        val reportDir = File(tempDir, "target/surefire-reports").apply { mkdirs() }
        File(reportDir, "TEST-com.example.ErrorTest.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="com.example.ErrorTest" tests="1" failures="0" errors="1" time="0.1">
              <testcase name="shouldError" classname="com.example.ErrorTest" time="0.1">
                <error message="NPE">java.lang.NullPointerException</error>
              </testcase>
            </testsuite>
            """.trimIndent()
        )

        val results = SurefireReportParser.parse(tempDir)
        val tests = results.filter { it.type == TestResultType.TEST }
        assertEquals(1, tests.size)
        assertEquals(TestStatus.FAILED, tests[0].status)
    }

    @Test
    fun `parse reads from both surefire and failsafe directories`() {
        val surefireDir = File(tempDir, "target/surefire-reports").apply { mkdirs() }
        val failsafeDir = File(tempDir, "target/failsafe-reports").apply { mkdirs() }

        surefireDir.resolve("TEST-UnitTest.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="UnitTest" tests="1" time="0.1">
              <testcase name="unitTest" classname="com.example.UnitTest" time="0.1"/>
            </testsuite>
            """.trimIndent()
        )
        failsafeDir.resolve("TEST-IntegrationTest.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="IntegrationTest" tests="1" time="0.2">
              <testcase name="integrationTest" classname="com.example.IntegrationTest" time="0.2"/>
            </testsuite>
            """.trimIndent()
        )

        val results = SurefireReportParser.parse(tempDir)
        val tests = results.filter { it.type == TestResultType.TEST }
        assertEquals(2, tests.size)
        assertTrue(tests.any { it.testName == "unitTest" })
        assertTrue(tests.any { it.testName == "integrationTest" })
    }

    @Test
    fun `parse normalizes timestamps so earliest starts at zero`() {
        val reportDir = File(tempDir, "target/surefire-reports").apply { mkdirs() }
        File(reportDir, "TEST-com.example.TimingTest.xml").writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <testsuite name="com.example.TimingTest" tests="2" time="0.3">
              <testcase name="first" classname="com.example.TimingTest" time="0.1"/>
              <testcase name="second" classname="com.example.TimingTest" time="0.2"/>
            </testsuite>
            """.trimIndent()
        )

        val results = SurefireReportParser.parse(tempDir)
        val tests = results.filter { it.type == TestResultType.TEST }
        // After normalization, the earliest startTime should be 0
        assertEquals(0L, tests.minOf { it.startTime })
    }
}
