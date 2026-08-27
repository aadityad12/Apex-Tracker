package com.example.apextracker

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apextracker.ui.design.ApexChartFrame
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexStatRow
import com.example.apextracker.ui.design.LocalApexSemantics
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetTrackerApp(onBackToMenu: () -> Unit, viewModel: BudgetViewModel = viewModel()) {
    val items by viewModel.allItems.collectAsState(initial = emptyList())
    val categories by viewModel.allCategories.collectAsState(initial = emptyList())
    val subscriptions by viewModel.allSubscriptions.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<BudgetItem?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showCalendar by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val overallLimit by viewModel.overallMonthlyLimit.collectAsState(initial = null)
    var isSearching by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    // Selected month is shared between the list and calendar views so toggling
    // doesn't jump the user to a different month.
    var selectedMonth by rememberSaveable(stateSaver = YearMonthSaver) { mutableStateOf(YearMonth.now()) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.budget_search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )
                    } else {
                        Text(stringResource(R.string.budget_title), 
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            viewModel.setSearchQuery("")
                        } else onBackToMenu()
                    }) {
                        Icon(
                            if (isSearching) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Home,
                            contentDescription = if (isSearching) stringResource(R.string.cd_back) else stringResource(R.string.cd_home)
                        )
                    }
                },
                actions = {
                    if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                        }
                    }
                    IconButton(onClick = { showCalendar = !showCalendar }) {
                        Icon(
                            if (showCalendar) Icons.Default.ViewAgenda else Icons.Default.CalendarMonth,
                            contentDescription = if (showCalendar) stringResource(R.string.cd_show_list) else stringResource(R.string.cd_show_calendar),
                            tint = if (showCalendar) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.menu_settings))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_item))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
        ) {
            if (showCalendar) {
                // Like the list view, the calendar only shows BudgetItems — pending
                // future subscription renewals aren't items yet, so they don't appear.
                BudgetCalendarView(
                    items = items,
                    categories = categories,
                    currentMonth = selectedMonth,
                    onMonthChange = { selectedMonth = it }
                )
            } else {
                BudgetOverview(
                    items, categories, subscriptions,
                    selectedMonth = selectedMonth,
                    onMonthChange = { selectedMonth = it },
                    onEdit = { itemToEdit = it },
                    searchQuery = searchQuery,
                    overallLimit = overallLimit
                )
            }
        }

        if (showAddDialog) {
            BudgetItemDialog(
                title = stringResource(R.string.budget_add_item_title),
                categories = categories,
                onDismiss = { showAddDialog = false },
                onConfirm = { title, amount, description, date, categoryId, type ->
                    viewModel.addItem(title, amount, description, date, categoryId, type)
                    showAddDialog = false
                }
            )
        }

        if (itemToEdit != null) {
            BudgetItemDialog(
                title = stringResource(R.string.budget_edit_item_title),
                initialTitle = budgetItemBaseTitle(itemToEdit!!.title),
                initialAmount = itemToEdit!!.amount.toString(),
                initialDescription = itemToEdit!!.description ?: "",
                initialDate = itemToEdit!!.date,
                initialCategoryId = itemToEdit!!.categoryId,
                initialType = itemToEdit!!.type,
                categories = categories,
                onDismiss = { itemToEdit = null },
                onConfirm = { title, amount, description, date, categoryId, type ->
                    viewModel.updateItem(itemToEdit!!.copy(
                        title = title,
                        amount = amount,
                        description = description,
                        date = date,
                        categoryId = categoryId,
                        type = type
                    ))
                    itemToEdit = null
                },
                onDelete = {
                    val deleted = itemToEdit!!
                    viewModel.deleteItem(deleted)
                    itemToEdit = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.deleted_quoted, deleted.title),
                            actionLabel = resources.getString(R.string.action_undo),
                            duration = SnackbarDuration.Short
                        )
                        // The cloud delete has already been pushed; undoing re-pushes
                        // the same cloudId, which recreates the doc.
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreItem(deleted)
                        }
                    }
                }
            )
        }

        if (showSettingsDialog) {
            BudgetSettingsDialog(
                categories = categories,
                allItems = items,
                currentMonth = selectedMonth,
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

// YearMonth isn't Parcelable, so rememberSaveable needs an explicit saver.
private val YearMonthSaver = Saver<YearMonth, String>(
    save = { it.toString() },
    restore = { YearMonth.parse(it) }
)

@Composable
fun BudgetOverview(
    items: List<BudgetItem>,
    categories: List<Category>,
    subscriptions: List<Subscription>,
    selectedMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onEdit: (BudgetItem) -> Unit,
    searchQuery: String = "",
    overallLimit: Double? = null
) {
    val availableMonths = items.map { YearMonth.from(it.date) }.distinct().sortedDescending()
    val monthToDisplay = if (availableMonths.contains(selectedMonth)) selectedMonth 
                         else availableMonths.firstOrNull() ?: selectedMonth

    val monthItems = items.filter { YearMonth.from(it.date) == monthToDisplay }
    // Only the transactions list narrows to the query — the totals, pie, limits and trend chart
    // keep describing the whole month, which is what those summaries are for (Issue #123).
    val categoryNames = categories.associate { it.id to it.name }
    val visibleItems = filterBudgetItems(monthItems, categoryNames, searchQuery)

    val pendingSubs = if (monthToDisplay == YearMonth.now()) {
        subscriptions.filter { sub ->
            !sub.isPaused &&
                YearMonth.from(sub.renewalDate) == monthToDisplay && sub.renewalDate.isAfter(LocalDate.now()) &&
                matchesQuery(searchQuery, sub.name, sub.notes)
        }
    } else emptyList()

    // Every "spending" figure below — the hero total, the pie, the trend chart, the limits card —
    // excludes income (Issue #218): a paycheck isn't spend, and it has no category to slice by.
    // Net balance is the one place income and expense combine.
    val monthExpenseItems = monthItems.expensesOnly()
    val totalExpenditure = monthExpenseItems.sumOf { it.amount }
    val totalIncome = monthItems.filterNot { it.isExpense }.sumOf { it.amount }
    val netBalance = totalIncome - totalExpenditure
    val expenseItems = items.expensesOnly()

    Column(modifier = Modifier.fillMaxSize()) {
        MonthSelectorCompact(
            currentMonth = monthToDisplay,
            onMonthChange = onMonthChange
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = ApexSpacing.l,
                end = ApexSpacing.l,
                top = ApexSpacing.s,
                // Scaffold's innerPadding does not reserve space for a floating action button, so
                // without this the last row — or the trend chart's month labels, for a month with no
                // transactions — sits underneath the FAB and cannot be read or tapped.
                bottom = ApexSpacing.xxl + ApexSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl)
        ) {
            item {
                MonthTotal(totalExpenditure, totalIncome, netBalance, monthExpenseItems.size)
            }

            if (monthExpenseItems.isNotEmpty() || pendingSubs.isNotEmpty()) {
                item {
                    ExpenseBreakdown(monthExpenseItems, categories, pendingSubs)
                }
            }

            // Sits above trends: a cap the user set is more actionable than history.
            // Gated here as well as inside the card so the arrangement's spacing doesn't leave a
            // gap for users who have never set a limit.
            if (categories.any { it.effectiveMonthlyLimit() != null } || overallLimit != null) {
                item {
                    BudgetLimitsCard(
                        items = expenseItems,
                        categories = categories,
                        month = monthToDisplay,
                        overallLimit = overallLimit
                    )
                }
            }

            item {
                BudgetTrendsCard(items = expenseItems, selectedMonth = monthToDisplay, onMonthSelected = onMonthChange)
            }

            if (visibleItems.isNotEmpty() || pendingSubs.isNotEmpty()) {
                val sortedItems = visibleItems.sortedByDescending { it.date }

                item {
                    ApexSectionHeader(stringResource(R.string.budget_transactions))
                }

                items(pendingSubs.sortedBy { it.renewalDate }) { sub ->
                    val category = subscriptionsCategory()
                    BudgetListItem(
                        BudgetItem(title = sub.name, amount = sub.amount, date = sub.renewalDate, categoryId = -1L),
                        category,
                        onClick = {},
                        isPending = true
                    )
                }

                items(sortedItems) { item ->
                    val category = if (item.categoryId == -1L) {
                        subscriptionsCategory()
                    } else {
                        categories.find { it.id == item.categoryId }
                    }
                    BudgetListItem(item, category, onClick = { onEdit(item) })
                }
            } else {
                item {
                    // An empty month is an invitation to act, not a dead end — so the no-data case
                    // names the button that fills it. The no-results case deliberately does not:
                    // the fix there is changing the query, not adding an expense.
                    ApexEmptyState(
                        message = if (searchQuery.isNotBlank() && monthItems.isNotEmpty()) {
                            stringResource(R.string.budget_search_no_results, searchQuery)
                        } else {
                            stringResource(R.string.budget_no_data)
                        }
                    )
                }
            }
        }
    }
}

/**
 * The month's headline figure. Replaces `SummaryCardModern`, which put this number inside a 24dp
 * `primaryContainer` card with a decorative 48dp accent circle beside it and the pie chart nested
 * underneath — the top of a vertical run of four cards, which is the shape this redesign removes.
 *
 * The number carries the screen on its own now: [ApexNumerals.hero] is Geist Mono, so the figure is
 * tabular and does not reflow as the month changes.
 *
 * Income/net (Issue #218) only render once the user has ever logged income — a pure
 * expense-tracking user sees byte-for-byte the same card as before. `total` and `itemCount` stay
 * expense-only so "Total spent · N transactions" keeps meaning exactly what it says.
 */
@Composable
private fun MonthTotal(total: Double, income: Double, net: Double, itemCount: Int) {
    Column {
        Text(
            text = formatCurrency(total, LocalCurrencyCode.current),
            style = ApexNumerals.hero,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = pluralStringResource(R.plurals.budget_total_transactions, itemCount, itemCount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (income > 0.0) {
            Spacer(Modifier.height(ApexSpacing.s))
            ApexStatRow(
                label = stringResource(R.string.budget_total_income),
                value = formatCurrency(income, LocalCurrencyCode.current),
                valueColor = LocalApexSemantics.current.positive
            )
            ApexStatRow(
                label = stringResource(R.string.budget_net_balance),
                value = formatCurrency(net, LocalCurrencyCode.current),
                valueColor = if (net >= 0.0) LocalApexSemantics.current.positive else MaterialTheme.colorScheme.error
            )
        }
    }
}

/**
 * Month stepper. De-carded — it used to be a tinted 16dp container, which read as a control panel
 * above the content rather than part of it.
 *
 * The month name is a localized pattern rather than `.uppercase()` on a hand-built string: the label
 * is a date, not an eyebrow, and upper-casing a localized month name is wrong in several languages.
 */
@Composable
fun MonthSelectorCompact(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    val locale = LocalLocale.current.platformLocale
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ApexSpacing.s, vertical = ApexSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_prev))
        }

        Text(
            text = currentMonth.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )

        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next))
        }
    }
}

