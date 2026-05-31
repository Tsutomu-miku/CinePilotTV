# 验证说明

## 当前可运行检查

本仓库当前默认健康检查是：

```bash
./scripts/check.sh
```

`check.sh` 是项目级快速健康检查，不是 Android 开发唯一或标准的验证方式。它的作用是把本项目当前最容易回归的约束集中挡住：中文文档、GitHub Actions APK 产物、TV 入口、关键播放链路、协议测试和少量 UI 约定。真正的 Android 验证仍然应该分层执行 Gradle 单测、lint、APK 构建、仪器测试和真机 / TV QA；后续新增能力应优先沉淀为测试或 lint 规则，`check.sh` 只负责串联这些检查和保留少量仓库结构护栏。

它会验证：

- 中文项目文档存在。
- `docs/DESIGN_NOTES.md` 记录 TV 详情页、首页和播放器的设计原则，避免后续样式继续暴露协议字段或偏离遥控器使用场景。
- Gradle 多模块结构包含 `:app` 和 `:core`。
- Android app 依赖 `:core`。
- Android ViewModel、runtime、TV workflow controller、最小 TV UI、Media3 player host 和 Media3 playback bridge 入口存在。
- Android manifest 允许 HTTP 明文流量，同时提供普通桌面 `LAUNCHER` 和 TV `LEANBACK_LAUNCHER` 入口，并把 leanback / touchscreen 声明为非必需 feature；Media3 player host 会授权播放 URL。
- Media3 播放错误会回到中文错误页，播放器释放时 stopped 上报失败不会阻止错误恢复。
- debug APK 提供受 `FLAG_DEBUGGABLE` 限制的 QA 登录 intent，配套 `./scripts/qa-login.sh`，用于禁止 adb input 注入的设备。
- `core` 的协议、媒体库、播放、session 和 TV workflow JVM 测试通过。

## Android 构建检查

