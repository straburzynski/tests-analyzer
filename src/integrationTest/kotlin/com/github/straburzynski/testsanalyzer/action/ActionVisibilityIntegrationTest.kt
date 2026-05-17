package com.github.straburzynski.testsanalyzer.action

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys.PROJECT
import com.intellij.openapi.actionSystem.CommonDataKeys.VIRTUAL_FILE
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.ReadAction
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.BaseIntegrationTest

class ActionVisibilityIntegrationTest : BaseIntegrationTest() {

    private fun createEvent(dataContext: DataContext): AnActionEvent {
        return AnActionEvent.createEvent(dataContext, Presentation(), "TestPlace", ActionUiKind.NONE, null)
    }

    @Test
    fun `both actions are registered in ActionManager`() {
        val actionManager = ActionManager.getInstance()
        assertNotNull(
            actionManager.getAction("TestsAnalyzer.RunTestsAndAnalyze"),
            "RunTestsAndAnalyze action should be registered",
        )
        assertNotNull(
            actionManager.getAction("TestsAnalyzer.AnalyzeExisting"),
            "AnalyzeExisting action should be registered",
        )
    }

    @Test
    fun `action group is registered in ActionManager`() {
        val actionManager = ActionManager.getInstance()
        val group = actionManager.getAction("TestsAnalyzer.Group")
        assertNotNull(group, "TestsAnalyzer.Group should be registered")
    }

    @Test
    fun `RunTestsAndAnalyzeAction is visible for module root with build gradle kts`() {
        val buildFile = myFixture.addFileToProject("mymodule/build.gradle.kts", "plugins { id(\"java\") }")
        val moduleDir = buildFile.virtualFile.parent

        val action = RunTestsAndAnalyzeAction()
        val dataContext = SimpleDataContext.builder()
            .add(PROJECT, project)
            .add(VIRTUAL_FILE, moduleDir)
            .build()

        val event = createEvent(dataContext)
        ReadAction.run<RuntimeException> { action.update(event) }

        assertTrue(
            event.presentation.isEnabledAndVisible,
            "Action should be visible for directory with build.gradle.kts",
        )
    }

    @Test
    fun `AnalyzeExistingAction is visible for module root with pom xml`() {
        val pomFile = myFixture.addFileToProject("project/pom.xml", "<project/>")
        val moduleDir = pomFile.virtualFile.parent

        val action = AnalyzeExistingAction()
        val dataContext = SimpleDataContext.builder()
            .add(PROJECT, project)
            .add(VIRTUAL_FILE, moduleDir)
            .build()

        val event = createEvent(dataContext)
        ReadAction.run<RuntimeException> { action.update(event) }

        assertTrue(
            event.presentation.isEnabledAndVisible,
            "AnalyzeExisting should be visible for directory with pom.xml",
        )
    }

    @Test
    fun `actions are invisible when no project is set`() {
        val file = myFixture.addFileToProject("build.gradle.kts", "plugins { }")

        val action = RunTestsAndAnalyzeAction()
        val dataContext = SimpleDataContext.builder()
            .add(VIRTUAL_FILE, file.virtualFile)
            .build()

        val event = createEvent(dataContext)
        ReadAction.run<RuntimeException> { action.update(event) }

        assertFalse(
            event.presentation.isEnabledAndVisible,
            "Action should be invisible when project is null",
        )
    }

    @Test
    fun `actions are invisible when no file is selected`() {
        val action = RunTestsAndAnalyzeAction()
        val dataContext = SimpleDataContext.builder()
            .add(PROJECT, project)
            .build()

        val event = createEvent(dataContext)
        ReadAction.run<RuntimeException> { action.update(event) }

        assertFalse(
            event.presentation.isEnabledAndVisible,
            "Action should be invisible when no file is selected",
        )
    }
}
