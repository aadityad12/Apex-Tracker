package com.example.apextracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    onManageGoals: () -> Unit,
    onOpenSettings: () -> Unit,
    signedIn: Boolean,
    isSyncing: Boolean,
    viewModel: DashboardViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    // Saved as a plain Int so it survives process death: -1 = Recent, 0 = Rolling12Months,
    // anything else = that calendar year. A sealed type isn't Parcelable without extra ceremony.
    var windowKey by rememberSaveable { mutableIntStateOf(-1) }
    val window = when (windowKey) {
        -1 -> HeatmapWindow.Recent
        0 -> HeatmapWindow.Rolling12Months
        else -> HeatmapWindow.Year(windowKey)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.dashboard_title), style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    ApexLogo(modifier = Modifier.padding(start = 16.dp).size(22.dp))
                },
                actions = {
                    if (signedIn) {
                        Icon(
                            imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                            contentDescription = stringResource(R.string.cd_sync_status),
                            tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 4.dp).size(20.dp)
                        )
                    }
                    IconButton(onClick = onManageGoals) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.dashboard_manage_goals))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.menu_settings))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        // A Column (not a LazyColumn): the heatmap takes whatever height is left and sizes its
        // cells to fit, so a full year is on screen with no page scrolling (Issue #128).
        // The page scrolls. It used to be a fixed viewport with the heatmap on weight(1f), which
        // meant that at a large font scale the scaled text consumed the column and the graph — the
        // whole point of the screen — was allocated zero height and disappeared. Sections now have
        // intrinsic heights and the page scrolls when it must; at the default font scale everything
        // still fits, so no scrollbar appears.
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ApexSpacing.l)
        ) {
            StreakHero(state.perfectStreak, state.loaded)
            Spacer(Modifier.height(ApexSpacing.l))
            ApexDivider()
            Spacer(Modifier.height(ApexSpacing.l))
            TodaySection(
                todayGoals = state.todayGoals,
                loaded = state.loaded,
                onToggle = viewModel::toggleTodayGoal,
                onManageGoals = onManageGoals
            )
            Spacer(Modifier.height(ApexSpacing.l))
            ApexDivider()
            Spacer(Modifier.height(ApexSpacing.l))
            HeatmapSection(
                state = state,
                window = window,
                onSelectWindow = { windowKey = it },
                onDayClick = { selectedDay = it }
            )
            Spacer(Modifier.height(ApexSpacing.l))
        }
    }

    selectedDay?.let { day ->
        DayDetailSheet(
            date = day,
            state = state,
            onToggle = { goal -> viewModel.toggleGoalForDate(goal, day) },
            onDismiss = { selectedDay = null }
        )
    }
}

/**
 * The screen's thesis: the streak, at hero size.
 *
 * The count is Geist Mono (every quantity is) and the unit beside it is Instrument Serif — the
 * mixed pair is deliberate, and it is the only place the display face appears on the home screen.
 * Baseline-aligned via Alignment.Bottom so the two faces sit on one line despite very different
 * cap heights.
 */
