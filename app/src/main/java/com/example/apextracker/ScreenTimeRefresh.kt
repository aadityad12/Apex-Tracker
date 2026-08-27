package com.example.apextracker

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar

private const val TAG = "ScreenTimeRefresh"

/**
 * True if this app currently holds Usage Access — the same `AppOpsManager` check
 * `ScreenTimeViewModel.checkPermission()` uses, exposed statelessly so a caller with no live
 * `ScreenTimeViewModel` (Overview, Issue #209) can gate a `UsageStatsManager` query without one.
 */
fun hasUsageAccess(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    } else {
        @Suppress("DEPRECATION")
        appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
    }
    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * Today's per-package foreground time from `UsageStatsManager`. Extracted from
 * `ScreenTimeViewModel.calculateAppSpecificUsage()` — that function now delegates here — so a
 * caller without a live `ScreenTimeViewModel` instance can run the same live query.
 */
suspend fun calculateTodayAppUsage(context: Context): Map<String, Long> = withContext(Dispatchers.IO) {
    val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val startTime = calendar.timeInMillis
    val endTime = System.currentTimeMillis()

    // Same as ScreenTimeViewModel: usage access can be revoked mid-session, which makes
    // queryEvents throw from the binder rather than return nothing.
    val usageEvents = try {
        usageStatsManager.queryEvents(startTime, endTime)
    } catch (e: SecurityException) {
        Log.w(TAG, "Usage access unavailable; reporting no app usage", e)
        return@withContext emptyMap()
    }
    val event = UsageEvents.Event()

    val events = mutableListOf<ForegroundEvent>()
    while (usageEvents.hasNextEvent()) {
        usageEvents.getNextEvent(event)
        val kind = when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> ForegroundEventKind.RESUMED
            UsageEvents.Event.ACTIVITY_PAUSED, UsageEvents.Event.ACTIVITY_STOPPED -> ForegroundEventKind.PAUSED
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                    event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE
                ) {
                    ForegroundEventKind.SCREEN_OFF
                } else {
                    null
                }
            }
        }
        if (kind != null) {
            events.add(ForegroundEvent(kind, event.packageName ?: "", event.timeStamp))
        }
    }

    aggregateForegroundDurations(events, startTime, endTime)
}

/**
 * Recomputes today's screen time live and writes it to Room — the same total
 * `ScreenTimeViewModel.updateScreenTime()` computes (excluded apps, this app itself, the
 * launcher, systemui), minus the `installedApps`-based restriction, which only narrows the set
 * further and which a caller outside `ScreenTimeViewModel` has no cheap way to load; omitting it
 * matches the *existing* fallback `updateScreenTime()` itself already uses while that list is
 * still loading (Issue #159's comment there).
 *
 * Exists because Room's `screen_time_sessions` row for today is only as fresh as the last write
 * from `ScreenTimeViewModel`'s own 30-second loop, which runs only while that screen has been
 * open recently — so a screen that reads today's screen time from Room instead (Overview, Issue
 * #209) could show 0m or a stale figure on a day the Screen Time screen hasn't been opened yet.
 * Every other reader of that table shares this same Room-backed table, so writing a fresher value
 * here benefits them too; a screen that also needs the installedApps-restricted figure should
 * still prefer a live `ScreenTimeViewModel`.
 *
 * A no-op without Usage Access, so a device that never granted it doesn't overwrite whatever's
 * already in Room (possibly nothing, possibly a real number from before permission was revoked)
 * with a false zero — the same permission gate `ScreenTimeViewModel`'s own loop applies via
 * `_hasPermission`.
 */
suspend fun refreshTodayScreenTime(db: AppDatabase, context: Context): Unit = withContext(Dispatchers.IO) {
    if (!hasUsageAccess(context)) return@withContext

    val usageMap = calculateTodayAppUsage(context)
    val excludedPackageNames = db.excludedAppDao().getAllExcludedAppsOneShot().map { it.packageName }.toSet()
    val myPackageName = context.packageName
    val launcherPackage = try {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName
    } catch (e: Exception) {
        null
    }

    val totalFilteredTime = usageMap.filter { (pkg, _) ->
        pkg !in excludedPackageNames &&
            pkg != myPackageName &&
            pkg != launcherPackage &&
            pkg != "com.android.systemui"
    }.values.sum()

    db.screenTimeSessionDao().insertSession(ScreenTimeSession(date = LocalDate.now(), durationMillis = totalFilteredTime))
}
