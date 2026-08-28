# Play Console Permissions Declaration Forms (Issue #256)

Not a code file — this is reference text to paste into Play Console's Permissions Declaration
Form fields at submission time, for the two restricted permissions this app declares. Prepared in
advance so submission isn't blocked on drafting justification text under review-deadline pressure.

`QUERY_ALL_PACKAGES` is deliberately **not** declared (Issue #72 replaced it with a narrower
`<queries>` block) and needs no form here — the positive case, included for completeness.

## PACKAGE_USAGE_STATS

**Core functionality this permission enables**: the Screen Time module — the app's per-app usage
tracking, daily/weekly usage charts, and per-app daily limits with notifications (Issue #124).
This is a primary, user-facing feature of the app, not incidental.

**Suggested declaration text**:

> ApexTracker is a personal productivity tracker. One of its core features, Screen Time, shows the
> user how long they've used each app on their own device today/this week, so they can build
> awareness of their own usage habits and optionally set per-app daily limits with a local
> notification when they're exceeded. `PACKAGE_USAGE_STATS` (requested via
> `UsageStatsManager`, granted manually by the user in system Settings — this app never requests
> it silently or via a deceptive flow) is the only API that exposes this data. All usage data stays
> on-device by default; it only leaves the device if the user explicitly signs in with Google, and
> then only to that user's own private, access-controlled Firestore path, for viewing their own
> usage across their own devices. It is never aggregated across users, sold, or shared with third
> parties.

## SCHEDULE_EXACT_ALARM

**Core functionality this permission enables**: the Reminders module's notifications firing at the
exact time the user set, including recurring reminders and after a device reboot.

**Suggested declaration text**:

> ApexTracker's Reminders feature lets the user schedule a notification for a specific date and
> time (optionally recurring). An inexact alarm can drift by many minutes, which defeats the
> purpose of a reminder set for a specific appointment or deadline — this is a calendar/alarm-clock
> style use case, which exact alarms are intended for. The app gracefully falls back to
> `setAndAllowWhileIdle` (inexact) if the user declines or later revokes the exact-alarm permission
> on API 31+, so the feature still works, just less precisely, if this permission isn't granted.

## Not applicable — included for completeness

`QUERY_ALL_PACKAGES` is **not** declared in the manifest. Screen Time needs to know which
installed apps have a launcher icon (so it can list "apps you can actually open" rather than every
installed package, including invisible system components), which is a narrower need than seeing
the full installed-package list. This is met via a `<queries>` block scoped to
`ACTION_MAIN`/`CATEGORY_LAUNCHER` intents (Issue #72) instead — Play's preferred, non-broad
alternative, which avoids this permission's history of automatic rejection entirely.
