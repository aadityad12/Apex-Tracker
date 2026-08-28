# ApexTracker Privacy Policy

**Last updated: August 28, 2026**

ApexTracker is a personal productivity app for tracking budgets, study time, screen time,
reminders, notes, and reading — built for one person to use daily, not to build a user base or
monetize attention. This policy explains what data the app touches, why, and where it goes.

## The short version

Your data lives on your device by default. Signing in with Google is optional and only turns on
sync between your own devices — it does not send your data anywhere else. The app has no ads, no
analytics, no tracking SDKs, and does not sell data to anyone, ever.

## What data the app collects, and why

| Feature | What's stored | Where |
|---|---|---|
| Budget, Study, Screen Time, Reminders, Notes, Papers, Dashboard/Goals | Everything you enter — amounts, categories, note text, session times, goal progress | On your device, in an encrypted local database. Also in Firestore under your account **only if you sign in** (see below). |
| Screen Time | Which apps you use and for how long, read from Android's on-device usage-access API | Stays on your device; only reaches the cloud if you sign in, and then only to your own account for viewing across your own devices |
| Receipt scanning (Budget) | A photo you pick to prefill an expense — read on-device with on-device text recognition | Never uploaded anywhere, never copied into the app's own storage; the photo stays wherever you picked it from |
| Daily Apex Tip (opt-in) | If you turn this on, a small set of **anonymized daily totals** — today's spending amount, study minutes, screen-time minutes, goal counts, and your current streak | Sent to Google's Gemini model (via Firebase AI Logic) to generate one suggestion. Names, note text, transaction details, and app names are never included — only the aggregate numbers. This is off by default and the in-app toggle states exactly what's sent before you turn it on. |

## Signing in and cloud sync (optional)

Signing in uses Google Sign-In and is entirely optional — every feature above works fully
offline without it. If you do sign in:

- Your data syncs to a private area of our Firestore database scoped to your account
  (`users/{your-account-id}/...`). Server-side security rules enforce that only your signed-in
  account can read or write your own data — not other users, not us browsing casually.
- Sync exists so your data follows you to a second device signed into the same account. It is
  not used for anything else — no aggregation across users, no analytics, no advertising.
- Signing out stops future sync; it does not retroactively delete what was already synced (see
  Data deletion below).

## What we don't do

- No advertising, and no ad SDKs.
- No analytics or crash-tracking SDKs beyond what Google Sign-In/Firestore themselves need to
  function as a login and database service.
- No selling, renting, or sharing your data with third parties for their own purposes.
- No account is required to use the app in full.

## Data deletion

- **Uninstalling the app** deletes everything stored locally, immediately and irreversibly.
- **Cloud data** (present only if you ever signed in): email **aaditya.d.desai@gmail.com** with your
  Google account email and we will delete your data from Firestore. We don't yet have a
  self-service "delete my account" button in the app itself — if that matters to you, ask, and
  it can be prioritized.

## Permissions the app requests, and why

- **Usage Access** (`PACKAGE_USAGE_STATS`) — lets Screen Time measure how long you use other
  apps. Granted manually in system settings, never used for anything besides showing you that
  number.
- **Notifications** — for reminder alerts.
- **Exact alarms** and **run at boot** — so a reminder set for a specific time actually fires at
  that time, including after a restart.
- **Internet** — required for optional Google Sign-In and Firestore sync, and for the optional
  Daily Apex Tip request.
- The app can see which other apps have a launcher icon (so Screen Time can list "apps you can
  actually open"), not a list of every package installed on your device.

## Security

- The local database is encrypted at rest (SQLCipher, with the encryption key itself protected
  by Android's hardware-backed Keystore).
- An optional biometric/PIN lock can gate the Budget and Notes screens behind a fresh device
  unlock.
- Firestore access is restricted by server-side rules to your own signed-in account only.

## Children's privacy

ApexTracker is not directed at children under 13 and we do not knowingly collect data from
children under 13.

## Changes to this policy

If this policy changes in a way that matters, the "Last updated" date above will change and, for
significant changes, we'll note it in the app itself.

## Contact

Questions about this policy or your data: **aaditya.d.desai@gmail.com**

---

*This app is developed and maintained by Aaditya Desai.*
