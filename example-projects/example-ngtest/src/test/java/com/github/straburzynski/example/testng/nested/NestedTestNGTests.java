package com.github.straburzynski.example.testng.nested;

import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class NestedTestNGTests {

    @Test
    public void shouldEncryptSensitiveData() {
        sleep(400);
        assertTrue(true);
    }

    @Test
    public void shouldValidateApiResponseSchema() {
        sleep(600);
        assertTrue(true);
    }

    @Test
    public void shouldThrottleOutboundNotifications() {
        sleep(900);
        assertTrue(true);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
