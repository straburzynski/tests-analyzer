package com.github.straburzynski.testsanalyzer.serializer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import com.github.straburzynski.testsanalyzer.executor.BuildSystem
import com.github.straburzynski.testsanalyzer.executor.BuildSystem.GRADLE
import com.github.straburzynski.testsanalyzer.executor.BuildSystem.MAVEN
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType.SUITE
import com.github.straburzynski.testsanalyzer.model.TestResultType.TEST
import com.github.straburzynski.testsanalyzer.model.TestStatus.FAILED
import com.github.straburzynski.testsanalyzer.model.TestStatus.SKIPPED
import com.github.straburzynski.testsanalyzer.model.TestStatus.SUCCESS
import java.io.File

class TestResultSerializerTest {

    @TempDir
    lateinit var tempDir: File

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `save and loadLatest round-trip preserves test data`(buildSystem: BuildSystem) {
        val results = listOf(
            TestResult("MyClass", "test1", "com.example", 150, SUCCESS, startTime = 0, endTime = 150),
            TestResult("MyClass", "test2", "com.example", 200, FAILED, startTime = 150, endTime = 350),
            TestResult("MyClass", "test3", "com.example", 0, SKIPPED, startTime = 350, endTime = 350),
        )

        TestResultSerializer.save(results, tempDir, "test", buildSystem)
        val loaded = TestResultSerializer.loadLatest(tempDir, buildSystem)

        assertEquals(3, loaded.size)
        assertEquals("MyClass", loaded[0].className)
        assertEquals("test1", loaded[0].testName)
        assertEquals("com.example", loaded[0].packageName)
        assertEquals(150L, loaded[0].durationMs)
        assertEquals(SUCCESS, loaded[0].status)
        assertEquals(FAILED, loaded[1].status)
        assertEquals(SKIPPED, loaded[2].status)
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `save creates JSON file in correct report directory`(buildSystem: BuildSystem) {
        val results = listOf(
            TestResult("MyClass", "test1", "com.example", 100, SUCCESS, startTime = 0, endTime = 100),
        )

        TestResultSerializer.save(results, tempDir, "test", buildSystem)

        val expectedFile = File(tempDir, "${buildSystem.reportDir}/test.json")
        assertTrue(expectedFile.exists(), "JSON file should be created at ${expectedFile.absolutePath}")
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `loadLatest returns empty list when no reports directory`(buildSystem: BuildSystem) {
        val nonExistent = File(tempDir, "nonexistent")
        val results = TestResultSerializer.loadLatest(nonExistent, buildSystem)
        assertTrue(results.isEmpty())
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `loadLatest does not cross-read from other build system directory`(buildSystem: BuildSystem) {
        val otherSystem = if (buildSystem == GRADLE) MAVEN else GRADLE
        val results = listOf(
            TestResult("MyClass", "test1", "pkg", 100, SUCCESS, startTime = 0, endTime = 100),
        )

        TestResultSerializer.save(results, tempDir, "test", otherSystem)
        val loaded = TestResultSerializer.loadLatest(tempDir, buildSystem)

        assertTrue(
            loaded.isEmpty(),
            "Should not load results saved by ${otherSystem.name} when loading for ${buildSystem.name}",
        )
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `save preserves SUITE entries`(buildSystem: BuildSystem) {
        val results = listOf(
            TestResult("suite", "MySuite", "", 500, SUCCESS, startTime = 0, endTime = 500, type = SUITE),
            TestResult("MyClass", "test1", "pkg", 100, SUCCESS, startTime = 0, endTime = 100, type = TEST),
        )

        TestResultSerializer.save(results, tempDir, "test", buildSystem)
        val loaded = TestResultSerializer.loadLatest(tempDir, buildSystem)

        assertEquals(2, loaded.size)
        val suite = loaded.find { it.type == SUITE }
        val test = loaded.find { it.type == TEST }
        assertTrue(suite != null)
        assertTrue(test != null)
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `normalizeTimestamps shifts times correctly on load`(buildSystem: BuildSystem) {
        val results = listOf(
            TestResult("A", "test1", "pkg", 100, SUCCESS, startTime = 5000, endTime = 5100),
            TestResult("B", "test2", "pkg", 200, SUCCESS, startTime = 5200, endTime = 5400),
        )

        TestResultSerializer.save(results, tempDir, "test", buildSystem)
        val loaded = TestResultSerializer.loadLatest(tempDir, buildSystem)

        // After normalization, earliest start should be 0
        assertEquals(0L, loaded.minOf { it.startTime })
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `parseJsonFile handles legacy FAILURE status`(buildSystem: BuildSystem) {
        val reportDir = File(tempDir, buildSystem.reportDir)
        reportDir.mkdirs()
        val jsonFile = File(reportDir, "test.json")
        jsonFile.writeText(
            """
            {
              "results": [
                {
                  "className": "com.example.MyClass",
                  "testName": "failingTest",
                  "startTime": 1000,
                  "endTime": 1200,
                  "durationMs": 200,
                  "resultType": "FAILURE",
                  "type": "TEST"
                }
              ]
            }
        """.trimIndent(),
        )

        val results = TestResultSerializer.parseJsonFile(jsonFile)

        assertEquals(1, results.size)
        assertEquals(FAILED, results[0].status)
    }

    @ParameterizedTest
    @EnumSource(BuildSystem::class)
    fun `parseJsonFile returns empty list for non-existent file`(@Suppress("UNUSED_PARAMETER") buildSystem: BuildSystem) {
        val results = TestResultSerializer.parseJsonFile(File(tempDir, "missing.json"))
        assertTrue(results.isEmpty())
    }
}
