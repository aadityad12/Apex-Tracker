package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationFormatTest {

    @Test
    fun `under an hour renders just minutes`() {
        assertEquals("45m", formatDurationCompact(45 * 60_000L))
    }

    @Test
    fun `an hour or more renders hours and minutes`() {
        assertEquals("2h 5m", formatDurationCompact((2 * 60 + 5) * 60_000L))
    }

    @Test
    fun `zero renders 0m`() {
        assertEquals("0m", formatDurationCompact(0L))
    }

    @Test
    fun `a negative duration clamps to 0m rather than rendering garbage`() {
        // Regression test for Issue #248: Kotlin's % on a negative dividend yields a negative
        // remainder, so this used to render "-2h -5m" instead of failing safely.
        assertEquals("0m", formatDurationCompact(-(2 * 60 + 5) * 60_000L))
    }
}
