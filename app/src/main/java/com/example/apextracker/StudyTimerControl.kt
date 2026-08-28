package com.example.apextracker

import android.content.Context
import com.example.apextracker.widget.refreshStudyWidget
import com.example.apextracker.widget.refreshTodayWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "StudyTimerControl"

/**
 * The shared start/pause path for the study stopwatch.
 *
 * These are top-level for the same reason [completeReminder] is: the home-screen widget's action
 * callback (Issue #132) runs with no `StudyViewModel` to reach, and duplicating "bank the elapsed
 * time under the right (date, subject) row, then push and refresh" is exactly how the widget and
 * the in-app button would drift apart. [StudyTimerStateStore] stays the one durable record of
 * whether the stopwatch is running, and Room stays the one record of how much time it has banked
 * — [StudyViewModel] holds a mirror of both and re-derives it from these results rather than
 * keeping its own parallel truth.
 *
 * **Any new way to start or stop the stopwatch must go through these.**
 */

/** What a pause banked, so a live ViewModel can re-sync without a second read. */
data class StudyPauseResult(
    val date: LocalDate,
    val subject: String,
    val bankedSeconds: Long
)

/**
 * Begins (or resumes) the stopwatch under [subject], returning the state that was persisted — or
 * the existing state untouched if it was already running.
 *
 * The base is read from Room rather than passed in: the row for `(today, subject)` is what the
 * total has to continue from, and reading it here means a caller with no idea what has been
 * studied today — the widget — is as correct as the Study screen.
 */
suspend fun startStudyTimer(
    db: AppDatabase,
    timerStore: StudyTimerStateStore,
    subject: String,
    nowMillis: Long,
    today: LocalDate = LocalDate.now()
): PersistedTimerState {
    timerStore.loadRunning()?.let { if (it.date == today) return it }
    val banked = db.studySessionDao().getSession(today, subject)?.durationSeconds ?: 0L
    timerStore.saveRunning(nowMillis, banked, today, subject)
    return PersistedTimerState(nowMillis, banked, today, subject)
}

/**
 * What stopping [state] should bank, and against which day.
 *
 * A state left over from an earlier day is credited to *that* day up to its own midnight boundary
 * via [finalizeSecondsAtEndOfDay], never to today; time past midnight belongs to no day, because
 * nothing recorded when the session actually ended. Pure so the day-boundary case is testable —
 * it is the one that silently loses or misattributes study time when it's wrong.
 */
fun studyPauseResultFor(
    state: PersistedTimerState,
    nowMillis: Long,
    today: LocalDate,
    zone: ZoneId
): StudyPauseResult {
    val total = if (state.date == today) {
        state.baseSeconds + ((nowMillis - state.startedAtMillis) / 1000).coerceAtLeast(0)
    } else {
        finalizeSecondsAtEndOfDay(state, zone)
    }
    return StudyPauseResult(state.date, state.subject, total)
}

/**
 * Stops the stopwatch and banks its elapsed time, returning what was written — or null if it
 * wasn't running.
 */
suspend fun pauseStudyTimer(
    context: Context,
    db: AppDatabase,
    timerStore: StudyTimerStateStore,
    firebaseManager: FirebaseManager,
    nowMillis: Long,
    today: LocalDate = LocalDate.now()
): StudyPauseResult? {
    val state = timerStore.loadRunning() ?: return null
    val result = studyPauseResultFor(state, nowMillis, today, ZoneId.systemDefault())
    timerStore.clear()
    bankStudySeconds(context, db, firebaseManager, result.date, result.subject, result.bankedSeconds)
    return result
}

/** Convenience for the widget's single control, which has no separate start and pause buttons. */
suspend fun toggleStudyTimer(
    context: Context,
    db: AppDatabase,
    timerStore: StudyTimerStateStore,
    firebaseManager: FirebaseManager,
    nowMillis: Long,
    today: LocalDate = LocalDate.now()
) {
    val running = timerStore.loadRunning()
    if (running != null) {
        pauseStudyTimer(context, db, timerStore, firebaseManager, nowMillis, today)
    } else {
        startStudyTimer(db, timerStore, timerStore.lastSubject, nowMillis, today)
        refreshStudyWidget(context)
        refreshTodayWidget(context)
    }
}

/**
 * Writes one `(date, subject)` total to Room, pushes it to the cloud and refreshes the widgets —
 * the "significant event" write, as opposed to the per-second ticker save that
 * [StudyViewModel.saveSessionForDate] throttles.
 */
private suspend fun bankStudySeconds(
    context: Context,
    db: AppDatabase,
    firebaseManager: FirebaseManager,
    date: LocalDate,
    subject: String,
    seconds: Long
) {
    val session = StudySession(date = date, subject = subject, durationSeconds = seconds)
    db.studySessionDao().insertSession(session)
    safeCloudCall(TAG, "push study session") { firebaseManager.pushStudySession(session) }
    refreshStudyWidget(context)
    refreshTodayWidget(context)
}

/**
 * Today's grand total across every subject, including the seconds the running stopwatch has not
 * banked yet. Shared by both widgets so they can't disagree about "today"; see
 * [todayStudySeconds], which this delegates to.
 */
suspend fun loadTodayStudySeconds(
    db: AppDatabase,
    timerStore: StudyTimerStateStore,
    nowMillis: Long,
    today: LocalDate = LocalDate.now()
): Long = withContext(Dispatchers.IO) {
    todayStudySeconds(
        db.studySessionDao().getAllSessionsOneShot(),
        today,
        timerStore.loadRunning(),
        nowMillis
    )
}
