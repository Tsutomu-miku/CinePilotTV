# Infuse 风格重设计规格

本文是 CinePilot TV 的 Infuse 风格设计规格。它不是主题色清单，而是后续实现首页、详情、搜索、播放器、设置、错误和诊断页面时必须遵守的视觉与交互目标。

## 设计目标

Infuse 参考对象来自 Firecore 官方介绍和 release notes：它强调优雅的视频播放器、丰富 artwork / metadata、清晰播放控制，以及 tvOS 版本中的 Liquid Glass design / player controls 外观更新。本项目不复制 Apple 私有组件，也不做逐像素复刻；目标是把这些方向转换为 Android TV 可维护的设计语言。

核心关键词：

- cinematic black：全局背景是近黑影院舞台，页面不应像普通表单工具。
- artwork-first：海报、backdrop、媒体 artwork 是首屏主视觉，不让工具栏或按钮抢第一注意力。
- liquid glass：信息层和控制层使用半透明深色玻璃、轻边框和柔和层级，不使用厚重实色卡片。
- cool blue highlight：焦点和选中态使用冷蓝 / 冷白高光，避免一眼像 Jellyfin 蓝或 Emby 绿。
- quiet metadata：metadata 是辅助判断信息，使用小号、轻量、可换行的标签，不变成调试字段墙。

禁止项：

- 只新增一个主题色，却继续保留页面硬编码灰蓝、纯黑面板或粗重按钮。
- 只使用半透明 `argb` 色块冒充玻璃；没有 blur、高光边、暗色 tint 和焦点 material state 的，不算 Infuse 对齐。
- 首页首屏首先看到工具栏，而不是媒体内容。
- 详情页变成字段表、调试面板或按钮列表。
- 大面积文字按钮、粗边框、强烈渐变装饰、光斑背景。
- 为了视觉效果牺牲遥控器焦点、字幕 / 音轨选择、播放器原生能力。

## 页面规格

### 首页

- 首页首屏必须是 focused artwork hub：当前焦点媒体的 `Backdrop > Thumb > Primary blurred` 作为背景，左侧显示标题和安静 metadata。
- 媒体 row 和海报卡片优先，工具栏收敛为顶部轻量操作区，默认焦点必须落在媒体而不是工具按钮。
- 海报卡片保留当前紧凑密度，但视觉上要更像 artwork：常态不压标题黑条，焦点卡才显示轻玻璃标题。
- 焦点卡片必须通过冷蓝高光、轻微层级、内部焦点环和标题遮罩变化表达；不能靠放大卡片制造选中感。
- 空状态仍要有恢复动作，但使用轻玻璃信息面板，不做大卡片套大卡片。
- 文件夹分页、搜索结果、继续观看、最新媒体 row 必须保持同一媒体墙语言。

### 详情页

- 详情页目标是 cinematic detail：全屏 backdrop / fanart 背景加暗角保护，左侧海报 / artwork，右侧标题、metadata、主动作和次级信息。
- 没有 backdrop 时使用 Thumb 或 Primary blurred fallback，不能退回纯黑表单页。
- 主动作只有“播放 / 继续播放”可以突出；从头播放、低码率、字幕样式、播放速度保持次级。
- metadata 和技术信息使用轻量玻璃标签，优先展示观看决策语言：分辨率、HDR / Dolby、编码、声道、字幕概览和转码风险。
- 媒体源、音轨、字幕继续使用 OptionSelect 摘要行 + popover，不平铺成 RadioSelect，不跳转到独立选择页。

### 搜索页

- 搜索页保持高信息密度，但外观必须接近轻玻璃面板，而不是手机表单。
- 筛选项继续使用紧凑横向选择组；当前值由选中态表达，不加“已选”前缀。
- 搜索结果页要继续露出媒体 artwork，不能只显示文字列表。
- 无结果页面保留“重新搜索 / 返回首页 / 切换账号”，主动作清晰但不使用巨大按钮墙。

### 设置页

- 设置页第一屏只承载全局偏好，不作为视觉展示页或营销页。
- 主题 OptionSelect 必须包含 `Infuse 玻璃`，文案说明它是深黑舞台与冷蓝玻璃高光。
- 后续新增偏好时使用同一设置行组件，不做密集手机表单。
- 设置页 Back 返回首页；主题选择立即应用并持久化到本机。

### 错误与诊断

- 错误恢复页必须保持主动作明确：低码率重试、重新登录、返回首页等。
- 信息说明和诊断内容放在轻玻璃面板中，不能像 raw log 窗口。
- 诊断导出 / 分享保留图标动作和初始焦点规则。
- 错误页切换 Infuse 主题后不能出现未主题化的硬编码灰蓝块。

### 播放器

- 本阶段不重写 Media3 控制层；保留原生字幕、音轨、时间轴和播放状态能力。
- 播放器仍是黑底全屏，内容优先，控制层不常驻遮挡。
- 视频信息按钮必须位于 Media3 设置按钮同一控制行，不能成为外层独立覆盖按钮。
- 视频信息面板使用 `TvColors.Overlay` 与轻边框；只在控制层可见时显示，避免遮挡画面。
- 快捷 seek 反馈保持轻量文字和时间轴显示，不增加第二套快进 / 快退按钮。

