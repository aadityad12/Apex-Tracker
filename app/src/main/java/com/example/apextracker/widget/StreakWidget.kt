package com.example.apextracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.glance.action.actionStartActivity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apextracker.AppDatabase
import com.example.apextracker.MainActivity
import com.example.apextracker.R
import com.example.apextracker.loadDashboardSnapshot
import java.time.LocalDate

/**
 * Home-screen widget showing the perfect-day goal streak (Issue #130). Reads the same snapshot the
 * Dashboard uses, so the number matches. Tapping it opens the app. Refreshed by the app calling
 * [refreshApexWidgets] after goal/completion changes, plus Android's own update cadence.
 */
class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val db = AppDatabase.getDatabase(context)
        val snapshot = loadDashboardSnapshot(db, LocalDate.now())
        provideContent {
            StreakWidgetContent(snapshot.perfectStreak, context)
        }
    }
}

@Composable
private fun StreakWidgetContent(streak: Int, context: Context) {
    // Colours are baked (widgets can't read MaterialTheme). Hand-kept mirror of ApexPalette's
    // GRAPHITE dark scheme — Glance has no access to the ColorScheme. In monochrome the "accent"
    // is ink (Frost), same as the app's primary. See the note in GoalsWidget.kt.
    val accent = ColorProvider(Color(0xFFE9EBEE))
    val onBg = ColorProvider(Color(0xFFE9EBEE))
    val bg = ColorProvider(Color(0xFF0E0F11))

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bg)
            .padding(12.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = streak.toString(),
            style = TextStyle(color = accent, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        )
        Text(
            text = context.resources.getQuantityString(R.plurals.widget_streak_days, streak),
            style = TextStyle(color = onBg, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        )
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}
