package com.example.apextracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.ApexNumerals
import com.example.apextracker.ui.design.ApexSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

// One-off data marker geometry (Design.md §6), deliberately not part of the spacing scale.
private val CalendarCategoryDotSize = 8.dp

@Composable
fun BudgetCalendarView(
    items: List<BudgetItem>,
    categories: List<Category>,
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit
) {
    var selectedDayItems by remember { mutableStateOf<List<BudgetItem>?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    // The grid's column order follows the locale's real first day of week (Issue #160) instead of
    // hardcoding Sunday=0 — most of Europe and elsewhere is Monday-first.
    val locale = LocalLocale.current.platformLocale
    val firstDayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }

    val daysInMonth = currentMonth.lengthOfMonth()
    val firstDayOfMonth = (currentMonth.atDay(1).dayOfWeek.value - firstDayOfWeek.value + 7) % 7
    val days = (1..daysInMonth).toList()
    val paddingDays = (0 until firstDayOfMonth).toList()

    Column(modifier = Modifier.fillMaxSize().padding(ApexSpacing.l)) {
        BudgetMonthSelector(currentMonth = currentMonth, onMonthChange = onMonthChange)
        Spacer(modifier = Modifier.height(ApexSpacing.l))
        WeekdayHeaders(firstDayOfWeek)
        Spacer(modifier = Modifier.height(ApexSpacing.s))
        CalendarGrid(days, paddingDays, currentMonth, items, onDayClick = { date, dayItems ->
            selectedDate = date
            selectedDayItems = dayItems
        })
    }

    if (selectedDayItems != null && selectedDate != null) {
        DayBreakdownDialog(date = selectedDate!!, items = selectedDayItems!!, categories = categories, onDismiss = {
            selectedDayItems = null
            selectedDate = null
        })
    }
}

@Composable
fun BudgetMonthSelector(currentMonth: YearMonth, onMonthChange: (YearMonth) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(ApexSpacing.l),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onMonthChange(currentMonth.minusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.cd_previous_month))
        }
        Text(
            text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = { onMonthChange(currentMonth.plusMonths(1)) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.cd_next_month))
        }
    }
}

@Composable
fun WeekdayHeaders(firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY) {
    val locale = LocalLocale.current.platformLocale
    val useNarrowLabels = LocalDensity.current.fontScale >= 1.5f
    Row(modifier = Modifier.fillMaxWidth()) {
        (0..6).map { firstDayOfWeek.plus(it.toLong()) }.forEach { day ->
            Text(
                text = day.getDisplayName(if (useNarrowLabels) TextStyle.NARROW else TextStyle.SHORT, locale),
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics {
                        contentDescription = day.getDisplayName(TextStyle.FULL, locale)
                    },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun CalendarGrid(days: List<Int>, paddingDays: List<Int>, currentMonth: YearMonth, items: List<BudgetItem>, onDayClick: (LocalDate, List<BudgetItem>) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(ApexSpacing.xs),
        horizontalArrangement = Arrangement.spacedBy(ApexSpacing.xs)
    ) {
        items(paddingDays) { Box(modifier = Modifier.aspectRatio(1f)) }
        items(days) { day ->
            val date = currentMonth.atDay(day)
            val itemsForDay = items.filter { it.date == date }
            // Income isn't spend, so it doesn't count toward the figure shown on the day cell
            // (Issue #218) — it still appears in the day's own breakdown dialog below.
            val totalSpent = itemsForDay.expensesOnly().sumOf { it.amount }
            CalendarDayCard(day, date, totalSpent, onClick = { onDayClick(date, itemsForDay) })
        }
    }
}

@Composable
fun CalendarDayCard(day: Int, date: LocalDate, totalSpent: Double, onClick: () -> Unit) {
    Card(
        modifier = Modifier.aspectRatio(0.8f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (date == LocalDate.now()) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(ApexSpacing.hairline).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = day.toString(), style = ApexNumerals.medium)
            if (totalSpent > 0) {
                Text(text = formatCurrency(totalSpent, LocalCurrencyCode.current), style = ApexNumerals.small, color = MaterialTheme.colorScheme.primary, maxLines = 1)
            }
        }
    }
}

@Composable
fun DayBreakdownDialog(date: LocalDate, items: List<BudgetItem>, categories: List<Category>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.budget_breakdown_title, date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")))) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                items.forEach { item ->
                    val category = if (item.categoryId == -1L) {
                        subscriptionsCategory()
                    } else {
                        categories.find { it.id == item.categoryId }
                    }
                    DayBreakdownItem(item, category)
                    HorizontalDivider()
                }
                // Matches the figure on the day cell that opened this dialog: income doesn't
                // count toward it (Issue #218), even though it's still listed above.
                TotalRow(items.expensesOnly().sumOf { it.amount })
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } }
    )
}

@Composable
fun DayBreakdownItem(item: BudgetItem, category: Category?) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (category != null) {
                    Box(modifier = Modifier.size(CalendarCategoryDotSize).background(categoryColorOf(category.colorHex), CircleShape))
                    Spacer(modifier = Modifier.width(ApexSpacing.xs))
                }
                Text(
                    text = if (isSubscriptionItem(item)) {
                        stringResource(R.string.budget_subscription_item_title, budgetItemBaseTitle(item.title))
                    } else {
                        budgetItemBaseTitle(item.title)
                    },
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (!item.description.isNullOrBlank()) Text(text = item.description, style = MaterialTheme.typography.bodySmall)
        }
        val amountText = if (!item.isExpense) "+" + formatCurrency(item.amount, LocalCurrencyCode.current) else formatCurrency(item.amount, LocalCurrencyCode.current)
        // Income used to read in Sage (Issue #218); with all semantic hue removed (2026-08-11)
        // it's plain ink — the leading "+" above is what carries "money in" now.
        Text(text = amountText, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TotalRow(total: Double) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = stringResource(R.string.budget_total), style = MaterialTheme.typography.titleMedium)
        Text(text = formatCurrency(total, LocalCurrencyCode.current), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    }
}
