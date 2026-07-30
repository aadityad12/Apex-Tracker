package com.example.apextracker

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.WarningAmber
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderView(onBackToMenu: () -> Unit, viewModel: ReminderViewModel = viewModel()) {
    val activeReminders by viewModel.activeReminders.collectAsState(initial = emptyList())
    val completedReminders by viewModel.completedReminders.collectAsState(initial = emptyList())
    val allActiveReminders by viewModel.allActiveReminders.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearching by remember { mutableStateOf(false) }
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState(initial = true)
    val allDayTime by viewModel.allDayNotificationTime.collectAsState(initial = LocalTime.NOON)
    val offset by viewModel.specificTimeOffsetMinutes.collectAsState(initial = 30)

    var showAddDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var showCompletedReminders by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showClearAllConfirm by remember { mutableStateOf(false) }
    
    var selectedCompletedIds by remember { mutableStateOf(setOf<Long>()) }
    var isSelectionMode by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    // Exact-alarm permission is denied by default on API 33+; without it reminders fire
    // inexactly (possibly hours late). Track it across resumes so returning from the system
    // grant screen updates the banner and re-arms alarms exactly.
    val context = LocalContext.current
    var canScheduleExact by remember { mutableStateOf(ReminderScheduler.canScheduleExactAlarms(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = ReminderScheduler.canScheduleExactAlarms(context)
                if (granted && !canScheduleExact) viewModel.rescheduleAll()
                canScheduleExact = granted
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val now = LocalDateTime.now()
    
    val sortedActiveReminders = remember(activeReminders, now) {
        activeReminders.sortedWith(compareByDescending<Reminder> { 
            it.isOverdue(now) 
        }.thenBy { it.date }.thenBy { it.time ?: LocalTime.MAX })
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text(stringResource(R.string.reminders_selected_count, selectedCompletedIds.size), style = MaterialTheme.typography.titleSmall)
                    } else if (isSearching) {
                        // Same in-top-bar search field Notes uses (Issue #123).
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.reminders_search_hint)) },
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
                        Text(stringResource(R.string.reminders_title), 
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                },
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { 
                            isSelectionMode = false
                            selectedCompletedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_exit_selection))
                        }
                    } else {
                        IconButton(onClick = {
                            if (isSearching) {
                                isSearching = false
                                viewModel.setSearchQuery("")
                            } else onBackToMenu()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = {
                            viewModel.deleteReminders(selectedCompletedIds.toList())
                            isSelectionMode = false
                            selectedCompletedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.cd_delete_selected))
                        }
                    } else if (!isSearching) {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                        }
                        IconButton(onClick = { showSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.menu_settings))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(ApexShapes.control)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_reminder))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = ApexSpacing.l),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            if (notificationsEnabled && !canScheduleExact) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    // A real alert treatment rather than a soft tinted blob (Design.md §10 flagged
                    // this). The outline plus the Alarm-coloured icon say "something is wrong";
                    // the action stays Ember because Ember is emphasis — the thing to press — and
                    // Alarm is the diagnosis. Separating those two roles is the whole point of the
                    // semantic pair.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(ApexShapes.container),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(ApexSpacing.l),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(ApexSpacing.m))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(R.string.reminders_late_banner_title),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    stringResource(R.string.reminders_late_banner_text),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(ApexSpacing.s))
                            TextButton(
                                onClick = {
                                    ReminderScheduler.requestExactAlarmIntent(context)?.let { context.startActivity(it) }
                                },
                                shape = RoundedCornerShape(ApexShapes.control)
                            ) {
                                Text(stringResource(R.string.reminders_allow), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(ApexSpacing.l))
                ApexSectionHeader(stringResource(R.string.reminders_active))
            }

            if (sortedActiveReminders.isEmpty()) {
                item {
                    // Distinguish "nothing left to do" from "nothing matched the search".
                    ApexEmptyState(
                        message = if (searchQuery.isNotBlank() && allActiveReminders.isNotEmpty()) {
                            stringResource(R.string.reminders_search_no_results, searchQuery)
                        } else {
                            stringResource(R.string.reminders_all_done)
                        }
                    )
                }
            } else {
                itemsIndexed(sortedActiveReminders) { i, reminder ->
                    if (i > 0) ApexDivider()
                    ReminderItemModern(
                        reminder = reminder,
                        isOverdue = reminder.isOverdue(now),
                        today = now.toLocalDate(),
                        onToggle = { viewModel.toggleCompletion(reminder) },
                        onEdit = { reminderToEdit = reminder }
                    )
                }
            }

            if (completedReminders.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.reminders_completed).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        TextButton(
                            onClick = { showCompletedReminders = !showCompletedReminders },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(if (showCompletedReminders) stringResource(R.string.reminders_hide) else stringResource(R.string.reminders_show_count, completedReminders.size), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                if (showCompletedReminders) {
                    items(completedReminders) { reminder ->
                        ReminderItemModern(
                            reminder = reminder,
                            isOverdue = false,
                            today = now.toLocalDate(),
                            onToggle = { viewModel.toggleCompletion(reminder) },
                            onEdit = { /* No editing for completed */ },
                            isSelected = selectedCompletedIds.contains(reminder.id),
                            isSelectionMode = isSelectionMode,
                            onLongClick = {
                                isSelectionMode = true
                                selectedCompletedIds = selectedCompletedIds + reminder.id
                            },
                            onClick = {
                                if (isSelectionMode) {
                                    selectedCompletedIds = if (selectedCompletedIds.contains(reminder.id)) {
                                        selectedCompletedIds - reminder.id
                                    } else {
                                        selectedCompletedIds + reminder.id
                                    }
                                    if (selectedCompletedIds.isEmpty()) isSelectionMode = false
                                }
                            }
                        )
                    }
                    
                    item {
                        if (!isSelectionMode) {
                            TextButton(
                                onClick = { showClearAllConfirm = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.reminders_clear_completed), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            ReminderEditDialog(
                title = stringResource(R.string.reminders_new_title),
                onDismiss = { showAddDialog = false },
                onConfirm = { name, date, time, description, recurrence, priority ->
                    viewModel.addReminder(name, date, time, description, recurrence, priority)
                    showAddDialog = false
                }
            )
        }

        if (reminderToEdit != null) {
            ReminderEditDialog(
                title = stringResource(R.string.reminders_edit_title),
                initialName = reminderToEdit!!.name,
                initialDescription = reminderToEdit!!.description ?: "",
                initialDate = reminderToEdit!!.date,
                initialTime = reminderToEdit!!.time,
                initialRecurrence = reminderToEdit!!.recurrence,
                initialPriority = parseReminderPriority(reminderToEdit!!.priority),
                onDismiss = { reminderToEdit = null },
                onConfirm = { name, date, time, description, recurrence, priority ->
                    viewModel.updateReminder(reminderToEdit!!.copy(
                        name = name,
                        date = date,
                        time = time,
                        description = description,
                        recurrence = recurrence,
                        priority = priority.name
                    ))
                    reminderToEdit = null
                },
                onDelete = {
                    val deleted = reminderToEdit!!
                    viewModel.deleteReminder(deleted)
                    reminderToEdit = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = resources.getString(R.string.deleted_quoted, deleted.name),
                            actionLabel = resources.getString(R.string.action_undo),
                            duration = SnackbarDuration.Short
                        )
                        // The cloud delete has already been pushed; undoing re-pushes
                        // the same cloudId, which recreates the doc (and re-arms the alarm).
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.restoreReminder(deleted)
                        }
                    }
                }
            )
        }

        if (showSettingsDialog) {
            ReminderSettingsDialog(
                enabled = notificationsEnabled,
                allDayTime = allDayTime,
                offset = offset,
                onDismiss = { showSettingsDialog = false },
                onToggleEnabled = { viewModel.setNotificationsEnabled(it) },
                onSetAllDayTime = { viewModel.setAllDayTime(it) },
                onSetOffset = { viewModel.setOffset(it) }
            )
        }

        if (showClearAllConfirm) {
            AlertDialog(
                onDismissRequest = { showClearAllConfirm = false },
                title = { Text(stringResource(R.string.reminders_clear_confirm_title)) },
                text = { Text(stringResource(R.string.reminders_clear_confirm_text)) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllCompleted()
                            showClearAllConfirm = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(stringResource(R.string.action_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearAllConfirm = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun ReminderEditDialog(
    title: String,
    initialName: String = "",
    initialDescription: String = "",
    initialDate: LocalDate = LocalDate.now(),
    initialTime: LocalTime? = null,
    initialRecurrence: Recurrence? = null,
    initialPriority: ReminderPriority = ReminderPriority.NORMAL,
    onDismiss: () -> Unit,
    onConfirm: (String, LocalDate, LocalTime?, String, Recurrence?, ReminderPriority) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var priority by remember { mutableStateOf(initialPriority) }
    var name by remember { mutableStateOf(initialName) }
    var description by remember { mutableStateOf(initialDescription) }
    var date by remember { mutableStateOf(initialDate) }
    var time by remember { mutableStateOf(initialTime) }
    var recurrence by remember { mutableStateOf(initialRecurrence) }
    
    var showRecurrencePicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.label_description_optional)) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            DatePickerDialog(context, { _, year, month, day ->
                                date = LocalDate.of(year, month + 1, day)
                            }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))
                    }
                    
                    Button(
                        onClick = {
                            val t = time ?: LocalTime.now()
                            TimePickerDialog(context, { _, hour, minute ->
                                time = LocalTime.of(hour, minute)
                            }, t.hour, t.minute, true).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(time?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: stringResource(R.string.reminders_all_day))
                    }
                }
                
                // Importance (Issue #126): orders the list within a day and sets the
                // notification's priority.
                Text(stringResource(R.string.reminders_priority_label), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReminderPriority.entries.forEach { option ->
                        FilterChip(
                            selected = priority == option,
                            onClick = { priority = option },
                            label = { Text(stringResource(reminderPriorityLabelRes(option))) }
                        )
                    }
                }

                if (time != null) {
                    TextButton(onClick = { time = null }) {
                        Text(stringResource(R.string.reminders_set_all_day))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.reminders_recurrence_prefix, recurrence?.frequency?.let { stringResource(frequencyLabelRes(it)) } ?: stringResource(R.string.reminders_recurrence_none)), style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showRecurrencePicker = true }) {
                        Text(stringResource(if (recurrence == null) R.string.action_set else R.string.action_change))
                    }
                }
                if (recurrence != null) {
                    TextButton(onClick = { recurrence = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.reminders_remove_recurrence))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onConfirm(name, date, time, description, recurrence, priority) },
                enabled = name.isNotBlank()
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text(stringResource(R.string.action_delete))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )

    if (showRecurrencePicker) {
        RecurrencePickerDialog(
            initialRecurrence = recurrence,
            onDismiss = { showRecurrencePicker = false },
            onConfirm = {
                recurrence = it
                showRecurrencePicker = false
            }
        )
    }
}

