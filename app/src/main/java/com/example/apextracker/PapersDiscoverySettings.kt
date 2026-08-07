package com.example.apextracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.papersDiscoveryDataStore: DataStore<Preferences> by preferencesDataStore(name = "papers_discovery")

/** Canonical Semantic Scholar fieldsOfStudy values — a PaperTopic narrows one of these with a keyword. */
val PAPER_DISCOVERY_FIELDS = listOf(
    "Computer Science",
    "Mathematics",
    "Physics",
    "Engineering",
    "Biology",
    "Medicine",
    "Economics",
    "Psychology"
)

val PAPER_DISCOVERY_FIELD_LABELS = mapOf(
    "Computer Science" to R.string.papers_field_computer_science,
    "Mathematics" to R.string.papers_field_mathematics,
    "Physics" to R.string.papers_field_physics,
    "Engineering" to R.string.papers_field_engineering,
    "Biology" to R.string.papers_field_biology,
    "Medicine" to R.string.papers_field_medicine,
    "Economics" to R.string.papers_field_economics,
    "Psychology" to R.string.papers_field_psychology
)

/**
 * Whole-day bookkeeping only — per-topic state (which topic, when it was last checked, its
 * engagement track record) lives on [PaperTopic] rows in Room, not here. This just gates "have we
 * already run today's fetch routine" and carries the shared 429 backoff window.
 */
data class PapersDiscoveryPreferences(
    val lastFetchDate: LocalDate? = null,
    val blockedUntilMillis: Long = 0L,
    val onboardingDismissed: Boolean = false
)

class PapersDiscoveryPrefs(private val context: Context) {
    private val lastFetchDateKey = stringPreferencesKey("last_fetch_date")
    private val blockedUntilKey = longPreferencesKey("blocked_until_millis")
    private val onboardingDismissedKey = booleanPreferencesKey("onboarding_dismissed")

    val preferences: Flow<PapersDiscoveryPreferences> = context.papersDiscoveryDataStore.data.map { prefs ->
        PapersDiscoveryPreferences(
            lastFetchDate = prefs[lastFetchDateKey]?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            },
            blockedUntilMillis = prefs[blockedUntilKey] ?: 0L,
            onboardingDismissed = prefs[onboardingDismissedKey] ?: false
        )
    }

    suspend fun recordAttempt(date: LocalDate, blockedUntilMillis: Long = 0L) {
        context.papersDiscoveryDataStore.edit { prefs ->
            prefs[lastFetchDateKey] = date.toString()
            if (blockedUntilMillis > 0L) prefs[blockedUntilKey] = blockedUntilMillis
            else prefs.remove(blockedUntilKey)
        }
    }

    suspend fun dismissOnboarding() {
        context.papersDiscoveryDataStore.edit { prefs -> prefs[onboardingDismissedKey] = true }
    }
}

/**
 * The whole-day gate: at most one fetch routine per day, and never before a stored 429 window has
 * passed. Whether there's anything to actually check (active topics) is decided by the ViewModel
 * from Room, not here — this only knows about the day/backoff bookkeeping.
 */
fun shouldFetchDailyPapers(
    preferences: PapersDiscoveryPreferences,
    today: LocalDate,
    nowMillis: Long
): Boolean = preferences.lastFetchDate != today && nowMillis >= preferences.blockedUntilMillis