@Composable
private fun StreakHero(streak: Int, loaded: Boolean) {
    // Don't assert "no streak" before Room has emitted — same gate TodaySection uses (Issue #118).
    if (!loaded) {
        Spacer(Modifier.height(56.dp))
        return
    }
    if (streak > 0) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                streak.toString(),
                style = ApexNumerals.hero,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(ApexSpacing.m))
            Text(
                stringResource(R.string.dashboard_streak_unit_other),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = ApexSpacing.s)
            )
        }
    } else {
        Column {
            Text(
                stringResource(R.string.dashboard_streak_start),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                stringResource(R.string.dashboard_streak_start_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Today's checklist. No card: an eyebrow plus rows separated by hairlines.
 *
 * The old version was a rounded Surface with an accent-coloured label at the top — the exact
 * stacked-card shape this redesign exists to remove (see Design.md §5). Dropping it also reclaims
 * ~32dp of container padding, which goes to the graph.
 */
@Composable
private fun TodaySection(
    todayGoals: List<GoalStatus>,
    loaded: Boolean,
    onToggle: (Goal) -> Unit,
    onManageGoals: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        ApexSectionHeader(
            stringResource(R.string.dashboard_today),
            trailing = {
                if (todayGoals.isNotEmpty()) {
                    Text(
                        String.format(
                            stringResource(R.string.dashboard_today_progress),
                            todayGoals.count { it.satisfied },
                            todayGoals.size
                        ),
                        style = ApexNumerals.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
        if (todayGoals.isEmpty()) {
            // Only claim "no goals" once the Room flows have actually emitted — the seeded EMPTY
            // state would otherwise flash the empty message on launch (Issue #118).
            if (loaded) {
                ApexEmptyState(
                    message = stringResource(R.string.dashboard_no_goals),
                    actionLabel = stringResource(R.string.dashboard_add_goal),
                    onAction = onManageGoals
                )
            }
        } else {
            todayGoals.forEachIndexed { i, status ->
                if (i > 0) ApexDivider()
                GoalStatusRow(status, onToggle)
            }
        }
    }
}

/**
 * One goal's row: an interactive checkbox (MANUAL) or a read-only computed status (AUTO).
 *
 * A satisfied goal reads Sage, not Ember. Ember is emphasis — the streak, selection, the FAB —
 * and using it for "met" as well would leave the screen unable to distinguish "notable" from
 * "good" (Design.md §3).
 */
@Composable
private fun GoalStatusRow(status: GoalStatus, onToggle: (Goal) -> Unit) {
    val goal = status.goal
    val isManual = goal.type == GoalType.MANUAL
    val met = LocalApexSemantics.current.positive
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isManual) Modifier.clickable { onToggle(goal) } else Modifier)
            // 48dp is the minimum *target*; extra vertical padding on top of it just spends
            // height the graph needs.
            .heightIn(min = 48.dp)
            .padding(vertical = ApexSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isManual) {
            Checkbox(
                checked = status.satisfied,
                onCheckedChange = { onToggle(goal) },
                colors = CheckboxDefaults.colors(checkedColor = met)
            )
        } else {
            Icon(
                if (status.satisfied) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (status.satisfied) met else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = ApexSpacing.m).size(24.dp)
            )
        }
        Spacer(Modifier.width(ApexSpacing.xs))
        Column(Modifier.weight(1f)) {
            Text(
                goal.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!isManual) {
                Text(
                    goalRuleText(goal),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isManual) {
            Text(
                text = if (status.satisfied) stringResource(R.string.dashboard_auto_met)
                else stringResource(R.string.dashboard_auto_unmet),
                style = MaterialTheme.typography.labelMedium,
                color = if (status.satisfied) met else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Cell size is a constant, not a function of available height. Deriving it from height is what
// produced a 7dp cell in a 79dp-wide grid: 53 rows simply do not fit a portrait phone. The window
// now decides how far the grid scrolls instead — see HeatmapWindow.
private val GUTTER_WIDTH = 36.dp
private val MAX_CELL = 30.dp
private val MIN_CELL = 14.dp
private val MONTH_LABEL_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM")
private val HEATCELL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")

@Composable
private fun HeatmapSection(
    state: DashboardUiState,
    window: HeatmapWindow,
    onSelectWindow: (Int) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = state.today
    val (rangeStart, rangeEnd) = remember(window, today) { heatmapRange(window, today) }
    val weeks = remember(state, rangeStart, rangeEnd) {
        heatmapWeeks(rangeStart, rangeEnd) { date -> state.dayCell(date) }
    }
    val years = remember(state.earliestGoalStart, today) { heatmapYears(state.earliestGoalStart, today) }

    // Sunday-first to match the rows heatmapWeeks builds, but the letters come from the locale
    // rather than a hardcoded English list (Issue #120).
    val locale = LocalLocale.current.platformLocale
    val weekdayLetters = remember(locale) {
        (0L..6L).map { DayOfWeek.SUNDAY.plus(it).getDisplayName(TextStyle.NARROW, locale) }
    }

    // A year is ~365 cells. Resolving string resources per cell (and giving each its own ripple)
    // made the first composition heavy enough to ANR on an emulator, so the templates are hoisted
    // and formatted per cell, and the cells share one interaction source with no indication.
    val labels = HeatCellLabels(
        dayFormat = stringResource(R.string.cd_dashboard_day),
        todayFormat = stringResource(R.string.cd_dashboard_day_today),
        untracked = stringResource(R.string.cd_dashboard_day_untracked),
        percentFormat = stringResource(R.string.cd_dashboard_day_percent),
        action = stringResource(R.string.cd_dashboard_day_action)
    )
    val interactionSource = remember { MutableInteractionSource() }
    // Resolve the ramp once, not once per cell — ~371 MaterialTheme reads was part of the ANR.
    val ramp = cellColorRamp()

    Column(modifier.fillMaxWidth()) {
        // The chips are too wide to live in the header's trailing slot — as one they squeezed
        // "CONSISTENCY" onto two lines and collided with it.
        ApexSectionHeader(stringResource(R.string.dashboard_consistency))
        Spacer(Modifier.height(ApexSpacing.s))
        WindowSelector(years, window, onSelectWindow)
        Spacer(Modifier.height(ApexSpacing.m))

        // Width-derived only: this sits inside a vertically-scrolling parent, where maxHeight is
        // unbounded, so there is no height to divide by. The result is a constant, legible cell —
        // which is the property that matters (a height-derived cell is what produced the old 7dp
        // grid) — and the window decides how tall the grid gets.
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val cellSize = ((maxWidth - GUTTER_WIDTH) / 7).coerceIn(MIN_CELL, MAX_CELL)
            val gridWidth = GUTTER_WIDTH + cellSize * 7

            Column(Modifier.width(gridWidth)) {
                // Height comes from the text, not a constant: a fixed height let the letters
                // overlap the first row of cells at a large font scale.
                Row(Modifier.fillMaxWidth()) {
                    Spacer(Modifier.width(GUTTER_WIDTH))
                    weekdayLetters.forEach { letter ->
                        Text(
                            letter,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column {
                    weeks.forEach { week ->
                        val monthLabel = week.firstOrNull { it?.date?.dayOfMonth == 1 }?.date
                            ?.format(MONTH_LABEL_FORMAT)?.uppercase() ?: ""
                        // Fixed row height: a month label must not inflate the twelve rows it
                        // lands on.
                        Row(Modifier.fillMaxWidth().height(cellSize), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.width(GUTTER_WIDTH), contentAlignment = Alignment.CenterEnd) {
                                if (monthLabel.isNotEmpty()) {
                                    Text(
                                        monthLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        modifier = Modifier.padding(end = ApexSpacing.s)
                                    )
                                }
                            }
                            week.forEach { cell ->
                                HeatCell(cell, today, labels, ramp, interactionSource, Modifier.size(cellSize), onDayClick)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(ApexSpacing.s))
        HeatmapLegend()
    }
}

/** Window chips: the short default, the rolling year, then one per calendar year with history. */
@Composable
private fun WindowSelector(years: List<Int>, window: HeatmapWindow, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)
    ) {
        WindowChip(stringResource(R.string.dashboard_window_recent), window is HeatmapWindow.Recent) { onSelect(-1) }
        WindowChip(stringResource(R.string.dashboard_window_rolling), window is HeatmapWindow.Rolling12Months) { onSelect(0) }
        years.forEach { year ->
            WindowChip(year.toString(), window is HeatmapWindow.Year && window.year == year) { onSelect(year) }
        }
    }
}

@Composable
private fun WindowChip(label: String, selected: Boolean, onClick: () -> Unit) {
    // M3 defaults a selected FilterChip to secondaryContainer and to a pill; both are overridden
    // here for the same reasons documented in Design.md §4.
    FilterChip(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(ApexShapes.control),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
    )
}

/** Pre-resolved accessibility strings, so a 365-cell grid resolves them once, not once per cell. */
private data class HeatCellLabels(
    val dayFormat: String,
    val todayFormat: String,
    val untracked: String,
    val percentFormat: String,
    val action: String
)

@Composable
private fun HeatCell(
    cell: DayCell?,
    today: LocalDate,
    labels: HeatCellLabels,
    ramp: List<Color>,
    interactionSource: MutableInteractionSource,
    modifier: Modifier,
    onDayClick: (LocalDate) -> Unit
) {
    // Empty padding cell: a bare spacer, no decoration/semantics — the majority of a full-year
    // grid, so keeping it to one trivial node matters for first-composition cost.
    if (cell == null) {
        Box(modifier)
        return
    }
    val isToday = cell.date == today
    // The cell has no text content of its own, so TalkBack needs the date and completion state
    // spelled out — tapping a cell is the only way into the day sheet (Issue #106).
    val label = remember(cell, isToday, labels) {
        val dateText = cell.date.format(HEATCELL_DATE_FORMAT)
        val state = cell.fraction?.let { String.format(labels.percentFormat, (it * 100).roundToInt()) }
            ?: labels.untracked
        String.format(labels.dayFormat, if (isToday) String.format(labels.todayFormat, dateText) else dateText, state)
    }
    val borderColor = MaterialTheme.colorScheme.primary
    // One Box per cell (padding folded in via a smaller inset), not two — halves the grid's node
    // count. Ramp colours are pre-resolved by the caller.
    Box(
        modifier
            .padding(1.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(rampColor(ramp, cell.bucket))
            .then(
                if (isToday) Modifier.border(1.5.dp, borderColor, RoundedCornerShape(4.dp))
                else Modifier
            )
            // No indication: a ripple instance per cell is part of what made this grid expensive.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClickLabel = labels.action
            ) { onDayClick(cell.date) }
            .semantics { contentDescription = label }
    )
}

@Composable
private fun HeatmapLegend() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.dashboard_legend_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        val ramp = cellColorRamp()
        (0..4).forEach { bucket ->
            Box(
                Modifier
                    .padding(horizontal = 2.dp)
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(rampColor(ramp, bucket))
            )
        }
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.dashboard_legend_more), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Tap-a-day sheet: that day's goal breakdown, with MANUAL goals editable (backfill). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailSheet(
    date: LocalDate,
    state: DashboardUiState,
    onToggle: (Goal) -> Unit,
    onDismiss: () -> Unit
) {
    val statuses = state.dayGoalStatuses(date)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            if (statuses.isEmpty()) {
                Text(stringResource(R.string.dashboard_day_no_goals), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                statuses.forEach { status -> GoalStatusRow(status, onToggle) }
            }
        }
    }
}

/**
 * The heatmap intensity ramp, indexed by bucket+1 (index 0 = the -1 "no goals" neutral, then 0..4
 * deepening toward a perfect day). Resolved once per heatmap render — see [rampColor] — rather than
 * reading MaterialTheme inside every one of ~371 cells.
 *
 * This used to alpha-modulate `colorScheme.primary` at 10/35/55/78/100%. Two problems, both
 * confirmed on device: alpha-over-a-dark-background compresses the low end so hard that the whole
 * grid nearly disappeared, and the middle steps did not separate from each other. The ramp is now
 * an explicit six-step scale in ApexPalette that gains chroma and lightness together.
 */
@Composable
private fun cellColorRamp(): List<Color> = LocalApexSemantics.current.heatRamp

private fun rampColor(ramp: List<Color>, bucket: Int): Color = ramp[(bucket + 1).coerceIn(0, 5)]
