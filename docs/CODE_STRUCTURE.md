# 代码结构

这份地图说明新代码应该放在哪里。

```text
app/
  src/main/java/tv/cinepilot/tv/
    MainActivity.kt              Android TV 启动 Activity、页面流程和事件转发
    CinePilotViewModel.kt        Android lifecycle 持有 runtime 和 workflow controller
    auth/AuthRouteController.kt  服务器连接、会话恢复、登录和 Quick Connect route 编排
    auth/AuthScreens.kt          服务器输入、登录、public users 和 Quick Connect 页面布局
    details/DetailsRouteScreen.kt 媒体详情页组装、详情动作按钮和海报 View
    details/DetailTrackControls.kt 详情页媒体源、音轨和字幕内联单选控件
    error/ErrorRouteScreen.kt    错误恢复页、播放失败恢复入口和重新登录入口
    home/HomeRouteScreens.kt     首页导航组装和搜索页布局
    playback/PlaybackRouteController.kt 详情、播放准备、播放器 route 编排
    playback/PlaybackDiagnosticsController.kt 诊断导出、分享和错误页返回 route 编排
    playback/PlaybackScreens.kt  播放准备、音轨字幕、速度和诊断页面布局
    playback/PlaybackPreferences.kt 详情页播放动作到协议播放偏好的转换
    playback/SubtitleStylePreferences.kt 字幕字号、颜色、背景偏好和本机持久化
    playback/SubtitleStyleScreen.kt 字幕样式设置页面和保存后刷新
    player/Media3PlayerHost.kt   Media3 ExoPlayer / PlayerView 宿主和遥控器媒体键控制
    player/Media3PlaybackBridge.kt Media3 事件到播放上报控制器的桥接
    player/Media3StreamIndexResolver.kt Media3 selected track 到协议 MediaStream.Index 的谨慎映射
    runtime/CinePilotRuntime.kt  Android app runtime composition root
    runtime/PrimaryImageLoader.kt 海报与 public user 头像异步加载和短超时网络读取
    runtime/QuickConnectPoller.kt Jellyfin Quick Connect 自动轮询、立即检查和陈旧回调隔离
    runtime/RecentAccountStore.kt 最近登录账号列表和旧版本账号存储兼容
    ui/TvDesign.kt               TV 色彩、间距、圆角、字号和固定尺寸 design tokens
    ui/TvUi.kt                   TV 暗色主题、按钮、输入框、文字和布局 helper
    ui/TvFocus.kt                TV 焦点态、初始焦点和密集选择按钮 helper
    ui/TvFlowLayout.kt           详情页 tags 自动换行布局
    ui/MediaShelf.kt             首页横向媒体架和海报卡片
    ui/HomeScreen.kt             首页屏幕布局和浏览操作区
    ui/DetailsScreen.kt          媒体详情屏幕布局
    ui/MediaTechnicalInfo.kt     playback info 到详情页技术信息标签的格式化
    ui/PlayerScreen.kt           黑底全屏播放器容器，不叠加第二套播放控件
    ui/PlaybackText.kt           播放选项、音轨字幕和剧集上下文展示文案
    ui/TvErrorMessages.kt        网络、认证和播放器错误到中文提示的格式化
  src/main/res/drawable/ic_*.xml 开源 Material Icons 风格矢量图标资源
core/
  build.gradle.kts              JVM/Java Gradle module
  src/main/java/tv/cinepilot/core/
    protocol/                    服务器、认证、endpoint 和播放协议规则
      PlaybackInfoOptions.java   playback info 查询参数
      HlsStreamOptions.java      HLS 播放 URL 参数
      MediaImageRequests.java    媒体海报和 public user 头像图片 URL 请求规格
      PlaybackSourceSelector.java 播放源选择规则
      PlaybackInfo.java          playback info 领域模型
      MediaBrowserResponseMapper.java 核心 API 响应到领域模型的映射
      MediaBrowserClient.java    发现、登录、播放信息和登出的协议编排入口
      MediaItemSummary.java      TV UI 可消费的媒体条目摘要
      MediaItemPage.java         媒体列表分页响应
      PlaybackCheckInScheduler.java 播放 started/progress/stopped 调度规则
      PlaybackSessionController.java 播放器事件到协议上报的控制器
      PlaybackUrlAuthorizer.java Media3 播放 URL token 授权
      HttpTransport.java         HTTP 发送边界
      UrlConnectionHttpTransport.java Android/JVM 可用的默认 HTTP transport
      FileSessionRepository.java 文件持久化 session repository
    tv/                          Android TV 状态、导航和焦点领域规则
      BrowseSession.java         首页临时浏览栈、搜索返回和文件夹分页上下文
      HomeRowsLoader.java        组合媒体库首页 rows
      TvDiagnostics.java         不含 token 的 TV 联调诊断信息
      TvWorkflowController.java  TV 用例编排入口
  src/test/java/tv/cinepilot/core/
    protocol/                    JVM 协议测试
docs/                            项目指导文档
  PROTOCOL_NOTES.md              Jellyfin / Emby 协议来源和约定
  VERIFICATION.md                本地检查、Android 构建和设备 QA 说明
scripts/                         健康检查和本地自动化
  bootstrap-gradle-wrapper.sh    生成 Gradle wrapper 的辅助脚本
  install-debug-apk.sh           安装并启动 debug APK 的设备 QA 脚本
```

## 放置规则

- 纯服务器语义和播放语义放在 `core`，并通过 Gradle `:core` module 暴露给 Android app。
- TV workflow、焦点身份和导航规则放在 `core/tv`，Android UI 只负责渲染和事件转发；临时浏览栈、搜索返回和文件夹分页上下文归 `BrowseSession`，不要回流到 Activity。
- Android 生命周期、焦点、私有文件目录接线、Quick Connect 轮询 / 手动检查协调和 Media3 集成放在 `app`。
- Android 视觉样式和可复用 View helper 放在 `app/.../tv/ui`；服务器连接、登录、会话恢复和 Quick Connect route 编排放在 `app/.../tv/auth`；详情页组装放在 `app/.../tv/details`；错误恢复页面放在 `app/.../tv/error`；首页导航和搜索页面放在 `app/.../tv/home`；详情到播放器的 route 编排、播放准备、音轨字幕、速度和诊断导出 / 分享放在 `app/.../tv/playback`；Activity 只保留生命周期、顶层路由和跨模块事件转发，尽量让单文件保持在 300 行左右。
- HTTP 发送边界和 session repository 接口放在 `core/protocol`；当前默认实现是 `UrlConnectionHttpTransport` 和 `FileSessionRepository`，后续如引入更完整 platform adapter，也必须保留核心接口语义。
- UI 代码不能依赖协议包里的实现细节。
- 测试放在保护对应行为的模块旁边。
- P0 / P1 批次开始或完成时，更新 `docs/ROADMAP.md`。
