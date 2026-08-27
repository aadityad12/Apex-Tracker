package com.example.apextracker

import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apextracker.ui.design.ApexChartFrame
import com.example.apextracker.ui.design.ApexDatePickerDialog
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexFlipClock
import com.example.apextracker.ui.design.ApexMotion
import com.example.apextracker.ui.design.FlipClockFitToWidth
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import com.example.apextracker.ui.design.FrostDim
import com.example.apextracker.ui.design.GraphiteBase
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudyTrackerView(
    onBackToMenu: () -> Unit,
    onFocusModeChange: (Boolean) -> Unit = {},
    viewModel: StudyViewModel = viewModel()
) {
    // Note the missing `by`. collectAsState() itself performs no snapshot read — only .value does —
    // so holding the State and handing a lambda to the clock keeps the once-a-second tick from
    // recomposing this whole screen, chart and history rows included.
    val timeSecondsState = viewModel.timeSeconds.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val currentSubject by viewModel.currentSubject.collectAsState()
    val allSessions by viewModel.getAllSessions().collectAsState(initial = emptyList())
    val dailyGoalMinutes by viewModel.dailyGoalMinutes.collectAsState()
    val todayTotalSeconds by viewModel.todayTotalSeconds.collectAsState()
    val studyStreak by viewModel.studyStreak.collectAsState()
    val context = LocalContext.current
    var ambientDisplay by rememberSaveable { mutableStateOf(false) }

    // Hoisted above the focus swap so scroll position survives a focus round trip.
    val scrollState = rememberScrollState()

    // Focus mode is exactly "the stopwatch is running" — there is no separate flag that could drift
    // out of sync with the timer. The onDispose is belt to AppNavigation's braces: between them the
    // bottom bar cannot get stuck hidden whatever happens inside this screen.
    //
    // If a subject switcher is ever added to the focus surface, note that selectSubject() pauses,
    // suspends on a DAO read, then resumes — isRunning goes true→false→true and focus mode would
    // flicker. It is unreachable today because the subject chip is one of the things focus hides.
    // The fix then is to derive focus from a debounced snapshotFlow { isRunning }, not to special-
    // case selectSubject.
    LaunchedEffect(isRunning) { onFocusModeChange(isRunning) }
    DisposableEffect(Unit) { onDispose { onFocusModeChange(false) } }

    LaunchedEffect(isRunning) {
        if (!isRunning) ambientDisplay = false
    }
    FocusWindowEffects(active = isRunning, ambient = ambientDisplay)

    // System back pauses rather than leaving the screen. Losing the surface you were watching by
    // accident is worse than the alternative, and Pause is the one thing this surface is for.
    BackHandler(enabled = isRunning) { viewModel.pauseTimer() }

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
            // Hidden while focused. expand/shrink rather than a bare fade, because the Scaffold
            // derives innerPadding from the measured bar height — animating the height animates the
            // padding too, so the body grows into the vacated space instead of leaving a hole.
            AnimatedVisibility(
                visible = !isRunning,
                enter = expandVertically(ApexMotion.enter(), expandFrom = Alignment.Top) + fadeIn(ApexMotion.enter()),
                exit = shrinkVertically(ApexMotion.exit(), shrinkTowards = Alignment.Top) + fadeOut(ApexMotion.exit()),
            ) {
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
                        IconButton(
                            onClick = {
                                shareCsv(
                                    context,
                                    buildStudyCsv(allSessions),
                                    "study_sessions_${LocalDate.now()}.csv"
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_export_study_csv)
                            )
                        }
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
        }
    ) { innerPadding ->
        // Running collapses the screen to the focus surface. The incoming side fades and scales in,
        // echoing the NavHost's own idiom, so focus mode reads as arriving somewhere rather than as
        // the rest of the screen merely vanishing.
        AnimatedContent(
            targetState = isRunning,
            transitionSpec = {
                (fadeIn(ApexMotion.enter()) + scaleIn(initialScale = 0.96f, animationSpec = ApexMotion.enter()))
                    .togetherWith(fadeOut(ApexMotion.exit()))
                    .using(SizeTransform(clip = false))
            },
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            label = "focus"
        ) { focused ->
            if (focused) {
                StudyFocusContent(
                    seconds = { timeSecondsState.value },
                    subject = currentSubject,
                    ambient = ambientDisplay,
                    onToggleAmbient = { ambientDisplay = !ambientDisplay },
                    onPause = { viewModel.toggleTimer() }
                )
            } else {
                StudyIdleContent(
                    scrollState = scrollState,
                    seconds = { timeSecondsState.value },
                    currentSubject = currentSubject,
                    allSessions = allSessions,
                    dailyGoalMinutes = dailyGoalMinutes,
                    todayTotalSeconds = todayTotalSeconds,
                    studyStreak = studyStreak,
                    pastDays = pastDays,
                    onToggleTimer = { viewModel.toggleTimer() },
                    onPickSubject = { showSubjectPicker = true },
                    onManualEntry = { manualEntry = it }
                )
            }
        }
    }
}

