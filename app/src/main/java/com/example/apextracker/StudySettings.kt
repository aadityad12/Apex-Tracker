package com.example.apextracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.studyDataStore: DataStore<Preferences> by preferencesDataStore(name = "study_settings")

/**
 * Study preferences (Issue #42) — currently just the daily study goal in minutes. Local-only, like
 * the other device settings: study sessions themselves sync, but the goal/streak are a per-device
 * target. 0 means "no goal" (streak/ring UI hidden). Default 60.
 */
class StudySettings(private val context: Context) {
    companion object {
        val DAILY_GOAL_MINUTES = intPreferencesKey("daily_goal_minutes")
        const val DEFAULT_GOAL_MINUTES = 60
        val SHOW_MEDIA_CONTROLS = booleanPreferencesKey("show_media_controls")
    }

    val dailyGoalMinutes: Flow<Int> = context.studyDataStore.data
        .map { prefs -> (prefs[DAILY_GOAL_MINUTES] ?: DEFAULT_GOAL_MINUTES).coerceAtLeast(0) }

    suspend fun setDailyGoalMinutes(minutes: Int) {
        context.studyDataStore.edit { prefs ->
            prefs[DAILY_GOAL_MINUTES] = minutes.coerceAtLeast(0)
        }
    }

    /**
     * Whether the focus surface carries the now-playing panel. On by default: with no notification
     * access granted the panel is a single quiet line, and defaulting it off would leave the feature
     * with nowhere to be discovered. Off removes it entirely — the focus surface is deliberately
     * bare, and someone who studies in silence should be able to keep it that way.
     */
    val showMediaControls: Flow<Boolean> = context.studyDataStore.data
        .map { prefs -> prefs[SHOW_MEDIA_CONTROLS] ?: true }

    suspend fun setShowMediaControls(enabled: Boolean) {
        context.studyDataStore.edit { prefs ->
            prefs[SHOW_MEDIA_CONTROLS] = enabled
        }
    }
}
