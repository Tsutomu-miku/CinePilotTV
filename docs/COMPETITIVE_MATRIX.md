# 竞品能力四维对比（Jellyfin 10.10 / Emby Premiere / Infuse 7 / CinePilotTV）

> 说明：2026-06 调研快照，**P1 批次 17/17 交付后更新**。Y = 完整支持，P = 部分支持或仅限部分平台/付费，N = 不支持或无明确路线，— = 不适用。
> 调研来源：Jellyfin 10.10.0 release notes、Emby Premiere 官方定价页、Emby 官方支持文档、Firecore Infuse 7/8 官网与支持中心、r/Jellyfin / r/Emby / r/Infuse 2024–2025 高热度讨论帖，以及本仓库代码、`docs/ROADMAP.md` 进度、commit 哈希核对。

## 1. 协议 / 登录

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| 用户名/密码登录 | Y | Y | Y | Y |
| 无密码 Public User 一键登录 | Y | Y | P（有 profile 选择） | Y |
| Quick Connect（无键盘 6 位码） | Y | N（自有 Emby Connect） | P（Jellyfin 时可用） | Y |
| Emby Connect PIN 登录 | — | Y | P（Emby 时可用） | N |
| LDAP / SSO | P（LDAP 插件） | P（Premiere） | N | N（不纳入范围） |
| API Key 请求头 `X-Emby-Token`/`Authorization` | Y | Y | Y | Y |
| 服务器路径前缀 `/emby` vs `/` | Y（规范 URL 带前缀） | Y | Y | Y |
| 多用户切换（同一服务器） | Y | Y | Y | N |

## 2. 媒体浏览 / 首页

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| 首页行：Continue Watching / Next Up / Latest | Y | Y | Y | Y |
| 规则化 Smart Screen / Smart 行 | N（手动行 + 插件） | Y（Premiere 动态规则） | Y（Smart Collections） | N |
| 跨服务器库融合 Library Fusion | — | Y（Premiere 多服务器联邦） | Y（Jellyfin+Emby+Plex 去重合并） | N |
| TV Show 三层结构（Series/Season/Episode） | Y | Y | Y | Y |
| Collections / Box Sets 行 | P（TMDb Box Sets 插件） | Y | Y（TMDb collection id） | Y[^P1-16] |
| 全局 Search | Y | Y | Y（Universal Search） | P（仅服务器侧搜索） |
| 筛选 / 排序 chips（genre/year/rating） | Y | Y（Premiere 规则更丰富） | Y | Y[^P1-15] |
| 喜欢 / 收藏夹 / 播放列表 | Y | Y | Y（含 Trakt） | P[^P1-10]（收藏夹已实现，播放列表未纳入） |

## 3. 元数据 / 图片

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| TMDb 主元数据 | Y（scraper） | Y | Y（默认主源） | P（仅透传服务器端 TMDb 元数据） |
| Fanart.tv clearlogo / disc art | P（插件） | Y（Premiere CoverArt） | Y | N |
| ProviderIds 透传（TMDB/IMDb/TVDb） | Y | Y | Y | Y[^P1-12] |
| People / 演职员 + 头像 | Y | Y | Y | Y[^P1-13] |
| 本地 NFO 覆盖 | Y | Y | Y | N（未干预） |
| 字幕抓取（OpenSubtitles 等） | P（插件） | P（Premiere） | Y（Pro） | N |

