# 架构说明

## 运行时边界

`app` 负责 Android TV 进程集成、生命周期、焦点处理和 Media3 播放器宿主。

`core` 负责媒体浏览协议身份、endpoint 构造、播放上报语义和纯产品规则。

Gradle 工程必须保持 `:app` 依赖 `:core`。Android UI 和播放器层只能通过 `:core` 的公开类型使用协议能力，不能复制请求构造或响应解析逻辑。

Android app 的运行时入口是 `CinePilotRuntime`。它负责创建客户端身份、HTTP transport、文件 session repository、`MediaBrowserClient`、`TvWorkflowController` 和初始 `TvAppState`。

客户端身份的 device id 应优先使用 `Settings.Secure.ANDROID_ID`，因为 session scope 依赖 device id 来避免 token 跨设备复用。只有无法读取 Android ID 时才 fallback 到设备型号和系统 build id。

`MainActivity` 当前使用 Android 原生 View 渲染最小 TV 流程：服务器输入、登录、首页、详情和播放准备。界面事件必须通过 `TvWorkflowController` 推进状态。

Android 遥控器 Back 键必须和页面按钮使用同一套 workflow 语义：登录、首页和错误页回到服务器输入；详情回首页；播放器页先释放 Media3 player 再回详情；只有服务器输入页交给系统退出。

登录页展示服务器公开用户时，带密码用户只预填用户名并把焦点交给密码框；免密码用户可以直接走空密码登录流程，避免在 TV 遥控器上多一步无意义输入。

Jellyfin 登录页可以发起 Quick Connect：Activity 展示服务器返回的授权码，并定时调用 controller 完成状态检查；一旦授权成功，controller 用 Quick Connect secret 换取 token 并进入首页。轮询属于 Android UI 生命周期行为，Quick Connect endpoint 和 token 保存语义仍在 `core`。

服务器地址输入应使用 URI text variation，密码输入必须使用 password variation。登录界面可以保留原生 `EditText`，但不能明文显示密码。

按钮和输入框必须有显式 focus color，不能只依赖平台默认样式；这样在深色 TV 背景上 D-pad 当前焦点始终可见。

`TvDiagnostics` 生成不含 token 的联调快照，供 TV UI 展示 server、user、item、media source、play method 和焦点信息。

播放准备页只展示播放方式、媒体源和“播放地址已准备”这类安全摘要，不直接展示 raw playback URL。需要排障时走 `TvDiagnostics`，且诊断输出不能包含 token。Android 诊断导出只能写入 app 私有文件，避免无意把 token 或播放 URL 暴露给其它应用。

`Media3PlayerHost` 负责把 `TvAppState.playableMedia` 转换为 Media3 `MediaItem`，创建 `ExoPlayer` 和 `PlayerView`，并在 Activity 销毁或用户停止播放时释放播放器。

进入播放器页后，Activity 应主动把焦点交给 `PlayerView`，让遥控器播放控制优先落在 Media3，而不是页面里的停止按钮。

`Media3PlaybackBridge` 监听 Media3 player 状态并调用 `PlaybackSessionController`，把 ready、pause、unpause、seek、ended 和 release 转换为服务器播放上报。Media3 `onPlayerError` 必须回到 Android 错误页，用中文提示播放失败和可尝试的低码率 / 轨道切换 / 转码设置方向；错误页和诊断都不能展示 raw playback URL 或 token。

Android manifest 允许 cleartext traffic，因为家庭 Jellyfin / Emby 服务器常见地址是 `http://host:8096`。Media3 播放 URL 由 `PlaybackUrlAuthorizer` 追加 `api_key`，避免播放器脱离 `HttpTransport` 后丢失认证。

Manifest 必须同时暴露普通 `LAUNCHER` 和 TV `LEANBACK_LAUNCHER` 入口。这样 sideload 到手机时能在桌面 / 应用抽屉出现，安装到 Android TV 时也能出现在 TV launcher；`android.software.leanback` 和 `android.hardware.touchscreen` 都只能作为非必需 feature，避免普通 Android 设备或无触摸 TV 设备被错误排除。

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

媒体图片 URL 由 `MediaBrowserClient.primaryImageUrl` 基于 `MediaItemSummary.imageTags` 生成并追加 token；Android UI 只负责异步加载位图。图片加载必须使用独立线程池和短超时，失败时不能阻塞详情页按钮、焦点或播放流程。

TV 状态流由 `core.tv.TvWorkflow` 建模。Android UI 应渲染 `TvAppState`，并把遥控操作转换成 workflow 输入；焦点恢复必须使用 `FocusedItem(rowId, itemId)`。

Android 首页按钮获得选择意图时，应先调用 `TvWorkflowController.focusItem(rowId, itemId)` 写回领域状态；从详情或播放器返回首页时，再用 `TvAppState.focus` 对应按钮调用平台 `requestFocus()`。

