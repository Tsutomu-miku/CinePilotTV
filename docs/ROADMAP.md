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

当前阶段优先做用户能在 TV 上立刻感知的体验改进：入口是否好找、焦点是否稳定、页面信息密度是否舒服、常用操作是否少按几次。底层兼容性、codec profile 等能力先降级为"遇到真实问题或用户明确需要时再做"，避免把精力继续花在用户暂时看不见的协议细节上。

当前产品范围锁定为 Jellyfin / Emby 点播播放器：登录、浏览媒体库、详情决策、字幕 / 音轨选择、播放闭环和 TV 遥控体验。Live TV 不进入当前产品路线图；后续 review roadmap 时，除非用户明确恢复该方向，否则不新增直播播放、频道表、EPG、录制、直播转码、直播时移或 `LiveTv` / `openLiveStream` 协议任务。

恢复 Live TV 方向的条件：

- 点播主路径已经能在目标 TV 上稳定完成登录、浏览、详情选择、播放、暂停、seek、退出和状态恢复。
- 用户明确需要频道 / EPG / 录制 / 时移中的某一类真实场景。
- 能拿到至少一个可联调的 Jellyfin 或 Emby Live TV 源，并补充独立验收标准。

## 竞品定位摘要（2026-06 新增）

四维竞品对比的完整能力矩阵与遴选理由见 `docs/COMPETITIVE_MATRIX.md`。
产品差异化定位（见 `docs/PROJECT_SPEC.md`）：以 Infuse 7 的 UX 标杆补齐 Jellyfin/Emby 官方 Android TV 客户端的长期短板，零付费墙，Android TV 单平台，不进入 Live TV/多后端/Apple 生态。

## P0 已完成批次回顾

### 批次 1：P0 协议基础

状态：**已完成**（commit 7d3fdeb 及此前批次）。

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

状态：**已完成**。

- P0-6 Gradle Android 构建：`:core` 与 `:app` 已接入多模块工程，release 构建 `minifyEnabled=true shrinkResources=true` + 完整 ProGuard 规则。
- P0-7 TV Activity + D-pad 安全导航脚手架：`CinePilotRuntime` 组合根，原生 View UI，显式焦点样式（FocusOutline ValueAnimator），Back 导航与播放器媒体键。
- P0-8 Media3 播放器宿主：生命周期、release、播放 URL 授权、progress ticker、错误回传、硬件媒体键；`Media3PlaybackBridge` 桥接 ready/pause/unpause/seek/ended/release/speed/error。

退出标准：

- 具备 Android SDK 的机器 `:app:assembleRelease` 通过（当前环境已验证）。
- 应用能启动到服务器选择页或恢复首页。
- 手工 QA 覆盖焦点移动和返回行为。

### 批次 3：P0 服务器登录与浏览

状态：**已完成**。

- P0-9 服务器发现：SystemInfo 请求 + HTTP transport + 客户端编排。
- P0-10 认证：用户名/密码、public users 一键登录、Jellyfin Quick Connect（4 步 Enable/Initiate/Connect poll → AuthenticateWithQuickConnect）。
- P0-11 会话持久化：`FileSessionsRepository`（加密 scope 隔离）、401 自动回到重新登录、不影响其他服务器、device id 稳定、最近登录列表、启动自动恢复。
- P0-12 浏览：views/items/resume/next up/latest/detail/image URL/search、稳定默认排序、文件夹分页、ShowStructure（Series→Season→Episode 三层）、HomeRowsCache（磁盘缓存）+ HomeEntryFlow（缓存优先绘制 + 后台刷新）。

退出标准：

- token 失效只影响对应服务器。
- 浏览页可进入详情与播放。
- 会话与浏览的 Robolectric（12 条）+ core 单元测试覆盖。

### 批次 4：P0 播放信息与播放闭环

状态：**已完成**。

