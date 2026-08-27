package com.example.apextracker

import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * A reminder models a due *moment*, not a span, so a calendar event needs some block of time —
 * 30 minutes is long enough to be visible on a day view without implying the task itself takes
 * that long.
 */
private const val DEFAULT_EVENT_DURATION_MINUTES = 30L

/**
 * Epoch millis a reminder's calendar event should begin at (Issue #220). An all-day reminder
 * (`time == null`) follows `CalendarContract`'s `ALL_DAY` convention: UTC midnight of that date,
 * regardless of the device's own zone — the calendar provider interprets an all-day event's
 * begin/end as UTC-anchored day boundaries, not local ones.
 */
fun calendarEventBeginMillis(date: LocalDate, time: LocalTime?, zone: ZoneId = ZoneId.systemDefault()): Long =
    if (time != null) {
        date.atTime(time).atZone(zone).toInstant().toEpochMilli()
    } else {
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

/**
 * Epoch millis the event should end at: a [DEFAULT_EVENT_DURATION_MINUTES] block for a timed
 * reminder, or the next UTC day boundary for an all-day one (a single-day all-day event's END is
 * the *start* of the following day, per `CalendarContract`).
 */
fun calendarEventEndMillis(date: LocalDate, time: LocalTime?, zone: ZoneId = ZoneId.systemDefault()): Long =
    if (time != null) {
        date.atTime(time).plusMinutes(DEFAULT_EVENT_DURATION_MINUTES).atZone(zone).toInstant().toEpochMilli()
    } else {
        date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }

/**
 * Hands a reminder off to the user's calendar app via `ACTION_INSERT` (Issue #220) rather than
 * writing to `CalendarContract` directly — no new permission to request, and the calendar app
 * shows its own confirm/edit UI before anything is actually saved, so this can't silently create
 * duplicate events on repeat taps the way a direct provider insert could.
 */
fun calendarInsertIntent(name: String, description: String?, date: LocalDate, time: LocalTime?): Intent =
    Intent(Intent.ACTION_INSERT).apply {
        data = CalendarContract.Events.CONTENT_URI
        putExtra(CalendarContract.Events.TITLE, name)
        description?.let { putExtra(CalendarContract.Events.DESCRIPTION, it) }
        putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, time == null)
        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, calendarEventBeginMillis(date, time))
        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, calendarEventEndMillis(date, time))
    }
