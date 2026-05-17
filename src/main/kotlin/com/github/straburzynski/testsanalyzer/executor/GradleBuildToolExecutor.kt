package com.github.straburzynski.testsanalyzer.executor

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
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
        val cleanTask = if (runSubProjects) "clean" else ":clean"
        val taskList = if (runSubProjects) {
            listOf(cleanTask, ":$taskName", taskName)
        } else {
            listOf(cleanTask, ":$taskName")
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

        ExternalSystemUtil.runTask(
            settings,
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID
        )
    }
}
