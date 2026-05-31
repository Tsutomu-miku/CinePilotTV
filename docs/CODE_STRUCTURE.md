# 代码结构

这份地图说明新代码应该放在哪里。

```text
app/
  src/main/java/tv/cinepilot/tv/
    MainActivity.kt              Android TV 启动 Activity
core/
  src/main/java/tv/cinepilot/core/
    protocol/                    服务器、认证、endpoint 和播放协议规则
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
