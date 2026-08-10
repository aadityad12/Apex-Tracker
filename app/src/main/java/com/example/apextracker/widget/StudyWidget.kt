package com.example.apextracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.apextracker.AppDatabase
import com.example.apextracker.FirebaseManager
import com.example.apextracker.MainActivity
import com.example.apextracker.R
import com.example.apextracker.StudyTimerStateStore
import com.example.apextracker.formatDurationCompact
import com.example.apextracker.loadTodayStudySeconds
import com.example.apextracker.toggleStudyTimer
import java.time.LocalDate

/** Everything the study widget renders, resolved before composition. */
private data class StudyWidgetState(
    val todaySeconds: Long,
    val running: Boolean,
    val subject: String
)

/**
 * Home-screen study timer with a start/pause control (Issue #132): today's total across every
 * subject, plus the one button.
 *
 * The button does not manage the timer itself — it calls [toggleStudyTimer], the same path
 * `StudyViewModel` uses, so the persisted run state and the banked `(date, subject)` row can't
 * diverge from what the in-app timer would have written. The widget deliberately has no subject
 * picker; it continues whatever subject was last selected in the app
 * ([StudyTimerStateStore.lastSubject]).
 *
 * The total can't tick — Glance recomposes only when something pushes it — so a running widget
 * shows the total as of the last update and says so. Tapping it opens the Study screen, where the
 * live flip clock is.
 */
class StudyWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val timerStore = StudyTimerStateStore(context)
        val running = timerStore.loadRunning()
        val state = StudyWidgetState(
            todaySeconds = loadTodayStudySeconds(
                db = AppDatabase.getDatabase(context),
                timerStore = timerStore,
                nowMillis = System.currentTimeMillis()
            ),
            running = running?.date == LocalDate.now(),
            subject = running?.subject ?: timerStore.lastSubject
        )
        provideContent { StudyWidgetContent(state, context) }
    }
}

@Composable
private fun StudyWidgetContent(state: StudyWidgetState, context: Context) {
    val openStudy = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_NAVIGATE_TO, "study_tracker")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetPalette.background)
            .padding(14.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .defaultWeight()
                .clickable(actionStartActivity(openStudy)),
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
            verticalAlignment = Alignment.Vertical.CenterVertically
        ) {
            Text(
                text = context.getString(
                    if (state.running) R.string.widget_study_running else R.string.widget_study_today
                ),
                style = TextStyle(color = WidgetPalette.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = formatDurationCompact(state.todaySeconds * 1000),
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(
                    color = if (state.running) WidgetPalette.positive else WidgetPalette.ink,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            if (state.subject.isNotEmpty()) {
                Text(
                    text = state.subject,
                    maxLines = 1,
                    modifier = GlanceModifier.padding(top = 2.dp),
                    style = TextStyle(color = WidgetPalette.muted, fontSize = 12.sp)
                )
            }
        }

        Spacer(GlanceModifier.height(10.dp))
        Text(
            text = context.getString(
                if (state.running) R.string.widget_study_pause else R.string.widget_study_start
            ),
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(WidgetPalette.track)
                .cornerRadius(12.dp)
                .padding(vertical = 10.dp)
                .clickable(actionRunCallback<ToggleStudyTimerAction>()),
            style = TextStyle(
                color = WidgetPalette.ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.glance.text.TextAlign.Center
            )
        )
    }
}

/**
 * Runs the toggle off the widget's own coroutine scope. Glance re-runs `provideGlance` for this
 * provider once the callback returns, and [toggleStudyTimer] refreshes the other widgets itself.
 */
class ToggleStudyTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        toggleStudyTimer(
            context = context,
            db = AppDatabase.getDatabase(context),
            timerStore = StudyTimerStateStore(context),
            firebaseManager = FirebaseManager(context),
            nowMillis = System.currentTimeMillis()
        )
        StudyWidget().update(context, glanceId)
    }
}

class StudyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StudyWidget()
}
