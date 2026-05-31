# 验证说明

## 当前可运行检查

本仓库当前默认健康检查是：

```bash
./scripts/check.sh
```

它会验证：

- 中文项目文档存在。
- Gradle 多模块结构包含 `:app` 和 `:core`。
- Android app 依赖 `:core`。
- Android ViewModel、runtime、TV workflow controller、最小 TV UI、Media3 player host 和 Media3 playback bridge 入口存在。
- Android manifest 允许 HTTP 明文流量，同时提供普通桌面 `LAUNCHER` 和 TV `LEANBACK_LAUNCHER` 入口，并把 leanback / touchscreen 声明为非必需 feature；Media3 player host 会授权播放 URL。
- Media3 播放错误会回到中文错误页，播放器释放时 stopped 上报失败不会阻止错误恢复。
- debug APK 提供受 `FLAG_DEBUGGABLE` 限制的 QA 登录 intent，配套 `./scripts/qa-login.sh`，用于禁止 adb input 注入的设备。
- `core` 的协议、媒体库、播放、session 和 TV workflow JVM 测试通过。

## Android 构建检查

当前机器没有全局 `ANDROID_HOME`，但仓库存在本地 `local.properties` 指向临时 SDK，因此可以完成 Android debug 构建。本线程尚未完成模拟器启动或真实设备播放验证。

GitHub Actions：

- `.github/workflows/android-apk.yml` 会在 push、pull request 和手动触发时构建 `:core:test` 与 `:app:assembleDebug`。
- workflow 会把 `ANDROID_HOME` / `ANDROID_SDK_ROOT` 写入环境，并生成 CI 专用 `local.properties`，确保 Android Gradle Plugin 能找到 SDK。
- workflow 上传 `cinepilot-tv-debug-apk` artifact，内容是 `app/build/outputs/apk/debug/*.apk`，可下载安装到 Android TV 设备或模拟器。
- `./scripts/check.sh` 会检查 workflow 是否仍然构建 debug APK 并上传 artifact；本地 Android SDK 可用时，还会用 `aapt dump badging` 验证 APK 同时具备手机桌面入口、TV launcher 入口，并且 leanback / touchscreen feature 不是必需项。

已确认：

- `./scripts/bootstrap-gradle-wrapper.sh` 可以生成 Gradle wrapper。
- `./gradlew :core:test` 可以通过，并会执行 `ProtocolCoreTest` 与 `TvWorkflowTest`。
- `HttpTransportIntegrationTest` 会启动本地 HTTP 服务，验证真实 `UrlConnectionHttpTransport` 可完成发现、登录、首页、详情、playback info 和播放进度上报。
- 使用本地临时 SDK（`local.properties` 指向 `build/android-sdk`）时，`./gradlew :app:assembleDebug` 可以通过并生成 debug APK。

最近验证记录（2026-05-31）：

- `./scripts/check.sh` 通过。
- `./scripts/install-debug-apk.sh` 可以完成 `:app:assembleDebug`。
- `adb devices` 已识别 Xiaomi 2211133C 真机；`./scripts/install-debug-apk.sh` 可以完成构建，但安装阶段被设备系统以 `INSTALL_FAILED_USER_RESTRICTED` 拒绝。需要在设备开发者选项中开启“通过 USB 安装”和“USB 调试（安全设置）”，并在安装确认弹窗中允许。
- Xiaomi 2211133C 手动安装成功，包名 `tv.cinepilot.tv` 同时暴露 `LAUNCHER` 和 `LEANBACK_LAUNCHER`；该设备禁止 adb `input tap/text/keyevent` 注入，因此补充 debug-only QA 登录入口用于继续真机验证。
- 新连接的 Xiaomi 24129PN74C 支持 adb input 注入，可用于常规 UI 登录 / 浏览 / 播放路径验证。
- 测试 Jellyfin `http://192.168.31.82:49156` 从开发机可达，`/System/Info/Public` 返回 Jellyfin Server 10.10.7，测试账号认证成功；真机 app 登录 / 浏览 / 播放仍需在支持输入注入的新设备上完成验证。

