package com.example.apextracker

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apextracker.ui.design.ApexChartFrame
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenTimeTrackerView(onBackToMenu: () -> Unit, viewModel: ScreenTimeViewModel = viewModel()) {
    val hasPermission by viewModel.hasPermission.collectAsState()
    val todayMillis by viewModel.todayScreenTimeMillis.collectAsState()
    val allSessions by viewModel.getAllSessions().collectAsState(initial = emptyList())
    val apps by viewModel.installedApps.collectAsState()
    val aggregatedUsage by viewModel.aggregatedUsage.collectAsState()
    val context = LocalContext.current
    
    var showSettings by remember { mutableStateOf(false) }
    var showAllApps by rememberSaveable { mutableStateOf(false) }
    // Non-null while the per-app limit dialog is open (Issue #124).
    var appForLimit by remember { mutableStateOf<AppUsageInfo?>(null) }

    appForLimit?.let { app ->
        AppLimitDialog(
            app = app,
            onDismiss = { appForLimit = null },
            onSave = { minutes ->
                viewModel.setAppLimit(app, minutes)
                appForLimit = null
            }
        )
    }

    LifecycleEffect(onEvent = { viewModel.checkPermission() })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.screen_time_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = if (showSettings) { { showSettings = false } } else onBackToMenu) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (!showSettings) {
                        IconButton(
                            onClick = {
                                shareCsv(
                                    context,
                                    buildScreenTimeCsv(allSessions),
                                    "screen_time_sessions_${LocalDate.now()}.csv"
                                )
                            }
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = stringResource(R.string.cd_export_screen_time_csv)
                            )
                        }
                        if (hasPermission) {
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_exclude_apps))
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = ApexSpacing.l)
        ) {
            if (!hasPermission) {
                PermissionRequestCard(onGrantClick = { viewModel.openPermissionSettings() })
            } else if (showSettings) {
                ExcludeAppsList(apps, onToggle = { viewModel.toggleAppExclusion(it) })
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = ApexSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl)
                ) {
                    item {
                        TotalApexTime(aggregatedUsage)
                    }

                    if (aggregatedUsage.size > 1) {
                        item {
                            ApexSectionHeader(stringResource(R.string.screen_device_breakdown))
                            Column {
                                aggregatedUsage.forEachIndexed { i, usage ->
                                    if (i > 0) ApexDivider()
                                    DeviceBreakdownItem(usage)
                                }
                            }
                        }
                    }

                    item {
                        ApexSectionHeader(stringResource(R.string.screen_todays_apps))
                    }

                    // apps is already sorted by usage descending in the ViewModel. Show the top 5
                    // by default with a "Show all (N)" toggle revealing every non-excluded app that
                    // logged usage today. Rendering more rows is purely presentational — it reads
                    // precomputed state and never re-triggers usage calculation.
                    val activeApps = apps.filter { it.usageTimeMillis > 0 && !it.isExcluded }
                    if (activeApps.isEmpty()) {
                        item { ApexEmptyState(message = stringResource(R.string.screen_no_usage)) }
                    } else {
                        val visibleApps = if (showAllApps) activeApps else activeApps.take(5)
                        items(visibleApps, key = { it.packageName }) { app ->
                            AppUsageItem(app, onClick = { appForLimit = app })
                        }
                        if (activeApps.size > 5) {
                            item {
                                TextButton(onClick = { showAllApps = !showAllApps }) {
                                    Text(
                                        if (showAllApps) stringResource(R.string.screen_show_less)
                                        else stringResource(R.string.screen_show_all, activeApps.size)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        ApexChartFrame(title = stringResource(R.string.screen_trends_title)) {
                            ScreenTimeTrendsChart(allSessions)
                        }
                    }

                    item {
                        ApexSectionHeader(stringResource(R.string.screen_daily_history))
                    }

                    val history = allSessions.filter { it.date.isBefore(LocalDate.now()) }
                    if (history.isEmpty()) {
                        item { ApexEmptyState(message = stringResource(R.string.screen_no_history)) }
                    } else {
                        itemsIndexed(history.take(7)) { i, session ->
                            if (i > 0) ApexDivider()
                            ScreenTimeHistoryItem(session)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The screen's headline figure: today's total across devices.
 *
 * Was a 28dp-rounded `primaryContainer` card with the value in `displayMedium` — which, after the
 * theme swap, meant the number rendered in Instrument Serif with a synthetic Black weight. Same
 * defect class as the study timer: a display serif standing in for a quantity. It is now
 * [ApexNumerals.hero], the screen's single dominant number, with the label as a quiet eyebrow and
 * no container at all.
 */
@Composable
fun TotalApexTime(devices: List<DeviceSession>) {
    val totalMillis = devices.sumOf { it.durationMillis }
    Column(modifier = Modifier.fillMaxWidth()) {
        ApexSectionHeader(stringResource(R.string.screen_total_apex_time))
        Spacer(modifier = Modifier.height(ApexSpacing.xs))
        Text(
            text = formatDurationCompact(totalMillis),
            style = ApexNumerals.hero,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (devices.size > 1) {
            Text(
                text = stringResource(R.string.screen_connected_devices, devices.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** One device's share of today's total. Was a tinted 16dp card; now a row on a hairline. */
@Composable
fun DeviceBreakdownItem(usage: DeviceSession) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (usage.deviceName.contains("Phone", true)) Icons.Default.Smartphone else Icons.Default.Devices,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(ApexSpacing.m))
        Text(
            text = usage.deviceName,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatDurationCompact(usage.durationMillis),
            style = ApexNumerals.medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AppUsageItem(app: AppUsageInfo, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .heightIn(min = 48.dp)
            .padding(vertical = ApexSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        app.icon?.let {
            Image(
                bitmap = it.toBitmap().asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(28.dp).clip(RoundedCornerShape(ApexShapes.control))
            )
            Spacer(modifier = Modifier.width(ApexSpacing.m))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                app.appName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            app.limitMinutes?.let { minutes ->
                // The over-limit state is carried by the *word*, in Alarm. The duration itself stays
                // in the normal ink: colouring both made the row shout twice, and a state that is
                // spelled out does not also need the number recoloured.
                Text(
                    text = if (app.isOverLimit) stringResource(R.string.screen_limit_over, minutes)
                    else stringResource(R.string.screen_limit_set, minutes),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (app.isOverLimit) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(ApexSpacing.s))
        Text(
            formatDurationCompact(app.usageTimeMillis),
            style = ApexNumerals.medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** Set or clear a per-app daily screen-time limit (Issue #124). */
@Composable
fun AppLimitDialog(app: AppUsageInfo, onDismiss: () -> Unit, onSave: (Int?) -> Unit) {
    var text by remember { mutableStateOf(app.limitMinutes?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.screen_limit_dialog_title, app.appName)) },
        text = {
            Column {
                Text(stringResource(R.string.screen_limit_dialog_desc), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(ApexSpacing.m))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.screen_limit_minutes_label)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text.toIntOrNull()) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (app.limitMinutes != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Text(stringResource(R.string.screen_limit_clear), color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        }
    )
}

@Composable
fun ExcludeAppsList(apps: List<AppUsageInfo>, onToggle: (AppUsageInfo) -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.screen_tracking_prefs),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            stringResource(R.string.screen_tracking_prefs_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = ApexSpacing.l)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ApexSpacing.xs)
        ) {
            items(apps) { app ->
                AppToggleItem(app, onToggle)
            }
        }
    }
}

@Composable
fun AppToggleItem(app: AppUsageInfo, onToggle: (AppUsageInfo) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isExcluded) 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) 
                else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(ApexSpacing.m)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            app.icon?.let {
                Image(
                    bitmap = it.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.width(ApexSpacing.l))
            Column(modifier = Modifier.weight(1f)) {
                Text(app.appName, fontWeight = FontWeight.Medium)
                Text(
                    text = stringResource(if (app.isExcluded) R.string.screen_app_excluded else R.string.screen_app_tracking),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (app.isExcluded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            Switch(
                checked = !app.isExcluded,
                onCheckedChange = { onToggle(app) }
            )
        }
    }
}

@Composable
fun PermissionRequestCard(onGrantClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(ApexSpacing.l),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(ApexSpacing.l), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(R.string.screen_permission_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(ApexSpacing.s))
            Text(
                stringResource(R.string.screen_permission_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(ApexSpacing.l))
            Button(onClick = onGrantClick) {
                Text(stringResource(R.string.screen_grant_permission))
            }
        }
    }
}

/** One past day's total. Was a card per day, which stacked into a card run. */
@Composable
fun ScreenTimeHistoryItem(session: ScreenTimeSession) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = session.date.format(DateTimeFormatter.ofPattern("MMM d")),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = session.date.format(DateTimeFormatter.ofPattern("EEEE")),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = formatDurationCompact(session.durationMillis),
            style = ApexNumerals.medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LifecycleEffect(onEvent: () -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                onEvent()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
