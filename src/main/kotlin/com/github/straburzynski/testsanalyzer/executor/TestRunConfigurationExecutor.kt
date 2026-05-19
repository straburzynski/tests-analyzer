package com.github.straburzynski.testsanalyzer.executor

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.github.straburzynski.testsanalyzer.action.KNOWN_TEST_SOURCE_SETS
import com.github.straburzynski.testsanalyzer.collector.SmTestRunnerCollector
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.serializer.TestResultSerializer
import com.github.straburzynski.testsanalyzer.settings.TestsAnalyzerSettings
import java.io.File

class TestRunConfigurationExecutor(
    private val project: Project,
    private val targetFile: VirtualFile,
) {

    fun executeAndCollect(onFinished: (List<TestResult>) -> Unit) {
        val basePath = project.basePath ?: run {
            onFinished(emptyList())
            return
        }
        val baseDir = File(basePath)
        val moduleDir = findModuleDir(targetFile, baseDir)
        val buildSystem = BuildSystem.detect(moduleDir)
        val (taskName, testPattern) = deriveTaskAndPattern(targetFile, moduleDir, buildSystem)
        val pluginSettings = TestsAnalyzerSettings.getInstance(project)

        val executor: BuildToolExecutor = when (buildSystem) {
            BuildSystem.GRADLE -> GradleBuildToolExecutor()
            BuildSystem.MAVEN -> MavenBuildToolExecutor()
        }

        // For Gradle, subscribe to SMTestRunner events on the project message bus.
        // For Maven, the executor handles result collection directly via Surefire XML reports
        // because Maven run configurations don't publish SMTestRunner events on the message bus.
        if (buildSystem == BuildSystem.GRADLE) {
            val connectionDisposable = Disposer.newDisposable("TestsAnalyzer-$taskName")
            Disposer.register(project as Disposable, connectionDisposable)

            val connection = project.messageBus.connect(connectionDisposable)
            val collector = SmTestRunnerCollector(
                onAllTestsFinished = { results ->
                    Disposer.dispose(connectionDisposable)
                    TestResultSerializer.save(results, moduleDir, taskName, buildSystem)
                    onFinished(results)
                }
            )
            connection.subscribe(SMTRunnerEventsListener.TEST_STATUS, collector)
        }

        executor.executeTests(
            project = project,
            moduleDir = moduleDir,
            testPattern = testPattern,
            taskName = taskName,
            runSubProjects = pluginSettings.runTestsForSubProjects,
            onFinished = onFinished,
        )
    }

    fun loadExistingResults(): List<TestResult> {
        val basePath = project.basePath ?: return emptyList()
        val baseDir = File(basePath)
        val moduleDir = findModuleDir(targetFile, baseDir)
        val buildSystem = BuildSystem.detect(moduleDir)
        return TestResultSerializer.loadLatest(moduleDir, buildSystem)
    }


    private fun findModuleDir(file: VirtualFile, baseDir: File): File {
        var current = File(file.path)
        if (!current.isDirectory) current = current.parentFile
        val basePath = baseDir.toPath()
        while (current.toPath().startsWith(basePath)) {
            if (BuildSystem.isModuleRoot(current)) {
                return current
            }
            current = current.parentFile
        }
        return baseDir
    }

    private data class TaskAndPattern(val taskName: String, val testPattern: String)

    private fun deriveTaskAndPattern(
        file: VirtualFile,
        moduleDir: File,
        buildSystem: BuildSystem,
    ): TaskAndPattern {
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        val sourceRoot = fileIndex.getSourceRootForFile(file)
            ?: findSourceRootForParent(file)

        if (sourceRoot != null) {
            val taskName = deriveTaskFromSourceRoot(sourceRoot, moduleDir, buildSystem)
            val testPattern = deriveTestPattern(file, sourceRoot)
            return TaskAndPattern(taskName, testPattern)
        }

        // Fallback: infer task name and source root from path convention (src/<sourceSet>/<language>/...)
        val inferred = inferFromPath(file, moduleDir, buildSystem)
        if (inferred != null) return inferred

        return TaskAndPattern("test", if (!file.isDirectory) file.nameWithoutExtension else "*")
    }

    /**
     * When IntelliJ's file index doesn't recognize the source root, infer the task name and test pattern from the
     * directory structure convention: src/<sourceSet>/<language>/...
     */
    private fun inferFromPath(file: VirtualFile, moduleDir: File, buildSystem: BuildSystem): TaskAndPattern? {
        val modulePath = moduleDir.absolutePath
        if (!file.path.startsWith(modulePath)) return null

        val relative = file.path.removePrefix(modulePath).removePrefix("/")
        val parts = relative.split("/")

        val sourceSetName = findSourceSetName(parts) ?: return null
        val taskName = resolveTaskName(sourceSetName, buildSystem)
        val testPattern = buildTestPattern(file, parts, parts.indexOf("src") + 2)

        return TaskAndPattern(taskName, testPattern)
    }

    /**
     * Finds the source set name from path parts by locating the "src" segment.
     * Returns null if no valid test source set is found.
     */
    private fun findSourceSetName(parts: List<String>): String? {
        val srcIndex = parts.indexOf("src")
        if (srcIndex == -1 || srcIndex + 1 >= parts.size) return null
        val sourceSetName = parts[srcIndex + 1]
        return if (sourceSetName in KNOWN_TEST_SOURCE_SETS) sourceSetName else null
    }

    /**
     * Maps a source set name to the appropriate build task name.
     */
    private fun resolveTaskName(sourceSetName: String, buildSystem: BuildSystem): String {
        return when (buildSystem) {
            BuildSystem.GRADLE -> sourceSetName
            BuildSystem.MAVEN -> when (sourceSetName) {
                "test" -> "test"
                "it", "integration-test", "integrationTest", "integration" -> "verify"
                else -> "test"
            }
        }
    }

    /**
     * Builds the test filter pattern from path parts after the language directory.
     * Convention: src/<sourceSet>/<language>/package/parts/TestClass.kt
     */
    private fun buildTestPattern(file: VirtualFile, parts: List<String>, languageDirIndex: Int): String {
        val packageParts = if (languageDirIndex < parts.size - 1) {
            parts.subList(languageDirIndex + 1, parts.size)
        } else {
            emptyList()
        }

        return when {
            packageParts.isEmpty() -> "*"
            file.isDirectory -> "${packageParts.joinToString(".")}.*"
            else -> {
                val dir = packageParts.dropLast(1).joinToString(".")
                val name = file.nameWithoutExtension
                if (dir.isNotEmpty()) "$dir.$name" else name
            }
        }
    }

    private fun findSourceRootForParent(dir: VirtualFile): VirtualFile? {
        if (!dir.isDirectory) return null
        val fileIndex = ProjectRootManager.getInstance(project).fileIndex
        for (root in ProjectRootManager.getInstance(project).contentSourceRoots) {
            if (fileIndex.isInTestSourceContent(root) && root.path.startsWith(dir.path + "/")) {
                return root
            }
        }
        return null
    }

    /**
     * Extracts the task/goal name from a test source root path.
     *
     * Gradle convention: src/<sourceSetName>/<language> -> task name is <sourceSetName>.
     * Maven convention: src/test/<language> -> "test"; src/integration-test -> "verify".
     */
    private fun deriveTaskFromSourceRoot(
        sourceRoot: VirtualFile,
        moduleDir: File,
        buildSystem: BuildSystem,
    ): String {
        val rootPath = sourceRoot.path
        val modulePath = moduleDir.absolutePath
        val relative = rootPath.removePrefix(modulePath).removePrefix("/")
        val parts = relative.split("/")
        if (parts.size >= 2 && parts[0] == "src") {
            val sourceSetName = parts[1]
            return when (buildSystem) {
                BuildSystem.GRADLE -> sourceSetName
                BuildSystem.MAVEN -> when (sourceSetName) {
                    "test" -> "test"
                    "it", "integration-test", "integrationTest" -> "verify"
                    else -> "test"
                }
            }
        }
        return "test"
    }

    /**
     * Derives the test filter pattern from the file relative to its source root.
     * The pattern format is build-system-agnostic (package.ClassName or package.*).
     */
    private fun deriveTestPattern(file: VirtualFile, sourceRoot: VirtualFile): String {
        // If file is a parent/ancestor of the source root, run all tests
        if (sourceRoot.path.startsWith(file.path + "/")) return "*"

        val relative = file.path.removePrefix(sourceRoot.path).removePrefix("/")
        if (relative.isEmpty()) return "*" // source root itself — run all tests

        return if (file.isDirectory) {
            val packagePattern = relative.replace("/", ".")
            "$packagePattern.*"
        } else {
            // Use nameWithoutExtension to handle any JVM language file extension
            val dir = relative.substringBeforeLast("/", "")
            val name = file.nameWithoutExtension
            if (dir.isNotEmpty()) {
                "${dir.replace("/", ".")}.$name"
            } else {
                name
            }
        }
    }
}