## 4. 播放 / 编解码 / 传输

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| DirectPlay 优先策略 | P（客户端各有差异） | P（官方 ATV 较弱） | Y（社区公认最佳） | Y（6 层选择器，DP 优先） |
| DirectStream / StaticDirectStream | Y | Y | Y | Y |
| 服务端转码 Transcoding（HLS/ts） | Y | Y | Y | Y |
| NVENC / QSV / VAAPI / VideoToolbox | Y | Y（Premiere 硬件加速） | —（客户端无转码） | —（服务器端） |
| HDR10 / HDR10+ / HLG / 色调映射 | Y（10.10 + ffmpeg tone-map） | Y（Premiere） | Y | P（透传，依赖 MediaCodec） |
| Dolby Vision Profile 5 直通 | Y（10.10 新支持） | Y（Premiere） | Y（Pro） | P（依赖设备 MediaCodec） |
| Dolby Vision Profile 7（双层） | N | P（转码 fallback） | N（P7 不可） | N（Android 不支持） |
| TrueHD Atmos 直通 | P（官方客户端 bug 多） | Y（2024 文档确认 Shield/Chromecast） | Y（Pro） | P（依赖设备 eARC 支持） |
| DTS-HD MA / DTS:X 直通 | P（同 bug） | Y | Y（Pro） | P（同上） |
| AV1 / HEVC RExt / VP9 | Y（10.10） | P | Y | P（依赖解码器） |
| 4K >100Mbps 蓝光原盘率播放 | P | P | Y（Pro 不限码率） | P（取决于网络与解码器） |
| 播放速度（0.5×–2×） | P（客户端差异） | P | Y（Pro） | Y |
| 自动帧率 / 色彩匹配（AFM/AFC） | P（部分安卓 TV） | P（部分） | Y（ATV） | Y[^P1-8] |
| A-B 循环 / 章节缩略图 | P（trickplay） | P | Y（Pro 章节缩略图） | P[^P1-9]（章节缩略图已实现，A-B 循环未纳入） |
| Trickplay Preview（章节 seek 缩略图） | Y（10.10 优化） | Y | Y | Y[^P1-7] |

## 5. 字幕

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| SRT / VTT | Y | Y | Y | Y |
| ASS/SSA 完整样式（LibASS） | P（Android TV 有 LibASS bug） | P | Y（Pro，完整样式） | P[^P1-1]（JNI 架构完成，LibASS 为存根，自动降级 WebVTT） |
| PGS / VobSub 图形字幕 | P（10.10 PGS Direct 直传仍易丢） | P | Y（Pro） | Y[^P1-2]（完整段解析 + 图形层叠加） |
| 转码时 Always Burn-in | Y（10.10 增加强制选项） | Y（Premiere） | — | Y |
| HLS 字幕递送（WebVTT embed） | Y | Y | Y | Y |
| 字幕偏移上报到协议层 | P（客户端各自实现） | P | P | Y |
| 字幕字体 / 大小 / 位置自定义 | P | P | Y（Pro） | P[^P1-4]（7 类样式可配置，含 ASS 作者样式保留） |
| 强制字幕识别（forced flag） | Y | Y | Y | Y |
| 自动编码识别（GBK/BIG5） | P（服务器端 UTF-8 抓取） | P | P（社区常见反馈缺失） | Y[^P1-3]（juniversalchardet 自动识别 + 手动强制） |

## 6. 剧集增强

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| Next Up 自动下一集 | Y | Y | Y（自动连播） | Y[^P1-6] |
| 播放末 N 秒 Next Up 卡片 | Y（官方 ATV 免费） | P（Premiere 独占） | Y（Pro） | Y[^P1-6] |
| 自动连播 Auto-Advance | P（客户端差异） | P | Y（Pro） | Y[^P1-6] |
| Intro Skip（片头跳过） | P（MediaSegment API 10.10，客户端需补） | Y（Premiere 专有检测） | Y | Y[^P1-5] |
| Credits Detection（片尾跳过） | P（MediaSegment，同上） | Y（Premiere） | Y | Y[^P1-5] |
| 季 / 集"下集"横向 strip | P（客户端差异） | Y | Y | Y（详情页已实现） |
| 已看 / 未看 / 进度角标 + 底部进度条 | Y | Y | Y（halo 内置进度） | Y（MediaStatusOverlays） |

## 7. 用户状态回写（用户产生数据）

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| Playback check-in（started/progress/stopped） | Y | Y | Y | Y |
| 喜欢 / 收藏 Favorite | Y | Y | Y | Y[^P1-10] |
| 用户评分 UserRating（1–10/100） | Y | Y | Y（Trakt 同步） | Y[^P1-11] |
| 播放列表 Playlists | Y | P（Premiere 备份/跨用户） | Y（Pro + Trakt） | N |
| Watched 状态手动切换 | Y | Y | Y | Y[^P1-10] |
| Trakt.tv 同步（观看/评分/收藏） | P（官方 Trakt 插件） | P（Premiere 插件） | Y（Pro） | N（不纳入近期） |

