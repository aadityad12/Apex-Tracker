package com.example.apextracker

import java.time.LocalDate

/**
 * The first monthly renewal on or after [today], starting from [renewalDate] (Issue #79).
 *
 * Resuming a paused subscription uses this instead of letting `checkAndAddSubscriptions()`
 * back-fill: the whole point of pausing is that those months were *not* charged, so the skipped
 * periods must be stepped over, not invoiced. A renewal that is already in the future is returned
 * unchanged. Day-of-month clamping is `LocalDate.plusMonths`'s (Jan 31 -> Feb 28), matching how the
 * catch-up loop already advances.
 */
fun nextRenewalOnOrAfter(renewalDate: LocalDate, today: LocalDate): LocalDate {
    var next = renewalDate
    while (next.isBefore(today)) {
        next = next.plusMonths(1)
    }
    return next
}

/**
 * The cloudId for the BudgetItem a subscription generates for one renewal.
 *
 * Deterministic, so the same subscription + the same renewal month is always the same document
 * no matter which device generates it. It used to be a fresh `UUID.randomUUID()` per generated
 * item, which meant two signed-in devices each minted their own row for the same charge and both
 * pushed — the user saw every subscription twice, with no way to tell a duplicate from a real
 * expense, and monthly totals silently doubled (Issue #196).
 *
 * Same reasoning as `studySessionDocId` and `goalCompletionDocId`: when a row is derived rather
 * than authored, its identity has to be derived too.
 *
 * The month, not the exact date, is the key — a renewal date that drifts within its month (short
 * months, a paused-then-resumed subscription) still refers to the same charge.
 */
fun subscriptionItemCloudId(subscriptionCloudId: String, renewal: LocalDate): String =
    "sub-$subscriptionCloudId-${renewal.year}-%02d".format(renewal.monthValue)
