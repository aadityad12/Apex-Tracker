package com.example.apextracker.design

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.apextracker.ui.design.*
import kotlin.math.max
import kotlin.math.min

/**
 * Debug-only style plate. Not part of the app — no nav route, no entry point in the UI.
 * Launch with:
 *   adb shell am start -n com.example.apextracker/com.example.apextracker.design.StylePlateActivity
 *
 * Everything here exists to answer a question the spec cannot: how Instrument Serif reads at
 * display size on a real AMOLED panel, whether Ember and Alarm are actually distinguishable,
 * and whether the heat ramp separates at 20dp.
 */
class StylePlateActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var dark by remember { mutableStateOf(true) }
            ApexTrackerTheme(darkTheme = dark) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    StylePlate(dark = dark, onToggleTheme = { dark = !dark })
                }
            }
        }
    }
}

@Composable
private fun StylePlate(dark: Boolean, onToggleTheme: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .safeDrawingPadding()
            .padding(horizontal = ApexSpacing.l),
        verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl)
    ) {
        Spacer(Modifier.height(ApexSpacing.s))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Style plate", style = MaterialTheme.typography.displayMedium)
                Text(
                    if (dark) "Dark · Graphite" else "Light · Graphite",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = dark, onCheckedChange = { onToggleTheme() })
        }

        TypeSection()
        NumeralSection()
        SurfaceSection()
        ComponentSurfaceSection()
        SemanticSection()
        HeatSection()
        ComponentSection()
        ChartSection()
        CardVsNoCardSection()

        Spacer(Modifier.height(ApexSpacing.xxl))
    }
}

// ── 1. Type ────────────────────────────────────────────────────────────────────────────

