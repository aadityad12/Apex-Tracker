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
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.apextracker.AppDatabase
import com.example.apextracker.BudgetPrefs
import com.example.apextracker.BudgetWidgetSnapshot
import com.example.apextracker.CurrencySettings
import com.example.apextracker.MainActivity
import com.example.apextracker.R
import com.example.apextracker.SecuritySettings
import com.example.apextracker.formatCurrency
import com.example.apextracker.loadBudgetWidgetSnapshot
import java.time.format.TextStyle as DateTextStyle

/** Home-screen summary of this month's spending against the overall Budget limit (Issue #167). */
class BudgetWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = loadBudgetWidgetSnapshot(
            db = AppDatabase.getDatabase(context),
            budgetPrefs = BudgetPrefs(context),
            currencySettings = CurrencySettings(context),
            securitySettings = SecuritySettings(context)
        )
        provideContent { BudgetWidgetContent(snapshot, context) }
    }
}

// See WidgetPalette — Glance cannot consume ApexTracker's Compose ColorScheme.
private val ink = WidgetPalette.ink
private val muted = WidgetPalette.muted
private val track = WidgetPalette.track
private val error = WidgetPalette.negative
private val background = WidgetPalette.background

@Composable
private fun BudgetWidgetContent(snapshot: BudgetWidgetSnapshot, context: Context) {
    val locale = context.resources.configuration.locales[0]
    val monthName = snapshot.month.month
        .getDisplayName(DateTextStyle.FULL, locale)
        .uppercase(locale)
    val openBudget = Intent(context, MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_NAVIGATE_TO, "budget_tracker")

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .padding(14.dp)
            .clickable(actionStartActivity(openBudget)),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        Text(
            text = context.getString(R.string.widget_budget_title, monthName),
            style = TextStyle(color = muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        )

        // Budget is locked (Issue #45/#187): the launcher is outside the lock's reach, so the
        // widget shows nothing but an invitation to open the app, where the prompt lives.
        if (snapshot.locked) {
            Text(
                text = context.getString(R.string.widget_budget_locked),
                modifier = GlanceModifier.padding(top = 5.dp),
                style = TextStyle(color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            )
            Text(
                text = context.getString(R.string.widget_budget_locked_hint),
                modifier = GlanceModifier.padding(top = 4.dp),
                style = TextStyle(color = muted, fontSize = 12.sp)
            )
            return@Column
        }

        Text(
            text = formatCurrency(snapshot.spent, snapshot.currencyCode),
            modifier = GlanceModifier.padding(top = 5.dp),
            style = TextStyle(color = ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        )

        val status = snapshot.limitStatus
        if (status == null) {
            Text(
                text = context.getString(R.string.widget_budget_spent_this_month),
                modifier = GlanceModifier.padding(top = 2.dp),
                style = TextStyle(color = muted, fontSize = 12.sp)
            )
            Text(
                text = context.getString(R.string.widget_budget_no_limit),
                modifier = GlanceModifier.padding(top = 7.dp),
                style = TextStyle(color = ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
        } else {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(top = 2.dp),
                horizontalAlignment = Alignment.Horizontal.End
            ) {
                Text(
                    text = context.getString(
                        R.string.widget_budget_of_limit,
                        formatCurrency(status.limit, snapshot.currencyCode)
                    ),
                    style = TextStyle(color = muted, fontSize = 12.sp)
                )
            }
            LinearProgressIndicator(
                progress = status.fraction,
                modifier = GlanceModifier.fillMaxWidth().height(5.dp).padding(top = 7.dp),
                color = if (status.isOver) error else ink,
                backgroundColor = track
            )
            Text(
                text = if (status.isOver) {
                    context.getString(
                        R.string.widget_budget_over,
                        formatCurrency(-status.remaining, snapshot.currencyCode)
                    )
                } else {
                    context.getString(
                        R.string.widget_budget_remaining,
                        formatCurrency(status.remaining, snapshot.currencyCode)
                    )
                },
                modifier = GlanceModifier.padding(top = 7.dp),
                style = TextStyle(
                    color = if (status.isOver) error else muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

class BudgetWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = BudgetWidget()
}
