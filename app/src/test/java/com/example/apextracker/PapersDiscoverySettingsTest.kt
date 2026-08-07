package com.example.apextracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PapersDiscoverySettingsTest {
    private val today = LocalDate.of(2026, 8, 7)

    @Test
    fun `daily gate requires a new unblocked date`() {
        assertFalse(shouldFetchDailyPapers(PapersDiscoveryPreferences(lastFetchDate = today), today, 100L))
        assertFalse(shouldFetchDailyPapers(PapersDiscoveryPreferences(blockedUntilMillis = 101L), today, 100L))
        assertTrue(shouldFetchDailyPapers(PapersDiscoveryPreferences(blockedUntilMillis = 100L), today, 100L))
        assertTrue(shouldFetchDailyPapers(PapersDiscoveryPreferences(), today, 100L))
    }
}
