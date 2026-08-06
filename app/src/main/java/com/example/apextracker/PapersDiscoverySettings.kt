package com.example.apextracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

val Context.papersDiscoveryDataStore: DataStore<Preferences> by preferencesDataStore(name = "papers_discovery")

/** Canonical Semantic Scholar fieldsOfStudy values exposed as Papers topic chips. */
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

data class PapersDiscoveryPreferences(
    val fields: Set<String> = emptySet(),
    val lastFetchDate: LocalDate? = null,
    val blockedUntilMillis: Long = 0L
)

class PapersDiscoveryPrefs(private val context: Context) {
    private val fieldsKey = stringSetPreferencesKey("fields")
    private val lastFetchDateKey = stringPreferencesKey("last_fetch_date")
    private val blockedUntilKey = longPreferencesKey("blocked_until_millis")

    val preferences: Flow<PapersDiscoveryPreferences> = context.papersDiscoveryDataStore.data.map { prefs ->
        PapersDiscoveryPreferences(
            fields = prefs[fieldsKey].orEmpty().filterTo(linkedSetOf()) { it in PAPER_DISCOVERY_FIELDS },
            lastFetchDate = prefs[lastFetchDateKey]?.let { value ->
                runCatching { LocalDate.parse(value) }.getOrNull()
            },
            blockedUntilMillis = prefs[blockedUntilKey] ?: 0L
        )
    }

    suspend fun setFields(fields: Set<String>) {
        val valid = fields.filterTo(linkedSetOf()) { it in PAPER_DISCOVERY_FIELDS }
        context.papersDiscoveryDataStore.edit { prefs ->
            val changed = prefs[fieldsKey].orEmpty() != valid
            prefs[fieldsKey] = valid
            // A changed selection gets one immediate fetch, independent of the previous topic's run.
            if (changed) prefs.remove(lastFetchDateKey)
        }
    }

    suspend fun recordAttempt(date: LocalDate, blockedUntilMillis: Long = 0L) {
        context.papersDiscoveryDataStore.edit { prefs ->
            prefs[lastFetchDateKey] = date.toString()
            if (blockedUntilMillis > 0L) prefs[blockedUntilKey] = blockedUntilMillis
            else prefs.remove(blockedUntilKey)
        }
    }
}

/** One request per date. A stored 429 window is shared with the later recommendations feed. */
fun shouldFetchDailyPapers(
    preferences: PapersDiscoveryPreferences,
    today: LocalDate,
    nowMillis: Long
): Boolean = preferences.fields.isNotEmpty() &&
    preferences.lastFetchDate != today &&
    nowMillis >= preferences.blockedUntilMillis

/** Rotate deterministically through selected fields so daily discovery stays to one API request. */
fun dailyPaperField(fields: Set<String>, date: LocalDate): String? {
    val ordered = fields.sorted()
    if (ordered.isEmpty()) return null
    return ordered[(date.toEpochDay().mod(ordered.size.toLong())).toInt()]
}
