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
import androidx.compose.material.icons.outlined.AutoAwesome
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
import com.example.apextracker.ui.design.ApexGroup
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardView(
    onManageGoals: () -> Unit,
    onOpenSettings: () -> Unit,
    signedIn: Boolean,
    isSyncing: Boolean,
    viewModel: DashboardViewModel = viewModel(),
    tipViewModel: ApexTipViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val tipState by tipViewModel.uiState.collectAsState()
    val currencyCode = LocalCurrencyCode.current
    val tipSnapshot = remember(state, currencyCode) {
        state.takeIf { it.loaded }?.toApexTipSnapshot(currencyCode)
    }
    LaunchedEffect(tipSnapshot) {
        tipSnapshot?.let(tipViewModel::updateSnapshot)
    }
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
                    ApexLogo(modifier = Modifier.padding(start = ApexSpacing.l).size(22.dp))
                },
                actions = {
                    if (signedIn) {
                        Icon(
                            imageVector = if (isSyncing) Icons.Default.CloudSync else Icons.Default.CloudDone,
                            contentDescription = stringResource(R.string.cd_sync_status),
                            tint = if (isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = ApexSpacing.xs).size(20.dp)
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
            DailyApexTipSection(
                state = tipState,
                onEnable = { tipViewModel.setEnabled(true) },
                onDisable = { tipViewModel.setEnabled(false) },
                onRetry = tipViewModel::retry
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

/** Opt-in AI summary. The disclosure is shown before the first request, beside the enable action. */
@Composable
private fun DailyApexTipSection(
    state: ApexTipUiState,
    onEnable: () -> Unit,
    onDisable: () -> Unit,
    onRetry: () -> Unit
) {
    if (!state.loaded) return

    Column(Modifier.fillMaxWidth()) {
        ApexSectionHeader(
            stringResource(R.string.apex_tip_title),
            trailing = {
                if (state.enabled) {
                    TextButton(onClick = onDisable) {
                        Text(stringResource(R.string.apex_tip_turn_off))
                    }
                }
            }
        )
        Spacer(Modifier.height(ApexSpacing.s))
        ApexGroup {
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(ApexSpacing.m))
                Column(Modifier.weight(1f)) {
                    when {
                        !state.enabled -> {
                            Text(
                                stringResource(R.string.apex_tip_opt_in),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(ApexSpacing.s))
                            Text(
                                stringResource(R.string.apex_tip_privacy),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(ApexSpacing.m))
                            Button(onClick = onEnable) {
                                Text(stringResource(R.string.apex_tip_enable))
                            }
                        }
                        state.loading -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(ApexSpacing.xl),
                                    strokeWidth = ApexSpacing.hairline
                                )
                                Spacer(Modifier.width(ApexSpacing.m))
                                Text(
                                    stringResource(R.string.apex_tip_loading),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        state.text != null -> {
                            Text(state.text, style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(ApexSpacing.s))
                            Text(
                                stringResource(R.string.apex_tip_attribution),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        state.failed -> {
                            Text(
                                stringResource(R.string.apex_tip_failed),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(ApexSpacing.s))
                            TextButton(onClick = onRetry) {
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    }
                }
            }
        }
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
    // A spinner bridges the gap instead of blank space (Issue #215) — same visual language as
    // DailyApexTipSection's own loading row below.
    if (!loaded) {
        Box(modifier = Modifier.height(56.dp), contentAlignment = Alignment.CenterStart) {
            CircularProgressIndicator(
                modifier = Modifier.size(ApexSpacing.xl),
                strokeWidth = ApexSpacing.hairline
            )
        }
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
            // state would otherwise flash the empty message on launch (Issue #118). Before that, a
            // spinner bridges the gap instead of rendering nothing under the header (Issue #215).
            if (loaded) {
                ApexEmptyState(
                    message = stringResource(R.string.dashboard_no_goals),
                    actionLabel = stringResource(R.string.dashboard_add_goal),
                    onAction = onManageGoals
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.l),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(ApexSpacing.xl),
                        strokeWidth = ApexSpacing.hairline
                    )
                }
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
 * A satisfied goal reads full ink, same as the streak and every other emphasised figure
 * (2026-08-11 — there is no separate "met" hue any more). The checkbox/circle shape plus the
 * label text ("Met") are what carry the distinction, not colour.
 */
@Composable
private fun GoalStatusRow(status: GoalStatus, onToggle: (Goal) -> Unit) {
    val goal = status.goal
    val isManual = goal.type == GoalType.MANUAL
    // Met is ink, not a hue (2026-08-11). The checkbox/filled-circle shape is the signal.
    val met = MaterialTheme.colorScheme.onSurface
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
                onCheckedChange = { onToggle(goal) }
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
            if (!isManual || goal.cadence == GoalCadence.WEEKLY) {
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
    val locale = LocalLocale.current.platformLocale
    // The grid's week rows anchor on the locale's real first day of week (Issue #160) — most of
    // Europe and elsewhere is Monday-first, and a Sunday-first grid for those locales doesn't
    // match how the reader's own calendar is laid out.
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val (rangeStart, rangeEnd) = remember(window, today, firstDayOfWeek) { heatmapRange(window, today, firstDayOfWeek) }
    val weeks = remember(state, rangeStart, rangeEnd, firstDayOfWeek) {
        heatmapWeeks(rangeStart, rangeEnd, firstDayOfWeek) { date -> state.dayCell(date) }
    }
    val years = remember(state.earliestGoalStart, today) { heatmapYears(state.earliestGoalStart, today) }

    // Letters follow the same locale-derived first day as the rows above; the letters themselves
    // still come from the locale rather than a hardcoded English list (Issue #120).
    val weekdayLetters = remember(locale, firstDayOfWeek) {
        (0L..6L).map { firstDayOfWeek.plus(it).getDisplayName(TextStyle.NARROW, locale) }
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
    // Resolve heatmap colours once, not once per cell — ~371 MaterialTheme reads was part of the ANR.
    val semantics = LocalApexSemantics.current
    val heatColors = HeatCellColors(
        ramp = semantics.heatRamp,
        today = MaterialTheme.colorScheme.primary
    )

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
                                HeatCell(cell, today, labels, heatColors, interactionSource, Modifier.size(cellSize), onDayClick)
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

/** Pre-resolved cell colours, so ~371 cells don't each read MaterialTheme (part of the ANR). */
private data class HeatCellColors(
    /** Six steps, brightest = most goals met. Index 0 is a day with no goals at all. */
    val ramp: List<Color>,
    val today: Color // the outline marking today
)

/**
 * One heatmap cell: a filled square whose **brightness is the share of that day's goals met**
 * (2026-08-11), replacing the fill-height bar the Graphite redesign shipped.
 *
 * The bar encoded intensity as geometry to avoid the GitHub reading; the owner's call is that the
 * familiar reading is worth having, and with the app now fully monochrome a brightness ramp has
 * no hue to compete with. A day with no goals at all takes ramp[0] and stays visibly dimmer than
 * a day that had goals and met none of them — that distinction is the point of the graph, and it
 * is why the ramp was re-authored rather than reused (see ApexHeatRampDark).
 */
@Composable
private fun HeatCell(
    cell: DayCell?,
    today: LocalDate,
    labels: HeatCellLabels,
    colors: HeatCellColors,
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
    val cellShape = RoundedCornerShape(ApexShapes.cell)
    val frac = cell.fraction
    val fill = if (frac == null) colors.ramp[0] else colors.ramp[intensityBucket(frac) + 1]
    Box(
        modifier
            // 1dp, not ApexSpacing.hairline (2dp): cells range 14-30dp, and a 2dp gap on each
            // side would eat up to a quarter of the smallest cell's width across a 7-wide row.
            .padding(1.dp)
            .clip(cellShape)
            .background(fill)
            .then(
                if (isToday) Modifier.border(1.5.dp, colors.today, cellShape)
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
    // Less → more, as the ramp's own steps. Index 0 (untracked) is deliberately absent: the
    // legend explains the *scale*, and "no goals that day" is not a point on it.
    val ramp = LocalApexSemantics.current.heatRamp
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.dashboard_legend_less), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        // Ramp swatches at rising intensity — the same 6-step ramp the cells themselves use.
        ramp.drop(1).forEach { fill ->
            Box(
                Modifier
                    .padding(horizontal = ApexSpacing.hairline)
                    .size(14.dp)
                    .clip(RoundedCornerShape(ApexShapes.cell))
                    .background(fill)
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
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = ApexSpacing.xl)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(ApexSpacing.m))
            if (statuses.isEmpty()) {
                Text(stringResource(R.string.dashboard_day_no_goals), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                statuses.forEach { status -> GoalStatusRow(status, onToggle) }
            }
        }
    }
}
