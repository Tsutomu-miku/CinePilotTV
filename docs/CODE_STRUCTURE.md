# 代码结构

这份地图说明新代码应该放在哪里。

```text
app/
  src/main/java/tv/cinepilot/tv/
    MainActivity.kt              Android TV 启动 Activity、页面流程和事件转发
    CinePilotViewModel.kt        Android lifecycle 持有 runtime 和 workflow controller
    player/Media3PlayerHost.kt   Media3 ExoPlayer / PlayerView 宿主
    player/Media3PlaybackBridge.kt Media3 事件到播放上报控制器的桥接
    runtime/CinePilotRuntime.kt  Android app runtime composition root
    ui/TvUi.kt                   TV 暗色主题、按钮、输入框、文字和布局 helper
    ui/MediaShelf.kt             首页横向媒体架和海报卡片
    ui/HomeScreen.kt             首页屏幕布局和浏览操作区
    ui/DetailsScreen.kt          媒体详情屏幕布局
  src/main/res/drawable/ic_*.xml 开源 Material Icons 风格矢量图标资源
core/
  build.gradle.kts              JVM/Java Gradle module
  src/main/java/tv/cinepilot/core/
    protocol/                    服务器、认证、endpoint 和播放协议规则
      PlaybackInfoOptions.java   playback info 查询参数
      HlsStreamOptions.java      HLS 播放 URL 参数
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
- TV workflow、焦点身份和导航规则放在 `core/tv`，Android UI 只负责渲染和事件转发。
- Android 生命周期、焦点、私有文件目录接线和 Media3 集成放在 `app`。
- Android 视觉样式和可复用 View helper 放在 `app/.../tv/ui`；页面流程仍在 Activity，后续继续按 auth / home / details / playback 拆分，尽量让单文件保持在 300 行左右。
- HTTP 发送边界和 session repository 接口放在 `core/protocol`；当前默认实现是 `UrlConnectionHttpTransport` 和 `FileSessionRepository`，后续如引入更完整 platform adapter，也必须保留核心接口语义。
- UI 代码不能依赖协议包里的实现细节。
- 测试放在保护对应行为的模块旁边。
- P0 / P1 批次开始或完成时，更新 `docs/ROADMAP.md`。
