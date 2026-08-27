package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTimeRefreshTest {

    // Issue #228: `resolveActivity(ACTION_MAIN + CATEGORY_HOME)`'s single "current best" pick was
    // observed to intermittently resolve to `com.android.settings` (via AOSP's low-priority
    // `Settings.FallbackHome` activity) instead of the real launcher, silently zeroing out genuine
    // Settings usage. `topPriorityHomePackages` is the fix's pure core: keep only the package(s)
    // tied for the highest declared intent-filter priority.

    @Test
    fun `a real launcher outranks Settings' low-priority FallbackHome`() {
        val candidates = listOf(
            "com.google.android.apps.nexuslauncher" to 0,
            "com.android.settings" to -1000
        )

        val result = topPriorityHomePackages(candidates)

        assertEquals(setOf("com.google.android.apps.nexuslauncher"), result)
    }

    @Test
    fun `two launchers tied at the top priority are both kept`() {
        val candidates = listOf(
            "com.google.android.apps.nexuslauncher" to 0,
            "com.example.otherlauncher" to 0,
            "com.android.settings" to -1000
        )

        val result = topPriorityHomePackages(candidates)

        assertEquals(setOf("com.google.android.apps.nexuslauncher", "com.example.otherlauncher"), result)
    }

    @Test
    fun `no candidates resolves to an empty set rather than crashing`() {
        assertEquals(emptySet<String>(), topPriorityHomePackages(emptyList()))
    }

    // filterTrackableUsage: the shared predicate unifying ScreenTimeViewModel.updateScreenTime()
    // and ScreenTimeRefresh.refreshTodayScreenTime() (Issue #228).

    @Test
    fun `home packages are excluded but a same-package non-home surface is not zeroed out`() {
        // The whole point of the fix: Settings itself is still counted even though its package
        // also happens to own a HOME-category fallback activity.
        val usageMap = mapOf(
            "com.google.android.apps.nexuslauncher" to 500L,
            "com.android.settings" to 80_000L,
            "com.example.app" to 1_000L
        )

        val total = filterTrackableUsage(
            usageMap = usageMap,
            excludedPackages = emptySet(),
            myPackageName = "com.example.apextracker",
            homePackages = setOf("com.google.android.apps.nexuslauncher")
        )

        assertEquals(81_000L, total)
    }

    @Test
    fun `excluded apps, this app, and systemui are all filtered out`() {
        val usageMap = mapOf(
            "com.example.excluded" to 100L,
            "com.example.apextracker" to 200L,
            "com.android.systemui" to 300L,
            "com.example.kept" to 400L
        )

        val total = filterTrackableUsage(
            usageMap = usageMap,
            excludedPackages = setOf("com.example.excluded"),
            myPackageName = "com.example.apextracker",
            homePackages = emptySet()
        )

        assertEquals(400L, total)
    }

    @Test
    fun `an empty trackablePackages set is unrestricted, a non-empty one narrows the total`() {
        val usageMap = mapOf("app.a" to 100L, "app.b" to 200L)

        val unrestricted = filterTrackableUsage(
            usageMap, excludedPackages = emptySet(), myPackageName = "", homePackages = emptySet()
        )
        val restricted = filterTrackableUsage(
            usageMap, excludedPackages = emptySet(), myPackageName = "", homePackages = emptySet(),
            trackablePackages = setOf("app.a")
        )

        assertEquals(300L, unrestricted)
        assertEquals(100L, restricted)
    }
}