## 8. 离线与缓存

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| 客户端离线下载（mobile/TV） | P（部分客户端） | Y（Premiere Convert & Sync） | Y（Pro） | N |
| 云端转码同步到云存储 | N | Y（Premiere Cloud Sync） | P（云直读不下载） | N |
| 本地首页 rows 缓存 | P（客户端各自） | P | P（iCloud 元数据） | Y（HomeRowsCache + HomeEntryFlow） |
| 本地海报 / 艺术图缓存（LRU + Disk） | P | P | Y（Pro） | Y（BitmapCache 双级） |

## 9. 高级服务器能力

| 能力 | Jellyfin 10.10 | Emby Premiere | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| Live TV / EPG | Y | Y（Premiere） | P（Jellyfin/Emby 时部分可用，DVR 不全） | N（明确不进入范围） |
| DVR 录制 / 时移 / 冲突处理 | Y | Y（Premiere） | N（未实现调度） | N |
| Emby Cinema Mode（影院预告/片头） | P（Local Intros 插件） | Y（Premiere） | P（片头跳过） | N |
| Smart Playlists（动态规则） | P（插件） | Y（Premiere 备份） | Y | N |
| 服务器配置备份 / 一键迁移 | P（脚本） | Y（Premiere） | — | — |

## 10. UI / UX

| 能力 | Jellyfin 10.10 (Android TV) | Emby (Android TV) | Infuse 7 (Apple TV) | CinePilotTV |
| --- | --- | --- | --- | --- |
| 原生 View / 原生焦点引擎 | Y（Kotlin+View） | Y（Xamarin→Kotlin） | Y（tvOS 原生） | Y（Kotlin+Android View） |
| 白色 Halo 焦点 glow + 缩放 + 动画 | P（基础焦点框） | P | Y（HDR 峰值亮度 + 动态染色） | Y（FocusOutline ValueAnimator 150/120ms） |
| Linear D-pad 加速度（不跳格） | P（0.19 引入开关，2024 Reddit 61% 抱怨） | Y（社区 82% 好评） | Y（fast snap end + smooth lag） | Y（自写 focus 移动，线性） |
| 焦点溢出（cutting 8dp / 负 padding） | N | N | —（tvOS 原生） | Y（FocusOverflow -8dp） |
| 焦点死区（row header ↔ cell） | 常见（Reddit 61%） | 基本无 | 无 | 无（MediaWallRow 已专项修复） |
| Cinematic backdrop / hero 动态艺术 | P | P | Y（hero + 季选切换动态更新） | Y[^UI-2] |
| 侧栏 / side sheet（设置/错误/信息） | P（全屏大卡） | P | Y（side panel 占 20% 屏幕） | Y（SideSheet） |
| 详情页 compact selector（音轨/字幕） | P（全屏选择） | P（全屏选择） | Y（非侵入式侧栏） | Y（CompactSelector） |
| Top Shelf / 系统级主屏行 | N（Android TV 通道有） | N | Y（tvOS Top Shelf，含 10s 预览） | N（Android TV 通道未做） |
| 系统语音搜索 / Universal Search | P（Google Assistant） | P | Y（tvOS Spotlight + Siri） | N |
| SharePlay / 多人同看 | — | — | Y（tvOS） | —（Android 平台无） |

## 11. OSD（播放中控制层）

| 能力 | Jellyfin Android TV | Emby Android TV | Infuse 7 ATV | CinePilotTV |
| --- | --- | --- | --- | --- |
| OSD 弹出延迟（首键触发） | 1.2–1.5s（Reddit 抱怨） | ~0.4s | 即时 | 取决于 Media3 皮肤 |
| 全屏音/字幕菜单 vs 侧栏 | 全屏 | 侧栏 20% | 侧栏 | 全屏（Media3 原生） |
| 全分辨率章节缩略图 | 低分辨率（Reddit 抱怨） | 全分辨率 | 全分辨率 + 章节 strip | 全分辨率[^P1-7] |
| 自定义自动消失计时器 | P | Y（2–10s / 禁用） | Y | P（Media3 默认） |
| 可调 per-direction seek（左 5s/右 10s） | Y | N（锁 10s，长按） | Y（可定制 skip 间隔） | N（Media3 默认 +/-10s） |
| 高级码率 / HDR / 音频元信息 overlay | Y（无付费墙） | P（需第三方 overlay） | P（有限） | P（debugInfo 非面向用户） |

