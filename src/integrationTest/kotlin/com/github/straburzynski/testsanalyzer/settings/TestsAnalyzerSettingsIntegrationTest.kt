package com.github.straburzynski.testsanalyzer.settings

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import com.github.straburzynski.testsanalyzer.BaseIntegrationTest
import javax.swing.JCheckBox

class TestsAnalyzerSettingsIntegrationTest : BaseIntegrationTest() {

    @Test
    fun `default runTestsForSubProjects is false`() {
        val settings = TestsAnalyzerSettings.getInstance(project)
        assertFalse(settings.runTestsForSubProjects, "Default value should be false")
    }

    @Test
    fun `setting persists after change`() {
        val settings = TestsAnalyzerSettings.getInstance(project)
        settings.runTestsForSubProjects = true
        assertTrue(settings.runTestsForSubProjects, "Value should be true after setting")

        val state = settings.state
        assertTrue(state.runTestsForSubProjects, "State object should reflect the change")
    }

    @Test
    fun `loadState restores persisted value`() {
        val settings = TestsAnalyzerSettings.getInstance(project)
        val newState = TestsAnalyzerSettings.State(runTestsForSubProjects = true)
        settings.loadState(newState)
        assertTrue(settings.runTestsForSubProjects, "Value should be restored from loaded state")
    }

    @Test
    fun `configurable creates panel with checkbox`() {
        val configurable = TestsAnalyzerConfigurable(project)
        val panel = configurable.createPanel()

        assertNotNull(panel, "Panel should not be null")

        val checkbox = findCheckbox(panel)
        assertNotNull(checkbox, "Panel should contain a checkbox")
        org.junit.jupiter.api.Assertions.assertEquals(
            "Run tests for sub-projects", checkbox!!.text, "Checkbox should have correct label",
        )
    }

    @Test
    fun `configurable checkbox reflects current setting value`() {
        val settings = TestsAnalyzerSettings.getInstance(project)
        settings.runTestsForSubProjects = false

        val configurable = TestsAnalyzerConfigurable(project)
        val panel = configurable.createPanel()
        configurable.reset()

        val checkbox = findCheckbox(panel)
        assertNotNull(checkbox)
        assertFalse(checkbox!!.isSelected, "Checkbox should be unchecked when setting is false")
    }

    @Test
    fun `configurable displayName is Tests Analyzer`() {
        val configurable = TestsAnalyzerConfigurable(project)
        org.junit.jupiter.api.Assertions.assertEquals("Tests Analyzer", configurable.displayName)
    }

    private fun findCheckbox(component: java.awt.Component): JCheckBox? {
        if (component is JCheckBox) return component
        if (component is java.awt.Container) {
            for (child in component.components) {
                val found = findCheckbox(child)
                if (found != null) return found
            }
        }
        return null
    }
}
