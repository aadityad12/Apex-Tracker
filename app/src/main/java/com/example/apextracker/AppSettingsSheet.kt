package com.example.apextracker

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.apextracker.widget.refreshBudgetWidget
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.apextracker.ui.design.ApexShapes
import com.example.apextracker.ui.design.ApexSpacing

// Component geometry, not spacing: the profile image is also its minimum 48dp touch-sized visual.
private val ProfileImageSize = 48.dp

/**
 * App-wide settings bottom sheet — account/sign-in, dark mode, and currency. Extracted
 * from the old MainMenu (retired in the Phase 4 nav restructure) so it can be hosted from the
 * Dashboard's settings gear. The dashboard is the app's home now, so this is the single place these
 * global controls live.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsSheet(
    onDismiss: () -> Unit,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    currencyCode: String,
    onCurrencyChange: (String) -> Unit,
    authViewModel: AuthViewModel = viewModel()
) {
    val user by authViewModel.user.collectAsState()
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Scrollable so every section (now incl. Backup, Issue #121) is reachable rather
                // than falling below the sheet's fold.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ApexSpacing.xl)
                .padding(bottom = ApexSpacing.xxl)
        ) {
            Text(
                stringResource(R.string.menu_settings),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(ApexSpacing.xl))

            // User Profile / Auth Section
            Text(
                stringResource(R.string.menu_account),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(ApexSpacing.m))

            Surface(
                shape = RoundedCornerShape(ApexShapes.container),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (user != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(ApexSpacing.l)
                    ) {
                        if (user?.photoUrl != null) {
                            AsyncImage(
                                model = user?.photoUrl,
                                contentDescription = stringResource(R.string.cd_profile_picture),
                                modifier = Modifier.size(ProfileImageSize).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(ProfileImageSize),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = (user?.displayName ?: "U").take(1).uppercase(),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(ApexSpacing.l))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user?.displayName ?: stringResource(R.string.user_fallback), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text(user?.email ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { authViewModel.signOut(context) }) {
                            Icon(Icons.Default.Logout, contentDescription = stringResource(R.string.cd_sign_out), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                } else {
                    Button(
                        onClick = { authViewModel.signInWithGoogle(context) },
                        modifier = Modifier.fillMaxWidth().padding(ApexSpacing.l),
                        shape = RoundedCornerShape(ApexShapes.control),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Cloud, contentDescription = null)
                        Spacer(modifier = Modifier.width(ApexSpacing.s))
                        Text(stringResource(R.string.sign_in_google))
                    }
                }
            }

            Spacer(modifier = Modifier.height(ApexSpacing.xl))

            Text(
                stringResource(R.string.menu_appearance),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(ApexSpacing.m))

            Surface(
                onClick = { onDarkModeChange(!isDarkMode) },
                shape = RoundedCornerShape(ApexShapes.container),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(ApexSpacing.l)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(ApexSpacing.l))
                    Text(stringResource(R.string.menu_dark_mode), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange,
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(modifier = Modifier.height(ApexSpacing.xl))

            Text(
                stringResource(R.string.menu_currency),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(ApexSpacing.l))

            CurrencyDropdown(currencyCode = currencyCode, onCurrencySelected = onCurrencyChange)

            Spacer(modifier = Modifier.height(ApexSpacing.xl))

            Text(
                stringResource(R.string.backup_section),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(ApexSpacing.m))
            BackupRestoreControls()
        }
    }
}

/**
 * Back up / restore the whole local dataset to a JSON file via SAF (Issue #121). Export uses
 * ACTION_CREATE_DOCUMENT (user picks where to save), restore uses ACTION_OPEN_DOCUMENT. Restore
 * replaces all local data, so it's gated behind a confirmation. Works fully offline; no
 * permissions needed.
 *
 * Both buttons sit behind the module lock when either Budget or Notes is locked (Issue #187).
 * This sheet is not behind a [LockGate] — it is reached from the Dashboard gear — and the export
 * writes every note body and budget item in plaintext to a user-chosen file, which made it a
 * three-tap bypass of the entire lock. Import is gated for the mirror reason: it *destroys* the
 * locked modules' data. Either lock arms both buttons, because one file covers both modules.
 */
@Composable
private fun BackupRestoreControls() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    var pendingRestoreJson by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }

    val securitySettings = remember { SecuritySettings(context) }
    val budgetLock by securitySettings.budgetLockEnabled.collectAsState(initial = null)
    val notesLock by securitySettings.notesLockEnabled.collectAsState(initial = null)
    // null (still loading) must stay null so the guard fails closed rather than reading `false`.
    val anyLock = if (budgetLock == null || notesLock == null) null else (budgetLock == true || notesLock == true)
    val runUnlocked = rememberUnlockedAction(
        scope = UNLOCK_SCOPE_BACKUP,
        lockEnabled = anyLock,
        promptTitle = stringResource(R.string.security_backup_prompt_title),
        promptSubtitle = stringResource(R.string.security_lock_subtitle)
    )

    // Pre-resolved here (not via context.getString in the launcher callbacks): stringResource is
    // composable-only, and reading resources off LocalContext in a Composable is a lint error.
    val exportDoneMsg = stringResource(R.string.backup_export_done)
    val restoreDoneMsg = stringResource(R.string.backup_restore_done)
    val failedMsg = stringResource(R.string.backup_failed)
    val invalidMsg = stringResource(R.string.backup_invalid)

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = buildBackupJson(exportBackup(db, LocalDateTime.now().toString()))
            val ok = writeBackupToUri(context, uri, json)
            status = if (ok) exportDoneMsg else failedMsg
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = readBackupFromUri(context, uri)
            if (json == null) { status = failedMsg; return@launch }
            pendingRestoreJson = json
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.m)) {
        OutlinedButton(
            onClick = { runUnlocked { exportLauncher.launch("apextracker-backup-${LocalDate.now()}.json") } },
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.backup_export)) }
        OutlinedButton(
            onClick = { runUnlocked { importLauncher.launch(arrayOf("application/json")) } },
            modifier = Modifier.weight(1f)
        ) { Text(stringResource(R.string.backup_import)) }
    }
    status?.let {
        Spacer(modifier = Modifier.height(ApexSpacing.s))
        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    pendingRestoreJson?.let { json ->
        AlertDialog(
            onDismissRequest = { pendingRestoreJson = null },
            title = { Text(stringResource(R.string.backup_restore_confirm_title)) },
            text = { Text(stringResource(R.string.backup_restore_confirm_text)) },
            confirmButton = {
                Button(onClick = {
                    pendingRestoreJson = null
                    scope.launch {
                        val data = try { parseBackupJson(json) } catch (e: Exception) { null }
                        if (data == null) {
                            status = invalidMsg
                        } else {
                            restoreBackup(db, data)
                            refreshBudgetWidget(context)
                            status = restoreDoneMsg
                        }
                    }
                }) { Text(stringResource(R.string.backup_restore_action)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreJson = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}
