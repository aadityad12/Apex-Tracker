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
    // Diagnostic only — records which Room version the data came from. Must be bumped alongside
    // the @Database version in AppDatabase.kt; an on-device export caught it still reading 22
    // after the v23 index migration (Issue #197). Not load-bearing, which is exactly why it
    // rots quietly.
    val appDbVersion: Int = 26,
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
    val papers: List<Paper> = emptyList(),
    // Absent in pre-discovery-redesign backups; Gson leaves the default, so old files restore cleanly.
    val paperTopics: List<PaperTopic> = emptyList(),
    // Absent in pre-linking backups (Issue #223); Gson leaves the default, so old files restore cleanly.
    val paperLinks: List<PaperLink> = emptyList()
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
        "excludedApps", "reminders", "notes", "goals", "goalCompletions", "appUsageLimits", "papers",
        "paperTopics", "paperLinks"
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
    // Issue #218: absent on every pre-income backup, which are all expenses by definition.
    objectRoot.getAsJsonArray("budgetItems").forEach { element ->
        val budgetItem = element.asJsonObject
        val type = budgetItem.get("type")
            ?.takeUnless { it.isJsonNull }
            ?.asString
        if (type != TransactionType.EXPENSE && type != TransactionType.INCOME) {
            budgetItem.addProperty("type", TransactionType.EXPENSE)
        }
    }
    // Issue #232: backupGson() has no Kotlin-constructor support (see the class doc above), so a
    // field absent from an older backup deserializes to a raw null rather than the entity's
    // Kotlin default — crashing Room's insert for a non-null-typed field. topicCloudId was added
    // to Paper on 2026-08-07, eight days after Papers backup support shipped (2026-07-30), so any
    // backup exported in that window is missing it. Every entity field added after its table
    // joined BackupData needs the same treatment as cadence/type above — this is the one other
    // gap found by audit; a new one added the same way this one was (a later commit adding a
    // non-null field to an already-backed-up entity) needs a matching normalization step here.
    objectRoot.getAsJsonArray("papers").forEach { element ->
        val paper = element.asJsonObject
        if (paper.get("topicCloudId")?.takeUnless { it.isJsonNull } == null) {
            paper.addProperty("topicCloudId", "")
        }
    }

    // Attachment names are resolved as paths under the note-attachments directory, so a
    // hand-edited backup could point them at anything in the sandbox — including the Room DB,
    // which deleting the note would then delete (Issue #193). Strip them here, at the boundary,
    // rather than relying on every consumer to be careful.
    objectRoot.getAsJsonArray("notes").forEach { element ->
        val note = element.asJsonObject
        val attachments = note.get("attachments")?.takeUnless { it.isJsonNull }?.asString ?: return@forEach
        val safe = sanitizeAttachments(attachments)
        if (safe != attachments) note.addProperty("attachments", safe)
    }
    return backupGson().fromJson(objectRoot, BackupData::class.java)
}