/**
 * Focus mode owns the window while it lasts: the screen stays awake (a stopwatch you are watching
 * must not time out) and the system bars go away, since this is the only full-bleed surface in the
 * app. Every acquisition is paired in onDispose, and [active] is a key rather than a branch inside
 * the effect, so the window is handed back the moment focus ends — not merely when the screen is
 * left.
 *
 * Teardown is covered on every path: pausing changes the key; navigating away disposes the whole
 * destination; being stopped with the screen on pauses the timer through the existing ON_STOP
 * observer; being stopped with the screen off deliberately keeps counting, which is harmless because
 * FLAG_KEEP_SCREEN_ON only acts on a visible window; rotation re-runs the effect against the new
 * window; and process death takes the flags with it.
 *
 * enableEdgeToEdge() does not conflict. It sets decorFitsSystemWindows = false and transparent bar
 * backgrounds; it does not pin bar visibility. Hiding the bars drives WindowInsets.systemBars to
 * zero and showing them brings it back, while the SystemBarStyle that governs icon contrast is
 * never touched here.
 */
@Composable
private fun FocusWindowEffects(active: Boolean, ambient: Boolean) {
    val view = LocalView.current
    val activity = LocalActivity.current
    if (view.isInEditMode || activity == null) return   // previews have no Activity window
    DisposableEffect(active, activity, view) {
        val window = activity.window
        val controller = WindowCompat.getInsetsController(window, view)
        if (active) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller.show(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
    DisposableEffect(active, ambient, activity) {
        val window = activity.window
        val previousBrightness = window.attributes.screenBrightness
        if (active && ambient) {
            window.attributes = window.attributes.apply {
                screenBrightness = AMBIENT_SCREEN_BRIGHTNESS
            }
        }
        onDispose {
            window.attributes = window.attributes.apply {
                screenBrightness = previousBrightness
            }
        }
    }
}

private const val AMBIENT_SCREEN_BRIGHTNESS = 0.03f

/**
 * The focus surface: the subject that is banking the time, the clock, and the way out. Nothing else
 * — no eyebrow (a screen that is only a clock does not need a caption reading FOCUSING), no goal
 * meter, no chart, no history, and neither app bar.
 *
 * Removing the rest of the screen from composition while the timer runs is also the one real fix for
 * a pre-existing cost: the session writes a Room row every second, so the weekly chart and every
 * history row used to re-execute once a second for the whole session.
 */
@Composable
private fun StudyFocusContent(
    seconds: () -> Long,
    subject: String,
    ambient: Boolean,
    onToggleAmbient: () -> Unit,
    onPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (ambient) GraphiteBase else MaterialTheme.colorScheme.background)
    ) {
        TextButton(
            onClick = onToggleAmbient,
            modifier = Modifier.align(Alignment.TopEnd).padding(ApexSpacing.s)
        ) {
            Text(
                text = stringResource(
                    if (ambient) R.string.study_restore_brightness else R.string.study_dim_display
                ),
                color = if (ambient) FrostDim else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = ApexSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = subject.ifBlank { stringResource(R.string.study_no_subject) },
                style = MaterialTheme.typography.titleSmall,
                color = if (ambient) FrostDim else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(ApexSpacing.xl))
            FlipClockFitToWidth {
                ApexFlipClock(seconds = seconds, active = true, ambient = ambient)
            }
            Spacer(Modifier.height(ApexSpacing.xxl))
            // navigationBarsPadding: 0dp while the bars are hidden, and correct if one is swiped
            // transiently back in — the button must never end up underneath the gesture handle.
            StudyToggleButton(
                isRunning = true,
                ambient = ambient,
                onClick = onPause,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}

/**
 * The full screen: one scrolling column with eyebrow-and-hairline sections. This replaced a
 * 1.45f/1f weighted split whose lower half was a 32dp-rounded surfaceVariant panel — a card at
 * half-screen scale, which also silently dimmed every value inside it. The weighted split
 * additionally left the timer region mostly empty while clipping the chart's day labels, and it
 * would have collapsed the same way the Dashboard's heatmap did at a large font scale.
 */
@Composable
private fun StudyIdleContent(
    scrollState: ScrollState,
    seconds: () -> Long,
    currentSubject: String,
    allSessions: List<StudySession>,
    dailyGoalMinutes: Int,
    todayTotalSeconds: Long,
    studyStreak: Int,
    pastDays: List<DayStudy>,
    onToggleTimer: () -> Unit,
    onPickSubject: () -> Unit,
    onManualEntry: (ManualSessionSeed) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = ApexSpacing.l),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(ApexSpacing.l))

        val goalSeconds = dailyGoalMinutes * 60L
        StudyTimerDisplay(
            seconds = seconds,
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
            SubjectSelectorChip(subject = currentSubject, onClick = onPickSubject)
            if (dailyGoalMinutes > 0 && studyStreak > 0) {
                StudyStreakChip(studyStreak)
            }
        }

        Spacer(Modifier.height(ApexSpacing.xl))
        StudyToggleButton(isRunning = false, onClick = onToggleTimer)

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
                IconButton(onClick = { onManualEntry(ManualSessionSeed(LocalDate.now().minusDays(1), "", 0L)) }) {
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
                onAction = { onManualEntry(ManualSessionSeed(LocalDate.now().minusDays(1), "", 0L)) }
            )
        } else {
            // A plain Column, not a LazyColumn: the page scrolls now, and a nested lazy list
            // inside a scrollable parent has no bounded height to work with.
            pastDays.forEachIndexed { i, day ->
                if (i > 0) ApexDivider()
                DayStudyItem(day, onEditSubject = { subject, seconds ->
                    onManualEntry(ManualSessionSeed(day.date, subject, seconds))
                })
            }
        }

        Spacer(Modifier.height(ApexSpacing.xxl))
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
    seconds: () -> Long,
    goalFraction: Float = 0f,
    goalLabel: String? = null
) {
    // There used to be two counter-rotating decorative rings behind this — a sweep-gradient arc
    // spinning one way and a faint circle spinning the other, carrying no information whatsoever.
    // They are the reference example of banned ambient motion in the design law. Deleted, and they
    // are not coming back as a flap: the split-flap clock below moves only when a digit changes,
    // and the gap between fields never blinks.
    //
    // The determinate goal arc that replaced them is gone too, for a geometric reason rather than a
    // design one: a 232dp ring cannot wrap a six-card flip row. The information it carried — today's
    // fraction of the daily goal — is now the hairline meter below, which reads as progress for the
    // same reason the arc did (it starts at one end and never loops) and reads better at a wide
    // aspect ratio.
    //
    // No isRunning parameter: this is StudyIdleContent's clock only, always paused. A running
    // session is StudyFocusContent's own ApexFlipClock(active = true) with no READY/FOCUSING
    // caption at all — see that composable.
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FlipClockFitToWidth {
            ApexFlipClock(seconds = seconds, active = false)
        }
        Spacer(Modifier.height(ApexSpacing.m))
        Text(
            text = stringResource(R.string.study_ready),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (goalLabel != null) {
            Spacer(modifier = Modifier.height(ApexSpacing.l))
            StudyGoalMeter(fraction = goalFraction, label = goalLabel)
        }
    }
}