@Composable
fun ReminderSettingsDialog(
    enabled: Boolean,
    allDayTime: LocalTime,
    offset: Int,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSetAllDayTime: (LocalTime) -> Unit,
    onSetOffset: (Int) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_settings_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.reminders_enable_notifications), modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = onToggleEnabled)
                }
                
                Column {
                    Text(stringResource(R.string.reminders_all_day_time), style = MaterialTheme.typography.labelMedium)
                    Button(
                        onClick = {
                            TimePickerDialog(context, { _, hour, minute ->
                                onSetAllDayTime(LocalTime.of(hour, minute))
                            }, allDayTime.hour, allDayTime.minute, true).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(allDayTime.format(DateTimeFormatter.ofPattern("HH:mm")))
                    }
                }

                Column {
                    Text(stringResource(R.string.reminders_offset_label), style = MaterialTheme.typography.labelMedium)
                    Slider(
                        value = offset.toFloat(),
                        onValueChange = { onSetOffset(it.toInt()) },
                        valueRange = 0f..120f,
                        steps = 23
                    )
                    Text(stringResource(R.string.reminders_minutes_before, offset), style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.action_close))
            }
        }
    )
}

/**
 * One reminder row. No container: rows are separated by hairlines like every other list in the app.
 *
 * The overdue state used to repaint the whole row — errorContainer background, an error border, an
 * error-tinted checkbox *and* an error-coloured name. That is four channels saying one thing. It is
 * now carried by the clock icon and the OVERDUE badge, both in Alarm, with the name left in normal
 * ink; the same split used for Screen Time's over-limit rows. Selection is the one state that still
 * tints the row, because selection is about the row as an object rather than about its content.
 *
 * [today] is passed in rather than read from `LocalDate.now()` here, for the same reason [isOverdue]
 * is: the row renders what the caller says the date is, it does not ask the clock. Reading the clock
 * inside the composable made the "Today" label depend on the wall date at render time, which silently
 * rotted the screenshot baselines overnight — a preview recorded on the 29th rendered "Today" and
 * then failed on the 30th. It is also simply wrong for a composable: a `now()` read during
 * composition never recomposes, so a row left on screen past midnight kept a stale "Today".
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReminderItemModern(
    reminder: Reminder,
    isOverdue: Boolean,
    today: LocalDate,
    onToggle: () -> Unit,
    onEdit: () -> Unit = {},
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onLongClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme
    val overdueNow = isOverdue && !reminder.isCompleted
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) cs.primaryContainer else Color.Transparent)
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onClick()
                    else if (!reminder.isCompleted) onEdit()
                },
                onLongClick = { if (reminder.isCompleted) onLongClick() }
            )
            .heightIn(min = 48.dp)
            .padding(vertical = ApexSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = reminder.isCompleted,
            onCheckedChange = { if (!isSelectionMode) onToggle() else onClick() },
            colors = CheckboxDefaults.colors(checkedColor = LocalApexSemantics.current.positive)
        )

        Column(modifier = Modifier.weight(1f).padding(start = ApexSpacing.s)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reminder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else null,
                    color = if (reminder.isCompleted) cs.onSurfaceVariant else cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // weight(fill = false) so the name yields space to the badges beside it rather
                    // than claiming the row. Without it, at a large font scale the name took the
                    // width and the priority badge was squeezed into a one-character-per-line column.
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (reminder.recurrence != null) {
                    Spacer(modifier = Modifier.width(ApexSpacing.xs))
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.cd_recurring),
                        modifier = Modifier.size(14.dp),
                        tint = cs.onSurfaceVariant
                    )
                }
                // Only non-default importance is marked — NORMAL is the vast majority and a badge on
                // every row would be noise (Issue #126).
                val priority = parseReminderPriority(reminder.priority)
                if (priority != ReminderPriority.NORMAL && !reminder.isCompleted) {
                    Spacer(modifier = Modifier.width(ApexSpacing.s))
                    Text(
                        text = stringResource(reminderPriorityLabelRes(priority)).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (priority == ReminderPriority.HIGH) cs.primary else cs.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = if (overdueNow) cs.error else cs.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(ApexSpacing.xs))
                // Dates and times are quantities: mono, so a column of rows stays aligned. "Today"
                // was a hardcoded English literal here, and the time was string-concatenated onto
                // it with a bullet — both localization bugs.
                Text(
                    text = if (reminder.date == today) stringResource(R.string.reminders_today)
                    else reminder.date.format(DateTimeFormatter.ofPattern("MMM d")),
                    style = ApexNumerals.small,
                    color = cs.onSurfaceVariant
                )
                reminder.time?.let { time ->
                    Spacer(modifier = Modifier.width(ApexSpacing.s))
                    Text(
                        text = time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        style = ApexNumerals.small,
                        color = cs.onSurfaceVariant
                    )
                }
                if (overdueNow) {
                    Spacer(modifier = Modifier.width(ApexSpacing.s))
                    Text(
                        text = stringResource(R.string.reminders_overdue).uppercase(),
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.error
                    )
                }
            }
        }

        if (!reminder.isCompleted && !isSelectionMode) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Display name for a reminder priority (Issue #126). */
@androidx.annotation.StringRes
fun reminderPriorityLabelRes(priority: ReminderPriority): Int = when (priority) {
    ReminderPriority.LOW -> R.string.reminders_priority_low
    ReminderPriority.NORMAL -> R.string.reminders_priority_normal
    ReminderPriority.HIGH -> R.string.reminders_priority_high
}
