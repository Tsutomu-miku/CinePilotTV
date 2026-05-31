#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$ROOT_DIR/build/check"
MAIN_CLASSES="$BUILD_DIR/main"
TEST_CLASSES="$BUILD_DIR/test"
PLAYBACK_ROUTE_CONTROLLER="$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackRouteController.kt"
AUTH_ROUTE_CONTROLLER="$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthRouteController.kt"

required_docs=(
  "$ROOT_DIR/README.md"
  "$ROOT_DIR/docs/REQUIREMENTS.md"
  "$ROOT_DIR/docs/ROADMAP.md"
  "$ROOT_DIR/docs/PROJECT_SPEC.md"
  "$ROOT_DIR/docs/ARCHITECTURE.md"
  "$ROOT_DIR/docs/CODE_STRUCTURE.md"
  "$ROOT_DIR/docs/DESIGN_NOTES.md"
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

if ! grep -q 'androidx.media3:media3-exoplayer-hls' "$ROOT_DIR/gradle/libs.versions.toml"; then
  echo "Version catalog must include Media3 HLS playback support" >&2
  exit 1
fi

if ! grep -q 'media3.exoplayer.hls' "$ROOT_DIR/app/build.gradle.kts"; then
  echo "Android app must depend on Media3 HLS module for Jellyfin transcode streams" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/core/build.gradle.kts" ]]; then
  echo "Missing core Gradle module build file" >&2
  exit 1
fi

if grep -R -q 'readAllBytes()' "$ROOT_DIR/core/src/main/java" "$ROOT_DIR/app/src/main/java"; then
  echo "Android runtime code must not call InputStream.readAllBytes()" >&2
  exit 1
fi

if grep -R -q 'URLEncoder.encode([^,]*,[^)]*StandardCharsets' "$ROOT_DIR/core/src/main/java" "$ROOT_DIR/app/src/main/java"; then
  echo "Android runtime code must not call URLEncoder.encode with Charset" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/UrlEncoding.java" ]]; then
  echo "Missing Android-compatible URL encoding helper" >&2
  exit 1
fi

if grep -R -qE '\b(List|Map|Set)\.of\(|\b(List|Map|Set)\.copyOf\(|\.toList\(' "$ROOT_DIR/core/src/main/java"; then
  echo "Android runtime Java code must not use Java 9+ collection factories or Stream.toList()" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/AndroidCollections.java" ]]; then
  echo "Missing Android-compatible collection helper" >&2
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

if ! grep -q 'ANDROID_SERIAL' "$ROOT_DIR/scripts/install-debug-apk.sh"; then
  echo "APK install script must support selecting a target device" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/.github/workflows/android-apk.yml" ]]; then
  echo "Missing Android APK GitHub Actions workflow" >&2
  exit 1
fi

if ! grep -q ':app:assembleDebug' "$ROOT_DIR/.github/workflows/android-apk.yml"; then
  echo "Android APK workflow must build the debug APK" >&2
  exit 1
fi

if ! grep -q 'actions/upload-artifact' "$ROOT_DIR/.github/workflows/android-apk.yml"; then
  echo "Android APK workflow must upload the APK artifact" >&2
  exit 1
fi

if ! grep -q 'cinepilot-tv-debug-apk' "$ROOT_DIR/.github/workflows/android-apk.yml"; then
  echo "Android APK workflow must publish the expected artifact name" >&2
  exit 1
fi

if ! grep -q 'sdk.dir=' "$ROOT_DIR/.github/workflows/android-apk.yml"; then
  echo "Android APK workflow must write local.properties with the CI SDK path" >&2
  exit 1
fi

if ! grep -q -- '--sdk_root="$ANDROID_HOME"' "$ROOT_DIR/.github/workflows/android-apk.yml"; then
  echo "Android APK workflow must install SDK packages into ANDROID_HOME" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/CinePilotRuntime.kt" ]]; then
  echo "Missing Android app runtime entry" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/CinePilotViewModel.kt" ]]; then
  echo "Missing Android ViewModel entry" >&2
  exit 1
fi

if ! grep -q 'CinePilotRuntime.create' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/CinePilotViewModel.kt"; then
  echo "CinePilotViewModel must initialize CinePilotRuntime" >&2
  exit 1
fi

if ! grep -q 'ViewModelProvider' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must obtain CinePilotViewModel through ViewModelProvider" >&2
  exit 1
fi

if ! grep -q 'workflowController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must drive TvWorkflowController" >&2
  exit 1
fi

if ! grep -q 'OnBackPressedCallback' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must handle Android TV Back navigation through OnBackPressedCallback" >&2
  exit 1
fi

if ! grep -q 'TvRoute.PLAYER' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity Back handling must release the player route" >&2
  exit 1
fi

if ! grep -q 'TYPE_TEXT_VARIATION_PASSWORD' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Auth screens must mask the password input" >&2
  exit 1
fi

if ! grep -q 'TYPE_TEXT_VARIATION_URI' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Auth screens must optimize server URL input" >&2
  exit 1
fi

if ! grep -q 'http://192.168.1.10:8096' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Server entry placeholder should match common local Jellyfin/Emby HTTP addresses" >&2
  exit 1
fi

if ! grep -q 'candidate = "http://"' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/MediaServerAddress.java"; then
  echo "Bare server addresses must default to local HTTP" >&2
  exit 1
fi

for auth_text in "连接服务器" "登录" "继续" "清除已保存登录"; do
  if ! grep -q "$auth_text" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
    echo "Auth screens are missing TV UI text: $auth_text" >&2
    exit 1
  fi
done

if ! grep -q "首页" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/HomeScreen.kt"; then
  echo "Home screen is missing TV UI text: 首页" >&2
  exit 1
fi

for playback_text in "播放" "退出登录"; do
  if ! grep -R -q "$playback_text" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv"; then
    echo "TV UI is missing text: $playback_text" >&2
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

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/PrimaryImageLoader.kt" ]]; then
  echo "Missing dedicated primary image loader" >&2
  exit 1
fi

if ! grep -q 'connectTimeout = 3_000' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/PrimaryImageLoader.kt"; then
  echo "PrimaryImageLoader must keep poster loading on short timeouts" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/RecentAccountStore.kt" ]]; then
  echo "Missing dedicated recent account store" >&2
  exit 1
fi

if ! grep -q 'recent_accounts' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/RecentAccountStore.kt"; then
  echo "RecentAccountStore must remember multiple recent accounts" >&2
  exit 1
fi

if ! grep -q 'recent_servers' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/RecentAccountStore.kt"; then
  echo "RecentAccountStore must remember recently connected servers" >&2
  exit 1
fi

if [[ ! -s "$AUTH_ROUTE_CONTROLLER" ]]; then
  echo "Missing dedicated auth route controller" >&2
  exit 1
fi

if ! grep -q 'RecentAccountStore' "$AUTH_ROUTE_CONTROLLER"; then
  echo "Auth route controller must delegate saved account handling to RecentAccountStore" >&2
  exit 1
fi

if ! grep -F -q '服务器 ${server.displayName()}' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Server entry must expose recently connected servers" >&2
  exit 1
fi

if ! grep -q 'restoreRecentAccountOnLaunch' "$AUTH_ROUTE_CONTROLLER"; then
  echo "Auth route controller must auto-restore the most recent saved account on launch" >&2
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

if ! grep -q 'restoreSession' "$AUTH_ROUTE_CONTROLLER"; then
  echo "Auth route controller must wire saved session restore" >&2
  exit 1
fi

if ! grep -q 'requestFocus' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must restore focused home item" >&2
  exit 1
fi

if ! grep -R -q 'setOnFocusChangeListener' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui"; then
  echo "TV UI helpers must make D-pad focus visibly change controls" >&2
  exit 1
fi

if ! grep -q 'openFirstChild' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must support opening the first child of a folder" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/BrowseSession.java" ]]; then
  echo "Missing dedicated TV browse session state helper" >&2
  exit 1
