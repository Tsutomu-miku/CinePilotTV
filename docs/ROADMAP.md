# 路线图与进度

## 目的

本文档把产品目标拆成可审查、可执行的优先级工作。每个批次都应该在通过验证后再进入下一层体验完善。

## 产品不变量

- 服务器身份是规范化 URL 加已发现的 server id。
- token 绝不能跨服务器或跨用户共享。
- 媒体项身份是 server id 加 item id，不是标题或图片 URL。
- 发给服务器的播放位置永远使用 ticks，不使用毫秒。
- 协议兼容性属于适配器，不属于界面组件。

## 优先级

- P0：核心语义、协议安全、运行可行性和数据归属。
- P1：日常 TV 体验完整度、可访问性和播放细节。
- P2：扩展能力、离线便利功能、高级服务器特性和更多平台能力。

## 执行策略

按优先级推进 P0 小批次，并保持 `./scripts/check.sh` 通过。如果代码暴露出边界问题，优先修正边界，而不是在各处添加零散兼容分支。

当前阶段优先做用户能在 TV 上立刻感知的体验改进：入口是否好找、焦点是否稳定、页面信息密度是否舒服、常用操作是否少按几次。底层兼容性、codec profile 等能力先降级为“遇到真实问题或用户明确需要时再做”，避免把精力继续花在用户暂时看不见的协议细节上。

当前产品范围锁定为 Jellyfin / Emby 点播播放器：登录、浏览媒体库、详情决策、字幕 / 音轨选择、播放闭环和 TV 遥控体验。Live TV 不进入当前产品路线图；后续 review roadmap 时，除非用户明确恢复该方向，否则不新增直播播放、频道表、EPG、录制、直播转码、直播时移或 `LiveTv` / `openLiveStream` 协议任务。

恢复 Live TV 方向的条件：

- 点播主路径已经能在目标 TV 上稳定完成登录、浏览、详情选择、播放、暂停、seek、退出和状态恢复。
- 用户明确需要频道 / EPG / 录制 / 时移中的某一类真实场景。
- 能拿到至少一个可联调的 Jellyfin 或 Emby Live TV 源，并补充独立验收标准。

## 当前执行批次

### 批次 1：P0 协议基础

状态：已完成。

- P0-1 规范化服务器地址，并保留路径前缀。进度：完成。
- P0-2 表示 Jellyfin / Emby 服务器类型，同时不把类型判断泄漏到 UI。进度：完成。
- P0-3 根据带作用域的会话身份生成认证请求头。进度：完成。
- P0-4 建模播放 check-in endpoint 和载荷语义。进度：完成。
- P0-5 把播放时间从毫秒转换为媒体浏览协议 ticks。进度：完成。

退出标准：

- `./scripts/check.sh` 通过。
- 协议测试覆盖地址规范化、请求头字段、endpoint 和 ticks 转换。
- 文档说明 token、媒体项、服务器和播放会话的身份规则。

### 批次 2：P0 Android TV 外壳

状态：进行中。

- P0-6 本机具备 Android SDK 后启用 Gradle Android 构建。进度：`:core` 与 `:app` 已接入 Gradle 多模块工程，当前环境通过本地临时 SDK 验证 `:app:assembleDebug`。
- P0-7 添加可启动的 TV Activity，并建立 D-pad 安全的导航脚手架。进度：TV workflow 状态 / 导航 reducer、Android runtime composition root、原生 UI、显式焦点样式、遥控器 Back 导航和播放器遥控器媒体键已完成；真机已验证首页、详情、播放器、Back 返回详情和 force-stop 后恢复。
- P0-8 添加 Media3 播放器宿主生命周期和占位播放状态接线。进度：`Media3PlayerHost` 生命周期、release、播放 URL 授权、progress ticker、播放错误回传、release 上报容错、硬件媒体键播放 / 暂停 / 快退 / 快进已完成；真机已验证 Jellyfin 播放启动、全屏播放器、Back 返回详情和无 fatal 播放错误。

退出标准：

- Android 构建命令已记录，并且在具备 Android SDK 的机器上通过。
- 应用能在 Android TV 模拟器或设备上启动到服务器选择页。
- 手工 QA 覆盖焦点移动和返回行为。

