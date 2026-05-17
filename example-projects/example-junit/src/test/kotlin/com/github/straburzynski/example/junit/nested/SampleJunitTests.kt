package com.github.straburzynski.example.junit.nested

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@Execution(ExecutionMode.CONCURRENT)
class SampleJunitTests {

    @Test
    fun `should process payment in under 1s`() {
        Thread.sleep(100)
        assertTrue(true)
    }

    @Test
    fun `should validate user input`() {
        Thread.sleep(250)
        assertTrue(true)
    }

    @Test
    fun `should fetch remote configuration`() {
        Thread.sleep(500)
        assertTrue(true)
    }

    @Test
    fun `should synchronize database records`() {
        Thread.sleep(750)
        assertTrue(true)
    }

    @Test
    fun `should generate analytics report`() {
        Thread.sleep(1000)
        assertTrue(true)
    }

    @Test
    fun `should complete full data migration`() {
        Thread.sleep(1500)
        assertTrue(true)
    }

    @Test
    fun `should handle concurrent write conflicts`() {
        Thread.sleep(300)
        assertEquals("expected", "actual", "Write conflict resolution failed")
    }

    @Test
    fun `should retry failed API call`() {
        Thread.sleep(800)
        assertTrue(false, "API retry logic did not succeed after max attempts")
    }

    @Test
    @Disabled("Payment gateway sandbox is unavailable in CI")
    fun `should charge credit card via gateway`() {
        Thread.sleep(400)
        assertTrue(true)
    }

    @Test
    @Disabled("Requires external S3 bucket access")
    fun `should upload file to cloud storage`() {
        Thread.sleep(600)
        assertTrue(true)
    }
}
