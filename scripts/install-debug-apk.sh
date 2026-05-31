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

ADB_TARGET=()
if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  ADB_TARGET=(-s "$ANDROID_SERIAL")
fi

DEVICE_COUNT="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  echo "No online Android device or emulator found. Connect a phone, Android TV device, or start an emulator." >&2
  exit 1
fi
if [[ "$DEVICE_COUNT" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
  "$ADB" devices >&2
  echo "Multiple Android devices are online. Set ANDROID_SERIAL to choose the install target." >&2
  exit 1
fi

if [[ -n "${ANDROID_SERIAL:-}" ]] && ! "$ADB" devices | awk -v serial="$ANDROID_SERIAL" '$1 == serial && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'; then
  "$ADB" devices >&2
  echo "ANDROID_SERIAL=$ANDROID_SERIAL is not an online Android device." >&2
  exit 1
fi

"$ADB" "${ADB_TARGET[@]}" install -r "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
"$ADB" "${ADB_TARGET[@]}" shell am start -n tv.cinepilot.tv/.MainActivity
