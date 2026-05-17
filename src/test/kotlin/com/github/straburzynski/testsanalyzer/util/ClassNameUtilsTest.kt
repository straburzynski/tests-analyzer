package com.github.straburzynski.testsanalyzer.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ClassNameUtilsTest {

    @Test
    fun `extractPackage returns package from fully qualified name`() {
        assertEquals("com.example", ClassNameUtils.extractPackage("com.example.MyClass"))
    }

    @Test
    fun `extractPackage returns empty string for simple class name`() {
        assertEquals("", ClassNameUtils.extractPackage("MyClass"))
    }

    @Test
    fun `extractPackage returns empty string for empty input`() {
        assertEquals("", ClassNameUtils.extractPackage(""))
    }

    @Test
    fun `extractPackage handles deeply nested packages`() {
        assertEquals("com.example.deep.nested", ClassNameUtils.extractPackage("com.example.deep.nested.MyClass"))
    }

    @Test
    fun `extractSimpleClassName returns simple name from fully qualified`() {
        assertEquals("MyClass", ClassNameUtils.extractSimpleClassName("com.example.MyClass"))
    }

    @Test
    fun `extractSimpleClassName returns input when no package`() {
        assertEquals("MyClass", ClassNameUtils.extractSimpleClassName("MyClass"))
    }

    @Test
    fun `extractSimpleClassName returns empty for empty input`() {
        assertEquals("", ClassNameUtils.extractSimpleClassName(""))
    }
}
