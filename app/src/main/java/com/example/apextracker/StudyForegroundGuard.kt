package com.example.apextracker

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

private const val TAG = "StudyAwayGuard"

/**
 * Whether any of this app's UI is currently on screen, tracked at the process level.
 *
 * Process-scoped rather than a lifecycle observer, the same shape as [UnlockSession]: the fact is
 * about the process, not about any one composable, and the alternative — pulling in
 * `androidx.lifecycle:lifecycle-process` for `ProcessLifecycleOwner` — is a dependency for
 * something this app's single Activity already knows exactly.
 *
 * A counter rather than a boolean because an Activity-to-Activity handoff overlaps (the incoming
 * onStart lands before the outgoing onStop): with a boolean, the trailing onStop clears a flag the
 * arriving Activity had just legitimately set, and the app reads as backgrounded while it is on
 * screen. Today this app has one Activity, so the overlap is unreachable and the counter is purely
 * defensive — but it costs nothing, and the failure it prevents is a study session pausing itself
 * for no visible reason, which is not a bug anyone would enjoy diagnosing.
 */
object AppForeground {
    private val startedActivities = AtomicInteger(0)

    val isForeground: Boolean
        get() = startedActivities.get() > 0

    fun onActivityStarted() {
        startedActivities.incrementAndGet()
    }

    fun onActivityStopped() {
        startedActivities.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
    }
}

/**
 * Whether a running study session should auto-pause, given what the device is doing right now.
 *
 * The study timer's policy is "leaving the app while awake pauses; the screen going off keeps
 * counting" — a session you are watching must survive the screen timing out, but wandering off into
 * another app is not studying. [StudyViewModel.handleAppBackground] enforces the first half at
 * ON_STOP, and that used to be the only enforcement there was, which left the screen-off branch as
 * a permanent hole: sleep the phone with the timer running, open something else from a lock-screen
 * notification, and the ON_STOP decision to keep counting had already been made and was never
 * revisited. The timer ran for as long as you liked, in any app.
 *
 * This is the missing second half — the same policy, re-evaluated while the app is away. Each term
 * earns its place:
 *  - [screenInteractive] / [keyguardLocked]: a glance at the lock screen is not leaving the app, so
 *    the session survives it. Only a real unlock counts.
 *  - [appForeground]: unlocking *into* ApexTracker is the ordinary case and must keep running. This
 *    is the term the whole thing turns on.
 *
 * Pure so the truth table is testable; the callers supply the readings.
 */
fun shouldAutoPauseOnUnlock(
    isRunning: Boolean,
    screenInteractive: Boolean,
    keyguardLocked: Boolean,
    appForeground: Boolean
): Boolean = isRunning && screenInteractive && !keyguardLocked && !appForeground

/**
 * How many consecutive one-second ticks must agree the user is away before the in-app ticker acts.
 *
 * Waking the device straight back into ApexTracker briefly looks identical to waking into some
 * other app: the screen turns on and the keyguard clears a moment before MainActivity's onStart
 * runs, so a single tick landing in that gap would pause a session the user is looking at. Two
 * agreeing ticks is about a second of sustained "unlocked, and not us", which no resume takes.
 */
const val AWAY_TICKS_BEFORE_PAUSE = 2

/** Folds one tick's reading into the run of consecutive away-ticks. Pure; see [AWAY_TICKS_BEFORE_PAUSE]. */
fun nextAwayTickCount(previous: Int, awayNow: Boolean): Int = if (awayNow) previous + 1 else 0

/**
 * The out-of-process half of the same policy: a heartbeat alarm that re-checks while the app is in
 * the background.
 *
 * This exists because the obvious implementations do not work, which is worth recording so nobody
 * re-derives them:
 *
 *  - **A receiver for ACTION_USER_PRESENT / ACTION_SCREEN_ON does not fire.** Measured on API 36:
 *    a dynamically registered receiver gets SCREEN_ON reliably while the app is in the foreground
 *    and never once while it is cached, which is the only state this feature is about. (Manifest
 *    registration is not an option either — USER_PRESENT is not delivered to manifest receivers.)
 *  - **The per-second ticker stops.** Also measured: it kept running for ~12s after the screen went
 *    off and was then frozen for the remaining 140s of the test, never resuming even once the
 *    device was unlocked into another app. Android freezes cached processes; a coroutine cannot
 *    watch for anything through that.
 *
 * An alarm is the mechanism that thaws a frozen process, so it is the one that works. The cost is
 * granularity: the pause lands within [CHECK_INTERVAL_MILLIS] of the unlock rather than on it. The
 * ticker's own check (see [nextAwayTickCount]) covers the first few seconds exactly, which is the
 * common case — put the phone down, pick it straight back up — and this covers the rest.
 *
 * Inexact and `AndAllowWhileIdle`: exactness buys nothing at a one-minute cadence, and the app
 * should not spend its exact-alarm budget on a poll. Doze deferring the alarm is *correct* here —
 * a dozing device is one nobody is using, so there is nothing to catch.
 */
object StudyAwayGuard {
    const val CHECK_INTERVAL_MILLIS = 60_000L
    private const val REQUEST_CODE = 91_741

    /** Arms the heartbeat if — and only if — a session is actually running. */
    fun armIfRunning(context: Context) {
        if (StudyTimerStateStore(context).loadRunning() == null) return
        arm(context)
    }

    fun arm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + CHECK_INTERVAL_MILLIS,
            pendingIntent(context)
        )
    }

    fun disarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context))
    }

    /** The device readings [shouldAutoPauseOnUnlock] needs, read from a plain Context. */
    fun isUserAwayFromApp(context: Context, isRunning: Boolean): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return shouldAutoPauseOnUnlock(
            isRunning = isRunning,
            screenInteractive = powerManager.isInteractive,
            keyguardLocked = keyguardManager.isKeyguardLocked,
            appForeground = AppForeground.isForeground
        )
    }

    private fun pendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        REQUEST_CODE,
        Intent(context, StudyAwayCheckReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

/**
 * One beat of [StudyAwayGuard]: pause the session if the user is out in the rest of their phone,
 * otherwise keep watching.
 *
 * Pausing goes through the shared [pauseStudyTimer] rather than a local copy for the same reason
 * the widget's toggle does (Issue #132) — Room and [StudyTimerStateStore] stay the two records of
 * truth, and a live [StudyViewModel] adopts the change through the store's own flow. Being woken
 * into a fresh process is fine and needs no special case: [AppForeground] reads false there, which
 * is exactly right, because a process that had to be restarted was not on screen.
 */
class StudyAwayCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        // goAsync + IO: this reads prefs, writes Room and pushes to Firestore, none of which fits
        // in a receiver's main-thread window — the same shape ReminderActionReceiver uses.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timerStore = StudyTimerStateStore(appContext)
                val running = timerStore.loadRunning() != null
                if (!running) {
                    // Stopped since the alarm was set — nothing to watch, and re-arming would
                    // leave a poll running for a session that no longer exists.
                    StudyAwayGuard.disarm(appContext)
                    return@launch
                }
                if (StudyAwayGuard.isUserAwayFromApp(appContext, isRunning = true)) {
                    Log.i(TAG, "Device in use elsewhere — pausing the study session")
                    pauseStudyTimer(
                        context = appContext,
                        db = AppDatabase.getDatabase(appContext),
                        timerStore = timerStore,
                        firebaseManager = FirebaseManager(appContext),
                        nowMillis = System.currentTimeMillis()
                    )
                    StudyAwayGuard.disarm(appContext)
                } else {
                    StudyAwayGuard.arm(appContext)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Away check failed", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
