package com.example.apextracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * An undirected "these two papers relate" link (Issue #223), deferred in Plan.md's settled
 * decisions with "under ~50 papers, memory does the linking." One row represents the
 * relationship in both directions — there is no separate "A links to B" vs. "B links to A"; the
 * pure helpers in `PaperLinks.kt` treat [paperCloudId]/[relatedPaperCloudId] as an unordered pair.
 *
 * Keyed on cloudId, not local Room [id]s — a local autoincrement id means nothing across devices,
 * same reasoning as [Paper.topicCloudId] and `resolveReminderParentLinks`. Every [Paper] is
 * assigned a cloudId at creation time (not deferred to sync), so this join key is always
 * resolvable even offline/signed-out.
 */
// Sync join keys — see the MIGRATION_22_23 note in AppDatabase.kt (Issue #197).
@Entity(
    tableName = "paper_links",
    indices = [Index(value = ["cloudId"]), Index(value = ["paperCloudId"]), Index(value = ["relatedPaperCloudId"])]
)
data class PaperLink(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val paperCloudId: String,
    val relatedPaperCloudId: String,
    val createdDate: LocalDate = LocalDate.now(),
    val cloudId: String = "",
    val modifiedAt: Long = 0L
)