- P0-13 PlaybackInfo：请求规格、领域模型、source name/path/bitrate mapper、resume start ticks、最大码率、media source、音轨/字幕/声道偏好。
- P0-14 播放源选择器：6 层 DirectPlay → StaticDirectStream → DirectStream → TranscodingUrl → Hls fallback。显式 media source id 保留、音轨/字幕 index 保留、HLS 字幕交付偏好、PGS/VobSub 图形字幕 AlwaysBurnInWhenTranscoding 回退。
- P0-15 播放 check-in：10s 调度器、started/progress/stopped 端点、本地 HTTP 集成测试覆盖。
- P0-16 事件同步：音轨/字幕/播放速度/暂停/seek/偏移立即上报；无法唯一映射到 Jellyfin/Emby `MediaStream.Index` 的 Media3 track 跳过同步，避免错误回写。

### 批次 5：P0 TV UI 与 Media3 集成

状态：**已完成**。

- P0-17 UI 全流程：原生 View、服务器输入、public users、密码遮蔽、焦点恢复、首页加载、搜索、文件夹/剧集三层、详情加载、海报/中文元信息/简介/类型/时长/季集展示、恢复时间可读、继续/从头/低码率/速度、内联媒体源/音轨/字幕 CompactSelector、错误分类中文恢复建议、Back 导航、全屏播放器、遥控器播放控制。
- P0-18 `TvWorkflow` ↔ Activity/ViewModel：`CinePilotViewModel`、焦点按 item id 恢复。
- P0-19 `Media3PlayerHost`：全屏播放器视图、URL `api_key` 授权、播放失败跳转可恢复错误页。
- P0-20 `Media3PlaybackBridge`：ready/pause/unpause/seek/ended/release/speed/error 桥接；自然结束释放播放器返回详情。

退出标准：

- `:core:test` + `:app:assembleRelease` + `:app:lintDebug` + `:app:detektDebug` + `--verify-focus-overflow` + `--verify-hygiene` 全部通过（已验证）。
- TV UI D-pad 可完成输入/登录/浏览/详情/播放。
- 播放开始/暂停/seek/停止触发协议上报（本地集成测试覆盖）。

## P1 当前推进批次（2026-06 竞品对比后新增）

### 批次 6：P1 播放核心增强（字幕 + 剧集体验 + 播放匹配）

对应 COMPETITIVE_MATRIX A-1/2/3/4/5/9。

- P1-1 **ASS/SSA 完整渲染**：引入 LibASS（或等效渲染管线），保留字体、颜色、阴影、`\pos`/`\move`/`\an8` 标签，不得降级到 Media3 WebVTT。验收：用 10 份社区公开高复杂度 ASS 样片（特效、多位置、动画）视觉对比，样式差异率 < 5%。
- P1-2 **PGS/VobSub 图形字幕**：实现独立图形图层叠加（Bitmap-based overlay），与 Media3 视频帧 pts 同步；不得由服务器默认转码烧录，除非用户显式切换。验收：20 份 PGS 蓝光原盘字幕样本，不丢帧、不花屏、无超过 100ms 延迟。
- P1-3 **GBK/BIG5/Shift_JIS/EUC-KR 自动编码识别**：引入 juniversalchardet 或等效字符探测库，字幕文件转 UTF-8 后再渲染；编码不得成为渲染失败的理由。验收：50 份中文 GBK SRT 样本，无乱码。
- P1-4 **字幕样式存储与设置页**：`SubtitleStyleStore`（已有）扩展字号、颜色、描边、边缘、底部边距、字体；设置页 SideSheet 可调节，跨播放会话保持。
- P1-5 **Intro Skip / Credits Skip**：接入 Jellyfin 10.10 `MediaSegment` 端点与 Emby Premiere `IntroStart`/`IntroEnd`、`CreditsStart`/`CreditsEnd` 字段；OSD 左下角/右下角「跳过片头」「跳过片尾」按钮，带倒计时，可 D-pad 聚焦 + 中心确认。
- P1-6 **Next Up 自动连播 + 末 30s 下一集卡片**：播放末 30s 弹出下一集预览卡（缩略图 + 季/集号 + 标题 + 5s 倒计时）；无操作自动跳转，或聚焦立即播放 / 取消。
- P1-7 **Trickplay 章节缩略图**：接入 `/Trickplay/{itemId}/{width}/tiles.jpg` 端点，OSD scrubber hover 预览。
- P1-8 **自动帧率/色彩匹配（AFM/AFC）开关**：基于媒体 `ReferenceFrameRate` 与 HDR 类型切换 Android TV `Display.Mode`；切换前确认通知；设置页开关。
- P1-9 **章节列表 + 跳转**：从协议 chapter info 构建 OSD 章节 strip，点击跳转。

