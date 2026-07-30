package com.example.apextracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import java.time.LocalDate
import java.time.format.TextStyle

private const val DAYS_BACK = 7
private val CHART_HEIGHT = 140.dp

/**
 * Total screen time per day for the [days] days ending at [today] (inclusive), oldest first.
 * Days with no recorded session still get an entry (0L) so the chart always has a fixed,
 * evenly-spaced set of bars; multiple sessions on the same day are summed (defensive — the
 * table's primary key is the date, so at most one row exists per day today). Days outside the
 * window are ignored. Mirrors BudgetTrends.monthlyTotals.
 */
fun dailyTotals(sessions: List<ScreenTimeSession>, days: Int, today: LocalDate): List<Pair<LocalDate, Long>> {
    val sums = sessions.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { s -> s.durationMillis } }
    return (days - 1 downTo 0).map { daysAgo ->
        val day = today.minusDays(daysAgo.toLong())
        day to (sums[day] ?: 0L)
    }
}

/**
 * Hand-drawn 7-day bar chart of this device's daily screen time (today rightmost, highlighted),
 * consistent with BudgetTrendsCard / ApexLogo (bare Canvas, no chart library). Purely
 * presentational: it draws from the already-computed [sessions] state, never triggering usage
 * recomputation. Normalizing to the tallest bar keeps a single inflated historical day (Issue #89)
 * from crashing or emptying the chart — it just reads as a full bar.
 */
/**
 * Seven-day bar chart of this device's daily screen time, today rightmost and accented.
 *
 * Per Design.md §6: no card, no shadow (a shadow over the near-black substrate renders nothing at
 * all, which is what the old `shadowElevation = 2.dp` was paying for), a hairline baseline, 2dp
 * bars, and non-current bars in the authored `chartMuted` tone rather than an accent alpha.
 *
 * Axis labels come from [durationAxisLabels] fed a *rounded* maximum, so the three ticks share one
 * unit (Issue #97) and read as round values instead of arbitrary ones.
 *
 * Purely presentational: it draws from already-computed [sessions] state and never triggers usage
 * recomputation. Normalizing to the rounded peak keeps a single inflated historical day (Issue #89)
 * from emptying the chart.
 */
@Composable
fun ScreenTimeTrendsChart(sessions: List<ScreenTimeSession>) {
    val today = remember { LocalDate.now() }
    val totals = remember(sessions) { dailyTotals(sessions, DAYS_BACK, today) }
    val peakMillis = totals.maxOfOrNull { it.second } ?: 0L
    val hasData = peakMillis > 0L
    // Rounded up past the peak so the tallest bar never sits flush against the top edge.
    val axisMaxMillis = niceAxisMaxMinutes((peakMillis / 60_000L).toInt()) * 60_000L

    val cs = MaterialTheme.colorScheme
    val accent = cs.primary
    val muted = LocalApexSemantics.current.chartMuted
    val line = cs.outline
    val locale = LocalLocale.current.platformLocale

    if (!hasData) {
        Box(modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.screen_trends_empty),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        return
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            // widthIn rather than a fixed width: a fixed one wraps "1h 30m" at a large font scale.
            modifier = Modifier.height(CHART_HEIGHT).widthIn(min = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            durationAxisLabels(axisMaxMillis).forEach { label ->
                Text(label, style = ApexNumerals.small, color = cs.onSurfaceVariant)
            }
        }
        Spacer(modifier = Modifier.width(ApexSpacing.s))
        Column(Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(CHART_HEIGHT),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                totals.forEach { (day, millis) ->
                    val heightFraction = (millis.toDouble() / axisMaxMillis).toFloat().coerceIn(0f, 1f)
                    val isToday = day == today
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                        Canvas(
                            modifier = Modifier.fillMaxWidth(0.56f).fillMaxHeight(heightFraction)
                        ) {
                            drawRoundRect(
                                color = if (isToday) accent else muted,
                                cornerRadius = CornerRadius(2.dp.toPx())
                            )
                        }
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(color = line) }
            Spacer(modifier = Modifier.height(ApexSpacing.xs))
            Row(modifier = Modifier.fillMaxWidth()) {
                totals.forEach { (day, _) ->
                    Text(
                        // NARROW, not SHORT.take(1): truncating a localized name is a localization
                        // bug the Budget chart was already fixed for.
                        text = day.dayOfWeek.getDisplayName(TextStyle.NARROW, locale),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = ApexNumerals.small,
                        color = if (day == today) accent else cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}
