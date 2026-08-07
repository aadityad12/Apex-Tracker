package com.example.apextracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The account-switch guard (Issue #186). The stakes are asymmetric: a missed reset uploads one
 * user's notes and budget into another user's Firestore, while a spurious reset destroys an
 * offline-only user's data. Both directions are pinned here.
 */
class AccountIdentityTest {

    @Test
    fun `switching accounts resets`() {
        assertTrue(shouldResetLocalDataForUid(previousUid = "uid-a", newUid = "uid-b"))
    }

    @Test
    fun `same account does not reset`() {
        // The overwhelmingly common case: every cold start with a restored session.
        assertFalse(shouldResetLocalDataForUid(previousUid = "uid-a", newUid = "uid-a"))
    }

    @Test
    fun `first ever sign-in keeps local data`() {
        // A null previous uid means this install has never been signed in, so whatever is in
        // Room is this user's own offline work. Wiping here would delete exactly the data that
        // performInitialSync's "push ALL local rows" pass exists to rescue (Issue #4).
        assertFalse(shouldResetLocalDataForUid(previousUid = null, newUid = "uid-a"))
    }

    @Test
    fun `uid comparison is exact`() {
        // Firebase uids are case-sensitive opaque strings; no normalization is applied, so a
        // near-match must still count as a different account rather than silently merging.
        assertTrue(shouldResetLocalDataForUid(previousUid = "uid-A", newUid = "uid-a"))
        assertTrue(shouldResetLocalDataForUid(previousUid = "uid-a ", newUid = "uid-a"))
        assertTrue(shouldResetLocalDataForUid(previousUid = "", newUid = "uid-a"))
    }
}
