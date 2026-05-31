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

if [[ ! -x "$ROOT_DIR/scripts/bootstrap-gradle-wrapper.sh" ]]; then
  echo "Missing executable Gradle wrapper bootstrap script" >&2
  exit 1
fi

if [[ ! -x "$ROOT_DIR/scripts/install-debug-apk.sh" ]]; then
  echo "Missing executable APK install script" >&2
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

if ! grep -q 'onBackPressed' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must handle Android TV Back navigation" >&2
  exit 1
fi

if ! grep -q 'TvRoute.PLAYER' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity Back handling must release the player route" >&2
  exit 1
fi

if ! grep -q 'TYPE_TEXT_VARIATION_PASSWORD' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must mask the password input" >&2
  exit 1
fi

if ! grep -q 'TYPE_TEXT_VARIATION_URI' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must optimize server URL input" >&2
  exit 1
fi

for ui_text in "连接服务器" "登录" "首页" "播放" "继续" "清除已保存登录" "退出登录"; do
  if ! grep -q "$ui_text" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
    echo "MainActivity is missing TV UI text: $ui_text" >&2
    exit 1
  fi
done

if ! grep -q 'TvWorkflowController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/CinePilotRuntime.kt"; then
  echo "CinePilotRuntime must expose TvWorkflowController" >&2
  exit 1
fi

if ! grep -q 'Settings.Secure.ANDROID_ID' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/CinePilotRuntime.kt"; then
  echo "CinePilotRuntime must use a stable Android device id for session scope" >&2
  exit 1
fi

if ! grep -q 'recent_accounts' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must remember multiple recent accounts" >&2
  exit 1
fi

if ! grep -q 'restoreSession' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must expose saved session restore" >&2
  exit 1
fi

if ! grep -q 'focusItem' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must expose TV focus updates" >&2
  exit 1
fi

if ! grep -q 'logout' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must expose logout" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PublicUserSummary.java" ]]; then
  echo "Missing public user summary model" >&2
  exit 1
fi

if ! grep -q 'loadPublicUsers' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must expose public user loading" >&2
  exit 1
fi

if ! grep -q 'restoreSession' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must wire saved session restore" >&2
  exit 1
fi

if ! grep -q 'requestFocus' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must restore focused home item" >&2
  exit 1
fi

if ! grep -q 'openFirstChild' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must support opening the first child of a folder" >&2
  exit 1
fi

if ! grep -q 'openFolder' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must open folders as browsable rows" >&2
  exit 1
fi

if ! grep -q '返回上级' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must let users return from folder browsing" >&2
  exit 1
fi

if ! grep -q '打开子项目' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must expose a child browse action in Chinese" >&2
  exit 1
fi

if ! grep -q 'forgetAuthenticatedSession' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must clear expired authenticated sessions" >&2
  exit 1
fi

if ! grep -q '会话已过期，请重新登录' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must explain expired sessions in Chinese" >&2
  exit 1
fi

if ! grep -q 'workflowController.fail' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must preserve workflow state for recoverable errors" >&2
  exit 1
fi

if ! grep -q '返回详情' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must let users recover from detail-scoped errors" >&2
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

if ! grep -q 'onPlaybackParametersChanged' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlaybackBridge.kt"; then
  echo "Media3PlaybackBridge must report playback speed changes" >&2
  exit 1
fi

if ! grep -q 'PlaybackSessionController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must create PlaybackSessionController" >&2
  exit 1
fi

if ! grep -q 'postDelayed' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must periodically tick playback progress" >&2
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

if ! grep -q 'defaultStreamIndex' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlaybackSourceSelector.java"; then
  echo "PlaybackSourceSelector must preserve default media stream indexes" >&2
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

if ! grep -q '正在打开详情' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must load media details through the background task path" >&2
  exit 1
fi

if ! grep -q '继续播放' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must expose resume playback for resumable items" >&2
  exit 1
fi

if ! grep -q '从头播放' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must expose start-over playback for resumable items" >&2
  exit 1
fi

if ! grep -q '选择用户' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must expose public users on the login screen" >&2
  exit 1
fi

if ! grep -q '没有可用播放源' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must explain unsupported playback in Chinese" >&2
  exit 1
fi

if ! grep -q '目录中没有可打开的媒体' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must explain empty folders in Chinese" >&2
  exit 1
fi

rm -rf "$BUILD_DIR"
mkdir -p "$MAIN_CLASSES" "$TEST_CLASSES"

javac --release 17 -d "$MAIN_CLASSES" $(find "$ROOT_DIR/core/src/main/java" -name '*.java' | sort)
javac --release 17 -cp "$MAIN_CLASSES" -d "$TEST_CLASSES" $(find "$ROOT_DIR/core/src/test/java" -name '*.java' | sort)
java -cp "$MAIN_CLASSES:$TEST_CLASSES" tv.cinepilot.core.protocol.ProtocolCoreTest
java -cp "$MAIN_CLASSES:$TEST_CLASSES" tv.cinepilot.core.tv.TvWorkflowTest
java -cp "$MAIN_CLASSES:$TEST_CLASSES" tv.cinepilot.core.protocol.HttpTransportIntegrationTest

if [[ -x "$ROOT_DIR/gradlew" ]]; then
  "$ROOT_DIR/gradlew" -q :core:test
  if [[ -n "${ANDROID_HOME:-}" || -f "$ROOT_DIR/local.properties" ]]; then
    "$ROOT_DIR/gradlew" -q :app:assembleDebug
  fi
fi

echo "check passed"
