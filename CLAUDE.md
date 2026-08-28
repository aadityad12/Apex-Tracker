# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Last full audit: 2026-07-07 (Firebase/auth follow-up: 2026-07-08; Known Issues fix pass + dependency bumps: 2026-07-09, branch `fix/known-issues-3-through-10`; Firebase sync unification (Issue #4): 2026-07-09, branch `fix/issue-4-firebase-sync`, merged as PR #16; bug-fix pass for issues #18–#31: 2026-07-10, merged as PRs #48/#50 — see "2026-07-10 Bug-Fix Pass" below; feature pass for issues #17/#32–#41/#53: 2026-07-11 → 2026-07-14, see "2026-07-11 → 2026-07-14 Feature Pass" below; doc-accuracy pass reconciling this file with the code: 2026-07-17; feature+verification pass 2026-07-18 → 2026-07-20: currency setting #76, category limits #75, notes export #77, screen-time app-list+chart #43, overview drill-in #47, subscriptions-colour #82, study subjects #78, and biometric lock #45 all merged, with the DB-migration (#78, v14) and biometric (#45) changes verified on-device — see the dated sections below; **Dashboard goal-tracking feature 2026-07-21 → 2026-07-22**, PRs #100–#103 merged + sync in progress, making the Dashboard the app home and bumping the DB to **v15** — see the "2026-07-21 → 2026-07-22 Dashboard" section; **bug/a11y/docs pass 2026-07-23** on branch `fix/issues-2026-07-23` covering issues #105–#120 and #97 — see the dated section at the end; **UI redesign foundation 2026-07-28 → 2026-07-29** on branch `redesign/foundation`, which replaced the whole theming layer, bundled real typefaces, and added a design-system skill — see "2026-07-29 Redesign foundation" at the end and **`Design.md` at the repo root**; **study timer flip clock 2026-08-01** on branch `feature/study-flip-clock`, which replaced the stopwatch readout with a split-flap digit display and added a focus mode — see "2026-08-01 Study timer flip clock" below; **Papers discovery redesign 2026-08-07**, which replaced the bare-field rotation with keyword topics and an engagement-weighted recommender, bumping the DB to **v22** — see "2026-08-07 Papers discovery redesign" below; **feature pass 2026-08-10 → 2026-08-11** clearing the last five open issues one PR each — the at-a-glance widget (#44), the study-timer widget and the shared start/pause path in `StudyTimerControl.kt` (#132), Papers recommendations (#150), receipt scanning (#46), and **SQLCipher at-rest encryption of the database** (#117) — see "Database encryption", "Home-screen widgets", "Receipt scanning" above; **doc reconciliation 2026-08-11**: CLAUDE.md and AGENTS.md had drifted in *both* directions (each held sections the other lacked) and were merged back to one roster — if you update one, update the other, or collapse them; **heatmap reverted to colour squares 2026-08-24**: the Dashboard's contribution-graph cells went back to GitHub-style fully-filled squares with colour-ramp intensity, undoing the 2026-07-30 Graphite pass's fill-height-bar experiment — see "2026-08-24 Heatmap reverted to colour squares" at the end; **backup key-wrapper fix 2026-08-25 (Issue #206)**: `database_key.xml` was missing from both backup-exclusion files, so a device restore left a passphrase this device's Keystore could never decrypt and the app silently fell back to an unencrypted database forever — fixed, plus a stale-wrapper self-heal in `DatabaseEncryption.kt` — see "2026-08-25 Backup excludes the SQLCipher key wrapper" at the end; **Play Store deployment pass starting 2026-08-27** on branch `claude/play-store-deployment-prep-cdf5b3`, working the open-issue backlog one issue per commit (implement → test → docs → push to main) toward a store-ready release — see "2026-08-27 Play Store deployment pass" at the end for the running log; **Screen Time launcher-exclusion fix (Issue #228), 2026-08-27**, a follow-up on a separate worktree branch to the loose end #209 flagged — see the `[Issue #228]` entry at the end of the Play Store deployment pass log; that same pass also shipped Budget CSV import (Issue #219) — see the `[Issue #219]` entry just above it; that pass's final entry, **Papers explicit paper-to-paper linking (Issue #223), 2026-08-27**, bumped the DB to **v25** and closed out the entire backlog — see the `[Issue #223]` entry at the very end of the Play Store deployment pass log; **deployment-readiness audit + fix pass starting 2026-08-28**: with the issue backlog cleared, a fine-tooth-comb multi-agent audit of the whole codebase (release config, DB migrations, Firebase sync, background work, encryption/backup, every feature module, i18n/theming) surfaced 27 new issues (#230–#256), now being worked the same one-issue-per-commit way — see "2026-08-28 Deployment-readiness audit fix pass" at the end for the running log). If you make significant architectural changes, update this file in the same session.

**Doc-accuracy note (2026-07-17)**: issues #68–#74 were all filed against *this file* for drifting out of sync with the code — stale DB version, a test roster missing three files, shipped features listed as open work. Enumerated lists here rot; prefer pointing at the source of truth (`gh issue list`, `find app/src/test -name '*Test.kt'`) over restating it.

## Environment Setup (read this first)

- **JDK 17+ is required to run Gradle**, but the system default `java` on this machine is JDK 11 (`/usr/libexec/java_home` only lists 11 and 8). Android Studio ships a bundled JBR that works — prefix Gradle commands with:
  ```bash
  JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew <task>
  ```
  If Android Studio isn't installed at that path, find another JDK 17+ via `/usr/libexec/java_home -V`.
