package com.github.straburzynski.testsanalyzer

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

abstract class BaseIntegrationTest : BasePlatformTestCase() {

    @BeforeEach
    fun before() {
        setUp()
    }

    @AfterEach
    fun after() {
        tearDown()
    }
}
