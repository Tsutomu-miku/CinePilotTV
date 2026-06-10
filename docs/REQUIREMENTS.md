# 需求说明

## 产品意图

CinePilot TV 是一个客厅优先的 Android TV 点播播放器，服务于使用 Jellyfin 或 Emby 管理个人媒体库的用户。第一个可用工作流是：连接服务器、认证登录、浏览可播放媒体、开始播放、把播放进度回报给服务器，并在之后从同一媒体项恢复播放。

**产品定位更新（2026-06 竞品对比）**：以 Infuse 7 的 Android TV 版体验为 UX 标杆，补齐官方 Jellyfin / Emby Android TV 客户端在焦点稳定性、OSD 响应、ASS/PGS 字幕、IntroSkip、自动连播等方面的长期短板，同时保持完全零付费墙。明确不进入 Live TV / DVR、多服务器联邦、多端 Apple 平台。详见 `docs/COMPETITIVE_MATRIX.md` 与 `docs/ROADMAP.md`。

## 文档规则

- `README.md` 只放项目定位和常用命令。
- 优先级、进度和实施批次放在 `docs/ROADMAP.md`。
- 产品行为和不变量放在本文档。
- 工程方向放在 `docs/PROJECT_SPEC.md`。
- 运行时边界放在 `docs/ARCHITECTURE.md`。
- 竞品四维对比（Jellyfin 10.10 / Emby Premiere / Infuse 7 / CinePilotTV）放在 `docs/COMPETITIVE_MATRIX.md`。

## 优先级与身份规则

- 媒体服务器的身份由规范化后的基础 URL 和 `/System/Info` 返回的 server id 共同确定。
- 已保存的会话 token 必须绑定到 server id、服务器 URL、user id、客户端名、device id 和应用版本。
- 媒体项的身份是 server id 加 item id。标题、季号、集号、海报和时长都是可变元数据，不是身份。
- 播放会话的身份是 server id、user id、item id、media source id 和 play session id。
- 播放位置使用媒体浏览协议里的 ticks，1 tick 等于 100 纳秒。界面可以使用毫秒，但 API 载荷必须使用 ticks。
- Jellyfin 与 Emby 的差异必须隔离在协议适配器后面。UI 代码不应该基于服务器类型分支，除非是在展示兼容性提示。**UI 代码不得直接 import `MediaItemType`**，必须通过 MediaPresentation / DetailPresentation 适配器的 `isSeries()` / `isSeason()` / `isEpisode()` / `isFolderBrowse()` 助手函数；此规则由 `scripts/check.sh --verify-hygiene` Rule A 守卫。
- **UI 代码不得包含 `/Items`、`/Users`、`/Sessions`、`/Shows`、`/Trickplay` 等服务器路径字面量**。协议路径只允许出现在 `:core` 模块的 `MediaBrowserRequests.java`。此规则由 `scripts/check.sh --verify-hygiene` Rule B 守卫。

## 当前范围（P0 + 既有功能，已完成或进行中）

### 服务器连接

- 用户必须能输入 HTTP 或 HTTPS 服务器地址。
- 系统必须规范化尾部斜杠，同时保留 `/jellyfin` 或 `/emby` 这样的路径前缀。
- 应用必须能记住多个服务器，且不能在服务器之间共享 token。
- 应用必须检测并存储服务器类型：`jellyfin`、`emby` 或 `unknown`。

### 认证

- 用户必须能用用户名和密码登录。
- 当服务器允许时，应用应支持 Jellyfin Quick Connect（6 位码 + 自动轮询授权状态）。
- TV 登录界面可以展示 public users，但选择某个用户后仍必须认证；支持 passwordless public user 一键登录 + 用户头像（基于 `PrimaryImageTag`）。
- token 被撤销或失效时，只应让用户回到该服务器的登录界面，不能影响其他已登录服务器。

### 媒体库浏览

- 用户必须能浏览媒体库、继续观看、最新媒体、电影、剧集、季、集和可播放文件夹。
- UI 在排序、筛选、焦点移动和页面恢复时必须保留媒体项身份。
- 图片加载失败不能让某一行或某个媒体项变得不可访问（BitmapCache 双级 + 占位 fallback）。
- 详情页标题过长时不得遮挡第一行元信息；标题应换行、截断或压缩到安全区域内。
- 详情页应次级展示播放相关技术信息，包括分辨率、容器 / 编码、大小、声道、HDR / Dolby Vision / Dolby Atmos 等能力，以及可用字幕概览。
- 字幕必须支持选择；选择字幕后，播放准备和实际播放必须使用对应字幕 stream index，并在需要 HLS 字幕交付时携带协议参数。
- **剧集三层结构**：Series → Season → Episode 为独立页面，不降级为文件夹浏览。显式 media navigation stack 保证 Episode → Season → Series → 原浏览位置，不得回到登录页。
- **焦点规范**：使用线性 D-pad 加速度，禁止非线性格式大跳格。白色 halo glow + 缩放 + 150/120ms ValueAnimator。8dp FocusOverflow（负 padding）避免 cell 内容被 focus 边框裁切。row header ↔ cell 之间不得出现焦点死区。