/** One slice: an entity, its share, and the colour that belongs to that entity. */
private data class Slice(val label: String, val amount: Double, val color: Color)

/** Slices beyond this many fold into a single "Other" row rather than being hidden. */
private const val MAX_NAMED_SLICES = 7

private val DONUT_SIZE = 108.dp
private val DONUT_STROKE = 18.dp
private val LEGEND_DOT = 10.dp

/**
 * Where the month's money went: the spec'd donut plus a legend that names **every** slice.
 *
 * The legend is not decoration and not optional. `Design.md` §6 records that three of the eight
 * palette hues fall to 2.76–2.92:1 against the light-mode paper — a non-dismissable warning from the
 * palette validator — and the relief that makes that acceptable is direct text labels. A legend-less
 * or partially-labelled donut violates the spec. The old legend showed the top four and collapsed
 * the rest into "+3 more", so the remainder had no label at all; the tail now folds into an explicit
 * **Other** slice, which is a named entity with its own row.
 *
 * Also fixed here: the labels used to be painted in `onPrimaryContainer` inside a tinted card, and
 * the transaction rows painted the category name in the category's own colour. Text wears text
 * tokens; the dot beside it carries identity (`Design.md` §6).
 */
@Composable
private fun ExpenseBreakdown(
    items: List<BudgetItem>,
    categories: List<Category>,
    pendingSubs: List<Subscription>
) {
    val totalExpenses = items.sumOf { it.amount }
    val totalPending = pendingSubs.sumOf { it.amount }
    val totalCombined = totalExpenses + totalPending
    if (totalCombined == 0.0) return

    // Hoisted out of the loop: it reads MaterialTheme, and the value is identical for every -1L item.
    val subsCategory = subscriptionsCategory()
    // Legend labels are user-facing text, so they go through strings.xml like the rest of the
    // screen (Issue #114) rather than being baked in as English literals.
    val uncategorizedLabel = stringResource(R.string.budget_uncategorized)
    val pendingLabel = stringResource(R.string.budget_pending_legend)
    val otherLabel = stringResource(R.string.budget_other_categories)
    val mutedColor = LocalApexSemantics.current.chartMuted

    val named = items.groupBy { it.categoryId }.map { (catId, catItems) ->
        val category = if (catId == -1L) subsCategory else categories.find { it.id == catId }
        Slice(
            label = category?.name ?: uncategorizedLabel,
            amount = catItems.sumOf { it.amount },
            // Via categoryColorOf, so a category still holding a legacy pastel hex renders as its
            // palette slot instead of as itself.
            color = category?.let { categoryColorOf(it.colorHex) } ?: mutedColor
        )
    }.sortedByDescending { it.amount }

    val slices = buildList {
        addAll(named.take(MAX_NAMED_SLICES))
        // Rank decides which entities get their own row, but never repaints the ones that do — the
        // survivors keep their own colour (Design.md §6).
        if (named.size > MAX_NAMED_SLICES) {
            add(Slice(otherLabel, named.drop(MAX_NAMED_SLICES).sumOf { it.amount }, mutedColor))
        }
        // Pending renewals are money not yet spent, so they read as muted rather than as a category.
        if (totalPending > 0) add(Slice(pendingLabel, totalPending, mutedColor))
    }

    ApexChartFrame(stringResource(R.string.budget_where_it_went)) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Donut(slices, totalCombined)
            Spacer(Modifier.height(ApexSpacing.l))
        }
        slices.forEach { slice ->
            LegendRow(slice, totalCombined)
        }
    }
}

