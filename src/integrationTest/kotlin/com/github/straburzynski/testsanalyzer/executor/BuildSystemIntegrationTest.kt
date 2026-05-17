package com.github.straburzynski.testsanalyzer.executor

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BuildSystemIntegrationTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `detect returns GRADLE for directory with build gradle kts`() {
        File(tempDir, "build.gradle.kts").writeText("plugins { }")
        assertEquals(BuildSystem.GRADLE, BuildSystem.detect(tempDir))
    }

    @Test
    fun `detect returns GRADLE for directory with build gradle`() {
        File(tempDir, "build.gradle").writeText("apply plugin: 'java'")
        assertEquals(BuildSystem.GRADLE, BuildSystem.detect(tempDir))
    }

    @Test
    fun `detect returns MAVEN for directory with pom xml`() {
        File(tempDir, "pom.xml").writeText("<project/>")
        assertEquals(BuildSystem.MAVEN, BuildSystem.detect(tempDir))
    }

    @Test
    fun `detect prefers GRADLE when both build gradle kts and pom xml exist`() {
        File(tempDir, "build.gradle.kts").writeText("plugins { }")
        File(tempDir, "pom.xml").writeText("<project/>")
        assertEquals(BuildSystem.GRADLE, BuildSystem.detect(tempDir))
    }

    @Test
    fun `detect returns GRADLE as fallback for empty directory`() {
        assertEquals(BuildSystem.GRADLE, BuildSystem.detect(tempDir))
    }

    @Test
    fun `isModuleRoot returns true for directory with build gradle kts`() {
        File(tempDir, "build.gradle.kts").writeText("plugins { }")
        assertTrue(BuildSystem.isModuleRoot(tempDir))
    }

    @Test
    fun `isModuleRoot returns true for directory with pom xml`() {
        File(tempDir, "pom.xml").writeText("<project/>")
        assertTrue(BuildSystem.isModuleRoot(tempDir))
    }

    @Test
    fun `isModuleRoot returns false for empty directory`() {
        assertFalse(BuildSystem.isModuleRoot(tempDir))
    }

    @Test
    fun `GRADLE reportDir is build-based`() {
        assertTrue(BuildSystem.GRADLE.reportDir.startsWith("build/"))
    }

    @Test
    fun `MAVEN reportDir is target-based`() {
        assertTrue(BuildSystem.MAVEN.reportDir.startsWith("target/"))
    }
}
