package com.example.apextracker

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/** Only anonymous totals cross the device boundary — never titles, notes, app names, or goal names. */
data class ApexTipSnapshot(
    val date: LocalDate,
    val currencyCode: String,
    val spentToday: Double,
    val spentThisMonth: Double,
    val monthlyBudgetLimit: Double?,
    val studyMinutes: Long,
    val screenMinutes: Long,
    val completedGoals: Int,
    val totalGoals: Int,
    val perfectStreak: Int
)

fun DashboardUiState.toApexTipSnapshot(currencyCode: String): ApexTipSnapshot {
    val metrics = metricsFor(today)
    val month = YearMonth.from(today)
    return ApexTipSnapshot(
        date = today,
        currencyCode = currencyCode,
        spentToday = metrics.spend,
        spentThisMonth = spendByDate.entries
            .filter { (date) -> YearMonth.from(date) == month }
            .sumOf { (_, amount) -> amount },
        monthlyBudgetLimit = null, // Filled from BudgetPrefs immediately before the request.
        studyMinutes = metrics.studyBySubject.values.sum() / 60,
        screenMinutes = metrics.screenMillis / 60_000,
        completedGoals = todayGoals.count { it.satisfied },
        totalGoals = todayGoals.size,
        perfectStreak = perfectStreak
    )
}

fun buildApexTipPrompt(snapshot: ApexTipSnapshot): String {
    val limit = snapshot.monthlyBudgetLimit?.let { formatPromptNumber(it) } ?: "not set"
    return """
        You are the Daily Apex Tip inside a private productivity tracker.
        Write exactly one practical, encouraging sentence in plain text, no more than 35 words.
        Base it only on the anonymous totals below. Do not invent context, infer sensitive traits,
        shame the user, or give medical, legal, or investment advice. If there is little data,
        suggest one small measurable action for today.

        Date: ${snapshot.date}
        Currency: ${snapshot.currencyCode}
        Spending today: ${formatPromptNumber(snapshot.spentToday)}
        Spending this month: ${formatPromptNumber(snapshot.spentThisMonth)}
        Monthly spending limit: $limit
        Study minutes today: ${snapshot.studyMinutes}
        Screen minutes today: ${snapshot.screenMinutes}
        Goals completed today: ${snapshot.completedGoals} of ${snapshot.totalGoals}
        Perfect-day streak: ${snapshot.perfectStreak}
    """.trimIndent()
}

fun shouldGenerateApexTip(
    enabled: Boolean,
    date: LocalDate,
    cachedDate: LocalDate?,
    lastAttemptDate: LocalDate?,
    forceRetry: Boolean = false
): Boolean = enabled && cachedDate != date && (forceRetry || lastAttemptDate != date)

fun normalizeApexTip(raw: String?, maxChars: Int = 280): String? {
    val clean = raw
        ?.trim()
        ?.removeSurrounding("\"")
        ?.replace(Regex("\\s+"), " ")
        ?.takeIf(String::isNotBlank)
        ?: return null
    if (clean.length <= maxChars) return clean
    val shortened = clean.take(maxChars - 1).substringBeforeLast(' ', missingDelimiterValue = clean.take(maxChars - 1))
    return "$shortened…"
}

private fun formatPromptNumber(value: Double): String = String.format(Locale.US, "%.2f", value)

/** Thin Firebase boundary so all prompt construction and request decisions stay pure and tested. */
class FirebaseApexTipGenerator {
    private val model = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
        modelName = MODEL_NAME,
        generationConfig = generationConfig {
            temperature = 0.35f
            maxOutputTokens = 96
        }
    )

    suspend fun generate(snapshot: ApexTipSnapshot): String? =
        model.generateContent(buildApexTipPrompt(snapshot)).text

    private companion object {
        // Stable, high-volume model: enough for a short suggestion without paying for a Pro model.
        const val MODEL_NAME = "gemini-3.5-flash-lite"
    }
}