fi

if grep -q 'ArrayDeque\|FolderBrowseContext' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must delegate temporary browse state to BrowseSession" >&2
  exit 1
fi

if ! grep -q 'openFolder' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must open folders as browsable rows" >&2
  exit 1
fi

if ! grep -q 'workflowController.back()' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "Android TV Back handling must let users return from folder browsing" >&2
  exit 1
fi

if ! grep -R -q '下一页' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv"; then
  echo "Android TV UI must expose folder pagination" >&2
  exit 1
fi

if ! grep -R -q '搜索媒体' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv"; then
  echo "Android TV UI must expose media search" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/home/HomeRouteScreens.kt" ]]; then
  echo "Missing dedicated home route screen module" >&2
  exit 1
fi

if ! grep -q 'IME_ACTION_SEARCH' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/home/HomeRouteScreens.kt"; then
  echo "Search input must submit from the TV keyboard search action" >&2
  exit 1
fi

if ! grep -R -q 'homeRows().all' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv"; then
  echo "Android TV UI must show an empty state for empty search results" >&2
  exit 1
fi

if ! grep -q 'canPageForwardInBrowse' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must support folder pagination" >&2
  exit 1
fi

if ! grep -q 'SearchTerm' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/ItemQuery.java"; then
  echo "ItemQuery must support server-side search terms" >&2
  exit 1
