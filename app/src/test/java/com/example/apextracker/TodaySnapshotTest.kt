package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/** The pure logic behind the "today at a glance" widget (Issue #44). */
class TodaySnapshotTest {

    private val today = LocalDate.of(2026, 8, 10)
    private val yesterday = today.minusDays(1)
    private val nowMillis = 1_000_000L

    private fun study(date: LocalDate, subject: String, seconds: Long) =
        StudySession(date = date, subject = subject, durationSeconds = seconds)

    private fun reminder(
        name: String,
        date: LocalDate,
        time: LocalTime?,
        completed: Boolean = false
    ) = Reminder(id = 0, name = name, date = date, time = time, isCompleted = completed)

    // --- todayStudySeconds -------------------------------------------------

    @Test
    fun `sums every subject studied today and ignores other days`() {
        val sessions = listOf(
            study(today, "", 60),
            study(today, "Math", 120),
            study(yesterday, "Math", 9999)
        )
        assertEquals(180, todayStudySeconds(sessions, today, running = null, nowMillis = nowMillis))
    }

    @Test
    fun `returns zero when nothing has been studied today`() {
        assertEquals(0, todayStudySeconds(listOf(study(yesterday, "", 500)), today, null, nowMillis))
    }

    @Test
    fun `adds the running timer's live seconds on top of its banked base`() {
        // Timer started 90s ago having already banked 30s under "Math".
        val running = PersistedTimerState(
            startedAtMillis = nowMillis - 90_000,
            baseSeconds = 30,
            date = today,
            subject = "Math"
        )
        val sessions = listOf(study(today, "Math", 30), study(today, "History", 10))
        assertEquals(30 + 90 + 10, todayStudySeconds(sessions, today, running, nowMillis))
    }

    @Test
    fun `does not double-count the running subject's stored row`() {
        // The ticker writes to Room every second, so the stored row already contains the live
        // total. baseSeconds must replace it, not add to it.
        val running = PersistedTimerState(
            startedAtMillis = nowMillis - 60_000,
            baseSeconds = 100,
            date = today,
            subject = "Math"
        )
        val sessions = listOf(study(today, "Math", 160))
        assertEquals(160, todayStudySeconds(sessions, today, running, nowMillis))
    }

    @Test
    fun `a timer left running from an earlier day contributes nothing to today`() {
        val stale = PersistedTimerState(
            startedAtMillis = nowMillis - 500_000,
            baseSeconds = 1_000,
            date = yesterday,
            subject = ""
        )
        assertEquals(45, todayStudySeconds(listOf(study(today, "", 45)), today, stale, nowMillis))
    }

    @Test
    fun `a start timestamp in the future never yields negative elapsed time`() {
        val skewed = PersistedTimerState(
            startedAtMillis = nowMillis + 60_000,
            baseSeconds = 20,
            date = today,
            subject = ""
        )
        assertEquals(20, todayStudySeconds(emptyList(), today, skewed, nowMillis))
    }

    // --- nextReminderFor ---------------------------------------------------

    private val now = LocalDateTime.of(today, LocalTime.of(12, 0))

    @Test
    fun `picks the soonest reminder still ahead of now`() {
        val next = nextReminderFor(
            listOf(
                reminder("later today", today, LocalTime.of(18, 0)),
                reminder("soon", today, LocalTime.of(13, 0)),
                reminder("tomorrow", today.plusDays(1), LocalTime.of(9, 0))
            ),
            now
        )
        assertEquals("soon", next?.name)
        assertFalse(next!!.isOverdue)
    }

    @Test
    fun `skips completed reminders`() {
        val next = nextReminderFor(
            listOf(
                reminder("done", today, LocalTime.of(13, 0), completed = true),
                reminder("open", today, LocalTime.of(14, 0))
            ),
            now
        )
        assertEquals("open", next?.name)
    }

    @Test
    fun `an all-day reminder today stays upcoming until midnight`() {
        val next = nextReminderFor(listOf(reminder("all day", today, null)), now)
        assertEquals("all day", next?.name)
        assertFalse(next!!.isOverdue)
        assertNull(next.time)
    }

    @Test
    fun `falls back to the most recently due overdue reminder`() {
        // Preferring the newest overdue one keeps an ancient unfinished reminder from squatting
        // on the widget forever.
        val next = nextReminderFor(
            listOf(
                reminder("ancient", today.minusMonths(3), LocalTime.of(9, 0)),
                reminder("this morning", today, LocalTime.of(8, 0))
            ),
            now
        )
        assertEquals("this morning", next?.name)
        assertTrue(next!!.isOverdue)
    }

    @Test
    fun `upcoming wins over overdue`() {
        val next = nextReminderFor(
            listOf(
                reminder("overdue", today, LocalTime.of(8, 0)),
                reminder("upcoming", today.plusDays(4), LocalTime.of(8, 0))
            ),
            now
        )
        assertEquals("upcoming", next?.name)
        assertFalse(next!!.isOverdue)
    }

    @Test
    fun `no active reminders yields null`() {
        assertNull(nextReminderFor(emptyList(), now))
        assertNull(
            nextReminderFor(listOf(reminder("done", today, LocalTime.of(9, 0), completed = true)), now)
        )
    }

    @Test
    fun `a reminder due exactly now counts as upcoming`() {
        val next = nextReminderFor(listOf(reminder("now", today, LocalTime.of(12, 0))), now)
        assertEquals("now", next?.name)
        assertFalse(next!!.isOverdue)
    }
}
