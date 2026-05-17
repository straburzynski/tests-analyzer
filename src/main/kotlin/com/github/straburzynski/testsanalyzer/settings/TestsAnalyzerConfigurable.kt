package com.github.straburzynski.testsanalyzer.settings

import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.panel

class TestsAnalyzerConfigurable(private val project: Project) : BoundConfigurable("Tests Analyzer") {

    private val settings get() = TestsAnalyzerSettings.getInstance(project)

    override fun createPanel() = panel {
        group("Tests Execution") {
            row {
                checkBox("Run tests for sub-projects")
                    .comment("Running tests on root test source will also execute tests in sub-projects")
                    .bindSelected(settings::runTestsForSubProjects)
            }
        }
    }
}
