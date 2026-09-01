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

    @Test
    fun `clock time pads seconds and drops an empty hour field`() {
        assertEquals("0:07", formatClockTime(7_000))
        assertEquals("3:29", formatClockTime(209_000))
        assertEquals("59:59", formatClockTime(3_599_000))
    }

    @Test
    fun `clock time grows an hour field only when it has one`() {
        assertEquals("1:00:00", formatClockTime(3_600_000))
        assertEquals("1:04:07", formatClockTime(3_847_000))
    }

    @Test
    fun `clock time clamps a negative duration to zero`() {
        // A media session is free to report nonsense; Kotlin's % would render it as "-1:-5".
        assertEquals("0:00", formatClockTime(-5_000))
    }
}
