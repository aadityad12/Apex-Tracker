package com.example.apextracker

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.filled.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.lazy.itemsIndexed
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexEmptyState
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing

internal val bulletSequence = listOf("• ", "  ◦ ", "    ▪ ")
internal val bulletRegex = Regex("^(\\s*[•◦▪])\\s")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteView(onBackToMenu: () -> Unit, viewModel: NoteViewModel = viewModel()) {
    var showRecycleBin by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }

    val activeNotes by viewModel.activeNotes.collectAsState()
    val filteredNotes by viewModel.filteredNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val deletedNotes by viewModel.deletedNotes.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cleanUpRecycleBin()
    }

    if (noteToEdit != null) {
        NoteEditor(
            note = noteToEdit!!,
            onDismiss = { noteToEdit = null },
            onTogglePin = {
                viewModel.togglePin(noteToEdit!!)
                noteToEdit = noteToEdit!!.copy(isPinned = !noteToEdit!!.isPinned)
            },
            onSave = { title, content, attachments ->
                if (noteToEdit!!.id == 0L) {
                    viewModel.addNote(title, content, attachments)
                } else {
                    viewModel.updateNote(noteToEdit!!.copy(title = title, content = content, attachments = attachments))
                }
                noteToEdit = null
            }
        )
    } else if (showRecycleBin) {
        RecycleBinView(
            notes = deletedNotes,
            onBack = { showRecycleBin = false },
            onRestore = { viewModel.restoreNote(it) },
            onDeletePermanently = { viewModel.deletePermanently(it) }
        )
    } else if (showSettings) {
        NoteSettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearching) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.notes_search_hint)) },
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
                            Text(
                                stringResource(R.string.notes_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { if (isSearching) { isSearching = false; viewModel.setSearchQuery("") } else onBackToMenu() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = stringResource(R.string.cd_search))
                            }
                            IconButton(onClick = { showRecycleBin = true }) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = stringResource(R.string.notes_recycle_bin))
                            }
                            IconButton(onClick = { showSettings = true }) {
                                Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.menu_settings))
                            }
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { noteToEdit = Note(title = "", content = "") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(ApexShapes.control)
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.cd_add_note))
                }
            }
        ) { innerPadding ->
            if (activeNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ApexEmptyState(message = stringResource(R.string.notes_empty))
                }
            } else if (filteredNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    ApexEmptyState(message = stringResource(R.string.notes_search_no_results, searchQuery))
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = ApexSpacing.l)
                ) {
                    itemsIndexed(filteredNotes) { i, note ->
                        if (i > 0) ApexDivider()
                        NoteRow(
                            note = note,
                            onClick = { noteToEdit = note },
                            onDelete = { viewModel.moveToRecycleBin(note) },
                            onTogglePin = { viewModel.togglePin(note) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * One note row. Was a tinted card per note, which stacked into a card run.
 *
 * The delete icon used to be `error` tinted, which read as irreversible — it is not: notes go to the
 * recycle bin and can be restored. It is now a normal secondary control, so Alarm keeps meaning
 * "something is wrong" rather than "this button is red".
 */
@Composable
fun NoteRow(note: Note, onClick: () -> Unit, onDelete: () -> Unit, onTogglePin: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = ApexSpacing.m)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = note.title.ifBlank { stringResource(R.string.notes_untitled) },
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onTogglePin) {
                Icon(
                    if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = stringResource(if (note.isPinned) R.string.cd_unpin_note else R.string.cd_pin_note),
                    tint = if (note.isPinned) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = stringResource(R.string.action_delete),
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (note.content.isNotBlank()) {
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = cs.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(ApexSpacing.xs))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // The timestamp is a quantity — mono, so the column of dates lines up. The old string
            // carried a "Modified: " prefix baked into the resource; the eyebrow-quiet styling says
            // the same thing without spending a line on it.
            Text(
                text = note.modifiedAt.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")),
                style = ApexNumerals.small,
                color = cs.onSurfaceVariant
            )
            // Image-attachment indicator (Issue #127).
            val attachmentCount = attachmentList(note.attachments).size
            if (attachmentCount > 0) {
                Spacer(modifier = Modifier.width(ApexSpacing.s))
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = cs.onSurfaceVariant
                )
                Text(
                    text = attachmentCount.toString(),
                    style = ApexNumerals.small,
                    color = cs.onSurfaceVariant,
                    modifier = Modifier.padding(start = ApexSpacing.hairline)
                )
            }
            // No "PINNED" badge: the filled accent pin icon above already says it, and the DAO
            // sorts pinned notes to the top. Two channels for one bit of state is noise.
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditor(note: Note, onDismiss: () -> Unit, onTogglePin: () -> Unit, onSave: (String, String, String) -> Unit) {
    var title by remember { mutableStateOf(note.title) }
    var contentValue by remember {
        mutableStateOf(TextFieldValue(note.content, selection = TextRange(note.content.length)))
    }
    val context = LocalContext.current
    val untitledLabel = stringResource(R.string.notes_untitled)

    // Image attachments (Issue #127). Held as the list of stored filenames; the picker copies the
    // chosen image into app-private storage before adding it.
    var attachments by remember { mutableStateOf(attachmentList(note.attachments)) }
    val scope = rememberCoroutineScope()
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                saveNoteAttachment(context, uri)?.let { filename ->
                    attachments = attachments + filename
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (note.id == 0L) R.string.notes_new_title else R.string.notes_edit_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
                actions = {
                    // Shares the current in-editor title/content, so unsaved edits are included.
                    IconButton(onClick = { shareNote(context, title, contentValue.text, untitledLabel) }) {
                        Icon(Icons.Default.Share, contentDescription = stringResource(R.string.cd_share_note))
                    }
                    // Pinning only applies to an already-saved note (an unsaved new note has no row to update).
                    if (note.id != 0L) {
                        IconButton(onClick = onTogglePin) {
                            Icon(
                                if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(if (note.isPinned) R.string.cd_unpin_note else R.string.cd_pin_note),
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                    TextButton(onClick = { onSave(title, contentValue.text, joinAttachments(attachments)) }) {
                        Text(stringResource(R.string.action_save), fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(ApexSpacing.l)) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text(stringResource(R.string.notes_placeholder_title)) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(ApexSpacing.s))
            TextField(
                value = contentValue,
                onValueChange = { newValue ->
                    contentValue = handleNoteContentChange(newValue, contentValue)
                },
                placeholder = { Text(stringResource(R.string.notes_placeholder_content)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            
            if (attachments.isNotEmpty()) {
                NoteAttachmentStrip(
                    filenames = attachments,
                    onRemove = { filename ->
                        attachments = attachments.filterNot { it == filename }
                        // The row isn't committed until Save, but the file copy already happened, so
                        // delete it now — a re-add would just copy a fresh one.
                        deleteNoteAttachment(context, filename)
                    }
                )
            }

            // Helper bar for lists
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s),
                horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)
            ) {
                InputToolButton(icon = Icons.Default.Image, label = stringResource(R.string.notes_tool_attach)) {
                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                InputToolButton(icon = Icons.AutoMirrored.Filled.List, label = stringResource(R.string.notes_tool_bullet)) {
                    contentValue = modifyCurrentLine(contentValue) { line ->
                        val match = bulletRegex.find(line)
                        if (match != null && match.value == bulletSequence[0]) {
                            line.substring(match.value.length)
                        } else if (match != null) {
                            bulletSequence[0] + line.substring(match.value.length)
                        } else {
                            bulletSequence[0] + line
                        }
                    }
                }
                InputToolButton(icon = Icons.AutoMirrored.Filled.FormatIndentIncrease, label = stringResource(R.string.notes_tool_indent)) {
                    contentValue = modifyCurrentLine(contentValue) { line ->
                        val match = bulletRegex.find(line)
                        if (match != null) {
                            val currentIndex = bulletSequence.indexOf(match.value)
                            if (currentIndex != -1 && currentIndex < bulletSequence.size - 1) {
                                bulletSequence[currentIndex + 1] + line.substring(match.value.length)
                            } else line
                        } else {
                            // Indent only applies to already-bulleted lines; a plain line is left untouched.
                            line
                        }
                    }
                }
                InputToolButton(icon = Icons.AutoMirrored.Filled.FormatIndentDecrease, label = stringResource(R.string.notes_tool_outdent)) {
                    contentValue = modifyCurrentLine(contentValue) { line ->
                        val match = bulletRegex.find(line)
                        if (match != null) {
                            val currentIndex = bulletSequence.indexOf(match.value)
                            if (currentIndex > 0) {
                                bulletSequence[currentIndex - 1] + line.substring(match.value.length)
                            } else if (currentIndex == 0) {
                                line.substring(match.value.length)
                            } else line
                        } else line
                    }
                }
            }
        }
    }
}

internal fun modifyCurrentLine(value: TextFieldValue, action: (String) -> String): TextFieldValue {
    val text = value.text
    val selection = value.selection
    val lineStart = text.lastIndexOf('\n', selection.start - 1).let { if (it == -1) 0 else it + 1 }
    val lineEnd = text.indexOf('\n', selection.start).let { if (it == -1) text.length else it }
    
    val currentLine = text.substring(lineStart, lineEnd)
    val newLine = action(currentLine)
    
    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
    val diff = newLine.length - currentLine.length
    val newCursor = (selection.start + diff).coerceIn(lineStart, lineStart + newLine.length)
    return TextFieldValue(newText, TextRange(newCursor))
}

internal fun handleNoteContentChange(
    newValue: TextFieldValue,
    oldValue: TextFieldValue
): TextFieldValue {
    if (newValue.text.length == oldValue.text.length + 1) {
        val cursor = newValue.selection.start
        if (cursor > 0 && newValue.text[cursor - 1] == '\n') {
            val textBeforeNewLine = newValue.text.substring(0, cursor - 1)
            val lastLine = textBeforeNewLine.substringAfterLast('\n')

            // Whether to outdent/clear (Enter on a truly empty bulleted line) vs. continue the
            // bullet onto a new line must be decided from the WHOLE original line, not just the
            // part before the cursor — `lastLine` above is only that prefix, so a cursor placed
            // right after the bullet marker in "• Hello" (nothing empty about that line) used to
            // match `lastLine.trim() == prefix.trim()` anyway and incorrectly take the "clear the
            // bullet" branch, silently dropping the bullet and any trailing content's formatting.
            val oldCursor = oldValue.selection.start
            val oldLineStart = oldValue.text.lastIndexOf('\n', oldCursor - 1).let { if (it == -1) 0 else it + 1 }
            val oldLineEnd = oldValue.text.indexOf('\n', oldCursor).let { if (it == -1) oldValue.text.length else it }
            val oldLine = oldValue.text.substring(oldLineStart, oldLineEnd)

            val match = bulletRegex.find(lastLine)
            if (match != null) {
                val prefix = match.value
                if (oldLine.trim() == prefix.trim()) {
                    // Empty bullet line - Outdent or Clear
                    val currentIndex = bulletSequence.indexOf(prefix)
                    val lineStart = cursor - 1 - lastLine.length
                    if (currentIndex > 0) {
                        val newPrefix = bulletSequence[currentIndex - 1]
                        return TextFieldValue(
                            newValue.text.substring(0, lineStart) + newPrefix + newValue.text.substring(cursor),
                            TextRange(lineStart + newPrefix.length)
                        )
                    } else {
                        return TextFieldValue(
                            newValue.text.substring(0, lineStart) + "\n" + newValue.text.substring(cursor),
                            TextRange(lineStart + 1)
                        )
                    }
                } else {
                    // Continue Bullet
                    return TextFieldValue(
                        newValue.text.substring(0, cursor) + prefix + newValue.text.substring(cursor),
                        TextRange(cursor + prefix.length)
                    )
                }
            }
        }
    } else if (newValue.text.length == oldValue.text.length - 1) {
        val oldCursor = oldValue.selection.start
        val textBefore = oldValue.text.substring(0, oldCursor)
        val lastLine = textBefore.substringAfterLast('\n')
        val match = bulletRegex.find(lastLine)
        if (match != null && match.value == lastLine && newValue.selection.start == oldCursor - 1) {
            val lineStart = oldCursor - lastLine.length
            return TextFieldValue(
                oldValue.text.substring(0, lineStart) + oldValue.text.substring(oldCursor),
                TextRange(lineStart)
            )
        }
    }
    return newValue
}

@Composable
fun InputToolButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp) },
        leadingIcon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinView(
    notes: List<Note>,
    onBack: () -> Unit,
    onRestore: (Note) -> Unit,
    onDeletePermanently: (Note) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notes_recycle_bin)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                }
            )
        }
    ) { innerPadding ->
        if (notes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.notes_recycle_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(innerPadding).fillMaxSize().padding(ApexSpacing.l),
                verticalArrangement = Arrangement.spacedBy(ApexSpacing.m)
            ) {
                items(notes) { note ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(ApexSpacing.l)) {
                            Text(note.title.ifBlank { stringResource(R.string.notes_untitled) }, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(ApexSpacing.s))
                            Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                                Button(
                                    onClick = { onRestore(note) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(stringResource(R.string.notes_restore), fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { onDeletePermanently(note) },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(0.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text(stringResource(R.string.notes_delete_forever), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteSettingsDialog(viewModel: NoteViewModel, onDismiss: () -> Unit) {
    val retentionHours by viewModel.recycleBinRetentionHours.collectAsState(initial = 72)
    var sliderValue by remember { mutableFloatStateOf(retentionHours.toFloat()) }
    val context = LocalContext.current
    val securitySettings = remember { SecuritySettings(context) }
    val notesLocked by securitySettings.notesLockEnabled.collectAsState(initial = false)
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.notes_settings_title)) },
        text = {
            Column {
                Text(stringResource(R.string.notes_retention))
                Text(
                    stringResource(R.string.notes_deleted_after, sliderValue.toInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(ApexSpacing.l))
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 1f..168f, // 1 hour to 1 week
                    steps = 167
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.notes_retention_1h), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.notes_retention_72h), style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.notes_retention_168h), style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.height(ApexSpacing.l))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(ApexSpacing.l))
                ModuleLockSetting(
                    checked = notesLocked,
                    titleRes = R.string.security_lock_notes_title,
                    onCheckedChange = { scope.launch { securitySettings.setNotesLock(it) } }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.setRetentionHours(sliderValue.toInt())
                onDismiss()
            }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/** Horizontal thumbnail strip of a note's image attachments, each removable and tap-to-view. */
@Composable
fun NoteAttachmentStrip(filenames: List<String>, onRemove: (String) -> Unit) {
    val context = LocalContext.current
    var viewing by remember { mutableStateOf<String?>(null) }

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s),
        horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)
    ) {
        items(filenames, key = { it }) { filename ->
            Box {
                AsyncImage(
                    model = noteAttachmentFile(context, filename),
                    contentDescription = stringResource(R.string.cd_note_attachment),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(ApexShapes.container))
                        .clickable { viewing = filename }
                )
                // Remove badge in the corner.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(ApexSpacing.xs)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .clickable { onRemove(filename) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_remove_attachment),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    viewing?.let { filename ->
        Dialog(onDismissRequest = { viewing = null }) {
            AsyncImage(
                model = noteAttachmentFile(context, filename),
                contentDescription = stringResource(R.string.cd_note_attachment),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(ApexShapes.container))
            )
        }
    }
}
