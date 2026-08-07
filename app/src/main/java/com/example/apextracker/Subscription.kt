package com.example.apextracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// Sync join keys — see the MIGRATION_22_23 note in AppDatabase.kt (Issue #197).
@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["cloudId"])]
)
data class Subscription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val renewalDate: LocalDate,
    val notes: String? = null,
    val lastAddedDate: LocalDate? = null,
    /**
     * Paused subscriptions stop auto-generating BudgetItems (Issue #79) without losing their
     * name/notes/history. Resuming rolls `renewalDate` forward past the skipped periods rather
     * than back-filling them — see BudgetViewModel.setSubscriptionPaused.
     */
    val isPaused: Boolean = false,
    val cloudId: String = "",
    val modifiedAt: Long = 0L
)