fi

if ! grep -q 'sortBy("SortName")' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/ItemQuery.java"; then
  echo "ItemQuery browse must use a stable default sort" >&2
  exit 1
fi

if ! grep -q '打开子项目' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must expose a child browse action in Chinese" >&2
  exit 1
fi

if ! grep -R -q '剧情简介' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv"; then
  echo "Android TV UI must show media overview on details when available" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvUi.kt" ]]; then
  echo "Missing reusable TV UI helper module" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvDesign.kt" ]]; then
  echo "Missing reusable TV design token module" >&2
  exit 1
fi

for token in TvColors TvSpacing TvType TvSize TvRadius; do
  if ! grep -q "object $token" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvDesign.kt"; then
    echo "TV design tokens must define $token" >&2
    exit 1
  fi
done

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/MediaShelf.kt" ]]; then
  echo "Missing reusable media shelf UI module" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/HomeScreen.kt" ]]; then
  echo "Missing reusable home screen UI module" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/DetailsScreen.kt" ]]; then
  echo "Missing reusable details screen UI module" >&2
  exit 1
fi

if ! grep -q 'detailTitle' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/DetailsScreen.kt"; then
  echo "Details screen must isolate long media titles from the page header" >&2
  exit 1
fi

if ! grep -q 'maxLines = 3' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/DetailsScreen.kt"; then
  echo "Details title must clamp long titles to protect metadata and actions" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/MediaTechnicalInfo.kt" ]]; then
  echo "Missing reusable media technical info formatter" >&2
  exit 1
fi

for technical_field in width height channels videoRangeType sizeBytes; do
  if ! grep -R -q "$technical_field" "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol"; then
    echo "Core protocol models must expose media technical field: $technical_field" >&2
    exit 1
  fi
done

for technical_label in HDR Dolby 字幕 声道; do
  if ! grep -q "$technical_label" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/MediaTechnicalInfo.kt"; then
    echo "Media technical info formatter must expose label: $technical_label" >&2
    exit 1
  fi
done

for technical_label in 默认音轨 默认字幕 外挂字幕 强制字幕; do
  if ! grep -q "$technical_label" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/MediaTechnicalInfo.kt"; then
    echo "Media technical info formatter must expose track detail: $technical_label" >&2
    exit 1
  fi
