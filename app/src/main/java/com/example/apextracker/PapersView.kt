package com.example.apextracker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSectionHeader
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.ApexStatRow
import com.example.apextracker.ui.design.apexMenuBorder
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * The Papers reading log (Plan.md Phase 1). The app owns the knowledge layer — queue, daily
 * pick, memos — and hands the document itself to the browser/PDF viewer via [openPaper].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PapersView(
    onBack: () -> Unit,
    viewModel: PapersViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val discoveryPreferences by viewModel.discoveryPreferences.collectAsState()
    val discoveryTopics by viewModel.discoveryTopics.collectAsState()
    val paperLinks by viewModel.paperLinks.collectAsState()
    val dailyFeedState by viewModel.dailyFeedState.collectAsState()
    val recommendationState by viewModel.recommendationState.collectAsState()
    val muteSuggestion by viewModel.muteSuggestion.collectAsState()
    var showAdd by remember { mutableStateOf(false) }
    var detailPaper by remember { mutableStateOf<Paper?>(null) }
    var memoTarget by remember { mutableStateOf<Paper?>(null) }
    var showExport by remember { mutableStateOf(false) }
    var showTopics by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val allPapers = remember(state.queue, state.history) { state.queue + state.history }
    val showOnboarding = discoveryTopics.isEmpty() && !discoveryPreferences.onboardingDismissed

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.papers_title), style = MaterialTheme.typography.titleSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showTopics = true }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.cd_papers_discovery))
                    }
                    IconButton(onClick = { showExport = true }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_papers_export))
                    }
                    IconButton(onClick = { showAdd = true }) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_papers_add))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        if (state.loaded && state.queue.isEmpty() && state.history.isEmpty()) {
            Column(
                Modifier.padding(innerPadding).fillMaxSize().padding(horizontal = ApexSpacing.l),
                verticalArrangement = Arrangement.Center
            ) {
                if (showOnboarding) {
                    OnboardingPrompt(
                        onSetTopics = { showTopics = true },
                        onImportSeeds = { viewModel.importSeeds() },
                        onSkip = { viewModel.dismissOnboarding() }
                    )
                } else {
                    ApexEmptyState(
                        message = stringResource(R.string.papers_empty),
                        actionLabel = stringResource(R.string.papers_import_seeds),
                        onAction = { viewModel.importSeeds() }
                    )
                }
                DailyPaperFeedMessage(dailyFeedState)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = ApexSpacing.l, vertical = ApexSpacing.s)
            ) {
                muteSuggestion?.let { topic ->
                    item(key = "mute-suggestion") {
                        MuteSuggestionRow(
                            topic = topic,
                            onMute = { viewModel.pauseTopic(topic); viewModel.dismissMuteSuggestion() },
                            onDismiss = { viewModel.dismissMuteSuggestion() }
                        )
                        Spacer(Modifier.height(ApexSpacing.s))
                    }
                }
                item(key = "daily-feed-status") {
                    DailyPaperFeedMessage(dailyFeedState)
                }
                state.todayPick?.let { pick ->
                    item(key = "pick") {
                        TodayPickCard(
                            paper = pick,
                            onMarkRead = { memoTarget = pick }
                        )
                        Spacer(Modifier.height(ApexSpacing.xl))
                    }
                }
                item(key = "recommendation-status") {
                    RecommendationStatusMessage(recommendationState)
                }
                state.recommendations?.let { shelf ->
                    item(key = "recommendations-header") {
                        ApexSectionHeader(recommendationHeading(shelf.basis))
                        Spacer(Modifier.height(ApexSpacing.s))
                    }
                    itemsIndexed(shelf.papers, key = { _, p -> p.id }) { i, paper ->
                        if (i > 0) ApexDivider()
                        ApexStatRow(
                            label = paper.title,
                            supporting = paperMetaLine(paper),
                            value = paper.year?.toString() ?: "",
                            onClick = { detailPaper = paper }
                        )
                    }
                    item { Spacer(Modifier.height(ApexSpacing.xl)) }
                }
                if (state.queueRest.isNotEmpty()) {
                    item(key = "queue-header") {
                        ApexSectionHeader(stringResource(R.string.papers_queue) + " · " + state.queueRest.size)
                        Spacer(Modifier.height(ApexSpacing.s))
                    }
                    itemsIndexed(state.queueRest, key = { _, p -> p.id }) { i, paper ->
                        if (i > 0) ApexDivider()
                        ApexStatRow(
                            label = paper.title,
                            supporting = paperMetaLine(paper),
                            value = paper.year?.toString() ?: "",
                            onClick = { detailPaper = paper }
                        )
                    }
                    item { Spacer(Modifier.height(ApexSpacing.xl)) }
                }
                if (state.history.isNotEmpty()) {
                    item(key = "history-header") {
                        ApexSectionHeader(stringResource(R.string.papers_history))
                        Spacer(Modifier.height(ApexSpacing.s))
                    }
                    itemsIndexed(state.history, key = { _, p -> p.id }) { i, paper ->
                        if (i > 0) ApexDivider()
                        HistoryRow(paper = paper, onClick = { detailPaper = paper })
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddPaperDialog(
            viewModel = viewModel,
            onDismiss = {
                showAdd = false
                viewModel.resetFetch()
            }
        )
    }
    if (showExport) {
        PapersExportDialog(
            onDismiss = { showExport = false },
            onExportBibtex = {
                shareFile(
                    context,
                    buildPapersBibtex(allPapers),
                    "papers_${java.time.LocalDate.now()}.bib",
                    "application/x-bibtex"
                )
                showExport = false
            },
            onExportCsv = {
                shareCsv(context, buildPapersCsv(allPapers), "papers_${java.time.LocalDate.now()}.csv")
                showExport = false
            }
        )
    }
    if (showTopics) {
        PapersTopicsSheet(
            topics = discoveryTopics,
            onDismiss = { showTopics = false },
            onAdd = { field, keyword -> viewModel.addTopic(field, keyword) },
            onPause = { viewModel.pauseTopic(it) },
            onResume = { viewModel.resumeTopic(it) },
            onDelete = { viewModel.deleteTopic(it) }
        )
    }
    detailPaper?.let { paper ->
        val relatedPapers = remember(paper, paperLinks, allPapers) {
            relatedPapersFor(paper, paperLinks, allPapers)
        }
        val linkCandidates = remember(paper, paperLinks, allPapers) {
            linkablePapersFor(paper, paperLinks, allPapers)
        }
        PaperDetailSheet(
            paper = paper,
            relatedPapers = relatedPapers,
            linkCandidates = linkCandidates,
            onDismiss = { detailPaper = null },
            onMarkRead = {
                detailPaper = null
                memoTarget = paper
            },
            onAbandon = {
                viewModel.abandon(paper, paper.memo)
                detailPaper = null
            },
            onRequeue = {
                viewModel.requeue(paper)
                detailPaper = null
            },
            onDelete = {
                viewModel.deletePaper(paper)
                detailPaper = null
            },
            onOpenRelated = { detailPaper = it },
            onUnlink = { related ->
                paperLinks.firstOrNull {
                    (it.paperCloudId == paper.cloudId && it.relatedPaperCloudId == related.cloudId) ||
                        (it.paperCloudId == related.cloudId && it.relatedPaperCloudId == paper.cloudId)
                }?.let { viewModel.removeLink(it) }
            },
            onLink = { related -> viewModel.addLink(paper, related) }
        )
    }
    memoTarget?.let { paper ->
        ReadMemoDialog(
            paper = paper,
            onDismiss = { memoTarget = null },
            onSave = { memo, signal ->
                viewModel.markRead(paper, memo, signal)
                memoTarget = null
            }
        )
    }
}

@Composable
private fun DailyPaperFeedMessage(state: DailyPaperFeedState) {
    val message = when (state) {
        DailyPaperFeedState.Idle -> null
        DailyPaperFeedState.Loading -> stringResource(R.string.papers_daily_loading)
        is DailyPaperFeedState.Added -> stringResource(R.string.papers_daily_added, state.count)
        DailyPaperFeedState.NoResults -> stringResource(R.string.papers_daily_no_results)
        DailyPaperFeedState.Unavailable -> stringResource(R.string.papers_daily_unavailable)
        DailyPaperFeedState.RateLimited -> stringResource(R.string.papers_daily_rate_limited)
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s)
        )
    }
}

