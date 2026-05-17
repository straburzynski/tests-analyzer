package com.github.straburzynski.testsanalyzer.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.github.straburzynski.testsanalyzer.executor.TestRunConfigurationExecutor
import com.github.straburzynski.testsanalyzer.ui.TestsAnalyzerDialog

class RunTestsAndAnalyzeAction : AbstractTestsAnalyzerAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val executor = TestRunConfigurationExecutor(project, file)
        executor.executeAndCollect { results ->
            ApplicationManager.getApplication().invokeLater {
                TestsAnalyzerDialog(project, results).show()
            }
        }
    }
}
