package com.example.apextracker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexSpacing
import java.time.YearMonth
import kotlin.math.abs

private val BAR_HEIGHT = 6.dp

/**
 * Spend-vs-cap progress for the capped categories in [month].
 *
 * Renders nothing at all when no category has a cap — limits are opt-in, and an empty
 * card would be permanent noise for users who never set one.
 *
 * De-carded in the 2026-07-29 redesign: this was a 24dp rounded `surfaceVariant` container with an
 * invisible shadow and an accent-coloured title, the third in a vertical run of four such cards.
 * A section eyebrow plus the bars themselves separates it just as well and costs no nesting.
 */
@Composable
fun BudgetLimitsCard(
    items: List<BudgetItem>,
    categories: List<Category>,
    month: YearMonth,
    overallLimit: Double? = null
) {
    val statuses = remember(items, categories, month) { categoryLimitStatuses(items, categories, month) }
    val overall = remember(items, month, overallLimit) { overallLimitStatus(items, month, overallLimit) }
    if (statuses.isEmpty() && overall == null) return

    Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.l)) {
        ApexSectionHeader(stringResource(R.string.budget_category_limits_title))
        // The whole-month ceiling reads first — it's the number the per-category rows add up
        // against (Issue #125).
        overall?.let { OverallLimitRow(it) }
        statuses.forEach { status ->
            CategoryLimitRow(status)
        }
    }
}

@Composable
private fun CategoryLimitRow(status: CategoryLimitStatus) {
    // Over-limit switches to the theme's error colour; under-limit keeps the category's own colour so
    // the row still reads as that category at a glance. Via categoryColorOf so a legacy pastel hex
    // renders as its palette slot rather than as itself.
    LimitRow(
        label = status.category.name,
        spent = status.spent,
        limit = status.limit,
        fraction = status.fraction,
        remaining = status.remaining,
        isOver = status.isOver,
        barColor = if (status.isOver) MaterialTheme.colorScheme.error else categoryColorOf(status.category.colorHex)
    )
}

@Composable
private fun LimitRow(
    label: String,
    spent: Double,
    limit: Double,
    fraction: Float,
    remaining: Double,
    isOver: Boolean,
    barColor: androidx.compose.ui.graphics.Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
        // FlowRow, not Row: a name and a "$1,806.49 of $2,200.00" pair cannot share one line at large
        // font scales. With a plain Row the value took the space it needed and squeezed the label into
        // a column narrower than one word, so "All spending" broke as "All / spend / ing" and collided
        // with the amount. Here the pair simply wraps onto its own line instead, and at normal scale
        // the layout is unchanged.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(ApexSpacing.xs)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.budget_limit_spent_of,
                    formatCurrency(spent, LocalCurrencyCode.current),
                    formatCurrency(limit, LocalCurrencyCode.current)
                ),
                style = ApexNumerals.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(BAR_HEIGHT),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )

        Text(
            text = if (isOver) {
                // remaining is negative once over; the string already says "over".
                stringResource(R.string.budget_limit_over_by, formatCurrency(abs(remaining), LocalCurrencyCode.current))
            } else {
                stringResource(R.string.budget_limit_remaining, formatCurrency(remaining, LocalCurrencyCode.current))
            },
            style = ApexNumerals.small,
            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** The overall monthly ceiling, styled like a category row but named and coloured for the total. */
@Composable
private fun OverallLimitRow(status: OverallLimitStatus) {
    LimitRow(
        label = stringResource(R.string.budget_overall_limit_label_row),
        spent = status.spent,
        limit = status.limit,
        fraction = status.fraction,
        remaining = status.remaining,
        isOver = status.isOver,
        // `error` and `primary` are both full-strength ink post-monochrome (Issue #257 review) —
        // a category row falls back to its own hue when under, but the overall total has none to
        // fall back to, so this mirrors the row's own text treatment two lines below
        // (error/onSurfaceVariant) instead: full ink when over, dimmed when under.
        barColor = if (status.isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
