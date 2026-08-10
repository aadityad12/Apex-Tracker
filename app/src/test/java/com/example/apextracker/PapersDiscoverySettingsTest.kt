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

    @Test
    fun `recommendation gate has its own date but shares the backoff window`() {
        // Separate day markers so a quiet topic search doesn't also stop recommendations…
        assertTrue(
            shouldFetchRecommendations(PapersDiscoveryPreferences(lastFetchDate = today), today, 100L)
        )
        assertFalse(
            shouldFetchRecommendations(PapersDiscoveryPreferences(lastRecommendationDate = today), today, 100L)
        )
        // …but one shared 429 window, because both draw on the same unauthenticated pool.
        assertFalse(
            shouldFetchRecommendations(PapersDiscoveryPreferences(blockedUntilMillis = 101L), today, 100L)
        )
        assertTrue(shouldFetchRecommendations(PapersDiscoveryPreferences(), today, 100L))
    }
}
