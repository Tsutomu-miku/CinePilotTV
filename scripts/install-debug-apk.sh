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

if [[ -n "${ANDROID_SERIAL:-}" ]]; then
  INSTALL_OUTPUT="$("$ADB" -s "$ANDROID_SERIAL" install -r "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk" 2>&1)" || {
    echo "$INSTALL_OUTPUT" >&2
    if grep -q 'INSTALL_FAILED_USER_RESTRICTED' <<<"$INSTALL_OUTPUT"; then
      echo "设备拒绝通过 USB 安装 APK。请在设备开发者选项中开启“通过 USB 安装”和“USB 调试（安全设置）”，并在安装确认弹窗中点允许。" >&2
    fi
    exit 1
  }
  echo "$INSTALL_OUTPUT"
  "$ADB" -s "$ANDROID_SERIAL" shell am start -n tv.cinepilot.tv/.MainActivity
else
  INSTALL_OUTPUT="$("$ADB" install -r "$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk" 2>&1)" || {
    echo "$INSTALL_OUTPUT" >&2
    if grep -q 'INSTALL_FAILED_USER_RESTRICTED' <<<"$INSTALL_OUTPUT"; then
      echo "设备拒绝通过 USB 安装 APK。请在设备开发者选项中开启“通过 USB 安装”和“USB 调试（安全设置）”，并在安装确认弹窗中点允许。" >&2
    fi
    exit 1
  }
  echo "$INSTALL_OUTPUT"
  "$ADB" shell am start -n tv.cinepilot.tv/.MainActivity
fi
