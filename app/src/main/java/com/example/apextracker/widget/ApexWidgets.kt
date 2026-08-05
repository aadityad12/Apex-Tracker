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
