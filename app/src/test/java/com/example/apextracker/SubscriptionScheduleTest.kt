package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

/** Issue #79 — resuming a paused subscription skips the paused months instead of back-filling. */
class SubscriptionScheduleTest {

    private val today = LocalDate.of(2026, 7, 23)

    @Test
    fun `a renewal months in the past rolls forward past today`() {
        // Paused since April: resuming should charge in August, not back-fill Apr/May/Jun/Jul.
        assertEquals(LocalDate.of(2026, 8, 5), nextRenewalOnOrAfter(LocalDate.of(2026, 4, 5), today))
    }

    @Test
    fun `todays renewal is kept`() {
        assertEquals(today, nextRenewalOnOrAfter(today, today))
    }

    @Test
    fun `a future renewal is untouched`() {
        val future = LocalDate.of(2026, 9, 1)
        assertEquals(future, nextRenewalOnOrAfter(future, today))
    }

    @Test
    fun `end-of-month days clamp the same way plusMonths does`() {
        // Jan 31 -> Feb 28 -> Mar 28 …, matching the existing catch-up loop's arithmetic.
        assertEquals(LocalDate.of(2026, 7, 28), nextRenewalOnOrAfter(LocalDate.of(2026, 1, 31), today))
    }

    @Test
    fun `generated item id is stable for a subscription and month`() {
        // Issue #196: this is what stops two devices minting two rows for one charge, so the
        // same inputs must always produce the same id.
        val a = subscriptionItemCloudId("sub-uuid", LocalDate.of(2026, 3, 15))
        val b = subscriptionItemCloudId("sub-uuid", LocalDate.of(2026, 3, 15))
        assertEquals(a, b)
    }

    @Test
    fun `a drifting day within the month is still the same charge`() {
        // Short-month clamping and pause/resume both move the day. The month is the identity.
        assertEquals(
            subscriptionItemCloudId("sub-uuid", LocalDate.of(2026, 2, 28)),
            subscriptionItemCloudId("sub-uuid", LocalDate.of(2026, 2, 3))
        )
    }

    @Test
    fun `different months and subscriptions stay distinct`() {
        val march = subscriptionItemCloudId("sub-uuid", LocalDate.of(2026, 3, 1))
        assertNotEquals(march, subscriptionItemCloudId("sub-uuid", LocalDate.of(2026, 4, 1)))
        assertNotEquals(march, subscriptionItemCloudId("other-uuid", LocalDate.of(2026, 3, 1)))
        // Zero-padded, so month 12 of one year cannot collide with month 1 of the next by
        // string concatenation (2026-1 + "2" vs 2026-12).
        assertNotEquals(
            subscriptionItemCloudId("s", LocalDate.of(2026, 12, 1)),
            subscriptionItemCloudId("s", LocalDate.of(2026, 1, 1)) + "2"
        )
    }
}
