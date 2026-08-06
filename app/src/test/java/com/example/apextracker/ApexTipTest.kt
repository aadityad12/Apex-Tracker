package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ApexTipTest {
    private val today = LocalDate.of(2026, 8, 5)

    @Test
    fun promptContainsOnlyTheExplicitAnonymousSnapshot() {
        val prompt = buildApexTipPrompt(
            ApexTipSnapshot(
                date = today,
                currencyCode = "USD",
                spentToday = 12.5,
                spentThisMonth = 80.0,
                monthlyBudgetLimit = 500.0,
                studyMinutes = 45,
                screenMinutes = 90,
                completedGoals = 2,
                totalGoals = 3,
                perfectStreak = 4
            )
        )

        assertTrue(prompt.contains("Spending today: 12.50"))
        assertTrue(prompt.contains("Monthly spending limit: 500.00"))
        assertTrue(prompt.contains("Goals completed today: 2 of 3"))
        assertTrue(prompt.contains("Perfect-day streak: 4"))
        assertTrue(prompt.contains("Do not invent context"))
    }

    @Test
    fun automaticGenerationRunsAtMostOncePerDay() {
        assertTrue(shouldGenerateApexTip(true, today, cachedDate = null, lastAttemptDate = null))
        assertFalse(shouldGenerateApexTip(true, today, cachedDate = today, lastAttemptDate = null))
        assertFalse(shouldGenerateApexTip(true, today, cachedDate = null, lastAttemptDate = today))
        assertFalse(shouldGenerateApexTip(false, today, cachedDate = null, lastAttemptDate = null))
        assertTrue(
            shouldGenerateApexTip(
                enabled = true,
                date = today,
                cachedDate = null,
                lastAttemptDate = today,
                forceRetry = true
            )
        )
    }

    @Test
    fun responseNormalizationRejectsEmptyAndBoundsStoredText() {
        assertNull(normalizeApexTip("   \n"))
        assertEquals("Take one small step today.", normalizeApexTip("  \"Take one   small step today.\"  "))

        val bounded = normalizeApexTip("word ".repeat(100), maxChars = 40)!!
        assertTrue(bounded.length <= 40)
        assertTrue(bounded.endsWith("…"))
    }
}
