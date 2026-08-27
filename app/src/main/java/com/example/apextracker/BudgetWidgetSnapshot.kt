package com.example.apextracker

import kotlinx.coroutines.flow.first
import java.time.YearMonth

/**
 * Read-only data needed by the Budget home-screen widget (Issue #167).
 *
 * [locked] mirrors the Budget module lock (Issue #45). A home-screen widget renders outside the
 * app entirely — no Activity, no [LockGate], nothing to prompt with — so a widget showing exact
 * spending defeated the lock completely: the figures sat on the launcher for anyone to read
 * while the module they came from was nominally protected (Issue #187). When locked, the widget
 * shows its title and a tap-to-open prompt and nothing else; every figure is withheld at the
 * *snapshot* level, not merely hidden in the layout, so no amount ever reaches the widget process.
 */
data class BudgetWidgetSnapshot(
    val month: YearMonth,
    val spent: Double,
    val limitStatus: OverallLimitStatus?,
    val currencyCode: String,
    val locked: Boolean = false
)

/**
 * Builds the widget model using [overallLimitStatus], so the widget and the in-app spending-limit
 * card cannot drift into two different definitions of "this month" or "over budget".
 *
 * A [locked] snapshot zeroes the figures rather than carrying them behind a flag — the widget
 * host serialises this into the launcher's process, so "present but not drawn" would still put
 * the amounts somewhere the lock is supposed to keep them out of.
 */
fun budgetWidgetSnapshot(
    items: List<BudgetItem>,
    month: YearMonth,
    limit: Double?,
    currencyCode: String,
    locked: Boolean = false
): BudgetWidgetSnapshot {
    val resolvedCurrency = parseCurrencySafe(currencyCode)?.currencyCode ?: DEFAULT_CURRENCY_CODE
    if (locked) {
        return BudgetWidgetSnapshot(month, spent = 0.0, limitStatus = null, currencyCode = resolvedCurrency, locked = true)
    }
    // Income isn't spend (Issue #218) — excluded before either the capped or uncapped path sums it.
    val expenseItems = items.expensesOnly()
    val status = overallLimitStatus(expenseItems, month, limit)
    val spent = status?.spent
        ?: expenseItems.filter { YearMonth.from(it.date) == month }.sumOf { it.amount }
    return BudgetWidgetSnapshot(
        month = month,
        spent = spent,
        limitStatus = status,
        currencyCode = resolvedCurrency,
        locked = false
    )
}

/** Loads one consistent widget snapshot from Room and the relevant DataStore settings. */
suspend fun loadBudgetWidgetSnapshot(
    db: AppDatabase,
    budgetPrefs: BudgetPrefs,
    currencySettings: CurrencySettings,
    securitySettings: SecuritySettings,
    month: YearMonth = YearMonth.now()
): BudgetWidgetSnapshot = budgetWidgetSnapshot(
    items = db.budgetDao().getAllItemsOneShot(),
    month = month,
    limit = budgetPrefs.overallMonthlyLimit.first(),
    currencyCode = currencySettings.currencyCode.first(),
    locked = securitySettings.budgetLockEnabled.first()
)
