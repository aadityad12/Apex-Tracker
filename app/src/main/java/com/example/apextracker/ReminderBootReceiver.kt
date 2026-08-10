package com.example.apextracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Re-arms exact alarms after the alarm table is cleared out from under us — on reboot, and on an
 * app update, which also drops every pending alarm (Issue #195).
 *
 * This used to compute trigger times itself with [ReminderScheduler.computeTriggerTime], which
 * skips the clamping [ReminderScheduler.resolveTriggerTime] does, so every reboot silently
 * reintroduced Issue #80: any reminder due sooner than the notification offset (30 minutes by
 * default) was dropped without a log. It now goes through [rescheduleAllReminders], the same
 * decision every other scheduling path uses.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    private companion object {
        private val REARM_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            // A package replace wipes pending alarms exactly like a reboot does, and nothing else
            // re-arms them — so before this, every app update silently disarmed every reminder.
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in REARM_ACTIONS) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                rescheduleAllReminders(appContext, AppDatabase.getDatabase(appContext))
            } catch (e: Exception) {
                Log.w("ReminderBootReceiver", "Failed to re-arm reminder alarms after ${intent.action}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
