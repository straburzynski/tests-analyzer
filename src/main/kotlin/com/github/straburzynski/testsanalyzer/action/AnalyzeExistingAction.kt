package com.github.straburzynski.testsanalyzer.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.github.straburzynski.testsanalyzer.executor.TestRunConfigurationExecutor
import com.github.straburzynski.testsanalyzer.ui.TestsAnalyzerDialog

class AnalyzeExistingAction : AbstractTestsAnalyzerAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val executor = TestRunConfigurationExecutor(project, file)
        val results = executor.loadExistingResults()

        if (results.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Tests Analyzer")
                .createNotification(
                    "No test results found",
                    "Run tests first using 'Run and Analyze' to generate results.",
                    NotificationType.WARNING
                )
                .notify(project)
            return
        }

        TestsAnalyzerDialog(project, results).show()
    }
}