### 批次 7：P1 用户回写 + 元数据流动

对应 COMPETITIVE_MATRIX A-6/7/10。

- P1-10 **喜欢/收藏夹 / 手动标记已看**：详情页主操作区新增强喜欢切换、标记已看切换按钮；调用 `/UserItems/{itemId}/Favorite` 与 `/PlayedStatus` 端点；首页新增加「收藏夹」行。
- P1-11 **用户评分 0–10**：详情页 star selector；回传到服务器 `UserData`。
- P1-12 **ProviderIds 面板 + 手动修正**：详情页技术信息区新增 TMDb/IMDb/TVDb micro badge，点击系统浏览器跳转；支持用户手动修正 ProviderId（立即触发 UI 元数据刷新）。
- P1-13 **演职员详情跳转**：点击 cast member 卡片进入同影人参与的媒体项列表（调用 `People/{personId}/Items`），实现 Infuse 式元数据流动。
- P1-14 **TV Show 详情页强化**：Season 详情「已看/未看」分区、「尚未开始的季」折叠 rail；Series 详情「继续观看 + 下一集 + 按季浏览」。

### 批次 8：P1 浏览增强 + 首页发现

对应 COMPETITIVE_MATRIX A-8。

- P1-15 **筛选 chips**：首页 / 搜索页按类型 / 年份 / 评分 / 未看 / 4K·HDR / 收藏夹 / 已看；筛选状态跨返回保留。
- P1-16 **Collections / Box Sets 行**：基于 `TmdbCollectionId` 与服务器端 Collection 项目，首页新增「精选合集」rail；详情页新增「同系列其他」rail。
- P1-17 **全剧总览视图**：Series 详情页继续观看 / 下一集优先；季折叠 rail。

## P2 候选能力池（涌现驱动，暂不排期）

- P2-1 **离线缓存管理**：下载队列、存储配额、下载质量配置、离线可浏览 + 播放路径切换。
- P2-2 **Trakt.tv 同步**：scrobble / 收藏 / 评分 / watchlist，依赖 P1-10/11 基础能力。
- P2-3 **单服务器多 profile 用户切换**：独立 profile token scope、独立焦点恢复、独立 Continue Watching / Next Up。
- P2-4 **服务器 Smart Collections / 自定义首页行**：服务器规则化动态行读入。
- P2-5 **Android TV Home Channels**：系统主屏 Next Up / Continue Watching channel。
- P2-6 **Google Assistant 语音搜索接入**。
- P2-7 **Chromecast 媒体路由发送**：依赖 MediaRouter。
- P2-8 **播放速度 / skip 间隔按方向可配置**。

## P1 UI 优先批次（Infuse 化 UX polish，继续推进）

状态：**当前优先推进**。每完成一个可独立验证的小体验点，都应本地验证并单独 commit / push。

路线复盘结论：上一轮"Infuse 化"失败不是颜色问题，而是结构仍然像 Android 表单 / 卡片页。近期不再扩协议面，也不启动 Live TV；优先把点播主路径重做成 Infuse 近似复刻的信息架构。

