package com.example.apextracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.apexTipDataStore: DataStore<Preferences> by preferencesDataStore(name = "daily_apex_tip")

data class ApexTipPreferences(
    val enabled: Boolean = false,
    val tipDate: LocalDate? = null,
    val tipText: String? = null,
    val lastAttemptDate: LocalDate? = null
)

/** Device-local consent, request throttle, and the one cached response for the current day. */
class ApexTipSettings(private val context: Context) {
    private companion object {
        val ENABLED = booleanPreferencesKey("enabled")
        val TIP_DATE = stringPreferencesKey("tip_date")
        val TIP_TEXT = stringPreferencesKey("tip_text")
        val LAST_ATTEMPT_DATE = stringPreferencesKey("last_attempt_date")
    }

    val preferences: Flow<ApexTipPreferences> = context.apexTipDataStore.data.map { prefs ->
        ApexTipPreferences(
            enabled = prefs[ENABLED] ?: false,
            tipDate = prefs[TIP_DATE]?.let(::parseDateOrNull),
            tipText = prefs[TIP_TEXT]?.takeIf(String::isNotBlank),
            lastAttemptDate = prefs[LAST_ATTEMPT_DATE]?.let(::parseDateOrNull)
        )
    }

    suspend fun setEnabled(enabled: Boolean) {
        context.apexTipDataStore.edit { it[ENABLED] = enabled }
    }

    suspend fun markAttempt(date: LocalDate) {
        context.apexTipDataStore.edit { it[LAST_ATTEMPT_DATE] = date.toString() }
    }

    suspend fun saveTip(date: LocalDate, text: String) {
        context.apexTipDataStore.edit {
            it[TIP_DATE] = date.toString()
            it[TIP_TEXT] = text
        }
    }
}

private fun parseDateOrNull(value: String): LocalDate? =
    runCatching { LocalDate.parse(value) }.getOrNull()