首页内容由 `HomeRowsLoader` 组合：用户媒体库、继续观看、下一集和每个媒体库的最新内容都转换成 `HomeRow`。Android UI 不应直接调多个媒体库 API 来拼首页。`下一集` 使用 `/Shows/NextUp`，只在服务器返回非空结果时展示，避免没有剧集库的用户看到空行。

首页中的可播放条目打开详情和播放准备入口；不可播放的媒体库或文件夹应调用 `TvWorkflowController.openFirstChild(parentId)` 浏览子项目，避免把 collection folder 当作影片播放。

层级浏览使用 `TvWorkflowController.openFolder(parentId, title)` 把子项目渲染成一行临时 home rows，并在 controller 内维护浏览返回栈。Android Back 和“返回上级”都必须调用 `TvWorkflowController.back()`，这样焦点和 root 首页 rows 能恢复到进入文件夹前的状态。

文件夹分页由 `TvWorkflowController` 持有当前 parent、title、total count 和 start index。Android 只根据 `canPageBackwardInBrowse()` / `canPageForwardInBrowse()` 展示“上一页 / 下一页”，并调用 controller 的分页方法，不自行拼 `StartIndex` 或 `Limit`。

首页搜索使用服务器侧 `SearchTerm` 查询，并把结果渲染为临时 home row。搜索结果进入同一套返回栈，用户可以通过“返回上级”回到搜索前的首页或目录。搜索结果为空时，首页必须显示“没有可显示的媒体”，不能只留下一个空标题。

普通 browse 查询默认使用 `SortBy=SortName` 和 `SortOrder=Ascending`，让分页、焦点恢复和剧集层级浏览有稳定顺序。特殊行如最新内容可以用自己的 endpoint 和排序语义。

`TvWorkflowController` 是 Android ViewModel 应调用的核心用例入口，负责服务器发现、登录、首页加载、详情打开和播放准备。它保持 `TvAppState`，并把协议 client 的结果转换为 workflow 状态。

`CinePilotViewModel` 是 Android 层持有 `CinePilotRuntime` 的入口。`MainActivity` 只能从 ViewModel 取得 `TvWorkflowController` 和 `MediaBrowserClient`，避免 Activity 重建时重新创建业务状态和 session repository。

`TvWorkflowController.restoreSession(userId)` 要求先完成服务器发现，再按已发现服务器、userId、客户端身份和设备身份恢复会话。Android UI 可以记住最近服务器地址和 userId 列表来提供多个“继续”入口，但不能自行保存或拼接 token。

启动时 Android UI 应优先尝试恢复最近账号；恢复成功直接进入首页，恢复失败则回到服务器选择页，保证首屏不是不可操作的错误状态。

debug APK 可以通过 `qa_server` / `qa_username` / `qa_password` intent extras 触发 QA 登录，用于小米等禁止 adb input 注入的真机验证。该入口必须检查 `ApplicationInfo.FLAG_DEBUGGABLE`，非 debuggable 构建不能响应。

`TvWorkflowController.loadPublicUsers()` 只用于减少 TV 端用户名输入。public users 来自 `/Users/Public`，选择用户后 Android UI 只能预填用户名，仍必须通过 `login(username, password)` 完成正式认证并获取 token。

Android 错误页必须把地址格式、DNS、连接拒绝、超时、HTTPS/证书和常见 HTTP 状态转换成中文操作建议，不能把 Java 异常类名直接暴露给用户。

当服务器返回 401 时，Android UI 应调用 `TvWorkflowController.forgetAuthenticatedSession()` 撤销当前 authenticated scope，并提示用户重新登录。这个撤销只删除当前服务器、用户和设备身份对应的 saved session，不影响其他服务器或其他用户。

非 401 错误应调用 `TvWorkflowController.fail(message)` 保留当前上下文。错误页需要根据 `TvAppState` 提供恢复入口：有 selected item 时可回详情，有 home rows 时可回首页，有 server 时可重新登录，始终可回服务器输入。

`TvWorkflowController` 对可预期的内容边界使用稳定错误消息：空目录使用 `NO_CHILD_ITEM_MESSAGE`，无法从 playback info 选择播放源时使用 `NO_PLAYABLE_SOURCE_MESSAGE`。Android UI 负责把这些消息翻译成中文用户提示。

TV 首页必须提供退出登录入口，调用 `TvWorkflowController.logout()` 让服务器 logout endpoint 和本地 scoped session 撤销走同一条路径，并从 Android 最近登录列表移除当前服务器 / 用户。

## 播放层

播放层将负责 Media3 player 设置、media source 创建、轨道选择、字幕处理和播放 check-in 调度。它把领域播放事件报告给协议层，不关心界面如何渲染。

播放信息请求、HLS URL 构造和播放 check-in 请求规格属于 `core`；Media3 只消费已经选出的播放 URL 和轨道选择结果。