### 播放

- 播放器必须优先 direct play，其次 direct stream / static direct stream，最后在服务器需要时 transcode。播放源选择器按 DirectPlay → StaticDirectStream → DirectStream → TranscodingUrl → Hls fallback 共 6 层优先级。
- 播放器必须使用 Android Media3 做播放能力检查和实际播放。
- 开始播放时必须调用 `/Sessions/Playing`。
- 播放进度必须约每 10 秒调用 `/Sessions/Playing/Progress`，并且在暂停、seek、切换音轨、切换清晰度、切换字幕或调整播放速度后立即上报。
- 停止播放时必须调用 `/Sessions/Playing/Stopped`。
- 音轨、字幕轨、字幕偏移、播放速度、暂停状态、是否可 seek、播放方式、media source id 和 play session id 必须一致上报。
- PGS / DVD subtitle / VobSub 图形字幕若无法作为外挂 `DeliveryUrl` 附加给 Media3，或服务端标记为 HLS 交付，播放源选择会改走服务器 HLS / transcode，并请求 `AlwaysBurnInWhenTranscoding`。
- 图形字幕、高码率（>40Mbps）、DV Profile 5、TrueHD Atmos 等兼容性 / 转码风险必须在详情页次级信息区以 micro badge 提示。

### Android TV 体验

- 应用必须能完全通过 D-pad 遥控器使用。
- 焦点必须可见、稳定，且不能困在不可播放的行里。
- 文本输入应尽量减少遥控器打字，优先使用 public users、已保存服务器和 Quick Connect。
- 启动后的首屏必须是服务器选择页，或上一次已认证用户的首页（HomeEntryFlow 缓存优先策略）。
- 播放器内按 Back 退出播放前必须二次确认，避免误操作导致播放中断。

### 错误与诊断

- 连接失败、HTTP 错误、超时、CodecList 解码失败、DRM 受保护、明文 HTTP 限制五类错误必须中文分类并显示恢复建议（含低码率重试、音轨字幕重选、查看诊断）。
- 诊断快照：基础设备信息、Media3 event 日志、当前播放项、播放方式、媒体源概览、已选音轨字幕、最后一次 `PlaybackInfo` 请求摘要、最后一次 check-in 结果。脱敏（token 隐藏）后可文本分享。

### 构建与质量门槛

- 每次变更必须通过 `./scripts/check.sh`（包含 javac 协议边界独立编译、`:core:test`、`:app:lintDebug`、`:app:detektDebug`、`--verify-focus-overflow`、`--verify-hygiene`）。
- `release` buildType 启用 `minifyEnabled=true` + `shrinkResources=true` + `proguard-rules.pro`（完整保留协议类名与 Media3 subclass）。
- Detekt 规则：`buildUponDefaultConfig=false`，仅启用 WildcardImport / ForbiddenComment / EqualsAlwaysReturnsTrueOrFalse / HasPlatformType / InvalidRange / Deprecation / UnsafeCast。
- Lint 规则：`abortOnError=true`，`fatal += NewApi`；禁用 MissingTranslation / HardcodedText / ContentDescription / UseSwitchCompatOrMaterialCode / IconDuplicatesConfig / UnsafeOptInUsageError。
- UI/protocol 边界与服务器路径字面量的两条 grep 守卫，三个 adapter 文件（MediaPresentation / HomeRowPresentation / DetailPresentation）作为明确例外。

## P1 新增需求范围（2026-06 竞品对比后规划）

> 详细优先级与批次见 `docs/ROADMAP.md` 批次 6–8 与 P1/P2 候选。

### P1 字幕增强（最优先，对应 COMPETITIVE_MATRIX A-1/2）

- ASS/SSA 字幕必须完整保留样式（字体、颜色、阴影、位置、\move \an8 \pos 标签），不得降级到 Media3 WebVTT；优先引入 LibASS 渲染或等效渲染管线。
- PGS / VobSub / DVD subtitle 作为独立图形图层叠加在画面上（Bitmap-based overlay），与 Media3 视频帧同步；分辨率必须全分辨率，不得由服务器转码烧录，除非用户明确切换。
- 必须支持 GBK / BIG5 / Shift_JIS / EUC-KR 编码的 SRT / ASS 自动探测与 UTF-8 转码，字符编码不得作为字幕渲染失败的理由。
- 字幕字体、字号、颜色、阴影、边缘描边、底部边距必须可设置，且存储在独立的 SubtitleStyleStore，跨播放会话保持。

### P1 剧集增强（对应 COMPETITIVE_MATRIX A-3/4）

