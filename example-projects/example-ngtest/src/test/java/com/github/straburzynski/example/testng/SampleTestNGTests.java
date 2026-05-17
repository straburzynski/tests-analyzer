package com.github.straburzynski.example.testng;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class SampleTestNGTests {

    @Test
    public void shouldValidateUserRegistration() {
        sleep(300);
        assertTrue(true);
    }

    @Test
    public void shouldProcessPaymentTransaction() {
        sleep(800);
        assertTrue(true);
    }

    @Test
    public void shouldGenerateMonthlyReport() {
        sleep(1500);
        assertTrue(true);
    }

    @Test
    public void shouldSyncDataAcrossRegions() {
        sleep(500);
        assertTrue(true);
    }

    @Test
    public void shouldHandleConcurrentRequests() {
        sleep(700);
        fail("Concurrent request handler returned inconsistent results");
    }

    @Test(enabled = false)
    public void shouldMigrateLegacyDatabaseSchema() {
        sleep(1000);
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