/**
 * Today's progress toward the daily study goal: a hairline track with a fill, and the figure under
 * it. Replaces the circular arc — same information, an aspect ratio that sits under a wide clock.
 *
 * Bare Canvas and drawRect, matching StudyWeeklyChart and BudgetTrends — there is no progress
 * component and no chart library in this app. Deliberately **not** animated: the underlying total
 * advances every second while the timer runs, so the fill is already a continuous quantity, and an
 * animateFloatAsState on it would be motion carrying no information beyond the value itself (plus a
 * recomposition per second in whatever scope owned it). The missing ApexMotion reference here is the
 * decision, not an oversight.
 */
@Composable
private fun StudyGoalMeter(fraction: Float, label: String, modifier: Modifier = Modifier) {
    val track = MaterialTheme.colorScheme.outlineVariant
    val met = LocalApexSemantics.current.positive
    val ink = MaterialTheme.colorScheme.onSurface
    val filled = fraction.coerceIn(0f, 1f)
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // xs, not hairline: this screen already carries full-width 1dp ApexDividers, and at 0%
        // progress a 2dp track sitting directly under the eyebrow is indistinguishable from one of
        // them. A 4dp bar reads as a meter even when empty.
        Canvas(Modifier.fillMaxWidth().height(ApexSpacing.xs)) {
            drawRect(track)
            if (filled > 0f) {
                drawRect(
                    color = if (fraction >= 1f) met else ink,
                    size = Size(size.width * filled, size.height)
                )
            }
        }
        Spacer(Modifier.height(ApexSpacing.s))
        Text(
            text = label,
            style = ApexNumerals.small,
            color = if (fraction >= 1f) met else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The one start/pause control, shared by the idle screen and the focus surface so the press
 * behaviour and geometry exist in one place.
 *
 * Not full-width: a 64dp-tall edge-to-edge slab was the loudest thing on the screen, competing with
 * the timer it is subordinate to.
 */
@Composable
private fun StudyToggleButton(
    isRunning: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    ambient: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.97f else 1f, animationSpec = ApexMotion.snap(), label = "press")
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp).widthIn(min = 200.dp).scale(scale),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(ApexShapes.control),
        colors = ButtonDefaults.buttonColors(
            containerColor = when {
                ambient -> GraphiteBase
                isRunning -> MaterialTheme.colorScheme.surfaceVariant
                else -> MaterialTheme.colorScheme.primary
            },
            contentColor = when {
                ambient -> FrostDim
                isRunning -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onPrimary
            }
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
                // The daily target, drawn behind the bars. Dashed because it is a reference line
                // rather than data — that distinction is the reason it stays dashed while
                // everything else is solid. Kept as one full-width Canvas rather than folded into
                // the per-bar bars below since it isn't associated with any single day.
                if (goalMinutes > 0) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
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
                // One Box per bar (rather than the single Canvas this used to be drawn with) so
                // each day can carry its own contentDescription — a TalkBack user swiping through
                // otherwise gets no per-day value at all, only the axis min/max (Issue #207).
                // Mirrors BudgetTrendsCard's per-bar Box pattern.
                Row(modifier = Modifier.fillMaxSize()) {
                    bars.forEach { (day, minutes) ->
                        val heightFraction = (minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
                        val dayName = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, locale)
                        val barLabel = stringResource(
                            R.string.study_trend_bar_cd,
                            dayName,
                            formatDurationCompact(minutes * 60_000L)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .semantics { contentDescription = barLabel },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Canvas(modifier = Modifier.fillMaxWidth(0.56f).fillMaxHeight(heightFraction)) {
                                drawRoundRect(
                                    color = if (day == today) accent else muted,
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
                                )
                            }
                        }
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

// formatTime() lived here — the stopwatch's HH:MM:SS / MM:SS string. It had exactly one caller, the
// timer readout, and the split-flap clock replaced it: flipClockGroups() in ui/design owns the field
// layout now, and it renders digits rather than a string. Deleted rather than left behind.

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
    var date by remember { mutableStateOf(seed.date) }
    var subject by remember { mutableStateOf(seed.subject) }
    var hours by remember { mutableStateOf((seed.seconds / 3600).toString()) }
    var minutes by remember { mutableStateOf(((seed.seconds % 3600) / 60).toString()) }
    var showDatePicker by remember { mutableStateOf(false) }
    // Today and later belong to the timer, so manual entries only go back to yesterday.
    val latestSelectableMillis = remember { LocalDate.now().minusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
    val selectableDates = remember(latestSelectableMillis) {
        object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= latestSelectableMillis
        }
    }

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
                    onClick = { showDatePicker = true },
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

    if (showDatePicker) {
        ApexDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it },
            selectableDates = selectableDates
        )
    }
}