- **Android Studio must be Quail 1 (2026.1.1) or newer to sync this project.** The 2026-07-09 dependency bump moved the project to AGP 9.2.1; Studio Otter 3 (2025.2.3, installed on this machine as of 2026-07-10) caps IDE sync at AGP 9.0.0 and fails with "The project is using an incompatible version (AGP 9.2.1)". Downgrading AGP instead is NOT viable: `core-ktx:1.19.0` and the `lifecycle:2.11.0` artifacts require AGP 9.1+ (AAR metadata check fails the build). **CLI Gradle is unaffected** — build/test/lint/installDebug all work regardless of Studio version, so use the terminal to install on a device if Studio hasn't been updated yet.
- **`app/google-services.json` is required by the Google Services Gradle plugin but is gitignored** (it contains real Firebase project secrets — it was committed once by accident and deleted in commit `bd3f18e`, then re-added to `.gitignore`). As of 2026-07-08 this machine has the **real** config in place (project `apex-tracker-3ed29`) with the debug SHA-1 fingerprint registered in the Firebase console — Google Sign-In is verified working end-to-end on a physical device. On a **fresh clone/new machine**, you'll need to redo this yourself: Firebase Console → Project Settings → your Android app → add your machine's debug SHA-1 (`keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`) → download `google-services.json` → place at `app/google-services.json`. Without it, the build still succeeds with a stub placeholder, but sign-in/Firestore won't function.
  - **Key rotation (Issue #60, 2026-08-03)**: the Android API key committed by accident in `0428af5` (still retrievable from public git history even after `bd3f18e` deleted the file — GitHub history is immutable) was **regenerated** in Google Cloud Console and the old value is dead. The new key is restricted under Application restrictions → Android apps to `com.example.apextracker` + this machine's debug SHA-1 (`03:13:DC:3F:C0:44:38:BD:B3:A9:1A:5A:81:89:FF:5C:47:55:88:88`) — a fresh `google-services.json` was pulled and swapped into place, `assembleDebug` verified successful against it. **Any other machine's debug keystore (or a release keystore) must have its SHA-1 added under the same key in Cloud Console, or sign-in will fail on that machine/build with an unrestricted-app error.** Git history itself was left unscrubbed (the dead key has no further value to rotate away from); `firestore.rules` (added 2026-07-17) remains the real access-control boundary, not the API key.

## Build & Run Commands

```bash
# Build debug APK
JAVA_HOME="<jdk17-path>" ./gradlew assembleDebug

# Install on connected device
JAVA_HOME="<jdk17-path>" ./gradlew installDebug

# Run unit tests
JAVA_HOME="<jdk17-path>" ./gradlew test

# Run lint (treat NewApi/error-severity issues as build-blocking)
JAVA_HOME="<jdk17-path>" ./gradlew lintDebug

# Run instrumented tests (requires connected device/emulator)
JAVA_HOME="<jdk17-path>" ./gradlew connectedAndroidTest

# Run a single unit test class (the aggregate `test` task rejects --tests on Gradle 9.4;
# target the variant task instead)
JAVA_HOME="<jdk17-path>" ./gradlew testDebugUnitTest --tests "com.example.apextracker.ExampleUnitTest"

# Clean build
JAVA_HOME="<jdk17-path>" ./gradlew clean
```

Note: unit tests cover the pure logic extracted during the fix passes — run `find app/src/test -name '*Test.kt'` for the current roster rather than trusting a list here (it has gone stale twice). ViewModels themselves are still untested (they need Android framework/Robolectric), as is anything requiring Room or the Android framework.

## Architecture Overview

**ApexTracker** is an Android app built with Jetpack Compose, MVVM architecture, Room for local persistence, and Firebase (Auth + Firestore) for optional cloud sync.

### Navigation & Entry Point
- `MainActivity.kt` — single Activity (a **`FragmentActivity`**, required by the biometric lock's `BiometricPrompt` — see "Biometric lock" below); sets up `AuthViewModel`, theme state (`ApexTheme`, `isDarkMode`), and `FirebaseManager`. Passes theme callbacks down to `AppNavigation`. `onStop()` clears `UnlockSession` (re-locks gated modules on backgrounding).
- `AppNavigation` hosts a `NavHost` (startDestination `dashboard`) with routes: `dashboard`, `goals`, `overview`, `budget_tracker`, `study_tracker`, `screen_time`, `reminders`, `notes`. **The Dashboard is the home** (Dashboard feature, see the dated section below); the `NavHost` is wrapped in a `Scaffold` whose bottom `NavigationBar` (`AppBottomBar`) shows on the four primary routes (`dashboard`/`study_tracker`/`screen_time`/`budget_tracker`) plus a **More** overflow (`MoreSheet` → `overview`/`reminders`/`notes`); the bar hides on secondary routes (`goals`/`reminders`/`notes`/`overview`), which keep their own back arrow. Tab selection uses `popUpTo("dashboard"){saveState}`; More destinations use `popUpTo("dashboard")` so backing out returns home. **The old `MainMenu` card grid is retired** (Phase 4) — its settings sheet (account/sign-in, dark mode, theme accent, currency) was extracted into `AppSettingsSheet.kt` and is hosted from the Dashboard's settings gear. The `budget_tracker` and `notes` composables are still wrapped in a `LockGate` (biometric lock, Issue #45). The Overview stat cards and Tasks header navigate via an `onNavigate` callback (Issue #47).
- **The splash is the system splash** (`androidx.core:core-splashscreen`, wired via `installSplashScreen()` in `MainActivity.onCreate` and the `Theme.ApexTracker.Splash` style). The old hand-rolled `SplashScreen` composable and its `delay(2000)` are gone, as is the `showSplash` gate — so `AppNavigation` no longer does splash-gating on top of navigation, which this file previously flagged as a wart. Measured cold start is ~800ms to interactive.

### Modules (each has View + ViewModel + Data layer)
| Route | View | ViewModel | Entities |
|---|---|---|---|
| `dashboard` (home) | `DashboardView.kt` + `GoalsView.kt` | `DashboardViewModel.kt` | `Goal`, `GoalCompletion` |
| `overview` | `OverviewView.kt` | `OverviewViewModel.kt` | Aggregates all DAOs |
| `budget_tracker` | `BudgetTrackerView.kt` + `BudgetComponents.kt` + `BudgetCalendar.kt` | `BudgetViewModel.kt` | `BudgetItem`, `Category`, `Subscription` |
| `study_tracker` | `StudyTrackerView.kt` | `StudyViewModel.kt` | `StudySession` |
| `screen_time` | `ScreenTimeTrackerView.kt` | `ScreenTimeViewModel.kt` | `ScreenTimeSession`, `ExcludedApp`, `AppUsageLimit` |
| `reminders` | `ReminderView.kt` | `ReminderViewModel.kt` | `Reminder` |
| `notes` | `NoteView.kt` | `NoteViewModel.kt` | `Note` |
| `papers` | `PapersView.kt` | `PapersViewModel.kt` | `Paper`, `PaperTopic` |

Settings dialogs for each module live in `*Settings.kt` files (e.g., `BudgetSettings.kt`, `ReminderSettings.kt`).

Screen Time supports per-app daily limits (Issue #124): `AppUsageLimit` stores each device-local limit and the last notification date, while `ScreenTimeLimitNotifier` alerts once per app per day after usage crosses that limit.

`BudgetCalendar.kt`'s `BudgetCalendarView` is reachable as of 2026-07-11 (Issue #32): a list/calendar `IconButton` toggle in the Budget top bar, with the selected month hoisted into `BudgetTrackerApp` and shared between both views.

### Database
- `AppDatabase.kt` — Room singleton (`budget_database`), `exportSchema = true`. **Don't trust a version number written here** — this line has gone stale twice (it said v15 while the code was at v19); read the `@Database` annotation in `AppDatabase.kt` for the current version and DAO roster. As of 2026-08-27 it's **v24** (v23→v24 added `BudgetItem.type` for income tracking, Issue #218).
- **Migration policy**: **do not maintain a migration roster here** — read the `.addMigrations(...)` call in `AppDatabase.kt`, which is the source of truth and must register every hand-written `Migration(n, n+1)` through the current `@Database` version. As of DB v24, it registers the complete `MIGRATION_11_12` through `MIGRATION_23_24` chain before `.fallbackToDestructiveMigration()`. `MIGRATION_14_15` (Dashboard feature) is a **purely additive** migration — two `CREATE TABLE IF NOT EXISTS` for `goals` + `goal_completions`, no data copy — and the simplest pattern to copy for a new table (its DDL was diffed against the exported `app/schemas/…/15.json` and verified on a real populated v14→v15 upgrade on-device). `MIGRATION_11_12` (Issue #40's `isPinned` column on `notes`), `MIGRATION_12_13` (Issue #75's `monthlyLimit` column on `categories`), and `MIGRATION_13_14` (Issue #78's per-subject study sessions) are real hand-written `Migration`s and the pattern to follow — **add** a new `Migration(n, n+1)` to the `addMigrations(...)` chain for every schema change. `MIGRATION_13_14` is the one to copy for a **primary-key** change: `study_sessions` moved from PK `(date)` to PK `(date, subject)`, which SQLite can't do in place, so it does the create-new / copy / drop / rename dance and copies every old daily total into the `subject = ''` ("No subject") bucket for its date — no study data lost. Note no SQL `DEFAULT` on `subject` (the entity declares only a Kotlin default, which Room does not emit as a column default; adding one would make TableInfo mismatch at runtime). The `fallbackToDestructiveMigration()` behind the chain is the backstop for versions with no migration path, and it **drops every table** — see the MIGRATION POLICY comment at the top of `AppDatabase.kt` (Issue #17).
- `Converters.kt` — Type converters for `LocalDate`/`LocalDateTime`/`Recurrence` and other non-primitive types. This is the **only** `@TypeConverters` class registered on `AppDatabase`.
- `Recurrence.kt` — Data model for recurring reminders (frequency, end condition, custom days). Persisted via `Converters.kt` (Gson round-trip).
- There used to be a separate `RecurrenceConverter.kt` with a duplicate, never-registered implementation of the same conversion logic — it was **deleted** during the 2026-07-07 cleanup pass since it was entirely dead code (not wired into `AppDatabase`, not referenced anywhere).

### Authentication & Cloud Sync
- `AuthViewModel.kt` — Manages Google Sign-In via Credential Manager API (`androidx.credentials` + `googleid`), wraps `FirebaseAuth`. Exposes `user: StateFlow<FirebaseUser?>`, `isSyncing: StateFlow<Boolean>`, and `signInError: StateFlow<String?>`.
- `FirebaseManager.kt` — Handles all Firestore operations. Syncs, under `users/{uid}/...`: app settings (theme/dark mode), budget items, categories, subscriptions, notes, reminders, study sessions, excluded apps, per-device screen time (`users/{uid}/devices/{deviceId}/screen_time`), and — as of the Dashboard feature — **goals** (`users/{uid}/goals/{cloudId}`, UUID cloudId like reminders) and **goal completions** (`users/{uid}/goal_completions/{goalCloudId}|{date}`, composite doc id like study sessions via `goalCompletionDocId()`). Both follow the standard 5-part shape (`parseXDoc`/`pushX`+`deleteX`+`pullAllX`/`applyXDoc`+`removeX`/`collectXChanges`/`syncX`), are registered in `performInitialSync` and `SyncCoordinator`, and are unit-tested in `FirebaseDocParsingTest`. `DashboardViewModel` pushes fire-and-forget via `safeCloudCall` on every mutation, same convention as the other ViewModels.
- Cloud sync is optional — the app works fully offline using Room as the source of truth.
- **Firestore rules (`firestore.rules` at repo root, added 2026-07-17 for Issue #61, deployed 2026-08-03)** — the only real enforcement of per-user isolation. `FirebaseManager` builds every path from `auth.currentUser?.uid`, but that's client-side and proves nothing; sign-in is open registration (`setFilterByAuthorizedAccounts(false)`), so any Google account is an authenticated client. The rule is `request.auth.uid == userId` on `users/{userId}/{document=**}`, default-deny everything else. **Committing the file does not deploy it** — run `firebase use <project-id> && firebase deploy --only firestore:rules`, and treat the Firebase console (Firestore → Rules) as the source of truth for what's actually live. **The rules were only actually deployed on 2026-08-03** (alongside the #60 key rotation) — from 2026-07-17 to then, the committed file had zero effect on the live project; before that, sync had been failing outright with `PERMISSION_DENIED` because the Cloud Firestore API itself was never enabled for `apex-tracker-3ed29` (a separate toggle from the rules — enabling one doesn't enable the other), which is also now fixed. Verified on-device: signed-in writes now land under `users/{uid}/...` in the Firebase console with no `PERMISSION_DENIED` in logcat. There is deliberately no `.firebaserc` (it would commit the project id) — pick the project with `firebase use` instead; this is no longer tied to #60's exposure risk (key is rotated) but avoiding it costs nothing.
- **Backup exclusions (`res/xml/data_extraction_rules.xml` + `res/xml/backup_rules.xml`, Issue #62)** — `allowBackup` is still true, but the Room DB and the `device_identity` prefs are excluded from both cloud backup and device transfer. Empty rules mean "back up everything", so those files are load-bearing; the rationale for each exclusion is in the XML comments. The two files must be kept in sync (API 31+ reads the first, API 26–30 the second).
- **Sync architecture (rebuilt 2026-07-09, Issue #4, branch `fix/issue-4-firebase-sync`)**: every ViewModel holds its own `FirebaseManager(application)` and pushes/deletes fire-and-forget on every mutation via the shared `safeCloudCall()` helper (top-level in `FirebaseManager.kt`) — failures are logged, never crash, Room stays source of truth. `cloudId` (UUID) + `modifiedAt` are assigned in the ViewModel at creation time; updates bump `modifiedAt` (and assign a cloudId if empty, covering pre-existing rows). Study sessions throttle to one push per 60s while the timer runs (`shouldSyncNow()` in `SyncThrottle.kt`), forced on pause/reset/day-rollover. `performInitialSync()` runs on **two** triggers (Issue #17, 2026-07-13, PR #51): the interactive sign-in transition (null → non-null user), and cold start with a persisted signed-in session — Firebase Auth restores the user before composition, so the sign-in transition never fires for returning users, and without the cold-start trigger cross-device changes only arrived after a manual sign-out/sign-in. `shouldRunInitialSync(signedIn, wasSignedOut, alreadyRanThisProcess)` in `InitialSyncGate.kt` is the pure decision function (unit-tested in `InitialSyncGateTest`); `MainActivity` guards the `performInitialSync()` call site with it. The sync itself: parses each pulled doc via pure `parseXDoc()` functions (top of `FirebaseManager.kt`, unit-tested in `FirebaseDocParsingTest`) with per-doc try/catch+logging; isolates each entity behind `syncStep()` so one failure can't abort the others; migrates legacy blank-cloudId budget docs written by the old ad-hoc path (`classifyLegacyBudgetDoc()`, unit-tested in `LegacyBudgetDocMigrationTest`); and pushes **all** local rows after the pull (assigning UUIDs where missing) so data created/edited while signed out reaches the cloud. **Real-time sync (Issue #37, 2026-07-13)**: `SyncCoordinator` (object, started/stopped from `MainActivity`'s existing sign-in `LaunchedEffect` after `performInitialSync()` completes) runs one live Firestore listener per entity via `FirebaseManager.collectXChanges(db)` — `callbackFlow` + `addSnapshotListener`, using `snapshot.documentChanges` (`ADDED`/`MODIFIED`/`REMOVED`) rather than diffing full result sets, same `hasPendingWrites()` echo-guard as `getSettingsFlow()`. The "apply one cloud doc to Room" logic is shared between `performInitialSync` and the listeners via `applyXDoc()`/`removeXByCloudId()` helpers (top of `FirebaseManager.kt`) — no duplicated parse/reconcile logic. Reminders' parent-link resolution is similarly shared via `resolveReminderParentLinks()`. Study sessions are insert-only (no remote-delete handling — local timer stays source of truth); excluded apps' `REMOVED` events map directly to the existing `includeApp()` DAO call (no cloudId indirection, `packageName` is the doc id). Cross-device screen time is separate — `getOtherDevicesScreenTimeFlow()` isn't Room-backed, it feeds `ScreenTimeViewModel`'s `DeviceSession` list directly via a parent listener on `devices` plus dynamically-managed child listeners per device's `screen_time/{today}` doc. **Known limitation, unaffected by this change**: deletes performed while signed out still leave the cloud doc in place (no tombstones) → the item resurrects on the next initial sync.

### Theming
- **The `ui/theme/` package no longer exists.** `Color.kt`, `Theme.kt` and `Type.kt` were deleted in the 2026-07-29 redesign foundation; everything moved to `ui/design/`. There is one theme entry point, not two.
- `ui/design/ApexPalette.kt` — hand-authored dark and light `ColorScheme`s plus the heatmap ramp. `shiftColorForLightMode()` is **gone**: light mode is authored, not HSV-derived from dark.
- `ui/design/ApexType.kt` — `ApexTypography` (**Martian Mono** display+headline / Geist UI) and `ApexNumerals` (**Martian Mono**, for every user-facing quantity). Fonts are bundled in `res/font/`, OFL text in `assets/licenses/`. (As of the 2026-07-30 Graphite pass, Instrument Serif and Geist Mono are **retired** — the display voice and the numerals are one mono. See "2026-07-30 Graphite identity" below.)
- `ui/design/ApexTokens.kt` — `ApexSpacing`, `ApexShapes`, `ApexMotion`, `ApexElevation`, `isLightScheme`, `ApexSemantics`/`LocalApexSemantics`, the `ApexTheme` enum, and `ApexTrackerTheme`. **`ApexElevation` is how a control that genuinely lifts separates from the substrate** — a shadow in light, a hairline ring in dark, because `shadowElevation` renders nothing over the dark background (`GraphiteBase`, `#0E0F11`). See `Design.md` §5 for which half keys off the theme and which off the fill; don't hand-roll a `shadowElevation` at a call site.
- `ui/design/ApexComponents.kt` — the shared component vocabulary (`ApexSectionHeader`, `ApexDivider`, `ApexStatRow`, `ApexChartFrame`, `ApexGroup`, `ApexEmptyState`).
- **`ApexTheme` has one entry (`GRAPHITE`)** — the cold-monochrome identity (2026-07-30), down from four accents then the single warm `EMBER`. The enum survives so a second identity is a drop-in; the settings accent picker is gone. `FirebaseManager` still round-trips the field by name and legacy values (`EMBER`/`EMERALD`/…) fail `valueOf` and fall back, which is correct.
- **Sage (the positive semantic) is deliberately NOT in the `ColorScheme`** — it lives only in `ApexSemantics.positive`. It used to be `secondary`/`secondaryContainer`, which made every M3 component defaulting to secondary render "goal met" green.
- **Every `ColorScheme` slot the app can reach is defined in *both* schemes** — audited 2026-07-30 (`Design.md` §8), ported onto graphite the same day. An undefined slot falls back to Material's purple-tinted *baseline*, silently, and **you cannot find it by grepping for `colorScheme.`**: the slot is reached through a component's own defaults, so the offending call sites are the ones that mention no colour. That audit found menus, all 19 `AlertDialog`s, `DatePickerDialog`, one `ModalBottomSheet` and all three snackbar colours rendering off-palette. `ApexPaletteSlotsTest` and the style plate's "undefined-slot detector" section now guard it. **Adding a new M3 component type means adding its slots to both schemes.**
- **`apexMenuBorder()` is not optional.** `surfaceContainer` (menus) and `surfaceContainerHigh` (dialogs) share one tone by design, so the hairline is the only thing separating a dropdown from the dialog it opens on. Pass it at every `DropdownMenu`/`ExposedDropdownMenu` call site.
- **`Design.md` at the repo root is the authority** for all values, measured contrast ratios, the chart spec and the screen inventory. `.claude/skills/android-product-design/SKILL.md` carries the enforcing rules. Don't restate values here — that's how this file drifted before.

### Background Work
- `ReminderWorker.kt` — a `CoroutineWorker` that posts a notification via the `reminder_channel` notification channel. As of the 2026-07-09 fix pass it is reachable: `ReminderScheduler` (object) sets an exact `AlarmManager` alarm per active reminder → `ReminderAlarmReceiver` (BroadcastReceiver) enqueues `ReminderWorker` via WorkManager → notification posts. `ReminderBootReceiver` re-arms alarms after reboot. Scheduling is wired into every `ReminderViewModel` mutation path (add/update/toggle/delete/settings changes).
- **Notification actions (Issue #41, 2026-07-14)**: the notification carries **Done** and **Snooze 10 min** buttons, both `PendingIntent.getBroadcast` into `ReminderActionReceiver` (manifest-registered, `exported="false"`) with distinct request codes (`id` / `-id`) so they don't share one PendingIntent.
  - **Done** → `ReminderCompleteWorker` via `enqueueUniqueWork("complete_reminder_$id", KEEP)`. WorkManager rather than a raw coroutine because the DB + network work must outlive the receiver's ~10s window and run with the app process dead.
  - **Snooze** → reads the row from Room (via `goAsync()`) and re-arms the alarm 10 minutes out, no DB write. It no-ops if the reminder is already completed or deleted (Issue #64) — the notification can outlive the reminder. A reboot mid-snooze re-arms from the real due time via `ReminderBootReceiver`, not the snooze (accepted tradeoff).
- **When an alarm fires (`ReminderScheduler.resolveTriggerTime`, Issue #80, 2026-07-17)** — the single "when, if ever" decision, pure and unit-tested in `ReminderSchedulerTest`. The offset setting means "notify N minutes *before* the task", so for a task nearer than N minutes the raw `computeTriggerTime()` lands in the past. It used to be dropped silently (alarm cancelled, reminder still looking active, no feedback) — with the 30-minute default that meant **any reminder set less than 30 minutes out never notified**, which is exactly how the feature gets first tested. It now clamps to `now` and only gives up once the task's own due time has passed. All-day reminders take no offset, so their past trigger is the real notification time having gone by and is deliberately **not** clamped. `scheduleReminderIfNeeded` logs the give-up case — without it, "notifications are broken" is only diagnosable via `adb shell dumpsys alarm`.
- `ReminderCompletion.kt` — **the shared completion path**: `completeReminder()` and `scheduleReminderIfNeeded()` are top-level functions used by *both* the in-app checkbox (`ReminderViewModel.toggleCompletion`) and the notification's Done action, so recurrence advancement / alarm cancel / cloud push can't drift apart. They're top-level precisely because a `BroadcastReceiver` has no `AndroidViewModel` to hang off. `completeReminder()` claims the completion with an atomic compare-and-set (`ReminderDao.markCompletedIfActive`, `WHERE id = :id AND isCompleted = 0`) and bails if it loses — the two call paths can race across *processes*, so `ReminderViewModel`'s in-memory `togglesInFlight` set can't cover it, and a lost race would insert a duplicate next occurrence for a recurring reminder (Issue #63). **Any new completion path must go through `completeReminder()`.**

### Database encryption (Issue #117)
`budget_database` is SQLCipher-encrypted under a 32-byte key wrapped by an Android Keystore
AES-GCM key. `DatabaseEncryption.kt` owns all of it; `AppDatabase` just asks it for an
`openHelperFactory`.

- **What it protects**: the file once it leaves the device (a rooted pull, an offline image, a
  file-access exploit). The Keystore key is deliberately **not** user-authentication-bound —
  reminders, widgets and WorkManager all open the database while the screen is locked, and
  requiring an unlock would break every one of them. Against someone holding the unlocked phone,
  the biometric lock is the control, not this.
- **Never destroys readable data.** Every failure path degrades instead: no Keystore key on a new
  install → run unencrypted; conversion fails → the untouched plaintext file is opened as before;
  an encrypted file with no key → renamed `.unreadable` and kept, never deleted, and the app
  starts fresh (signed-in users re-pull, same guarantee `fallbackToDestructiveMigration` relies on).
- **Existing installs convert once, in place**, before Room opens the file — that is the only
  moment it can happen. `user_version` has to survive the copy or Room sees version 0 and runs the
  destructive fallback.

Four things about `net.zetetic:sqlcipher-android` that are not in the obvious docs, each of which
cost a debug cycle here:
1. **It does not load its own native library** and has no `loadLibs` helper (the old artifact did).
   Without an explicit `System.loadLibrary("sqlcipher")` everything fails with `UnsatisfiedLinkError`.
2. **Open flags propagate to `ATTACH`.** Opening the source without `CREATE_IF_NECESSARY` means the
   attach cannot create the destination — `SQLITE_CANTOPEN`, or silently no file and no error.
3. **A text key literal and the same bytes as a blob derive different keys.** `KEY 'text'` produces
   a file that `SupportOpenHelperFactory(byte[])` then rejects as "file is not a database". Use the
   `x'<hex>'` blob form everywhere, and verify by reopening through the byte[] overload.
4. **`PRAGMA cipher_memory_security = OFF` is required in practice.** Its `mlock` of key pages fails
   continuously against Android's small `RLIMIT_MEMLOCK` (`errno=12`), which ANR'd the app on first
   load. Applied via an `SQLiteDatabaseHook` on every connection.

Cost: **+0.35MB** of APK for the classes, plus ~4–6MB of `libsqlcipher.so` per ABI.

### Home-screen widgets (Glance)
Everything lives in `widget/`; each provider is a `GlanceAppWidget` + a `GlanceAppWidgetReceiver`
registered in the manifest against an `res/xml/*_widget_info.xml`. **Don't restate the roster here**
— read the `<receiver>` entries in `AndroidManifest.xml`.

- **Glance runs in the launcher's process and cannot read the app's Compose `ColorScheme`.**
  `WidgetPalette.kt` is the single hand-kept mirror of `ApexPalette`'s GRAPHITE dark values; nothing
  enforces the correspondence, so changing `ApexPalette` means changing it too. Widgets are always
  dark, in both launcher themes.
- **A widget reads a snapshot, never a flow** — `loadDashboardSnapshot` (#130/#131),
  `loadBudgetWidgetSnapshot` (#167), `loadTodaySnapshot` (#44). The snapshot function is where the
  DAO calls and the pure logic meet, and it is shared with the app so the two can't disagree.
- **Refresh is explicit.** `updatePeriodMillis` bottoms out at 30 minutes, so the `refresh*Widget`
  helpers in `ApexWidgets.kt` are called from the app's own write points. Pick the hook by how
  often the source changes: `ReminderViewModel` collects its table (six mutation methods, plus sync
  and restore, for free), `StudyViewModel` fires only on start and the `forcePush` saves (**never
  the per-second tick**), and `ScreenTimeViewModel` fires only when the *displayed minute* moves.
  Updates land through a WorkManager `SessionWorker`, so a refresh issued as the app backgrounds
  arrives a few seconds later — that lag is Glance, not a missed hook.
- **Widgets are outside the biometric lock's reach** (Issue #187): a gated module withholds its
  figures in the *snapshot*, not the layout, because the snapshot is what crosses into the
  launcher's process. See `budgetWidgetSnapshot`.
- Deep links go through `MainActivity.EXTRA_NAVIGATE_TO`, which `sanitizeRequestedRoute` filters
  against `APP_ROUTES` (Issue #105).
- **A widget that *writes* goes through a shared top-level path, never its own copy of the logic.**
  The study widget's start/pause button (#132) calls `StudyTimerControl.kt`, which `StudyViewModel`
  also calls — same reason `ReminderCompletion.kt` is top-level. `StudyTimerStateStore` is the
  durable record of whether the stopwatch runs and Room is the record of what it has banked; the
  ViewModel only *mirrors* both, and collects `StudyTimerStateStore.runningFlow()` so a launcher
  toggle reaches it instead of leaving that mirror stale. Read `StudyTimerControl.kt` before adding
  any other way to start or stop the stopwatch.

### Receipt scanning (Issue #46)
A camera button in `BudgetItemDialog`'s amount field picks a photo (`PickVisualMedia`, so **no
permission is ever requested**) and prefills title / amount / date. Nothing is auto-saved and the
image is never copied into app storage.

- **`ReceiptOcr.kt` is the only file that touches ML Kit**, so the dependency is one deletion away.
  It uses the **Play-Services-delivered** recognizer, not `com.google.mlkit:text-recognition`:
  bundling the model costs 45MB of APK (39MB of native libraries, one per ABI) against 0.35MB for
  the wrapper, and the app already requires Play Services. The manifest's
  `com.google.mlkit.vision.DEPENDENCIES` meta-data asks for the model at install time.
- **`ReceiptParse.kt` is where the feature actually lives** — pure and unit-tested in
  `ReceiptParseTest`. Two things there are non-obvious and were both found by scanning a real
  photo, not by reading the API:
  - **`reflowReceiptLines` is load-bearing.** Text recognition groups by *block*, and a receipt's
    label column and amount column are usually two blocks — so `Text.text` returns every label,
    then every amount, and `TOTAL` never shares a line with its figure. Rebuilding visual rows from
    the lines' bounding boxes is what makes label-based ranking work at all. Don't "simplify" this
    back to `result.text`.
  - Ranking: labelled lines (TOTAL/AMOUNT DUE…) first, subtotal/tax/change **dropped** rather than
    ranked low, and among unlabelled numbers a figure *with cents* beats a larger bare integer —
    otherwise a street number in the address wins.

### Permissions
- `PACKAGE_USAGE_STATS` + `QUERY_ALL_PACKAGES` — Required for screen time tracking.
- `POST_NOTIFICATIONS` — Requested at runtime (API 33+) in `MainActivity.onCreate` via `registerForActivityResult` (added 2026-07-09).
- `SCHEDULE_EXACT_ALARM` + `RECEIVE_BOOT_COMPLETED` — For exact reminder alarms and re-arming them after reboot (added 2026-07-09). `ReminderScheduler` falls back to inexact `setAndAllowWhileIdle` if the user revokes exact-alarm permission on API 31+.

## Key Conventions
- All ViewModels extend `AndroidViewModel` and access Room through `AppDatabase.getDatabase(application)`.
- Firebase sync is fire-and-forget inside `viewModelScope.launch` via `safeCloudCall()`; local Room is always updated first. As of 2026-07-09 (Issue #4) this convention is actually implemented across all ViewModels — see "Authentication & Cloud Sync" above.
- Light/dark mode detection in Composables uses the extension `Color.isLight()` defined at the bottom of `MainActivity.kt`.
- The `BudgetViewModel` auto-creates `BudgetItem` entries for due subscriptions on init and on any subscription change (`checkAndAddSubscriptions()`), which back-fills one `BudgetItem` per elapsed month if a subscription's renewal date is far in the past.
- "Xh Ym" duration formatting goes through the shared `formatDurationCompact(millis)` in `DurationFormat.kt` (consolidated 2026-07-09). `StudyTrackerView.formatTime` — the study timer's HH:MM:SS/MM:SS readout, kept separate from `formatDurationCompact` because it's a different format — was itself **deleted 2026-08-01** (`c3f6c93`): the timer no longer renders a formatted string at all, it renders digit groups via `flipClockGroups()` in `ui/design/ApexFlipClock.kt` (see "2026-08-01 Study timer flip clock" below). Periodic 30s polling loops go through `CoroutineScope.launchPeriodic()` in `PeriodicRefresh.kt`. Currency rendering goes through `formatCurrency(amount, currencyCode)` in `CurrencyFormat.kt` — the code comes from the user's stored setting (`CurrencySettings` DataStore, surfaced by `AppSettingsSheet`'s `CurrencyDropdown` and synced to Firestore; Issue #76). USD is only the fallback `parseCurrencySafe()`/`defaultCurrencyCode()` land on, not a hardcoded default. Duration axis labels for the trend charts go through `durationAxisLabels()` in the same file as `formatDurationCompact` (Issue #97). **Every user-facing quantity renders in `ApexNumerals` (Martian Mono, tabular)** — currency, durations, the study timer, percentages, counts, axis labels. Proportional figures make a running timer jitter and a currency column ragged. Layout values come from `ApexSpacing`/`ApexShapes` and animations from `ApexMotion`; a raw `.dp`, hex, `spring()` or `tween()` at a call site is a bug. User-facing UI strings live in `res/values/strings.xml` via `stringResource()` (Issue #36) — add new UI strings there, keyed by screen (`budget_*`, `reminders_*`, shared `action_*`). contentDescriptions were extracted too (Issue #53, PR #54): every one is now either `stringResource(R.string.cd_*)` or `null` for decorative icons — keep it that way.

## Known Issues (as of 2026-07-07 audit)

This section exists so the next work session doesn't have to rediscover these from scratch. Ordered roughly by severity/impact. **2026-07-09 status update**: most of these were addressed on branch `fix/known-issues-3-through-10` (one commit per issue) — each section below is annotated with what was fixed and what remains. All fixes verified via `assembleDebug` + unit tests + `lintDebug` (0 errors) only; **no device/emulator was available**, so an on-device smoke test is still owed before closing the GitHub issues.

Each section below is tracked as a GitHub issue, numbered in recommended fix order: [#3](https://github.com/aadityad12/Trackers/issues/3) Reminders, [#4](https://github.com/aadityad12/Trackers/issues/4) Firebase sync, [#5](https://github.com/aadityad12/Trackers/issues/5) Overview display bugs, [#6](https://github.com/aadityad12/Trackers/issues/6) Notes, [#7](https://github.com/aadityad12/Trackers/issues/7) Screen Time accounting, [#8](https://github.com/aadityad12/Trackers/issues/8) Auth polish, [#9](https://github.com/aadityad12/Trackers/issues/9) code-duplication cleanup, [#10](https://github.com/aadityad12/Trackers/issues/10) dependency bumps.

### [Issue #3] Reminders — notifications don't fire (highest-impact bug) — **mostly fixed 2026-07-09**
1. ~~`ReminderWorker` never enqueued~~ **Fixed**: exact `AlarmManager` alarms via new `ReminderScheduler`/`ReminderAlarmReceiver`/`ReminderBootReceiver` (see "Background Work" above).
2. ~~`POST_NOTIFICATIONS` never requested~~ **Fixed**: requested at runtime in `MainActivity.onCreate`.
3. ~~Dropdowns hardcoded `expanded = false`~~ **Fixed**: both dropdowns in `RecurrencePickerDialog.kt` now use real `remember` state.
4. ~~Recurrence advancement only happens on manual "complete"~~ **Resolved 2026-07-10** (refiled as issue #30, Option A): overdue recurring reminders stay visible; completing one catches the chain up past today via `calculateNextOccurrenceAfter` — see the 2026-07-10 pass below.
5. ~~Recurrence picker resets to defaults when editing~~ **Fixed**: prefills from the reminder's existing `Recurrence` via new `initialRecurrence` param.

### [Issue #4] Firebase sync — architecture inconsistencies — **fixed 2026-07-09 (second pass, branch `fix/issue-4-firebase-sync`)**
- ~~Budget items' two competing sync paths~~ **Fixed**: `BudgetViewModel`'s ad-hoc `firestore`/`auth` path (`syncItemToCloud`, Room-id-keyed docs) deleted; everything routes through `FirebaseManager`'s cloudId scheme. Legacy Room-id-keyed docs in Firestore are auto-migrated/deleted during `syncBudgetItems` via `classifyLegacyBudgetDoc()` (dedup-guarded, unit-tested).
- ~~Sync is "once at sign-in," not continuous~~ **Fixed**: all ViewModels now push/delete on every mutation (see "Authentication & Cloud Sync" above). ~~cross-device pull still requires a sign-in sync~~ **Fixed 2026-07-13** (Issue #37, live Firestore listeners — see below). Remaining gap, deliberately out of scope: no tombstones for offline deletes.
- ~~`checkAndAddSubscriptions()` race~~ **Fixed**: now guarded by a `Mutex`, and the catch-up loop calls DAOs directly instead of re-entrant public methods (which used to spawn nested launches re-triggering the check).
- ~~`syncReminders()` first-sync `parentCloudId` ordering bug~~ **Fixed**: extracted into pure `resolvePendingReminderCloudIds()` (top of `FirebaseManager.kt`) which threads batch-assigned cloudIds; unit-tested order-independent.
- ~~Cloud-document parsing silently drops malformed documents~~ **Fixed**: pure `parseXDoc()` functions throw on malformed docs; sync loops catch per-doc and `Log.w` with the doc id; `performInitialSync` isolates each entity behind `syncStep()` so one bad doc/entity can't abort the rest (previously a single bad date string aborted the whole sync).

### [Issue #5] Overview module — display bugs — **fixed 2026-07-09**
- ~~Total spent rounds to whole dollars~~ **Fixed**: `"%.2f"`.
- ~~Study/screen time shown as raw minutes~~ **Fixed**: both use `formatDurationCompact()`.
- **Still open (perf-only, not a bug)**: `OverviewViewModel` recomputes aggregates by scanning entire tables on every combine — revisit only if it becomes a performance issue.

### [Issue #6] Notes module — **partially fixed 2026-07-09**
- **Unconfirmed**: the "backspacing a bullet needs two keystrokes / leaves a dangling glyph" report did NOT reproduce through the pure edit-diffing logic — a unit test (`NoteBulletEditingTest`) shows an empty bullet line clears in one keystroke via `handleNoteContentChange`. If it happens on-device, it's likely IME-batching-specific; needs a device repro before changing the regex.
- ~~"Indent" on a plain line creates a level-2 bullet~~ **Fixed**: Indent now leaves non-bulleted lines untouched.

### [Issue #7] Screen Time — usage accounting edge cases — **fixed 2026-07-09**
- ~~Undercounting/overcounting in `calculateAppSpecificUsage()`~~ **Fixed**: event processing extracted into pure `aggregateForegroundDurations()` (`ScreenTimeUsageAggregator.kt`, unit-tested). Back-to-back `RESUMED` no longer resets the start time; a session already foregrounded before the window is counted from window start; `SCREEN_NON_INTERACTIVE` (API 28+) closes out the foreground app on screen lock.
- ~~cross-device totals lag up to ~30s (one-shot fetch on the 30s polling loop, no live Firestore listener)~~ **Fixed 2026-07-13** (Issue #37): the *other-devices* portion is now a live listener (`FirebaseManager.getOtherDevicesScreenTimeFlow()`); this device's own 30s poll is unchanged (still the only way to measure local `UsageStatsManager` data).

### [Issue #8] Auth — **mostly fixed**
- ~~Credential unwrap bug~~ **Fixed** earlier (PR [#2](https://github.com/aadityad12/Trackers/pull/2)).
- ~~`AuthStateListener` leak~~ **Fixed 2026-07-09**: listener stored and removed in `onCleared()`.
- ~~`signOut()` leaves `isSyncing`/`signInError` stale~~ **Fixed 2026-07-09**: both reset on sign-out.
- ~~Theme-sync echo loop~~ **Fixed 2026-07-10** (refiled as issue #31): `getSettingsFlow()` now skips snapshots with `hasPendingWrites()` — only server-acknowledged remote state drives the theme listener.

### [Issue #9] Study Tracker (code-duplication cleanup) — **fixed 2026-07-09**
- ~~Duplicated 30s polling loops~~ **Fixed**: both use `launchPeriodic()` (`PeriodicRefresh.kt`). Still always-on polls by design (30s tolerance accepted).
- ~~Three hand-rolled duration formatters~~ **Fixed**: `formatTimeCompact`/`formatMillis` merged into `formatDurationCompact()` (`DurationFormat.kt`); `formatTime` (stopwatch HH:MM:SS) intentionally kept separate at the time — **since deleted** (2026-08-01, `c3f6c93`): the plain-text stopwatch it fed no longer exists, replaced by the split-flap clock's digit renderer.

### [Issue #10] Dependency freshness — **fixed 2026-07-09**
All catalog versions bumped to latest (AGP 9.2.1, Kotlin 2.4.0, KSP 2.3.9, Compose BOM 2026.06.01, Room 2.8.4, Firebase BOM 34.16.0, etc.), Gradle wrapper 9.1.0 → 9.4.1, compileSdk 35 → 37 (targetSdk stays 35 — no runtime behavior opt-ins). **AGP 9 migration notes**: the standalone `org.jetbrains.kotlin.android` plugin is gone (AGP 9 has built-in Kotlin and refuses it); `kotlinOptions{}` became `kotlin { compilerOptions {} }` in `app/build.gradle.kts`. KSP is standalone-versioned from 2.3.0 (no longer `<kotlin>-<ksp>` coupled). Coil intentionally left at 2.7.0 (Coil 3 = artifact/package migration, not a bump). Verified by build/tests/lint only — **needs an on-device smoke test** (sign-in, sync, each module) before merging.

## 2026-07-07 Cleanup Pass (what was already fixed — don't re-flag these)

- Removed a duplicate `id("com.google.gms.google-services") version "4.4.4" apply false` plugin declaration in `app/build.gradle.kts` that collided with the version-catalog alias applied on the line above it (build-breaking).
- Added a gitignored placeholder `app/google-services.json` so the project builds without real Firebase secrets (see Environment Setup above); added `app/google-services.json` to `.gitignore`.
- Added missing Gradle dependencies that were imported in source but never declared: `androidx.credentials`, `androidx.credentials:credentials-play-services-auth`, `googleid`, `coil-compose` (all present in `libs.versions.toml` already, just missing `implementation(...)` lines in `app/build.gradle.kts`) — this was a build-breaking compile error.
- Fixed `ScreenTimeViewModel.kt`/`ScreenTimeTrackerView.kt`: these referenced a `DeviceUsage` data class and `FirebaseManager.getAggregatedScreenTime()`/`uploadScreenTime()` methods that no longer existed after `FirebaseManager.kt` was replaced wholesale by a later commit with a different API (`DeviceSession`, `uploadScreenTimeSession()`, `getOtherDevicesTodayUsage()`). Updated the ViewModel/View to use the current API — `aggregatedUsage` is now a `MutableStateFlow<List<DeviceSession>>` refreshed via a one-shot call alongside the existing 30s polling loop. This was a build-breaking compile error.
- Fixed `ScreenTimeViewModel.checkPermission()`: called `AppOpsManager.unsafeCheckOpNoThrow` which requires API 29, but `minSdk` is 26 — would crash on Android 8.0–9.0 devices below API 29. Now branches on `Build.VERSION.SDK_INT` and falls back to the deprecated `checkOpNoThrow` below API 29. (Caught by `lintDebug`, which now passes with 0 errors.)
- Removed dead code: orphaned `RecurrenceConverter.kt` (duplicate of `Converters.kt`, never registered on `AppDatabase`, never referenced); `FirebaseManager.authStateFlow()` (unused, duplicated by `AuthViewModel`'s own listener); `StudySessionDao.updateSession()` (unused — all writes go through `insertSession` with `REPLACE`); `BudgetViewModel.observeCloudChanges()` (registered a Firestore snapshot listener whose body did nothing but comments, and was never removed — a no-op leak); unused imports in `BudgetTrackerView.kt` (`detectTapGestures`, `pointerInput`, `LocalHapticFeedback`, `HapticFeedbackType`, `TextDecoration`, `atan2` — leftover from removed gesture-based pie-chart code); dead color constants in `ui/theme/Color.kt` (`ElectricBlue`, `CyberCyan` — exact duplicates of `OceanPrimary`/`OceanSecondary`; `CyberGreen`, `SoftGreen` — unused).
- Renamed `alphaAnim` → `scaleAnim` in `MainActivity.kt`'s `SplashScreen` — the variable was used as a `.scale()` modifier, not alpha/opacity; the misleading name was vestigial from an earlier fade-based design, per the developer's own note in `notes.txt` about splash-screen cleanup.
- Verified: `./gradlew assembleDebug`, `./gradlew test`, and `./gradlew lintDebug` all pass clean (lint: 0 errors after the `AppOpsManager` fix; ~45 pre-existing warnings remain, mostly dependency-freshness and a few `@OptIn`/deprecation notices — none build-blocking).

## 2026-07-10 Bug-Fix Pass (Issues #18–#31, PRs #48 + #50 — all merged, issues closed)

Fixed all open bug-labeled issues, one commit per issue, verified by build/tests/lint per commit plus on-device testing (Samsung SM-S931U1, Android 16). New architecture pieces to know about:

- **Recurrence advancement is now pure and lives in `Recurrence.kt`** (moved out of `ReminderViewModel`): `calculateNextDate` (respects new nullable `Recurrence.anchorDay` — anchors monthly/yearly chains to the original day-of-month so short-month clamping doesn't drift, #26), `withAnchorFrom`, and `calculateNextOccurrenceAfter` (catch-up past today for missed chains, #30 Option A: skipped periods don't count toward AFTER_OCCURRENCES). `anchorDay` is null on legacy persisted data and fills in lazily on the next advancement.
- **Reminder completion integrity**: `toggleCompletion` re-reads the row and holds a per-id in-flight guard (double-tap can't insert two next occurrences, #25); `OverviewView` routes toggles through `ReminderViewModel.toggleCompletion` instead of `OverviewViewModel`'s deleted raw-DAO flip (#18).
- **Notification tap** opens the Reminders screen: `ReminderWorker` sets a `contentIntent` with `MainActivity.EXTRA_NAVIGATE_TO`; `MainActivity` holds a `pendingRoute` (set in `onCreate`/`onNewIntent`) that `AppNavigation` consumes once the NavHost exists (#19; the splash gate it used to wait on is gone as of 2026-07-29).
- **Exact alarms**: the Reminders screen shows a grant banner when `ReminderScheduler.canScheduleExactAlarms()` is false (denied by default on API 33+), deep-links to `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`, rechecks on resume, and re-arms all alarms via `ReminderViewModel.rescheduleAll()` on grant (#21).
- **`Converters.kt` never throws**: pure `parse*Safe` helpers; corrupt dates fall back to an epoch/midnight sentinel (null would still crash non-null fields), corrupt `Recurrence` to null; `parseRecurrenceSafe` rejects Gson output with null `frequency`/`endType`. Note: `LocalDate.EPOCH` is API 34+ — lint caught this; use `LocalDate.of(1970,1,1)` (#22).
- **Study timer**: `rolloverIfNeeded()` runs synchronously in every session-write path and saves target `lastResetDate` (not `LocalDate.now()`), killing the midnight misattribution window (#27). `StudyTimerStateStore` (SharedPreferences) persists the running stopwatch; same-day process death resumes seamlessly, cross-day death credits the old day to its midnight boundary via pure `finalizeSecondsAtEndOfDay` (#24).
- **Screen time**: `calculateAppSpecificUsage()` is now `suspend` + `Dispatchers.IO` (blocking `queryEvents` + full-day event loop used to run on Main from three paths, #23).
- **Misc**: `deleteCategory` detaches referencing BudgetItems (null `categoryId`, Room + cloud) before deleting (#20); null/blank `ANDROID_ID` falls back to a persisted per-install UUID instead of shared `"unknown_device"` (#28); `parseColorSafe()` in `ColorUtils.kt` guards all category-color rendering (#29, PR #48); `getSettingsFlow()` skips `hasPendingWrites` snapshots (#31).

Every issue in this pass is closed. For what's still open, ask GitHub (`gh issue list`) rather than trusting a list here — this line has gone stale once already.

## 2026-07-11 → 2026-07-14 Feature Pass (Issues #17, #32–#41, #53)

The UI-polish issues (#32–#36) are documented inline in the sections above (Navigation, Theming, Key Conventions). The rest:

- **Cold-start initial sync (#17, PR #51)** — see "Authentication & Cloud Sync" above. Also added the `MIGRATION_11_12` migration policy to `AppDatabase.kt` (see Database above): a destructive migration wiping local data is only survivable if something pulls it back from Firestore, which is exactly what #17 fixed.
- **Real-time sync (#37)** — see "Authentication & Cloud Sync" above (`SyncCoordinator` + per-entity Firestore listeners).
- **contentDescription extraction (#53, PR #54)** — 23 `cd_*` keys in `strings.xml`; decorative icons take `null`. Finished the job #36 started.
- **Budget CSV export (#38, PR #56)** — `BudgetCsvExport.kt`: pure `buildBudgetCsv()` (RFC-4180 quoting, unit-tested in `BudgetCsvExportTest`) + a `FileProvider` share-sheet handoff. Writes to `cacheDir`; the provider paths are declared in `res/xml/file_paths.xml`.
- **Budget spending-trend chart (#39, PR #57)** — `BudgetTrends.kt`: pure `monthlyTotals(items, monthsBack, today)` aggregation (unit-tested in `BudgetTrendsTest`) plus the `BudgetTrendsCard` composable that draws it with a bare `Canvas` — no chart library, consistent with `ApexLogo`'s hand-drawn approach. Tapping a bar selects that month.
- **Notes search + pin-to-top (#40, PR #58)** — `isPinned` column on `notes` (hence DB v12 + `MIGRATION_11_12`). Pinning sorts in the DAO query (`ORDER BY isPinned DESC, modifiedAt DESC`); search filters in `NoteViewModel` (`filteredNotes` = `activeNotes` combined with `_searchQuery`, matching title or content case-insensitively), not in SQL.
- **Reminder notification actions (#41, PR #59)** — see "Background Work" above (`ReminderActionReceiver`, `ReminderCompleteWorker`, shared `ReminderCompletion.kt`).
- **Per-category spending caps (#75)** — nullable `monthlyLimit` column on `categories` (hence DB v13 + `MIGRATION_12_13`). `CategoryLimits.kt` holds the pure logic (`categoryLimitStatuses`, `effectiveMonthlyLimit`, `parseMonthlyLimitInput`, unit-tested in `CategoryLimitsTest`); `BudgetLimitsCard.kt` renders it on the Budget screen. **null = uncapped, and non-positive/non-finite caps normalize to null** — there's no renderable progress for a 0 cap, so `effectiveMonthlyLimit()` is the single gate every read goes through. Caps sync via the existing category path; `pushCategory` always writes the `monthlyLimit` key so a cleared cap isn't left behind by `SetOptions.merge()`.

## 2026-07-17 Study subject categorization (Issue #78)

- **Per-subject study sessions** — `StudySession` gained a `subject: String` field and its primary key changed from `(date)` to `(date, subject)`; `""` is the "No subject" / uncategorized bucket and the startup default, so a user who never picks a subject behaves exactly as before. DB v13 → **v14** + `MIGRATION_13_14` (see Database above); every pre-v14 daily aggregate migrates into that date's `""` row.
- **Timer** — `StudyViewModel` holds `currentSubject: StateFlow<String>`; the displayed stopwatch shows "this subject's total today", not the daily grand total. `selectSubject()` pauses-banks-under-old-subject then resumes under the new one, so switching mid-study never misattributes seconds. `resetTimerManual()` now clears only the current subject's today total. `StudyTimerStateStore`/`PersistedTimerState` persist the subject so a process death mid-session resumes under the right subject.
- **Pure logic** in `StudySubjectStats.kt` (unit-tested in `StudySubjectStatsTest`): `normalizeSubject` (trim + collapse whitespace; blank → `""`), `groupSessionsByDate` (→ `DayStudy`/`SubjectTotal`, biggest-subject-first), `knownSubjects` (quick-picks). History UI (`StudyTrackerView`) groups by date with a per-subject breakdown; a subject-picker dialog (existing subjects as chips + free-text add) sits under the timer.
- **Sync** — `parseStudySessionDoc` reads `subject` (absent → `""`, so legacy docs are backward-compatible); `pushStudySession` writes it and keys the Firestore doc via `studySessionDocId(date, subject)` — the `""` bucket keeps the bare-date id (so old aggregate docs stay in place), named subjects append `|subject` (with `/`→`_`). Still insert-only / local-timer-is-source-of-truth, same as before. Unit-tested in `FirebaseDocParsingTest`.
- **Verified on-device (2026-07-20, SM-S931U1)** in addition to `assembleDebug`/`testDebugUnitTest`/`lintDebug`: the `MIGRATION_13_14` PK-change dance was exercised on a **real populated upgrade** (a seeded v12 DB with budget/notes rows plus a `study_sessions` row upgraded straight to v14) — the existing daily total was preserved into the `subject = ''` bucket with its duration intact, other tables survived, no crash. The subject picker, adding a new subject, and per-subject total switching were all confirmed working.

## 2026-07-20 Biometric lock (Issue #45)

- **Opt-in convenience lock for Budget and Notes** — gates the module UI behind a fresh device unlock. **Not the encryption**: this shields the in-app screens; the database file itself is encrypted separately (Issue #117, see "Database encryption" above). `SecuritySettings.kt` holds everything: DataStore flags (`budget_lock_enabled`/`notes_lock_enabled`), the pure `biometricAvailabilityFrom()` mapper (unit-tested in `SecuritySettingsTest`), the process-scoped `UnlockSession` holder, `promptUnlock()` (the `BiometricPrompt` glue, authenticators = `BIOMETRIC_WEAK or DEVICE_CREDENTIAL` so PIN/pattern is the fallback and no-biometric devices still work), and the `LockGate`/`LockOverlay`/`ModuleLockSetting` composables.
- **`MainActivity` is now a `FragmentActivity`, not `ComponentActivity`** — `androidx.biometric`'s `BiometricPrompt` requires one. `FragmentActivity` *is* a `ComponentActivity`, so `setContent`/`viewModel()`/`registerForActivityResult` are unaffected. If you add another Activity that shows a biometric prompt, it must also be a `FragmentActivity`.
- **Session policy**: `MainActivity.onStop()` calls `UnlockSession.lockAll()`, so backgrounding re-locks; within one foreground session an unlocked module isn't re-prompted. The gate lives in `AppNavigation` (wraps the `budget_tracker` and `notes` composables); cancel/error pops back to the menu, and it **fails closed** while the DataStore flag is still `null` (loading) so a locked module can't flash its data.
- Toggles live in the Budget and Notes settings sheets; greyed out (with an explanation) when the device has no screen lock, and enabling requires one successful auth first. **Verified on-device (2026-07-20)**: the toggle renders, availability detection is correct, and tapping it fires the real system prompt hosted by the FragmentActivity (no crash). Completing an auth needs real biometric/PIN input (not adb-automatable), so the enable→gate→re-lock cycle end-to-end is the one path checked by wiring/inference rather than a full automated tap-through.

## 2026-07-21 → 2026-07-22 Dashboard (goal-tracking heatmap)

New home surface: a GitHub-contribution-style heatmap scoring each day by the fraction of the user's active **goals** completed. Built and verified phase-by-phase on-device (SM-S931U1), merged as PRs #100–#103 with sync (this section) the final piece. Goals are distinct from reminders — reminders never feed this graph.

- **Model** — `Goal` is `MANUAL` (a `GoalCompletion` check-off per day) or `AUTO` (a `metric` ∈ {`SCREEN_TIME`,`STUDY`,`SPEND`} + `comparator` ∈ {`UNDER`,`OVER`} + hour/currency `threshold`, optional study `subject`). AUTO goals store nothing per day — they're evaluated on read from the existing `ScreenTimeSession`/`StudySession`/`BudgetItem` tables. Each goal has a `startDate` (counts from) and nullable `archivedDate` (stops counting on/after), so editing goals never rewrites past days. `GoalCompletion` is keyed `(goalCloudId, date)` so its Firestore doc id is device-stable.
- **Pure scoring** — `DashboardScoring.kt` (`activeGoalsOn`, `evaluateAutoGoal`, `dayFraction`, `intensityBucket`, `perfectDayStreak`, `goalStreak`), unit-tested in `DashboardScoringTest`. `OVER` is met at-or-above the threshold, `UNDER` at-or-below; a day with no active goals is a `null` fraction (empty cell, ≠ a 0.0 missed day); only a perfect day hits intensity 4.
- **UI** — `DashboardViewModel` `combine`s goals/completions/study/screen/budget/**papers** flows (mirrors `OverviewViewModel`). `DashboardView` draws the heatmap by hand (vertical weeks-as-rows, **newest week on top**, today outlined; cells are **fully-filled squares whose colour intensity carries the day's fraction of goals met** — the classic GitHub contribution-graph read, restored 2026-08-24 after the 2026-07-30 Graphite pass's fill-height-bar experiment, see "2026-08-24 Heatmap reverted to colour squares" below) with a Today checklist; tapping any cell opens a day-detail `ModalBottomSheet` that backfills MANUAL goals for that date. `GoalsView` (route `goals`) manages goals (add/edit/archive/unarchive/delete) via a shared `GoalEditDialog`.
- **Nav** — Phase 4 made the Dashboard the home behind the bottom nav bar and retired `MainMenu` (see Navigation above).
- **Sync** — see Cloud Sync above. **On-device round-trip is signed-in-only and was not automatable** (Google sign-in needs real user interaction, which the safety rules bar the agent from completing); build/unit-tests/lint pass and the signed-out path is a verified no-op. A signed-in two-session round-trip (create a goal → confirm `users/{uid}/goals` in the Firestore console → reinstall+sign-in pulls it back) is the one check owed before fully closing this out. Note the `firestore-api-disabled` memory: if goal pushes log `PERMISSION_DENIED`, Firestore isn't enabled/deployed for the project.
- **Known behavior**: the perfect-day streak reads "No streak yet" each morning until today's goals are ticked (streak counts today inclusive) — deliberate, trivially changed. A primary bottom-bar destination (e.g. Budget) still shows its own top-bar back arrow alongside the bar — minor redundancy, left to avoid touching each module's Scaffold.

## 2026-07-08 Follow-up (PR #2)

- Set up a real Firebase project connection: registered the debug SHA-1 fingerprint in the Firebase console, downloaded the real `google-services.json` (replacing the 2026-07-07 placeholder — still gitignored, never committed).
- Fixed Google Sign-In actually completing on real devices: `AuthViewModel.handleSignIn()` only matched `credential is GoogleIdTokenCredential`, but Credential Manager's `GetGoogleIdOption` returns the token wrapped in a `CustomCredential` (type `TYPE_GOOGLE_ID_TOKEN_CREDENTIAL`) that must be unwrapped via `GoogleIdTokenCredential.createFrom(credential.data)` — Google's documented pattern. The old check never matched, so sign-in silently no-op'd: Credential Manager returned a response, `auth.signInWithCredential()` was never called, no error shown, no Firebase auth state persisted. Confirmed via live device testing (adb logs + inspecting the app's private storage for auth persistence files) before and after the fix. Verified working end-to-end on a physical Samsung device post-fix.
- Filed the remaining Known Issues above as GitHub issues [#3](https://github.com/aadityad12/Trackers/issues/3)–[#10](https://github.com/aadityad12/Trackers/issues/10), numbered in recommended fix order.

## 2026-07-23 Bug / accessibility / docs / feature pass (issues #97, #105–#128)

Each fix is one commit on `fix/issues-2026-07-23`, built + unit-tested and driven on the
**Android emulator** (`Medium_Phone` AVD, adb — CLAUDE.md's older "no device available" notes are
obsolete; see the `android-emulator-available` memory).

- **`NavRoutes.kt` is the intent-extra gate (#105)** — `MainActivity` is exported, so the
  `navigate_to` extra is untrusted input; `sanitizeRequestedRoute()` drops anything outside
  `APP_ROUTES` before it reaches `navController.navigate()`, which used to crash the app on cold
  start (`am start … --es navigate_to garbage`). **Add every new NavHost route to `APP_ROUTES`.**
- **Accessibility (#106, #107)** — heatmap cells, theme swatches, and category-colour swatches were
  text-free clickable `Box`es invisible to TalkBack. They now carry `semantics { contentDescription }`
  (+ `selectable(selected = …)` for the two pickers). Colour names come from the pure
  `swatchHueOf()` classifier in `ColorUtils.kt`. Verify a11y work with
  `adb shell uiautomator dump` and grep for `content-desc`.
- **Goal-completion race (#111)** — `DashboardViewModel` now holds a `togglesInFlight` set keyed on
  `(goalCloudId, date)`, the same guard `ReminderViewModel` uses (#25).
- **Localization pass (#112, #114, #119, #120)** — recurrence enums render via
  `frequencyLabelRes()`/`endTypeLabelRes()` in `Recurrence.kt`; the pie legend uses
  `budget_uncategorized`/`budget_pending_legend`; the heatmap weekday header derives from
  `DayOfWeek.getDisplayName(NARROW, locale)`. **Subscription budget items no longer persist the
  `"[Subscription] "` prefix** — `BudgetViewModel` stores the bare name and the label is composed at
  render time; `BudgetItemTitle.kt` holds the `-1L` category sentinel plus `budgetItemBaseTitle()`,
  which strips the prefix from rows written by older builds (and from Firestore copies).
- **Theme tokens (#113)** and the Today-card `loaded` gate (#118) are one-liners in the same spirit.
- **Chart axis labels (#97)** — `durationAxisLabels()` in `DurationFormat.kt` picks one unit (h m /
  m / s) from the max, so a sub-minute week no longer renders three "0m"s.
- **Bonus crash fix** — revoking Usage Access on a running app made `queryEvents` throw
  `SecurityException` and killed the process on the next 30s poll; `calculateAppSpecificUsage()`
  now catches it and reports no usage.

Features added in the same pass:

- **Search on Reminders and Budget (#123)** — the Notes pattern (#40) extended: a `_searchQuery`
  StateFlow per ViewModel plus a top-bar field. Matching lives in pure `ListSearch.kt`
  (`filterReminders`, `filterBudgetItems` — the latter also matches the item's *category name*).
  Only the Budget **transactions list** narrows; the totals/pie/limits/trend keep describing the
  whole month.
- **Manual past study sessions (#122)** — `StudyViewModel.logManualSession(date, subject, seconds)`
  reuses the timer's own `saveSessionForDate`, so the `(date, subject)` PK, cloud push, and
  "0 clears the row" semantics come for free. **Today is deliberately not editable** — the running
  timer owns today's row. UI: `ManualSessionDialog`, opened from + in the history header or by
  tapping a past subject row.
- **Overall monthly budget (#125)** — `BudgetPrefs` (new DataStore, **local-only**, not synced)
  holds a single ceiling; pure `overallLimitStatus()` sits beside `categoryLimitStatuses()` and the
  card (now titled SPENDING LIMITS) renders an "All spending" row above the category rows via a
  shared `LimitRow`.
- **Heatmap year windowing (#128)** — the Dashboard no longer scrolls. `DashboardScoring.kt` gained
  pure `heatmapRange`/`heatmapYears`/`heatmapWeeks`, the grid moved out of the ViewModel
  (`DashboardUiState.dayCell(date)` computes one cell on demand), and `DashboardView` is a Column
  whose heatmap takes the remaining height and sizes cells with `BoxWithConstraints`. **Keep month
  labels on a fixed-height row** — letting them set the row height inflates twelve rows and pushes
  the year off screen.

## 2026-07-30 Papers feature (Plan.md Phase 1, branch `feature/papers`)

A reading log for academic papers — the app owns the knowledge layer (queue, one rotating
daily pick, structured memos: status/`what I learned`/1–5 signal) and opens the PDF externally
(`ACTION_VIEW`); it is deliberately **not** a PDF reader. **`Plan.md` at the repo root holds
the thirteen settled decisions** (identity, this feature, README) — read it before touching
any of the three workstreams. Key pieces: `Paper`/`PaperDao` (DB v20, `MIGRATION_19_20`),
`SemanticScholar.kt` (S2 Graph API client; pure `normalizePaperIdInput`/`parseS2PaperJson`,
unit-tested in `SemanticScholarTest` — note the unauthenticated S2 pool rate-limits hard),
`PapersLogic.kt` (pure queue/daily-pick/read-counts, `PapersLogicTest`), `PaperSeeds.kt`
(offline starter list), route `papers` (More sheet). Reading feeds the heatmap via
`GoalMetric.PAPERS` (`DayMetrics.papersRead`); papers ride the full-dataset backup
(`BackupData.papers`). **Firestore sync shipped in Issue #151**: papers use the standard
`users/{uid}/papers/{cloudId}` last-writer-wins path, participate in initial sync and live
snapshot listeners, and every Papers mutation mirrors to the cloud while Room remains the source
of truth. **Daily discovery shipped in Issue #149**: `PapersDiscoverySettings.kt` stores selected
Semantic Scholar fields, the last attempt date, and shared 429 backoff in DataStore. Opening
Reading rotates through one field per day, makes at most one `/paper/search` request, deduplicates
by `s2Id`, and inserts at most three `PaperSource.DAILY` rows. **Recommendations shipped in Issue
#150**: `PapersRecommendations.kt` turns the reading log into positive (READ, signal 4–5) and
negative (signal 1–2, or ABANDONED) examples for S2's batch `/recommendations/v1/papers` endpoint,
and the results land as `PaperSource.RECOMMENDED` WANT rows — ordinary queue rows, so `paperQueue`
/`dailyPick` are untouched; the "Because you read …" shelf is a *view* over them, which is why the
queue section renders `queueRest`. Papers with no `s2Id` (every bundled seed) can't be examples and
are skipped rather than resolved, to spare the rate-limited unauthenticated pool. Its day gate is
`lastRecommendationDate` — separate from topic search — but the **429 backoff window is shared**,
because both draw on that same pool.

## 2026-07-30 Graphite identity (Plan.md Phase 2, branch `redesign/graphite`)

The Ember identity (2026-07-29) was **replaced** by GRAPHITE — a cold monochrome, no accent hue,
with a mono display voice. Owner's driver: Ember's warm-dark + terracotta + Instrument Serif read
as Claude's surfaces. **`Design.md` §0 is the authoritative value set**; the rest of that file
still describes Ember and is marked superseded (full table rewrite deferred). The enforcing skill
(`.claude/skills/android-product-design/SKILL.md`) is fully rewritten to graphite.

- **Type** (`ui/design/ApexType.kt`) — Instrument Serif + Geist Mono **retired**; **Martian Mono**
  (bundled `res/font/`, OFL in `assets/licenses/`) is now both the display/headline voice and
  every `ApexNumerals` figure. Geist stays for body/labels. Martian is *wide*: display sizes are
  smaller + negative-tracked vs. the old serif scale.
- **Colour** (`ui/design/ApexPalette.kt`) — cold graphite, both themes hand-authored. **No accent**:
  `primary` is ink (Frost `#E9EBEE` dark / Char `#191C20` light), so filled buttons/FAB/nav are
  inverse blocks. Only hues are `Sage` (positive) and `Crimson` (`error`). Values + measured
  contrasts in Design.md §0.
- **Heatmap reshaped** (`DashboardView.HeatCell`) — **fill-height bars, not colour squares**: bar
  height = fraction of the day's goals met, bottom-anchored in a fixed slot; perfect = solid ink
  fill. Pitched at the time as the signature that stops the grid reading as GitHub's.
  **Superseded 2026-08-24** — the owner wanted the GitHub read back; see "2026-08-24 Heatmap
  reverted to colour squares" below. `ApexSemantics`' `heatInk`/`heatSlot` (added for this) are
  gone again as of that revert.
- **`ApexTheme.EMBER` → `GRAPHITE`**. Glance widgets carry a hand-kept mirror of the graphite dark
  hexes (a met goal keeps Sage). Screenshot baselines (`app/src/screenshotTestDebug/reference/`)
  re-recorded. Verified on the SM-S931U1 in both themes + 200% font; no screen's hierarchy needed
  per-screen rescue (ink primary + mono headlines carry it). The debug `StylePlateActivity` renders
  the graphite system on-device.
- **Follow-up (same day): the M3 undefined-slot bug ported forward.** The Ember palette had a
  known instance of this (`tertiaryContainer`, dark-only — see the `apextracker-architecture`
  memory / the pre-graphite Design.md §8 note), but a separate, uncommitted 2026-07-30 audit found
  it was far bigger: `surfaceContainer`/`surfaceContainerHigh`/`surfaceContainerLow`, `scrim`,
  `surfaceTint`, and the whole `inverseSurface`/`inverseOnSurface`/`inversePrimary` snackbar trio
  were *also* undefined, so dropdown menus, all 19 `AlertDialog`s, `DatePickerDialog`, one
  `ModalBottomSheet`, and every snackbar were silently rendering Material's baseline purple. The
  graphite rewrite above shipped without this fix (it only carried `tertiaryContainer`) and
  reopened the same hole under new names; it was ported onto graphite immediately after, in the
  same session, once the gap was found. See §8 in Design.md and `ApexPaletteSlotsTest`.

## 2026-08-01 Study timer flip clock (branch `feature/study-flip-clock`, commit `c3f6c93`)

Replaced the study timer's plain-text HH:MM:SS/MM:SS stopwatch readout with a split-flap ("flip
clock") digit display, and added a focus mode.

- **`ui/design/ApexFlipClock.kt`** — the `ApexFlipClock` composable renders elapsed seconds as
  flipping digit groups (`ApexNumerals.hero`, so it's still Martian Mono/tabular). Digit-group
  layout is pure logic in **`FlipClockDigits.kt`** (unit-tested in `FlipClockDigitsTest`) — the
  same field-layout job `StudyTrackerView.formatTime()` used to do as a string, now done as
  structured digit groups instead. `formatTime()` had exactly one caller and was **deleted**
  rather than left behind (`grep -n "formatTime() lived here" StudyTrackerView.kt` finds the note).
- **Focus mode** — starting the timer collapses `StudyTrackerView` into a full-screen focus
  surface (`StudyFocusContent`): app bars/bottom nav hidden, screen kept awake
  (`FLAG_KEEP_SCREEN_ON` via `FocusWindowEffects`). Focus mode has no separate on/off flag — it's
  defined as exactly `isRunning`, so the two states can't drift apart; the window flag is applied
  as a `DisposableEffect` so it's released the instant focus mode ends, not just when the screen
  is destroyed.
- **Screenshot baselines** — `FlipClockScreenshots.kt` added 3 new reference PNGs (dark, light,
  dark @200% font) to `app/src/screenshotTestDebug/reference/`.
- **Ambient display (Issue #171)** — focus mode's **Dim display** control lowers the Activity
  window brightness, switches the whole surface and flip clock to a fixed dark graphite palette,
  and disables flap animation to reduce light and motion. Pausing restores the exact prior window
  brightness and normal theme. This is deliberately an in-app always-on surface; Android does not
  let a normal phone app replace the system/lock-screen AOD.

## 2026-08-05 Weekly goal cadence (Issue #166)

- `Goal.cadence` is `DAILY` (the migration/default for every existing goal) or `WEEKLY`.
  Weekly means an ISO Monday–Sunday period; `goalCompletionDate()` canonicalizes manual check-offs
  to that Monday, so every day in the same week reads/writes one stable completion row/document.
- Automatic weekly goals aggregate the active portion of that period via
  `metricsForGoalPeriod()` before reusing `evaluateAutoGoal()`. A mid-week start or archive clamps
  the aggregation window so inactive days never contribute.
- DB v21 / `MIGRATION_20_21` adds the non-null cadence column with `DEFAULT 'DAILY'`. Firestore and
  backup parsing use the same legacy default. The v20→v21 migration and manual weekly workflow
  were verified on the connected physical device without losing its existing data.

## 2026-08-05 Daily Apex Tip (Issue #168)

- `ApexTipViewModel` exposes an opt-in card on Dashboard. Before enabling, the card states exactly
  which anonymous daily aggregates leave the device; names, notes, transaction details and app
  names are never put in the prompt.
- `ApexTipSettings` stores consent, the last attempt date and one locally cached response. Automatic
  generation is capped at one attempt per local day; a failed request requires an explicit Retry.
  The DataStore file is excluded from Android backup because the response is personal and dated.
- `FirebaseApexTipGenerator` uses Firebase AI Logic with stable `gemini-3.5-flash-lite`; debug builds
  use App Check's debug provider and release builds use Play Integrity. The Firebase project must
  have AI Logic enabled and the local debug token allowlisted before a debug request can succeed.
- Prompt construction, output bounding and daily-request decisions are pure logic covered by
  `ApexTipTest`. Firebase remains behind the small generator boundary; Room is not involved.

## 2026-08-07 Papers discovery redesign

Replaced the old bare-field discovery (pick from 8 umbrella fields, one rotates per day, the S2
query was literally the field name as text) with keyword-scoped topics and an engagement-weighted
recommender. Scoped via a 14-question `/grill-me` interview before implementation; DB bumped to
**v22** (`MIGRATION_21_22`).

- **`PaperTopic`** (new entity, `paper_topics` table) — a user-added `(field, keyword)` pair, e.g.
  Computer Science + "diffusion models"; `keyword` is the real S2 search query now, `field` stays
  only as the `fieldsOfStudy` filter. Max **8** topics (`MAX_PAPER_TOPICS`). Each topic accumulates
  `readCount`/`abandonedCount`/`ratingSum`/`ratingCount`/`consecutiveAbandons` and a
  `lastCheckedDate`; `pausedAt` (non-null = muted) excludes it from rotation while keeping history.
  `Paper` gained `topicCloudId` (the topic's *cloudId*, not local Room id — same
  cross-device-reference reasoning as `resolveReminderParentLinks`; "" = MANUAL/SEED/deleted-topic,
  falls back to neutral weight, no cleanup needed).
- **Pure logic in `PapersDiscoveryScoring.kt`** (unit-tested in `PapersDiscoveryScoringTest`):
  `topicEngagementScore` (0.5 neutral with no outcomes yet, else a blend of read-ratio and average
  rating), `guaranteedSlotTopic` (oldest/never-checked topic — the "every topic gets covered"
  guarantee), `bonusSlotTopics` (weighted-random by engagement, floor-weighted so a
  consistently-abandoned topic is never fully unreachable — "healthy rotation" without starving
  anyone), `dailyTopicFetchPlan` (1 guaranteed + up to 2 bonus slots/day), `shouldPromptMute` (3
  consecutive abandons). `PapersLogic.dailyPick` changed signature to `(queue, topics)` — no longer
  epoch-day-indexed; it's just "the WANT paper whose topic has the best track record," so
  **same-day chaining is free**: marking the current pick READ/ABANDONED removes it from the WANT
  queue, Room re-emits, and the next-best paper is promoted automatically without waiting for
  tomorrow.
- **Fetch volume**: up to 3 topic-slots/day, hard-capped at **5 new papers/day total** across
  whichever slots run (`PapersViewModel.PER_SLOT_LIMIT`/`DAILY_TOTAL_CAP`). A slot's
  `lastCheckedDate` only advances if its request actually ran — hitting the daily cap or a network
  failure skips the slot without falsely marking it checked, so it stays "most overdue" and gets
  priority next time.
- **Reactivity gotcha, caught by on-device testing**: the daily-fetch trigger (`PapersViewModel`
  init) must recompute on **both** `discoveryPrefs.preferences` *and* `discoveryTopics` changes —
  adding your first topic doesn't touch the DataStore prefs, so a collector keyed only on
  `preferences` (as the old bare-field version was, since fields lived in that same DataStore)
  silently never fires until the next cold start. Fixed by `combine(discoveryPrefs.preferences,
  discoveryTopics) { preferences, _ -> preferences }.collect { ... }` — verified end-to-end on the
  emulator (add topic → immediate fetch attempt → real Semantic Scholar 429 handled cleanly, topic
  correctly left unchecked for retry).
- **Onboarding** — first opening Reading with zero topics shows a front-and-center "What do you
  want to read about?" prompt (`PapersView.OnboardingPrompt`) ahead of the old "Import starter
  list" empty-state action, with a `discoveryOnboardingDismissed`-style DataStore flag so it
  doesn't nag once dismissed. Existing installs are **reset, not migrated** — the old
  `PapersDiscoveryPrefs.fields` key is simply no longer read, and `paper_topics` starts empty for
  everyone, which was an explicit decision in the grill-me interview.
- **Mute nudge** — an inline dismissible row (not a snackbar) surfaces after 3 straight abandons
  from one topic, offering Mute (pauses, doesn't delete) or Keep it.
- **Sync** — `PaperTopic` follows the standard 5-part Firestore shape at
  `users/{uid}/paper_topics/{cloudId}`, registered in `performInitialSync` and `SyncCoordinator`
  alongside `papers`. Included in the full-dataset backup (`BackupData.paperTopics`).
- **Verified on-device (2026-08-07, Medium_Phone emulator)**: onboarding prompt, topic
  add/list/mute UI (including the `apexMenuBorder()` field dropdown), the reactive fetch-trigger
  fix, real Semantic Scholar network integration (hit the documented unauthenticated rate limit
  cleanly, no crash), seed import, mark-read with same-day chaining to the next queue item — all
  confirmed working, no crashes in logcat throughout. Schema diffed against the exported
  `app/schemas/…/22.json`.

## Developer's own TODO list (from notes.txt, still current)
- Budget: "Extract from receipt" — shipped as the ML Kit scan in `BudgetItemDialog` (#46, see below).
- Study Timer: "Always on display" support — shipped as the dimmed in-app ambient display (#171).
- Ideas floated for later: animated ring-chart visualizations (Canvas-based, like `ApexLogo`). (Home-screen widgets, biometric lock and Daily Apex Tip have all shipped — see "Home-screen widgets" below.)

## 2026-07-29 Redesign foundation (branch `redesign/foundation`, not yet merged)

A UI redesign, planned via `/grill-me` and settled as twelve explicit decisions. **`Design.md` at
the repo root is the specification** — values, measured contrast ratios, chart spec, screen
inventory, and the reasoning. Do not restate its values here.

- **Skills are now in-repo** under `.claude/skills/`: `android-product-design` (a fork of
  Anthropic's `frontend-design`, narrowed to this app's locked identity and platform floor) plus
  four vendored from github.com/android/skills — `adaptive`, `edge-to-edge`, `testing-setup`,
  `perfetto-trace-analysis`. `migrate-to-compose` and `agp-9-upgrade` were deliberately **not**
  taken (0 XML layouts; already on AGP 9.2.1). The `android` CLI is a separate download and is not
  installed on this machine; the skills were copied from a clone.
- **Identity**: Instrument Serif (display) + Geist (UI) + Geist Mono (all numerals), one accent
  (Ember), hand-authored dark **and** light, no Dynamic Color. Stable material3 1.4.0 — **M3
  Expressive was rejected**, its motion/shape APIs are 1.5.0-alpha only.
- **Geist is bundled as static weights, not its variable file**, because every
  `FontVariation.Settings` overload is `@ExperimentalTextApi`. Costs ~340KB more, needs no opt-in.
- **Screenshot testing works here** (`com.android.compose.screenshot`, 8 baselines in
  `app/src/screenshotTestDebug/reference/`). It renders through **Layoutlib, not Robolectric**, so
  the serialization clash that blocks Room's `MigrationTestHelper` does not apply. Gotchas: the
  enabling flag is needed in **both** `gradle.properties` and the module's `experimentalProperties`,
  and `@PreviewTest` needs an explicit `screenshot-validation-api` dependency.
- **A debug-only style plate** (`app/src/debug/.../StylePlateActivity.kt`) renders the whole design
  system on a real device. Launch:
  `adb shell am start -n com.example.apextracker/com.example.apextracker.design.StylePlateActivity`.
  It exists because five values were correct on paper and wrong on an AMOLED panel — invisible
  hairlines, a heatmap ramp that compressed to nothing, and an Ember/Alarm pair that rendered as
  one colour in light mode. **Judge design changes there before touching a screen.**
- **All eight screens have landed** (PRs #140–#146, merged 2026-07-29/30). `Design.md` §10 tracks
  per-screen state; don't restate it here.
- **Category colours are mapped on read, never migrated** (`CategoryPalette.kt`). The picker offers
  the eight validated hues from `Design.md` §6; every legacy `Category.colorHex` still sits in Room
  and Firestore untouched, and `resolveCategoryHex()` maps it onto a palette slot at render time.
  **Every surface that paints a category colour must go through `categoryColorOf()`/
  `resolveCategoryHex()`** — a raw `parseColorSafe(category.colorHex)` reintroduces the two-palette
  split. The mapping is many-to-one, so colour is never the only channel: always show the category
  name too. Collapse table and rationale in `Design.md` §8.
- **Every `ColorScheme` slot the app touches must exist in both schemes.** An undefined slot falls
  back to Material's *default* scheme, not to something neutral — that's how the Overview's
  screen-time card rendered in Material Purple.
- **`outline` is for borders, dividers and strokes — never for text or a meaningful icon.** Raising it
  for hairline visibility silently dropped 51 text call sites across eleven files to ~2.5:1. Text uses
  `onSurfaceVariant`.
- **Still untouched**: the settings sheets, `CalendarGrid`, and the various dialogs/editors — left out
  of the per-screen PRs deliberately to keep diffs reviewable.

## 2026-08-07 Security & correctness pass (issues #186–#198)

A full-codebase review filed thirteen issues and fixed all thirteen, one commit each, on branch
`claude/codebase-security-review-934d59`. Verified by `assembleDebug` / `testDebugUnitTest` (399
tests) / `lintDebug` (0 errors) / `assembleRelease`. **No device or emulator run** — the sync and
migration changes below are the ones that most deserve an on-device check before release.

- **Account switching wipes local data (#186)** — the highest-impact finding. `performInitialSync`
  pushes *every* local row to the signed-in uid (that's how offline edits reach the cloud, Issue
  #4), but nothing recorded *whose* rows they were, so signing in as B uploaded A's notes and
  budget into `users/{B-uid}/...`. `AccountIdentity` persists the owning uid,
  `shouldResetLocalDataForUid` is the pure decision (**null previous uid = first sign-in, keep the
  data**), and `clearLocalUserData` wipes Room + note attachment files + the personal DataStores
  before the sync runs. `FirebaseManager.firestore` is now a per-access getter so terminating the
  client to clear its cache can't strand live managers on a dead instance.
- **The biometric lock actually locks (#187)** — it gated two routes; the same data was readable
  from the backup export (three taps, plaintext file), the Budget widget, and Overview.
  `rememberUnlockedAction` is the new shape for a one-shot sensitive action on an unlocked surface
  (`LockGate` can only protect a whole destination). The widget withholds figures in
  `budgetWidgetSnapshot`, not the layout — the snapshot crosses into the launcher's process.
  **The Dashboard was deliberately left alone**: `GoalStatus` carries only the goal and a
  satisfied boolean, so a SPEND goal leaks no amount.
- **`rescheduleAllReminders` is the one alarm sweep (#188, #195)** — reminders written by sync or a
  backup restore never got an alarm and silently never fired. It is idempotent, so a blanket
  re-arm is safe; **that is why it's a sweep rather than a call threaded through each write site**
  — the next thing that inserts a `Reminder` gets it for free. `ReminderBootReceiver` now goes
  through it too (it had been recomputing trigger times with `computeTriggerTime`, reintroducing
  Issue #80 on every reboot) and listens for `MY_PACKAGE_REPLACED`.
- **Restore replaces, in the cloud too (#189)** — `restoreBackupAndReconcile` stops the listeners,
  restores, then `replaceCloudWithLocal` prunes and re-pushes. Previously the listeners re-inserted
  everything the restore cleared. Screen time is exempt (per-device measurement, not this device's
  to delete).
- **Untrusted-data boundaries (#190–#194)** — `sanitizeWebUrl` at both paper-parse boundaries
  (`file:` URIs were an uncaught `FileUriExposedException` crash); `csvEscape` neutralizes
  spreadsheet formula triggers (**amounts deliberately bypass it** so a refund stays a number);
  `noteAttachmentFile` is nullable and containment-checked, with sanitizing in `parseBackupJson`;
  `parseReminderDoc` uses `parseRecurrenceSafe` like the Room converter always did; and
  `note_attachments` + the personal DataStores are excluded from Android backup.
- **Derived rows get derived identities (#196)** — subscription-generated `BudgetItem`s used a
  random UUID, so every signed-in device minted its own row for the same charge and totals doubled.
  `subscriptionItemCloudId(subscriptionCloudId, renewal)` keys on the **month**, since the day
  drifts. `renewalDate` is now treated as monotonic in `applySubscriptionDoc` — it's a cursor, and
  last-writer-wins would rewind it.
- **R8 is on in release (#198)** — and this is the one to be careful with: `backupGson()`
  serializes by *field name*, so `app/proguard-rules.pro` keeping those members is load-bearing.
  A minified build without it writes an unreadable backup format. Read that file before touching
  `isMinifyEnabled`. Verified by parsing the release DEX with `dexdump` — note that checking this
  with `strings` gives false negatives, because DEX pool entries run together in its output.
  `androidx.biometric` stays at 1.1.0: every later version on Google Maven is an alpha.

## 2026-08-24 Heatmap reverted to colour squares

The 2026-07-30 Graphite pass drew the Dashboard heatmap cells as **fill-height bars** (bar height
= fraction of that day's goals met), explicitly to stop the grid reading as GitHub's contribution
graph. On seeing it running, the owner asked for the opposite: a fully-filled square per day, with
colour intensity — not geometry — carrying the fraction, i.e. the GitHub read the bar-height
version was built to avoid.

- **`DashboardView.HeatCell`** now fills the whole cell with one of six ramp shades
  (`intensityBucket(fraction) + 1`, or index 0 for an untracked/no-active-goals day) instead of
  drawing a bottom-anchored bar inside a slot colour. `HeatmapLegend` follows the same ramp. The
  `today` outline is unchanged.
- **`ApexSemantics.heatInk`/`heatSlot`**, added by the 2026-07-30 pass specifically for the bar
  rendering, are deleted — nothing else referenced them. `heatRamp` (already used by the Glance
  widgets and the style plate) is now the *only* heatmap colour source, in-app and in the widgets,
  which incidentally closes a small drift: the two had already diverged in how "intensity" was
  encoded and now can't.
- **No DB/schema change** — this is a pure rendering change over the same `dayFraction`/
  `intensityBucket` pipeline from `DashboardScoring.kt`; `DayCell`/`GoalStatus` etc. are untouched.
- Screenshot baselines under `app/src/screenshotTestDebug/reference/` covering the Dashboard were
  re-recorded against the new cell rendering.
- Verified on the `Medium_Phone` emulator: untracked days render the dim baseline shade, marking
  a goal complete immediately brightens today's cell to the perfect-day shade, and the `today`
  outline still tracks the current cell correctly.

## 2026-08-25 Backup excludes the SQLCipher key wrapper (Issue #206)

The Issue #62 backup exclusions (`data_extraction_rules.xml`/`backup_rules.xml`) excluded
`budget_database` but not `database_key` — the SharedPreferences file `DatabaseEncryption.kt`
wraps the SQLCipher passphrase in (Issue #117). Restoring that file onto a device whose Android
Keystore key is necessarily different left `loadOrCreatePassphrase()` holding a wrapped passphrase
it could never decrypt, on every single launch, forever — and since `budget_database` itself was
correctly excluded and so never existed on the restored device, the app fell back to creating a
**brand-new unencrypted** database with no error surfaced. Issue #117's guarantee was silently and
permanently defeated on any backup restore or device transfer.

- **Fix**: `database_key.xml` (`sharedpref` domain) is now excluded alongside `device_identity.xml`
  in both files. A restored device now mints its own fresh passphrase and its own fresh encrypted
  database, exactly like a new install.
- **Defense in depth**: `loadOrCreatePassphrase()`'s stale-wrapper case (decrypt fails against the
  current Keystore key — a wipe, a corrupted key, or this same restored-backup scenario if it ever
  recurs some other way) now clears `PREF_WRAPPED`/`PREF_IV` via the new `clearStalePassphrase()`
  rather than leaving them in place. Before this, a lost Keystore key wedged the app on the
  unencrypted fallback permanently, because every subsequent launch re-read the same undecryptable
  wrapper and failed the same way. Clearing it lets the *next* launch generate a fresh passphrase;
  `databaseOpenHelperFactory` then finds a plaintext database (the one just created on the fallback
  path) and encrypts it, self-healing back to encrypted storage instead of staying stuck. This is
  only safe because it's gated behind the existing `passphrase == null` branch, which already
  quarantines a real still-present encrypted file before falling through — it never overwrites a
  wrapper this device could still legitimately use.
- Not unit-testable beyond what `DatabaseEncryptionTest` already covers (`looksLikePlaintextSqlite`
  is the only pure function in this file); the rest needs `Context`/`KeyStore` and a real backup/
  restore cycle to exercise end-to-end, which is why this shipped as a careful read of the existing
  code paths rather than a new automated test.

## 2026-08-27 Play Store deployment pass

Working the full open-issue backlog (16 issues as of 2026-08-27, `gh issue list --state open`)
one at a time toward a store-ready release: pick the highest-priority open issue, implement,
test (`assembleDebug`/`testDebugUnitTest`/`lintDebug`, plus on-device where the change needs it),
update docs, then commit and push straight to `main` (no PR) before moving to the next issue —
this file's own "one commit per issue, verified" convention from past passes, just pushed
directly rather than through a merged PR. This section is a running log; read `gh issue list` for
the current state of the backlog rather than trusting a snapshot here.

- **Release signing + privacy policy merged** — a previously unmerged branch,
  `play-store/signing-and-privacy-policy` (not from this pass), added a gitignored
  `keystore.properties`-based release signing config to `app/build.gradle.kts`
  (`assembleRelease`/`bundleRelease` stay unsigned until that file exists locally — see
  `docs/release-signing.md`), `scripts/generate_release_keystore.sh` to generate one, and a
  `docs/privacy-policy.md` draft (placeholders for date/contact/developer name, needs a hosted
  public URL before Play Console submission — GitHub Pages or Firebase Hosting both work since a
  Firebase project already exists). Reviewed for correctness/security (no secret ever touches
  version control; the signing block is a true no-op without the properties file, verified by a
  passing `assembleRelease` before and after) and merged into `main` ahead of the issue backlog,
  since these are hard requirements for any Play Store submission and were sitting unmerged
  outside the numbered issues.
- **[Issue #207] Study/Screen Time trend charts now carry per-bar accessibility semantics** — both
  drew their 7-bar weekly charts with a bare `Canvas` and no `.semantics{}` anywhere, so a
  TalkBack user got only the axis min/max, never any single day's value (unlike the sibling
  `BudgetTrendsCard`, which already had this). `ScreenTimeTrendsChart` (`ScreenTimeTrends.kt`)
  already drew one `Box` per bar, so it only needed a `.semantics { contentDescription }` added
  to that existing Box. `StudyWeeklyChart` (`StudyTrackerView.kt`) drew all 7 bars *and* the dashed
  goal-target line in one shared `Canvas`, so it had to be restructured: the goal line stays a
  single full-width background `Canvas` (it isn't tied to any one day), and the bars became a
  `Row` of per-bar `Box`es layered on top, each with its own `.semantics{}` and its own small
  `Canvas` — mirroring `BudgetTrendsCard`'s existing pattern exactly. New string resources
  `study_trend_bar_cd`/`screen_time_trend_bar_cd` (`"%1$s: %2$s"`, weekday + formatted duration,
  same shape as `budget_trends_bar_cd`). No `.clickable` was added (unlike Budget's bars) since
  neither chart has a day-selection callback to attach — clickable was the *ideal*, not
  compulsory, per the issue. Verified on the `Medium_Phone` emulator via `uiautomator dump`: all
  7 bars on both screens now report a `content-desc` (e.g. `"Friday: 0m"`); the visual rendering
  is byte-for-byte unchanged (screenshot-compared) since the restructure only adds a semantics
  layer, it doesn't change what's drawn.
- **[Issue #218] Budget module can now track income, not just expenses** — `BudgetItem` gained a
  `type: String` field (`TransactionType.EXPENSE`/`INCOME`, DB v23→**v24**, `MIGRATION_23_24`,
  additive `ALTER TABLE ... DEFAULT 'EXPENSE'` so every pre-#218 row keeps its exact historical
  meaning). `BudgetItem.isExpense`/`List<BudgetItem>.expensesOnly()` (`BudgetItem.kt`) are the
  **one** filter every "spending" figure in the app now goes through — audited and updated at
  every call site that sums `BudgetItem.amount`: the pie chart and trend chart
  (`BudgetTrackerView.kt`), category/overall limits (`BudgetLimitsCard`), the calendar's day-cell
  and day-breakdown totals (`BudgetCalendar.kt`), the Budget home-screen widget
  (`BudgetWidgetSnapshot.kt`), the Dashboard's SPEND goal metric (`DashboardViewModel.kt` +
  `DashboardSnapshot.kt` — these are two independent copies of the same per-day aggregation, both
  needed the fix), and the Overview spending stat (`OverviewViewModel.kt`). Missing even one of
  these would have let a logged paycheck silently inflate a "spending" figure elsewhere in the
  app, which is why this shipped as a full call-site audit rather than touching just the Budget
  screen. Net balance (income − expense) is the one place the two combine, computed in
  `BudgetOverview` and shown only once a user has ever logged income — a pure expense-tracking
  user sees byte-for-byte the same Budget screen as before. `BudgetItemDialog` gained an
  Expense/Income `SingleChoiceSegmentedButtonRow`; switching to Income hides the category picker
  (a paycheck has no spending category) and clears any category already chosen while on Expense.
  Income rows render in `LocalApexSemantics.current.positive` (Sage) with a leading "+" in the
  transaction list and the calendar's day-breakdown dialog — the same green used for "goal met"
  elsewhere. CSV export gained a `type` column (`date,title,amount,type,category,description`).
  Firestore sync (`pushBudgetItem`/`parseBudgetItemDoc`) and the full-dataset backup
  (`BackupData.kt`, `parseBackupJson`'s per-element normalization, same pattern as the
  `Goal.cadence` one it sits beside) both default an absent/invalid `type` to `EXPENSE`, so every
  pre-#218 cloud doc and backup file restores exactly as it always has. Verified on the
  `Medium_Phone` emulator with a **real v23→v24 migration over populated data** (installed the
  new build directly over an existing app with real budget/goal/streak history, no uninstall): no
  crash, no destructive-fallback log line, existing data intact, and the income entry point end to
  end — segmented toggle, category picker hiding, the pie/trend/limits/widget/calendar all
  correctly excluding the $2,000 test income from spend while the transaction list and day
  breakdown both show it in Sage with the net balance updating live.
- **[Issue #209] Overview's Screen Time stat no longer reads 0m while the Screen Time tracker
  shows real usage for the same day** — Room's `screen_time_sessions` row for today was only as
  fresh as `ScreenTimeViewModel`'s own 30-second poll, which runs only while that screen has been
  opened recently. Overview, often the first screen opened, could read a stale/zero figure on any
  day Screen Time hadn't been visited yet. `ScreenTimeRefresh.kt` (new) extracts
  `ScreenTimeViewModel`'s live `UsageStatsManager` query (`calculateTodayAppUsage`, was
  `calculateAppSpecificUsage`) and its `AppOpsManager` permission check (`hasUsageAccess`, was
  inlined in `checkPermission()`) into shared top-level functions — `ScreenTimeViewModel` now
  delegates to both rather than holding its own copy, so the two call sites can't drift. The new
  `refreshTodayScreenTime(db, context)` reruns the same filtered total `updateScreenTime()`
  computes (excluded apps, this app itself, the launcher, systemui) **minus** the
  `installedApps`-restriction, which only narrows further and which a caller outside
  `ScreenTimeViewModel` has no cheap way to load — omitting it matches the *existing* fallback
  `updateScreenTime()` itself already uses while that list is still loading. It's a no-op without
  Usage Access, so a device that never granted it can't have this overwrite real Room data with a
  false zero. `OverviewViewModel` calls it once on init and again from `selectDate()` whenever the
  newly-selected date is today; `dayOverview`'s existing `combine()` picks up the fresh Room row
  automatically since `getAllSessions()` is a Flow — no direct wiring into the reactive pipeline
  needed, and no risk of the live query re-running on every unrelated emission. **Verified on the
  Medium_Phone emulator with real usage data**: force-stopped the app, foregrounded the Clock app
  for ~70s (confirmed via `adb shell dumpsys usagestats`), then opened Overview directly (deep
  link, Screen Time screen never opened) — the Screen stat correctly showed 1m on first load,
  where it previously would have shown 0m.
  - **Found in passing, not fixed here (flagged as a follow-up task, not yet a filed issue):** the
    `resolveActivity(ACTION_MAIN + CATEGORY_HOME)` call both this and the original
    `ScreenTimeViewModel` code use to identify "the launcher, so being on the home screen doesn't
    count as screen time" can apparently misresolve to a just-used app instead of the real
    launcher under some circumstances (reproduced on the emulator after repeatedly launching
    Settings via `am start`) — confirmed pre-existing and not introduced by this change, because
    `ScreenTimeViewModel`'s own "TOTAL APEX TIME" headline showed the same wrong 0m at the same
    time its own itemized "Today's Apps" list correctly showed nonzero usage for the misidentified
    app. Root cause not yet investigated.
- **[Issue #210] Papers: today's featured pick no longer renders a second time as the first row
  of the queue below it.** `PapersUiState.queueRest`'s own doc comment already said its purpose
  was "no paper renders twice on the screen," but `queueExcludingRecommendations()` only strips
  `PaperSource.RECOMMENDED` rows — it has no awareness of which paper `dailyPick()` chose, and
  `dailyPick()` can select *any* WANT-status paper (SEED, DAILY, manually-added, not just
  RECOMMENDED). New `queueRestExcludingTodayPick(queue, todayPickId)` in
  `PapersRecommendations.kt` (unit-tested in `PapersRecommendationsTest`) filters out both; the
  shelf-only helper (`queueExcludingRecommendations`) is unchanged and still used by the new one
  internally, so its own existing tests/callers are unaffected. `PapersViewModel.uiState`'s
  `combine` now computes `dailyPick()` once into a local and feeds its id to the new helper for
  `queueRest`, rather than deriving `todayPick`/`queueRest` independently from the same `queue`.
  Verified on the `Medium_Phone` emulator with real seeded data: "Deep Residual Learning for Image
  Recognition" (a `MANUAL`-source pick, not `RECOMMENDED`) rendered as TODAY'S PAPER and correctly
  did **not** reappear as the first row of QUEUE · 10 below it.
- **[Issue #211] `ReceiptOcr.kt` no longer silently drops OCR lines missing a bounding box.**
  `readImageText()`'s own doc comment already said a missing box (nothing in the ML Kit API
  guarantees one) should fall back to plain text rather than dropping the line, but the
  implementation only handled the *all* lines missing case — `mapNotNull` on `line.boundingBox`
  silently discarded any individual boxless line in a mixed result, and a receipt's TOTAL line was
  as likely as any other to be the one without a box. Now `readImageText()` still reflows the
  boxed lines into rows via `reflowReceiptLines` (unchanged, still handles the two-column
  label/amount case), then appends the *text* of any boxless lines below as their own lines —
  they can't be placed into a specific row without a position, but they're no longer invisible to
  the amount/date/merchant heuristics in `ReceiptParse.kt` either. Only falls back to the whole
  unreflowed `result.text` blob when *every* line lacks a box, same as before, since that blob
  already contains those lines' text and appending them again would duplicate it. Not
  unit-testable — `readImageText` touches ML Kit's `Text`/`Line`/`Block` types directly and has no
  existing test infrastructure (`reflowReceiptLines`, which it calls into, is untouched and stays
  covered by `ReceiptParseTest`). Verified on the `Medium_Phone` emulator with a synthetic receipt
  photo (all lines boxed, so this exercises the unchanged reflow path end-to-end post-refactor):
  scanning correctly extracted `8.43` as the top amount candidate ahead of `4.50`/`3.25`/`123.00`
  (the street-number decoy `ReceiptParse.kt`'s own doc comment calls out by name), confirming the
  refactor didn't regress the standard path. The specific mixed-box scenario this fixes isn't
  reproducible on demand — ML Kit's box assignment is an internal recognizer detail, not something
  a test image can force — so this rests on the code fix matching the doc comment's already-stated
  intent, reviewed rather than device-reproduced.
- **[Issue #212] Reminder notification text now routes through `strings.xml`, like the rest of the
  app.** `ReminderWorker.kt` built the notification channel name/description, content title, and
  body fallback as raw English literals — the one notification path that never got the Issue
  #36/#53 string-resource treatment; only its Done/Snooze action labels already went through
  `stringResource`. `ReminderAlarmReceiver.kt` also hardcoded the same `"Reminder"` fallback name,
  duplicated. New keys — `reminder_channel_name`, `reminder_channel_desc`, `reminder_notif_title`,
  `reminder_notif_text`, `reminder_default_name` — mirror the exact pattern
  `ScreenTimeLimitNotifier.kt` already established for its own channel. No live locale exists yet
  (only base `values/`), so this doesn't change today's behavior — every string is
  byte-for-byte identical to the literal it replaced — but it stops these five lines from silently
  shipping untranslated the moment a `values-XX/` locale is added. Not verified with a live
  on-device notification: the emulator's UI automation grew unreliable partway through this
  session after a long run of interactions (stray taps, an unrelated `com.android.settings`
  intent-resolution quirk noted under Issue #209's follow-up), and forcing `ReminderWorker` to run
  outside the normal alarm/WorkManager path proved more effort than the change's risk profile
  warrants — it is a mechanical text-literal-to-resource move with no logic change, verified by a
  clean `assembleDebug`/`testDebugUnitTest`/`lintDebug` and by matching an already-proven pattern.
- **[Issue #219] Budget can now import CSV, round-tripping its own export format.** New
  `BudgetCsvImport.kt`: `parseCsvRows()` is a small RFC-4180-aware tokenizer (quoted fields can
  hold commas, doubled quotes, and literal newlines — the same reason `csvEscape()` quotes them
  on the way out), `parseBudgetCsv()` turns rows into `BudgetCsvImportRow`s that are either valid
  or carry a per-row `error` string (never silently dropped), and `resolveImportedCategoryId()`
  matches a row's free-text category name against the live list case-insensitively (no match =
  uncategorized — a "Subscriptions" label from a prior export is **not** special-cased back onto
  the synthetic -1L bucket). Deliberately scoped to this app's own `date,title,amount,type,
  category,description` format (always skips row 1 as the header) rather than an arbitrary bank
  CSV, per the issue's own suggested scope. `csvEscape()`'s formula-neutralizer prefix
  (`CSV_FORMULA_TRIGGERS`, now `internal` instead of `private` so both files can share it) is
  reversed on import so re-importing an exported file round-trips exactly. Unit-tested in
  `BudgetCsvImportTest` (26 cases) including a full `buildBudgetCsv` → `parseBudgetCsv` round trip.
  UI: `BudgetSettingsDialog` gained an "Import CSV" item beside "Export CSV", using
  `ActivityResultContracts.GetContent()` (`text/*`, no persisted URI permission needed — a
  one-shot read, same reasoning as the receipt-photo picker) to read the file, then
  `BudgetImportPreviewDialog` shows the valid count and every row's error before anything commits
  — Confirm is disabled with nothing valid to import. `BudgetViewModel.importItems()` commits
  exactly like `addItem()` (fresh cloudId, fire-and-forget cloud push) for each valid row.
  Verified on-device end to end: pushed a CSV with one income row, one expense row, and one
  malformed row (bad date) to the emulator, imported through the real file-picker → preview →
  confirm flow — the preview correctly reported "2 transactions ready to import" and
  "Line 4: Unreadable date", and after confirming, Budget's totals updated to exactly the
  expected `$53.63` spent / `$2,300.00` income / `$2,246.37` net, with both new rows appearing in
  the transaction list (description and INCOME styling intact) and the bad row absent.
- **[Issue #228] Screen Time's launcher exclusion no longer risks zeroing out real usage.** The
  follow-up flagged at the end of #209 above turned out to be a real, separately-filed bug, not an
  emulator artifact: `resolveActivity(ACTION_MAIN + CATEGORY_HOME, MATCH_DEFAULT_ONLY)` returns the
  platform's single "current best" HOME resolver, and on the `Medium_Phone` emulator that pick
  intermittently drifted to `com.android.settings` instead of the real launcher
  (`com.google.android.apps.nexuslauncher`) — confirmed via `adb shell cmd package query-activities`,
  which shows Settings really does declare a `MAIN`+`HOME` intent-filter, at `priority=-1000`,
  belonging to AOSP's `Settings.FallbackHome` (a real, always-present fallback for when no launcher
  is otherwise ready). Because `UsageEvents` are tracked per-*package* rather than per-activity, a
  naive alternate fix — exclude every package that can *ever* present a HOME surface, e.g. the full
  `PackageManager.getHomeActivities()` result — would have been strictly worse: it would zero out
  all genuine Settings usage permanently, not just during the occasional misresolution. The actual
  fix keeps `resolveActivity`'s candidate set (queried instead via `queryIntentActivities`, a static
  manifest lookup rather than a "current best guess," so it isn't subject to whatever transient
  state caused the drift) but disambiguates by **priority**: `topPriorityHomePackages()`
  (`ScreenTimeRefresh.kt`, unit-tested in `ScreenTimeRefreshTest`) keeps only the package(s) tied for
  the *highest* declared priority, which excludes real launchers (priority 0) while leaving
  Settings' priority -1000 fallback untouched. This was also the moment to unify the two
  near-duplicate copies of the launcher-resolution + usage-filtering logic that #209 had left behind
  (one in `ScreenTimeViewModel.updateScreenTime()`, one in `ScreenTimeRefresh.refreshTodayScreenTime()`)
  into one shared `filterTrackableUsage()` predicate both now call, alongside the new
  `resolveHomePackages()`. Verified on the `Medium_Phone` emulator: `adb shell cmd package
  query-activities -a android.intent.action.MAIN -c android.intent.category.HOME` — the exact
  query/flags `resolveHomePackages()` now runs — confirms this device's real candidate set is
  precisely `com.google.android.apps.nexuslauncher` at `priority=0` and `com.android.settings` at
  `priority=-1000`, so `topPriorityHomePackages()` keeps only the launcher. Foregrounded Settings
  for ~65s (`dumpsys window`/`mCurrentFocus` confirmed it held focus throughout), sent it home, and
  confirmed on the rebuilt app that Settings still shows non-zero minutes in the Screen Time
  screen's "Today's Apps" list rather than being silently zeroed. `testDebugUnitTest` /
  `lintDebug` / `assembleDebug` all pass, and no crash appeared in logcat across the run.
- **[Issue #220] Reminders can hand off to the user's calendar app.** New
  `ReminderCalendarExport.kt`: `calendarEventBeginMillis`/`calendarEventEndMillis` (pure,
  unit-tested in `ReminderCalendarExportTest`) compute a 30-minute event block for a timed
  reminder or a `CalendarContract`-conventional UTC-midnight-to-next-UTC-midnight span for an
  all-day one — a reminder models a due *moment*, not a duration, so 30 minutes is just long
  enough to be visible on a day view. `calendarInsertIntent()` builds an `ACTION_INSERT` intent
  against `CalendarContract.Events.CONTENT_URI` rather than writing to the provider directly — no
  new permission needed, and the calendar app shows its own confirm/edit UI before anything
  actually saves, so a repeat tap can't silently create a duplicate event the way a direct insert
  could. Wired into `ReminderEditDialog`'s title row as a calendar icon button (enabled only once
  a name is entered), reading from the dialog's *current* field state rather than a saved
  `Reminder` — works identically mid-add and mid-edit. `calendarInsertIntent()` itself isn't
  unit-tested (constructs a real `android.content.Intent` against `CalendarContract` — framework
  types unavailable in this project's plain JVM unit tests, no Robolectric); the millis math it
  depends on is the only non-trivial logic and is covered. Verified on the `Medium_Phone`
  emulator: tapping the icon launched Google Calendar with no crash — it resolved the intent and
  showed its own "no calendars have been synchronized with this device yet" state, which is this
  particular emulator having no signed-in account rather than anything this app's intent got
  wrong; the dispatch and app-resolution half of the flow (the half under this app's control) is
  confirmed working.
- **[Issue #215] Dashboard and Overview no longer render a blank body while their first Room
  flow is still loading.** Both screens already had a `loaded`/`null` gate to avoid asserting a
  *wrong* empty state before Room's `combine()` first emits (Issue #118) — the gap was that the
  gate rendered nothing at all rather than a loading indicator. `DashboardView.kt`'s
  `StreakHero`'s `!loaded` branch and `TodaySection`'s `!loaded && todayGoals.isEmpty()` branch,
  and `OverviewView.kt`'s `overview == null` branch, all now show a `CircularProgressIndicator`
  sized `ApexSpacing.xl` / stroke `ApexSpacing.hairline` — the exact visual language
  `DailyApexTipSection` already used for its own loading state on the same screen, not a new
  pattern. Not verified with a captured screenshot of the loading frame itself: it's a genuinely
  transient state (well under a second once Room's page cache is warm, which it is for the rest
  of a session after the first cold launch), and several attempts to race `adb screencap` against
  it came back already-loaded rather than mid-load. The change is a minimal, direct application
  of an already-proven pattern already in the same file, compiles clean, and passes
  `testDebugUnitTest`/`lintDebug`.
- **[Issue #214] Raw `.dp` literals swept onto `ApexSpacing`/`ApexShapes` tokens across 10
  screen files** (`NoteView.kt`, `DashboardView.kt`, `StudyTrackerView.kt`,
  `ScreenTimeTrackerView.kt`, `ReminderView.kt`, `OverviewView.kt`, `BudgetComponents.kt`,
  `SecuritySettings.kt`, `BudgetTrackerView.kt`, `RecurrencePickerDialog.kt` — `BudgetSettings.kt`
  and `GoalsView.kt` were audited too and needed no changes, their few remaining raw values are
  already-named one-off dimension constants or min-touch-target `heightIn`s, not spacing/radius).
  Exact-match spacing values (2/4/8/12/16/24/40dp) became `hairline`/`xs`/`s`/`m`/`l`/`xl`/`xxl`;
  exact-match corner radii (3/9/14/26dp) became `cell`/`control`/`container`/`sheet`. Left alone,
  deliberately: icon/avatar/component sizes (`Modifier.size`), stroke widths, min touch targets,
  named one-off geometry constants (`DONUT_SIZE`-style), and a handful of non-matching values
  (mostly 20dp/32dp paddings) with no inferable reason to force onto a token — under-fixing was
  the explicit instruction over guessing a wrong mapping. Two corner-radius calls landed on a
  token 5-7dp off their original literal (12/16dp → `container`'s 14dp, on note-attachment
  thumbnails and a full-width image viewer) — an intentional, barely-visible unification the issue
  itself called for, not an oversight. One found the sweep was *fixing* a genuine inconsistency
  rather than introducing one: Budget's FAB was hardcoded at `RoundedCornerShape(16.dp)` while
  Notes' and Reminders' FABs already used `ApexShapes.control` (9dp) — the sweep brought Budget in
  line with the other two, it didn't invent a new look. `StudyTrackerView.kt`'s chart-bar corner
  radius (2dp, no exact token match) was mapped to `ApexShapes.cell` (3dp) by direct analogy with
  `BudgetTrends.kt`'s identical already-tokenized chart-bar pattern. One value was deliberately
  kept as a raw literal with a new comment explaining why (`DashboardView.kt`'s heatmap cell
  padding: 1dp, not `hairline`'s 2dp, because cells run as small as 14dp and a 2dp gap eats a
  quarter of that). Verified: clean `assembleDebug`/`testDebugUnitTest`/`lintDebug`, plus an
  on-device visual pass on the `Medium_Phone` emulator (Budget's FAB, Overview's selected calendar
  day cell, Dashboard's heatmap) confirming no call site reads as visually broken or unintentional.
- **[Issue #222] The Budget home-screen widget has a quick-add-expense button, not just
  navigate-only.** `BudgetWidget.kt` gained a second clickable region — a small pill button below
  the existing progress row — nested inside the whole-widget "open Budget" click, the same way
  Glance/RemoteViews already give `StudyWidget`'s start/pause button its own tap target distinct
  from its container. It fires `actionStartActivity` against a second `Intent` carrying both
  `MainActivity.EXTRA_NAVIGATE_TO = "budget_tracker"` (the existing deep-link extra) and a new
  `MainActivity.EXTRA_BUDGET_QUICK_ADD` boolean. `MainActivity` threads that flag down through
  `AppNavigation` to `BudgetTrackerApp` (mirroring the already-proven `pendingRoute`/
  `onRequestedRouteConsumed` pattern notification taps use), where a `LaunchedEffect` opens the
  add-item dialog immediately and consumes the flag so returning to Budget later doesn't reopen
  it. Deliberately the "simpler" option from the issue's suggested scope (deep-link into the
  existing dialog) rather than a Glance-native amount-entry surface, since Glance has no text
  input widget to build one with. Verified on-device: simulating the exact intent the widget
  button constructs (`am start` with both extras) reliably opened Budget straight into the
  add-item dialog on a cold start, confirming the `MainActivity` → `AppNavigation` →
  `BudgetTrackerApp` wiring end to end. The widget-side click itself (Glance button → this same
  intent) wasn't verified with an actually-placed widget instance — placing a *new* widget via
  drag-and-drop proved too unreliable to automate over adb on this emulator — but the button is
  structurally identical to `StudyWidget`'s already-working one, just a different destination
  `Intent`.
- **[Issue #221] ApexTracker now ships a second locale (Spanish, `values-es/`)** — the payoff for
  every prior UI-string-extraction pass (#36/#53/#65/#112/#114/#119/#120), and the reason those
  passes existed at all. Two pieces:
  - **Infrastructure**: `app/build.gradle.kts` sets `androidResources { generateLocaleConfig =
    true }` (AGP auto-generates `res/xml/_generated_res_locale_config.xml` from whichever
    `values-<locale>/` directories exist, and auto-injects `android:localeConfig` into the merged
    manifest — no hand-authored XML to keep in sync as locales are added or removed). This
    requires a `res/resources.properties` file declaring the base directory's own locale
    (`unqualifiedResLocale=en-US` — the *module's* `res/`, not `res/values/`; AGP's own error
    message if this is missing or misplaced points at the wrong location, costing a debug cycle
    here). Wires up the API 33+ per-app language picker (Settings → Apps → ApexTracker →
    Language) for free — no in-app language switcher UI needed, adding one is a separate,
    optional future issue if wanted.
  - **Translation**: `values-es/strings.xml`, a complete Latin American Spanish translation of
    all 498 strings + 6 `<plurals>` blocks (same keys, same order, same section comments,
    untranslated proper nouns), using "tú" throughout since this is a personal single-user app,
    not enterprise software. Every format specifier (`%1$s`, `%2$d`, …) verified to match the
    English source in count and type, reordered only where natural Spanish word order calls for
    it. `lintDebug` flags `MissingQuantity: es also wants "many"` on the 6 plurals — a real but
    obscure CLDR rule (large multiples of a million) with no realistic trigger in this app's data
    (transaction counts, streak days, etc. never reach that range); left unaddressed as the
    same accepted tradeoff most apps make, not a defect. The issue's own listed blocker —
    Reminder notification text bypassing string resources — was already fixed earlier in this
    same pass (#212).
  - **Verified on-device**: `adb shell cmd locale set-app-locales com.example.apextracker
    --locales es` (the exact mechanism the system language picker uses) on the `Medium_Phone`
    emulator — Dashboard, Overview, and the bottom nav all rendered correctly in Spanish,
    including locale-aware date formatting ("JUEVES agosto 27") and number formatting ("0,00
    US$"), with user-entered content (goal names, reminder titles) correctly left untranslated
    since those aren't resource strings.
- **[Issue #223] Papers can now be explicitly linked to each other** — deferred in Plan.md's
  settled decisions and never turned into a tracked issue until now; this was the last item in
  the open backlog. New `PaperLink` entity (`paper_links` table, undirected — `paperCloudId`/
  `relatedPaperCloudId` with no meaningful direction, `PaperLinkDao.findExisting`/
  `PaperLinks.kt`'s `canLinkPapers` both check either stored order) keyed on the two papers'
  *cloudIds*, the same cross-device-stable-reference reasoning as `Paper.topicCloudId` and
  `resolveReminderParentLinks`. DB v24 → **v25**, `MIGRATION_24_25` (additive `CREATE TABLE IF
  NOT EXISTS` + 3 indices, mirrors `MIGRATION_21_22`). Pure logic in `PaperLinks.kt`
  (`otherPaperCloudId`, `relatedPapersFor`, `linkablePapersFor`, `canLinkPapers`), unit-tested in
  `PaperLinksTest.kt`. A link pointing at a since-deleted paper is silently excluded from
  `relatedPapersFor` — nothing to clean up, the same "dangling reference resolves to nothing"
  treatment `topicCloudId` already gets. Sync follows the standard 5-part Firestore shape at
  `users/{uid}/paper_links/{cloudId}`, registered in `performInitialSync`, `SyncCoordinator`, and
  `replaceCloudWithLocal`, unit-tested in `FirebaseDocParsingTest`; full-dataset backup/restore
  (`BackupData.paperLinks`, `BackupManager.kt`) follows suit, with `appDbVersion` bumped to 25
  alongside it. UI: `PaperDetailSheet` gained a "Related papers" section — each linked paper is a
  row with an unlink (×) button, tapping the row itself navigates to that paper's own detail
  sheet (reusing the same composable, so the link graph is browsable both directions); "Link a
  paper" opens `PaperLinkPickerDialog`, a simple list of `linkablePapersFor`'s result (every other
  paper minus itself and already-linked papers) so every row is guaranteed valid. New strings
  (`papers_related_label`, `papers_related_add`, `papers_link_picker_title`,
  `papers_link_picker_empty`, `cd_papers_unlink`) were added to both `values/strings.xml` and
  `values-es/strings.xml` (Issue #221's now-live second locale), keeping the two in sync from the
  start rather than letting Spanish drift immediately after shipping it.
  **Verified on-device (`Medium_Phone` emulator) with a real v24→v25 migration over populated
  data** (installed directly over an existing app with real Papers history, no uninstall): no
  crash, no destructive-fallback log line, existing queue/history/recommendations all intact.
  Linked "Adam: A Method for Stochastic Optimization" to "BERT: Pre-training of Deep Bidirectional
  Transformers…" via the picker — the link appeared correctly from *both* papers' detail sheets
  (confirming the undirected read), survived an app restart (confirming the Room round-trip), and
  removing it via the × button correctly cleared it from both sides. `testDebugUnitTest`
  (`PaperLinksTest` — 10 cases — plus 2 new `FirebaseDocParsingTest` cases) and `lintDebug` both
  pass clean. This closes the entire issue backlog worked through in this deployment pass.

## 2026-08-28 Deployment-readiness audit fix pass

With the tracked issue backlog fully cleared, a fine-tooth-comb multi-agent audit (release
config/manifest/permissions, DB migrations, Firebase sync/auth, background work/notifications/
widgets, encryption/backup/security, Budget+Study+Screen Time, Notes+Reminders+Dashboard+
Overview+Papers, and i18n/theming completeness — seven parallel deep-read passes, each
cross-checked against this file so nothing already-fixed was re-flagged) surfaced 27 new issues,
filed as `#230`–`#256`. Working them the same one-issue-per-commit way as the deployment pass
above. This section is a running log; read `gh issue list` for current backlog state.

- **[Issue #230] Screen Time's cross-device listener no longer leaks the previous account's data
  after an account switch.** `ScreenTimeViewModel.init` used to gate its
  `getOtherDevicesScreenTimeFlow()` collection with a one-shot `if (firebaseManager.userId !=
  null)` check at construction time — the underlying Firestore listener's uid was captured once
  and never re-read, so an account switch (which never touches this kept-alive tab's
  `ViewModelStore`) left the old account's listener writing into the new account's session. This
  was exactly Issue #186's leak class in a live-listener path that fix didn't reach. New
  `FirebaseManager.userIdFlow()` wraps a `FirebaseAuth.AuthStateListener` in a `callbackFlow`,
  emitting the current uid on every auth-state change (sign-in, sign-out, account switch);
  `ScreenTimeViewModel` now `flatMapLatest`s on it, so an account change cancels the old
  listener and starts a fresh one for the new uid — and clears `latestOtherDevices` immediately
  on the switch (before the new listener's first snapshot even arrives) so stale cross-device
  data can't linger on screen for even a brief window. `@OptIn(ExperimentalCoroutinesApi::class)`
  added to the class for `flatMapLatest`, matching the same unavoidable opt-in
  `OverviewViewModel.kt` already carries. **Not fully verified on-device**: reproducing the
  original leak needs two distinct signed-in Google accounts and an interactive account switch,
  which isn't automatable here (the same "signed-in round-trip not automatable" limitation
  documented elsewhere in this file for sync work) — verified instead by `assembleDebug`/
  `testDebugUnitTest`/`lintDebug` passing clean and a smoke-launch of the Screen Time screen on
  the `Medium_Phone` emulator showing no crash with the new `flatMapLatest` wiring in place.
- **[Issue #231] Screen Time's 30-second cloud push now goes through `safeCloudCall`, like every
  other push in the app.** `updateScreenTime()`'s `firebaseManager.uploadScreenTimeSession(...)`
  call was the one exception to the codebase-wide convention — it ran unguarded inside the
  always-on 30s polling loop, so any Firestore exception there (permission-denied, a deadline, or
  the shared client being torn down mid-write by `firestore.terminate()` during an account
  switch) had no `CoroutineExceptionHandler` to catch it and would crash the process. Wrapped in
  `safeCloudCall(TAG, "upload screen time") { ... }`; added the same `private companion object {
  const val TAG = "ScreenTimeViewModel" }` pattern every other ViewModel already uses, and
  normalized the two existing `safeCloudCall("ScreenTimeViewModel", ...)` call sites (exclude/
  include app) to the new `TAG` constant while in the file. Verified: clean `assembleDebug`/
  `testDebugUnitTest`/`lintDebug`, plus a smoke-launch of Screen Time on the `Medium_Phone`
  emulator with no crash. Not separately reproducible on-device (would require forcing a real
  Firestore write failure at the exact moment the poll fires) — the fix is a mechanical
  apply-the-established-pattern change with no logic change to what gets uploaded.
- **[Issue #232] Restoring a backup exported before Papers' `topicCloudId` field shipped no longer
  crashes the app.** `topicCloudId` was added to `Paper` on 2026-08-07, eight days after Papers
  backup support shipped 2026-07-30 — any backup from that window is missing the field, and
  `backupGson()` has no Kotlin-constructor support (see its own class doc), so a missing field
  deserializes to a raw Java `null` rather than the entity's Kotlin default, which then crashes
  Room's `insertPaper` on restore (`NullPointerException: ... bindText`, confirmed by reproducing
  it on-device before the fix). `parseBackupJson` now normalizes `topicCloudId` to `""` when
  absent, the same pattern already used for `Goal.cadence`/`BudgetItem.type` — with a comment
  flagging that any future field added to an already-backed-up entity needs the same treatment.
  Also hardened the restore call site (`AppSettingsSheet.kt`): `restoreBackupAndReconcile(...)`
  is now wrapped in try/catch, so **any** future schema-drift gap of this shape degrades to the
  existing "backup can't be restored" status message instead of crashing — this is the safety net
  that would have caught the issue even before the specific `topicCloudId` fix landed. New
  regression test in `BackupDataTest.kt`. **Verified on-device end-to-end**: reproduced the exact
  crash pre-fix with a synthetic backup JSON missing `topicCloudId` (confirmed via logcat: the
  predicted `NullPointerException` at `PaperDao_Impl.insertPaper`), then confirmed post-fix that
  the same file restores cleanly — no crash, "Data restored," and the paper appears correctly in
  the Papers screen with all its fields intact.
- **[Issue #233] Locked Budget/Notes content can no longer be captured in the OS recents/
  task-switcher thumbnail.** `LockGate` only re-locked on `MainActivity.onStop()`, but the OS
  takes its recents thumbnail at the same backgrounding instant — before that re-lock has a
  chance to hide anything — so an unlocked Budget/Notes screen sat in Recents (and any
  screen-recording tool) in the clear, defeating the point of the lock against exactly the
  "someone else picks up the unlocked phone" threat model it's meant to cover. New
  `SecureWindowGuard` (`SecuritySettings.kt`) is a reference-counted holder for
  `WindowManager.LayoutParams.FLAG_SECURE`, acquired/released via a `DisposableEffect` inside
  `LockGate` itself whenever `lockEnabled == true` — reference-counted rather than a plain
  set/clear pair because the NavHost's custom crossfade transitions keep the outgoing and
  incoming destination composed simultaneously during navigation, and a naive pair from each
  `LockGate` instance would let one's `onDispose` clear a flag the other still needs. Applies to
  all three `LockGate` call sites (`overview`, `budget_tracker`, `notes`) automatically since
  they all route through the same composable. **Verified**: clean `assembleDebug`/
  `testDebugUnitTest`/`lintDebug`, plus an on-device smoke test confirming the *default*
  (lock-disabled) path has zero regression — screenshots still capture normally when no module
  lock is enabled, which is the state every existing on-device verification in this file already
  depends on. **Not verified end-to-end** (lock enabled → FLAG_SECURE actually applied → recents
  thumbnail confirmed blank): enabling the biometric lock toggle requires one real
  biometric/PIN auth first, and the `Medium_Phone` emulator has no screen lock configured
  (`locksettings get-disabled` → `true`) — the same "not adb-automatable" limitation the original
  Issue #45 section of this file already documents for this exact feature. Verified instead by
  code review: `SecureWindowGuard`'s acquire/release pairing is symmetric on every
  `DisposableEffect` exit path (including recomposition when `lockEnabled` changes), and
  `WindowManager.LayoutParams.FLAG_SECURE` is the standard, well-established Android mechanism
  for this exact problem.
- **[Issue #234] Pressing Enter right after a bullet marker in Notes no longer deletes the bullet
  and corrupts the line's formatting.** `handleNoteContentChange`'s empty-bullet-line detection
  (deciding whether Enter should outdent/clear a bullet vs. continue it onto a new line) compared
  `lastLine.trim() == prefix.trim()`, where `lastLine` was only the text *before* the cursor on
  the newly-split line — not the whole original line. Placing the cursor right after "• " in
  "• Hello" and pressing Enter made `lastLine` exactly "• ", which trimmed equal to the prefix,
  so the code took the "clear an empty bullet" branch even though the line had real content —
  silently dropping the bullet and leaving "Hello" on the next line with no formatting at all.
  Fixed by computing the comparison from the whole original line (read from `oldValue`, before
  the Enter split it) instead of the post-split fragment. Two new regression tests in
  `NoteBulletEditingTest.kt`, hand-traced against the actual implementation to confirm expected
  output (`"• Hello"` at cursor index 2 → `"• \n• Hello"`; a genuinely empty `"• "` bulleted line
  still correctly clears to `"\n"`). **On-device**: confirmed `bulletRegex` (`^(\s*[•◦▪])\s`)
  requires a trailing space to match at all, consistent with the fix's reasoning, and exercised
  the note editor's real bullet toolbar + text input; a clean, unambiguous repro of the exact
  reported cursor position (index 2, immediately after "• ") proved hard to land reliably via
  blind tap coordinates on a small glyph boundary in the time available, so the authoritative
  verification here is the pure-function unit tests, which exercise `handleNoteContentChange`
  with the exact same `TextFieldValue` inputs the real UI produces for this keystroke — not an
  approximation of the bug, the same code path with a controlled, exact cursor index. Clean
  `assembleDebug`/`testDebugUnitTest`/`lintDebug`.
- **[Issue #235] `SyncCoordinator.stop()` now actually waits for every listener to detach before
  returning**, closing a race that could reopen Issue #186's cross-account leak. Cancellation of a
  coroutine suspended in a `callbackFlow`'s `collect()` is cooperative — if a Firestore snapshot
  for the outgoing account was already in flight (`trySend` already called) at the moment
  `job?.cancel()` fired, it could still be applied to Room afterward. `MainActivity.kt`'s
  account-switch `LaunchedEffect` calls `SyncCoordinator.stop()` immediately before
  `clearLocalUserData(...)` wipes Room, so a listener that hadn't actually finished detaching yet
  could re-insert the old account's row into what's supposed to be a clean slate. `stop()` and
  `start()` (which calls `stop()` first) are both now `suspend`, and `stop()` uses
  `job?.cancelAndJoin()` instead of a plain `cancel()`, so every listener's `awaitClose {
  listener.remove() }` has genuinely run before the caller proceeds. Every existing call site
  (`MainActivity.kt`'s `LaunchedEffect`, `BackupManager.kt`'s `restoreBackupAndReconcile`) was
  already inside a suspend context, so this compiled with no call-site changes needed — confirmed
  by a clean `assembleDebug`. Verified: clean `testDebugUnitTest`/`lintDebug`, plus a cold-start
  smoke test on the `Medium_Phone` emulator showing no crash. Not separately reproducible
  on-device (the race window is a handful of milliseconds during an interactive account switch,
  which — per this file's existing "signed-in round-trip not automatable" note — can't be driven
  end-to-end here); verified instead by the `cancelAndJoin` semantics being the documented,
  correct fix for exactly this class of coroutine-cancellation race.
