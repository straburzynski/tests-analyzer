package com.github.straburzynski.testsanalyzer.executor

import com.intellij.execution.ExecutionManager
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType
import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration
import org.jetbrains.plugins.gradle.util.GradleConstants
import com.github.straburzynski.testsanalyzer.model.TestResult
import java.io.File

class GradleBuildToolExecutor : BuildToolExecutor {

    private val log = Logger.getInstance(GradleBuildToolExecutor::class.java)

    override fun executeTests(
        project: Project,
        moduleDir: File,
        testPattern: String,
        taskName: String,
        runSubProjects: Boolean,
        onFinished: (List<TestResult>) -> Unit,
    ) {
        // When externalProjectPath points to the module, task names are module-relative.
        // Use bare task name for current module; for subProjects add unqualified name which
        // Gradle resolves to all subprojects from this module's perspective.
        val taskList = if (runSubProjects) {
            listOf(taskName, ":$taskName")
        } else {
            listOf(taskName)
        }

        val settings = ExternalSystemTaskExecutionSettings().apply {
            externalSystemIdString = GradleConstants.SYSTEM_ID.id
            externalProjectPath = moduleDir.absolutePath
            taskNames = taskList
            scriptParameters = buildString {
                append("--tests \"$testPattern\"")
                if (runSubProjects) {
                    append(" --continue")
                }
            }
        }

        log.info("Launching Gradle tasks ${settings.taskNames} with params '${settings.scriptParameters}'")

        val runManager = RunManager.getInstance(project)
        val configType = GradleExternalTaskConfigurationType.getInstance()
        val runnerAndConfigSettings = runManager.createConfiguration(
            "Tests Analyzer: $taskName",
            configType.factory
        )
        val runConfiguration = runnerAndConfigSettings.configuration as GradleRunConfiguration
        runConfiguration.settings.apply {
            externalSystemIdString = settings.externalSystemIdString
            externalProjectPath = settings.externalProjectPath
            taskNames = settings.taskNames
            scriptParameters = settings.scriptParameters
        }
        runConfiguration.isRunAsTest = true

        val executor = DefaultRunExecutor.getRunExecutorInstance()
        val environment = ExecutionEnvironmentBuilder.create(executor, runnerAndConfigSettings)
            .build()

        ExecutionManager.getInstance(project).restartRunProfile(environment)
    }
}
