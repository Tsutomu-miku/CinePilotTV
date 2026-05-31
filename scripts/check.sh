#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/check"
MAIN_CLASSES="$BUILD_DIR/main"
TEST_CLASSES="$BUILD_DIR/test"

required_docs=(
  "$ROOT_DIR/README.md"
  "$ROOT_DIR/docs/REQUIREMENTS.md"
  "$ROOT_DIR/docs/ROADMAP.md"
  "$ROOT_DIR/docs/PROJECT_SPEC.md"
  "$ROOT_DIR/docs/ARCHITECTURE.md"
  "$ROOT_DIR/docs/CODE_STRUCTURE.md"
  "$ROOT_DIR/docs/PROTOCOL_NOTES.md"
  "$ROOT_DIR/docs/VERIFICATION.md"
)

for doc in "${required_docs[@]}"; do
  if [[ ! -s "$doc" ]]; then
    echo "Missing required doc: $doc" >&2
    exit 1
  fi
done

if ! grep -q 'include(":app", ":core")' "$ROOT_DIR/settings.gradle.kts"; then
  echo "settings.gradle.kts must include both :app and :core" >&2
  exit 1
fi

if ! grep -q 'implementation(project(":core"))' "$ROOT_DIR/app/build.gradle.kts"; then
  echo "app/build.gradle.kts must depend on project(\":core\")" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/core/build.gradle.kts" ]]; then
  echo "Missing core Gradle module build file" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/CinePilotRuntime.kt" ]]; then
  echo "Missing Android app runtime entry" >&2
  exit 1
fi

if ! grep -q 'CinePilotRuntime.create' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must initialize CinePilotRuntime" >&2
  exit 1
fi

if ! grep -q 'workflowController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must drive TvWorkflowController" >&2
  exit 1
fi

for ui_text in "连接服务器" "登录" "首页" "播放"; do
  if ! grep -q "$ui_text" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
    echo "MainActivity is missing TV UI text: $ui_text" >&2
    exit 1
  fi
done

if ! grep -q 'TvWorkflowController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/CinePilotRuntime.kt"; then
  echo "CinePilotRuntime must expose TvWorkflowController" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt" ]]; then
  echo "Missing Media3 player host" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlaybackBridge.kt" ]]; then
  echo "Missing Media3 playback bridge" >&2
  exit 1
fi

if ! grep -q 'PlaybackSessionController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must create PlaybackSessionController" >&2
  exit 1
fi

if ! grep -q 'Media3PlayerHost' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must initialize Media3PlayerHost" >&2
  exit 1
fi

if ! grep -q 'usesCleartextTraffic="true"' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must allow cleartext traffic for local Jellyfin/Emby HTTP servers" >&2
  exit 1
fi

if ! grep -q 'PlaybackUrlAuthorizer' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must authorize playback URLs" >&2
  exit 1
fi

if ! grep -q '打开播放器' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must expose a player launch action" >&2
  exit 1
fi

if ! grep -q '诊断信息' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must expose diagnostics" >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$MAIN_CLASSES" "$TEST_CLASSES"

javac --release 17 -d "$MAIN_CLASSES" $(find "$ROOT_DIR/core/src/main/java" -name '*.java' | sort)
javac --release 17 -cp "$MAIN_CLASSES" -d "$TEST_CLASSES" $(find "$ROOT_DIR/core/src/test/java" -name '*.java' | sort)
java -cp "$MAIN_CLASSES:$TEST_CLASSES" tv.cinepilot.core.protocol.ProtocolCoreTest
java -cp "$MAIN_CLASSES:$TEST_CLASSES" tv.cinepilot.core.tv.TvWorkflowTest

echo "check passed"