播放准备的 start ticks 由 `TvWorkflowController.preparePlayback` 决定：调用方传入 `null` 表示按媒体项 resume ticks 继续播放；传入 `PlaybackSelectionPreferences` 表示显式偏好，`startTimeTicks=0` 即从头播放。最大码率、音轨、字幕和最大声道数偏好会转发给 playback info 请求，分辨率和码率偏好也会继续用于 HLS URL 构造。Android 详情页应在有 resume 进度时同时暴露“继续播放”和“从头播放”，用可读时间展示恢复位置、媒体时长和剧集上下文，不能把协议 ticks 直接显示给用户，并提供低码率播放入口。

播放前的媒体源、音轨和字幕选择由 `TvWorkflowController.loadPlaybackChoices` 获取服务器 playback info。Android 对多个 `MediaSources` 必须展示可选媒体源，优先使用服务器返回的 source name、path 文件名、container 和 bitrate 形成可读标签；音轨 / 字幕只展示 `MediaStream.Index`、语言、标题和默认 / 强制 / 外挂标记。用户选择后通过 `PlaybackSelectionPreferences` 重新准备播放，确保 playback info 请求、播放源选择、HLS URL 和播放上报使用同一个协议 media source id / stream index。

播放源选择规则在 `core` 中执行：优先 direct play，其次 direct stream，最后 transcode。相对 URL 必须按服务器基础地址解析；缺少可用 URL 但支持转码时，由 HLS 请求规格补齐。

当 `PlaybackSelectionPreferences.mediaSourceId` 非空时，`PlaybackSourceSelector` 只能在该媒体源内选择 direct play / direct stream / transcode；如果服务器没有返回对应媒体源，必须返回无可播放源，而不能静默回退到另一个版本。

播放源选择还负责保留服务器默认 media stream index：显式用户偏好优先；没有偏好时，音轨使用服务器标记的默认音轨，缺失默认标记时退到第一个音轨；字幕只使用服务器标记的默认字幕，不自动选择任意字幕。

HLS 播放请求在用户明确选择字幕时必须同时带上 `SubtitleStreamIndex` 和 `SubtitleMethod=Hls`，让服务器把字幕按 HLS 方式交付给 Media3；用户关闭字幕时可以传 `SubtitleStreamIndex=-1`，但不能强行指定字幕交付方式。

播放 check-in 调度由 `PlaybackCheckInScheduler` 建模：开始播放立即发 started，常规进度约每 10 秒发 progress，暂停、seek、轨道变化等事件立即发 progress，停止播放发 stopped。调度器只产生领域事件；`MediaBrowserClient.sendPlaybackCheckIn` 负责把事件转换成协议请求并通过 transport 发送。

`PlaybackSessionController` 组合 `PlayableMedia`、`PlaybackCheckInScheduler` 和 `MediaBrowserClient`，为 Media3 事件桥接提供单一入口。Media3 层应调用它的 start、progressIfDue、pause、seek、audioTrackChanged、subtitleTrackChanged、playbackRateChanged 和 stop 方法。

`Media3PlayerHost` 创建播放器后需要用主线程 ticker 定期调用 `Media3PlaybackBridge.tick(currentPosition)`；真正的 10 秒节流仍由 core scheduler 控制，Android ticker 只负责给 scheduler 提供播放进度采样。

释放播放器时应尽力发送 stopped check-in，但 stopped 上报失败不能阻止播放器释放或错误页展示；真实网络失败只应影响上报，不应让用户卡在播放器页。

Media3 播放速度变化通过 `onPlaybackParametersChanged` 上报到 `PlaybackSessionController.playbackRateChanged`。音轨和字幕变化只有在能从 Media3 selected track 稳定映射回服务器 `MediaStream.Index` 时才应上报，避免把本地 track ordinal 错当协议 stream index。

## 状态与持久化

会话状态按服务器和用户身份划分作用域。持久化记录必须包含足够身份信息，避免用户修改 URL 或切换多服务器后把 token 发给错误服务器。

当前 `InMemorySessionRepository` 用于领域验证和早期集成，`FileSessionRepository` 提供 JVM 可用的落盘实现。Android 可用版本可以复用文件实现或包一层平台存储路径，但必须保持相同 `SessionScope` 规则。

Android Activity 只用 SharedPreferences 保存最近登录提示信息：服务器地址、服务器显示名和 userId。真正的访问 token 仍由 `FileSessionRepository` 存在 app 私有文件中，并在恢复时经过 `MediaBrowserClient.restore` 读取。

## 验证策略

- 协议和领域行为在 JVM 上测试。
- Android instrumentation 只用于生命周期、焦点和 Media3 这类很难便宜地在 JVM 上覆盖的行为。
- TV 遥控器行为、模拟器播放和服务器兼容矩阵保留手工 QA 记录。
