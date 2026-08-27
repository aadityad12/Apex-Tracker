package com.example.apextracker

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * BudgetItem.type values (Issue #218). Stored as a plain string, not a Room TypeConverter enum —
 * matches the convention set by GoalType/GoalCadence/GoalMetric in Goal.kt.
 */
object TransactionType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

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
    // TransactionType.*. Defaults to EXPENSE so every pre-#218 row (and every call site that
    // doesn't pass this explicitly) keeps its exact historical meaning.
    val type: String = TransactionType.EXPENSE,
    val cloudId: String = "",
    val modifiedAt: Long = 0L
)

/** False only for an explicit income row — any other/legacy/malformed value reads as spend. */
val BudgetItem.isExpense: Boolean get() = type != TransactionType.INCOME

/**
 * The single filter every "spending" total in the app goes through — the pie chart, trend chart,
 * category/overall limits, the Dashboard SPEND goal metric, the Overview stat, the calendar day
 * totals, and the Budget widget snapshot. Income rows have no category and aren't a cap to blow,
 * so they're excluded rather than netted in; net balance is computed separately (BudgetOverview).
 */
fun List<BudgetItem>.expensesOnly(): List<BudgetItem> = filter { it.isExpense }
