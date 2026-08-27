package com.example.apextracker

import java.time.LocalDate

/**
 * One parsed row from a budget CSV import (Issue #219). [error] is non-null when the row couldn't
 * be turned into a [BudgetItem] — an import never silently drops or half-applies a row, it either
 * imports it or reports why it didn't, and the caller decides whether to proceed with the rest.
 */
data class BudgetCsvImportRow(
    val lineNumber: Int,
    val date: LocalDate?,
    val title: String,
    val amount: Double?,
    val type: String,
    val categoryName: String,
    val description: String?,
    val error: String?
) {
    val isValid: Boolean get() = error == null
}

/** The full result of parsing a CSV file: every row, valid or not. */
data class BudgetCsvImportResult(val rows: List<BudgetCsvImportRow>) {
    val valid: List<BudgetCsvImportRow> get() = rows.filter { it.isValid }
    val invalid: List<BudgetCsvImportRow> get() = rows.filter { !it.isValid }
}

/**
 * Splits raw CSV text into rows of fields, honoring RFC 4180 quoting. A quoted field can contain
 * commas, doubled quotes, and literal newlines — the same reason [csvEscape] quotes them on the
 * way out — so a naive per-line `split(",")`/`split("\n")` would break on any file it produced
 * containing a comma or newline inside a title or description.
 */
internal fun parseCsvRows(csv: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    var field = StringBuilder()
    var row = mutableListOf<String>()
    var inQuotes = false
    var i = 0
    while (i < csv.length) {
        val c = csv[i]
        when {
            inQuotes -> when {
                c == '"' && i + 1 < csv.length && csv[i + 1] == '"' -> {
                    field.append('"')
                    i++
                }
                c == '"' -> inQuotes = false
                else -> field.append(c)
            }
            c == '"' -> inQuotes = true
            c == ',' -> {
                row.add(field.toString())
                field = StringBuilder()
            }
            c == '\r' -> Unit // swallowed; \n (or end of input) closes the row
            c == '\n' -> {
                row.add(field.toString())
                rows.add(row)
                row = mutableListOf()
                field = StringBuilder()
            }
            else -> field.append(c)
        }
        i++
    }
    // A final row with no trailing newline, or a genuinely non-empty last field.
    if (field.isNotEmpty() || row.isNotEmpty()) {
        row.add(field.toString())
        rows.add(row)
    }
    // Blank lines (a lone empty field) are formatting, not data — export never writes one, but a
    // hand-edited or spreadsheet-resaved file can pick up a trailing blank line.
    return rows.filterNot { it.size == 1 && it[0].isBlank() }
}

/** Reverses [csvEscape]'s formula-neutralizer prefix so re-importing an exported file round-trips. */
private fun unescapeFormulaNeutralizer(field: String): String =
    if (field.length > 1 && field[0] == '\'' && field[1] in CSV_FORMULA_TRIGGERS) field.substring(1) else field

/**
 * Parses CSV text in the app's own export format (`date,title,amount,type,category,description`,
 * see [buildBudgetCsv]) into rows ready to import. Deliberately scoped to round-tripping this
 * app's own format rather than an arbitrary bank statement — column order and the header row are
 * both assumed fixed, matching the issue's suggested "start with our own format, expand later"
 * scope. The first row is always treated as a header and skipped, since every file this produces
 * has one.
 *
 * A category is resolved to a real [Category] later, once the caller has the live list — this
 * function only reports the raw name, so it stays pure and framework-free.
 */
fun parseBudgetCsv(csv: String): BudgetCsvImportResult {
    val allRows = parseCsvRows(csv)
    if (allRows.isEmpty()) return BudgetCsvImportResult(emptyList())

    val dataRows = allRows.drop(1)
    return BudgetCsvImportResult(
        dataRows.mapIndexed { index, fields ->
            // 1-indexed for a human-readable message, plus the header row this file always has.
            parseBudgetCsvRow(lineNumber = index + 2, fields = fields)
        }
    )
}

private fun parseBudgetCsvRow(lineNumber: Int, fields: List<String>): BudgetCsvImportRow {
    val dateText = fields.getOrElse(0) { "" }
    val title = unescapeFormulaNeutralizer(fields.getOrElse(1) { "" }).trim()
    val amountText = fields.getOrElse(2) { "" }
    val typeText = fields.getOrElse(3) { "" }.trim().uppercase()
    val categoryName = unescapeFormulaNeutralizer(fields.getOrElse(4) { "" }).trim()
    val description = unescapeFormulaNeutralizer(fields.getOrElse(5) { "" }).trim()

    val date = runCatching { LocalDate.parse(dateText.trim()) }.getOrNull()
    val amount = amountText.trim().toDoubleOrNull()
    // Anything other than a recognized INCOME marker reads as EXPENSE (Issue #218's own default),
    // rather than rejecting the row over an unrecognized/blank type column.
    val type = if (typeText == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE

    val error = when {
        fields.size < 3 -> "Too few columns"
        date == null -> "Unreadable date \"$dateText\""
        title.isBlank() -> "Missing title"
        amount == null || !amount.isFinite() -> "Unreadable amount \"$amountText\""
        else -> null
    }

    return BudgetCsvImportRow(
        lineNumber = lineNumber,
        date = date,
        title = title,
        amount = amount,
        type = type,
        categoryName = categoryName,
        description = description.ifBlank { null },
        error = error
    )
}

/**
 * Matches an imported row's free-text category name against the live category list, case
 * insensitively. No match (including the "Subscriptions" label a previous export may have
 * written for a synthetic -1L row) leaves the item uncategorized rather than guessing — a
 * literal "Subscriptions" is only ever wired up correctly when the user has a real category by
 * that name.
 */
fun resolveImportedCategoryId(categoryName: String, categories: List<Category>): Long? =
    categoryName.takeIf { it.isNotBlank() }
        ?.let { name -> categories.find { it.name.equals(name, ignoreCase = true) } }
        ?.id
