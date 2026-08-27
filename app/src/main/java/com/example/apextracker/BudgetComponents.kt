package com.example.apextracker

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexDatePickerDialog
import com.example.apextracker.ui.design.ApexDivider
import com.example.apextracker.ui.design.ApexMotion
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSpacing
import com.example.apextracker.ui.design.LocalApexSemantics
import com.example.apextracker.ui.design.apexMenuBorder
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * The synthetic `id = -1L` "Subscriptions" bucket that subscription-derived budget items fall
 * into. It has no `categories` row — it's rebuilt on the fly wherever such items render, so its
 * colour has to be produced, not looked up. See [SUBSCRIPTIONS_CATEGORY_HEX] for which colour and why.
 *
 * This exists because the colour used to drift: the transactions list and pie chart built it from
 * the accent while the calendar day-breakdown hardcoded gold (#82), the same class of bug #67 fixed
 * for the label. One helper, called from every site, is the only thing that keeps them in sync.
 * ([BudgetCsvExport.resolveCategoryName] also knows about -1L but deliberately stays pure/Context-
 * free — see the note there.)
 */
@Composable
fun subscriptionsCategory(): Category = Category(
    id = -1L,
    name = stringResource(R.string.budget_category_subscriptions),
    colorHex = SUBSCRIPTIONS_CATEGORY_HEX
)

/**
 * The fixed palette slot the synthetic Subscriptions bucket occupies.
 *
 * It used to be derived from `MaterialTheme.colorScheme.primary` — i.e. Ember, the accent. That
 * breaks the rule that status colours are reserved and never appear as a category (`Design.md` §6):
 * the accent carries state and emphasis, so spending it on one bucket in the pie made that bucket
 * look important rather than merely different. A fixed palette hex also means the colour no longer
 * changes with the theme, which is what the old derivation did on every light/dark switch.
 *
 * Being fixed, it can collide with a user category that resolves to the same slot. That is
 * acceptable for the same reason the many-to-one legacy mapping is: every surface showing a category
 * colour also shows the category's name.
 */
private const val SUBSCRIPTIONS_CATEGORY_HEX = "#5A62CC" // PALETTE[6], indigo

/**
 * One transaction. De-carded in the 2026-07-29 redesign.
 *
 * This was a `Card` per row, elevated 2dp and filled with the category's own colour at 20% alpha —
 * so a screen of transactions was a vertical run of differently-tinted rounded rectangles. Three
 * things were wrong with it: the elevation draws nothing over a near-black background, colour was
 * being spent on every row rather than on what matters, and the category name was painted in the
 * category's colour, which is text wearing a series colour (`Design.md` §6). Identity now comes from
 * a small dot; the row itself sits on the background with a hairline under it.
 */
@Composable
fun BudgetListItem(
    item: BudgetItem,
    category: Category?,
    onClick: () -> Unit,
    isPending: Boolean = false
) {
    val catColor = category?.let { categoryColorOf(it.colorHex) } ?: MaterialTheme.colorScheme.outline
    // Pending renewals haven't happened yet, so they read as provisional rather than as spend.
    val dotColor = if (isPending) catColor.copy(alpha = 0.4f) else catColor
    // Income reads in the same Sage used for "goal met" elsewhere (Issue #218) — money in, not
    // money out — with a leading "+" since the stored amount is always a positive magnitude.
    val amountColor = when {
        !item.isExpense -> LocalApexSemantics.current.positive
        isPending -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val amountPrefix = if (!item.isExpense) "+" else ""

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressTint by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else Color.Transparent,
        animationSpec = ApexMotion.snap(),
        label = "press"
    )

    // Auto-created subscription items get their "[Subscription]" label here rather than baked into
    // the stored title; rows written by older builds still carry the old English prefix, which
    // budgetItemBaseTitle() strips (Issue #119).
    val baseTitle = budgetItemBaseTitle(item.title)
    val title = if (isSubscriptionItem(item) && !isPending) {
        stringResource(R.string.budget_subscription_item_title, baseTitle)
    } else {
        baseTitle
    }

    // The category name and the date share the supporting line — two separate small Texts stacked
    // under the title is what made the old row three lines tall for no extra information.
    val dateLabel = item.date.format(SHORT_DATE)
    val categoryLabel = category?.let {
        if (isPending) stringResource(R.string.budget_pending_category, it.name) else it.name
    }
    val supporting = listOfNotNull(categoryLabel, dateLabel, item.description?.takeIf { it.isNotBlank() })
        .joinToString(" · ")

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
                .background(pressTint)
                .heightIn(min = 48.dp)
                .padding(vertical = ApexSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
            Spacer(modifier = Modifier.width(ApexSpacing.m))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(ApexSpacing.m))
            Text(
                text = amountPrefix + formatCurrency(item.amount, LocalCurrencyCode.current),
                style = ApexNumerals.medium,
                color = amountColor
            )
        }
        ApexDivider()
    }
}

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetItemDialog(
    title: String,
    initialTitle: String = "",
    initialAmount: String = "",
    initialDescription: String = "",
    initialDate: LocalDate = LocalDate.now(),
    initialCategoryId: Long? = null,
    initialType: String = TransactionType.EXPENSE,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String?, LocalDate, Long?, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var itemTitle by remember { mutableStateOf(initialTitle) }
    var amount by remember { mutableStateOf(initialAmount) }
    var description by remember { mutableStateOf(initialDescription) }
    var date by remember { mutableStateOf(initialDate) }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == initialCategoryId }) }
    var transactionType by remember { mutableStateOf(initialType) }
    var expanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Receipt scanning (Issue #46). Nothing here saves on its own — a scan only fills the fields
    // in front of the user, who still has to press Save.
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var scanning by remember { mutableStateOf(false) }
    var scanFailed by remember { mutableStateOf(false) }
    var amountCandidates by remember { mutableStateOf(emptyList<Double>()) }
    val pickReceipt = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scanning = true
        scanFailed = false
        scope.launch {
            val guess = recognizeReceipt(context, uri)
            scanning = false
            // "Read nothing usable" is a blurry photo, not an error state — say so and leave every
            // field exactly as the user had it.
            if (guess == null || (guess.amountCandidates.isEmpty() && guess.merchantGuess == null)) {
                scanFailed = true
                return@launch
            }
            amountCandidates = guess.amountCandidates
            guess.amountCandidates.firstOrNull()?.let { amount = formatAmountInput(it) }
            // Only fill what's still empty: a scan run to correct one field must not wipe what the
            // user already typed into the others.
            guess.merchantGuess?.takeIf { itemTitle.isBlank() }?.let { itemTitle = it }
            guess.dateGuess?.let { date = it }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold)
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Income has no category of its own — a paycheck isn't a spending category — so
                // switching to it clears whatever the user had picked while on Expense (Issue #218).
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = transactionType == TransactionType.EXPENSE,
                        onClick = { transactionType = TransactionType.EXPENSE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) { Text(stringResource(R.string.budget_type_expense)) }
                    SegmentedButton(
                        selected = transactionType == TransactionType.INCOME,
                        onClick = {
                            transactionType = TransactionType.INCOME
                            selectedCategory = null
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) { Text(stringResource(R.string.budget_type_income)) }
                }
                OutlinedTextField(value = itemTitle, onValueChange = { itemTitle = it }, label = { Text(stringResource(R.string.label_title)) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                    label = { Text(stringResource(R.string.label_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    trailingIcon = {
                        IconButton(
                            enabled = !scanning,
                            onClick = {
                                pickReceipt.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            if (scanning) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    Icons.Default.PhotoCamera,
                                    contentDescription = stringResource(R.string.cd_scan_receipt)
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                if (scanFailed) {
                    Text(
                        text = stringResource(R.string.budget_receipt_unreadable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // Several plausible totals — offer them rather than silently committing to one.
                if (amountCandidates.size > 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                        amountCandidates.forEach { candidate ->
                            val label = formatAmountInput(candidate)
                            FilterChip(
                                selected = amount == label,
                                onClick = { amount = label },
                                label = { Text(label, style = ApexNumerals.small) }
                            )
                        }
                    }
                }
                if (transactionType == TransactionType.EXPENSE) {
                    CategoryDropdown(categories, selectedCategory, expanded, onExpandedChange = { expanded = it }, onCategorySelected = { selectedCategory = it; expanded = false })
                }
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(stringResource(R.string.label_description_optional)) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    shape = MaterialTheme.shapes.medium
                ) { 
                    Text(stringResource(R.string.budget_date_prefix, date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))) 
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (itemTitle.isNotBlank()) {
                        onConfirm(itemTitle, amount.toDoubleOrNull() ?: 0.0, description.ifBlank { null }, date, selectedCategory?.id, transactionType)
                    }
                },
                shape = MaterialTheme.shapes.medium
            ) {
                Text(stringResource(R.string.action_save)) 
            } 
        },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text(stringResource(R.string.action_cancel)) 
            } 
        },
        shape = MaterialTheme.shapes.extraLarge
    )

    if (showDatePicker) {
        ApexDatePickerDialog(
            initialDate = date,
            onDismiss = { showDatePicker = false },
            onConfirm = { date = it }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(categories: List<Category>, selectedCategory: Category?, expanded: Boolean, onExpandedChange: (Boolean) -> Unit, onCategorySelected: (Category?) -> Unit) {
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = onExpandedChange) {
        OutlinedTextField(
            value = selectedCategory?.name ?: stringResource(R.string.budget_no_category), 
            onValueChange = {}, 
            readOnly = true, 
            label = { Text(stringResource(R.string.label_category)) }, 
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }, 
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            shape = MaterialTheme.shapes.medium
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
            border = apexMenuBorder()
        ) {
            DropdownMenuItem(text = { Text(stringResource(R.string.budget_no_category)) }, onClick = { onCategorySelected(null) })
            categories.forEach { category ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(categoryColorOf(category.colorHex), CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(category.name)
                        }
                    },
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}
