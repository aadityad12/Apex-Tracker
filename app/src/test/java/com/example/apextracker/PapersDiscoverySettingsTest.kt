package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PapersDiscoverySettingsTest {
    private val today = LocalDate.of(2026, 8, 7)

    @Test
    fun `daily gate requires topics and a new unblocked date`() {
        assertFalse(shouldFetchDailyPapers(PapersDiscoveryPreferences(), today, 100L))
        assertFalse(
            shouldFetchDailyPapers(
                PapersDiscoveryPreferences(setOf("Physics"), lastFetchDate = today), today, 100L
            )
        )
        assertFalse(
            shouldFetchDailyPapers(
                PapersDiscoveryPreferences(setOf("Physics"), blockedUntilMillis = 101L), today, 100L
            )
        )
        assertTrue(
            shouldFetchDailyPapers(
                PapersDiscoveryPreferences(setOf("Physics"), blockedUntilMillis = 100L), today, 100L
            )
        )
    }

    @Test
    fun `daily field is stable and rotates across selected fields`() {
        val fields = setOf("Physics", "Computer Science", "Mathematics")
        val first = dailyPaperField(fields, today)
        assertEquals(first, dailyPaperField(fields.reversed().toSet(), today))
        assertTrue(dailyPaperField(fields, today.plusDays(1)) in fields)
        assertFalse(first == dailyPaperField(fields, today.plusDays(1)))
        assertNull(dailyPaperField(emptySet(), today))
    }
}