done

for icon in search refresh logout play back subtitles speed; do
  if [[ ! -s "$ROOT_DIR/app/src/main/res/drawable/ic_${icon}.xml" ]]; then
    echo "Missing TV action icon: ic_${icon}.xml" >&2
    exit 1
  fi
done

if ! grep -q 'enum class TvIcon' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvUi.kt"; then
  echo "TV UI helpers must centralize action icons" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt" ]]; then
  echo "Missing dedicated details route screen module" >&2
  exit 1
fi

if ! grep -q 'addPosterIfAvailable' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must render media posters when available" >&2
  exit 1
fi

if ! grep -q 'primaryImageUrl' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/MediaBrowserClient.java"; then
  echo "MediaBrowserClient must expose primary image URLs" >&2
  exit 1
fi

if ! grep -q 'runCatching' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/PrimaryImageLoader.kt"; then
  echo "PrimaryImageLoader must keep image loading failures non-blocking" >&2
  exit 1
fi

if ! grep -q 'Executors.newFixedThreadPool' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/PrimaryImageLoader.kt"; then
  echo "PrimaryImageLoader must load images outside the workflow executor" >&2
  exit 1
fi

if ! grep -q 'connectTimeout' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/PrimaryImageLoader.kt"; then
  echo "PrimaryImageLoader must bound poster connection time" >&2
  exit 1
fi

if ! grep -R -q 'durationLabel' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/DetailsScreen.kt"; then
  echo "Android TV UI must show localized runtime on details when available" >&2
  exit 1
fi

if ! grep -q 'episodeLabel' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/PlaybackText.kt"; then
  echo "PlaybackText must show episode season context on details when available" >&2
  exit 1
fi

if ! grep -q 'Genres' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/MediaBrowserResponseMapper.java"; then
  echo "MediaBrowserResponseMapper must map media genres" >&2
  exit 1
fi

if ! grep -q 'forgetAuthenticatedSession' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must clear expired authenticated sessions" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt" ]]; then
  echo "Missing dedicated TV error message formatter" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/error/ErrorRouteScreen.kt" ]]; then
  echo "Missing dedicated error recovery route screen" >&2
  exit 1
fi

if ! grep -q 'errorRouteScreen' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must delegate error recovery UI to ErrorRouteScreen" >&2
  exit 1
fi

if ! grep -q '会话已过期，请重新登录' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain expired sessions in Chinese" >&2
  exit 1
fi

if ! grep -q 'workflowController.fail' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must preserve workflow state for recoverable errors" >&2
  exit 1
fi

if ! grep -q '返回详情' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/error/ErrorRouteScreen.kt"; then
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

if ! grep -q 'onPlayerError' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlaybackBridge.kt"; then
  echo "Media3PlaybackBridge must report Media3 playback failures" >&2
  exit 1
fi

if ! grep -q 'PlaybackSessionController' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must create PlaybackSessionController" >&2
  exit 1
fi

if ! grep -q 'startTimeTicks' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlayableMedia.java"; then
  echo "PlayableMedia must preserve playback start ticks" >&2
  exit 1
fi

if ! grep -q 'initialPlayerPositionMillis' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must seek direct playback to the resume start position" >&2
  exit 1
fi

if ! grep -q 'SubtitleOffset' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlaybackReport.java"; then
  echo "PlaybackReport must include subtitle offset in playback check-ins" >&2
  exit 1
fi

if ! grep -q 'subtitleOffsetChanged' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlaybackSessionController.java"; then
  echo "PlaybackSessionController must expose subtitle offset changes" >&2
  exit 1
fi

if ! grep -q 'runCatching { bridge?.stop' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must tolerate stopped check-in failures during release" >&2
  exit 1
fi

if ! grep -q 'checkInExecutor' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must send playback check-ins away from the Android main thread" >&2
  exit 1
fi

