package com.example.apextracker

import java.time.DateTimeException
import java.time.LocalDate

/**
 * Turning the text ML Kit read off a receipt photo into something the add-item dialog can prefill
 * (Issue #46). Pure and Android-free, so the guessing — which is the whole of this feature — is
 * unit-tested rather than eyeballed against one photo.
 *
 * Everything here is a *guess*. Nothing is ever saved without the user confirming it; a wrong
 * amount that the user corrects is a much better outcome than no amount at all, so these
 * heuristics lean towards offering candidates rather than towards being sure.
 */

/**
 * One piece of text OCR found, with where it sat on the page. Only what [reflowReceiptLines]
 * needs — deliberately not ML Kit's own type, so the reflow stays pure and testable.
 */
data class ReceiptTextElement(
    val text: String,
    val top: Int,
    val bottom: Int,
    val left: Int
)

/**
 * Rebuilds the receipt's visual rows from OCR output.
 *
 * This is load-bearing, and it is the thing a naive implementation gets wrong. A receipt is two
 * columns — labels on the left, amounts on the right — and text recognition groups by *block*, so
 * it happily returns every label as one block and every amount as another. Reading its plain text
 * output then gives you `TOTAL` and `8.43` on different lines, at which point no amount is ever
 * attributable to a label and the parser is reduced to guessing at bare numbers. (Measured, not
 * theorised: on a real scan this made "123 Market St" outrank the actual total.)
 *
 * So elements are regrouped by vertical position: anything whose vertical midpoint falls within
 * [ROW_TOLERANCE] of the row's is the same row, and within a row they are ordered left to right.
 * The tolerance scales with text height rather than being a fixed pixel count, because the same
 * receipt photographed closer produces proportionally larger boxes.
 */
fun reflowReceiptLines(elements: List<ReceiptTextElement>): String {
    val usable = elements.filter { it.text.isNotBlank() && it.bottom > it.top }
    if (usable.isEmpty()) return ""

    val ordered = usable.sortedBy { it.top + it.bottom }
    val rows = mutableListOf<MutableList<ReceiptTextElement>>()
    var rowCenter = 0.0
    var rowTolerance = 0.0

    ordered.forEach { element ->
        val center = (element.top + element.bottom) / 2.0
        val tolerance = (element.bottom - element.top) * ROW_TOLERANCE
        if (rows.isEmpty() || kotlin.math.abs(center - rowCenter) > maxOf(rowTolerance, tolerance)) {
            rows += mutableListOf(element)
            rowCenter = center
            rowTolerance = tolerance
        } else {
            rows.last() += element
        }
    }

    return rows.joinToString("\n") { row ->
        row.sortedBy { it.left }.joinToString("  ") { it.text.trim() }
    }
}

/** How far off a row's midpoint a box may sit, as a fraction of its own height. */
private const val ROW_TOLERANCE = 0.6

/** What could be read off a receipt. Empty lists / nulls mean "nothing plausible found". */
data class ReceiptGuess(
    /** Plausible totals, best guess first. */
    val amountCandidates: List<Double>,
    val dateGuess: LocalDate?,
    val merchantGuess: String?
)

/**
 * Line keywords that mark the number on that line as the amount actually paid. Ranked, because a
 * receipt usually prints several: the grand total, a subtotal before tax, the cash tendered, the
 * change given. Picking the largest number is wrong (that's often the cash tendered) and picking
 * the last is wrong too (that's often the change), so the label is what decides.
 */
private val TOTAL_KEYWORDS = listOf(
    "grand total" to 100,
    "amount due" to 95,
    "balance due" to 95,
    "total due" to 95,
    "amount paid" to 90,
    "total" to 80
)

/**
 * Keywords that *demote* a line. "Subtotal" contains "total", so without this a pre-tax subtotal
 * would tie with the real total and could win on ordering alone.
 */
private val NON_TOTAL_KEYWORDS = listOf(
    "subtotal", "sub total", "sub-total", "tax", "vat", "gst", "hst",
    "tip", "gratuity", "change", "cash", "tendered", "discount", "savings"
)

