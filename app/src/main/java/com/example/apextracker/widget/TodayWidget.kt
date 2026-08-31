package com.example.apextracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.apextracker.AppDatabase
import com.example.apextracker.MainActivity
import com.example.apextracker.NextReminder
import com.example.apextracker.R
import com.example.apextracker.StudyTimerStateStore
import com.example.apextracker.TodaySnapshot
import com.example.apextracker.formatDurationCompact
import com.example.apextracker.loadTodaySnapshot
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * "Today at a glance" home-screen widget (Issue #44): today's study time, today's screen time and
 * the next reminder in one tile. Read-only — every row deep-links into the screen that owns it.
 *
 * Refreshed by [refreshTodayWidget] from the app's own write points plus Android's 30-minute
 * update cadence; see [loadTodaySnapshot] for why the screen-time figure is the app's stored total
 * rather than a fresh usage query.
 */
class TodayWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadTodaySnapshot(
            db = AppDatabase.getDatabase(context),
            timerStore = StudyTimerStateStore(context),
            now = LocalDateTime.now(),
            nowMillis = System.currentTimeMillis()
        )
        provideContent { TodayWidgetContent(snapshot, context) }
    }
}

@Composable
private fun TodayWidgetContent(snapshot: TodaySnapshot, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetPalette.background)
            .padding(12.dp)
            // Anything not covered by a row's own deep link opens the app's home.
            .clickable(actionStartActivity(openRoute(context, "dashboard")))
    ) {
        Text(
            text = context.getString(R.string.widget_today_title),
            style = TextStyle(color = WidgetPalette.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(GlanceModifier.height(8.dp))

        MetricRow(
            label = context.getString(
                if (snapshot.studyRunning) R.string.widget_today_study_running else R.string.widget_today_study
            ),
            value = formatDurationCompact(snapshot.studySeconds * 1000),
            valueColor = WidgetPalette.ink,
            route = "study_tracker",
            context = context
        )
        Spacer(GlanceModifier.height(4.dp))
        MetricRow(
            label = context.getString(R.string.widget_today_screen),
            value = formatDurationCompact(snapshot.screenMillis),
            valueColor = WidgetPalette.ink,
            route = "screen_time",
            context = context
        )

        // Pushes the reminder block to the bottom edge, so the tile reads as two anchored groups
        // at whatever height the user resizes it to rather than a stack floating at the top.
        Spacer(GlanceModifier.defaultWeight())
        NextReminderRow(snapshot.nextReminder, context)
    }
}

@Composable
private fun MetricRow(
    label: String,
    value: String,
    valueColor: ColorProvider,
    route: String,
    context: Context
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity(openRoute(context, route))),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = label,
            modifier = GlanceModifier.defaultWeight(),
            style = TextStyle(color = WidgetPalette.muted, fontSize = 12.sp)
        )
        Text(
            text = value,
            style = TextStyle(color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
private fun NextReminderRow(next: NextReminder?, context: Context) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(openRoute(context, "reminders")))
    ) {
        Text(
            text = context.getString(
                if (next?.isOverdue == true) R.string.widget_today_overdue else R.string.widget_today_next
            ),
            style = TextStyle(
                // Monochrome emphasis: overdue rises to full ink, everything else stays muted.
                color = if (next?.isOverdue == true) WidgetPalette.ink else WidgetPalette.muted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        )
        if (next == null) {
            Text(
                text = context.getString(R.string.widget_today_no_reminders),
                modifier = GlanceModifier.padding(top = 2.dp),
                style = TextStyle(color = WidgetPalette.muted, fontSize = 13.sp)
            )
        } else {
            Text(
                text = next.name,
                maxLines = 1,
                modifier = GlanceModifier.padding(top = 2.dp),
                style = TextStyle(color = WidgetPalette.ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            )
            Text(
                text = reminderWhen(next, LocalDate.now(), context),
                style = TextStyle(color = WidgetPalette.muted, fontSize = 12.sp)
            )
        }
    }
}

/**
 * "8:30 AM" for something due today, otherwise a dated form — a bare clock time on a widget reads
 * as today, which is exactly wrong for a reminder three days out. All-day reminders have no clock
 * time to show, so they render as the date alone.
 */
private fun reminderWhen(next: NextReminder, today: LocalDate, context: Context): String {
    val locale = context.resources.configuration.locales[0]
    val date = next.dueAt.toLocalDate()
    val time = next.time?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
    val day = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    return when {
        date == today && time != null -> time
        date == today -> context.getString(R.string.widget_today_all_day)
        time != null -> context.getString(R.string.widget_today_date_at_time, day, time)
        else -> day
    }
}

/** Deep-links into one screen; [MainActivity] drops anything outside `APP_ROUTES` (Issue #105). */
private fun openRoute(context: Context, route: String): Intent =
    Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_NAVIGATE_TO, route)

class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()
}
