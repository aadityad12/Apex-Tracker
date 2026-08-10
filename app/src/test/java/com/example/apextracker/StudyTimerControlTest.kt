package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The arithmetic behind the shared start/pause path (Issue #132) — the part the widget and the
 * in-app button both depend on, and the part that silently misattributes study time when wrong.
 */
class StudyTimerControlTest {
    private val utc = ZoneId.of("UTC")
    private val today = LocalDate.of(2026, 8, 10)

    private fun millisOf(dateTime: LocalDateTime): Long =
        dateTime.atZone(utc).toInstant().toEpochMilli()

    @Test
    fun `banks the base plus the seconds elapsed since the timer started`() {
        val state = PersistedTimerState(
            startedAtMillis = millisOf(LocalDateTime.of(2026, 8, 10, 9, 0)),
            baseSeconds = 300,
            date = today,
            subject = "Math"
        )
        val result = studyPauseResultFor(
            state,
            nowMillis = millisOf(LocalDateTime.of(2026, 8, 10, 9, 30)),
            today = today,
            zone = utc
        )
        assertEquals(today, result.date)
        assertEquals("Math", result.subject)
        assertEquals(300 + 1800, result.bankedSeconds)
    }

    @Test
    fun `a session started yesterday is credited to yesterday, up to midnight`() {
        // The user forgot to stop the timer overnight. The seconds after midnight belong to no
        // day — nothing recorded when they actually stopped studying — and none of them may leak
        // into today's row.
        val state = PersistedTimerState(
            startedAtMillis = millisOf(LocalDateTime.of(2026, 8, 9, 23, 0)),
            baseSeconds = 600,
            date = today.minusDays(1),
            subject = "History"
        )
        val result = studyPauseResultFor(
            state,
            nowMillis = millisOf(LocalDateTime.of(2026, 8, 10, 11, 0)),
            today = today,
            zone = utc
        )
        assertEquals(today.minusDays(1), result.date)
        assertEquals("History", result.subject)
        assertEquals(600 + 3600, result.bankedSeconds)
    }

    @Test
    fun `pausing immediately banks exactly the base`() {
        val start = millisOf(LocalDateTime.of(2026, 8, 10, 9, 0))
        val state = PersistedTimerState(start, baseSeconds = 42, date = today, subject = "")
        assertEquals(42, studyPauseResultFor(state, start, today, utc).bankedSeconds)
    }

    @Test
    fun `a start timestamp in the future never subtracts from the base`() {
        // Defensive: a clock change between start and pause could put the start ahead of now.
        val state = PersistedTimerState(
            startedAtMillis = millisOf(LocalDateTime.of(2026, 8, 10, 12, 0)),
            baseSeconds = 100,
            date = today,
            subject = ""
        )
        val result = studyPauseResultFor(
            state,
            nowMillis = millisOf(LocalDateTime.of(2026, 8, 10, 11, 0)),
            today = today,
            zone = utc
        )
        assertEquals(100, result.bankedSeconds)
    }

    @Test
    fun `the uncategorized bucket keeps its empty subject`() {
        val state = PersistedTimerState(
            startedAtMillis = millisOf(LocalDateTime.of(2026, 8, 10, 9, 0)),
            baseSeconds = 0,
            date = today,
            subject = ""
        )
        val result = studyPauseResultFor(
            state,
            nowMillis = millisOf(LocalDateTime.of(2026, 8, 10, 9, 1)),
            today = today,
            zone = utc
        )
        assertEquals("", result.subject)
        assertEquals(60, result.bankedSeconds)
    }
}
