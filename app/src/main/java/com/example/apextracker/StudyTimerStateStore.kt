package com.example.apextracker

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.time.LocalDate
import java.time.ZoneId

private const val KEY_LAST_SUBJECT = "last_subject"

data class PersistedTimerState(
    val startedAtMillis: Long,
    val baseSeconds: Long,
    val date: LocalDate,
    val subject: String = ""
)

/**
 * Final duration for a session that was still running when the process died and whose day has
 * since ended: base + elapsed from start to that day's midnight boundary. Time past midnight is
 * deliberately not credited to any day — we can't know when the process actually died.
 */
fun finalizeSecondsAtEndOfDay(state: PersistedTimerState, zone: ZoneId): Long {
    val endOfDayMillis = state.date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val ranSeconds = ((endOfDayMillis - state.startedAtMillis) / 1000).coerceAtLeast(0)
    return state.baseSeconds + ranSeconds
}

/**
 * Persists the running stopwatch's identity (start timestamp + accumulated base) so a process
 * death mid-session doesn't silently stop the count. Written on start/pause/reset/rollover —
 * not per tick — so it's cheap; SharedPreferences is sufficient for three fields.
 */
class StudyTimerStateStore(context: Context) {
    private val prefs = context.getSharedPreferences("study_timer_state", Context.MODE_PRIVATE)

    fun saveRunning(startedAtMillis: Long, baseSeconds: Long, date: LocalDate, subject: String) {
        prefs.edit()
            .putBoolean("is_running", true)
            .putLong("started_at", startedAtMillis)
            .putLong("base_seconds", baseSeconds)
            .putString("date", date.toString())
            .putString("subject", subject)
            .putString(KEY_LAST_SUBJECT, subject)
            .apply()
    }

    /**
     * The subject the stopwatch last ran under. Survives [clear] on purpose: it is what the
     * home-screen widget attributes a session to (Issue #132), and the widget has no subject
     * picker — starting from the launcher should continue whatever you were last studying rather
     * than silently dumping the time into the "No subject" bucket.
     */
    var lastSubject: String
        get() = prefs.getString(KEY_LAST_SUBJECT, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAST_SUBJECT, value).apply()

    /** Clears the *running* state only — [lastSubject] is a separate, longer-lived preference. */
    fun clear() {
        prefs.edit()
            .remove("is_running")
            .remove("started_at")
            .remove("base_seconds")
            .remove("date")
            .remove("subject")
            .apply()
    }

    /**
     * Emits the running state on every change, so a start/pause made outside the app — from the
     * widget — reaches a live [StudyViewModel] instead of leaving its in-memory copy stale.
     * Emits the current value immediately on collection.
     */
    fun runningFlow(): Flow<PersistedTimerState?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            trySend(loadRunning())
        }
        trySend(loadRunning())
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /** State of a timer that was running when the app last died, or null if none/not running. */
    fun loadRunning(): PersistedTimerState? {
        if (!prefs.getBoolean("is_running", false)) return null
        val date = prefs.getString("date", null)?.let { parseLocalDateSafe(it) } ?: return null
        return PersistedTimerState(
            startedAtMillis = prefs.getLong("started_at", 0L),
            baseSeconds = prefs.getLong("base_seconds", 0L),
            date = date,
            subject = prefs.getString("subject", "") ?: ""
        )
    }
}