// A money-looking token: optional symbol, digits, optional grouping/decimal separators.
//
// A colon or a slash on either side disqualifies it. Every receipt prints a timestamp, and
// "08/04/2026 10:42" otherwise yields 8, 4, 10 and 42 as candidate amounts — all of which
// outranked the real total on a real scan. A leading minus is excluded for the same reason it
// would be useless: the amount field is positive-only.
private val AMOUNT_TOKEN =
    Regex("""(?<![\w.,:/-])[$€£¥₹]?\s?\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?(?![\d:/-])""")

private val ISO_DATE = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
private val SLASH_DATE = Regex("""\b(\d{1,2})[/.\-](\d{1,2})[/.\-](\d{2,4})\b""")
private val MONTH_NAME_DATE =
    Regex("""\b([A-Za-z]{3,9})\.?\s+(\d{1,2})(?:st|nd|rd|th)?,?\s+(\d{4})\b""")

private val MONTH_NAMES = listOf(
    "jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"
)

/**
 * Reads a number the way a receipt writes one, in either convention: `1,234.56` and `1.234,56`
 * both mean the same amount.
 *
 * The rule is positional rather than locale-based, because the OCR text carries no locale: the
 * *last* separator is the decimal point when 1–2 digits follow it, and every separator is a
 * thousands grouping otherwise. That makes `12,50` fifty cents over twelve and `1,234` one
 * thousand two hundred and thirty-four, which is what each is on a real receipt.
 */
fun parseReceiptAmount(raw: String): Double? {
    val cleaned = raw.trim().trimStart('$', '€', '£', '¥', '₹').trim()
    if (cleaned.isEmpty() || cleaned.none { it.isDigit() }) return null
    if (cleaned.any { !it.isDigit() && it != '.' && it != ',' }) return null

    val lastSeparator = cleaned.indexOfLast { it == '.' || it == ',' }
    val normalized = if (lastSeparator == -1) {
        cleaned
    } else {
        val decimals = cleaned.length - lastSeparator - 1
        if (decimals in 1..2) {
            cleaned.substring(0, lastSeparator).filter { it.isDigit() } + "." +
                cleaned.substring(lastSeparator + 1)
        } else {
            cleaned.filter { it.isDigit() }
        }
    }
    return normalized.toDoubleOrNull()?.takeIf { it > 0.0 }
}

/** Score for a line: higher means "the number here is more likely the amount paid". */
private fun lineScore(lowerLine: String): Int {
    if (NON_TOTAL_KEYWORDS.any { lowerLine.contains(it) }) return -1
    return TOTAL_KEYWORDS.firstOrNull { (keyword, _) -> lowerLine.contains(keyword) }?.second ?: 0
}

/**
 * Amounts worth offering, best first.
 *
 * Labelled lines come first in keyword order. Unlabelled numbers follow, and among those a figure
 * printed with cents beats a bare integer regardless of size — a receipt writes money to two
 * decimal places, so `8.43` is a price and `123` is a house number. Only then does magnitude
 * decide. Demoted lines (subtotal, tax, change) are dropped entirely rather than ranked last:
 * offering the tax amount as a candidate is worse than offering one fewer.
 */
fun receiptAmountCandidates(fullText: String): List<Double> {
    data class Candidate(val score: Int, val order: Int, val amount: Double, val hasCents: Boolean)

    val candidates = mutableListOf<Candidate>()
    fullText.lineSequence().forEachIndexed { index, line ->
        val score = lineScore(line.lowercase())
        if (score < 0) return@forEachIndexed
        AMOUNT_TOKEN.findAll(line).forEach { match ->
            parseReceiptAmount(match.value)?.let { amount ->
                candidates += Candidate(score, index, amount, hasCents = amount % 1.0 != 0.0)
            }
        }
    }

    val labelled = candidates.filter { it.score > 0 }
        // Later lines win ties: a receipt that prints "TOTAL" twice (itemised, then on the payment
        // stub) means the same figure, and the printed-last one is the one that was charged.
        .sortedWith(compareByDescending<Candidate> { it.score }.thenByDescending { it.order })
    val unlabelled = candidates.filter { it.score == 0 }
        .sortedWith(compareByDescending<Candidate> { it.hasCents }.thenByDescending { it.amount })

    return (labelled + unlabelled).map { it.amount }.distinct().take(MAX_AMOUNT_CANDIDATES)
}

