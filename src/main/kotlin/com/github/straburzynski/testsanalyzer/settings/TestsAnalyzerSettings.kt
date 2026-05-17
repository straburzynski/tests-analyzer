package com.github.straburzynski.testsanalyzer.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "TestsAnalyzerSettings", storages = [Storage("testsAnalyzer.xml")])
class TestsAnalyzerSettings : PersistentStateComponent<TestsAnalyzerSettings.State> {

    data class State(
        var runTestsForSubProjects: Boolean = false,
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var runTestsForSubProjects: Boolean
        get() = myState.runTestsForSubProjects
        set(value) {
            myState.runTestsForSubProjects = value
        }

    companion object {
        fun getInstance(project: Project): TestsAnalyzerSettings = project.service()
    }
}