/**
 * "Because you read <title>" — the shelf's provenance (#150). Naming the paper the request was
 * built from is what makes a recommendation checkable rather than a black box, so the heading
 * carries a title even when the request weighed several.
 */
@Composable
private fun recommendationHeading(basis: List<Paper>): String = when {
    basis.isEmpty() -> stringResource(R.string.papers_recommended)
    basis.size == 1 -> stringResource(R.string.papers_recommended_because, basis.first().title)
    else -> stringResource(
        R.string.papers_recommended_because_more,
        basis.first().title,
        basis.size - 1
    )
}

@Composable
private fun RecommendationStatusMessage(state: RecommendationState) {
    val message = when (state) {
        RecommendationState.Idle -> null
        RecommendationState.Loading -> stringResource(R.string.papers_recommended_loading)
        RecommendationState.Unavailable -> stringResource(R.string.papers_daily_unavailable)
        RecommendationState.RateLimited -> stringResource(R.string.papers_daily_rate_limited)
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s)
        )
    }
}

/** Front-and-center first-run prompt (Q4): setting topics is the primary action, not the seed list. */
@Composable
private fun OnboardingPrompt(
    onSetTopics: () -> Unit,
    onImportSeeds: () -> Unit,
    onSkip: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            stringResource(R.string.papers_onboarding_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(ApexSpacing.s))
        Text(
            stringResource(R.string.papers_onboarding_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(ApexSpacing.l))
        Button(onClick = onSetTopics) { Text(stringResource(R.string.papers_onboarding_set_topics)) }
        Spacer(Modifier.height(ApexSpacing.s))
        TextButton(onClick = onImportSeeds) { Text(stringResource(R.string.papers_import_seeds)) }
        TextButton(onClick = onSkip) {
            Text(stringResource(R.string.papers_onboarding_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Inline decision row (Q7-C) after 3 straight abandons from one topic — not a fire-and-forget toast. */
@Composable
private fun MuteSuggestionRow(topic: PaperTopic, onMute: () -> Unit, onDismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.papers_mute_prompt, topic.keyword),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(ApexSpacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            TextButton(onClick = onMute) { Text(stringResource(R.string.papers_mute_action)) }
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.papers_mute_dismiss)) }
        }
        Spacer(Modifier.height(ApexSpacing.s))
        ApexDivider()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PapersTopicsSheet(
    topics: List<PaperTopic>,
    onDismiss: () -> Unit,
    onAdd: (field: String, keyword: String) -> Unit,
    onPause: (PaperTopic) -> Unit,
    onResume: (PaperTopic) -> Unit,
    onDelete: (PaperTopic) -> Unit
) {
    var field by remember { mutableStateOf(PAPER_DISCOVERY_FIELDS.first()) }
    var keyword by remember { mutableStateOf("") }
    var fieldExpanded by remember { mutableStateOf(false) }
    val atCap = topics.size >= MAX_PAPER_TOPICS

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ApexSpacing.l)
                .padding(bottom = ApexSpacing.xl)
        ) {
            ApexSectionHeader(stringResource(R.string.papers_topics_title))
            Spacer(Modifier.height(ApexSpacing.xs))
            Text(
                stringResource(R.string.papers_topics_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(ApexSpacing.l))

            if (topics.isEmpty()) {
                Text(
                    stringResource(R.string.papers_topics_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                topics.forEachIndexed { i, topic ->
                    if (i > 0) ApexDivider()
                    TopicRow(
                        topic = topic,
                        onPause = { onPause(topic) },
                        onResume = { onResume(topic) },
                        onDelete = { onDelete(topic) }
                    )
                }
            }

            Spacer(Modifier.height(ApexSpacing.l))
            ApexDivider()
            Spacer(Modifier.height(ApexSpacing.l))

            if (atCap) {
                Text(
                    stringResource(R.string.papers_topics_cap_reached, MAX_PAPER_TOPICS),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                ApexSectionHeader(stringResource(R.string.papers_topics_add))
                Spacer(Modifier.height(ApexSpacing.s))
                ExposedDropdownMenuBox(expanded = fieldExpanded, onExpandedChange = { fieldExpanded = it }) {
                    OutlinedTextField(
                        value = stringResource(PAPER_DISCOVERY_FIELD_LABELS.getValue(field)),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.papers_topics_field_label)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fieldExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = fieldExpanded,
                        onDismissRequest = { fieldExpanded = false },
                        border = apexMenuBorder()
                    ) {
                        PAPER_DISCOVERY_FIELDS.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(stringResource(PAPER_DISCOVERY_FIELD_LABELS.getValue(option))) },
                                onClick = { field = option; fieldExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(ApexSpacing.s))
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { keyword = it },
                    label = { Text(stringResource(R.string.papers_topics_keyword_label)) },
                    placeholder = { Text(stringResource(R.string.papers_topics_keyword_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(ApexSpacing.s))
                Button(
                    onClick = { onAdd(field, keyword); keyword = "" },
                    enabled = keyword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(stringResource(R.string.papers_topics_add_action)) }
            }
        }
    }
}

@Composable
private fun TopicRow(
    topic: PaperTopic,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDelete: () -> Unit
) {
    val paused = topic.pausedAt != null
    Row(
        Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(topic.keyword, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            val fieldLabel = stringResource(PAPER_DISCOVERY_FIELD_LABELS.getValue(topic.field))
            Text(
                if (paused) fieldLabel + " · " + stringResource(R.string.papers_topics_paused) else fieldLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        TextButton(onClick = if (paused) onResume else onPause) {
            Text(stringResource(if (paused) R.string.papers_topics_resume else R.string.papers_topics_pause))
        }
        TextButton(onClick = onDelete) {
            Text(stringResource(R.string.papers_delete), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun PapersExportDialog(
    onDismiss: () -> Unit,
    onExportBibtex: () -> Unit,
    onExportCsv: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.papers_export_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                Button(onClick = onExportBibtex, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.papers_export_bibtex))
                }
                Button(onClick = onExportCsv, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.papers_export_csv))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** authors · venue, elided naturally by the row. Year is the row's mono value, not repeated here. */
private fun paperMetaLine(paper: Paper): String =
    listOf(paper.authors, paper.venue).filter { it.isNotBlank() }.joinToString(" · ")

/**
 * Opens the paper outside the app — PDF when known, else the landing page.
 *
 * The URL is not the user's: it comes from the Semantic Scholar response (the daily fetch inserts
 * papers without anyone ever seeing the link), from another device via Firestore, or from a
 * restored backup file. Handing an arbitrary scheme to ACTION_VIEW launches whatever app claims
 * it, and a `file:` URI throws FileUriExposedException — a RuntimeException, so the
 * ActivityNotFoundException catch below never saw it and the app crashed on tap (Issue #190).
 */
private fun openPaper(context: Context, paper: Paper) {
    val raw = paper.pdfUrl.ifBlank { paper.url }
    val target = sanitizeWebUrl(raw)
    if (target.isEmpty()) {
        if (raw.isNotBlank()) Log.w("PapersView", "Refusing to open non-web paper URL")
        return
    }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, target.toUri()))
    } catch (_: ActivityNotFoundException) {
        // No browser/PDF app — nothing sane to do; the link stays visible in the sheet.
    }
}

@Composable
private fun TodayPickCard(paper: Paper, onMarkRead: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        ApexSectionHeader(stringResource(R.string.papers_today_pick))
        Spacer(Modifier.height(ApexSpacing.m))
        Text(paper.title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(ApexSpacing.xs))
        val meta = listOfNotNull(
            paper.authors.takeIf { it.isNotBlank() },
            paper.year?.toString(),
            paper.venue.takeIf { it.isNotBlank() }
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (paper.tldr.isNotBlank()) {
            Spacer(Modifier.height(ApexSpacing.s))
            Text(paper.tldr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        Spacer(Modifier.height(ApexSpacing.m))
        Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
            Button(onClick = { openPaper(context, paper) }) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(ApexSpacing.s))
                Text(stringResource(R.string.papers_open))
            }
            TextButton(onClick = onMarkRead) { Text(stringResource(R.string.papers_mark_read)) }
        }
        Spacer(Modifier.height(ApexSpacing.m))
        ApexDivider()
    }
}

@Composable
private fun HistoryRow(paper: Paper, onClick: () -> Unit) {
    val supporting = buildString {
        if (paper.status == PaperStatus.ABANDONED) {
            append(stringResource(R.string.papers_status_abandoned))
        } else {
            paper.readDate?.let {
                append(stringResource(R.string.papers_read_on, it.format(historyDateFormat)))
            }
        }
        if (paper.memo.isNotBlank()) {
            if (isNotEmpty()) append(" · ")
            append(paper.memo.lineSequence().first())
        }
    }
    ApexStatRow(
        label = paper.title,
        supporting = supporting.ifBlank { paperMetaLine(paper) },
        value = paper.signal?.let { "$it/5" } ?: "",
        onClick = onClick
    )
}

private val historyDateFormat: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaperDetailSheet(
    paper: Paper,
    relatedPapers: List<Paper>,
    linkCandidates: List<Paper>,
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onAbandon: () -> Unit,
    onRequeue: () -> Unit,
    onDelete: () -> Unit,
    onOpenRelated: (Paper) -> Unit,
    onUnlink: (Paper) -> Unit,
    onLink: (Paper) -> Unit
) {
    val context = LocalContext.current
    var showLinkPicker by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ApexSpacing.l)
                .padding(bottom = ApexSpacing.xl)
        ) {
            Text(paper.title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(ApexSpacing.xs))
            val meta = listOfNotNull(
                paper.authors.takeIf { it.isNotBlank() },
                paper.year?.toString(),
                paper.venue.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (paper.memo.isNotBlank()) {
                Spacer(Modifier.height(ApexSpacing.l))
                ApexSectionHeader(stringResource(R.string.papers_memo_label))
                Spacer(Modifier.height(ApexSpacing.xs))
                Text(paper.memo, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            if (paper.tldr.isNotBlank()) {
                Spacer(Modifier.height(ApexSpacing.l))
                ApexSectionHeader(stringResource(R.string.papers_tldr_label))
                Spacer(Modifier.height(ApexSpacing.xs))
                Text(paper.tldr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(ApexSpacing.l))
            ApexSectionHeader(stringResource(R.string.papers_abstract_label))
            Spacer(Modifier.height(ApexSpacing.xs))
            Text(
                paper.abstractText.ifBlank { stringResource(R.string.papers_no_abstract) },
                style = MaterialTheme.typography.bodyMedium,
                color = if (paper.abstractText.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(ApexSpacing.l))
            ApexSectionHeader(stringResource(R.string.papers_related_label))
            Spacer(Modifier.height(ApexSpacing.xs))
            relatedPapers.forEach { related ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenRelated(related) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        related.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(vertical = ApexSpacing.s)
                    )
                    IconButton(onClick = { onUnlink(related) }) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_papers_unlink, related.title)
                        )
                    }
                }
            }
            TextButton(onClick = { showLinkPicker = true }) {
                Text(stringResource(R.string.papers_related_add))
            }
            Spacer(Modifier.height(ApexSpacing.s))
            if (paper.url.isNotBlank() || paper.pdfUrl.isNotBlank()) {
                Button(onClick = { openPaper(context, paper) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.papers_open))
                }
                Spacer(Modifier.height(ApexSpacing.s))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                if (paper.status == PaperStatus.WANT) {
                    TextButton(onClick = onMarkRead) { Text(stringResource(R.string.papers_mark_read)) }
                    TextButton(onClick = onAbandon) { Text(stringResource(R.string.papers_abandon)) }
                } else {
                    TextButton(onClick = onMarkRead) { Text(stringResource(R.string.papers_memo_label)) }
                    TextButton(onClick = onRequeue) { Text(stringResource(R.string.papers_requeue)) }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDelete) {
                    Text(stringResource(R.string.papers_delete), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    if (showLinkPicker) {
        PaperLinkPickerDialog(
            candidates = linkCandidates,
            onDismiss = { showLinkPicker = false },
            onPick = { candidate ->
                onLink(candidate)
                showLinkPicker = false
            }
        )
    }
}

/** Picker for [PaperDetailSheet]'s "Link a paper" action (Issue #223) — [candidates] is already
 * [linkablePapersFor]'s result, so every row here is a valid pick. */
@Composable
private fun PaperLinkPickerDialog(
    candidates: List<Paper>,
    onDismiss: () -> Unit,
    onPick: (Paper) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.papers_link_picker_title)) },
        text = {
            if (candidates.isEmpty()) {
                Text(
                    stringResource(R.string.papers_link_picker_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    candidates.forEachIndexed { i, candidate ->
                        if (i > 0) ApexDivider()
                        Text(
                            candidate.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(candidate) }
                                .padding(vertical = ApexSpacing.s)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/** Mark-read / edit-memo dialog: the structured memo (text + 1–5 signal) written at finish time. */
@Composable
private fun ReadMemoDialog(
    paper: Paper,
    onDismiss: () -> Unit,
    onSave: (memo: String, signal: Int?) -> Unit
) {
    var memo by remember { mutableStateOf(paper.memo) }
    var signal by remember { mutableStateOf(paper.signal) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(paper.title, style = MaterialTheme.typography.titleMedium) },
        confirmButton = {
            TextButton(onClick = { onSave(memo, signal) }) { Text(stringResource(R.string.papers_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.papers_cancel)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.m)) {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text(stringResource(R.string.papers_memo_label)) },
                    placeholder = { Text(stringResource(R.string.papers_memo_hint)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    stringResource(R.string.papers_signal_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.xs)) {
                    (1..5).forEach { n ->
                        FilterChip(
                            selected = signal == n,
                            onClick = { signal = if (signal == n) null else n },
                            label = { Text("$n", style = ApexNumerals.medium) }
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun AddPaperDialog(viewModel: PapersViewModel, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val fetch by viewModel.fetchState.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.papers_add_title)) },
        confirmButton = {
            when (val f = fetch) {
                is PaperFetchState.Preview -> TextButton(onClick = {
                    viewModel.addFetched(f.paper)
                    onDismiss()
                }) { Text(stringResource(R.string.papers_add_confirm)) }
                else -> TextButton(
                    enabled = input.isNotBlank() && fetch !is PaperFetchState.Loading,
                    onClick = { viewModel.fetchForAdd(input) }
                ) { Text(stringResource(R.string.papers_add_fetch)) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.papers_cancel)) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.m)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        input = it
                        if (fetch !is PaperFetchState.Idle) viewModel.resetFetch()
                    },
                    label = { Text(stringResource(R.string.papers_add_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                when (val f = fetch) {
                    is PaperFetchState.Loading -> CircularProgressIndicator()
                    is PaperFetchState.Preview -> {
                        Text(f.paper.title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                        val meta = listOfNotNull(
                            f.paper.authors.takeIf { it.isNotBlank() },
                            f.paper.year?.toString(),
                            f.paper.venue.takeIf { it.isNotBlank() }
                        ).joinToString(" · ")
                        if (meta.isNotEmpty()) {
                            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    is PaperFetchState.Duplicate ->
                        Text(
                            stringResource(R.string.papers_add_duplicate, f.existing.title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    is PaperFetchState.Error ->
                        Text(
                            stringResource(if (f.notFound) R.string.papers_add_not_found else R.string.papers_add_network_error),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    PaperFetchState.Idle -> {}
                }
            }
        }
    )
}
