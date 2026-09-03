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
    val onboardingDismissed: Boolean = false,
    /** Last day the recommendations request ran (#150) — tracked apart from topic search so one
     *  source going quiet doesn't stop the other from trying. */
    val lastRecommendationDate: LocalDate? = null
)

class PapersDiscoveryPrefs(private val context: Context) {
    private val lastFetchDateKey = stringPreferencesKey("last_fetch_date")
    private val blockedUntilKey = longPreferencesKey("blocked_until_millis")
    private val onboardingDismissedKey = booleanPreferencesKey("onboarding_dismissed")
    private val lastRecommendationDateKey = stringPreferencesKey("last_recommendation_date")

    val preferences: Flow<PapersDiscoveryPreferences> = context.papersDiscoveryDataStore.data.map { prefs ->
        PapersDiscoveryPreferences(
            lastFetchDate = prefs[lastFetchDateKey]?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            },
            blockedUntilMillis = prefs[blockedUntilKey] ?: 0L,
            onboardingDismissed = prefs[onboardingDismissedKey] ?: false,
            lastRecommendationDate = prefs[lastRecommendationDateKey]?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            }
        )
    }

    suspend fun recordAttempt(date: LocalDate, blockedUntilMillis: Long = 0L) {
        context.papersDiscoveryDataStore.edit { prefs ->
            prefs[lastFetchDateKey] = date.toString()
            if (blockedUntilMillis > 0L) prefs[blockedUntilKey] = blockedUntilMillis
            else prefs.remove(blockedUntilKey)
        }
    }

    suspend fun recordRecommendationAttempt(date: LocalDate, blockedUntilMillis: Long = 0L) {
        context.papersDiscoveryDataStore.edit { prefs ->
            prefs[lastRecommendationDateKey] = date.toString()
            // The 429 window is deliberately shared with topic search: both draw on the same
            // unauthenticated pool, so a backoff earned by one has to apply to the other.
            if (blockedUntilMillis > 0L) prefs[blockedUntilKey] = blockedUntilMillis
        }
    }

    /**
     * Record a 429 window without touching either day marker.
     *
     * The manual add path shares this pool, so a rate limit it earns has to slow discovery down
     * too — but a user tapping "Look up" must not consume today's *discovery* attempt, which is
     * what [recordAttempt] and [recordRecommendationAttempt] would do by stamping their dates.
     */
    suspend fun recordRateLimit(blockedUntilMillis: Long) {
        if (blockedUntilMillis <= 0L) return
        context.papersDiscoveryDataStore.edit { prefs ->
            prefs[blockedUntilKey] = blockedUntilMillis
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

/**
 * The same gate for recommendations (#150), on its own day marker but the shared backoff window.
 * Whether there is enough rated history to ask at all is [canRecommend]'s decision, not this one.
 */
fun shouldFetchRecommendations(
    preferences: PapersDiscoveryPreferences,
    today: LocalDate,
    nowMillis: Long
): Boolean =
    preferences.lastRecommendationDate != today && nowMillis >= preferences.blockedUntilMillis