### 批次 3：P0 服务器登录与浏览

状态：进行中。

- P0-9 通过直接 URL 实现服务器发现。进度：请求规格、系统信息响应 mapper、HTTP transport 和 client 编排已完成。
- P0-10 实现 Jellyfin 与 Emby 的用户名 / 密码认证。进度：请求规格、public users 请求 / mapper、登录响应 mapper、Quick Connect 请求规格 / mapper、HTTP transport、client 编排、TV 登录页 public users 选择入口、免密码 public user 一键登录、Jellyfin Quick Connect 入口和授权状态自动轮询已完成。
- P0-11 按服务器和用户作用域持久化会话。进度：内存 repository、文件 repository、client 保存 / 恢复 / logout / 本地忘记撤销流程、Android 稳定 device id、APK version name 进入 client identity、Android 最近登录列表、最近服务器地址、启动自动恢复最近账号和 TV 退出登录入口已完成；401 过期会话会清除当前 scope 并回到重新登录。
- P0-12 通过协议适配器获取首页分区和媒体详情。进度：views、items、resume、next up、latest、detail、image URL、search 请求规格、响应模型、简介 / 类型 / 图片 metadata、client 编排、TV 首页 row 组合、稳定默认排序、不可播放文件夹分页浏览、上一页 / 下一页、上级返回、打开首个子项目、服务器侧搜索、空文件夹错误提示和本地 HTTP 集成测试已完成。

退出标准：

- token 失效时只影响对应服务器并触发重新认证。进度：`MediaBrowserClient.forget` 和 Android 401 处理已完成，本地验证覆盖当前 scope 撤销不影响其他服务器。
- 浏览页能打开一个可播放媒体项详情。
- 会话持久化测试覆盖多服务器场景。

### 批次 4：P0 播放信息与播放闭环

状态：进行中。

- P0-13 获取播放信息，并保存 media source id 与 play session id。进度：playback info 请求规格、领域模型、source name / path / bitrate 响应 mapper、client 编排、resume / 从头播放 start ticks、最大码率、媒体源、音轨、字幕和声道偏好转发已完成。
- P0-14 生成 Media3 可播放 media item，包括 direct stream / transcode URL。进度：播放源选择器、显式 media source id 固定选择、默认音轨 / 字幕 index 保留、低码率 HLS 偏好、HLS 字幕交付偏好、direct play / direct stream 外挂字幕 `DeliveryUrl` 附加、无可播放源错误提示、Media3 URL 接线和播放 URL token 授权已完成。
- P0-15 调度 started / progress / stopped 播放 check-in。进度：check-in 请求规格、10 秒进度调度器、client 网络发送接线、本地 HTTP 集成测试和 Media3 宿主周期 tick 接线已完成。
- P0-16 把播放暂停、seek、音轨、字幕和播放速度变化同步到协议层。进度：立即上报事件调度、client 发送、默认音轨 / 字幕 index、播放前音轨 / 字幕 / 播放速度选择、`PlaybackSessionController`、Media3 ready / pause / unpause / seek / ended / release / playback speed 桥接、字幕偏移字段上报、播放中音轨 / 字幕变化的唯一映射上报已完成；无法唯一映射 Jellyfin / Emby `MediaStream.Index` 的 Media3 track 不会上报，避免错误同步。

### 批次 5：P0 TV UI 与 Media3 集成

状态：进行中。

