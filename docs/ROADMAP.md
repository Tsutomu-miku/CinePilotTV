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

状态：未开始。

- P0-6 本机具备 Android SDK 后启用 Gradle Android 构建。进度：`:core` 已接入 Gradle 多模块工程，完整 Android 构建仍等待 Android SDK。
- P0-7 添加可启动的 TV Activity，并建立 D-pad 安全的导航脚手架。进度：TV workflow 状态 / 导航 reducer、Android runtime composition root、原生 UI 和遥控器 Back 导航接线已完成；真实设备焦点 QA 未完成。
- P0-8 添加 Media3 播放器宿主生命周期和占位播放状态接线。

退出标准：

- Android 构建命令已记录，并且在具备 Android SDK 的机器上通过。
- 应用能在 Android TV 模拟器或设备上启动到服务器选择页。
- 手工 QA 覆盖焦点移动和返回行为。

### 批次 3：P0 服务器登录与浏览

状态：进行中。

- P0-9 通过直接 URL 实现服务器发现。进度：请求规格、系统信息响应 mapper、HTTP transport 和 client 编排已完成。
- P0-10 实现 Jellyfin 与 Emby 的用户名 / 密码认证。进度：请求规格、public users 请求 / mapper、登录响应 mapper、HTTP transport、client 编排和 TV 登录页 public users 选择入口已完成。
- P0-11 按服务器和用户作用域持久化会话。进度：内存 repository、文件 repository、client 保存 / 恢复 / logout / 本地忘记撤销流程、Android 上次登录恢复入口和 TV 退出登录入口已完成；401 过期会话会清除当前 scope 并回到重新登录。
- P0-12 通过协议适配器获取首页分区和媒体详情。进度：views、items、resume、latest、detail 请求规格、响应模型、client 编排、TV 首页 row 组合、不可播放文件夹打开首个子项目、空文件夹错误提示和本地 HTTP 集成测试已完成。

退出标准：

- token 失效时只影响对应服务器并触发重新认证。进度：`MediaBrowserClient.forget` 和 Android 401 处理已完成，本地验证覆盖当前 scope 撤销不影响其他服务器。
- 浏览页能打开一个可播放媒体项详情。
- 会话持久化测试覆盖多服务器场景。

### 批次 4：P0 播放信息与播放闭环

状态：进行中。

- P0-13 获取播放信息，并保存 media source id 与 play session id。进度：playback info 请求规格、领域模型、响应 mapper、client 编排、resume start ticks 和从头播放 start ticks 已完成。
- P0-14 生成 Media3 可播放 media item，包括 direct stream / transcode URL。进度：播放源选择器、默认音轨 / 字幕 index 保留、无可播放源错误提示、Media3 URL 接线和播放 URL token 授权已完成。
- P0-15 调度 started / progress / stopped 播放 check-in。进度：check-in 请求规格、10 秒进度调度器、client 网络发送接线、本地 HTTP 集成测试和 Media3 宿主周期 tick 接线已完成。
- P0-16 把播放暂停、seek、音轨、字幕和播放速度变化同步到协议层。进度：立即上报事件调度、client 发送、默认音轨 / 字幕 index、`PlaybackSessionController`、Media3 ready / pause / unpause / seek / ended / release / playback speed 桥接已完成；播放中音轨和字幕变化仍需在能可靠映射 Jellyfin / Emby `MediaStream.Index` 后接入。

### 批次 5：P0 TV UI 与 Media3 集成

状态：未开始。

- P0-17 用 Android TV UI 渲染服务器输入、登录、首页、详情和播放器入口。进度：原生 View 最小流程已完成，服务器 URL 输入、public users 选择、密码遮蔽、服务器连接、登录、会话恢复、首页加载、详情加载、继续播放、从头播放、上下文错误恢复、Back 导航和播放准备均走已接线流程，尚未做最终 TV 视觉打磨。
- P0-18 将 `TvWorkflow` 接入 Activity / ViewModel，保证焦点按 item id 恢复。进度：`TvWorkflow`、`HomeRowsLoader`、核心 `TvWorkflowController`、Android runtime 暴露和首页 item id 焦点恢复已完成，正式 ViewModel 接线未开始。
- P0-19 将 `PlayableMedia` 接入 Media3 player host。进度：`Media3PlayerHost` 已接入最小播放器视图，并为播放 URL 追加 `api_key`；真实设备播放验证未完成。
- P0-20 将 Media3 播放事件桥接到 `PlaybackSessionController`。进度：`Media3PlaybackBridge` 已接入 player host，真实设备验证未完成。

退出标准：

- Android 构建在具备 Android SDK 的机器上通过。当前环境已用临时 SDK 验证 `:core:test` 和 `:app:assembleDebug` 通过。
- TV UI 可以用 D-pad 完成服务器输入、登录、浏览、详情、播放入口。
- 播放开始、暂停、seek、停止能触发协议上报。

退出标准：

- 播放请求规格和播放上报载荷都有 JVM 测试。
- Media3 宿主可以从一个协议播放候选创建播放源。
- 手工 QA 记录至少覆盖一个 Jellyfin 或 Emby 服务器上的播放开始、暂停、seek、停止和恢复。

## P1 候选

- Jellyfin Quick Connect。
- public-user 登录界面已具备最小入口，后续可补头像和 passwordless 一键确认。
- 继续观看、最新媒体行、剧集 / 季 / 集浏览。
- 字幕和音轨选择。
- 恢复播放弹窗和下一集行为；详情页已提供继续播放和从头播放的最小入口。
- 无法连接服务器、不支持媒体等错误状态继续完善；token 过期和上下文错误恢复已具备最小入口。
- 设备端诊断信息已具备最小快照，后续可扩展为导出日志。

## P2 候选

- 单服务器多 profile。
- 服务器侧搜索。
- Live TV 支持。
- 离线诊断导出。
- 更丰富的 codec 兼容性报告。
