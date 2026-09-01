package com.example.apextracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The four-way truth table for the unlock guard. The interesting rows are the two that must *not*
 * pause: a glance at the lock screen, and unlocking straight back into the app.
 */
class StudyForegroundGuardTest {

    @Test
    fun `unlocking into another app pauses a running session`() {
        assertTrue(
            shouldAutoPauseOnUnlock(
                isRunning = true,
                screenInteractive = true,
                keyguardLocked = false,
                appForeground = false
            )
        )
    }

    @Test
    fun `unlocking back into ApexTracker keeps counting`() {
        assertFalse(
            shouldAutoPauseOnUnlock(
                isRunning = true,
                screenInteractive = true,
                keyguardLocked = false,
                appForeground = true
            )
        )
    }

    @Test
    fun `a glance at the lock screen keeps counting`() {
        // Screen on, still locked — the user has not gone anywhere.
        assertFalse(
            shouldAutoPauseOnUnlock(
                isRunning = true,
                screenInteractive = true,
                keyguardLocked = true,
                appForeground = false
            )
        )
    }

    @Test
    fun `a screen that is not interactive keeps counting`() {
        assertFalse(
            shouldAutoPauseOnUnlock(
                isRunning = true,
                screenInteractive = false,
                keyguardLocked = false,
                appForeground = false
            )
        )
    }

    @Test
    fun `a stopped timer is never paused again`() {
        assertFalse(
            shouldAutoPauseOnUnlock(
                isRunning = false,
                screenInteractive = true,
                keyguardLocked = false,
                appForeground = false
            )
        )
    }

    @Test
    fun `a run of away ticks has to be consecutive`() {
        var ticks = 0
        ticks = nextAwayTickCount(ticks, awayNow = true)
        assertEquals(1, ticks)
        // The frame between the screen coming on and MainActivity.onStart reads as away; the very
        // next tick, once the app has resumed, must throw the run away rather than pausing.
        ticks = nextAwayTickCount(ticks, awayNow = false)
        assertEquals(0, ticks)
    }

    @Test
    fun `two agreeing ticks reach the pause threshold`() {
        var ticks = 0
        repeat(AWAY_TICKS_BEFORE_PAUSE) { ticks = nextAwayTickCount(ticks, awayNow = true) }
        assertTrue(ticks >= AWAY_TICKS_BEFORE_PAUSE)
    }

    @Test
    fun `foreground tracking counts overlapping activity starts`() {
        assertFalse(AppForeground.isForeground)
        AppForeground.onActivityStarted()
        assertTrue(AppForeground.isForeground)
        // An Activity handoff: the incoming start lands before the outgoing stop.
        AppForeground.onActivityStarted()
        AppForeground.onActivityStopped()
        assertTrue(AppForeground.isForeground)
        AppForeground.onActivityStopped()
        assertFalse(AppForeground.isForeground)
        // Never goes negative, so an unpaired stop can't wedge it below zero.
        AppForeground.onActivityStopped()
        AppForeground.onActivityStarted()
        assertTrue(AppForeground.isForeground)
        AppForeground.onActivityStopped()
    }
}