- P0-17 用 Android TV UI 渲染服务器输入、登录、首页、详情和播放器入口。进度：原生 View 流程已完成，启动自动恢复、服务器 URL 输入、public users 选择、密码遮蔽、显式焦点样式、服务器连接、登录、会话恢复、首页加载、搜索、文件夹 / 剧集层级浏览、详情加载、海报、中文元信息、简介 / 类型 / 时长 / 季集展示、可读恢复播放时间、继续播放、从头播放、低码率播放、播放速度、详情页内联媒体源 / 音轨 / 字幕 OptionSelect、播放中 Media3 原生音轨 / 字幕控制层入口、连接 / HTTP / 地址错误中文提示、上下文错误恢复、Back 导航、全屏播放器和遥控器播放控制均走已接线流程；仍需继续做 TV 视觉细节打磨。
- P0-18 将 `TvWorkflow` 接入 Activity / ViewModel，保证焦点按 item id 恢复。进度：`TvWorkflow`、`HomeRowsLoader`、核心 `TvWorkflowController`、Android runtime 暴露、`CinePilotViewModel` 接线和首页 item id 焦点恢复已完成。
- P0-19 将 `PlayableMedia` 接入 Media3 player host。进度：`Media3PlayerHost` 已接入全屏播放器视图，为播放 URL 追加 `api_key`，并在播放失败时回到可恢复错误页；真机已验证 Jellyfin 媒体可播放、停止后返回详情。
- P0-20 将 Media3 播放事件桥接到 `PlaybackSessionController`。进度：`Media3PlaybackBridge` 已接入 player host，ready / pause / unpause / seek / ended / release / playback speed / playback error 均有桥接路径；播放自然结束后会释放播放器并回详情；本地 HTTP 集成测试覆盖上报，真机已验证播放、硬件媒体键 smoke 和停止返回不崩溃。

退出标准：

- Android 构建在具备 Android SDK 的机器上通过。当前环境已用临时 SDK 验证 `:core:test` 和 `:app:assembleDebug` 通过。
- TV UI 可以用 D-pad 完成服务器输入、登录、浏览、详情、播放入口。
- 播放开始、暂停、seek、停止能触发协议上报。

退出标准：

- 播放请求规格和播放上报载荷都有 JVM 测试。
- Media3 宿主可以从一个协议播放候选创建播放源。
- 手工 QA 记录至少覆盖一个 Jellyfin 或 Emby 服务器上的播放开始、暂停、seek、停止和恢复。

## P1 候选

- Jellyfin Quick Connect 已具备登录入口、授权状态自动轮询和可见等待状态。
- public-user 登录界面已具备最小入口、passwordless 一键确认、“需要密码 / 免密码登录”状态标识，以及基于 `PrimaryImageTag` 的用户头像展示。
- 继续观看、下一集、最新媒体行、剧集 / 季 / 集浏览、分页和稳定默认排序已具备最小入口。
- 详情页技术信息展示：已从 playback info / media stream metadata 整理分辨率、容器 / 编码、文件大小、码率、声道、HDR、Dolby Vision、Dolby Atmos、音轨数量、默认音轨、字幕数量、字幕语言、字幕格式、默认字幕、外挂字幕、强制字幕，以及 HEVC / AV1 / 杜比视界、高码率、高清音频和图形字幕的兼容性 / 转码风险提示，作为播放按钮之后的次级信息展示；后续继续根据真机样式微调密度。
- 详情页长标题排版：已把媒体标题从通用页面大标题移到详情内容区，最多三行截断，首行元信息和播放按钮在标题下方布局，避免遮挡；后续真机校准不同电视缩放比例。
- 播放前媒体源、字幕和音轨选择已放在详情页 OptionSelect 里：页面展示当前值摘要，按确认打开 popover 列表；用户可以在同一媒体源下选择默认 / 指定音轨、服务器默认 / 关闭 / 指定字幕，确保 audio stream index 与 subtitle stream index 可以组合后一起进入播放准备；选中 PGS、DVD subtitle 或 VobSub 图形字幕时会在转码播放准备中请求服务器烧录字幕。播放中可通过 Media3 原生控制层调整音轨 / 字幕；当 selected track 的 label、language 和 codec 能唯一匹配当前媒体源 stream metadata 时，会把 Jellyfin / Emby `MediaStream.Index` 同步到播放上报，匹配不唯一时跳过。
- 显式选择的字幕如果无法作为外部 `DeliveryUrl` 附加给 Media3，或服务端标记为 HLS / 烧录交付，播放源选择会改走服务器 HLS / transcode；当 playback info 已返回 `TranscodingUrl` 时优先复用并最小覆盖字幕参数，避免 direct play 路径丢字幕或让播放器 CC 图标无可选轨道。
- 恢复播放弹窗和下一集行为；详情页已提供继续播放、从头播放、低码率播放，以及基于 `SeriesId` / `/Shows/NextUp` 的“本剧下一集”入口。
- 播放器防误触：Back 退出播放前需要二次确认，确认层也不能形成第二套播放控制。
- 无法连接服务器、不支持媒体、token 过期和上下文错误恢复已具备最小入口。
- Media3 播放失败会释放播放器并显示中文恢复建议，已细分网络 / HTTP、超时、设备解码或格式不支持、DRM / 受保护内容和 HTTP 明文限制。
- 设备端诊断信息已具备最小快照、不含 token 的文件导出、系统文件分享、文本 fallback 和 Android `MediaCodecList` codec 能力快照；Android 运行时会把基础 codec 能力转换为 playback info `DeviceProfile` 参与播放候选协商；错误消息里的播放 URL token、access token query 和 token header 会统一脱敏。

