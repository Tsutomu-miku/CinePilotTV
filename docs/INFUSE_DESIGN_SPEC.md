# Infuse 近似复刻 UI 规格

本文是 CinePilot TV 的新 UI 规格。上一版“Infuse 主题”失败的根因是页面模型没变：仍然是 Android 表单页、卡片页和大标题页。本轮目标是按 Infuse 的产品模型重做：artwork 是主体，metadata 服务当前焦点和播放决策，Jellyfin / Emby 协议信息只作为次级信息或调试层出现。

官方参考：[Infuse 产品页](https://firecore.com/infuse)、[Infuse Release Notes](https://firecore.com/releases)。

## 核心目标

- artwork-first media library：首屏必须首先看到媒体 artwork，而不是应用名、页面标题或操作说明。
- beautifully organized metadata：metadata 只帮助用户判断当前媒体，不做字段墙。
- elegant player：播放器内容优先，控制层和视频信息只在需要时出现。
- liquid glass as overlay：玻璃材料只用于浮层、popover、side sheet、HUD，不用于首页 / 详情主结构的页面卡片。
- cool blue highlight：焦点态使用冷白 / 冷蓝内描边和轻 glow，不能回到粗蓝块或大面积实色按钮。
- dense TV layout：1080p 下信息密度要接近成熟媒体库，不能为了“高级感”牺牲可浏览数量。

## 首页规格

- 禁止首页主路径展示 `媒体库`、`选择媒体`、`移动焦点浏览媒体库` 之类大标题或说明。
- 顶部只保留极轻 icon rail：搜索、刷新、账号、设置、退出；默认焦点必须落在第一个媒体 artwork cell。
- 首页结构是 row-aware media wall：collection、landscape、poster 三种 row 形态按内容类型选择。
- `views` 行使用 collection rail，不显示“媒体库”标题；`resume` / `next-up` 使用 16:9 landscape rail。
- movie / series 主内容才使用 poster cell；episode / video / 继续观看不得被裁成竖 poster。
- 焦点态使用内部细描边、冷白 / 冷蓝 glow、轻阴影和短动画；不通过放大造成裁切或跳动。
- 1080p 目标：poster `104x156dp`，landscape `188x106dp`，collection `150x52dp`，gap `8dp`，首屏至少显示 collection rail + 两行完整内容 row。

## 详情页规格

- 详情页首屏固定为 backdrop 背景、poster、媒体标题、metadata、主播放动作和少量次级动作。
- 不展示页面标题；标题只来自媒体本身，长标题 2-3 行自然排版。
- 播放 / 继续播放是唯一强主动作；从头播放、低码率、字幕样式、播放速度、下一集都是 compact secondary action。
- 技术信息默认是一行可换行 micro badges：分辨率、HDR / Dolby、编码、声道、字幕概览、直连 / 转码风险。
- 媒体源、音轨、字幕使用 compact selector 摘要行 + popover；不得占用大表单块。
- 剧情简介、完整技术信息下移到轻量信息区，不参与首屏播放决策。

## TV Show 能力模型

- Infuse 和 Jellyfin 的共同事实不是“文件夹里有视频”，而是 TV Show 按 `Series -> Season -> Episode` 组织；CinePilot 必须尊重服务端 metadata，而不是把剧集和季当普通文件夹。
- Series 详情页是剧集总页：展示剧集 artwork、metadata、简介、季列表和当前季集数预览。
- Season 详情页是季页，也是同季选集 hub：上半屏必须有当前集信息区，包含集标题、评分、首播日期、时长、分辨率 / 编码 / 音频 / 字幕摘要和简介；下方直接展示同季集数，不能只剩一个空标题和 episode grid。
- Series / Season 页必须消费 Jellyfin / Emby 提供的 rich metadata：`PremiereDate`、`CommunityRating`、`OfficialRating`、`People`、`MediaStreams`、`Overview`、`Genres`。拿不到字段时可以降级，但不能把页面退化成普通 folder。
- Episode 详情页是播放决策页：展示当前集、播放动作、音轨 / 字幕 / 媒体源，以及返回季 / 剧集的层级导航。
- Back 路径必须按媒体层级返回：`Episode -> Season -> Series -> 原浏览位置`；不得从季或集数页退到登录页。

## 辅助页与播放器

- 搜索页使用 media wall shell：compact search bar + filter chips，搜索结果仍展示 artwork cell。
- 设置页使用窄 side sheet；主题只是偏好入口，不是页面主视觉。
- 错误页使用 recovery side sheet：短说明 + 明确主动作 + 次级诊断入口。
- 播放器保留 Media3 核心；视频信息 HUD 是 compact side sheet，跟随控制层显示，隐藏时不遮挡画面。
- 本轮不重写 Media3 控制层，只调整周边入口、HUD 和视觉层级。

## 组件边界

- 新主路径组件：`CinematicStage`、`EdgeChrome`、`CollectionRail`、`LandscapeArtworkCell`、`PosterArtworkCell`、`MediaWallRow`、`FocusOutline`、`CompactSelector`、`SideSheet`。
- 旧 `Infuse*` 组件可以临时保留，但首页 / 详情主路径不得继续依赖 `infusePanelScreen`、`homeTopChrome` 或 card 型 poster 容器。
- `MediaPresentation` 是唯一展示模型入口，包含 `title`、`contextLine`、`microMetadata`、`watchState`、`deliveryBadges` 和 artwork fallback。
- `HomeRowPresentation` 是首页 row 形态入口，负责把 `views`、`resume`、`next-up`、`latest:*`、`search:*`、`folder:*` 映射到 collection / landscape / poster。
- glass primitive 继续存在，但只服务 overlay / popover / sheet / HUD。

## 截图验收

- 首页一眼是媒体墙，不是工具页。
- 首页不得出现大“媒体库”标题；分类入口不得是大竖 poster。
- 继续观看必须是横向 16:9 artwork，底部不得有遮挡内容的 summary bar。
- 首页默认焦点在媒体 artwork cell，遥控器上下左右路径稳定。
- 详情页像电影详情，不像海报加表单。
- 搜索、设置、错误、播放器信息与主路径视觉一致，但不抢主路径注意力。
- `scripts/check.sh` 防止旧结构回流：禁止首页大标题文案，禁止主路径使用大 panel/card，检查 media wall density tokens。