/**
 * The ring. **No centre label** — it used to show `spentExpenses / combinedTotal`, which is 100%
 * whenever there are no pending renewals, i.e. almost always. A number that reads "100%" on a chart
 * showing seven categories is worse than no number, and every share the reader might want is in the
 * legend directly below with its own text label.
 */
@Composable
private fun Donut(slices: List<Slice>, total: Double) {
    // A 2dp gap of surface between adjacent fills, per the chart spec. Expressed as an angle so it
    // stays 2dp of arc regardless of the ring's radius.
    val gapDegrees = 2f
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(DONUT_SIZE)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = DONUT_STROKE.toPx()
            val inset = stroke / 2f
            var startAngle = -90f
            slices.forEach { slice ->
                val sweep = (slice.amount / total).toFloat() * 360f
                // A slice narrower than the gap would render as nothing at all; give it the gap's
                // width rather than dropping it, so a tiny category is still visible.
                val drawn = (sweep - gapDegrees).coerceAtLeast(gapDegrees / 2f)
                drawArc(
                    color = slice.color,
                    startAngle = startAngle,
                    sweepAngle = drawn,
                    useCenter = false,
                    style = Stroke(width = stroke, cap = StrokeCap.Butt),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - stroke, size.height - stroke)
                )
                startAngle += sweep
            }
        }
    }
}

@Composable
private fun LegendRow(slice: Slice, total: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(LEGEND_DOT).clip(CircleShape).background(slice.color))
        Spacer(modifier = Modifier.width(ApexSpacing.m))
        Text(
            text = slice.label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(ApexSpacing.s))
        Text(
            text = formatPercent(slice.amount / total),
            style = ApexNumerals.small,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(ApexSpacing.m))
        Text(
            text = formatCurrency(slice.amount, LocalCurrencyCode.current),
            style = ApexNumerals.small,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