## 12. 平台特性

| 能力 | Jellyfin (Android TV) | Emby (Android TV) | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| Android TV / Google TV | Y | Y | N（Infuse 8 beta） | Y |
| Apple TV / tvOS | N | N | Y | N（明确不支持） |
| Android Auto / CarPlay | P（官方客户端） | Y（Premiere） | Y（CarPlay Pro） | N |
| AirPlay 2 / Chromecast Casting | P（Chromecast） | Y（Premiere） | Y（Pro，AirPlay 2 + Cast） | N（无内置 Cast） |
| PiP（画中画） | N（Android TV 无标准） | N（同上） | Y（Pro，tvOS） | N |
| iCloud 跨端进度/元数据同步 | — | — | Y（Pro） | N |

## 13. 播放稳定性 / 已知问题

| 条目 | Jellyfin Android TV | Emby Android TV | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| DTS-HD MA 直通 bug | 7+ open issues（Reddit/issue） | 文档确认 Shield 等可用 | Y（Pro，社区参考实现） | P（取决于设备） |
| HDR → 错误转码触发 | 常见（VideoRangeTypeNotSupported） | P | Y | P（无显式 HDR fallback） |
| 字幕渲染失败（LibASS / PGS / VOB） | 6+ open issues | P | Y（Pro） | P（ASS/PGS 弱） |
| LiveTV 崩溃 / 闪退 | 常见 | P | N | —（未实现） |
| 系统屏保在播放中激活 | 有报告 | P | — | P |
| Fire TV Continue Watching 消失 | 有报告 | P | — | 未验证 |

## 14. 工程与构建

| 条目 | Jellyfin Android TV | Emby Android TV | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| 语言 | 68% Kotlin + 32% Java | Kotlin（历史有 Xamarin） | Swift（tvOS/iOS） | 100% Kotlin + Java（core） |
| 构建系统 | Gradle | Gradle | Xcode | Gradle |
| 静态检查 | Detekt 等 | — | — | Detekt 1.23.7 + Gradle Lint |
| UI / 协议分离（不泄漏类型） | P（偶有服务器类型泄漏） | P | — | Y（grep 守卫 + 3 个 adapter 例外） |
| 不嵌入服务器路径字面量（UI 侧） | N（经常硬编码） | N | — | Y（scripts/check.sh Rule B grep） |
| Robolectric / 单元测试 | P（有） | — | — | Y（Robolectric 4.14.1 SDK 26，12 tests） |
| ProGuard / R8 minify | P | Y | — | Y（release build 配置完整） |
| release build shrinkResources | P | Y | — | Y |

## 15. 订阅模式

| 条目 | Jellyfin | Emby | Infuse 7 | CinePilotTV |
| --- | --- | --- | --- | --- |
| 服务器端 | 全免费开源 | $5/月 / $54/年 / $119 终身 | —（客户端产品） | 全免费 |
| 移动端 TV 客户端 | 全免费 GPLv2 | Premiere 解锁全部 | $1.99/月 $9.99/年 $29.99 终身 | 全免费 |
| 核心特性付费墙 | 无 | IntroSkip / Credits / Cinema / Smart Screens / Cloud Sync 等 10+ 付费独占 | DV P5 / TrueHD / PGS 字幕 / iCloud 同步 / AirPlay+Cast / 4K 高码率 等 Pro 独占 | 无 |

---

## P1 交付能力脚注（2026-06 完成批次）

