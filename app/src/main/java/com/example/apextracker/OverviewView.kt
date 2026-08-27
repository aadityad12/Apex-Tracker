package com.example.apextracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.style.TextOverflow
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverviewView(
    onBackToMenu: () -> Unit,
    // Navigates to a module route (budget_tracker/study_tracker/screen_time/reminders). Plain
    // navigate (not popBackStack), so system back returns here to the Overview.
    onNavigate: (String) -> Unit = {},
    viewModel: OverviewViewModel = viewModel(),
    // Reminder toggles route through ReminderViewModel so completing here behaves exactly like
    // the Reminders screen: alarm cancel/reschedule, recurrence advancement, and cloud push.
    reminderViewModel: ReminderViewModel = viewModel()
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val overview by viewModel.dayOverview.collectAsState()
    var showCalendar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(stringResource(R.string.overview_title), 
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(if (showCalendar) Icons.Default.ViewAgenda else Icons.Default.CalendarMonth, 
                            contentDescription = stringResource(R.string.cd_toggle_calendar_view),
                            tint = if (showCalendar) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            if (showCalendar) {
                CalendarGrid(
                    selectedDate = selectedDate,
                    onDateSelected = { 
                        viewModel.selectDate(it)
                        showCalendar = false
                    }
                )
            } else {
                CompactDateNavigator(
                    selectedDate = selectedDate,
                    onDateSelected = { viewModel.selectDate(it) }
                )
                
                overview?.let { data ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = ApexSpacing.l),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        // Stats Row
                        item {
                            // IntrinsicSize.Max + fillMaxHeight on each card so all three match the
                            // tallest. Without it, a value that wraps at a large font scale (a
                            // currency amount does, in a third of the width) made its own card
                            // taller and left the row ragged.
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(IntrinsicSize.Max)
                                    .padding(vertical = ApexSpacing.s),
                                horizontalArrangement = Arrangement.spacedBy(ApexSpacing.m)
                            ) {
                                // All three take the same surface tone. They used to be
                                // primaryContainer / secondaryContainer / tertiaryContainer, i.e.
                                // three different hues for three equivalent stats — colour spent on
                                // decoration. The icon and label already say which is which.
                                StatCard(stringResource(R.string.overview_stat_spent), formatCurrency(data.totalSpent, LocalCurrencyCode.current), Icons.Default.AccountBalanceWallet, Modifier.weight(1f).fillMaxHeight()) { onNavigate("budget_tracker") }
                                StatCard(stringResource(R.string.overview_stat_study), formatDurationCompact(data.studyTimeMinutes * 60000), Icons.Default.Timer, Modifier.weight(1f).fillMaxHeight()) { onNavigate("study_tracker") }
                                StatCard(stringResource(R.string.overview_stat_screen), formatDurationCompact(data.screenTimeMinutes * 60000), Icons.Default.Monitor, Modifier.weight(1f).fillMaxHeight()) { onNavigate("screen_time") }
                            }
                        }

                        // Reminders Section — the header doubles as a "see all" jump to the Reminders screen.
                        item {
                            Spacer(modifier = Modifier.height(ApexSpacing.s))
                            ApexSectionHeader(
                                stringResource(R.string.overview_tasks),
                                modifier = Modifier.clickable { onNavigate("reminders") },
                                trailing = {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.cd_overview_see_all_tasks),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                        
                        if (data.missedReminders.isNotEmpty()) {
                            items(data.missedReminders) { reminder ->
                                ApexDivider()
                                ReminderSummaryCard(reminder, OverviewReminderStatus.MISSED) { reminderViewModel.toggleCompletion(it) }
                            }
                        }
                        
                        if (data.pendingReminders.isNotEmpty()) {
                            items(data.pendingReminders) { reminder ->
                                ApexDivider()
                                ReminderSummaryCard(reminder, OverviewReminderStatus.PENDING) { reminderViewModel.toggleCompletion(it) }
                            }
                        }

                        if (data.completedReminders.isNotEmpty()) {
                            items(data.completedReminders) { reminder ->
                                ApexDivider()
                                ReminderSummaryCard(reminder, OverviewReminderStatus.COMPLETED) { reminderViewModel.toggleCompletion(it) }
                            }
                        }

                        if (data.pendingReminders.isEmpty() && data.completedReminders.isEmpty() && data.missedReminders.isEmpty()) {
                            item { ApexEmptyState(message = stringResource(R.string.overview_all_clear)) }
                        }
                    }
                } ?: run {
                    // dayOverview is null only until the Room combine() first emits (Issue #215)
                    // — a spinner bridges that gap instead of a blank body under the date row.
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(ApexSpacing.xl),
                            strokeWidth = ApexSpacing.hairline
                        )
                    }
                }
            }
        }
    }
}

