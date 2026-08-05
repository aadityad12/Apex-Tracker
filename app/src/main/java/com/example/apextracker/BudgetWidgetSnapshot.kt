package com.example.apextracker

import kotlinx.coroutines.flow.first
import java.time.YearMonth

/** Read-only data needed by the Budget home-screen widget (Issue #167). */
data class BudgetWidgetSnapshot(
    val month: YearMonth,
    val spent: Double,
    val limitStatus: OverallLimitStatus?,
    val currencyCode: String
)

/**
 * Builds the widget model using [overallLimitStatus], so the widget and the in-app spending-limit
 * card cannot drift into two different definitions of "this month" or "over budget".
 */
fun budgetWidgetSnapshot(
    items: List<BudgetItem>,
    month: YearMonth,
    limit: Double?,
    currencyCode: String
): BudgetWidgetSnapshot {
    val status = overallLimitStatus(items, month, limit)
    val spent = status?.spent
        ?: items.filter { YearMonth.from(it.date) == month }.sumOf { it.amount }
    return BudgetWidgetSnapshot(
        month = month,
        spent = spent,
        limitStatus = status,
        currencyCode = parseCurrencySafe(currencyCode)?.currencyCode ?: DEFAULT_CURRENCY_CODE
    )
}

/** Loads one consistent widget snapshot from Room and the two relevant DataStore settings. */
suspend fun loadBudgetWidgetSnapshot(
    db: AppDatabase,
    budgetPrefs: BudgetPrefs,
    currencySettings: CurrencySettings,
    month: YearMonth = YearMonth.now()
): BudgetWidgetSnapshot = budgetWidgetSnapshot(
    items = db.budgetDao().getAllItemsOneShot(),
    month = month,
    limit = budgetPrefs.overallMonthlyLimit.first(),
    currencyCode = currencySettings.currencyCode.first()
)