[^P1-1]: ASS/SSA 渲染架构：`LibassSubtitleDecoder` + `subs_decoder_jni.cpp` JNI 桥接 + `PgsSubtitleOverlay` 图形层。当前 LibASS 为 C++ 存根（返回空帧 → 自动降级 Media3 默认文字渲染），CMakeLists.txt 已预留 real LibASS 接入位置。详见 commit `c2a90b1`。
[^P1-2]: PGS/VobSub 图形字幕：`PgsSubtitleDecoder` 完整段解析（PCS/WDS/PDS/ODS/END）+ YCbCr→RGBA 调色板 + ODS RLE 解码；`PgsSubtitleOverlay` 最上层透明图层叠加，按 video size letterbox 计算显示区域。详见 commit `c2a90b1`。
[^P1-3]: 字幕自动编码识别：`SubtitleEncodingDetector`（`tv/cinepilot/tv/subtitles/`）基于 `juniversalchardet 2.4.0`，支持 GBK/BIG5/Shift_JIS/EUC-KR/UTF-8/UTF-16；`EncodingNormalizingSubtitleDecoder` 在 queueInputBuffer 中归一化到 UTF-8；设置页可手动强制编码。详见 commit `c2a90b1`。
[^P1-4]: 字幕样式自定义：`SubtitleStyleStore` 持久化 7 类枚举（字号/颜色/背景/字体/描边/边距/不透明度），`applySubtitleStyle` 支持 `setApplyEmbeddedStyles=true` 保留 ASS 作者样式。
[^P1-5]: Intro/Credits Skip：`MediaSegmentInfo.Type.INTRO/CREDITS` 段接入，OSD 左下「跳过片头」/右下「跳过片尾」按钮，支持 `autoSkipIntro/autoSkipCredits` 设置级自动跳，`showIntroSkipButton/showCreditsSkipButton` 显示开关。详见 commit `152fe6c`。
[^P1-6]: Next Up 自动连播：末 30s 或 credits 起点弹出 `PlayerNextUpInfo` 倒计时卡片，`autoPlayNext=true` 自动跳转下一集，支持聚焦立即播放/取消。剧集结束流程内连播。详见 commit `152fe6c`。
[^P1-7]: Trickplay 章节缩略图：`/Trickplay/{itemId}/{width}/tiles.jpg` 端点集成，`TrickplayInfo` + `TrickplayGridSpec`（tileW/H, tilesPerRow, tileIntervalTicks, tileCount），设置页 `showTrickplayPreview` 开关。详见 commit `152fe6c`。
[^P1-8]: AFM/AFC 自动帧率/色彩匹配：`DisplayModeApplier.computePendingMode/commitPendingMode` 两阶段切换，`AfmConfirmationSheet` 三选项确认通知（立即切换/本次不切换/不再提醒），设置页 3 开关（刷新率/色彩空间/切换前确认）。详见 commit `068fde4`。
[^P1-9]: 章节列表 + 跳转：`PlayerScreen.chapterStrip` OSD 底部章节 title chips，点击 `onChapterClick(targetTicks) → seekToTicks`，设置页 `showChapterStrip` 开关。A-B 循环未纳入近期路线图。详见 commit `152fe6c`。
[^P1-10]: 喜欢/收藏/已看手动切换：`TvWorkflowController.toggleFavorite()` 调用 `MediaBrowserClient.toggleFavorite()`；`TvWorkflowController.toggleWatched()` 调用 `MediaBrowserClient.markWatched()`；详情页主操作区切换按钮。P0 基线交付 + P1 期间 polished。
[^P1-11]: 用户评分 0–10：`UserRatingRow` 5 颗半星分辨率 chip（点击 +2pt，长摁 +1pt）+ 社区评分后缀 + 「清除评分」；3 类详情页插入 Hero 后；`TvWorkflow.selectedItemUpdated` 同步回 row 徽章。详见 commit `835e920`。
[^P1-12]: ProviderIds 面板 + 刷新元数据：`ProviderIdEditor` 4 字段（TMDb/IMDb/TVDb/TMDb Collection）+ 保存后重新扫描元数据复选框；`MediaBrowserRequests.updateProviderIds` POST `/Items/{itemId}`；`MediaBrowserRequests.refreshMetadata` POST `/Items/{itemId}/Refresh`。详见 commit `a144e8f`。
[^P1-13]: 演职员详情跳转：`ShowDetailComponents.peopleStrip` top-10 cast 卡片 + `onPersonClick` → `TvWorkflowController.openPerson` → `MediaBrowserClient.personItems` → 同影人作品搜索结果页。3 类详情页均已接入。详见 commit `42be498`。
[^P1-14]: TV Show 详情页强化：`ShowStructureSeasonsStatus` 季详情「已看/未看」分区；「尚未开始的季」折叠 rail + `onExpandFoldedSeasons` 展开；Series 详情 Hero 区分「继续观看/下一集/查看季集」主操作。详见 commit `d335f41`。
[^P1-15]: 筛选 chips：`FilterChipsRow` 类型/年份十年段/未观看/已看过/已收藏/4K/HDR；`MediaBrowseFilters.EMPTY.withXxx()` 状态机；HomeScreen 多位置注入。详见 commit `647fb09`。
[^P1-16]: Collections / Box Sets 行：`TvWorkflowController.resolveCollectionIdFor(item)` 双路径（ParentId BoxSet 子 / TmdbCollection 匹配 BoxSet）；`ShowDetailComponents.sameCollectionRail` 详情页「同系列其他」poster rail；3 类详情页 + 结构页 `onOpenCollectionItem`。详见 commit `2667c87`。
[^P1-17]: 全剧总览视图：`seriesDetailScreen` 聚合继续观看/下一集优先 + 季折叠 rail + 同系列 rail + 演职员 rail + user rating + provider id。详见 commits `d335f41` + `2667c87`。
[^UI-2]: Cinematic hero / backdrop：`DetailsStage` cinematic 容器（backdrop + 84% alpha + 渐变 scrim + 垂直滚动，左 1180dp 可读区）；`HomeScreen` cinematic media wall 重写。详见 UI-2/3 批次。

