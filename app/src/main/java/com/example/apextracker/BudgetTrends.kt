package com.example.apextracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexChartFrame
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import java.time.YearMonth
import java.time.format.TextStyle

private const val MONTHS_BACK = 6
private val CHART_HEIGHT = 140.dp

/**
 * Total spend per month for the [monthsBack] months ending at [today] (inclusive), oldest
 * first. Months with no items still get an entry (amount 0.0) so the chart always has a
 * fixed, evenly-spaced set of bars.
 */
fun monthlyTotals(items: List<BudgetItem>, monthsBack: Int, today: YearMonth): List<Pair<YearMonth, Double>> {
    val sums = items.groupBy { YearMonth.from(it.date) }.mapValues { (_, v) -> v.sumOf { item -> item.amount } }
    return (monthsBack - 1 downTo 0).map { monthsAgo ->
        val month = today.minusMonths(monthsAgo.toLong())
        month to (sums[month] ?: 0.0)
    }
}

/**
 * Six months of spend. Framed by [ApexChartFrame] rather than a card: the old surface was a 24dp
 * rounded container with `shadowElevation = 2.dp`, which draws nothing at all over a near-black
 * background — one of the stacked cards this redesign exists to remove.
 *
 * Two fixes `Design.md` §6 called for by name:
 * - **Month labels are three letters.** They used to be `getDisplayName(SHORT, locale).take(1)`,
 *   which renders J F M A M J J A S O N D — in a six-month window up to three bars could all be
 *   labelled "J", so the axis could not be read at all.
 * - **Non-current bars use `ApexSemantics.chartMuted`,** not `primary.copy(alpha = 0.3f)`. A single
 *   alpha that reads on the near-black substrate is nearly invisible on paper, which is why the
 *   token is authored per theme.
 */
@Composable
fun BudgetTrendsCard(items: List<BudgetItem>, selectedMonth: YearMonth, onMonthSelected: (YearMonth) -> Unit) {
    val today = remember { YearMonth.now() }
    val totals = remember(items) { monthlyTotals(items, MONTHS_BACK, today) }
    val maxTotal = totals.maxOf { it.second }

    ApexChartFrame(stringResource(R.string.budget_trends_title)) {
        // An empty plot is a legitimate state, not an error: draw the baseline and say so, never a
        // blank box (Design.md §6).
        if (maxTotal == 0.0) {
            Column {
                ApexDivider()
                Spacer(Modifier.height(ApexSpacing.m))
                Text(
                    stringResource(R.string.budget_trends_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@ApexChartFrame
        }

        val currencyCode = LocalCurrencyCode.current
        val primaryColor = MaterialTheme.colorScheme.primary
        val mutedColor = LocalApexSemantics.current.chartMuted

        val locale = LocalLocale.current.platformLocale

        // The axis column sizes itself to its labels rather than to a fixed width, and the baseline
        // and month labels live *inside* the plot column so they inherit that width. The previous
        // version pinned the axis at 48dp and aligned the labels with `padding(start = 60.dp)` — two
        // magic numbers that had to agree, and at 200% font scale the label no longer fitted, so
        // "$1,806" wrapped to "$1," / "806".
        Row {
            Column(
                modifier = Modifier.height(CHART_HEIGHT),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                // Max and zero only — never a full grid (Design.md §6). The mid tick the old chart
                // drew was a third label carrying nothing the endpoints did not already give.
                Text(
                    formatCurrencyCompact(maxTotal, currencyCode),
                    style = ApexNumerals.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Text(
                    formatCurrencyCompact(0.0, currencyCode),
                    style = ApexNumerals.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(ApexSpacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    totals.forEach { (month, amount) ->
                        val heightFraction = (amount / maxTotal).toFloat()
                        val isCurrentMonth = month == today
                        val monthName = month.month.getDisplayName(TextStyle.FULL, locale)
                        val barLabel = stringResource(
                            R.string.budget_trends_bar_cd,
                            monthName,
                            formatCurrency(amount, currencyCode)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics { contentDescription = barLabel }
                                .clickable(onClickLabel = barLabel) { onMonthSelected(month) },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth(0.5f)
                                    .fillMaxHeight(heightFraction)
                            ) {
                                drawRoundRect(
                                    color = if (isCurrentMonth) primaryColor else mutedColor,
                                    cornerRadius = CornerRadius(ApexShapes.cell.toPx())
                                )
                            }
                        }
                    }
                }

                // The baseline the bars stand on.
                ApexDivider()
                Spacer(modifier = Modifier.height(ApexSpacing.xs))

                Row(modifier = Modifier.fillMaxWidth()) {
                    totals.forEach { (month, _) ->
                        Text(
                            text = month.month.getDisplayName(TextStyle.SHORT, locale),
                            modifier = Modifier.weight(1f).clickable { onMonthSelected(month) },
                            textAlign = TextAlign.Center,
                            style = ApexNumerals.small,
                            color = if (month == selectedMonth) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
