package com.github.straburzynski.testsanalyzer.executor

import com.intellij.openapi.project.Project
import com.github.straburzynski.testsanalyzer.model.TestResult
import java.io.File

interface BuildToolExecutor {

    fun executeTests(
        project: Project,
        moduleDir: File,
        testPattern: String,
        taskName: String,
        runSubProjects: Boolean,
        onFinished: (List<TestResult>) -> Unit,
    )
}
