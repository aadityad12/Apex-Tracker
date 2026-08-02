package com.example.apextracker.ui.design

/**
 * The digit model behind [ApexFlipClock]. Deliberately Compose-free — no imports from
 * androidx.compose — so the keying rules can be unit-tested on the host JVM (see
 * FlipClockDigitsTest), the same split StudySubjectStats.kt and DurationFormat.kt use.
 *
 * The whole point of this file is the *keys*. A split-flap card flips when the character behind its
 * key changes, so which slot owns which key decides what animates.
 */

/**
 * One card in the clock. [key] identifies the *slot* — the physical card — and is stable across
 * ticks; [char] is what that card currently shows.
 */
data class FlipSlot(val key: String, val char: Char)

/**
 * Splits an elapsed duration into groups of digit slots: hours (omitted under an hour), minutes,
 * seconds. Mirrors StudyTrackerView.formatTime, which drops the hour field below 3600s —
 * FlipClockDigitsTest pins the two together so a change to either breaks loudly.
 *
 * Negative input coerces to zero rather than rendering a minus card; the callers are elapsed-time
 * counters and a negative there is a bug upstream, not something to display.
 */
fun flipClockGroups(seconds: Long): List<List<FlipSlot>> {
    val total = seconds.coerceAtLeast(0L)
    val hours = total / 3600
    return buildList {
        if (hours > 0) add(digitsOf(hours, "h"))
        add(digitsOf((total % 3600) / 60, "m"))
        add(digitsOf(total % 60, "s"))
    }
}

/**
 * Zero-padded digits for one field, keyed **from the right**: the ones column is always `${prefix}0`,
 * the tens column `${prefix}1`, and so on.
 *
 * Numbering from the right is load-bearing twice over. Within a field, crossing 100 hours adds an
 * `h2` card without re-keying the two already on screen, so `99:59:59 -> 100:00:00` flips the digits
 * that changed instead of every card. Across fields, prefixing by field (rather than keying by
 * position in the flattened row) means the minute and second cards keep their identity when the hour
 * group appears at 3600s — position 0 stops meaning "minutes tens" there, and index keys would flip
 * the entire clock at once.
 */
private fun digitsOf(value: Long, prefix: String): List<FlipSlot> {
    val text = value.toString().padStart(2, '0')
    return text.mapIndexed { i, c -> FlipSlot("$prefix${text.lastIndex - i}", c) }
}