## 组件规格

### Design Tokens

Infuse 相关主题必须覆盖完整视觉 token，而不是只换焦点色：

- background：近黑影院舞台。
- surface / surfaceRaised / surfaceControl / surfaceInput：半透明深色玻璃层级。
- posterFallback / posterBorder：海报缺省底色和轻边框。
- focus / focusRing / focusText：冷蓝和冷白焦点。
- accent / accentStrong / resume：动作和恢复播放高光。
- textPrimary / textSecondary / textMuted：冷白文字层级。
- pillBorder：metadata 标签轻边框。
- overlay：海报标题遮罩和播放器信息面板。
- glassTint / glassFocusTint / glassBorder：真正玻璃层的暗色 tint、焦点 material state 和高光边。

### Glass Primitive

- 必须通过共享 `GlassDrawable` / `GlassTokens` / `glassPanel` 实现，不允许页面各自写半透明矩形。
- API 31+ backdrop 图片应使用 `RenderEffect.createBlurEffect`；低版本 fallback 也必须有 dark tint、hairline border 和轻高光。
- 玻璃层只承载摘要、选择器、诊断等信息；不能把整个页面包进大卡片。

### 海报卡片

- 卡片尺寸继续服务 TV 信息密度，不为“高级感”盲目放大。
- 标题遮罩必须半透明，并随主题变化。
- 焦点态使用内部描边、轻微 elevation 和短动画，不改变布局尺寸。

### 按钮

- 按钮尽量图标 + 短文本；播放器控制区优先 icon-only。
- 主按钮只给最高频动作使用，不能一排按钮同权重。
- 焦点态必须高对比，但边框不得超出组件边界导致裁切。

### OptionSelect

- 摘要行显示字段名 + 当前值；确认后打开 popover。
- Popover 使用轻玻璃 surface、冷蓝选中态和清晰当前项标记。
- 长选项单行截断，避免弹层撑满屏幕。

### Metadata Pill

- 字号小于正文，颜色低于标题。
- 边框使用 `TvColors.PillBorder`，背景使用主题化 poster fallback 或 glass surface。
- 多行换行，不横向滚出屏幕。

### 信息面板

- 播放器视频信息、错误诊断和空状态说明使用轻玻璃面板。
- 面板内容保持可扫读，不使用 raw protocol 字段名作为主视觉。
- 信息面板不能嵌套在另一个卡片里。

### 焦点动画

- 获得 / 失去焦点使用短动画，建议 120-180ms。
- 允许 alpha、foreground ring、translationZ、背景色过渡。
- 不允许通过 scale 放大造成边框裁切、滚动跳动或布局重排。

## 硬编码迁移要求

后续实现 Infuse 化时，以下颜色不得散落在页面里：

- 任何 metadata pill 边框必须走 `TvColors.PillBorder`。
- 海报标题遮罩和播放器信息面板必须走 `TvColors.Overlay`。
- 海报 fallback 必须走 `TvColors.PosterFallback`。
- 页面 / 组件 surface 必须走 `TvColors.Surface*`。
- 输入框焦点不能写死青绿色，应走主题 surface / focus token。

## 截图验收

设计验收以 1920x1080 TV 截图为主，不以单个颜色值为主。

- 首页首屏：第一眼是媒体 artwork，而不是工具栏或设置页。
- 详情页：像媒体详情页，有海报、标题、主动作和安静 metadata，而不是字段表。
- 搜索页：筛选紧凑，结果仍以媒体 artwork 为主。
- 设置页：轻量、克制，不像手机设置表单。
- 错误 / 诊断页：恢复动作明确，信息面板不突兀。
- 播放器：内容优先，控制层不抢戏，视频信息按钮和设置按钮同一层级。

## 后续实现顺序

1. 首页媒体墙：弱化顶部工具区视觉权重，强化海报标题遮罩和焦点质感。
2. 详情页 artwork 氛围：在不影响可读性的前提下引入 backdrop / poster-derived 背景。
3. 搜索 / 设置 / 错误页轻玻璃统一：把所有硬编码面板迁到 token。
4. 播放器周边 polish：只优化信息按钮、视频信息面板、快捷 seek 反馈和焦点，不重写 Media3 控制层。
5. 如果仍不够接近 Infuse，再单独规划自定义播放器控制层。

## 当前实现状态

- 已新增共享 glass primitive：`GlassDrawable`、`GlassTokens`、`glassPanel` 和 API 31+ backdrop blur helper。
- 已补齐 artwork 协议和 runtime loader：媒体项建模 `BackdropImageTags`，Android 端可按 Backdrop / Thumb / Primary fallback 加载背景。
- 首页已切换为 focused artwork hub：焦点媒体驱动背景 artwork 和左侧摘要，海报卡常态不再压黑色标题条。
- 详情页已切换为 cinematic detail：全屏背景 artwork + 暗角，主决策区直接浮在背景上，音轨 / 字幕 / 技术信息进入轻玻璃区。
- OptionSelect 和播放器视频信息已改用共享 glass primitive；搜索、设置、错误页仍需后续按同一语言继续 polish。
