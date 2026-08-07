package com.example.apextracker

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexDatePickerDialog
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.widget.refreshBudgetWidget
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// One-off component geometry; these are sizes/caps, not spacing-scale entries (Design.md §5).
private val BudgetSettingsContentMaxHeight = 400.dp
private val CategoryColorDotSize = 20.dp
private val ColorSwatchSize = 32.dp

@Composable
fun BudgetSettingsDialog(
    categories: List<Category>,
    allItems: List<BudgetItem>,
    currentMonth: YearMonth,
    viewModel: BudgetViewModel,
    onDismiss: () -> Unit
) {
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showOverallLimitDialog by remember { mutableStateOf(false) }
    val overallLimit by viewModel.overallMonthlyLimit.collectAsState(initial = null)
    val context = LocalContext.current
    val securitySettings = remember { SecuritySettings(context) }
    val budgetLocked by securitySettings.budgetLockEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (activeSubScreen != null) {
                    IconButton(onClick = { activeSubScreen = null }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
                Text(
                    text = when (activeSubScreen) {
                        "categories" -> stringResource(R.string.budget_manage_categories)
                        "subscriptions" -> stringResource(R.string.budget_manage_subscriptions)
                        else -> stringResource(R.string.budget_settings_title)
                    }
                )
            }
        },
        text = {
            Box(modifier = Modifier.fillMaxWidth().heightIn(max = BudgetSettingsContentMaxHeight)) {
                when (activeSubScreen) {
                    "categories" -> {
                        CategoriesView(
                            categories = categories,
                            onAdd = { name, color, limit -> viewModel.addCategory(name, color, limit) },
                            onUpdate = { viewModel.updateCategory(it) },
                            onDelete = { viewModel.deleteCategory(it) }
                        )
                    }
                    "subscriptions" -> {
                        SubscriptionsView(viewModel)
                    }
                    else -> {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)
                        ) {
                            BudgetSettingsItem(stringResource(R.string.budget_manage_categories)) { activeSubScreen = "categories" }
                            BudgetSettingsItem(stringResource(R.string.budget_manage_subscriptions)) { activeSubScreen = "subscriptions" }
                            BudgetSettingsItem(
                                stringResource(R.string.budget_overall_limit_setting),
                                value = overallLimit?.let { formatCurrency(it, LocalCurrencyCode.current) }
                                    ?: stringResource(R.string.budget_limit_none)
                            ) { showOverallLimitDialog = true }
                            BudgetSettingsItem(stringResource(R.string.budget_export_csv)) { showExportDialog = true }
                            HorizontalDivider()
                            ModuleLockSetting(
                                checked = budgetLocked,
                                titleRes = R.string.security_lock_budget_title,
                                onCheckedChange = {
                                    scope.launch {
                                        securitySettings.setBudgetLock(it)
                                        // The widget reads this flag to decide whether to show
                                        // figures at all (Issue #187). Without this, enabling the
                                        // lock leaves the last-rendered amounts sitting on the
                                        // launcher until something else happens to redraw it.
                                        refreshBudgetWidget(context)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_done))
            }
        }
    )

    if (showOverallLimitDialog) {
        OverallLimitDialog(
            current = overallLimit,
            onDismiss = { showOverallLimitDialog = false },
            onSave = {
                viewModel.setOverallMonthlyLimit(it)
                showOverallLimitDialog = false
            }
        )
    }

    if (showExportDialog) {
        BudgetExportScopeDialog(
            onDismiss = { showExportDialog = false },
            onExport = { scopeToCurrentMonth ->
                val exportItems = if (scopeToCurrentMonth) {
                    allItems.filter { YearMonth.from(it.date) == currentMonth }
                } else {
                    allItems
                }
                val csv = buildBudgetCsv(exportItems, categories)
                val fileName = if (scopeToCurrentMonth) "budget_$currentMonth.csv" else "budget_all_time.csv"
                shareCsv(context, csv, fileName)
                showExportDialog = false
            }
        )
    }
}

@Composable
fun BudgetExportScopeDialog(onDismiss: () -> Unit, onExport: (scopeToCurrentMonth: Boolean) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_export_scope_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                Button(onClick = { onExport(true) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.budget_export_current_month))
                }
                Button(onClick = { onExport(false) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.budget_export_all_time))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
fun BudgetSettingsItem(label: String, value: String? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(ApexSpacing.l).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(ApexSpacing.s))
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CategoriesView(
    categories: List<Category>,
    onAdd: (String, String, Double?) -> Unit,
    onUpdate: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.budget_create_category))
        }
        Spacer(modifier = Modifier.height(ApexSpacing.s))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            items(categories) { category ->
                CategoryItem(
                    category = category,
                    onEdit = { categoryToEdit = category }
                )
            }
        }
    }

    if (showAddDialog) {
        CategoryDialog(
            title = stringResource(R.string.budget_new_category),
            // Pre-select a slot nothing else is using, so a run of new categories doesn't all come
            // out the same colour.
            initialColor = nextCategoryHex(categories.map { it.colorHex }),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, color, limit -> onAdd(name, color, limit) }
        )
    }

    if (categoryToEdit != null) {
        CategoryDialog(
            title = stringResource(R.string.budget_edit_category),
            initialName = categoryToEdit!!.name,
            initialColor = categoryToEdit!!.colorHex,
            initialLimit = categoryToEdit!!.monthlyLimit,
            onDismiss = { categoryToEdit = null },
            onConfirm = { name, color, limit ->
                onUpdate(categoryToEdit!!.copy(name = name, colorHex = color, monthlyLimit = limit))
                categoryToEdit = null
            },
            onDelete = {
                onDelete(categoryToEdit!!)
                categoryToEdit = null
            }
        )
    }
}