---

# 差距分析与进入路线图的遴选

## A. 明确不纳入近期的能力（Out of Scope）

以下能力在对比中均存在于竞品，但明确不进入 CinePilotTV 近期路线图：

- **Live TV / EPG / DVR / 时移**：ROADMAP.md 已有退出条件。
- **Emby Connect**：产品优先 Jellyfin；Emby 用户可继续使用用户名密码。
- **LDAP / SSO**：服务器侧功能，客户端不新增 UI/流程。
- **Apple TV / tvOS / iOS / CarPlay**：Android TV 单平台定位。
- **SharePlay / Handoff / Universal Search**：Apple 平台特性。
- **服务器转码硬件加速 / Emby Cinema / CoverArt / Smart Screen**：服务器端能力。
- **多服务器联邦（Library Fusion）**：Infuse 最炫的卖点之一，但当前仓库只有单服务器语义（server id + item id），跨服务器合并要求身份与缓存重建。
- **多用户切换**：需要 profile 存储、焦点恢复、token 作用域扩展；等单用户稳定后再评估。
- **Cast / AirPlay**：需要 Google Play Services 可选依赖，可在 P3 考虑。
- **PiP / Android Auto**：Android TV 几乎无标准 PiP。
- **iCloud / 云存储作为源**：Android 平台不同，且需要额外云端 SDK。

## B. P1 已完成交付（17/17，2026-06）

原列为"差距"的 P1 条目已全部交付，详见上文脚注与 `docs/ROADMAP.md` 批次 6/7/8 及 UI-1/2/3/4 完成记录。完成状态通过 `./scripts/check.sh` 编译、单测、lint、detekt 全绿验证。

