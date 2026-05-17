package com.github.straburzynski.testsanalyzer.serializer

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.intellij.openapi.diagnostic.Logger
import com.github.straburzynski.testsanalyzer.executor.BuildSystem
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.model.TestResultType
import com.github.straburzynski.testsanalyzer.model.TestStatus
import com.github.straburzynski.testsanalyzer.util.ClassNameUtils
import com.github.straburzynski.testsanalyzer.util.TimestampUtils
import java.io.File

object TestResultSerializer {

    private val log = Logger.getInstance(TestResultSerializer::class.java)
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun save(results: List<TestResult>, moduleDir: File, taskName: String, buildSystem: BuildSystem) {
        try {
            val outDir = File(moduleDir, buildSystem.reportDir)
            outDir.mkdirs()

            val jsonData = mapOf(
                "results" to results.map { r ->
                    mapOf(
                        "className" to "${r.packageName}.${r.className}".trimStart('.'),
                        "testName" to r.testName,
                        "startTime" to r.startTime,
                        "endTime" to r.endTime,
                        "durationMs" to r.durationMs,
                        "resultType" to r.status.name,
                        "type" to r.type.name
                    )
                }
            )

            val jsonFile = File(outDir, "$taskName.json")
            jsonFile.writeText(gson.toJson(jsonData))
            log.info("Saved ${results.size} test results to ${jsonFile.absolutePath}")
        } catch (e: Exception) {
            log.error("Failed to save test results", e)
        }
    }

    fun loadLatest(moduleDir: File, buildSystem: BuildSystem): List<TestResult> {
        val reportDir = File(moduleDir, buildSystem.reportDir)
        if (!reportDir.exists()) return emptyList()

        val latestFile = reportDir.listFiles { f -> f.extension == "json" }
            ?.maxByOrNull { it.lastModified() }

        if (latestFile == null) {
            log.warn("No JSON files found in ${reportDir.absolutePath}")
            return emptyList()
        }

        return parseJsonFile(latestFile)
    }

    fun parseJsonFile(file: File): List<TestResult> {
        if (!file.exists()) return emptyList()
        val json = file.readText()
        val root = gson.fromJson(json, JsonObject::class.java) ?: return emptyList()
        val resultsArray = root.getAsJsonArray("results") ?: return emptyList()

        val results = resultsArray.map { element ->
            val obj = element.asJsonObject
            val className = obj.get("className")?.asString ?: ""
            val testName = obj.get("testName")?.asString ?: ""
            val startTime = obj.get("startTime")?.asLong ?: 0L
            val endTime = obj.get("endTime")?.asLong ?: 0L
            val durationMs = obj.get("durationMs")?.asLong ?: (endTime - startTime)
            val resultType = obj.get("resultType")?.asString ?: "SUCCESS"
            val typeStr = obj.get("type")?.asString ?: "TEST"

            val status = when (resultType.uppercase()) {
                "SUCCESS" -> TestStatus.SUCCESS
                "FAILED", "FAILURE" -> TestStatus.FAILED
                else -> TestStatus.SKIPPED
            }
            val type = when (typeStr.uppercase()) {
                "SUITE" -> TestResultType.SUITE
                else -> TestResultType.TEST
            }
            val packageName = if (type == TestResultType.SUITE) "" else ClassNameUtils.extractPackage(className)
            val simpleClassName = if (type == TestResultType.SUITE) className else ClassNameUtils.extractSimpleClassName(className)
            TestResult(
                className = simpleClassName,
                testName = testName,
                packageName = packageName,
                durationMs = durationMs,
                status = status,
                startTime = startTime,
                endTime = endTime,
                type = type
            )
        }

        return TimestampUtils.normalizeTimestamps(results)
    }
}
