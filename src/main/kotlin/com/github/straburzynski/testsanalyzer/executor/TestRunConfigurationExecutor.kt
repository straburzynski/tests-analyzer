package com.github.straburzynski.testsanalyzer.executor

import com.intellij.execution.testframework.sm.runner.SMTRunnerEventsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
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

        val taskName = sourceRoot?.let { deriveTaskFromSourceRoot(it, moduleDir, buildSystem) } ?: "test"

        val testPattern = if (sourceRoot != null) {
            deriveTestPattern(file, sourceRoot)
        } else {
            if (!file.isDirectory) file.nameWithoutExtension else "*"
        }

        return TaskAndPattern(taskName, testPattern)
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
                    "integration-test", "integrationTest" -> "verify"
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