@Composable
fun CategoryItem(category: Category, onEdit: () -> Unit) {
    Surface(
        onClick = onEdit,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(ApexSpacing.m).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.size(CategoryColorDotSize).background(categoryColorOf(category.colorHex), CircleShape))
                Spacer(modifier = Modifier.width(ApexSpacing.m))
                Column {
                    Text(category.name, style = MaterialTheme.typography.bodyMedium)
                    val limit = category.effectiveMonthlyLimit()
                    Text(
                        text = if (limit != null) {
                            stringResource(R.string.budget_limit_summary, formatCurrency(limit, LocalCurrencyCode.current))
                        } else {
                            stringResource(R.string.budget_limit_none)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.cd_edit), tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CategoryDialog(
    title: String,
    initialName: String = "",
    initialColor: String? = null,
    initialLimit: Double? = null,
    onDismiss: () -> Unit,
    onConfirm: (String, String, Double?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    // Rendered bare (not via formatCurrency) because this is an editable numeric field —
    // a "$400.00" string wouldn't survive a round-trip back through the input filter.
    var limit by remember { mutableStateOf(initialLimit?.let { formatLimitForInput(it) } ?: "") }
    // The 24 Google Calendar swatches this used to offer are gone — see CategoryPalette.kt. An
    // existing category whose stored hex predates the palette opens with its *resolved* slot
    // pre-selected, which is the same colour the rest of the app is already showing it as; without
    // resolving, the dialog would open with nothing selected and silently recolour on save.
    val colors = PALETTE
    var selectedColor by remember { mutableStateOf(resolveCategoryHex(initialColor)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.l)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_category_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limit,
                    // Same numeric filter as the budget item amount field.
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) limit = it },
                    label = { Text(stringResource(R.string.budget_limit_label)) },
                    supportingText = { Text(stringResource(R.string.budget_limit_hint)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.budget_select_color))
                ColorGrid(colors, selectedColor) { selectedColor = it }
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, selectedColor, parseMonthlyLimitInput(limit)); onDismiss() }) {
                Text(stringResource(if (initialName.isEmpty()) R.string.action_create else R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

/**
 * The category colour picker: the eight [PALETTE] slots in their fixed order.
 *
 * [colors] is still a parameter rather than reading [PALETTE] directly so the composable stays
 * stateless and previewable, but every caller passes the palette — a category colour that is not a
 * palette slot can no longer be created.
 *
 * Names come from [PALETTE_NAME_RES], indexed positionally, rather than from `swatchHueOf`. The hue
 * classifier puts both the gold and the brown slot in its ORANGE band, so TalkBack announced two of
 * the eight swatches identically — which defeats the point of labelling them at all (Issue #107).
 */
@Composable
fun ColorGrid(colors: List<String>, selectedColor: String, onColorSelected: (String) -> Unit) {
    val columns = 4
    Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.m)) {
        colors.chunked(columns).forEachIndexed { rowIndex, rowColors ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ApexSpacing.m)) {
                rowColors.forEachIndexed { colIndex, color ->
                    val index = rowIndex * columns + colIndex
                    val isSelected = selectedColor == color
                    val name = PALETTE_NAME_RES.getOrNull(index)
                        ?.let { stringResource(it) }
                        ?: stringResource(swatchHueLabelRes(swatchHueOf(color)))
                    val colorLabel = stringResource(R.string.cd_color_swatch, name)
                    // The visual swatch is 32dp; minimumInteractiveComponentSize brings the touch
                    // target up to 48dp without growing the dot.
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .selectable(selected = isSelected, onClick = { onColorSelected(color) }),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(ColorSwatchSize)
                                    .background(parseColorSafe(color), CircleShape)
                                    .border(
                                        width = if (isSelected) ApexSpacing.hairline else ApexSpacing.hairline / 2,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.outlineVariant,
                                        shape = CircleShape
                                    )
                                    .semantics { contentDescription = colorLabel }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubscriptionsView(viewModel: BudgetViewModel) {
    val subscriptions by viewModel.allSubscriptions.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var subToEdit by remember { mutableStateOf<Subscription?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.budget_add_subscription))
        }
        Spacer(modifier = Modifier.height(ApexSpacing.s))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            items(subscriptions) { sub ->
                SubscriptionItem(
                    subscription = sub,
                    onTogglePause = { viewModel.setSubscriptionPaused(sub, !sub.isPaused) },
                    onClick = { subToEdit = sub }
                )
            }
        }
    }

    if (showAddDialog) {
        SubscriptionDialog(
            title = stringResource(R.string.budget_new_subscription),
            onDismiss = { showAddDialog = false },
            onConfirm = { name, amount, date, notes ->
                viewModel.addSubscription(name, amount, date, notes)
            }
        )
    }

    if (subToEdit != null) {
        SubscriptionDialog(
            title = stringResource(R.string.budget_edit_subscription),
            initialName = subToEdit!!.name,
            initialAmount = subToEdit!!.amount.toString(),
            initialDate = subToEdit!!.renewalDate,
            initialNotes = subToEdit!!.notes ?: "",
            onDismiss = { subToEdit = null },
            onConfirm = { name, amount, date, notes ->
                viewModel.updateSubscription(subToEdit!!.copy(name = name, amount = amount, renewalDate = date, notes = notes))
            },
            onDelete = {
                viewModel.deleteSubscription(subToEdit!!)
                subToEdit = null
            }
        )
    }
}

@Composable
fun SubscriptionItem(subscription: Subscription, onTogglePause: () -> Unit, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(ApexSpacing.m).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subscription.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (subscription.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (subscription.isPaused) {
                        stringResource(R.string.budget_subscription_paused)
                    } else {
                        stringResource(R.string.budget_renews_prefix, subscription.renewalDate.format(DateTimeFormatter.ofPattern("MMM dd")))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (subscription.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                formatCurrency(subscription.amount, LocalCurrencyCode.current),
                style = MaterialTheme.typography.bodyMedium,
                color = if (subscription.isPaused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
            )
            IconButton(onClick = onTogglePause) {
                Icon(
                    imageVector = if (subscription.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = stringResource(
                        if (subscription.isPaused) R.string.budget_subscription_resume
                        else R.string.budget_subscription_pause
                    ),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun SubscriptionDialog(
    title: String,
    initialName: String = "",
    initialAmount: String = "",
    initialDate: LocalDate = LocalDate.now(),
    initialNotes: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, Double, LocalDate, String?) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(initialName) }
    var amount by remember { mutableStateOf(initialAmount) }
    var date by remember { mutableStateOf(initialDate) }
    var notes by remember { mutableStateOf(initialNotes) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text(stringResource(R.string.label_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = amount, onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it }, label = { Text(stringResource(R.string.label_amount)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                TextButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.budget_next_renewal, date.format(DateTimeFormatter.ISO_LOCAL_DATE))) }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(stringResource(R.string.label_notes_optional)) }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, amount.toDoubleOrNull() ?: 0.0, date, notes.ifBlank { null }); onDismiss() }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )

    if (showDatePicker) {
        ApexDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it }
        )
    }
}

/**
 * Sets the overall monthly spending ceiling (Issue #125). Blank/zero clears it — same
 * normalization as the per-category cap field, via [parseMonthlyLimitInput].
 */
@Composable
fun OverallLimitDialog(current: Double?, onDismiss: () -> Unit, onSave: (Double?) -> Unit) {
    var text by remember { mutableStateOf(current?.let { formatLimitForInput(it) } ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_overall_limit_setting)) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.budget_overall_limit_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(ApexSpacing.s))
                Text(
                    stringResource(R.string.budget_limit_hint),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(parseMonthlyLimitInput(text)) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
