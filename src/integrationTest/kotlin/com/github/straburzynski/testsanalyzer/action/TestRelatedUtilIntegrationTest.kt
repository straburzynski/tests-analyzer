package com.github.straburzynski.testsanalyzer.action

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.roots.ProjectRootManager
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import com.github.straburzynski.testsanalyzer.BaseIntegrationTest

class TestRelatedUtilIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `isTestRelated returns true for file in test source root`() {
        val testFile = myFixture.addFileToProject("src/test/kotlin/SampleTest.kt", "class SampleTest")
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(testFile.virtualFile, rootManager)
        }
        assertTrue(result, "File in test source root should be test-related")
    }

    @Test
    fun `isTestRelated returns false for file in main source root`() {
        val mainFile = myFixture.addFileToProject("src/Main.kt", "fun main() {}")
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(mainFile.virtualFile, rootManager)
        }
        assertFalse(result, "Main source file should not be test-related")
    }

    @Test
    fun `isTestRelated returns true for directory that is parent of test source root`() {
        val testFile = myFixture.addFileToProject("src/test/kotlin/SampleTest.kt", "class SampleTest")
        val testDir = testFile.virtualFile.parent.parent // src/test
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(testDir, rootManager)
        }
        assertTrue(result, "Parent of test source root should be test-related")
    }

    @Test
    fun `isTestRelated returns false for null-like non-project file`() {
        val rootManager = ProjectRootManager.getInstance(project)
        val outsideFile = myFixture.addFileToProject("random/NotInProject.txt", "data")
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(outsideFile.virtualFile, rootManager)
        }
        assertFalse(result, "File outside source roots should not be test-related (unless module root)")
    }

    @Test
    fun `isTestRelated returns true for module root with build gradle kts`() {
        val buildFile = myFixture.addFileToProject("mymodule/build.gradle.kts", "plugins { id(\"java\") }")
        val moduleDir = buildFile.virtualFile.parent
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(moduleDir, rootManager)
        }
        assertTrue(result, "Directory with build.gradle.kts should be recognized as module root")
    }

    @Test
    fun `isTestRelated returns true for module root with pom xml`() {
        val pomFile = myFixture.addFileToProject("project/pom.xml", "<project/>")
        val moduleDir = pomFile.virtualFile.parent
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(moduleDir, rootManager)
        }
        assertTrue(result, "Directory with pom.xml should be recognized as module root")
    }

    @Test
    fun `isTestRelated returns false for regular file not directory`() {
        val regularFile = myFixture.addFileToProject("file.txt", "content")
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(regularFile.virtualFile, rootManager)
        }
        assertFalse(result, "Regular file without build marker should not be test-related")
    }

    @ParameterizedTest(name = "trusted folder ''{0}'' -> isTestRelated={1}")
    @CsvSource(
        "src/test,                  true",
        "src/integrationTest,       true",
        "src/integration,           true",
        "src/test,                  true",
        "src/test/kotlin,           true",
        "src/integrationTest/java,  true",
        "src/main,                  false",
        "src/main/kotlin,           false",
        "src/other-dir,             false",
    )
    fun `isTestRelated recognizes trusted test directories`(dirPath: String, expected: Boolean) {
        val file = myFixture.addFileToProject("${dirPath.trim()}/.placeholder", "")
        val dir = file.virtualFile.parent
        val rootManager = ProjectRootManager.getInstance(project)
        val result = ReadAction.compute<Boolean, RuntimeException> {
            isTestRelated(dir, rootManager)
        }
        if (expected) {
            assertTrue(result, "'$dirPath' should be test-related (trusted folder)")
        } else {
            assertFalse(result, "'$dirPath' should NOT be test-related")
        }
    }
}