if ! grep -q 'postDelayed' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must periodically tick playback progress" >&2
  exit 1
fi

if ! grep -q 'onPlaybackEnded' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlaybackBridge.kt"; then
  echo "Media3PlaybackBridge must notify the Activity when playback naturally ends" >&2
  exit 1
fi

if ! grep -q 'Media3PlayerHost' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must initialize Media3PlayerHost" >&2
  exit 1
fi

if ! grep -q 'qa_server' "$AUTH_ROUTE_CONTROLLER"; then
  echo "Auth route controller must keep a debug-only QA login intent for restricted ADB input devices" >&2
  exit 1
fi

if ! grep -q 'FLAG_DEBUGGABLE' "$AUTH_ROUTE_CONTROLLER"; then
  echo "QA login intent must be limited to debuggable builds" >&2
  exit 1
fi

if [[ ! -x "$ROOT_DIR/scripts/qa-login.sh" ]]; then
  echo "Missing executable QA login helper" >&2
  exit 1
fi

if ! grep -q 'playerView.requestFocus' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must focus the Media3 player view" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/PlayerScreen.kt" ]]; then
  echo "Missing reusable fullscreen player screen UI module" >&2
  exit 1
fi

if ! grep -q 'FrameLayout.LayoutParams.MATCH_PARENT' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/PlayerScreen.kt"; then
  echo "PlayerScreen must render the Media3 player as a fullscreen surface" >&2
  exit 1
fi

if grep -q 'iconAction\|action(' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/PlayerScreen.kt"; then
  echo "PlayerScreen must not add a second app-level playback control layer" >&2
  exit 1
fi

for host_action in seekBack seekForward togglePlayPause handleRemoteKey KEYCODE_MEDIA_PLAY_PAUSE KEYCODE_MEDIA_REWIND KEYCODE_MEDIA_FAST_FORWARD; do
  if ! grep -q "$host_action" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
    echo "Media3PlayerHost must wire remote playback control: $host_action" >&2
    exit 1
  fi
done

for in_player_track_control in 'setShowSubtitleButton(true)' KEYCODE_CAPTIONS KEYCODE_SETTINGS; do
  if ! grep -q "$in_player_track_control" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
    echo "Media3PlayerHost must expose Media3 in-player audio/subtitle controls: $in_player_track_control" >&2
    exit 1
  fi
done

if grep -q 'screen("播放器")' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "Player route must not be embedded inside the scrolling document screen" >&2
  exit 1
fi

if [[ ! -s "$PLAYBACK_ROUTE_CONTROLLER" ]]; then
  echo "Missing dedicated playback route controller" >&2
  exit 1
fi

if ! grep -q 'handlePlaybackBackPressed' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must guard Back before exiting playback" >&2
  exit 1
fi

if ! grep -q '再次按返回退出播放' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback exit guard must use a Chinese toast prompt" >&2
  exit 1
fi

if ! grep -q 'Toast.makeText' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback exit guard must use Toast instead of replacing the player screen" >&2
  exit 1
fi

if grep -q 'AlertDialog.Builder\|screen("退出播放' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback exit guard must not show a blocking dialog or replace the player screen" >&2
  exit 1
fi

if grep -R -q 'metaLine(item)' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv"; then
  echo "Details screen must not expose raw protocol type/year metadata" >&2
  exit 1
fi

if ! grep -q 'usesCleartextTraffic="true"' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must allow cleartext traffic for local Jellyfin/Emby HTTP servers" >&2
  exit 1
fi

if ! grep -q 'android.software.leanback' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must declare leanback support" >&2
  exit 1
fi

if ! grep -q 'android:required="false"' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must keep leanback optional for phone sideloads" >&2
  exit 1
fi

if ! grep -q 'android.hardware.touchscreen' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must declare touchscreen as optional for TV devices" >&2
  exit 1
fi

