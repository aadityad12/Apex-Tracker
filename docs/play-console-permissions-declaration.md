# Play Console Permissions Declaration Forms (Issue #256)

Not a code file — this is reference text to paste into Play Console's Permissions Declaration
Form fields at submission time, for the restricted permissions this app declares. Prepared in
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

## BIND_NOTIFICATION_LISTENER_SERVICE (notification access)

Declared by `ApexMediaListenerService` in the manifest. Play reviews this one against its
Notification Listener policy, and the answer has to be that the app has a core feature that
genuinely needs it and does not read notification content — which is exactly the case here, but it
has to be *said*, since a study tracker asking for notification access is not self-explanatory.

**Core functionality this permission enables**: the now-playing panel on the study timer's focus
screen, which shows the track currently playing and lets the user play/pause and skip it without
leaving a study session.

**Suggested declaration text**:

> ApexTracker's study timer has a full-screen focus mode that the user stays in for the length of a
> study session. The one thing users reliably break focus for is their music player, so the focus
> screen shows what is currently playing and offers play/pause and skip controls.
>
> Reaching the device's active media session requires either MEDIA_CONTENT_CONTROL — a signature
> permission unavailable to third-party apps — or a registered notification listener. The app
> therefore declares a NotificationListenerService, `ApexMediaListenerService`, purely as the
> component that grants access to `MediaSessionManager.getActiveSessions()`.
>
> **The app does not read notifications.** `ApexMediaListenerService` has an empty body: it does
> not override `onNotificationPosted` or `onNotificationRemoved`, so no notification ever enters
> the app's process. No notification content is collected, stored, transmitted, or shared. The
> media metadata that is read (track title, artist, cover art, playback position) is used only to
> render the on-screen panel and is never persisted or sent anywhere.
>
> The feature is entirely optional: access is off until the user grants it from the panel's own
> "Connect music controls" prompt, and the panel itself can be removed from the study settings
> dialog. The rest of the app is unaffected if the permission is never granted.

Cross-references, which must stay in agreement with the above: the class doc on
`ApexMediaListenerService`, the manifest comment on its `<service>` entry, and the notification
access bullet in `docs/privacy-policy.md`.

## Not applicable — included for completeness

`QUERY_ALL_PACKAGES` is **not** declared in the manifest. Screen Time needs to know which
installed apps have a launcher icon (so it can list "apps you can actually open" rather than every
installed package, including invisible system components), which is a narrower need than seeing
the full installed-package list. This is met via a `<queries>` block scoped to
`ACTION_MAIN`/`CATEGORY_LAUNCHER` intents (Issue #72) instead — Play's preferred, non-broad
alternative, which avoids this permission's history of automatic rejection entirely.
