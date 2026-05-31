# 架构说明

## 运行时边界

`app` 负责 Android TV 进程集成、生命周期、焦点处理和 Media3 播放器宿主。

`core` 负责媒体浏览协议身份、endpoint 构造、播放上报语义和纯产品规则。

Gradle 工程必须保持 `:app` 依赖 `:core`。Android UI 和播放器层只能通过 `:core` 的公开类型使用协议能力，不能复制请求构造或响应解析逻辑。

Android app 的运行时入口是 `CinePilotRuntime`。它负责创建客户端身份、HTTP transport、文件 session repository、`MediaBrowserClient`、`TvWorkflowController` 和初始 `TvAppState`。

`MainActivity` 当前使用 Android 原生 View 渲染最小 TV 流程：服务器输入、登录、首页、详情和播放准备。界面事件必须通过 `TvWorkflowController` 推进状态。

`TvDiagnostics` 生成不含 token 的联调快照，供 TV UI 展示 server、user、item、media source、play method 和焦点信息。

`Media3PlayerHost` 负责把 `TvAppState.playableMedia` 转换为 Media3 `MediaItem`，创建 `ExoPlayer` 和 `PlayerView`，并在 Activity 销毁或用户停止播放时释放播放器。

`Media3PlaybackBridge` 监听 Media3 player 状态并调用 `PlaybackSessionController`，把 ready、pause、unpause、seek、ended 和 release 转换为服务器播放上报。

Android manifest 允许 cleartext traffic，因为家庭 Jellyfin / Emby 服务器常见地址是 `http://host:8096`。Media3 播放 URL 由 `PlaybackUrlAuthorizer` 追加 `api_key`，避免播放器脱离 `HttpTransport` 后丢失认证。

后续的 `platform` 适配器会负责 HTTP transport、token 持久化、图片加载和 Android 专用存储。

UI 界面应该调用应用控制器或 store。界面组件不能直接构造 Jellyfin / Emby URL、认证请求头或播放上报载荷。

## 协议层

协议层负责：

- 服务器地址规范化。
- 服务器类型表示。
- 认证请求头构造。
- 登录、发现、用户、媒体库和播放相关请求规格。
- 播放 endpoint 路径。
- Jellyfin 与 Emby 共享的播放载荷字段。
- 播放位置 ticks 转换。

这个边界上的缺陷应该通过协议类型和测试修复，而不是靠某个界面里的特殊分支掩盖。

请求规格只表达 method、path、headers、query 和 body。真实网络发送、重试、超时、TLS 证书策略和响应解析属于后续 platform adapter。

基础 JSON 值解析和核心响应 mapper 暂时放在 `core`，用于把系统信息、登录结果和 playback info 转换成领域模型。后续引入 Kotlin/Android JSON 库时，必须保持这些领域模型和测试语义不变。

`MediaBrowserClient` 是协议核心的编排入口，负责发现服务器、登录、恢复会话、获取 playback info、选择播放源和登出。真实网络由 `HttpTransport` 提供；默认实现是基于 `HttpURLConnection` 的 `UrlConnectionHttpTransport`，可在 JVM 和 Android 上使用。

媒体库浏览响应映射到 `MediaItemPage` 和 `MediaItemSummary`。UI 必须使用这些领域模型里的 `id` 保持焦点和选择身份，而不是用标题或列表位置。

TV 状态流由 `core.tv.TvWorkflow` 建模。Android UI 应渲染 `TvAppState`，并把遥控操作转换成 workflow 输入；焦点恢复必须使用 `FocusedItem(rowId, itemId)`。

首页内容由 `HomeRowsLoader` 组合：用户媒体库、继续观看和每个媒体库的最新内容都转换成 `HomeRow`。Android UI 不应直接调多个媒体库 API 来拼首页。

`TvWorkflowController` 是 Android ViewModel 应调用的核心用例入口，负责服务器发现、登录、首页加载、详情打开和播放准备。它保持 `TvAppState`，并把协议 client 的结果转换为 workflow 状态。

`TvWorkflowController.restoreSession(userId)` 要求先完成服务器发现，再按已发现服务器、userId、客户端身份和设备身份恢复会话。Android UI 可以记住上次服务器地址和 userId 来提供“继续”入口，但不能自行保存或拼接 token。

## 播放层

播放层将负责 Media3 player 设置、media source 创建、轨道选择、字幕处理和播放 check-in 调度。它把领域播放事件报告给协议层，不关心界面如何渲染。

播放信息请求、HLS URL 构造和播放 check-in 请求规格属于 `core`；Media3 只消费已经选出的播放 URL 和轨道选择结果。

播放源选择规则在 `core` 中执行：优先 direct play，其次 direct stream，最后 transcode。相对 URL 必须按服务器基础地址解析；缺少可用 URL 但支持转码时，由 HLS 请求规格补齐。

播放 check-in 调度由 `PlaybackCheckInScheduler` 建模：开始播放立即发 started，常规进度约每 10 秒发 progress，暂停、seek、轨道变化等事件立即发 progress，停止播放发 stopped。调度器只产生领域事件；`MediaBrowserClient.sendPlaybackCheckIn` 负责把事件转换成协议请求并通过 transport 发送。

`PlaybackSessionController` 组合 `PlayableMedia`、`PlaybackCheckInScheduler` 和 `MediaBrowserClient`，为 Media3 事件桥接提供单一入口。Media3 层应调用它的 start、progressIfDue、pause、seek、audioTrackChanged、subtitleTrackChanged、playbackRateChanged 和 stop 方法。

## 状态与持久化

会话状态按服务器和用户身份划分作用域。持久化记录必须包含足够身份信息，避免用户修改 URL 或切换多服务器后把 token 发给错误服务器。

当前 `InMemorySessionRepository` 用于领域验证和早期集成，`FileSessionRepository` 提供 JVM 可用的落盘实现。Android 可用版本可以复用文件实现或包一层平台存储路径，但必须保持相同 `SessionScope` 规则。

Android Activity 只用 SharedPreferences 保存上次登录提示信息：服务器地址、服务器显示名和 userId。真正的访问 token 仍由 `FileSessionRepository` 存在 app 私有文件中，并在恢复时经过 `MediaBrowserClient.restore` 读取。

## 验证策略

- 协议和领域行为在 JVM 上测试。
- Android instrumentation 只用于生命周期、焦点和 Media3 这类很难便宜地在 JVM 上覆盖的行为。
- TV 遥控器行为、模拟器播放和服务器兼容矩阵保留手工 QA 记录。
