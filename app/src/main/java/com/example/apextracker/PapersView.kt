package com.example.apextracker

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
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
    var showAdd by remember { mutableStateOf(false) }
    var detailPaper by remember { mutableStateOf<Paper?>(null) }
    var memoTarget by remember { mutableStateOf<Paper?>(null) }
    var showExport by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val allPapers = remember(state.queue, state.history) { state.queue + state.history }

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
                ApexEmptyState(
                    message = stringResource(R.string.papers_empty),
                    actionLabel = stringResource(R.string.papers_import_seeds),
                    onAction = { viewModel.importSeeds() }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = ApexSpacing.l, vertical = ApexSpacing.s)
            ) {
                state.todayPick?.let { pick ->
                    item(key = "pick") {
                        TodayPickCard(
                            paper = pick,
                            onMarkRead = { memoTarget = pick }
                        )
                        Spacer(Modifier.height(ApexSpacing.xl))
                    }
                }
                if (state.queue.isNotEmpty()) {
                    item(key = "queue-header") {
                        ApexSectionHeader(stringResource(R.string.papers_queue) + " · " + state.queue.size)
                        Spacer(Modifier.height(ApexSpacing.s))
                    }
                    itemsIndexed(state.queue, key = { _, p -> p.id }) { i, paper ->
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
    detailPaper?.let { paper ->
        PaperDetailSheet(
            paper = paper,
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
            }
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

/** Opens the paper outside the app — PDF when known, else the landing page. */
private fun openPaper(context: Context, paper: Paper) {
    val target = paper.pdfUrl.ifBlank { paper.url }
    if (target.isBlank()) return
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
    onDismiss: () -> Unit,
    onMarkRead: () -> Unit,
    onAbandon: () -> Unit,
    onRequeue: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
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
            Spacer(Modifier.height(ApexSpacing.xl))
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