当前机器没有全局 `ANDROID_HOME`，但仓库存在本地 `local.properties` 指向临时 SDK，因此可以完成 Android debug 构建。本线程已经用 Xiaomi 24129PN74C 真机完成 Jellyfin 登录恢复、浏览、详情、全屏播放、遥控器播放控制 smoke 和 Back 返回详情验证。

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
- 测试 Jellyfin `http://192.168.31.82:49156` 从开发机可达，`/System/Info/Public` 返回 Jellyfin Server 10.10.7，测试账号认证成功；真机 app 已完成登录恢复、首页浏览、继续观看进入详情和 Media3 播放验证。
- 登录成功后执行系统关闭 / force-stop，再从普通 launcher 入口启动，已验证自动恢复最近账号并进入首页，不要求重新输入服务器和账号。
- 详情页已验证显示中文元信息，例如“单集”“第 1 季 / 第 2 集”“约 25 分钟”，不再暴露 `EPISODE` 这类协议枚举。
- 播放器已验证为黑底全屏 `PlayerView`；UI dump 只包含 Media3 `PlayerView` / `SurfaceView` / `exo_*` 控件树，没有第二套 app 级播放按钮。
- 真机通过 `adb shell input keyevent 85/90/89` smoke 测试播放 / 暂停、快进、快退硬件媒体键；系统 Back 可释放播放器并返回详情，logcat 未见 fatal、`NetworkOnMainThreadException` 或播放错误。
- 播放器 Back 退出交互已改为轻量 Toast 确认：第一次 Back 提示“再次按返回退出播放”，短时间内第二次 Back 才释放播放器并返回详情，期间不显示弹窗、不替换播放器画面。
- 真机详情页已验证“媒体信息”次级区域可展示 `1920x1080`、`H.264`、`FLAC / 立体声`、容器、大小、码率和“字幕 3 条 / 外挂 1 条”；标题和第一行中文元信息没有互相遮挡。
- 真机“音轨 / 字幕”入口已验证可以列出音轨、关闭字幕和多个字幕 stream，例如外挂中文字幕与英文字幕，并可把选择带入播放准备。

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
- 详情页元信息必须使用中文用户语言，例如“单集”“约 24 分钟”“第 1 季 / 第 5 集”，不能直接展示 `EPISODE` 这类协议枚举。
- 详情页标题过长时，第一行元信息和播放按钮不能被遮挡；技术信息应作为次级信息展示，包括分辨率、编码、大小、声道、HDR / Dolby 和字幕概览。
- 首页首屏应优先露出媒体内容；搜索输入放在独立搜索页，避免占用 TV 浏览页的主要视野。
- 搜索页在软键盘 / 遥控器键盘按“搜索”或“完成”时应直接提交搜索，不必须再移动焦点点搜索按钮。
- 搜索、刷新、退出、播放、返回、字幕和低码率播放等关键操作应显示图标，帮助遥控器用户快速识别动作。
- Jellyfin 服务器启用 Quick Connect 时，可在登录页用授权码完成登录；等待授权期间 TV 端会显示自动检查状态，授权后自动进入首页。
- 点击播放后 Media3 player 能打开可播放 URL。
- 详情页点击播放、继续播放、从头播放或低码率播放后，应在准备播放信息加载完成后直接进入播放器，不再要求用户停在中间确认页再点一次。
- 详情页“播放速度”应能以 0.75x、1.0x、1.25x、1.5x、2.0x 准备播放，并由 Media3 应用选中速度，同时播放上报保留 `PlaybackRate`。
- 播放器页面必须是黑底全屏播放 surface，遥控器焦点默认交给 Media3 PlayerView；页面不能叠加第二套 app 级播放按钮，播放 / 暂停、seek、进度条和控制显隐交给 Media3 原生控制层与遥控器媒体键，退出播放使用系统 Back。
- 播放自然结束后应释放播放器并回到详情页，让用户可以重新播放、切换音轨 / 字幕或进入“本剧下一集”。
- 播放器按 Back 时必须先在播放器上方显示 Toast 提示“再次按返回退出播放”，只有短时间内第二次 Back 才释放播放器并回到详情；不能弹出阻塞对话框，也不能把播放器页面替换成普通确认页。
- 当 Media3 因编码、转码、网络或 URL 问题播放失败时，界面会释放播放器并显示中文恢复建议，而不是停留在不可诊断的播放器页。
- 播放失败后的错误页应提供“诊断信息”入口，可导出或分享不含 token 的诊断快照，并能返回错误页继续选择恢复动作。
- 播放失败后的错误页应提供“低码率重试”入口，直接带低码率偏好重新准备播放并进入播放器，不要求用户先返回详情页。
- Media3 播放失败会根据错误类型给出更具体的中文建议，包括网络 / HTTP、超时、设备解码或格式不支持、DRM / 受保护内容和 HTTP 明文限制。
- Jellyfin / Emby 返回 HLS 转码播放候选时，APK 包含 `media3-exoplayer-hls`，打开播放器不会因缺少 `HlsMediaSource.Factory` 崩溃。
- HTTP 本地服务器地址如 `http://host:8096` 可以连接。
- 裸地址如 `192.168.1.10:8096` 会默认补成 `http://192.168.1.10:8096/`；显式 `https://` 地址不应被降级。
- 成功发现服务器后，服务器输入页会记住最近服务器地址；即使没有已恢复登录，也可以点“服务器 ……”一键回到该服务器登录流程。
- Jellyfin latest items 返回数组时，首页仍能加载最新媒体行，不会因只支持分页对象而失败。
- Jellyfin 列表条目缺少 `IsPlayable` 时，非 folder 的 Movie / Episode / Video 仍会作为可播放项进入详情，而不是被误当作目录。
- 地址格式错误、DNS 失败、连接拒绝、超时、HTTPS/证书失败和常见 HTTP 错误会显示中文操作建议。
- 播放 URL 携带 token 后，Media3 能访问受保护流。
- Jellyfin `PlaybackInfo` 声明可 direct play 但没有返回 `DirectStreamUrl` 时，播放准备应使用静态 `/Videos/{Id}/stream?Static=true` 请求，避免不必要地进入 HLS 转码路径。
- 详情页有 Primary 图片时会加载海报；图片加载使用独立线程池和短超时，失败不能影响按钮焦点或播放入口。
- 详情页能展示服务器返回的简介、类型、时长和季集信息。
- 播放准备页的诊断信息不包含 token，且能显示 server、item、media source、play method。
- 诊断页可以导出不含 token 的 `cinepilot-diagnostics.txt` 到 app 私有文件目录。
- 诊断页可以通过系统分享发送不含 token 的文本诊断快照，方便从电视设备导出排障信息。
- 详情页展示可读的恢复播放时间，不显示原始协议 ticks。
- 详情页的音轨 / 字幕入口能按 media source 分组列出 playback info 中的 media sources、audio streams 和 subtitle streams，并把选择所属的 media source id / stream index 一起用于播放准备，避免多版本影片选错源。
- 详情页必须能看见字幕能力，字幕选择必须能进入并选择具体 subtitle stream。
- 详情页的低码率播放会把最大码率、声道数、起播 ticks 等偏好传入 playback info，并在 HLS URL 构造时继续使用分辨率 / 码率偏好。
- HLS 播放准备在用户选择字幕时会带上 `SubtitleMethod=Hls`。
- direct play / direct stream 播放准备在用户选择外挂字幕且服务器返回 `DeliveryUrl` 时，会把字幕 URL 附加到 Media3 `MediaItem.SubtitleConfiguration`。
- 搜索无结果或服务器返回空媒体行时，界面会显示“没有可显示的媒体”。
- 播放开始、暂停、seek、停止会触发 Jellyfin / Emby 播放上报。
- 硬件媒体键的播放 / 暂停、快退、快进应调用 Media3 控制，保证遥控器按键和播放器原生控制行为一致。
- Media3 播放状态回调发生在主线程时，播放上报必须切到后台线程发送，不能因 `NetworkOnMainThreadException` 退出应用。
- token 失效时只影响对应服务器并回到登录。
- 多服务器 session 不串用。
