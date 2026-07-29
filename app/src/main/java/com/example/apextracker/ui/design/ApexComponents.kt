package com.example.apextracker.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The shared component vocabulary. Every screen composes from these rather than assembling
 * Surfaces and Texts locally — that local assembly is how the app ended up with eight corner
 * radii, nine padding values, and 66 inline fontWeight overrides.
 *
 * See .claude/skills/android-product-design for the rules these encode.
 */

/**
 * The ALL-CAPS section eyebrow. Replaces the "card with a coloured label at the top" pattern:
 * this labels a section without wrapping it in a container.
 *
 * Deliberately [MaterialTheme.typography.titleSmall] in `onSurfaceVariant`, not the accent. An
 * accent-coloured label on every section spends the accent on structure, leaving nothing to
 * signal what actually matters on the screen.
 */
@Composable
fun ApexSectionHeader(
    text: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** The structural hairline. One weight, one colour — see ApexPalette on why it is not fainter. */
@Composable
fun ApexDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * A label/value row — the workhorse of every list in this app.
 *
 * The value is always [ApexNumerals.medium] (Geist Mono, tabular) when it is a quantity, which is
 * why [value] is separate from [supporting] rather than being concatenated into one string: a
 * proportional-figure amount in a list makes the column ragged and makes a changing value jitter.
 */
@Composable
fun ApexStatRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        // 48dp minimum target even though the visual row is shorter.
                        .minimumInteractiveComponentSize()
                        .selectable(selected = false, interactionSource = interaction, onClick = onClick)
                } else Modifier
            )
            .background(
                if (pressed) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f) else Color.Transparent
            )
            .padding(vertical = ApexSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(ApexSpacing.m))
        }
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting != null) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(ApexSpacing.m))
        Text(value, style = ApexNumerals.medium, color = valueColor)
    }
}

/**
 * Frames a chart: eyebrow, the plot, and a hairline baseline. No card, no shadow.
 *
 * Shadows are the specific thing being avoided here — `shadowElevation` over a near-black
 * background renders nothing at all, so the old chart cards were paying for an effect that was
 * invisible. Separation comes from the eyebrow and the rule instead.
 */
@Composable
fun ApexChartFrame(
    title: String,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth()) {
        ApexSectionHeader(title, trailing = trailing)
        Spacer(Modifier.height(ApexSpacing.m))
        content()
    }
}

/**
 * Grouped content that genuinely lifts off the page. Use sparingly — a card has to earn itself by
 * being liftable or tappable as a unit, otherwise use a header and a hairline.
 *
 * Note the explicit `contentColor`. `Surface(color = surfaceVariant)` otherwise sets
 * LocalContentColor to `onSurfaceVariant`, which silently dims *every* Text inside it — including
 * the headline number it was meant to showcase. That trap was caught on the style plate, where the
 * carded value rendered visibly greyer than the un-carded one.
 */
@Composable
fun ApexGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(ApexShapes.container),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(Modifier.padding(ApexSpacing.l), content = content)
    }
}

/**
 * An empty state. "No data" is not an empty state — an empty screen is an invitation to act, so
 * this requires a [message] that says what would fill it and, where there is one, the action that
 * does so.
 */
@Composable
fun ApexEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().heightIn(min = 96.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(ApexSpacing.s))
            TextButton(onClick = onAction, shape = RoundedCornerShape(ApexShapes.control)) {
                Text(actionLabel)
            }
        }
    }
}

// ── Previews ───────────────────────────────────────────────────────────────────────────────

@Composable
private fun ComponentGallery() {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.padding(ApexSpacing.l), verticalArrangement = Arrangement.spacedBy(ApexSpacing.xl)) {
            ApexChartFrame("This month") {
                Text("$1,284.50", style = ApexNumerals.large)
            }
            Column {
                ApexSectionHeader("Transactions")
                Spacer(Modifier.height(ApexSpacing.s))
                ApexStatRow("Groceries", "$1,284.50", supporting = "Recurring · 12 Jul")
                ApexDivider()
                ApexStatRow("Rent", "$2,100.00", onClick = {})
                ApexDivider()
                ApexStatRow(
                    "Coffee", "$8.75",
                    valueColor = LocalApexSemantics.current.positive
                )
            }
            ApexGroup {
                ApexSectionHeader("Today")
                Spacer(Modifier.height(ApexSpacing.s))
                Text("02:14:37", style = ApexNumerals.hero)
            }
            ApexEmptyState(
                message = "No goals yet — add one to start tracking",
                actionLabel = "Add goal",
                onAction = {}
            )
        }
    }
}

@Preview(name = "Components · dark", showBackground = true)
@Composable
private fun ComponentsDarkPreview() {
    ApexTrackerTheme(darkTheme = true) { ComponentGallery() }
}

@Preview(name = "Components · light", showBackground = true)
@Composable
private fun ComponentsLightPreview() {
    ApexTrackerTheme(darkTheme = false) { ComponentGallery() }
}

@Preview(name = "Components · dark 200% font", showBackground = true, fontScale = 2.0f)
@Composable
private fun ComponentsLargeFontPreview() {
    ApexTrackerTheme(darkTheme = true) { ComponentGallery() }
}