@Composable
private fun TypeSection() = PlateSection("Type") {
    val t = MaterialTheme.typography
    Text("Martian Mono", style = t.displayLarge)
    Text("Budget flow", style = t.displayMedium)
    Text("Screen time this week", style = t.displaySmall)
    Text("Study subjects", style = t.headlineMedium)
    Divider()
    Text("Geist · titleLarge — a screen title", style = t.titleLarge)
    Text("Geist · titleMedium — a row heading", style = t.titleMedium)
    Text("GEIST · TITLESMALL — SECTION EYEBROW", style = t.titleSmall, color = MaterialTheme.colorScheme.primary)
    Text(
        "Geist · bodyLarge. The quick brown fox jumps over the lazy dog, 0123456789. " +
            "This is what a note or a description reads like at length.",
        style = t.bodyLarge
    )
    Text("Geist · bodyMedium — the workhorse for list rows and secondary content.", style = t.bodyMedium)
    Text("Geist · bodySmall — captions and hints.", style = t.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text("Geist · labelMedium", style = t.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ── 2. Numerals ────────────────────────────────────────────────────────────────────────

@Composable
private fun NumeralSection() = PlateSection("Numerals · Geist Mono") {
    Text("02:14:37", style = ApexNumerals.hero)
    Text("Stopwatch — hero", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(ApexSpacing.s))

    // The alignment test: proportional figures make these ragged, mono makes them a column.
    listOf(
        "Groceries" to "$1,284.50",
        "Rent" to "$2,100.00",
        "Coffee" to "$8.75",
        "Transit" to "$96.40",
        "Subscriptions" to "$47.99"
    ).forEach { (label, value) ->
        Row(
            Modifier.fillMaxWidth().padding(vertical = ApexSpacing.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(value, style = ApexNumerals.medium)
        }
    }
    Divider()
    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.l)) {
        Column { Text("4h 12m", style = ApexNumerals.large); Caption("large") }
        Column { Text("87%", style = ApexNumerals.large); Caption("large") }
        Column { Text("0m", style = ApexNumerals.small); Caption("small · axis") }
    }
}

// ── 3. Surfaces ────────────────────────────────────────────────────────────────────────

@Composable
private fun SurfaceSection() = PlateSection("Surfaces") {
    val cs = MaterialTheme.colorScheme
    // The full ladder, in M3's own order. Several steps share a tone: this design has four tones
    // and M3 has five container slots, so they are mapped by role rather than stretched to fit.
    listOf(
        "background" to cs.background,
        "surface" to cs.surface,
        "surfaceVariant" to cs.surfaceVariant,
        "containerLowest" to cs.surfaceContainerLowest,
        "containerLow" to cs.surfaceContainerLow,
        "container" to cs.surfaceContainer,
        "containerHigh" to cs.surfaceContainerHigh,
        "containerHighest" to cs.surfaceContainerHighest,
        "inverseSurface" to cs.inverseSurface
    ).forEach { (name, c) ->
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ApexShapes.control))
                .background(c)
                .border(1.dp, cs.outlineVariant, RoundedCornerShape(ApexShapes.control))
                .padding(ApexSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(c.hex(), style = ApexNumerals.small, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.height(ApexSpacing.xs))
    }
    Caption("Body text contrast on background: ${contrast(cs.onBackground, cs.background)}:1 · needs ≥4.5")
    Caption("Secondary text on background: ${contrast(cs.onSurfaceVariant, cs.background)}:1 · needs ≥4.5")
    Caption("Accent on background: ${contrast(cs.primary, cs.background)}:1 · needs ≥4.5 for text, ≥3 for indicators")
    Caption("Hairline on background: ${contrast(cs.outline, cs.background)}:1 · needs ≥3 to be visible")
}

// ── 3b. Component surfaces — the undefined-slot detector ───────────────────────────────

/**
 * Every surface an M3 component picks *for itself* when the call site passes no colour.
 *
 * These read from the `*Defaults` objects rather than from named `ColorScheme` slots on purpose:
 * that is the same path the real component takes, so if a slot is ever left undefined, the tile
 * here renders Material's baseline (a lavender/purple-tinted neutral) instead of a palette tone and
 * the hole is visible at a glance. The 2026-07-30 audit found six such slots — dropdown menus,
 * every AlertDialog, DatePickerDialog, one ModalBottomSheet, and all three snackbar colours were
 * all rendering off-palette, and none of it was reachable by grepping for `colorScheme.`
 *
 * A tile whose hex is not in Design.md §3 is a bug.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ComponentSurfaceSection() = PlateSection("Component surfaces — undefined-slot detector") {
    val cs = MaterialTheme.colorScheme
    Caption("What each component picks when the call site passes nothing. Every hex here must appear in Design.md §3.")
    Spacer(Modifier.height(ApexSpacing.s))

    SlotRow("menu", MenuDefaults.containerColor, cs)
    SlotRow("dialog", AlertDialogDefaults.containerColor, cs)
    SlotRow("bottom sheet", BottomSheetDefaults.ContainerColor, cs)
    SlotRow("scrim", BottomSheetDefaults.ScrimColor, cs)
    SlotRow("card", CardDefaults.cardColors().containerColor, cs)
    SlotRow("top bar (scrolled)", cs.surfaceContainer, cs)
    SlotRow("nav bar", NavigationBarDefaults.containerColor, cs)
    SlotRow("snackbar", SnackbarDefaults.color, cs)
    SlotRow("snackbar text", SnackbarDefaults.contentColor, cs)
    SlotRow("snackbar action", SnackbarDefaults.actionColor, cs)

    Spacer(Modifier.height(ApexSpacing.m))
    Caption("In context — a snackbar is the one inverted surface in the app:")
    Spacer(Modifier.height(ApexSpacing.s))
    Snackbar(action = { TextButton(onClick = {}) { Text("UNDO") } }) { Text("Reminder deleted") }

    Spacer(Modifier.height(ApexSpacing.m))
    Caption("A menu opens on top of a dialog (RecurrencePickerDialog). Same tone by design — the hairline is what separates them:")
    Spacer(Modifier.height(ApexSpacing.s))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ApexShapes.sheet))
            .background(AlertDialogDefaults.containerColor)
            .padding(ApexSpacing.l)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(ApexShapes.control))
                .background(MenuDefaults.containerColor)
                .border(1.dp, cs.outline, RoundedCornerShape(ApexShapes.control))
                .padding(ApexSpacing.m)
        ) {
            Text("Menu item", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun SlotRow(label: String, color: Color, cs: ColorScheme) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = ApexSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(ApexShapes.cell))
                .background(color)
                .border(1.dp, cs.outlineVariant, RoundedCornerShape(ApexShapes.cell))
        )
        Spacer(Modifier.width(ApexSpacing.m))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(color.hex(), style = ApexNumerals.small, color = cs.onSurfaceVariant)
    }
}

// ── 4. The semantic collision test ─────────────────────────────────────────────────────

@Composable
private fun SemanticSection() = PlateSection("Semantics — the collision test") {
    val cs = MaterialTheme.colorScheme
    val positive = LocalApexSemantics.current.positive
    Caption("Ember is an orange-red. If Ember and Alarm are not obviously different here, the semantic set is wrong.")
    Spacer(Modifier.height(ApexSpacing.s))
    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s), modifier = Modifier.fillMaxWidth()) {
        SwatchTile("Ember", "emphasis", cs.primary, Modifier.weight(1f))
        SwatchTile("Sage", "met / under", positive, Modifier.weight(1f))
        SwatchTile("Alarm", "over / error", cs.error, Modifier.weight(1f))
    }
    Spacer(Modifier.height(ApexSpacing.m))
    // In context, which is the only place it matters.
    StateRow("Study 2h daily", "Met", positive, cs)
    StateRow("Screen time under 4h", "Missed", cs.error, cs)
    StateRow("Groceries budget", "$284 over", cs.error, cs)
    StateRow("Reading streak", "12 days", cs.primary, cs)
}

@Composable
private fun StateRow(label: String, value: String, tint: Color, cs: ColorScheme) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = ApexSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(6.dp).clip(RoundedCornerShape(ApexShapes.cell)).background(tint))
        Spacer(Modifier.width(ApexSpacing.m))
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = tint)
    }
    HorizontalDivider(color = cs.outlineVariant)
}

// ── 5. Heat ramp — the signature ───────────────────────────────────────────────────────

@Composable
private fun HeatSection() = PlateSection("Heatmap ramp — the signature") {
    val ramp = LocalApexSemantics.current.heatRamp
    Caption("Six steps. Index 0 is a neutral: an untracked day must not read as a barely-achieved one.")
    Spacer(Modifier.height(ApexSpacing.s))
    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.xs)) {
        ramp.forEachIndexed { i, c ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(ApexShapes.cell)).background(c))
                Text(if (i == 0) "–" else "${i - 1}", style = ApexNumerals.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    Spacer(Modifier.height(ApexSpacing.m))
    Caption("At real cell size (20dp), 14 weeks. Do steps 2 and 3 separate?")
    Spacer(Modifier.height(ApexSpacing.s))
    MiniHeatmap(ramp)
}

@Composable
private fun MiniHeatmap(ramp: List<Color>) {
    val today = MaterialTheme.colorScheme.primary
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(14) { week ->
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                repeat(7) { day ->
                    // Deterministic pseudo-data — enough variety to judge separation.
                    val b = ((week * 7 + day) * 37 % 11).let { if (it > 5) it - 6 else it - 1 }
                    val isToday = week == 0 && day == 3
                    Box(
                        Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(ApexShapes.cell))
                            .background(ramp[(b + 1).coerceIn(0, 5)])
                            .then(if (isToday) Modifier.border(1.5.dp, today, RoundedCornerShape(ApexShapes.cell)) else Modifier)
                    )
                }
            }
        }
    }
}

// ── 6. Components ──────────────────────────────────────────────────────────────────────

@Composable
private fun ComponentSection() = PlateSection("Components") {
    var selected by remember { mutableIntStateOf(0) }
    // M3 defaults a selected FilterChip to secondaryContainer — which in this palette is Sage,
    // i.e. "goal met". Selection is emphasis, not a semantic, so it takes the accent.
    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
        listOf("12 months", "2026", "2025").forEachIndexed { i, label ->
            FilterChip(
                selected = selected == i,
                onClick = { selected = i },
                shape = RoundedCornerShape(ApexShapes.control),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                label = { Text(label) }
            )
        }
    }
    Spacer(Modifier.height(ApexSpacing.m))
    // Button ignores shapes.small and defaults to a full pill. Pills are banned as decoration
    // here, so the shape is passed explicitly — the component library will bake this in.
    Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
        Button(onClick = {}, shape = RoundedCornerShape(ApexShapes.control)) { Text("Save") }
        OutlinedButton(onClick = {}, shape = RoundedCornerShape(ApexShapes.control)) { Text("Cancel") }
        TextButton(onClick = {}, shape = RoundedCornerShape(ApexShapes.control)) { Text("Add goal") }
    }
    Spacer(Modifier.height(ApexSpacing.m))
    var checked by remember { mutableStateOf(true) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = { checked = it })
        Text("Read 30 minutes", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text("Met", style = MaterialTheme.typography.labelMedium, color = LocalApexSemantics.current.positive)
    }
    Spacer(Modifier.height(ApexSpacing.s))
    OutlinedTextField(
        value = "Groceries",
        onValueChange = {},
        label = { Text("Category") },
        modifier = Modifier.fillMaxWidth()
    )
}

// ── 7. Chart, per the spec ─────────────────────────────────────────────────────────────

@Composable
private fun ChartSection() = PlateSection("Chart — no card, hairline baseline, 3-letter months") {
    val cs = MaterialTheme.colorScheme
    val data = listOf("Feb" to 0.42f, "Mar" to 0.61f, "Apr" to 0.35f, "May" to 0.88f, "Jun" to 0.54f, "Jul" to 0.73f)
    val accent = cs.primary
    val muted = cs.onSurface.copy(alpha = 0.14f)
    val line = cs.outline

    Row {
        Column(
            Modifier.height(120.dp).width(40.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            Text("$2.4k", style = ApexNumerals.small, color = cs.onSurfaceVariant)
            Text("$0", style = ApexNumerals.small, color = cs.onSurfaceVariant)
        }
        Spacer(Modifier.width(ApexSpacing.s))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.height(120.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s),
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEachIndexed { i, (_, v) ->
                    Canvas(Modifier.weight(1f).fillMaxHeight(v)) {
                        drawRoundRect(
                            color = if (i == data.lastIndex) accent else muted,
                            cornerRadius = CornerRadius(2.dp.toPx())
                        )
                    }
                }
            }
            Canvas(Modifier.fillMaxWidth().height(1.dp)) {
                drawRect(color = line)
            }
            Spacer(Modifier.height(ApexSpacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(ApexSpacing.s)) {
                data.forEachIndexed { i, (m, _) ->
                    Text(
                        m,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = ApexNumerals.small,
                        color = if (i == data.lastIndex) accent else cs.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ── 8. Card vs no card ─────────────────────────────────────────────────────────────────

@Composable
private fun CardVsNoCardSection() = PlateSection("Card vs. no card") {
    val cs = MaterialTheme.colorScheme
    Caption("Left is the pattern being removed. Right is the replacement: eyebrow + hairline + rhythm.")
    Spacer(Modifier.height(ApexSpacing.m))

    Surface(shape = RoundedCornerShape(20.dp), color = cs.surfaceVariant, shadowElevation = 2.dp) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("THIS MONTH", style = MaterialTheme.typography.labelLarge, color = cs.primary)
            Spacer(Modifier.height(ApexSpacing.s))
            Text("$1,284.50", style = ApexNumerals.large)
        }
    }

    Spacer(Modifier.height(ApexSpacing.xl))

    Column(Modifier.fillMaxWidth()) {
        Text("THIS MONTH", style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(ApexSpacing.xs))
        Text("$1,284.50", style = ApexNumerals.large)
        Spacer(Modifier.height(ApexSpacing.m))
        HorizontalDivider(color = cs.outlineVariant)
    }
}

// ── plumbing ───────────────────────────────────────────────────────────────────────────

@Composable
private fun PlateSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(ApexSpacing.m))
        content()
    }
}

@Composable
private fun Caption(text: String) = Text(
    text,
    style = MaterialTheme.typography.bodySmall,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
private fun Divider() {
    Spacer(Modifier.height(ApexSpacing.s))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Spacer(Modifier.height(ApexSpacing.s))
}

@Composable
private fun SwatchTile(name: String, role: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(ApexShapes.control))
                .background(color)
        )
        Spacer(Modifier.height(ApexSpacing.xs))
        Text(name, style = MaterialTheme.typography.labelMedium)
        Text(role, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(color.hex(), style = ApexNumerals.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun Color.hex(): String =
    "#%02X%02X%02X".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

/** WCAG relative-luminance contrast ratio, rounded to 1dp — shown on the plate so the
 *  numbers are checked against the real rendered palette, not against a spreadsheet. */
private fun contrast(a: Color, b: Color): String {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    val ratio = max(la, lb) / min(la, lb)
    return "%.1f".format(ratio)
}