- Intro Skip：接入 Jellyfin 10.10 `MediaSegment` API 与 Emby `IntroStart`/`IntroEnd` 字段。当检测到 Intro Segment 存在时，播放器 OSD 左下角出现可聚焦的「跳过片头」按钮（时长可见倒计时），D-pad 左/下可聚焦，右/中心确认跳过。
- Credits Skip：片尾检测同上，右下角「跳过片尾」按钮，不阻挡演员表用户主动观看。
- Next Up 自动连播：单集播放自然结束前 30s 右下角出现下一集卡片（缩略图、季/集号、标题、剩余时间提示）；5s 内用户无操作则自动跳转播放；用户可按方向键聚焦取消或立即播放。
- Trickplay 章节缩略图：接入 `/Trickplay/*` tile endpoint，seek OSD scrubber 下方弹出缩略图 strip 与 hover 预览。

### P1 播放增强（对应 COMPETITIVE_MATRIX A-5）

- 自动帧率 / 色彩匹配开关（设置页）。开启后，播放器根据媒体 `ReferenceFrameRate` 与 HDR 类型切换 Android TV `Display.Mode`；切换前弹出确认通知，并保存用户偏好。
- 详情页演职员扩展：点击 cast member 卡片跳转至同一影人参与的媒体项列表（调用 `Items/{itemId}/LocalTrailers` / `People/{personId}/Items`），实现 Infuse 式元数据流动。
- ProviderIds 面板：详情页技术信息区 micro badge 追加 TMDb / IMDb / TVDb 外链（点击用系统浏览器打开），并支持锁定服务器侧不匹配项用户手动修正 ProviderId（修改后立即刷新元数据）。
- 喜欢 / 收藏夹 / 手动标记已看：详情页主操作区追加「喜欢」与「标记已看」切换按钮；调用 `/UserItems/{itemId}/Favorite` 与 `/PlayedStatus` 端点。用户评分（0–10）后移到 P2。

### P1 浏览扩展（对应 COMPETITIVE_MATRIX A-8/10）

- 首页 / 搜索页筛选 chips：按类型、年份、评分、未看、4K/HDR、收藏夹、已看。筛选条件状态跨返回保留。
- Collections / Box Sets 行：基于 `TmdbCollectionId` 与服务器端 Collection 项目，首页新增「精选合集」rail，详情页「同系列其他」rail。
- 全剧总览视图：Series 详情页强化继续观看 / 下一集，并把"尚未开始的季"作为折叠 rail，未看 / 已看分区。

### P2 高级特性（非紧迫，需求涌现时推进）

- 离线下载与缓存管理（P2-1）。
- Trakt.tv 同步：scrobble、收藏、评分、watchlist（P2-2，依赖 P1 喜欢/评分基础）。
- 单服务器多 profile 切换（P2-3）。
- 服务器自定义 Smart Collections / 规则化动态行（P2-4）。
- Android TV Home Channels（Next Up / Continue Watching 系统主屏行，P2-5）。
- Google Assistant 语音搜索（P2-6）。
- Chromecast 媒体路由（P2-7）。

## 明确排除范围（Out of Scope）

- **Live TV / EPG / DVR / 时移**：ROADMAP 设定了恢复条件，在达成前不推进。
- 多服务器联邦 / Library Fusion / 跨服务器去重合并。
- Apple 平台（tvOS / iOS / macOS）、SharePlay、Handoff、Siri、Top Shelf、iCloud 同步。
- LDAP / SSO、Emby Connect PIN 登录（用户名/密码已覆盖）。
- 服务器端能力：Emby Cinema、CoverArt、Smart Screen 动态规则、Cloud Sync、硬件加速（这些是服务器端特性，客户端只消费端点）。
- CarPlay / Android Auto / PiP / AirPlay。
- Kodi 原生协议接入 / UPnP/DLNA 原生服务器（Jellyfin/Emby 后端已覆盖）。
- Emby Premiere 独占的「Smart Screens AI 推荐」与「Cloud Sync 云上传」，因为这要求用户购买 Emby 服务器许可，不符合零付费墙定位。

## 外部 API 备注

- Jellyfin 认证通常返回 access token，后续 API 调用需要带上该 token。服务器启用时，Jellyfin Quick Connect 是更适合电视端的登录方式。
- Emby 文档包含 `/Users/AuthenticateByName`、通过 `X-Emby-Token` 复用 token、通过 `/Sessions/Logout` 登出，以及用 `/System/Info` 的 server id 约束已保存 token。
- Emby 文档包含 `/Sessions/Playing`、`/Sessions/Playing/Progress`、`/Sessions/Playing/Stopped` 这些播放 check-in；CinePilot 会把这些视为共享媒体浏览协议语义，除非某个服务器专用适配器证明需要不同处理。
- Jellyfin 10.10 新增 `MediaSegment` 端点族（`/Items/{itemId}/MediaSegments`）、`/Trickplay/{itemId}/{width}/tiles.jpg`、`/Videos/{itemId}/hls1/*` HLS remux，这些是 P1 IntroSkip / Trickplay 的实现基础。
- Emby Premiere 的 `IntroStart`、`IntroEnd`、`CreditsStart`、`CreditsEnd` 作为 item-level 字段返回，在 Emby 服务器适配器中以兼容方式映射到 `MediaSegment`。
