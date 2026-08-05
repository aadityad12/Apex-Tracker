package com.example.apextracker

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonDeserializer
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * A full-dataset backup (Issue #121): every Room entity, so an offline-only user can save a file
 * before uninstalling / switching phones and restore it later. Distinct from the CSV export
 * (budget-only, share-sheet) — this is the complete, re-importable dataset.
 *
 * [formatVersion] is the backup schema version (bump when this container's shape changes);
 * [appDbVersion] records the Room version the data came from, for diagnostics.
 */
data class BackupData(
    val formatVersion: Int = 2,
    val appDbVersion: Int = 21,
    val exportedAt: String = "",
    val budgetItems: List<BudgetItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val studySessions: List<StudySession> = emptyList(),
    val screenTimeSessions: List<ScreenTimeSession> = emptyList(),
    val excludedApps: List<ExcludedApp> = emptyList(),
    val reminders: List<Reminder> = emptyList(),
    val notes: List<Note> = emptyList(),
    val goals: List<Goal> = emptyList(),
    val goalCompletions: List<GoalCompletion> = emptyList(),
    val appUsageLimits: List<AppUsageLimit> = emptyList(),
    // Absent in pre-Papers backups; Gson leaves the default, so old files restore cleanly.
    val papers: List<Paper> = emptyList()
)

/**
 * Gson configured for the entities' java.time fields (ISO strings). The Room [Converters] only
 * apply inside Room, so a plain Gson would emit broken java.time objects — these adapters keep the
 * backup human-readable and stable. Enums (RecurrenceFrequency, GoalType, DayOfWeek, …) serialize
 * by name via Gson's defaults, which is exactly what we want.
 */
fun backupGson(): Gson = GsonBuilder()
    .serializeNulls()
    .registerTypeAdapter(LocalDate::class.java, JsonSerializer<LocalDate> { src, _, _ -> JsonPrimitive(src.toString()) })
    .registerTypeAdapter(LocalDate::class.java, JsonDeserializer { json, _, _ -> LocalDate.parse(json.asString) })
    .registerTypeAdapter(LocalDateTime::class.java, JsonSerializer<LocalDateTime> { src, _, _ -> JsonPrimitive(src.toString()) })
    .registerTypeAdapter(LocalDateTime::class.java, JsonDeserializer { json, _, _ -> LocalDateTime.parse(json.asString) })
    .registerTypeAdapter(LocalTime::class.java, JsonSerializer<LocalTime> { src, _, _ -> JsonPrimitive(src.toString()) })
    .registerTypeAdapter(LocalTime::class.java, JsonDeserializer { json, _, _ -> LocalTime.parse(json.asString) })
    .create()

fun buildBackupJson(data: BackupData): String = backupGson().toJson(data)

/**
 * Parses a backup file. Throws (Gson/date exceptions) on malformed input; the caller surfaces that
 * as a failed restore rather than corrupting the DB. A null result (Gson can return null for the
 * literal "null") is normalized to an error by the caller.
 */
fun parseBackupJson(json: String): BackupData? {
    val root = JsonParser.parseString(json)
    if (root.isJsonNull) return null
    val objectRoot = root.asJsonObject

    // Gson does not run Kotlin default arguments for absent fields. Normalize older backups before
    // constructing entities so newly-added collections stay empty and pre-#166 goals stay DAILY.
    listOf(
        "budgetItems", "categories", "subscriptions", "studySessions", "screenTimeSessions",
        "excludedApps", "reminders", "notes", "goals", "goalCompletions", "appUsageLimits", "papers"
    ).forEach { field ->
        if (!objectRoot.has(field) || objectRoot.get(field).isJsonNull) objectRoot.add(field, JsonArray())
    }
    objectRoot.getAsJsonArray("goals").forEach { element ->
        val goal = element.asJsonObject
        val cadence = goal.get("cadence")
            ?.takeUnless { it.isJsonNull }
            ?.asString
        if (cadence != GoalCadence.DAILY && cadence != GoalCadence.WEEKLY) {
            goal.addProperty("cadence", GoalCadence.DAILY)
        }
    }
    return backupGson().fromJson(objectRoot, BackupData::class.java)
}
