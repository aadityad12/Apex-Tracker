package com.example.apextracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "ReminderAlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(ReminderScheduler.EXTRA_REMINDER_ID, 0)
        val reminderName = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_NAME)
            ?: context.getString(R.string.reminder_default_name)
        val reminderDescription = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_DESCRIPTION)
        val reminderPriority = intent.getStringExtra(ReminderScheduler.EXTRA_REMINDER_PRIORITY)

        val inputData = Data.Builder()
            .putString("reminder_name", reminderName)
            .putString("reminder_description", reminderDescription)
            .putLong("reminder_id", reminderId)
            .putString("reminder_priority", reminderPriority)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInputData(inputData)
            .build()

        // enqueue() returning does not mean the request is durably persisted yet — that happens
        // asynchronously on WorkManager's own executor. Without goAsync(), the OS can reclaim this
        // process in the narrow window before that write lands (plausible right after an
        // exact-alarm Doze wake-up), silently dropping the reminder with no retry (Issue #236).
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WorkManager.getInstance(appContext).enqueue(workRequest).result.get()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to enqueue reminder work for id=$reminderId", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