具备 Android SDK 后，应运行：

```bash
./gradlew :core:test :app:assembleDebug
```

如果没有 Gradle wrapper，先运行：

```bash
./scripts/bootstrap-gradle-wrapper.sh
./gradlew :core:test :app:assembleDebug
```

## 设备 QA

Android TV 设备或模拟器上需要验证：

- 连接设备后运行 `./scripts/install-debug-apk.sh` 可以安装并启动 debug APK；多台设备在线时使用 `ANDROID_SERIAL=<serial>` 指定手机、Android TV 或模拟器。
- sideload 到普通 Android 手机时，应用会出现在桌面 / 应用抽屉，而不只在应用管理里可见。
- 可用 D-pad 完成服务器输入、登录、首页浏览和详情打开。
- 首页应呈现暗色 TV 媒体架，媒体条目以横向海报卡片展示；详情页应呈现左海报、右信息与操作区，而不是调试面板式的竖排按钮列表。
- Jellyfin 服务器启用 Quick Connect 时，可在登录页用授权码完成登录；授权后 TV 端会自动进入首页。
- 点击播放后 Media3 player 能打开可播放 URL。
- 当 Media3 因编码、转码、网络或 URL 问题播放失败时，界面会释放播放器并显示中文恢复建议，而不是停留在不可诊断的播放器页。
- Jellyfin / Emby 返回 HLS 转码播放候选时，APK 包含 `media3-exoplayer-hls`，打开播放器不会因缺少 `HlsMediaSource.Factory` 崩溃。
- HTTP 本地服务器地址如 `http://host:8096` 可以连接。
- Jellyfin latest items 返回数组时，首页仍能加载最新媒体行，不会因只支持分页对象而失败。
- Jellyfin 列表条目缺少 `IsPlayable` 时，非 folder 的 Movie / Episode / Video 仍会作为可播放项进入详情，而不是被误当作目录。
- 地址格式错误、DNS 失败、连接拒绝、超时、HTTPS/证书失败和常见 HTTP 错误会显示中文操作建议。
- 播放 URL 携带 token 后，Media3 能访问受保护流。
- Jellyfin `PlaybackInfo` 声明可 direct play 但没有返回 `DirectStreamUrl` 时，播放准备应使用静态 `/Videos/{Id}/stream?Static=true` 请求，避免不必要地进入 HLS 转码路径。
- 详情页有 Primary 图片时会加载海报；图片加载使用独立线程池和短超时，失败不能影响按钮焦点或播放入口。
- 详情页能展示服务器返回的简介、类型、时长和季集信息。
- 播放准备页的诊断信息不包含 token，且能显示 server、item、media source、play method。
- 诊断页可以导出不含 token 的 `cinepilot-diagnostics.txt` 到 app 私有文件目录。
- 详情页展示可读的恢复播放时间，不显示原始协议 ticks。
- 详情页的音轨 / 字幕入口能列出 playback info 中的多个 media sources、audio streams 和 subtitle streams，并把选择的 media source id / stream index 用于播放准备。
- 详情页的低码率播放会把最大码率、声道数、起播 ticks 等偏好传入 playback info，并在 HLS URL 构造时继续使用分辨率 / 码率偏好。
- HLS 播放准备在用户选择字幕时会带上 `SubtitleMethod=Hls`。
- 搜索无结果或服务器返回空媒体行时，界面会显示“没有可显示的媒体”。
- 播放开始、暂停、seek、停止会触发 Jellyfin / Emby 播放上报。
- Media3 播放状态回调发生在主线程时，播放上报必须切到后台线程发送，不能因 `NetworkOnMainThreadException` 退出应用。
- token 失效时只影响对应服务器并回到登录。
- 多服务器 session 不串用。
