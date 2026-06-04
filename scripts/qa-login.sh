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
  echo "adb not found. Install Android platform-tools first." >&2
  exit 1
fi

SERVER="${CINEPILOT_QA_SERVER:-${1:-}}"
USERNAME="${CINEPILOT_QA_USERNAME:-${2:-}}"
PASSWORD="${CINEPILOT_QA_PASSWORD:-${3:-}}"

if [[ -z "$SERVER" || -z "$USERNAME" ]]; then
  echo "Usage: CINEPILOT_QA_SERVER=http://host:8096 CINEPILOT_QA_USERNAME=user CINEPILOT_QA_PASSWORD=pass ./scripts/qa-login.sh" >&2
  echo "This starts the debug APK with a debug-only QA login intent. Do not use it for release builds." >&2
  exit 1
fi

DEVICE_COUNT="$("$ADB" devices | awk 'NR > 1 && $2 == "device" { count++ } END { print count + 0 }')"
if [[ "$DEVICE_COUNT" -eq 0 ]]; then
  echo "No online Android device or emulator found." >&2
  exit 1
fi
if [[ "$DEVICE_COUNT" -gt 1 && -z "${ANDROID_SERIAL:-}" ]]; then
  "$ADB" devices >&2
  echo "Multiple Android devices are online. Set ANDROID_SERIAL to choose the QA target." >&2
  exit 1
fi

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  "$ADB" -s "$ANDROID_SERIAL" shell am start \
    -n tv.cinepilot.tv/.MainActivity \
    --es qa_server "$SERVER" \
    --es qa_username "$USERNAME" \
    --es qa_password "$PASSWORD"
else
  "$ADB" shell am start \
    -n tv.cinepilot.tv/.MainActivity \
    --es qa_server "$SERVER" \
    --es qa_username "$USERNAME" \
    --es qa_password "$PASSWORD"
fi
