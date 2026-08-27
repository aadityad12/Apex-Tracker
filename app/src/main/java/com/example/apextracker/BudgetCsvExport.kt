package com.example.apextracker

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.time.format.DateTimeFormatter

/**
 * Characters that make Excel / Sheets / LibreOffice treat a cell as a formula rather than text.
 * Quoting does not help — a spreadsheet evaluates `"=1+1"` from a quoted CSV field just the same.
 */
private val CSV_FORMULA_TRIGGERS = charArrayOf('=', '+', '-', '@', '\t', '\r')

/**
 * RFC 4180 quoting, plus a leading apostrophe on anything a spreadsheet would evaluate.
 *
 * RFC 4180 says nothing about formulas, and the Papers export carries Semantic Scholar's own
 * title/author/venue strings — data nobody in this app typed or reviewed, inserted unattended by
 * the daily discovery fetch — into a file the user opens in Excel (Issue #192). The apostrophe is
 * the conventional neutralizer and survives a round-trip more readably than a leading tab.
 *
 * Numeric columns must NOT come through here: `-12.50` is a legitimate negative amount, and
 * prefixing it would break the column. `buildBudgetCsv` already writes `item.amount` unescaped —
 * keep it that way.
 */
internal fun csvEscape(field: String): String {
    val neutralized = if (field.isNotEmpty() && field[0] in CSV_FORMULA_TRIGGERS) "'$field" else field
    val needsQuoting = neutralized.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuoting) "\"" + neutralized.replace("\"", "\"\"") + "\"" else neutralized
}

/**
 * Same category-name resolution the Budget UI uses: -1L is the synthetic "Subscriptions" bucket,
 * else a lookup, else blank.
 *
 * The -1L label stays an untranslated literal here rather than moving to
 * `R.string.budget_category_subscriptions` with the UI call sites (Issue #65), for two reasons:
 * CSV is machine-readable output where a stable column value beats a localized one, and taking a
 * Context would cost this function its purity — it's unit-tested in `BudgetCsvExportTest` with no
 * framework. If the export ever should follow the UI's locale, pass the resolved name in as a
 * parameter rather than reaching for a Context in here.
 */
internal fun resolveCategoryName(categoryId: Long?, categories: List<Category>): String = when (categoryId) {
    -1L -> "Subscriptions"
    null -> ""
    else -> categories.find { it.id == categoryId }?.name ?: ""
}

/** Pure CSV builder: `date,title,amount,type,category,description`, one row per item, RFC 4180 quoted. */
fun buildBudgetCsv(items: List<BudgetItem>, categories: List<Category>): String {
    val dateFormat = DateTimeFormatter.ISO_LOCAL_DATE
    val header = "date,title,amount,type,category,description"
    val rows = items.map { item ->
        listOf(
            item.date.format(dateFormat),
            csvEscape(item.title),
            item.amount.toString(),
            item.type,
            csvEscape(resolveCategoryName(item.categoryId, categories)),
            csvEscape(item.description ?: "")
        ).joinToString(",")
    }
    return (listOf(header) + rows).joinToString("\n")
}

/** Writes a generated file to app cache and shares it through FileProvider — no storage permission needed. */
fun shareFile(context: Context, contents: String, fileName: String, mimeType: String) {
    val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val file = File(exportDir, fileName)
    file.writeText(contents)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, fileName))
}

fun shareCsv(context: Context, csv: String, fileName: String) =
    shareFile(context, csv, fileName, "text/csv")