if ! grep -q 'android.intent.category.LAUNCHER' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must expose a phone launcher entry" >&2
  exit 1
fi

if ! grep -q 'android.intent.category.LEANBACK_LAUNCHER' "$ROOT_DIR/app/src/main/AndroidManifest.xml"; then
  echo "AndroidManifest must expose a TV launcher entry" >&2
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

if ! grep -q 'candidateSources' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlaybackSourceSelector.java"; then
  echo "PlaybackSourceSelector must honor explicit media source preferences" >&2
  exit 1
fi

if ! grep -q 'staticVideoStream' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlaybackSourceSelector.java"; then
  echo "PlaybackSourceSelector must use static direct streams before HLS fallback when direct play has no URL" >&2
  exit 1
fi

if ! grep -q '打开播放器' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt"; then
  echo "Playback screens must expose a player launch action" >&2
  exit 1
fi

if ! grep -q '播放地址已准备' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt"; then
  echo "Playback screens must avoid showing raw playback URLs on the player-ready screen" >&2
  exit 1
fi

if ! grep -q 'showPlayer(workflowController.state())' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must enter the Media3 player directly after preparing playback" >&2
  exit 1
fi

if ! grep -q '诊断信息' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt"; then
  echo "Playback screens must expose diagnostics" >&2
  exit 1
fi

if ! grep -q '导出诊断' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt"; then
  echo "Playback screens must export diagnostics" >&2
  exit 1
fi

if ! grep -q '分享诊断' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt"; then
  echo "Playback screens must expose diagnostics sharing" >&2
  exit 1
fi

if ! grep -q 'errorMessage=' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvDiagnostics.java"; then
  echo "TV diagnostics must include the recoverable error message" >&2
  exit 1
fi

if grep -q 'accessToken\|Token=' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvDiagnostics.java"; then
  echo "TV diagnostics must not expose access tokens" >&2
  exit 1
fi

if ! grep -q 'showDiagnosticsFromError' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must expose diagnostics from playback errors" >&2
  exit 1
fi

if ! grep -q '返回错误页' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback diagnostics must return to the error recovery page when opened from an error" >&2
  exit 1
fi

if ! grep -q 'showDiagnosticsFromError' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "Error recovery page must expose playback diagnostics" >&2
  exit 1
fi

if ! grep -q 'retryLowBitrateFromError' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must support low bitrate retry from playback errors" >&2
  exit 1
fi

if ! grep -q '低码率重试' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/error/ErrorRouteScreen.kt"; then
  echo "Error recovery page must expose low bitrate retry" >&2
  exit 1
fi

if ! grep -q 'showPlaybackOptionsFromError' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must expose track selection from playback errors" >&2
  exit 1
fi

if ! grep -q '切换音轨 / 字幕' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/error/ErrorRouteScreen.kt"; then
  echo "Error recovery page must expose audio and subtitle switching" >&2
  exit 1
fi

if ! grep -q 'Intent.ACTION_SEND' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Diagnostics sharing must use a text share intent" >&2
  exit 1
fi

if ! grep -q 'cinepilot-diagnostics.txt' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must use a stable diagnostics export file name" >&2
  exit 1
fi

if ! grep -q '正在打开详情' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must load media details through the background task path" >&2
  exit 1
fi

if ! grep -q '继续播放' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must expose resume playback for resumable items" >&2
  exit 1
fi

if ! grep -q '从头播放' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must expose start-over playback for resumable items" >&2
  exit 1
fi

if grep -q 'ticks 继续播放' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/MainActivity.kt"; then
  echo "MainActivity must not show raw protocol ticks in the details UI" >&2
  exit 1
fi

if ! grep -q 'formatPlaybackPosition' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/PlaybackText.kt"; then
  echo "PlaybackText must format resume playback position for users" >&2
  exit 1
fi

if ! grep -q '低码率播放' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must expose a low bitrate playback action" >&2
  exit 1
fi