| # | 能力 | 实现要点 | 对应 commit / 类 |
| --- | --- | --- | --- |
| P1-1 | ASS/SSA 渲染架构 + PGS/VobSub 图形字幕 | PGS 完整段解析 + 图形层叠加；ASS JNI 桥接架构完成（LibASS 为存根，预留 real 接入位） | `c2a90b1` / `PgsSubtitleDecoder`, `PgsSubtitleOverlay`, `LibassSubtitleDecoder` |
| P1-2 | 字幕自动编码识别（GBK/BIG5/Shift_JIS→UTF-8） | juniversalchardet 自动识别 + 手动强制编码 + 2-pass UTF-8 兜底 | `c2a90b1` / `SubtitleEncodingDetector`（subtitles 包） |
| P1-3 | Intro Skip / Credits Skip | MediaSegment API 接入 + OSD 跳过按钮 + 设置级自动跳 | `152fe6c` / `MediaSegmentInfo`, `PlayerOverrides` |
| P1-4 | Next Up 自动连播 + 末 30s 下一集卡片 | 倒计时卡片 + 自动跳转 + 手动取消 | `152fe6c` / `PlayerNextUpInfo`, `PlaybackRouteController` |
| P1-5 | 自动帧率 / 色彩匹配（AFM/AFC） | DisplayModeApplier 两阶段切换 + 三选项确认通知 | `068fde4` / `DisplayModeApplier`, `AfmConfirmationSheet` |
| P1-6 | 喜欢 Favorite / 用户评分 / 手动标记已看 | TvWorkflowController 端点接入 + 详情页切换按钮 | P0 基线 + 抛光 / `TvWorkflowController.toggleFavorite()`, `toggleWatched()` |
| P1-7 | ProviderIds 面板 + 刷新元数据 | 4 字段编辑器 + 保存后重扫复选框 + 协议端点 | `a144e8f` / `ProviderIdEditor`, `MediaBrowserRequests.updateProviderIds()` |
| P1-8 | 筛选 chips + Collections / Box Sets 行 | FilterChipsRow 7 类筛选 + 同系列 rail | `647fb09`, `2667c87` / `FilterChipsRow`, `sameCollectionRail` |
| P1-9 | Trickplay 章节缩略图 | `/Trickplay/*` 端点集成 + TrickplayGridSpec | `152fe6c` / `TrickplayInfo`, `TrickplayGridSpec` |
| P1-10 | 演职员详情跳转 | peopleStrip 点击 → 同影人作品搜索结果 | `42be498` / `peopleStrip`, `TvWorkflowController.openPerson()` |

> **注**：原 P1 列表共 10 项，ROADMAP 扩展为 17 项（含 P1-11~P1-17 的 TV Show 强化、全剧总览、章节列表跳转、字幕样式存储等），全部完成。

## C. 进入 P2（后续能力池）的差距 — 推荐优先级

基于用户价值、竞品差距与工程成本，推荐按以下优先级推进：

| # | 能力 | 理由 | 优先级 | 依赖 |
| --- | --- | --- | --- | --- |
| P2-1 | 多 profile 单服务器用户切换 | 家庭场景最高频需求；当前单用户是明显短板 | 高（先做） | 独立 profile token scope |
| P2-2 | Trakt.tv 同步（scrobble / 收藏 / 评分） | 高级用户核心特性；P1-6/10/11 基础已具备 | 高 | P1-6/10/11 已完成 |
| P2-3 | Android TV 主屏通道（Next Up / Continue Watching） | 系统级入口增强；零新协议依赖 | 中 | 无 |
| P2-4 | 离线缓存管理 | Emby Premiere 独占；Infuse Pro 主卖点；Android TV 需求中等 | 中 | 下载队列 + 存储配额 |
| P2-5 | 服务器 Smart Collections / 自定义首页行 | Emby Premiere 独占；P1-16 Box Sets 基础已具备 | 中 | P1-16 已完成 |
| P2-6 | Google Assistant 语音搜索接入 | 可访问性增强；低工程成本 | 低 | 无 |
| P2-7 | Chromecast 媒体路由发送 | 多房间体验；需引入 MediaRouter 依赖 | 低 | MediaRouter |
| P2-8 | 播放速度 / skip 间隔按方向可配置 | 小众 QoL；极低用户价值 | 低 | 无 |

## D. 维持现状（已经具备或无需立即升级）

- P0 协议基础（批次 1）：**已完成**。
- P0 Android TV 外壳 + 登录 + 浏览 + 播放闭环（批次 2–5）：**已完成**（进入 polish 阶段）。
- P1 播放增强 + 用户回写 + 浏览增强（批次 6–8）：**17/17 已完成**。
- UI-1 Media Wall 基础：**已完成**。
- UI-2 首页 / UI-3 详情页 / UI-4 辅助页：**全部完成**（Series→Season→Episode 三层、焦点 glow、FocusOverflow、CompactSelector、SideSheet、PlayerOverrides 均已落地）。
- 构建质量系统（check.sh / Detekt / Lint / Robolectric / R8）：**已完成**。
