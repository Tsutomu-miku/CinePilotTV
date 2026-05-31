# 项目规格

## 产品目标

CinePilot TV 应该像一个原生 Android TV 媒体播放器，同时严格保留 Jellyfin 和 Emby 的协议语义。详细产品行为见 `docs/REQUIREMENTS.md`。

## 工程目标

- Android TV UI 和应用编排使用 Kotlin。
- 协议和领域规则放在 JVM 兼容模块中，确保不依赖 Android runtime 也能测试。
- 播放、轨道选择和播放能力检查使用 Android Media3。
- 协议请求先建模为 method、path、headers、query、body 的纯规格，再由 HTTP adapter 发送。
- 协议适配器与 UI、播放器组件分离。
- 状态转换放进类型化 action 或领域函数中。
- TV 界面状态和焦点身份先放在 JVM 可测试 workflow 中，再由 Android UI 渲染。
- Android ViewModel 应调用 `TvWorkflowController`，不要在 UI 层直接编排协议请求。
- 新行为进入 Android runtime 前，应尽量能通过 `./scripts/check.sh` 测试。
- 优先修复身份、生命周期和适配器边界的根因问题。

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
