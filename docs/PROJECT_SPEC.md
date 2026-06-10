# 项目规格

## 产品目标

CinePilot TV 应该像一个原生 Android TV 媒体播放器，同时严格保留 Jellyfin 和 Emby 的协议语义。详细产品行为见 `docs/REQUIREMENTS.md`。

**产品定位（2026-06 竞品对比后明确）**：CinePilotTV 是面向 Jellyfin / Emby 用户的 **Android TV 原生点播播放器**，以 Infuse 7 的「信息密度高、焦点清晰、直放优先、字幕完整」为 UX 标杆，但不复制其多后端 / Apple 平台特性。与官方 Jellyfin Android TV（焦点死区、OSD 慢、ASS/PGS bug）和 Emby Android TV（UX 稍好但 IntroSkip/Cinema 等核心能力锁 Premiere 付费墙）相比，CinePilotTV 的差异化在于：

1. **零付费墙**：Intro Skip / Credits Skip（通过 Jellyfin 10.10 MediaSegment 或 Emby Premiere 端点）、4K HDR、完整字幕渲染、Next Up 自动连播全部免费。
2. **工程洁癖**：UI / 协议强隔离（scripts/check.sh Rule A/B grep 守卫 + MediaPresentation/DetailPresentation adapter），服务器路径字面量不泄漏到 UI，UI 不导入原始 `MediaItemType`。
3. **Infuse 化信息架构**：CinematicStage + MediaWall + compact selector + side sheet + FocusOverflow，彻底告别"居中大卡片 / 全屏选择菜单"。
4. **播放源选择器 6 层 DirectPlay 优先**：与 Infuse 社区公认的"不做不必要转码"哲学一致。
5. **单平台 Android TV，不做 iOS/tvOS**：原生 Android View，无 Compose，无跨平台，聚焦一个平台做极致。
6. **明确不进入 Live TV / DVR**（见 `docs/ROADMAP.md` 退出条件）、不做多服务器联邦、不做 Apple 平台特性。

**竞品参考文档**：`docs/COMPETITIVE_MATRIX.md`（四维能力对比 + 差距分析 + P1/P2 遴选）。

## 工程目标

- Android TV UI 和应用编排使用 Kotlin。
- 协议和领域规则放在 JVM 兼容模块中，确保不依赖 Android runtime 也能测试。
- 播放、轨道选择和播放能力检查使用 Android Media3。
- 协议请求先建模为 method、path、headers、query、body 的纯规格，再由 HTTP adapter 发送。
- 协议适配器与 UI、播放器组件分离。
- 状态转换放进类型化 action 或领域函数中。
- TV 界面状态和焦点身份先放在 JVM 可测试 workflow 中，再由 Android UI 渲染。
- Android ViewModel 应调用 `TvWorkflowController`，不要在 UI 层直接编排协议请求。
- 在 Compose/Media3 完整 UI 接入前，允许 `MainActivity` 使用原生 View 承载最小可用流程，但业务状态仍必须走 core controller。
- 新行为进入 Android runtime 前，应尽量能通过 `./scripts/check.sh` 测试。
- 优先修复身份、生命周期和适配器边界的根因问题。

## 产品差异化工程原则（2026-06 新增，对应竞品差距）

- **字幕是核心功能，不是附属**。ASS/SSA 完整样式（LibASS）、PGS/VobSub 作为图形层叠加、GBK/BIG5 自动转 UTF-8，都必须是一等公民，而不是"能用 SRT 就行"。
- **剧集体验 = 自动跳过片头 + 末 30s 下一集卡 + 已看角标 + 底部进度条**。Season/Episode 不能降级为文件夹浏览。
- **默认直放，不做无用转码**。DeviceProfile 能力升级只在遇到真实不兼容时做，不增加 speculative codec 分支。
- **不嵌入服务器路径字符串到 UI 代码**。所有 `/Items` / `/Users` / `/Shows` / `/Sessions` / `/Trickplay` 字面量只允许出现在 `:core` 模块的 `MediaBrowserRequests.java`。
- **UI 不导入 `MediaItemType`**。`isSeries()` / `isSeason()` / `isEpisode()` / `isFolderBrowse()` 必须通过 MediaPresentation / DetailPresentation 适配器提供。此原则由 `scripts/check.sh --verify-hygiene` 守卫。

## 架构原则

- 把 Jellyfin 与 Emby 视为同源媒体浏览协议的两个方言，并通过兼容适配器屏蔽差异。
- 模块边界使用稳定公开入口。
- token 存储、HTTP transport、播放引擎和 TV UI 是不同的变化原因，应该分离。
- 不机械拆文件；只有当归属或可测试性变清楚时才拆分。
- 兼容性 fallback 必须显式，并被测试覆盖。

## 参考结构

```text
app/
  src/main/java/tv/cinepilot/tv/       Android TV Activity、界面、播放器宿主
core/
  src/main/java/tv/cinepilot/core/     协议与领域规则
  src/test/java/tv/cinepilot/core/     JVM 测试
docs/                                  产品与工程指导
scripts/                               本地验证
```

## 文件拆分判断

适合拆分的情况：

- 一个文件同时包含 HTTP 细节、状态变更、播放规则和 UI 渲染。
- 某个 helper 可以直接测试，并被多个界面复用。
- import 让人看不清模块到底属于领域、平台还是 UI。

适合保持在一起的情况：

- 逻辑只有一个清晰的变化原因。
- 抽取后只是多一层转发包装。

## 验证门槛

每次变更健康与否以 `./scripts/check.sh` 通过为准。本机安装 Android SDK 和 Gradle 后，需要把 Android 构建与单元测试命令加入这个门槛。