/** More than a handful of chips is a list, not a suggestion. */
const val MAX_AMOUNT_CANDIDATES = 4

/**
 * The first date on the receipt that could plausibly be the purchase date.
 *
 * `dd/mm` versus `mm/dd` can't be resolved from the text, so a first component over 12 forces
 * day-first and anything else is read month-first (the US convention most receipts in that format
 * use). A wrong-by-a-few-days date in an editable field is a far smaller cost than refusing to
 * guess; the date picker is right there.
 */
fun receiptDateGuess(fullText: String, today: LocalDate = LocalDate.now()): LocalDate? {
    fun plausible(date: LocalDate): Boolean =
        !date.isAfter(today) && date.isAfter(today.minusYears(5))

    ISO_DATE.findAll(fullText).forEach { m ->
        safeDate(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
            ?.takeIf(::plausible)?.let { return it }
    }
    MONTH_NAME_DATE.findAll(fullText).forEach { m ->
        val month = MONTH_NAMES.indexOfFirst { m.groupValues[1].lowercase().startsWith(it) } + 1
        if (month > 0) {
            safeDate(m.groupValues[3].toInt(), month, m.groupValues[2].toInt())
                ?.takeIf(::plausible)?.let { return it }
        }
    }
    SLASH_DATE.findAll(fullText).forEach { m ->
        val first = m.groupValues[1].toInt()
        val second = m.groupValues[2].toInt()
        val year = expandYear(m.groupValues[3].toInt(), today)
        val date = if (first > 12) safeDate(year, second, first) else safeDate(year, first, second)
        date?.takeIf(::plausible)?.let { return it }
    }
    return null
}

/** Two-digit years are this century, unless that lands in the future. */
private fun expandYear(raw: Int, today: LocalDate): Int = when {
    raw >= 100 -> raw
    2000 + raw <= today.year -> 2000 + raw
    else -> 1900 + raw
}

private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
    try {
        LocalDate.of(year, month, day)
    } catch (e: DateTimeException) {
        null
    }

/**
 * The merchant name: the first line that reads like a name rather than a number, an address or a
 * separator rule. Receipts print the shop name at the top in the largest type, so "first" is
 * usually right and cheap to compute.
 */
fun receiptMerchantGuess(fullText: String): String? =
    fullText.lineSequence()
        .map { it.trim().replace(Regex("""\s+"""), " ") }
        .firstOrNull { line ->
            line.length in 2..40 &&
                line.count { it.isLetter() } >= 2 &&
                // Mostly letters, so "123 Main St" and "**** 4242" don't win.
                line.count { it.isLetter() } * 2 >= line.length
        }

/**
 * Renders a guessed amount for the dialog's amount field, which accepts `^\d*\.?\d{0,2}$` — so
 * plain digits and a dot, no grouping, no currency symbol, and always `Locale.ROOT` regardless of
 * the display currency. Anything else would be rejected by the field's own input filter, leaving
 * the user with a scan that appeared to do nothing.
 */
fun formatAmountInput(amount: Double): String =
    String.format(java.util.Locale.ROOT, "%.2f", amount)

/** Everything the add-item dialog can prefill from one receipt photo. */
fun extractReceiptFields(fullText: String, today: LocalDate = LocalDate.now()): ReceiptGuess =
    ReceiptGuess(
        amountCandidates = receiptAmountCandidates(fullText),
        dateGuess = receiptDateGuess(fullText, today),
        merchantGuess = receiptMerchantGuess(fullText)
    )
