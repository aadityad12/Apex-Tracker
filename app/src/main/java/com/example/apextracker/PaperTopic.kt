package com.example.apextracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A user-defined discovery interest: a [field] (one of [PAPER_DISCOVERY_FIELDS]) narrowed by a
 * required [keyword] — the actual Semantic Scholar search text (see SemanticScholar.kt). Replaces
 * the old bare-field rotation; multiple topics can share a field.
 *
 * [lastCheckedDate] drives the guaranteed-coverage fetch slot (PapersDiscoveryScoring.kt); the
 * read/abandon/rating counters drive [topicEngagementScore], which both weights the bonus fetch
 * slots and weights which queue item becomes "today's pick" (see dailyPick in PapersLogic.kt).
 * [consecutiveAbandons] resets to 0 on a READ and trips the mute-suggestion prompt at 3.
 * [pausedAt] (non-null = muted) excludes a topic from fetch rotation and pick-weighting without
 * losing its history — reversible, per the explicit decision not to delete on mute.
 */
// Sync join keys — see the MIGRATION_22_23 note in AppDatabase.kt (Issue #197).
@Entity(
    tableName = "paper_topics",
    indices = [Index(value = ["cloudId"])]
)
data class PaperTopic(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val field: String,
    val keyword: String,
    val pausedAt: LocalDate? = null,
    val createdDate: LocalDate = LocalDate.now(),
    val lastCheckedDate: LocalDate? = null,
    val readCount: Int = 0,
    val abandonedCount: Int = 0,
    val ratingSum: Int = 0,
    val ratingCount: Int = 0,
    val consecutiveAbandons: Int = 0,
    val cloudId: String = "",
    val modifiedAt: Long = 0L
)

/** Max active-or-paused topics a user can hold at once (Q10: keeps the guarantee window sane). */
const val MAX_PAPER_TOPICS = 8
