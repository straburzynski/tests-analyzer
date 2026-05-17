package com.github.straburzynski.testsanalyzer.executor

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.jetbrains.idea.maven.execution.MavenRunConfiguration
import org.jetbrains.idea.maven.execution.MavenRunConfigurationType
import org.jetbrains.idea.maven.execution.MavenRunnerParameters
import org.jetbrains.idea.maven.execution.MavenRunnerSettings
import com.github.straburzynski.testsanalyzer.model.TestResult
import com.github.straburzynski.testsanalyzer.serializer.TestResultSerializer
import com.github.straburzynski.testsanalyzer.util.SurefireReportParser
import java.io.File

class MavenBuildToolExecutor : BuildToolExecutor {

    private val log = Logger.getInstance(MavenBuildToolExecutor::class.java)

    override fun executeTests(
        project: Project,
        moduleDir: File,
        testPattern: String,
        taskName: String,
        runSubProjects: Boolean,
        onFinished: (List<TestResult>) -> Unit,
    ) {
        val goal = if (taskName == "integrationTest" || taskName == "integration-test" || taskName == "verify") {
            "verify"
        } else {
            "test"
        }
        val goals = mutableListOf("clean", goal)

        val params = MavenRunnerParameters(
            true,
            moduleDir.absolutePath,
            null as String?,
            goals,
            null as Collection<String>?
        )

        if (!runSubProjects) {
            params.commandLine = "--non-recursive"
        }

        val settings = MavenRunConfigurationType.createRunnerAndConfigurationSettings(
            null,
            null,
            params,
            project
        )
        settings.isTemporary = true

        val runConfig = settings.configuration as MavenRunConfiguration
        val runnerSettings = runConfig.runnerSettings ?: MavenRunnerSettings()
        val properties = LinkedHashMap(runnerSettings.mavenProperties)

        val mavenPattern = toMavenTestPattern(testPattern)
        if (mavenPattern.isNotBlank() && mavenPattern != "*") {
            val propertyName = if (goal == "verify") "it.test" else "test"
            properties[propertyName] = mavenPattern
            properties["failIfNoTests"] = "false"
        }

        runnerSettings.mavenProperties = properties
        runConfig.runnerSettings = runnerSettings

        log.info("Launching Maven: goals=$goals, properties=$properties, moduleDir=${moduleDir.absolutePath}")

        val messageBusConnection = project.messageBus.connect()
        messageBusConnection.subscribe(ExecutionManager.EXECUTION_TOPIC, object : ExecutionListener {
            override fun processTerminated(
                executorId: String,
                env: ExecutionEnvironment,
                handler: ProcessHandler,
                exitCode: Int
            ) {
                if (env.runnerAndConfigurationSettings?.configuration === runConfig) {
                    messageBusConnection.disconnect()
                    log.info("Maven process terminated with exit code $exitCode, parsing Surefire reports")

                    ApplicationManager.getApplication().executeOnPooledThread {
                        val results = SurefireReportParser.parse(moduleDir)
                        if (results.isEmpty()) {
                            log.warn("No test results found in Surefire reports for ${moduleDir.absolutePath}")
                        }
                        TestResultSerializer.save(results, moduleDir, taskName, BuildSystem.MAVEN)
                        onFinished(results)
                    }
                }
            }
        })

        ApplicationManager.getApplication().invokeLater {
            ProgramRunnerUtil.executeConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
        }
    }

    private fun toMavenTestPattern(pattern: String): String {
        if (pattern == "*") return "*"
        return pattern
    }
}
