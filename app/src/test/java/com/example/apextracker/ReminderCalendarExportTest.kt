package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class ReminderCalendarExportTest {

    private val utc = ZoneOffset.UTC
    private val date = LocalDate.of(2026, 8, 27)

    @Test
    fun `a timed reminder begins at its own date and time`() {
        val begin = calendarEventBeginMillis(date, LocalTime.of(18, 30), utc)
        assertEquals(date.atTime(18, 30).atZone(utc).toInstant().toEpochMilli(), begin)
    }

    @Test
    fun `a timed reminder ends 30 minutes after it begins`() {
        val begin = calendarEventBeginMillis(date, LocalTime.of(18, 30), utc)
        val end = calendarEventEndMillis(date, LocalTime.of(18, 30), utc)
        assertEquals(30 * 60_000L, end - begin)
    }

    @Test
    fun `an all-day reminder begins at UTC midnight of its date regardless of zone`() {
        val zone = ZoneId.of("America/Los_Angeles")
        val begin = calendarEventBeginMillis(date, null, zone)
        assertEquals(date.atStartOfDay(utc).toInstant().toEpochMilli(), begin)
    }

    @Test
    fun `an all-day reminder ends at the next UTC day boundary`() {
        val begin = calendarEventBeginMillis(date, null, utc)
        val end = calendarEventEndMillis(date, null, utc)
        assertEquals(24 * 60 * 60_000L, end - begin)
    }

    @Test
    fun `a timed reminder's begin time honors the given zone, not UTC`() {
        // 18:30 in Los Angeles (UTC-7 in August, DST) is 01:30 UTC the next day.
        val zone = ZoneId.of("America/Los_Angeles")
        val begin = calendarEventBeginMillis(date, LocalTime.of(18, 30), zone)
        val expected = date.atTime(18, 30).atZone(zone).toInstant().toEpochMilli()
        assertEquals(expected, begin)
        assertEquals(date.plusDays(1).atTime(1, 30).atZone(utc).toInstant().toEpochMilli(), begin)
    }

    // calendarInsertIntent() itself isn't unit-tested — it constructs a real android.content.Intent
    // and reads android.provider.CalendarContract constants, both framework types unavailable in a
    // plain JVM unit test (no Robolectric in this project). The millis math it depends on, tested
    // above, is the only non-trivial logic in that function.
}