## 当前 UI 优先批次

状态：当前优先推进。每完成一个可独立验证的小体验点，都应本地验证并单独 commit / push。

路线复盘结论：上一轮“Infuse 化”失败不是颜色问题，而是结构仍然像 Android 表单 / 卡片页。近期不再扩协议面，也不启动 Live TV；优先把点播主路径重做成 Infuse 近似复刻的信息架构。

本轮 UI 最高优先级：

- 首页推倒重做为 media wall：无应用名大标题、无“媒体库 / 选择媒体”说明、无 poster card；默认焦点落在第一个 artwork cell。
- 详情页推倒重做为 cinematic detail：poster + 标题 metadata + 主播放动作 + compact selector；技术信息只做 micro badges。
- 搜索结果复用 media wall；设置、错误、播放器信息改为 side sheet / HUD，不能再成为居中大卡片。
- 继续保留 Media3 播放核心、字幕 / 音轨语义、直连 / 转码调试能力，但这些只能作为次级或调试信息出现。

### UI-1 Media wall 基础

- 新建主路径组件：`CinematicStage`、`EdgeChrome`、`ArtworkCell`、`MediaWallRow`、`FocusOutline`、`CompactSelector`、`SideSheet`。
- 旧 `Infuse*` 组件只允许迁移期保留，不作为首页 / 详情主结构。
- 1080p 密度目标：poster `108-116dp x 162-174dp`，gap `8dp`，row title `13-14sp`，首屏至少两行完整 row 并露出第三行。
- `scripts/check.sh` 防回退：禁止首页大标题文案，禁止主路径依赖 `infusePanelScreen`，禁止 poster 主组件继续叫 card。

### UI-2 首页重写

- 首页入口直接展示 media wall；顶部只保留极轻 icon rail。
- 焦点切换更新背景 artwork 和低矮焦点摘要，不再使用占屏 hero。
- 空状态和分页仍要有恢复动作，但必须是轻量 overlay，不影响正常媒体墙结构。

### UI-3 详情页重写

- 首屏固定为 backdrop 背景、poster、标题、metadata、播放主动作、少量次级动作。
- 媒体源、音轨、字幕继续使用 OptionSelect 语义，但视觉上改为 compact selector row。
- 剧情简介和完整技术信息后移；首屏只展示观看决策需要的信息。

### UI-4 辅助页与播放器

- 搜索页使用 compact search bar + filter chips + media wall results。
- 设置页和错误页使用 side sheet；主动作明确，说明文字短。
- 播放器视频信息 HUD 改为 compact side sheet，跟随控制层显示，不常驻遮挡画面。

## 暂缓能力池

这些能力不进入近期批次，除非用户明确要求或真机/服务器联调暴露问题。

- 单服务器多 profile。
- Live TV 支持：暂不考虑，包括直播频道、EPG、录制、时移、直播转码策略、直播播放进度语义、`/LiveTv/*` endpoint 和 `openLiveStream` 路径。
- `DeviceProfile` 精细映射：基础 codec profile 已参与 playback info POST 协商；profile / level / HDR / container 条件矩阵等底层兼容性优化，等遇到具体播放问题再补。
- 更细的转码策略、码率策略和设备兼容矩阵。
