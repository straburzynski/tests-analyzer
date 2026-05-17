package com.github.straburzynski.example.junit

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class AnotherJunitTests {

    @Test
    fun `should send welcome email to new user`() {
        Thread.sleep(200)
        assertTrue(true)
    }

    @Test
    fun `should calculate shipping cost`() {
        Thread.sleep(600)
        assertTrue(true)
    }

    @Test
    fun `should render dashboard widgets`() {
        Thread.sleep(1200)
        assertTrue(true)
    }

    @Test
    fun `should enforce rate limiting`() {
        Thread.sleep(400)
        assertTrue(false, "Rate limiter allowed too many requests")
    }

    @Test
    @Disabled("Notification service not configured in test environment")
    fun `should deliver push notification`() {
        Thread.sleep(500)
        assertTrue(true)
    }
}
