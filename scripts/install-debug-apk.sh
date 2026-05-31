#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ADB="${ADB:-}"

if [[ -z "$ADB" ]]; then
  if [[ -x "$ROOT_DIR/build/android-sdk/platform-tools/adb" ]]; then
    ADB="$ROOT_DIR/build/android-sdk/platform-tools/adb"
  else
    ADB="$(command -v adb || true)"
  fi
fi

if [[ -z "$ADB" ]]; then
  echo "adb not found. Install Android platform-tools or run Android SDK setup first." >&2
  exit 1
fi

"$ROOT_DIR/gradlew" :app:assembleDebug

DEVICE_COUNT="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  echo "No online Android device or emulator found. Connect an Android TV device or start an emulator." >&2
  exit 1
fi

"$ADB" install -r "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
"$ADB" shell am start -n tv.cinepilot.tv/.MainActivity

