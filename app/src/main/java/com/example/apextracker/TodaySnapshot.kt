package com.example.apextracker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A read-only snapshot of the three "today" numbers the at-a-glance widget shows (Issue #44),
 * computed off Room in one place so the widget and the app agree. Mirrors [DashboardSnapshot]'s
 * shape: the arithmetic lives in the pure functions below, this type is just the result.
 */
data class TodaySnapshot(
    val studySeconds: Long,
    /** True while the study stopwatch is running, so the widget can mark the total as live. */
    val studyRunning: Boolean,
    val screenMillis: Long,
    val nextReminder: NextReminder?
)

/** The one reminder the widget names, plus whether its due time has already passed. */
data class NextReminder(
    val name: String,
    val dueAt: LocalDateTime,
    /** Null for an all-day reminder — it has no clock time to render. */
    val time: LocalTime?,
    val isOverdue: Boolean
)

/**
 * Today's study total in seconds: every banked per-subject row for [today] plus, when the
 * stopwatch is running, the seconds it has accumulated since its last save.
 *
 * The running timer only writes to Room on pause/reset/rollover, so a banked-only total reads as
 * frozen for however long a session lasts — the one number a study widget exists to show. The live
 * part is deliberately derived from [PersistedTimerState] rather than a second source of truth:
 * `baseSeconds` is what was already banked when the timer started, so it must replace, not add to,
 * that subject's stored row. A state left over from an earlier day contributes nothing here; it is
 * credited to its own day by [finalizeSecondsAtEndOfDay] when the app next starts.
 */
fun todayStudySeconds(
    sessions: List<StudySession>,
    today: LocalDate,
    running: PersistedTimerState?,
    nowMillis: Long
): Long {
    val todays = sessions.filter { it.date == today }
    val live = running?.takeIf { it.date == today }
    val banked = todays.filter { live == null || it.subject != live.subject }.sumOf { it.durationSeconds }
    if (live == null) return banked
    val elapsed = ((nowMillis - live.startedAtMillis) / 1000).coerceAtLeast(0)
    return banked + live.baseSeconds + elapsed
}

/**
 * The single active reminder worth naming on the widget: the soonest one still ahead of [now],
 * falling back to the *most recently* due overdue reminder when nothing is upcoming.
 *
 * Preferring upcoming over overdue is what stops a months-old unfinished reminder from squatting
 * on the widget forever; picking the newest of the overdue ones (rather than the oldest) keeps the
 * fallback moving as reminders come due. All-day reminders are ordered at end-of-day, the same
 * convention [Reminder.isOverdue] uses, so "today, all day" stays upcoming until midnight.
 */
fun nextReminderFor(reminders: List<Reminder>, now: LocalDateTime): NextReminder? {
    fun dueAt(r: Reminder) = LocalDateTime.of(r.date, r.time ?: LocalTime.MAX)
    val active = reminders.filterNot { it.isCompleted }
    val upcoming = active.filter { !dueAt(it).isBefore(now) }.minByOrNull { dueAt(it) }
    val chosen = upcoming ?: active.maxByOrNull { dueAt(it) } ?: return null
    val due = dueAt(chosen)
    return NextReminder(
        name = chosen.name,
        dueAt = due,
        time = chosen.time,
        isOverdue = due.isBefore(now)
    )
}

/**
 * Reads the current snapshot for [today]; safe to call from a widget's provideGlance.
 *
 * Screen time comes from the row the Screen Time screen persists, not a fresh `UsageStatsManager`
 * query: re-deriving it here would need the installed-app filter that keeps the headline total in
 * step with the app list (Issue #159), and enumerating packages is far too heavy for a widget
 * update. The tradeoff is that the tile ages until the app next polls, which is why every poll
 * calls [com.example.apextracker.widget.refreshTodayWidget].
 */
suspend fun loadTodaySnapshot(
    db: AppDatabase,
    timerStore: StudyTimerStateStore,
    now: LocalDateTime,
    nowMillis: Long
): TodaySnapshot = withContext(Dispatchers.IO) {
    val today = now.toLocalDate()
    val running = timerStore.loadRunning()
    TodaySnapshot(
        studySeconds = todayStudySeconds(
            db.studySessionDao().getAllSessionsOneShot(),
            today,
            running,
            nowMillis
        ),
        studyRunning = running?.date == today,
        screenMillis = db.screenTimeSessionDao().getSessionByDate(today)?.durationMillis ?: 0L,
        nextReminder = nextReminderFor(db.reminderDao().getAllRemindersOneShot(), now)
    )
}
