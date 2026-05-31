# 代码结构

这份地图说明新代码应该放在哪里。

```text
app/
  src/main/java/tv/cinepilot/tv/
    MainActivity.kt              Android TV 启动 Activity
core/
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
      HttpTransport.java         HTTP 发送边界
      JavaNetHttpTransport.java  JVM 默认 HTTP transport
  src/test/java/tv/cinepilot/core/
    protocol/                    JVM 协议测试
docs/                            项目指导文档
  PROTOCOL_NOTES.md              Jellyfin / Emby 协议来源和约定
scripts/                         健康检查和本地自动化
```

## 放置规则

- 纯服务器语义和播放语义放在 `core`。
- Android 生命周期、焦点和 Media3 集成放在 `app`。
- HTTP client 和持久化放在后续 platform adapter 包下。
- UI 代码不能依赖协议包里的实现细节。
- 测试放在保护对应行为的模块旁边。
- P0 / P1 批次开始或完成时，更新 `docs/ROADMAP.md`。
