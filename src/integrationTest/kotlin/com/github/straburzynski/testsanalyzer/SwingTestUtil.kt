package com.github.straburzynski.testsanalyzer

import javax.swing.JButton

object SwingTestUtil {

    @Suppress("UNCHECKED_CAST")
    fun <T> findComponent(root: java.awt.Component?, type: Class<T>): T? {
        if (root == null) return null
        if (type.isInstance(root)) return root as T
        if (root is java.awt.Container) {
            for (child in root.components) {
                val found = findComponent(child, type)
                if (found != null) return found
            }
        }
        return null
    }

    fun findAllButtons(root: java.awt.Component): List<JButton> {
        val buttons = mutableListOf<JButton>()
        collectButtons(root, buttons)
        return buttons
    }

    private fun collectButtons(component: java.awt.Component, buttons: MutableList<JButton>) {
        if (component is JButton) buttons.add(component)
        if (component is java.awt.Container) {
            for (child in component.components) {
                collectButtons(child, buttons)
            }
        }
    }

    fun findButton(root: java.awt.Component?, text: String): JButton? {
        if (root == null) return null
        if (root is JButton && root.text == text) return root
        if (root is java.awt.Container) {
            for (child in root.components) {
                val found = findButton(child, text)
                if (found != null) return found
            }
        }
        return null
    }
}
