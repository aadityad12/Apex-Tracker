package com.example.apextracker.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll

/**
 * Pushes fresh goal data to the streak and goals widgets (Issues #130/#131). Kept separate from
 * [refreshBudgetWidget]: Glance schedules asynchronous sessions per provider, and firing unrelated
 * providers together can coalesce away the update that actually has changed data.
 */
suspend fun refreshApexWidgets(context: Context) {
    suspend fun refresh(name: String, update: suspend () -> Unit) {
        try {
            update()
        } catch (e: Exception) {
            Log.w("ApexWidgets", "$name widget refresh failed", e)
        }
    }
    refresh("streak") { StreakWidget().updateAll(context) }
    refresh("goals") { GoalsWidget().updateAll(context) }
}

/** Immediately refreshes the Budget provider after its Room/DataStore source changes (#167). */
suspend fun refreshBudgetWidget(context: Context) {
    try {
        BudgetWidget().updateAll(context)
    } catch (e: Exception) {
        Log.w("ApexWidgets", "budget widget refresh failed", e)
    }
}

/**
 * Immediately refreshes the "today at a glance" provider (#44) after any of its three sources
 * changes. Called from the study timer, the screen-time poll and reminder mutations rather than
 * left to the 30-minute `updatePeriodMillis`, which is far too coarse for a tile whose whole point
 * is the current state of the day.
 */
suspend fun refreshTodayWidget(context: Context) {
    try {
        TodayWidget().updateAll(context)
    } catch (e: Exception) {
        Log.w("ApexWidgets", "today widget refresh failed", e)
    }
}

/**
 * Immediately refreshes the study timer provider (#132) after the stopwatch starts, pauses or
 * banks time. Its own toggle button refreshes itself; this is for changes made inside the app.
 */
suspend fun refreshStudyWidget(context: Context) {
    try {
        StudyWidget().updateAll(context)
    } catch (e: Exception) {
        Log.w("ApexWidgets", "study widget refresh failed", e)
    }
}
