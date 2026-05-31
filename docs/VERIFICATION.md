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
- Android runtime、TV workflow controller、最小 TV UI、Media3 player host 和 Media3 playback bridge 入口存在。
- Android manifest 允许 HTTP 明文流量，Media3 player host 会授权播放 URL。
- `core` 的协议、媒体库、播放、session 和 TV workflow JVM 测试通过。

## Android 构建检查

当前机器没有全局 `ANDROID_HOME`，但仓库存在本地 `local.properties` 指向临时 SDK，因此可以完成 Android debug 构建。本线程尚未完成模拟器启动或真实设备播放验证。

GitHub Actions：

- `.github/workflows/android-apk.yml` 会在 push、pull request 和手动触发时构建 `:core:test` 与 `:app:assembleDebug`。
- workflow 上传 `cinepilot-tv-debug-apk` artifact，内容是 `app/build/outputs/apk/debug/*.apk`，可下载安装到 Android TV 设备或模拟器。
- `./scripts/check.sh` 会检查 workflow 是否仍然构建 debug APK 并上传 artifact。

已确认：

- `./scripts/bootstrap-gradle-wrapper.sh` 可以生成 Gradle wrapper。
- `./gradlew :core:test` 可以通过，并会执行 `ProtocolCoreTest` 与 `TvWorkflowTest`。
- `HttpTransportIntegrationTest` 会启动本地 HTTP 服务，验证真实 `UrlConnectionHttpTransport` 可完成发现、登录、首页、详情、playback info 和播放进度上报。
- 使用本地临时 SDK（`local.properties` 指向 `build/android-sdk`）时，`./gradlew :app:assembleDebug` 可以通过并生成 debug APK。

最近验证记录（2026-05-31）：

- `./scripts/check.sh` 通过。
- `./scripts/install-debug-apk.sh` 可以完成 `:app:assembleDebug`。
- `./scripts/install-debug-apk.sh` 在安装阶段停止，原因是 `adb devices` 没有在线 Android TV 设备或模拟器。

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

- 连接设备后运行 `./scripts/install-debug-apk.sh` 可以安装并启动 debug APK。
- 可用 D-pad 完成服务器输入、登录、首页浏览和详情打开。
- Jellyfin 服务器启用 Quick Connect 时，可在登录页用授权码完成登录；授权后 TV 端会自动进入首页。
- 点击播放后 Media3 player 能打开可播放 URL。
- HTTP 本地服务器地址如 `http://host:8096` 可以连接。
- 播放 URL 携带 token 后，Media3 能访问受保护流。
- 详情页能展示服务器返回的简介、类型、时长和季集信息。
- 播放准备页的诊断信息不包含 token，且能显示 server、item、media source、play method。
- 详情页展示可读的恢复播放时间，不显示原始协议 ticks。
- 详情页的音轨 / 字幕入口能列出 playback info 中的 audio / subtitle streams，并把选择的 stream index 用于播放准备。
- 详情页的低码率播放会把最大码率、声道数、起播 ticks 等偏好传入 playback info，并在 HLS URL 构造时继续使用分辨率 / 码率偏好。
- 搜索无结果或服务器返回空媒体行时，界面会显示“没有可显示的媒体”。
- 播放开始、暂停、seek、停止会触发 Jellyfin / Emby 播放上报。
- token 失效时只影响对应服务器并回到登录。
- 多服务器 session 不串用。
