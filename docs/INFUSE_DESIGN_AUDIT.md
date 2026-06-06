# Infuse 风格设计审计

## 官方依据

- Infuse 官方定位强调优雅视频播放器、丰富 artwork / metadata 和高质量播放控制。
- Firecore release notes 显示 tvOS 8.2.5 引入 Liquid Glass design，tvOS 8.2.7 继续更新 player controls 外观。
- 本项目不能复制 Apple 私有组件，因此只提炼可落地原则：深黑舞台、半透明玻璃 surface、冷蓝高光、海报优先、控制层轻量、metadata 不喧宾夺主。

## 已落地

- 全局设置新增 `Infuse 玻璃` 主题。
- `TvColors.applyTheme` 从只切焦点色升级为完整 palette：背景、surface、raised surface、control、input、poster fallback、边框、焦点、文字、pill 边框和 overlay。
- 首页海报标题遮罩使用主题 overlay，焦点标题底条使用主题强调色。
- 详情页海报 fallback、metadata pill 和技术信息 pill 使用主题 token。
- 播放器视频信息面板使用主题 overlay；视频信息按钮保持在 Media3 设置同级控制行。
- 输入框、空状态、OptionSelect、设置页继续复用统一 surface / focus token。

## 页面审计

- 登录 / 服务器入口：当前结构正确，主要依赖主题 token；后续如果继续 Infuse 化，可把最近账号和服务器行改得更像轻玻璃列表，但不增加首屏复杂度。
- 首页：海报密度已合适，Infuse 主题重点在深黑背景和冷蓝焦点。不要扩大海报或增加营销式 hero。
- 搜索：筛选行沿用紧凑 setting row，符合 TV 可扫读目标。后续可加入更轻的搜索结果标题样式。
- 详情：海报和播放动作优先级正确。Infuse 化只强化 surface 和 metadata 质感，不改变详情到播放的短路径。
- 播放器：保持黑底全屏；控制层只保留原生播放 / 暂停、时间轴、字幕 / 设置 / 视频信息，不再增加第二套 app 控制。
- 设置：主题是第一屏唯一全局偏好，避免变成手机设置表单。
- 错误 / 诊断：主动作和信息面板使用统一 token，保证切换主题后不割裂。

## 后续改进

- 增加与当前媒体 artwork 相关的详情页背景，需要先做图片异步加载和亮度保护，避免标题可读性下降。
- 播放器控制层如果后续自定义，需要保持 Media3 音轨 / 字幕能力，不能为了视觉完全重写控制器。
- Infuse 主题可以继续微调透明度和边框，但不应引入大面积紫蓝渐变或装饰光斑。
