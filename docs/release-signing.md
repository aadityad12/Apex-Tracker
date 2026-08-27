# Release signing

Every Android app is cryptographically signed before it can be installed; Play Store uploads are
no exception. This doc covers how ApexTracker's release signing is wired up, how to generate the
actual keystore, and what changes once you have one.

## Current state

There is no release keystore in this repo, and there shouldn't be — it's a secret, not code.
`app/build.gradle.kts` reads signing config from `keystore.properties` at the repo root
(gitignored) if that file exists, and silently does nothing otherwise. That means:

- `./gradlew assembleDebug` / `installDebug` are completely unaffected either way.
- `./gradlew assembleRelease` / `bundleRelease` work today, but produce an **unsigned** artifact
  — fine for local inspection (this is how the R8/proguard verification in CLAUDE.md's
  2026-08-07 security pass was done), but not installable on a device or uploadable to Play
  Console.
- Once `keystore.properties` exists, the exact same commands produce a signed, uploadable
  artifact. Nothing else about the build changes.

## Generating a keystore

```bash
./scripts/generate_release_keystore.sh
```

This runs `keytool` interactively, asking for a key alias and a display name for the
certificate, then prompts you to set (and re-enter) a store password and a key password. It
writes the keystore to `~/keystores/apextracker-release.jks` by default (pass a different path
as the first argument if you'd rather put it somewhere else — anywhere outside this repo is the
point), and writes `keystore.properties` at the repo root pointing at it.

**Read this before running it for real:**

- **This secret cannot be regenerated.** Unlike an API key, there is no console to rotate this
  from. If you lose the keystore file or forget its passwords *after* your first Play Store
  upload, and you are not using Play App Signing (see below), you can never update that app
  listing again — you'd have to publish a new app under a new package name and lose every
  install, rating, and review the old one had.
- **Back the `.jks` file up somewhere durable and outside this machine alone** — a password
  manager's file-attachment storage, an encrypted cloud folder, whatever you already trust with
  other irreplaceable secrets. `.gitignore` keeps it out of this repo; it does not back it up
  anywhere.
- **The two passwords are equally irreplaceable.** Save them in a password manager the moment
  the script asks for them, not "after I'm done setting this up."

## Play App Signing (recommended, and the modern default)

Play Console offers **Play App Signing**: you upload your app signed with an *upload key* (the
one generated above), and Google re-signs it with a separate *app signing key* that Google
manages and stores for you before distributing it to users. The practical effect: if you ever
lose the upload keystore, Google's account-recovery process can issue you a new upload key,
because the actual app signing key that matters to users' devices was never yours to lose in the
first place.

New apps are enrolled in Play App Signing automatically when you first upload to Play Console —
there's no separate opt-in step to remember here, just be aware it's happening and why it makes
the keystore above meaningfully less catastrophic to eventually lose (though still worth not
losing).

## What else needs the release signing certificate

Google Sign-In (via Firebase Auth) authorizes callers by SHA-1 certificate fingerprint, not just
by package name. Today only this machine's **debug** SHA-1 is registered with the
`apex-tracker-3ed29` Firebase project (see CLAUDE.md's Environment Setup section). Before sign-in
will work in a release build, you need to add a second fingerprint:

1. If using Play App Signing: after your first upload, Play Console → Setup → App signing shows
   the **App signing key certificate**'s SHA-1. Use that one — it's the certificate that will
   actually sign what users receive, not your local upload keystore.
2. If not using Play App Signing (uncommon for a new app): get your own release keystore's SHA-1
   with `keytool -list -v -keystore <path> -alias <alias>`.
3. Firebase Console → Project Settings → your Android app → Add fingerprint → paste the SHA-1.
4. Download the updated `google-services.json` and replace `app/google-services.json` (still
   gitignored, still never committed) — it now covers both the debug and release certificates.

Skipping this step doesn't crash the release build; it just makes Google Sign-In fail silently
for anyone using it, the same class of "no error, just doesn't work" failure the Issue #206 fix
(`CLAUDE.md`, "2026-08-25 Backup excludes the SQLCipher key wrapper") was about on the encryption
side. Verify sign-in against an actual signed release build before shipping it, not just debug.