本轮 UI 最高优先级：

- 首页推倒重做为 media wall：无应用名大标题、无"媒体库 / 选择媒体"说明、无 poster card；默认焦点落在第一个 artwork cell。
- 详情页推倒重做为 cinematic detail：poster + 标题 metadata + 主播放动作 + compact selector；技术信息只做 micro badges。
- TV Show 能力模型重构：Series / Season / Episode 必须成为三类媒体层级页面，尊重 Jellyfin / Emby 的 `Series -> Season -> Episode` 结构；不能再把剧集和季退化成普通 folder browse。
- 搜索结果复用 media wall；设置、错误、播放器信息改为 side sheet / HUD，不能再成为居中大卡片。
- 继续保留 Media3 播放核心、字幕 / 音轨语义、直连 / 转码调试能力，但这些只能作为次级或调试信息出现。

### UI-1 Media wall 基础

状态：**已完成**（CinematicStage / EdgeChrome / CollectionRail / LandscapeArtworkCell / PosterArtworkCell / MediaWallRow / FocusOutline / CompactSelector / SideSheet）。

- `scripts/check.sh` 防回退：禁止首页大标题文案，禁止主路径依赖 `infusePanelScreen`，禁止 `resume` / `next-up` 回到 poster cell。

### UI-2 首页重写

状态：**进行中**。P1-15（筛选 chips）与 P1-16（Collections Box Sets 行）并入此批次 polish。

### UI-3 详情页重写

状态：**进行中**。P1-12（ProviderIds 面板）、P1-13（演职员跳转）、P1-14（TV Show 强化）并入此批次 polish。

### UI-4 辅助页与播放器

状态：**进行中**。P1-5（Intro/Credits Skip 按钮）、P1-6（Next Up 卡片）、P1-7（Trickplay 缩略图 strip）、P1-8（帧率匹配通知）并入此批次 polish。P1-1/P1-2/P1-3 的字幕渲染也直接影响 OSD 外观。

## 暂缓能力池

这些能力不进入近期批次，除非用户明确要求或真机/服务器联调暴露问题。

- 单服务器多 profile（等 P1 全部完成后再评估）。
- Live TV 支持：暂不考虑，包括直播频道、EPG、录制、时移、直播转码策略、直播播放进度语义、`/LiveTv/*` endpoint 和 `openLiveStream` 路径。
- `DeviceProfile` 精细映射：基础 codec profile 已参与 playback info POST 协商；profile / level / HDR / container 条件矩阵等底层兼容性优化，等遇到具体播放问题再补。
- 更细的转码策略、码率策略和设备兼容矩阵。
- 多服务器联邦（Library Fusion）：当前项目只有单服务器身份语义，跨服务器合并要求身份与缓存重建。
- Emby Connect PIN 登录、LDAP/SSO、Kodi 原生协议、UPnP/DLNA。
- 服务器端能力（Emby Cinema / CoverArt / Smart Screen / Cloud Sync / 硬件加速）：仅需客户端消费端点，不新增 UI。

## 质量门槛（批次入口 / 出口不变量）

任何批次的变更都必须满足：

1. `./scripts/check.sh` 通过（包含 javac 协议边界独立编译、`:core:test`、`:app:lintDebug`、`:app:detektDebug`、`--verify-focus-overflow`、`--verify-hygiene`）。
2. 新增特性对应的验收项在仓库审计 / 测试中可单独验证（Robolectric 或 core 单元测试，或明确标注仅手工 QA）。
3. UI 代码不直接依赖 `MediaItemType`，UI 代码不包含服务器路径字面量（`--verify-hygiene` 守卫）。
4. `MainActivity` 行数不超过阈值（当前 330，脚本自动判定）。
5. 新文件 / 新 import 的协议类型不得跨越 `tv.cinepilot.tv.ui` 包边界，除非是明确的 presentation adapter。