if ! grep -q 'onPlaybackSpeed' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must route playback speed selection" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt" ]]; then
  echo "Missing dedicated playback screens module" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackPreferences.kt" ]]; then
  echo "Missing dedicated playback preference formatter" >&2
  exit 1
fi

if ! grep -q '播放速度' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/playback/PlaybackScreens.kt"; then
  echo "Playback screens must expose playback speed selection" >&2
  exit 1
fi

if ! grep -q 'setPlaybackSpeed' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must apply selected playback speed" >&2
  exit 1
fi

if ! grep -q 'KEYCODE_DPAD_LEFT' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must support D-pad left/right seek shortcuts" >&2
  exit 1
fi

if ! grep -q 'detailTrackControls' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must expose inline audio and subtitle selection" >&2
  exit 1
fi

if ! grep -q 'consumeUp = trackControls == null' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/DetailsScreen.kt"; then
  echo "Details playback actions must allow D-pad up/down around inline track controls" >&2
  exit 1
fi

if ! grep -q 'consumeDown = trackControls == null' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/DetailsScreen.kt"; then
  echo "Details playback actions must allow D-pad up/down around inline track controls" >&2
  exit 1
fi

if ! grep -q 'radioChoice' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailTrackControls.kt"; then
  echo "Details track controls must use single-choice radio controls" >&2
  exit 1
fi

if ! grep -q '本剧下一集' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailsRouteScreen.kt"; then
  echo "Details screen must expose a series next-up action when SeriesId is available" >&2
  exit 1
fi

if ! grep -R -q 'SeriesId' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol"; then
  echo "Core protocol must preserve SeriesId for next-up episode actions" >&2
  exit 1
fi

if ! grep -q 'nextUpForSelectedSeries' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must load next up for the selected series" >&2
  exit 1
fi

if ! grep -q 'applyTrackSelection' "$PLAYBACK_ROUTE_CONTROLLER"; then
  echo "Playback route controller must merge detail track selection into playback preferences" >&2
  exit 1
fi

if ! grep -q 'subtitleStreamIndex = -1' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/details/DetailTrackControls.kt"; then
  echo "Details track controls must support disabling subtitles" >&2
  exit 1
fi

if ! grep -q 'subtitleMethod("Hls")' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlaybackSourceSelector.java"; then
  echo "PlaybackSourceSelector must request HLS subtitle delivery when a subtitle is selected" >&2
  exit 1
fi

if ! grep -q 'subtitleDeliveryUrl' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/PlayableMedia.java"; then
  echo "PlayableMedia must preserve selected external subtitle delivery URLs" >&2
  exit 1
fi

if ! grep -q 'SubtitleConfiguration' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/player/Media3PlayerHost.kt"; then
  echo "Media3PlayerHost must attach selected external subtitles for direct playback" >&2
  exit 1
fi

if ! grep -q 'loadPlaybackChoices' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must expose playback choices for track selection" >&2
  exit 1
fi

if ! grep -q 'maxStreamingBitrate' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must forward playback bitrate preferences" >&2
  exit 1
fi

if ! grep -q 'builder.mediaSourceId' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/tv/TvWorkflowController.java"; then
  echo "TvWorkflowController must forward playback media source preferences" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt" ]]; then
  echo "Missing dedicated auth screens module" >&2
  exit 1
fi

if ! grep -q '选择用户' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Auth screens must expose public users on the login screen" >&2
  exit 1
fi

if ! grep -q '免密码登录' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Auth screens must expose passwordless public-user login" >&2
  exit 1
fi

if ! grep -q 'Quick Connect' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/auth/AuthScreens.kt"; then
  echo "Auth screens must expose Jellyfin Quick Connect login" >&2
  exit 1
fi

if [[ ! -s "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/QuickConnectPoller.kt" ]]; then
  echo "Missing dedicated Quick Connect poller" >&2
  exit 1
fi

