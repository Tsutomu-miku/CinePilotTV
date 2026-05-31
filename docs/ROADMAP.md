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
- P0-7 添加可启动的 TV Activity，并建立 D-pad 安全的导航脚手架。进度：TV workflow 状态 / 导航 reducer 已完成，Android UI 渲染未开始。
- P0-8 添加 Media3 播放器宿主生命周期和占位播放状态接线。

退出标准：

- Android 构建命令已记录，并且在具备 Android SDK 的机器上通过。
- 应用能在 Android TV 模拟器或设备上启动到服务器选择页。
- 手工 QA 覆盖焦点移动和返回行为。

### 批次 3：P0 服务器登录与浏览

状态：进行中。

- P0-9 通过直接 URL 实现服务器发现。进度：请求规格、系统信息响应 mapper、HTTP transport 和 client 编排已完成。
- P0-10 实现 Jellyfin 与 Emby 的用户名 / 密码认证。进度：请求规格、登录响应 mapper、HTTP transport 和 client 编排已完成。
- P0-11 按服务器和用户作用域持久化会话。进度：内存 repository、文件 repository 和 client 保存 / 恢复 / logout 撤销流程已完成；Android 集成路径未接线。
- P0-12 通过协议适配器获取首页分区和媒体详情。进度：views、items、resume、latest、detail 请求规格、响应模型、client 编排和 TV 首页 row 组合已完成。

退出标准：

- token 失效时只影响对应服务器并触发重新认证。
- 浏览页能打开一个可播放媒体项详情。
- 会话持久化测试覆盖多服务器场景。

### 批次 4：P0 播放信息与播放闭环

状态：进行中。

- P0-13 获取播放信息，并保存 media source id 与 play session id。进度：playback info 请求规格、领域模型、响应 mapper 和 client 编排已完成。
- P0-14 生成 Media3 可播放 media item，包括 direct stream / transcode URL。进度：播放源选择器已完成，Media3 接线未开始。
- P0-15 调度 started / progress / stopped 播放 check-in。进度：check-in 请求规格、10 秒进度调度器和 client 网络发送接线已完成。
- P0-16 把播放暂停、seek、音轨、字幕和播放速度变化同步到协议层。进度：立即上报事件调度和 client 发送已建模，Media3 事件桥接未开始。

### 批次 5：P0 TV UI 与 Media3 集成

状态：未开始。

- P0-17 用 Android TV UI 渲染服务器输入、登录、首页、详情和播放器入口。
- P0-18 将 `TvWorkflow` 接入 Activity / ViewModel，保证焦点按 item id 恢复。进度：`TvWorkflow` 与 `HomeRowsLoader` 已完成，Android 接线未开始。
- P0-19 将 `PlayableMedia` 接入 Media3 player host。
- P0-20 将 Media3 播放事件桥接到 `PlaybackCheckInScheduler` 和 `MediaBrowserClient.sendPlaybackCheckIn`。

退出标准：

- Android 构建在具备 Android SDK 的机器上通过。
- TV UI 可以用 D-pad 完成服务器输入、登录、浏览、详情、播放入口。
- 播放开始、暂停、seek、停止能触发协议上报。

退出标准：

- 播放请求规格和播放上报载荷都有 JVM 测试。
- Media3 宿主可以从一个协议播放候选创建播放源。
- 手工 QA 记录至少覆盖一个 Jellyfin 或 Emby 服务器上的播放开始、暂停、seek、停止和恢复。

## P1 候选

- Jellyfin Quick Connect。
- public-user 登录界面。
- 继续观看、最新媒体行、剧集 / 季 / 集浏览。
- 字幕和音轨选择。
- 恢复播放弹窗和下一集行为。
- 无法连接服务器、不支持媒体、token 过期等错误状态。

## P2 候选

- 单服务器多 profile。
- 服务器侧搜索。
- Live TV 支持。
- 离线诊断导出。
- 更丰富的 codec 兼容性报告。
