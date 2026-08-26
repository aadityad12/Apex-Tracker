#!/usr/bin/env bash
#
# Generates a release signing keystore for ApexTracker and writes the matching
# keystore.properties at the repo root, wired into app/build.gradle.kts automatically.
#
# This creates a permanent, irreplaceable secret. If it's ever lost before the app's first
# Play Store upload, the fix is simple (generate a new one, nothing is published yet). If it's
# lost AFTER the first upload and Play App Signing was not enabled, there is no fix — that
# applicationId can never be updated again. Read docs/release-signing.md before running this
# for real, not just this comment.
#
# Usage:
#   ./scripts/generate_release_keystore.sh [output-path]
#
# output-path defaults to ~/keystores/apextracker-release.jks (deliberately outside the repo —
# see docs/release-signing.md for why .gitignore alone isn't enough insurance for this file).

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_PATH="$HOME/keystores/apextracker-release.jks"
OUT_PATH="${1:-$DEFAULT_PATH}"
PROPS_FILE="$REPO_ROOT/keystore.properties"

if [ -e "$OUT_PATH" ]; then
    echo "Refusing to overwrite existing file: $OUT_PATH" >&2
    exit 1
fi

if [ -e "$PROPS_FILE" ]; then
    echo "Refusing to overwrite existing $PROPS_FILE" >&2
    exit 1
fi

if ! command -v keytool >/dev/null 2>&1; then
    echo "keytool not found on PATH. It ships with the JDK — try running this from a shell" >&2
    echo "where JAVA_HOME/bin is on PATH (the same JDK 17+ CLAUDE.md has you use for Gradle)." >&2
    exit 1
fi

mkdir -p "$(dirname "$OUT_PATH")"

echo "Generating a release keystore at: $OUT_PATH"
echo "You will be asked for a keystore password and a key password — they can be the same"
echo "value, but whatever you choose, save it in a password manager right now. There is no"
echo "recovery path for a forgotten password on this file."
echo

read -r -p "Key alias [apextracker]: " KEY_ALIAS
KEY_ALIAS="${KEY_ALIAS:-apextracker}"

read -r -p "Your name (for the certificate, not shown to users) [ApexTracker]: " CERT_NAME
CERT_NAME="${CERT_NAME:-ApexTracker}"

keytool -genkeypair -v \
    -keystore "$OUT_PATH" \
    -alias "$KEY_ALIAS" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=$CERT_NAME, OU=, O=, L=, ST=, C=US"

echo
echo "Keystore created. Re-enter the same two passwords now so keystore.properties can be"
echo "written for Gradle — they are not re-derived from the keystore, this just saves you"
echo "retyping the path/alias by hand."
read -r -s -p "Store password: " STORE_PASSWORD
echo
read -r -s -p "Key password: " KEY_PASSWORD
echo

cat > "$PROPS_FILE" <<EOF
storeFile=$OUT_PATH
storePassword=$STORE_PASSWORD
keyAlias=$KEY_ALIAS
keyPassword=$KEY_PASSWORD
EOF

echo
echo "Wrote $PROPS_FILE (gitignored — verify with: git check-ignore -v keystore.properties)."
echo "./gradlew bundleRelease will now produce a signed release bundle."
echo
echo "Next: back up $OUT_PATH itself somewhere durable and outside this machine alone —"
echo "a password manager's file attachment, an encrypted cloud folder, whatever you already"
echo "trust with other irreplaceable secrets. This file is not covered by anything else in"
echo "this repo's backup story."