if ! grep -q 'quickConnectPoller.start' "$AUTH_ROUTE_CONTROLLER"; then
  echo "Auth route controller must start Quick Connect authorization polling" >&2
  exit 1
fi

if ! grep -q 'QUICK_CONNECT_NOT_APPROVED_MESSAGE' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/runtime/QuickConnectPoller.kt"; then
  echo "QuickConnectPoller must continue polling until authorization is approved" >&2
  exit 1
fi

if ! grep -q 'updateQuickConnectWaiting' "$AUTH_ROUTE_CONTROLLER"; then
  echo "Auth route controller must show visible Quick Connect polling status" >&2
  exit 1
fi

if ! grep -q 'AuthenticateWithQuickConnect' "$ROOT_DIR/core/src/main/java/tv/cinepilot/core/protocol/MediaBrowserRequests.java"; then
  echo "MediaBrowserRequests must model Quick Connect authentication" >&2
  exit 1
fi

if ! grep -q '没有可用播放源' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain unsupported playback in Chinese" >&2
  exit 1
fi

if ! grep -q '播放器无法打开媒体' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain Media3 playback failures in Chinese" >&2
  exit 1
fi

for playback_error_hint in 网络 超时 编码 DRM; do
  if ! grep -q "$playback_error_hint" "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
    echo "TV error messages must classify Media3 playback failure hint: $playback_error_hint" >&2
    exit 1
  fi
done

if ! grep -q '目录中没有可打开的媒体' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain empty folders in Chinese" >&2
  exit 1
fi

if ! grep -q '无法连接到服务器' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain connection failures in Chinese" >&2
  exit 1
fi

if ! grep -q 'HTTPS 连接失败' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain HTTPS failures in Chinese" >&2
  exit 1
fi

if ! grep -q '服务器请求失败，HTTP' "$ROOT_DIR/app/src/main/java/tv/cinepilot/tv/ui/TvErrorMessages.kt"; then
  echo "TV error messages must explain HTTP failures in Chinese" >&2
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
    APK_FILE="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
    AAPT_BIN="${AAPT:-}"
    if [[ -z "$AAPT_BIN" && -n "${ANDROID_HOME:-}" ]]; then
      AAPT_BIN="$(find "$ANDROID_HOME/build-tools" -name aapt -type f 2>/dev/null | sort -V | tail -n 1)"
    fi
    if [[ -z "$AAPT_BIN" && -f "$ROOT_DIR/local.properties" ]]; then
      SDK_DIR="$(sed -n 's/^sdk.dir=//p' "$ROOT_DIR/local.properties" | tail -n 1)"
      if [[ -n "$SDK_DIR" ]]; then
        AAPT_BIN="$(find "$SDK_DIR/build-tools" -name aapt -type f 2>/dev/null | sort -V | tail -n 1)"
      fi
    fi
    if [[ -x "$AAPT_BIN" ]]; then
      APK_BADGING="$("$AAPT_BIN" dump badging "$APK_FILE")"
      if ! grep -q '^launchable-activity:' <<<"$APK_BADGING"; then
        echo "Debug APK must expose a phone launcher activity" >&2
        exit 1
      fi
      if ! grep -q '^leanback-launchable-activity:' <<<"$APK_BADGING"; then
        echo "Debug APK must expose a TV launcher activity" >&2
        exit 1
      fi
      if ! grep -q "uses-feature-not-required: name='android.software.leanback'" <<<"$APK_BADGING"; then
        echo "Debug APK must keep leanback optional" >&2
        exit 1
      fi
      if ! grep -q "uses-feature-not-required: name='android.hardware.touchscreen'" <<<"$APK_BADGING"; then
        echo "Debug APK must keep touchscreen optional" >&2
        exit 1
      fi
    else
      echo "aapt not found; skipped APK badging validation" >&2
    fi
  fi
fi

echo "check passed"
