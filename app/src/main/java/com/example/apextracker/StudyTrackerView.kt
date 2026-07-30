package com.example.apextracker

import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import android.app.DatePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apextracker.ui.design.ApexChartFrame
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexMotion
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyTrackerView(onBackToMenu: () -> Unit, viewModel: StudyViewModel = viewModel()) {
    val timeSeconds by viewModel.timeSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val currentSubject by viewModel.currentSubject.collectAsState()
    val allSessions by viewModel.getAllSessions().collectAsState(initial = emptyList())
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val todayTotalSeconds by viewModel.todayTotalSeconds.collectAsState()
    val studyStreak by viewModel.studyStreak.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.handleAppBackground()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pastDays = remember(allSessions) {
        val today = LocalDate.now()
        groupSessionsByDate(allSessions.filter { it.date.isBefore(today) })
    }
    val knownSubjects = remember(allSessions) { knownSubjects(allSessions) }

    var showResetConfirm by remember { mutableStateOf(false) }
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text(stringResource(R.string.study_reset_confirm_title)) },
            text = { Text(stringResource(R.string.study_reset_confirm_text)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetTimerManual()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.action_reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    // Manual backfill of a missed past session (Issue #122). Non-null while the dialog is open;
    // the value seeds the date/subject/duration fields (an existing row when opened from history).
    var manualEntry by remember { mutableStateOf<ManualSessionSeed?>(null) }
    manualEntry?.let { seed ->
        ManualSessionDialog(
            seed = seed,
            knownSubjects = knownSubjects,
            onDismiss = { manualEntry = null },
            onSave = { date, subject, seconds ->
                viewModel.logManualSession(date, subject, seconds)
                manualEntry = null
            }
        )
    }

    var showGoalDialog by remember { mutableStateOf(false) }
    if (showGoalDialog) {
        StudyGoalDialog(
            currentMinutes = dailyGoalMinutes,
            onDismiss = { showGoalDialog = false },
            onSave = { viewModel.setDailyGoalMinutes(it); showGoalDialog = false }
        )
    }

    var showSubjectPicker by remember { mutableStateOf(false) }
    if (showSubjectPicker) {
        SubjectPickerDialog(
            current = currentSubject,
            knownSubjects = knownSubjects,
            onDismiss = { showSubjectPicker = false },
            onSelect = {
                viewModel.selectSubject(it)
                showSubjectPicker = false
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(stringResource(R.string.study_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showGoalDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.study_goal_setting))
                    }
                    IconButton(onClick = { showResetConfirm = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_reset))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        // One scrolling column with eyebrow-and-hairline sections. This replaced a 1.45f/1f weighted
        // split whose lower half was a 32dp-rounded surfaceVariant panel — a card at half-screen
        // scale, which also silently dimmed every value inside it. The weighted split additionally
        // left the timer region mostly empty while clipping the chart's day labels, and it would have
        // collapsed the same way the Dashboard's heatmap did at a large font scale.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ApexSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(ApexSpacing.l))

            val goalSeconds = dailyGoalMinutes * 60L
            StudyTimerDisplay(
                seconds = timeSeconds,
                isRunning = isRunning,
                goalFraction = goalFraction(todayTotalSeconds, goalSeconds),
                goalLabel = if (dailyGoalMinutes > 0) {
                    stringResource(R.string.study_goal_progress, (todayTotalSeconds / 60L).toInt(), dailyGoalMinutes)
                } else null
            )

            Spacer(Modifier.height(ApexSpacing.l))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)
            ) {
                SubjectSelectorChip(
                    subject = currentSubject,
                    onClick = { showSubjectPicker = true }
                )
                if (dailyGoalMinutes > 0 && studyStreak > 0) {
                    StudyStreakChip(studyStreak)
                }
            }

            Spacer(Modifier.height(ApexSpacing.xl))

            // Not full-width: a 64dp-tall edge-to-edge slab was the loudest thing on the screen,
            // competing with the timer it is subordinate to.
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, animationSpec = ApexMotion.snap(), label = "press")
            Button(
                onClick = { viewModel.toggleTimer() },
                modifier = Modifier.height(52.dp).widthIn(min = 200.dp).scale(scale),
                interactionSource = interactionSource,
                shape = RoundedCornerShape(ApexShapes.control),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isRunning) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(ApexSpacing.s))
                Text(
                    text = stringResource(if (isRunning) R.string.study_pause_session else R.string.study_start_studying),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(Modifier.height(ApexSpacing.xl))
            ApexDivider()
            Spacer(Modifier.height(ApexSpacing.l))

            if (dailyGoalMinutes >= 0) {
                ApexChartFrame(
                    title = stringResource(R.string.study_this_week),
                    trailing = {
                        Text(
                            formatDurationCompact(weeklyStudyMinutes(allSessions, 7, LocalDate.now())
                                .sumOf { it.second } * 60_000L),
                            style = ApexNumerals.small,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                ) {
                    StudyWeeklyChart(sessions = allSessions, goalMinutes = dailyGoalMinutes)
                }
                Spacer(Modifier.height(ApexSpacing.xl))
                ApexDivider()
                Spacer(Modifier.height(ApexSpacing.l))
            }

            ApexSectionHeader(
                stringResource(R.string.study_recent_history),
                trailing = {
                    IconButton(onClick = { manualEntry = ManualSessionSeed(LocalDate.now().minusDays(1), "", 0L) }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.study_log_past_session),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )

            if (pastDays.isEmpty()) {
                ApexEmptyState(
                    message = stringResource(R.string.study_no_history),
                    actionLabel = stringResource(R.string.study_log_past_session),
                    onAction = { manualEntry = ManualSessionSeed(LocalDate.now().minusDays(1), "", 0L) }
                )
            } else {
                // A plain Column, not a LazyColumn: the page scrolls now, and a nested lazy list
                // inside a scrollable parent has no bounded height to work with.
                pastDays.forEachIndexed { i, day ->
                    if (i > 0) ApexDivider()
                    DayStudyItem(day, onEditSubject = { subject, seconds ->
                        manualEntry = ManualSessionSeed(day.date, subject, seconds)
                    })
                }
            }

            Spacer(Modifier.height(ApexSpacing.xxl))
        }
    }
}

/** Tappable label under the timer showing which subject time is being attributed to. */
@Composable
fun SubjectSelectorChip(subject: String, onClick: () -> Unit) {
    val chipDescription = stringResource(R.string.cd_choose_subject)
    Surface(
        onClick = onClick,
        // Was CircleShape. Also sets contentColor explicitly: Surface(color = surfaceVariant)
        // otherwise dims everything inside it to onSurfaceVariant (Design.md §5).
        shape = RoundedCornerShape(ApexShapes.control),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { contentDescription = chipDescription }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ApexSpacing.m, vertical = ApexSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.study_subject_label),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(ApexSpacing.s))
            Text(
                text = subject.ifBlank { stringResource(R.string.study_no_subject) },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SubjectPickerDialog(
    current: String,
    knownSubjects: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var newSubject by remember { mutableStateOf("") }
    // "" (No subject) always offered first, then every previously used subject.
    val options = remember(knownSubjects) { listOf("") + knownSubjects }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.study_choose_subject)) },
        text = {
            Column {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = option == current,
                            onClick = { onSelect(option) },
                            label = { Text(option.ifBlank { stringResource(R.string.study_no_subject) }) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubject,
                        onValueChange = { newSubject = it },
                        label = { Text(stringResource(R.string.study_new_subject_hint)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardActions = KeyboardActions(
                            onDone = { if (newSubject.isNotBlank()) onSelect(newSubject) }
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { if (newSubject.isNotBlank()) onSelect(newSubject) },
                        enabled = newSubject.isNotBlank()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.study_add_subject))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
fun StudyTimerDisplay(
    seconds: Long,
    isRunning: Boolean,
    goalFraction: Float = 0f,
    goalLabel: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(contentAlignment = Alignment.Center) {
        // There used to be two counter-rotating decorative rings behind this — a sweep-gradient arc
        // spinning one way and a faint circle spinning the other, carrying no information whatsoever.
        // They are the reference example of banned ambient motion in the design law. Deleted.
        //
        // What remains is the determinate goal arc, which does carry information: the track plus a
        // sweep for today's fraction of the daily goal, starting at 12 o'clock and never rotating,
        // so it reads as progress rather than decoration.
        if (goalLabel != null) {
            val trackColor = MaterialTheme.colorScheme.outlineVariant
            Canvas(modifier = Modifier.size(232.dp)) {
                val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
                if (goalFraction > 0f) {
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * goalFraction.coerceAtMost(1f),
                        useCenter = false,
                        style = stroke
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Geist Mono, tabular. This used to be displayLarge (now Instrument Serif) with
            // FontWeight.Black and -2sp tracking, which was three violations at once: a proportional
            // face on a value that changes every second (so the digits shifted as it counted), a
            // synthetic bold on a face that ships exactly one weight, and a display serif used for a
            // quantity. Mono figures are fixed-width, so the timer no longer moves while running.
            Text(
                text = formatTime(seconds),
                style = ApexNumerals.hero,
                color = if (isRunning) primaryColor else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isRunning) stringResource(R.string.study_focusing) else stringResource(R.string.study_ready),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (goalLabel != null) {
                Spacer(modifier = Modifier.height(ApexSpacing.s))
                Text(
                    text = goalLabel,
                    style = ApexNumerals.small,
                    color = if (goalFraction >= 1f) LocalApexSemantics.current.positive
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Flame + consecutive-day count for the study goal streak (Issue #42). */
@Composable
fun StudyStreakChip(streak: Int) {
    Surface(
        // Was RoundedCornerShape(50) — a pill. Pills are for filters and choices here, not labels.
        shape = RoundedCornerShape(ApexShapes.control),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = ApexSpacing.m, vertical = ApexSpacing.s),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalFireDepartment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(ApexSpacing.s))
            Text(
                text = stringResource(R.string.study_streak, streak),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Set the daily study goal in minutes; 0 turns the goal/streak UI off (Issue #42). */
@Composable
fun StudyGoalDialog(currentMinutes: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var text by remember { mutableStateOf(currentMinutes.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.study_goal_setting)) },
        text = {
            Column {
                Text(stringResource(R.string.study_goal_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.study_goal_minutes_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text.toIntOrNull() ?: 0) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/** 7-day bar chart of study minutes with the daily goal as a dashed line (Issue #42). */
/**
 * Minutes per day for the last seven days, per the chart spec in Design.md §6: axis labels are
 * max-and-zero only, the baseline is a hairline, bars are 2dp, the current day takes the accent and
 * the rest sit at 12% onSurface. Previously this drew bars with no axis labels at all — the reader
 * had no idea whether a bar meant twenty minutes or four hours.
 */
@Composable
fun StudyWeeklyChart(sessions: List<StudySession>, goalMinutes: Int) {
    val today = remember { LocalDate.now() }
    val bars = remember(sessions) { weeklyStudyMinutes(sessions, 7, today) }
    val peak = (bars.maxOfOrNull { it.second } ?: 0).coerceAtLeast(goalMinutes)
    // Rounded up past the peak rather than scaled by a factor: scaling gave axis maxima like
    // "1h 6m". See niceAxisMaxMinutes — it also guarantees the headroom that keeps the dashed
    // target line off the top edge.
    val maxMinutes = niceAxisMaxMinutes(peak)
    val hasData = bars.any { it.second > 0 }
    val locale = LocalLocale.current.platformLocale

    val cs = MaterialTheme.colorScheme
    val accent = cs.primary
    val muted = LocalApexSemantics.current.chartMuted
    val line = cs.outline
    val goalColor = cs.onSurfaceVariant

    Row(modifier = Modifier.fillMaxWidth()) {
        // Three ticks via durationAxisLabels, which forces one shared unit across them — a
        // sub-minute week used to collapse to three identical "0m"s (Issue #97). Fed the *rounded*
        // maximum so the values read round rather than arbitrary. Both duration charts in the app
        // (this and Screen Time's) use this same treatment.
        Column(
            // widthIn, not width: a fixed 44dp clipped "1h 30m" onto two lines at a large font
            // scale, where it collided with the plot.
            modifier = Modifier.height(120.dp).widthIn(min = 40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            durationAxisLabels(maxMinutes * 60_000L).forEach { label ->
                Text(label, style = ApexNumerals.small, color = cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(ApexSpacing.s))
        Column(Modifier.weight(1f)) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                if (!hasData) {
                    Text(
                        stringResource(R.string.study_week_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val slot = size.width / bars.size
                    val barWidth = slot * 0.56f
                    bars.forEachIndexed { i, (day, minutes) ->
                        val h = size.height * (minutes.toFloat() / maxMinutes)
                        val left = i * slot + (slot - barWidth) / 2
                        drawRoundRect(
                            color = if (day == today) accent else muted,
                            topLeft = androidx.compose.ui.geometry.Offset(left, size.height - h),
                            size = androidx.compose.ui.geometry.Size(barWidth, h),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                        )
                    }
                    // The daily target. Dashed because it is a reference line rather than data —
                    // that distinction is the reason it stays dashed while everything else is solid.
                    if (goalMinutes > 0) {
                        val y = size.height * (1f - goalMinutes.toFloat() / maxMinutes)
                        drawLine(
                            color = goalColor,
                            start = androidx.compose.ui.geometry.Offset(0f, y),
                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                        )
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) { drawRect(color = line) }
            Spacer(Modifier.height(ApexSpacing.xs))
            Row(modifier = Modifier.fillMaxWidth()) {
                bars.forEach { (day, _) ->
                    Text(
                        text = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale),
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

/**
 * One day of history: the date, its grand total, and a per-subject breakdown.
 *
 * Was a 16dp tinted card per day, which stacked into exactly the card-run this redesign removes.
 * Now a heading row plus indented subject rows, all durations in Geist Mono so the column aligns.
 */
@Composable
fun DayStudyItem(day: DayStudy, onEditSubject: (String, Long) -> Unit = { _, _ -> }) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern("MMM d")),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = day.date.format(DateTimeFormatter.ofPattern("EEEE")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatDurationCompact(day.totalSeconds * 1000),
                style = ApexNumerals.medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Only surface the breakdown when there's something to differentiate — a single
        // uncategorised bucket adds no information over the total already shown above.
        val showBreakdown = day.subjects.size > 1 ||
            (day.subjects.size == 1 && day.subjects.first().subject.isNotBlank())
        if (showBreakdown) {
            Spacer(modifier = Modifier.height(ApexSpacing.s))
            day.subjects.forEach { subjectTotal ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onEditSubject(subjectTotal.subject, subjectTotal.seconds) }
                        .heightIn(min = 48.dp)
                        .padding(start = ApexSpacing.l),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = subjectTotal.subject.ifBlank { stringResource(R.string.study_no_subject) },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDurationCompact(subjectTotal.seconds * 1000),
                        style = ApexNumerals.medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    else String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

/** Seed values for [ManualSessionDialog] — a blank new entry or an existing row being edited. */
data class ManualSessionSeed(val date: LocalDate, val subject: String, val seconds: Long)

/**
 * Manual entry/edit of a past day's study time (Issue #122). Writes through
 * [StudyViewModel.logManualSession], which keys on (date, subject) exactly like the timer, so
 * saving over an existing row replaces it and 0 clears it. Today isn't offered — the running timer
 * owns today's totals.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ManualSessionDialog(
    seed: ManualSessionSeed,
    knownSubjects: List<String>,
    onDismiss: () -> Unit,
    onSave: (LocalDate, String, Long) -> Unit
) {
    val context = LocalContext.current
    var date by remember { mutableStateOf(seed.date) }
    var subject by remember { mutableStateOf(seed.subject) }
    var hours by remember { mutableStateOf((seed.seconds / 3600).toString()) }
    var minutes by remember { mutableStateOf(((seed.seconds % 3600) / 60).toString()) }

    val parsedSeconds = parseManualDurationSeconds(hours, minutes)
    val options = remember(knownSubjects, seed.subject) {
        (listOf("") + knownSubjects + seed.subject).distinct()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.study_log_past_session)) },
        text = {
            Column {
                OutlinedButton(
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val picked = LocalDate.of(year, month + 1, dayOfMonth)
                                // Today and later belong to the timer, so clamp to yesterday.
                                date = if (picked.isBefore(LocalDate.now())) picked else LocalDate.now().minusDays(1)
                            },
                            date.year, date.monthValue - 1, date.dayOfMonth
                        ).also { dialog ->
                            dialog.datePicker.maxDate = System.currentTimeMillis() - 86_400_000L
                            dialog.show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy")))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(stringResource(R.string.study_subject_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { option ->
                        FilterChip(
                            selected = normalizeSubject(option) == normalizeSubject(subject),
                            onClick = { subject = option },
                            label = { Text(option.ifBlank { stringResource(R.string.study_no_subject) }) }
                        )
                    }
                }
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text(stringResource(R.string.study_new_subject_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text(stringResource(R.string.study_hours_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutes,
                        onValueChange = { minutes = it },
                        label = { Text(stringResource(R.string.study_minutes_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { parsedSeconds?.let { onSave(date, subject, it) } },
                enabled = parsedSeconds != null
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