/** Day stepper. Was a tinted 16dp card; now a plain row — it is chrome, not content. */
@Composable
fun CompactDateNavigator(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = ApexSpacing.s, vertical = ApexSpacing.s),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onDateSelected(selectedDate.minusDays(1)) }) {
            Icon(Icons.Default.ChevronLeft, contentDescription = stringResource(R.string.cd_prev))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE")).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // The date is the screen's subject, so it takes the display face — the one place a
            // serif belongs on this screen.
            Text(
                text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d")),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        IconButton(onClick = { onDateSelected(selectedDate.plusDays(1)) }) {
            Icon(Icons.Default.ChevronRight, contentDescription = stringResource(R.string.cd_next))
        }
    }
}

/**
 * A tappable drill-in to one module's number for the selected day.
 *
 * A card is justified here — each one is a discrete tappable unit, which is the bar Design.md §5
 * sets. What is not justified is three different container hues for three equivalent stats; they
 * now share one surface tone and the value carries the weight, in mono.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(ApexShapes.container),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(modifier = Modifier.fillMaxHeight().padding(ApexSpacing.m)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                if (onClick != null) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                        modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // Martian Mono is wide, so a long value ($1,284.50 / 4h 43m) overflows a third-width
            // card at the full large size. Auto-size down to fit on one line rather than wrap the
            // figure — a wrapped quantity reads as broken.
            Text(
                value,
                style = ApexNumerals.large,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 13.sp, maxFontSize = 23.sp, stepSize = 1.sp)
            )
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/**
 * How a reminder stands on the selected day.
 *
 * A type, not a string. This used to be a `String` that was both the display label and the value
 * switched on (`when (status) { "Missed" -> … }`), which is why the call sites carried hardcoded
 * English literals — untranslatable, and a typo would have silently fallen through to the else.
 */
enum class OverviewReminderStatus { MISSED, PENDING, COMPLETED }

/** One reminder on the selected day. Was a per-status tinted card; now a row on a hairline. */
@Composable
fun ReminderSummaryCard(
    reminder: Reminder,
    status: OverviewReminderStatus,
    onToggle: (Reminder) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    // Only a missed reminder gets a colour, and it is the icon plus the badge — not the row.
    val accentForStatus = if (status == OverviewReminderStatus.MISSED) cs.error else cs.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(vertical = ApexSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = reminder.isCompleted,
            onCheckedChange = { onToggle(reminder) },
            colors = CheckboxDefaults.colors(checkedColor = LocalApexSemantics.current.positive)
        )
        Spacer(modifier = Modifier.width(ApexSpacing.s))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = reminder.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (reminder.isCompleted) cs.onSurfaceVariant else cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccessTime, contentDescription = null,
                    modifier = Modifier.size(12.dp), tint = accentForStatus)
                Spacer(modifier = Modifier.width(ApexSpacing.xs))
                Text(
                    text = reminder.time?.format(DateTimeFormatter.ofPattern("HH:mm"))
                        ?: stringResource(R.string.overview_no_time),
                    style = ApexNumerals.small,
                    color = cs.onSurfaceVariant
                )
            }
        }
        if (status == OverviewReminderStatus.MISSED) {
            Text(
                stringResource(R.string.overview_missed).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                color = cs.error,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CalendarGrid(selectedDate: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    val currentMonth = remember(selectedDate) { selectedDate.withDayOfMonth(1) }
    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfWeek = currentMonth.dayOfWeek.value % 7 // 0 for Sunday

    Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
        Text(
            currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(bottom = 20.dp)
        )
        
        // Days of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(day, 
                    modifier = Modifier.weight(1f), 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, 
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(ApexSpacing.m))

        // Days grid
        var day = 1
        for (i in 0 until 6) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (j in 0 until 7) {
                    val currentDayIndex = i * 7 + j
                    if (currentDayIndex < firstDayOfWeek || day > daysInMonth) {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = currentMonth.withDayOfMonth(day)
                        val isSelected = date == selectedDate
                        val isToday = date == LocalDate.now()
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(ApexSpacing.hairline)
                                .clip(RoundedCornerShape(ApexShapes.control))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = when {
                                    isSelected -> MaterialTheme.colorScheme.onPrimary
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                },
                                fontWeight = if (isToday || isSelected) FontWeight.Black else FontWeight.Normal
                            )
                        }
                        day++
                    }
                }
            }
            if (day > daysInMonth) break
        }
    }
}
