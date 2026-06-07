# Infuse 近似复刻设计审计

## 官方依据

- Firecore/Infuse 官方产品页把 Infuse 定位为 elegant video player，并强调媒体库会自动获取 rich metadata、artwork、cast、ratings、trailers，让来源各异的媒体看起来属于同一个媒体库。
- Firecore release notes 持续提到 Liquid Glass playback controls、player controls 外观更新、Jellyfin / Emby 地址与 artwork 相关改进。
- 本项目只能复刻信息架构、密度、层级、焦点路径和控件形态，不复制 Apple 私有组件、商标或私有素材。

## CinePilot 与 Infuse 的需求差异

- Infuse 的首页任务是“优雅组织媒体库”；CinePilot 的首页任务也是“快速选择媒体”，不是展示应用名称、解释如何搜索、或把账号/设置操作放到视觉中心。
- Infuse 的详情页任务是“判断是否播放”；CinePilot 额外需要 Jellyfin / Emby 的媒体源、音轨、字幕、直连 / 转码信息，但这些只能作为次级决策信息或调试层出现。
- Infuse 的播放器任务是“内容优先”；CinePilot 额外需要视频信息 HUD，但它必须跟随控制层，不得常驻遮挡内容。

## 上一轮失败根因

- 首页仍然保留 `媒体库`、`选择媒体`、`移动焦点浏览媒体库` 这类产品自述文案，首屏注意力没有交给 artwork。
- 当前所谓 Infuse 化仍然基于 `title + hero + shelf + card` 页面模型，结构上还是 Android TV 工具页。
- poster 被实现成 `card` 控件，有背景、有圆角、有标题覆盖层，导致信息密度低，也不像 Infuse 的 artwork wall。
- `glassPanel` / `infusePanelScreen` 被用作页面骨架，而不是浮层材料；结果看起来是大块表单卡片。
- 协议信息、技术信息和恢复动作仍过早进入主视觉，压过了“选片 / 播放”这个核心需求。

## 电视截图审计补充

- 2026-06-07 的电视截图显示，分类入口、继续观看、最新内容全部被渲染为同一种竖向 poster cell，导致裁图和低密度。
- `views` 行标题“媒体库”在首屏左上出现，视觉上仍像页面大标题。
- 底部 focus summary 遮挡第二行内容，进一步压缩媒体墙。
- Infuse 官方媒体库参考中，episode / season 内容明显使用横向 artwork，只有电影 / 剧集主封面更适合 poster。
- 修复方向必须是 row-aware media wall：collection、landscape、poster 分形态渲染，而不是继续调单一 poster cell。

## 新设计原则

- 首页不展示应用名和页面大标题；顶部只允许极轻 icon rail，默认焦点落在媒体 artwork。
- 主路径组件按产品模型命名和实现：`CinematicStage`、`EdgeChrome`、`CollectionRail`、`LandscapeArtworkCell`、`PosterArtworkCell`、`MediaWallRow`、`FocusOutline`、`CompactSelector`、`SideSheet`。
- 首页和详情的主结构不得使用大 glass panel、card container 或表单 section；glass 只用于 popover、side sheet、debug HUD、错误恢复等浮层。
- 1080p 首页首屏必须至少展示两行完整媒体 row，并露出第三行；密度优先级高于装饰性“高级感”。
- 焦点态使用内描边、轻阴影和低矮摘要，不通过放大、粗边框或大标题制造状态。

## 后续审计口径

- 首页截图第一眼必须是媒体墙，不是工具栏、说明页或设置页。
- 详情页截图必须像媒体详情页，不像“海报 + 表单控件”。
- 搜索结果必须复用 media wall；设置、错误、播放器信息必须是边缘浮层，不是居中大卡片。
- 检查脚本需要防止旧结构回流：禁止首页大标题文案、禁止主路径依赖 `infusePanelScreen`、禁止 `resume` / `next-up` 使用 poster cell。
