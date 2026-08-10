package com.example.apextracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

// Sync join keys — see the MIGRATION_22_23 note in AppDatabase.kt (Issue #197).
@Entity(
    tableName = "budget_items",
    indices = [Index(value = ["cloudId"])]
)
data class BudgetItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val description: String? = null,
    val date: LocalDate = LocalDate.now(),
    val categoryId: Long? = null,
    val cloudId: String = "",
    val modifiedAt: Long = 0L
)
